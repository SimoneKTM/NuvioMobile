package com.nuvio.app.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object CloudflareSolver {
    private val savedCookies = ConcurrentHashMap<String, Map<String, String>>()
    @Volatile
    private var webViewUserAgent: String? = null
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().removeAllCookies(null)
    }

    actual fun getWebViewUserAgent(): String? = webViewUserAgent

    actual fun getCookies(host: String): Map<String, String> =
        savedCookies[host] ?: emptyMap()

    actual fun clear() {
        savedCookies.clear()
        webViewUserAgent = null
    }

    actual suspend fun solve(url: String): Boolean = withContext(Dispatchers.Main) {
        val ctx = context ?: return@withContext false
        Log.d("CloudflareKiller", "solve() called for URL: $url")
        val deferred = CompletableDeferred<Boolean>()
        var webView: WebView? = null
        val originalHost = URI(url).host ?: ""

        try {
            webView = WebView(ctx.applicationContext).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false

                webViewUserAgent = settings.userAgentString

                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val pageUrl = url ?: return
                        if (tryExtractCookie(pageUrl) || tryExtractCookie(originalHost)) {
                            deferred.complete(true)
                        }
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.proceed()
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val requestUrl = request.url.toString()
                        return if (requestUrl.contains("/cdn-cgi/") || requestUrl.contains("recaptcha")) {
                            super.shouldInterceptRequest(view, request)
                        } else if (shouldBlockResource(requestUrl)) {
                            WebResourceResponse("image/png", null, null)
                        } else {
                            super.shouldInterceptRequest(view, request)
                        }
                    }
                }
                loadUrl(url)
            }

            withTimeout(60_000L) {
                while (!deferred.isCompleted) {
                    if (tryExtractCookie(url) || tryExtractCookie(originalHost)) {
                        deferred.complete(true)
                    } else {
                        delay(500)
                    }
                }
                deferred.await()
            }
        } catch (_: Exception) {
            if (!deferred.isCompleted) deferred.complete(false)
            false
        } finally {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    actual suspend fun scrapePage(url: String): PageScrapeResult? = withContext(Dispatchers.Main) {
        val ctx = context ?: return@withContext null
        Log.d("CloudflareScraper", "scrapePage() called for URL: $url")
        val deferred = CompletableDeferred<PageScrapeResult?>()
        var webView: WebView? = null

        try {
            webView = WebView(ctx.applicationContext).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    private var pageLoaded = false

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (pageLoaded) return
                        pageLoaded = true
                        view?.postDelayed({
                            view.evaluateJavascript(
                                "(function() { return document.documentElement.outerHTML; })();"
                            ) { html ->
                                val pageHtml = html ?: ""
                                val iframes = extractIframes(pageHtml)
                                val videoUrls = extractVideoUrlsFromHtml(pageHtml)
                                val result = PageScrapeResult(
                                    url = url ?: this@apply.url ?: "",
                                    html = pageHtml,
                                    iframes = iframes,
                                    videoUrls = videoUrls,
                                )
                                if (!deferred.isCompleted) deferred.complete(result)
                            }
                        }, 1500)
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.proceed()
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val requestUrl = request.url.toString()
                        return if (shouldBlockResource(requestUrl)) {
                            WebResourceResponse("image/png", null, null)
                        } else {
                            super.shouldInterceptRequest(view, request)
                        }
                    }
                }
                loadUrl(url)
            }

            withTimeout(30_000L) {
                deferred.await()
            }
        } catch (e: Exception) {
            Log.e("CloudflareScraper", "scrapePage failed for $url", e)
            if (!deferred.isCompleted) deferred.complete(null)
            null
        } finally {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    actual suspend fun scrapePageWithIframeFollow(url: String): PageScrapeResult? = withContext(Dispatchers.Main) {
        val ctx = context ?: return@withContext null
        Log.d("CloudflareScraper", "scrapePageWithIframeFollow() called for URL: $url")

        val initial = scrapePage(url) ?: return@withContext null
        val allVideoUrls = initial.videoUrls.toMutableList()
        val allIframes = initial.iframes.toMutableList()

        for (iframeUrl in initial.iframes) {
            val absoluteUrl = resolveUrl(iframeUrl, url)
            Log.d("CloudflareScraper", "Following iframe: $absoluteUrl")
            try {
                val iframeResult = scrapePage(absoluteUrl)
                if (iframeResult != null) {
                    allVideoUrls.addAll(iframeResult.videoUrls)
                    allIframes.addAll(iframeResult.iframes)
                }
            } catch (e: Exception) {
                Log.e("CloudflareScraper", "Failed to follow iframe $absoluteUrl", e)
            }
        }

        PageScrapeResult(
            url = url,
            html = initial.html,
            iframes = allIframes.distinct(),
            videoUrls = allVideoUrls.distinct(),
        )
    }

    private fun tryExtractCookie(urlOrHost: String): Boolean {
        val host = if (urlOrHost.startsWith("http")) {
            URI(urlOrHost).host ?: return false
        } else {
            urlOrHost
        }

        val cookie = CookieManager.getInstance().getCookie(
            if (urlOrHost.startsWith("http")) urlOrHost else "https://$urlOrHost/"
        ) ?: return false

        return if (cookie.contains("cf_clearance")) {
            savedCookies[host] = parseCookieMap(cookie)
            Log.d("CloudflareKiller", "cf_clearance found for host: $host")
            true
        } else false
    }

    private fun shouldBlockResource(url: String): Boolean {
        val lower = url.lowercase()
        val blacklisted = listOf(
            ".jpg", ".png", ".webp", ".jpeg", ".webm", ".mp4", ".mp3",
            ".gifv", ".flv", ".asf", ".mov", ".mng", ".mkv", ".ogg", ".avi",
            ".wav", ".woff2", ".woff", ".ttf", ".css", ".vtt", ".srt", ".ts",
            ".gif", "wss://", ".ico",
        )
        return blacklisted.any { lower.contains(it) }
    }
}

private fun extractIframes(html: String): List<String> {
    val iframeRegex = Regex(
        """<iframe[^>]*src\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    return iframeRegex.findAll(html).map {
        var src = it.groupValues[1]
        if (src.startsWith("//")) src = "https:$src"
        src
    }.distinct().toList()
}

private fun extractVideoUrlsFromHtml(html: String): List<String> {
    val urls = mutableListOf<String>()

    val patterns = listOf(
        Regex("""https?://[^"'\s<>]+\.(?:mp4|m3u8)[^"'\s<>]*""", RegexOption.IGNORE_CASE),
        Regex("""src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        Regex("""data-src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        Regex("""(?:file|url):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
    )

    for (pattern in patterns) {
        for (match in pattern.findAll(html)) {
            var url = match.groupValues[1].takeIf { it.isNotBlank() } ?: match.value
            if (url.startsWith("//")) url = "https:$url"
            if (url.startsWith("http") && !urls.contains(url)) urls.add(url)
        }
    }

    return urls.distinct()
}

private fun resolveUrl(href: String, baseUrl: String): String {
    if (href.startsWith("http://") || href.startsWith("https://")) return href
    if (href.startsWith("//")) return "https:$href"
    if (href.startsWith("/")) {
        val base = baseUrl.substringBefore("://").let { proto ->
            "$proto://${baseUrl.substringAfter("://").substringBefore("/")}"
        }
        return "$base$href"
    }
    val base = baseUrl.trimEnd('/')
    return if (href.startsWith("?")) "$base$href" else "$base/$href"
}

private fun parseCookieMap(cookie: String): Map<String, String> =
    cookie.split(";").associate {
        val split = it.split("=", limit = 2)
        (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
    }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
