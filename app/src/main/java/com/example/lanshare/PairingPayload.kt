package com.example.lanshare

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class PairingPayload(
    val host: String,
    val port: Int,
    val token: String,
    val fingerprint: String,
    val alias: String,
    val version: Int = CURRENT_VERSION
) {
    fun encode(): String {
        validate()
        return buildString {
            append("lanshare://pair?v=")
            append(version)
            append("&host=").append(urlEncode(host))
            append("&port=").append(port)
            append("&token=").append(urlEncode(token))
            append("&fp=").append(urlEncode(fingerprint))
            append("&alias=").append(urlEncode(alias))
        }
    }

    fun toPeer(): Peer = Peer(alias, host, port, fingerprint, token)

    private fun validate() {
        require(version == CURRENT_VERSION) { "不支持的二维码版本" }
        require(isValidIpv4(host)) { "二维码中的 IP 地址无效" }
        require(port in 1..65535) { "二维码中的端口无效" }
        require(token.matches(Regex("[0-9]{4}"))) { "二维码中的连接码无效" }
        require(fingerprint.isNotBlank() && fingerprint.length <= 256) { "二维码中的设备标识无效" }
        require(alias.isNotBlank() && alias.length <= 80) { "二维码中的设备名称无效" }
    }

    companion object {
        const val CURRENT_VERSION = 1

        fun parse(raw: String): PairingPayload {
            val uri = runCatching { URI(raw.trim()) }.getOrElse { error("无法识别二维码") }
            require(uri.scheme == "lanshare" && uri.host == "pair") { "这不是 LanShare 连接二维码" }
            val params = uri.rawQuery.orEmpty()
                .split('&')
                .filter { it.isNotBlank() }
                .associate { part ->
                    val pair = part.split('=', limit = 2)
                    urlDecode(pair[0]) to urlDecode(pair.getOrElse(1) { "" })
                }
            val payload = PairingPayload(
                version = params["v"]?.toIntOrNull() ?: error("二维码缺少版本"),
                host = params["host"].orEmpty(),
                port = params["port"]?.toIntOrNull() ?: error("二维码端口无效"),
                token = params["token"].orEmpty(),
                fingerprint = params["fp"].orEmpty(),
                alias = params["alias"].orEmpty()
            )
            payload.validate()
            return payload
        }

        private fun isValidIpv4(host: String): Boolean {
            val parts = host.split('.')
            return parts.size == 4 && parts.all { part ->
                val numeric = part.toIntOrNull()
                part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit) &&
                    numeric != null && numeric in 0..255 && (part == "0" || !part.startsWith('0'))
            }
        }

        private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
            .replace("+", "%20")

        private fun urlDecode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())
    }
}
