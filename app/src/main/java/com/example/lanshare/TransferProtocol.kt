package com.example.lanshare

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Minimal LAN protocol inspired by LocalSend's prepare -> upload -> cancel flow.
 * This MVP adds explicit resume/status and whole-file SHA-256 verification.
 */
class TransferServer(private val context: Context, private val port: Int = 53317) {
    private val executor = Executors.newCachedThreadPool()
    private val sessions = ConcurrentHashMap<String, ReceiveSession>()
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (running) return
        running = true
        executor.execute {
            serverSocket = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(port)) }
            while (running) {
                val socket = try { serverSocket!!.accept() } catch (_: IOException) { break }
                executor.execute { handle(socket) }
            }
        }
    }

    fun stop() { running = false; runCatching { serverSocket?.close() }; executor.shutdownNow() }

    private fun handle(socket: Socket) = socket.use { s ->
        s.soTimeout = 60_000
        val input = BufferedInputStream(s.getInputStream())
        val output = BufferedOutputStream(s.getOutputStream())
        val request = HttpRequest.read(input) ?: return@use
        try {
            when {
                request.method == "POST" && request.path == "/v1/prepare" -> prepare(request, s, output)
                request.method == "GET" && request.path == "/v1/status" -> status(request, output)
                request.method == "PUT" && request.path == "/v1/upload" -> upload(request, input, output)
                request.method == "POST" && request.path == "/v1/complete" -> complete(request, output)
                request.method == "POST" && request.path == "/v1/cancel" -> cancel(request, output)
                else -> HttpResponse.text(output, 404, "not found")
            }
        } catch (e: Exception) {
            HttpResponse.json(output, 500, JSONObject().put("error", e.message ?: "server error"))
        }
        output.flush()
    }

    private fun prepare(req: HttpRequest, socket: Socket, out: OutputStream) {
        val body = JSONObject(String(req.body ?: ByteArray(0)))
        val sessionId = body.optString("sessionId").ifBlank { UUID.randomUUID().toString() }
        val filesJson = body.getJSONArray("files")
        val files = (0 until filesJson.length()).map { i ->
            val f = filesJson.getJSONObject(i)
            IncomingFileMeta(f.getString("id"), f.getString("name"), f.getLong("size"), f.optString("mime", null), f.getString("sha256"))
        }
        val existing = sessions[sessionId]
        if (existing != null && existing.senderHost == socket.inetAddress.hostAddress) {
            return HttpResponse.json(out, 200, existing.describe())
        }
        val approval = java.util.concurrent.CompletableFuture<Boolean>()
        val offer = IncomingOffer(sessionId, body.optString("senderAlias", socket.inetAddress.hostAddress), socket.inetAddress.hostAddress ?: "", files, approval)
        AppState.incoming.value = offer
        AppState.transfer.value = TransferUiState(TransferStage.WAITING_APPROVAL, message = "收到来自 ${offer.senderAlias} 的发送请求")
        val accepted = try { approval.get(120, java.util.concurrent.TimeUnit.SECONDS) } catch (_: Exception) { false }
        if (!accepted) return HttpResponse.text(out, 403, "rejected")
        AppState.incoming.value = null
        val rs = ReceiveSession(context, sessionId, offer.senderHost, files)
        sessions[sessionId] = rs
        HttpResponse.json(out, 200, rs.describe())
    }

    private fun status(req: HttpRequest, out: OutputStream) {
        val sid = req.query["sessionId"] ?: return HttpResponse.text(out, 400, "missing sessionId")
        val session = sessions[sid] ?: return HttpResponse.text(out, 404, "session not found")
        HttpResponse.json(out, 200, session.describe())
    }

    private fun upload(req: HttpRequest, input: InputStream, out: OutputStream) {
        val sid = req.query["sessionId"] ?: return HttpResponse.text(out, 400, "missing sessionId")
        val fileId = req.query["fileId"] ?: return HttpResponse.text(out, 400, "missing fileId")
        val offset = req.query["offset"]?.toLongOrNull() ?: 0L
        val session = sessions[sid] ?: return HttpResponse.text(out, 404, "session not found")
        val item = session.items[fileId] ?: return HttpResponse.text(out, 404, "file not found")
        if (offset != item.temp.length()) return HttpResponse.text(out, 409, "offset mismatch:${item.temp.length()}")
        val remaining = req.contentLength
        RandomAccessFile(item.temp, "rw").use { raf ->
            raf.seek(offset)
            val buf = ByteArray(1024 * 1024)
            var left = remaining
            while (left > 0) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                if (n < 0) throw EOFException("unexpected eof")
                raf.write(buf, 0, n)
                left -= n
                AppState.transfer.value = TransferUiState(TransferStage.TRANSFERRING, item.meta.name, item.temp.length(), item.meta.size, "接收中")
            }
            raf.fd.sync()
        }
        HttpResponse.json(out, 200, JSONObject().put("offset", item.temp.length()))
    }

    private fun complete(req: HttpRequest, out: OutputStream) {
        val body = JSONObject(String(req.body ?: ByteArray(0)))
        val sid = body.getString("sessionId")
        val fileId = body.getString("fileId")
        val session = sessions[sid] ?: return HttpResponse.text(out, 404, "session not found")
        val item = session.items[fileId] ?: return HttpResponse.text(out, 404, "file not found")
        if (item.temp.length() != item.meta.size) return HttpResponse.text(out, 409, "size mismatch")
        AppState.transfer.value = TransferUiState(TransferStage.VERIFYING, item.meta.name, item.meta.size, item.meta.size, "SHA-256 校验中")
        val actual = FileUtil.sha256(item.temp)
        if (!actual.equals(item.meta.sha256, ignoreCase = true)) {
            return HttpResponse.json(out, 422, JSONObject().put("error", "sha256 mismatch").put("actual", actual))
        }
        session.publish(item)
        item.verified = true
        AppState.transfer.value = TransferUiState(TransferStage.COMPLETE, item.meta.name, item.meta.size, item.meta.size, "已验证并保存到 Download/LanShare")
        HttpResponse.json(out, 200, JSONObject().put("verified", true).put("sha256", actual))
    }

    private fun cancel(req: HttpRequest, out: OutputStream) {
        val sid = req.query["sessionId"] ?: return HttpResponse.text(out, 400, "missing sessionId")
        sessions.remove(sid)
        AppState.transfer.value = TransferUiState(TransferStage.CANCELLED, message = "传输已取消")
        HttpResponse.text(out, 200, "cancelled")
    }
}

