package com.example.lanshare

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Bounds and path rules shared by the HTTP server and JVM tests.
 *
 * These limits are deliberately protocol limits rather than UI assumptions: a peer can
 * send metadata without using this app's client code.
 */
internal object TransferProtocolValidation {
    const val MAX_PREPARE_FILES = 256
    const val MAX_SESSION_ID_LENGTH = 128
    const val MAX_FILE_ID_LENGTH = 128
    const val MAX_FILE_NAME_LENGTH = 255
    const val MAX_MIME_LENGTH = 255
    const val MAX_SENDER_ALIAS_LENGTH = 128
    const val MAX_FILE_SIZE_BYTES = 100L * 1024L * 1024L * 1024L
    const val MAX_TOTAL_SIZE_BYTES = 500L * 1024L * 1024L * 1024L
    const val MAX_METADATA_BODY_BYTES = 4L * 1024L * 1024L

    private val identifierPattern = Regex("[A-Za-z0-9_-]{1,128}")
    private val sha256Pattern = Regex("[0-9a-fA-F]{64}")

    data class PrepareRequest(
        val sessionId: String,
        val senderAlias: String,
        val files: List<IncomingFileMeta>
    )

    data class CompleteRequest(
        val sessionId: String,
        val fileId: String,
        val sha256: String?
    )

    data class UploadCheck(
        val acceptedOffset: Boolean,
        val currentOffset: Long
    )

    class ProtocolValidationException(message: String) : IllegalArgumentException(message)

    fun parsePrepare(body: JSONObject, defaultSenderAlias: String): PrepareRequest {
        val sessionId = if (!body.has("sessionId")) {
            UUID.randomUUID().toString()
        } else {
            requireSessionId(stringValue(body, "sessionId"))
        }
        val senderAlias = optionalText(body, "senderAlias", MAX_SENDER_ALIAS_LENGTH)
            ?: defaultSenderAlias
        val filesValue = body.opt("files")
        if (filesValue !is JSONArray) {
            reject("files must be an array")
        }
        if (filesValue.length() !in 1..MAX_PREPARE_FILES) {
            reject("files must contain 1-$MAX_PREPARE_FILES items")
        }

        val files = (0 until filesValue.length()).map { index ->
            val fileValue = filesValue.opt(index)
            if (fileValue !is JSONObject) {
                reject("files[$index] must be an object")
            }
            val id = requireFileId(stringValue(fileValue, "id"))
            val name = requiredText(fileValue, "name", MAX_FILE_NAME_LENGTH)
            val size = requiredLong(fileValue, "size")
            val mime = optionalText(fileValue, "mime", MAX_MIME_LENGTH)
            val sha256 = optionalSha256(fileValue, "sha256")
            IncomingFileMeta(id = id, name = name, size = size, mime = mime, sha256 = sha256)
        }
        return PrepareRequest(sessionId, senderAlias, validateFiles(files))
    }

    /** Pure metadata validation used by the JSON parser and JVM tests. */
    fun validateFiles(files: List<IncomingFileMeta>): List<IncomingFileMeta> {
        if (files.size !in 1..MAX_PREPARE_FILES) {
            reject("files must contain 1-$MAX_PREPARE_FILES items")
        }
        var totalSize = 0L
        val ids = HashSet<String>(files.size)
        files.forEachIndexed { index, file ->
            requireFileId(file.id)
            if (!ids.add(file.id)) {
                reject("duplicate file id: ${file.id}")
            }
            if (file.name.isBlank()) reject("files[$index].name must not be blank")
            validateText(file.name, "files[$index].name", MAX_FILE_NAME_LENGTH)
            if (file.size < 0) reject("files[$index].size must be non-negative")
            if (file.size > MAX_FILE_SIZE_BYTES) {
                reject("files[$index].size exceeds $MAX_FILE_SIZE_BYTES bytes")
            }
            if (totalSize > MAX_TOTAL_SIZE_BYTES - file.size) {
                reject("total file size exceeds $MAX_TOTAL_SIZE_BYTES bytes")
            }
            totalSize += file.size
            file.mime?.takeIf { it.isNotBlank() }?.let {
                validateText(it, "files[$index].mime", MAX_MIME_LENGTH)
            }
            file.sha256?.takeIf { it.isNotBlank() }?.let {
                validateSha256(it, "files[$index].sha256")
            }
        }
        return files
    }

    fun parseComplete(body: JSONObject): CompleteRequest {
        return CompleteRequest(
            sessionId = requireSessionId(stringValue(body, "sessionId")),
            fileId = requireFileId(stringValue(body, "fileId")),
            // Older LanShare senders may put the digest in prepare and omit it here.
            // The server resolves that fallback against the prepared metadata.
            sha256 = optionalSha256(body, "sha256")
        )
    }

