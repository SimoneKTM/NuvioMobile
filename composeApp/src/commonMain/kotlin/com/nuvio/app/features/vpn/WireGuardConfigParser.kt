package com.nuvio.app.features.vpn

object WireGuardConfigParser {
    data class ParsedWireGuardConfig(
        val serverHost: String,
        val endpoint: String,
    )

    private val sectionHeader = Regex("""^\s*\[([^\]]+)]\s*$""")
    private val keyValue = Regex("""^\s*([A-Za-z0-9]+)\s*=\s*(.+?)\s*$""")
    private val base64Key = Regex("""^[A-Za-z0-9+/]{42,44}={0,2}$""")

    fun parse(text: String): ParsedWireGuardConfig? {
        var section = ""
        var privateKey: String? = null
        var address: String? = null
        var hasPeer = false
        var peerPublicKey: String? = null
        var peerAllowedIps: String? = null
        var peerEndpoint: String? = null

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            sectionHeader.matchEntire(line)?.let { match ->
                section = match.groupValues[1].trim().lowercase()
                continue
            }
            val keyValueMatch = keyValue.matchEntire(line) ?: continue
            val key = keyValueMatch.groupValues[1].lowercase()
            val value = keyValueMatch.groupValues[2]
            when (section) {
                "interface" -> when (key) {
                    "privatekey" -> if (privateKey == null) privateKey = value
                    "address" -> if (address == null) address = value
                }
                "peer" -> {
                    hasPeer = true
                    when (key) {
                        "publickey" -> if (peerPublicKey == null) peerPublicKey = value
                        "allowedips" -> if (peerAllowedIps == null) peerAllowedIps = value
                        "endpoint" -> if (peerEndpoint == null) peerEndpoint = value
                    }
                }
            }
        }

        if (privateKey == null || !isValidKey(privateKey)) return null
        if (address.isNullOrBlank()) return null
        if (!hasPeer || peerPublicKey == null || !isValidKey(peerPublicKey)) return null
        if (peerAllowedIps.isNullOrBlank()) return null
        val endpoint = peerEndpoint?.trim().orEmpty()
        if (endpoint.isBlank()) return null

        val serverHost = endpointHost(endpoint) ?: endpoint
        return ParsedWireGuardConfig(serverHost = serverHost, endpoint = endpoint)
    }

    private fun isValidKey(value: String): Boolean {
        if (!base64Key.matches(value)) return false
        val decodedLength = (value.length * 3 / 4) - value.count { it == '=' }
        return decodedLength == 32
    }

    private fun endpointHost(endpoint: String): String? {
        val trimmed = endpoint.trim()
        return when {
            trimmed.startsWith("[") -> {
                val closing = trimmed.indexOf(']')
                if (closing == -1) null else trimmed.substring(1, closing)
            }
            else -> trimmed.substringBeforeLast(':', trimmed).ifBlank { null }
        }
    }
}
