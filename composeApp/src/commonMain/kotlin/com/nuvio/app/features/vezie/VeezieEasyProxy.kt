package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpGetTextWithHeaders

data class VeezieEasyProxyConfig(
    val proxyUrl: String,
    val email: String,
    val apiPassword: String,
) {
    fun buildProxyUrl(target: String): String {
        val base = proxyUrl.trimEnd('/')
        val encoded = encodeUrlComponent(target)
        return "$base/proxy/manifest.m3u8?url=$encoded&api_password=$apiPassword"
    }

    fun buildExtractorUrl(host: String, url: String): String {
        val base = proxyUrl.trimEnd('/')
        val encodedUrl = encodeUrlComponent(url)
        return "$base/extractor/video?host=$host&url=$encodedUrl&api_password=$apiPassword"
    }

    private fun encodeUrlComponent(s: String): String {
        return s.encodeURL()
    }
}

internal fun String.encodeURL(): String {
    val sb = StringBuilder()
    for (ch in this) {
        when (ch) {
            in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '~' -> sb.append(ch)
            ' ' -> sb.append("%20")
            '/' -> sb.append("%2F")
            ':' -> sb.append("%3A")
            '?' -> sb.append("%3F")
            '&' -> sb.append("%26")
            '=' -> sb.append("%3D")
            '#' -> sb.append("%23")
            '@' -> sb.append("%40")
            '%' -> sb.append("%25")
            '+' -> sb.append("%2B")
            ',' -> sb.append("%2C")
            ';' -> sb.append("%3B")
            '\'' -> sb.append("%27")
            '"' -> sb.append("%22")
            '<' -> sb.append("%3C")
            '>' -> sb.append("%3E")
            '{' -> sb.append("%7B")
            '}' -> sb.append("%7D")
            '|' -> sb.append("%7C")
            '\\' -> sb.append("%5C")
            '^' -> sb.append("%5E")
            '`' -> sb.append("%60")
            else -> {
                val bytes = ch.toString().encodeToByteArray()
                for (b in bytes) sb.append("%${b.toUByte().toString(16).uppercase().padStart(2, '0')}")
            }
        }
    }
    return sb.toString()
}

internal object VeezieEasyProxy {
    private val log = Logger.withTag("VeezieEasyProxy")

    private var _config: VeezieEasyProxyConfig? = null
    private val browserHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
        "Accept-Language" to "it-IT,it;q=0.9",
    )

    fun isConfigured(): Boolean = _config != null

    fun getConfig(): VeezieEasyProxyConfig? = _config

    fun configure(proxyUrl: String, email: String, apiPassword: String) {
        _config = VeezieEasyProxyConfig(
            proxyUrl = proxyUrl.trimEnd('/'),
            email = email,
            apiPassword = apiPassword,
        )
    }

    fun clear() {
        _config = null
    }

    suspend fun httpGetViaProxy(url: String): String {
        val config = _config
        if (config != null) {
            val proxyUrl = config.buildProxyUrl(url)
            return try {
                httpGetTextWithHeaders(proxyUrl, browserHeaders)
            } catch (_: Exception) {
                httpGetText(url)
            }
        }
        return httpGetTextWithHeaders(url, browserHeaders)
    }

    suspend fun extractHostUrl(host: String, videoUrl: String): String? {
        val config = _config ?: return null
        val extractorUrl = config.buildExtractorUrl(host, videoUrl)
        return try {
            httpGetTextWithHeaders(extractorUrl, browserHeaders)
        } catch (_: Exception) { null }
    }
}
