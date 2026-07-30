package com.nuvio.app.features.vezie

import com.nuvio.app.core.network.CloudflareSolver
import com.nuvio.app.core.network.PageScrapeResult
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpRequestRaw

internal object PageFetcher {
    private val browserHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language" to "it-IT,it;q=0.9",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    )

    suspend fun fetch(url: String): String? {
        val httpResult = tryHttp(url)
        if (httpResult != null) return httpResult

        val flareResult = tryFlareSolverr(url)
        if (flareResult != null) return flareResult

        return try {
            CloudflareSolver.scrapePage(url)?.html
        } catch (_: Exception) { null }
    }

    suspend fun fetchWithIframes(url: String): PageScrapeResult? {
        val httpResult = tryHttp(url)
        if (httpResult != null) {
            val iframes = extractIframes(httpResult)
            val videoUrls = extractVideoUrls(httpResult)
            return PageScrapeResult(url, httpResult, iframes, videoUrls)
        }

        val flareHtml = tryFlareSolverr(url)
        if (flareHtml != null) {
            val iframes = extractIframes(flareHtml)
            val videoUrls = extractVideoUrls(flareHtml)
            return PageScrapeResult(url, flareHtml, iframes, videoUrls)
        }

        return try {
            CloudflareSolver.scrapePageWithIframeFollow(url)
        } catch (_: Exception) { null }
    }

    private suspend fun tryFlareSolverr(url: String): String? {
        return try {
            FlareSolverr.scrapePage(url)
        } catch (_: Exception) { null }
    }

    private suspend fun tryHttp(url: String): String? {
        return try {
            httpGetTextWithHeaders(url, browserHeaders)
        } catch (_: Exception) {
            try {
                val raw = httpRequestRaw("GET", url, browserHeaders, "")
                if (raw.status in 200..299) raw.body else null
            } catch (_: Exception) { null }
        }
    }
}

internal fun extractIframes(html: String): List<String> {
    val regex = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return regex.findAll(html).map {
        var src = it.groupValues[1]
        if (src.startsWith("//")) src = "https:$src"
        src
    }.distinct().toList()
}

internal fun extractVideoUrls(html: String): List<String> {
    val urls = mutableListOf<String>()
    val patterns = listOf(
        Regex("""https?://[^"'\s<>]+\.(?:mp4|m3u8)[^"'\s<>]*""", RegexOption.IGNORE_CASE),
        Regex("""src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        Regex("""data-src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        Regex("""data-lazy-src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        Regex("""(?:file|url):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
    )
    for (pattern in patterns) {
        for (match in pattern.findAll(html)) {
            var u = match.groupValues[1].takeIf { it.isNotBlank() } ?: match.value
            if (u.startsWith("//")) u = "https:$u"
            if (u.startsWith("http") && !urls.contains(u)) urls.add(u)
        }
    }
    return urls.distinct()
}
