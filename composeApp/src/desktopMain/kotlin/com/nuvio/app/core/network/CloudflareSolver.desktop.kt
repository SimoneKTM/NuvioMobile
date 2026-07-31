package com.nuvio.app.core.network

actual object CloudflareSolver {
    actual suspend fun solve(url: String): Boolean = false

    actual fun getCookies(host: String): Map<String, String> = emptyMap()

    actual fun getWebViewUserAgent(): String? = null

    actual fun clear() = Unit

    actual suspend fun scrapePage(
        url: String,
        jsRenderDelayMs: Long,
        timeoutMs: Long,
    ): PageScrapeResult? = null

    actual suspend fun scrapePageWithIframeFollow(
        url: String,
        maxDepth: Int,
        jsRenderDelayMs: Long,
    ): PageScrapeResult? = null
}
