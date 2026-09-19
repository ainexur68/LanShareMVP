package com.example.lanshare

/** User-entered or discovered address of a LanShare receiver. */
data class PeerEndpoint(val host: String, val port: Int) {
    companion object {
        fun parse(raw: String): PeerEndpoint {
            var value = raw.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore('/')
            require(value.isNotBlank()) { "地址不能为空" }

            val host: String
            val port: Int
            if (value.startsWith("[")) {
                val closing = value.indexOf(']')
                require(closing > 1) { "IPv6 地址格式无效" }
                host = value.substring(1, closing)
                port = if (value.length > closing + 1) {
                    require(value[closing + 1] == ':') { "端口格式无效" }
                    value.substring(closing + 2).toIntOrNull() ?: error("端口格式无效")
                } else {
                    TransferProtocol.DEFAULT_PORT
                }
            } else {
                val separator = value.lastIndexOf(':')
                if (separator > 0 && value.indexOf(':') == separator) {
                    host = value.substring(0, separator)
                    port = value.substring(separator + 1).toIntOrNull() ?: error("端口格式无效")
                } else {
                    host = value
                    port = TransferProtocol.DEFAULT_PORT
                }
            }

            require(host.isNotBlank()) { "主机地址不能为空" }
            require(port in 1..65_535) { "端口范围无效" }
            return PeerEndpoint(host, port)
        }
    }
}