    fun requireSessionId(value: String?): String = requireIdentifier(value, "sessionId")

    fun requireFileId(value: String?): String = requireIdentifier(value, "fileId")

    fun requireOffset(value: String?): Long {
        val raw = value ?: reject("missing offset")
        val offset = raw.toLongOrNull() ?: reject("offset must be a non-negative integer")
        if (offset < 0) reject("offset must be non-negative")
        return offset
    }

    fun requireContentLength(present: Boolean, value: Long): Long {
        if (!present) reject("missing Content-Length")
        if (value < 0) reject("Content-Length must be non-negative")
        return value
    }

    fun validateUpload(
        offset: Long,
        contentLength: Long,
        currentOffset: Long,
        declaredSize: Long
    ): UploadCheck {
        if (declaredSize < 0) reject("declared file size is invalid")
        if (offset < 0 || offset > declaredSize) {
            reject("offset must be between 0 and the declared file size")
        }
        if (contentLength < 0) reject("Content-Length must be non-negative")
        val remaining = declaredSize - offset
        if (contentLength > remaining) {
            reject("Content-Length exceeds the remaining file size")
        }
        if (contentLength == 0L && remaining > 0L) {
            reject("Content-Length must be positive while bytes remain")
        }
        return UploadCheck(acceptedOffset = offset == currentOffset, currentOffset = currentOffset)
    }

    /**
     * Resolve the only directory used for a session and keep the canonical parent relationship
     * explicit as a second line of defence behind identifier validation.
     */
    fun sessionDirectory(incomingRoot: File, sessionId: String): File {
        val root = incomingRoot.canonicalFile
        val directory = File(root, requireSessionId(sessionId)).canonicalFile
        if (directory.parentFile != root) {
            reject("sessionId resolves outside the incoming cache")
        }
        return directory
    }

    fun partFile(sessionDirectory: File, fileId: String): File {
        val directory = sessionDirectory.canonicalFile
        val part = File(directory, "${requireFileId(fileId)}.part").canonicalFile
        if (part.parentFile != directory) {
            reject("fileId resolves outside the session cache")
        }
        return part
    }

    /** Create the empty part required by publish(); non-empty uploads create it on first write. */
    fun ensurePartFile(part: File, declaredSize: Long): File {
        if (declaredSize < 0) reject("declared file size is invalid")
        if (declaredSize == 0L) {
            if (part.exists()) {
                if (!part.isFile) throw IOException("empty transfer part is not a regular file")
            } else if (!part.createNewFile() && !part.isFile) {
                throw IOException("cannot create empty transfer part")
            }
        }
        return part
    }

    private fun requireIdentifier(value: String?, field: String): String {
        val candidate = value ?: reject("missing $field")
        if (candidate.length > MAX_SESSION_ID_LENGTH && field == "sessionId") {
            reject("$field is too long")
        }
        if (candidate.length > MAX_FILE_ID_LENGTH && field == "fileId") {
            reject("$field is too long")
        }
        if (!identifierPattern.matches(candidate)) {
            reject("$field contains unsupported characters")
        }
        return candidate
    }

    private fun stringValue(body: JSONObject, field: String): String {
        val value = body.opt(field)
        if (value !is String) reject("$field must be a string")
        return value
    }

    private fun requiredText(body: JSONObject, field: String, maxLength: Int): String {
        val value = stringValue(body, field)
        if (value.isBlank()) reject("$field must not be blank")
        validateText(value, field, maxLength)
        return value
    }

    private fun optionalText(body: JSONObject, field: String, maxLength: Int): String? {
        if (!body.has(field) || body.isNull(field)) return null
        val value = stringValue(body, field)
        if (value.isBlank()) return null
        validateText(value, field, maxLength)
        return value
    }

    private fun validateText(value: String, field: String, maxLength: Int) {
        if (value.length > maxLength) reject("$field is too long")
        if (value.any { it == '\u0000' || it.isISOControl() }) {
            reject("$field contains control characters")
        }
    }

    private fun requiredLong(body: JSONObject, field: String): Long {
        val value = body.opt(field)
        if (value !is Number) reject("$field must be an integer")
        return value.toString().toLongOrNull() ?: reject("$field must be an integer")
    }

    private fun optionalSha256(body: JSONObject, field: String): String? {
        if (!body.has(field) || body.isNull(field)) return null
        val value = stringValue(body, field)
        if (value.isBlank()) return null
        validateSha256(value, field)
        return value
    }

    private fun validateSha256(value: String, field: String): String {
        if (!sha256Pattern.matches(value)) reject("$field must be a 64-character SHA-256 hex string")
        return value
    }

    private fun reject(message: String): Nothing = throw ProtocolValidationException(message)
}