private data class ReceiveItem(val meta: IncomingFileMeta, val temp: File, var verified: Boolean = false)

private class ReceiveSession(
    private val context: Context,
    val sessionId: String,
    val senderHost: String,
    files: List<IncomingFileMeta>
) {
    val items = files.associate { meta ->
        val tempDir = File(context.cacheDir, "incoming/$sessionId").apply { mkdirs() }
        meta.id to ReceiveItem(meta, File(tempDir, "${meta.id}.part"))
    }

    fun describe(): JSONObject {
        val offsets = JSONObject()
        items.forEach { (id, item) -> offsets.put(id, item.temp.length()) }
        return JSONObject().put("sessionId", sessionId).put("offsets", offsets)
    }

    fun publish(item: ReceiveItem) {
        val resolver = context.contentResolver
        val base = FileUtil.safeName(item.meta.name)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, base)
            put(MediaStore.MediaColumns.MIME_TYPE, item.meta.mime ?: "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LanShare")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("cannot create Downloads item")
        try {
            resolver.openOutputStream(uri, "w")!!.use { out -> item.temp.inputStream().use { it.copyTo(out, 1024 * 1024) } }
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            item.temp.delete()
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}

class TransferClient(private val context: Context) {
    fun send(peer: Peer, files: List<SharedFile>) {
        Thread {
            val sessionId = UUID.randomUUID().toString()
            try {
                AppState.transfer.value = TransferUiState(TransferStage.PREPARING, message = "计算 SHA-256")
                val preparedFiles = files.mapIndexed { index, f ->
                    val hash = FileUtil.sha256(context.contentResolver, f.uri)
                    f.copy(sha256 = hash) to "f$index-${UUID.randomUUID()}"
                }
                val arr = JSONArray()
                preparedFiles.forEach { (f, id) ->
                    require(f.size >= 0) { "无法确定文件大小: ${f.name}" }
                    arr.put(JSONObject().put("id", id).put("name", f.name).put("size", f.size).put("mime", f.mime).put("sha256", f.sha256))
                }
                val prep = JSONObject().put("sessionId", sessionId).put("senderAlias", DeviceIdentity.alias(context)).put("files", arr)
                val response = request(peer, "POST", "/v1/prepare", body = prep.toString().toByteArray(), contentType = "application/json")
                if (response.code != 200) error("对方拒绝或超时 (${response.code})")
                val offsets = JSONObject(String(response.body)).getJSONObject("offsets")
                for ((f, id) in preparedFiles) {
                    var offset = offsets.optLong(id, 0L).coerceIn(0, f.size)
                    var retryCount = 0
                    while (offset < f.size) {
                        val bodyLength = f.size - offset
                        AppState.transfer.value = TransferUiState(TransferStage.TRANSFERRING, f.name, offset, f.size, if (retryCount == 0) "发送中" else "网络恢复后续传中")
                        try {
                            val upload = upload(peer, sessionId, id, f, offset, bodyLength)
                            if (upload.code == 200) {
                                offset = JSONObject(String(upload.body)).getLong("offset")
                                retryCount = 0
                                continue
                            }
                            if (upload.code != 409) error("上传失败 ${upload.code}")
                        } catch (e: Exception) {
                            retryCount++
                            if (retryCount > 30) throw e
                            AppState.transfer.value = TransferUiState(TransferStage.TRANSFERRING, f.name, offset, f.size, "连接中断，等待恢复（$retryCount/30）")
                            Thread.sleep(2_000)
                        }

                        // Receiver is the source of truth for resume offset. Retry status for transient Wi-Fi loss.
                        var resumed = false
                        for (attempt in 0 until 10) {
                            try {
                                val st = request(peer, "GET", "/v1/status?sessionId=$sessionId")
                                if (st.code == 200) {
                                    offset = JSONObject(String(st.body)).getJSONObject("offsets").optLong(id, 0L).coerceIn(0, f.size)
                                    resumed = true
                                    break
                                }
                            } catch (_: Exception) { }
                            Thread.sleep(1_000)
                        }
                        if (!resumed) error("无法从接收端获取续传偏移")
                    }
                    AppState.transfer.value = TransferUiState(TransferStage.VERIFYING, f.name, f.size, f.size, "等待接收端校验")
                    val done = request(peer, "POST", "/v1/complete", JSONObject().put("sessionId", sessionId).put("fileId", id).toString().toByteArray(), "application/json")
                    if (done.code != 200) error("完整性校验失败 (${done.code})")
                }
                AppState.transfer.value = TransferUiState(TransferStage.COMPLETE, message = "发送完成，接收端 SHA-256 校验通过")
            } catch (e: Exception) {
                AppState.transfer.value = TransferUiState(TransferStage.FAILED, message = e.message ?: "传输失败")
            }
        }.start()
    }

    private fun upload(peer: Peer, sid: String, fileId: String, f: SharedFile, offset: Long, length: Long): Response {
        val conn = open(peer, "PUT", "/v1/upload?sessionId=$sid&fileId=$fileId&offset=$offset")
        conn.doOutput = true
        conn.setFixedLengthStreamingMode(length)
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        context.contentResolver.openInputStream(f.uri)!!.use { input ->
            skipFully(input, offset)
            conn.outputStream.use { out ->
                val buf = ByteArray(1024 * 1024)
                var left = length
                var sent = offset
                while (left > 0) {
                    val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                    if (n < 0) throw EOFException("source changed while sending")
                    out.write(buf, 0, n); left -= n; sent += n
                    AppState.transfer.value = TransferUiState(TransferStage.TRANSFERRING, f.name, sent, f.size, "发送中")
                }
            }
        }
        return read(conn)
    }

    private fun skipFully(input: InputStream, target: Long) {
        var left = target
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped > 0) left -= skipped else if (input.read() >= 0) left-- else throw EOFException("cannot resume at $target")
        }
    }

    private fun request(peer: Peer, method: String, path: String, body: ByteArray? = null, contentType: String? = null): Response {
        val conn = open(peer, method, path)
        if (body != null) {
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(body.size)
            if (contentType != null) conn.setRequestProperty("Content-Type", contentType)
            conn.outputStream.use { it.write(body) }
        }
        return read(conn)
    }

    private fun open(peer: Peer, method: String, path: String): HttpURLConnection =
        (URL("http://${peer.host}:${peer.port}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 8_000; readTimeout = 130_000; useCaches = false
        }

    private fun read(conn: HttpURLConnection): Response {
        val code = conn.responseCode
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        val body = stream?.use { it.readBytes() } ?: ByteArray(0)
        conn.disconnect()
        return Response(code, body)
    }

    private data class Response(val code: Int, val body: ByteArray)
}

