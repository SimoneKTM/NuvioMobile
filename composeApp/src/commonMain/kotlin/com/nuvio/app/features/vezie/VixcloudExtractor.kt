package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders

internal object VixcloudExtractor {
    private val log = Logger.withTag("VixcloudExtractor")

    suspend fun extractPlaylistUrl(vixcloudUrl: String): String? {
        return try {
            val html = httpGetTextWithHeaders(
                vixcloudUrl,
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Referer" to vixcloudUrl.substringBeforeLast("/"),
                    "Accept-Language" to "it-IT,it;q=0.9",
                ),
            )

            extractMasterPlaylistUrl(html)?.let { return it }
            extractStreamsUrl(html)?.let { return it }

            val scriptText = extractScriptWithVideoData(html) ?: return null

            val videoId = extractVideoId(scriptText) ?: return null

            val token = extractToken(scriptText)
            val expires = extractExpires(scriptText)

            val host = vixcloudUrl.substringAfter("://").substringBefore("/")
            val baseUrl = "https://$host"

            buildPlaylistUrl(baseUrl, videoId, token, expires)
        } catch (e: Exception) {
            log.e(e) { "Failed to extract Vixcloud playlist from $vixcloudUrl" }
            null
        }
    }

    private fun extractMasterPlaylistUrl(html: String): String? {
        val urlMatch = Regex("""window\.masterPlaylist[\s\S]*?url\s*:\s*'([^']+)'""").find(html) ?: return null
        val url = urlMatch.groupValues[1].replace("\\/", "/")
        val token = Regex("""'token'\s*:\s*'([^']+)'""").find(html)?.groupValues?.getOrNull(1)
        val expires = Regex("""'expires'\s*:\s*'([^']+)'""").find(html)?.groupValues?.getOrNull(1)
        val params = mutableListOf<String>()
        if (token != null) params.add("token=${token.encodeURL()}")
        if (expires != null) params.add("expires=${expires.encodeURL()}")
        return if (params.isNotEmpty()) "$url?${params.joinToString("&")}" else url
    }

    private fun extractStreamsUrl(html: String): String? {
        val active = Regex(""""name":"[^"]+","active":true,"url":"([^"]+)"""").find(html)
        val url = active?.groupValues?.getOrNull(1)
            ?: Regex("""window\.streams\s*=\s*\[[\s\S]*?"url":"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
            ?: return null
        return url.replace("\\/", "/")
    }

    private fun extractScriptWithVideoData(html: String): String? {
        val scriptRegex = Regex(
            """<script[^>]*>([\s\S]*?window\.video[\s\S]*?)</script>""",
            RegexOption.IGNORE_CASE,
        )
        return scriptRegex.find(html)?.groupValues?.getOrNull(1)
    }

    private fun extractVideoId(script: String): Int? {
        val patterns = listOf(
            Regex("""window\.video\s*=\s*\{[^}]*id\s*:\s*['"]?(\d+)['"]?"""),
            Regex("""id\s*:\s*['"]?(\d+)['"]?"""),
            Regex(""""id"\s*:\s*(\d+)"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(script)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }
        return null
    }

    private fun extractToken(script: String): String? {
        val patterns = listOf(
            Regex("""token:\s*"([^"]+)"""),
            Regex(""""token"\s*:\s*"([^"]+)"""),
            Regex("""token:\s*'([^']+)'"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(script)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractExpires(script: String): String? {
        val patterns = listOf(
            Regex("""expires:\s*"([^"]+)"""),
            Regex(""""expires"\s*:\s*"([^"]+)"""),
            Regex("""expires:\s*'([^']+)'"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(script)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun buildPlaylistUrl(
        baseUrl: String,
        videoId: Int,
        token: String?,
        expires: String?,
    ): String {
        val sb = StringBuilder("$baseUrl/playlist/$videoId")
        val params = mutableListOf<String>()
        if (token != null) params.add("token=${token.encodeURL()}")
        if (expires != null) params.add("expires=${expires.encodeURL()}")
        params.add("b=1")
        if (params.isNotEmpty()) {
            sb.append("?")
            sb.append(params.joinToString("&"))
        }
        return sb.toString()
    }
}
