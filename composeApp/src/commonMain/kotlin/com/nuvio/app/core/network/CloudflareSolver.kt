package com.nuvio.app.core.network

import com.nuvio.app.features.addons.RawHttpResponse

data class PageScrapeResult(
    val originalUrl: String,
    val finalUrl: String,
    val pageTitle: String,
    val pageHtml: String,
    val iframes: List<String>,
    val videoSources: List<String>,
    val videoUrls: List<String>,
    val scriptContents: String,
) {
    val hasVideo: Boolean get() = videoUrls.isNotEmpty() || videoSources.isNotEmpty()
    val firstIframe: String? get() = iframes.firstOrNull()
}

expect object CloudflareSolver {
    suspend fun solve(url: String): Boolean
    fun getCookies(host: String): Map<String, String>
    fun getWebViewUserAgent(): String?
    fun clear()
    suspend fun scrapePage(
        url: String,
        jsRenderDelayMs: Long = 3000L,
        timeoutMs: Long = 45_000L,
    ): PageScrapeResult?
    suspend fun scrapePageWithIframeFollow(
        url: String,
        maxDepth: Int = 3,
        jsRenderDelayMs: Long = 3000L,
    ): PageScrapeResult?
}

fun isCloudflareChallenge(response: RawHttpResponse): Boolean {
    if (response.status != 403 && response.status != 503) return false
    val serverHeader = response.headers["server"]?.lowercase() ?: ""
    if (serverHeader.contains("cloudflare")) return true
    val bodyLower = response.body.lowercase()
    return bodyLower.contains("just a moment") ||
            bodyLower.contains("__cf_chl") ||
            bodyLower.contains("cf-ray") ||
            bodyLower.contains("checking your browser")
}