private data class HttpRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: ByteArray?,
    val contentLength: Long
) {
    companion object {
        fun read(input: InputStream): HttpRequest? {
            val reader = ByteArrayOutputStream()
            var matched = 0
            val end = byteArrayOf(13, 10, 13, 10)
            while (reader.size() < 64 * 1024) {
                val b = input.read(); if (b < 0) return null
                reader.write(b)
                matched = if (b.toByte() == end[matched]) matched + 1 else if (b.toByte() == end[0]) 1 else 0
                if (matched == 4) break
            }
            val headerText = reader.toString(Charsets.ISO_8859_1.name())
            val lines = headerText.split("\r\n")
            val first = lines.first().split(" ")
            val target = first.getOrElse(1) { "/" }
            val qPos = target.indexOf('?')
            val path = if (qPos >= 0) target.substring(0, qPos) else target
            val query = if (qPos >= 0) target.substring(qPos + 1).split('&').filter { it.isNotBlank() }.associate {
                val p = it.split('=', limit = 2); java.net.URLDecoder.decode(p[0], "UTF-8") to java.net.URLDecoder.decode(p.getOrElse(1) { "" }, "UTF-8")
            } else emptyMap()
            val headers = lines.drop(1).mapNotNull {
                val i = it.indexOf(':'); if (i > 0) it.substring(0, i).trim().lowercase() to it.substring(i + 1).trim() else null
            }.toMap()
            val length = headers["content-length"]?.toLongOrNull() ?: 0L
            val body = if (length > 0 && length <= 4 * 1024 * 1024) ByteArray(length.toInt()).also { buf ->
                var off = 0
                while (off < buf.size) { val n = input.read(buf, off, buf.size - off); if (n < 0) throw EOFException(); off += n }
            } else null
            return HttpRequest(first[0], path, query, headers, body, length)
        }
    }
}

private object HttpResponse {
    fun text(out: OutputStream, code: Int, text: String) = bytes(out, code, "text/plain; charset=utf-8", text.toByteArray())
    fun json(out: OutputStream, code: Int, json: JSONObject) = bytes(out, code, "application/json", json.toString().toByteArray())
    private fun bytes(out: OutputStream, code: Int, type: String, body: ByteArray) {
        val reason = when(code){200->"OK";403->"Forbidden";404->"Not Found";409->"Conflict";422->"Unprocessable Entity";else->"Error"}
        out.write("HTTP/1.1 $code $reason\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(body)
    }
}
