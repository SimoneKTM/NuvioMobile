package com.nuvio.app.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

actual object CloudflareSolver {
    private val savedCookies = ConcurrentHashMap<String, Map<String, String>>()
    @Volatile
    private var webViewUserAgent: String? = null
    private var context: Context? = null
    private const val scraperTag = "WebViewScraper"

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

    actual suspend fun scrapePage(
        url: String,
        jsRenderDelayMs: Long,
        timeoutMs: Long,
    ): PageScrapeResult? = withContext(Dispatchers.Main) {
        val ctx = context ?: return@withContext null
        var webView: WebView? = null
        val deferred = CompletableDeferred<PageScrapeResult?>()

        try {
            webView = WebView(ctx.applicationContext).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = false
                settings.blockNetworkImage = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

                if (webViewUserAgent == null) webViewUserAgent = settings.userAgentString

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        val pageUrl = finishedUrl ?: url
                        view?.postDelayed({
                            view.evaluateJavascript(
                                """(function() {
                                    try {
                                        var iframes = [];
                                        try {
                                            var tags = document.querySelectorAll('iframe');
                                            for (var i = 0; i < tags.length; i++) {
                                                var src = tags[i].src || tags[i].getAttribute('src') || '';
                                                if (src) iframes.push(src);
                                            }
                                        } catch(e) {}

                                        var videos = [];
                                        try {
                                            var vids = document.querySelectorAll('video');
                                            for (var i = 0; i < vids.length; i++) {
                                                var src = vids[i].src || vids[i].currentSrc || '';
                                                if (src) videos.push(src);
                                                var sources = vids[i].querySelectorAll('source');
                                                for (var j = 0; j < sources.length; j++) {
                                                    if (sources[j].src) videos.push(sources[j].src);
                                                }
                                            }
                                        } catch(e) {}

                                        var scripts = [];
                                        try {
                                            var scs = document.querySelectorAll('script');
                                            for (var i = 0; i < scs.length; i++) {
                                                var t = scs[i].textContent || '';
                                                if (t) scripts.push(t);
                                            }
                                        } catch(e) {}

                                        return JSON.stringify({
                                            title: document.title || '',
                                            html: document.documentElement.outerHTML || '',
                                            iframes: iframes,
                                            videos: videos,
                                            scripts: scripts.join('\n')
                                        });
                                    } catch(e) {
                                        return JSON.stringify({title:'', html:'', iframes:[], videos:[], scripts:'', error: e.message});
                                    }
                                })()""".trimIndent()
                            ) { json ->
                                if (json != null && json != "null" && json.isNotEmpty()) {
                                    deferred.complete(parseJsResult(pageUrl, json))
                                } else {
                                    deferred.complete(null)
                                }
                            }
                        }, jsRenderDelayMs)
                    }
                }

                loadUrl(url)
            }

            withTimeout(timeoutMs) {
                deferred.await()
            }
        } catch (e: Exception) {
            Log.e(scraperTag, "Scrape failed for $url: ${e.message}")
            if (!deferred.isCompleted) deferred.complete(null)
            null
        } finally {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    actual suspend fun scrapePageWithIframeFollow(
        url: String,
        maxDepth: Int,
        jsRenderDelayMs: Long,
    ): PageScrapeResult? {
        var currentUrl = url
        var depth = 0
        var lastResult: PageScrapeResult? = null

        while (depth < maxDepth) {
            Log.d(scraperTag, "Scrape depth $depth: $currentUrl")
            val result = scrapePage(currentUrl, jsRenderDelayMs) ?: break
            lastResult = result

            if (result.hasVideo) {
                Log.d(scraperTag, "Video found at depth $depth: ${result.videoUrls}")
                return result
            }

            val iframeUrl = result.firstIframe ?: break
            if (iframeUrl == currentUrl || iframeUrl.isBlank()) break

            currentUrl = normalizeUrl(iframeUrl, currentUrl)
            depth++
        }

        return lastResult
    }

    private fun parseJsResult(pageUrl: String, rawJson: String): PageScrapeResult? {
        return try {
            val json = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                JSONObject(rawJson.drop(1).dropLast(1).replace("\\\"", "\""))
            } else {
                JSONObject(rawJson)
            }

            val title = json.optString("title", "")
            val html = json.optString("html", "")
            val iframes = toList(json.optJSONArray("iframes"))
            val videos = toList(json.optJSONArray("videos"))
            val scripts = json.optString("scripts", "")

            val allVideoUrls = extractVideoUrlsFromHtml(html, pageUrl) + videos

            PageScrapeResult(
                originalUrl = pageUrl,
                finalUrl = pageUrl,
                pageTitle = title,
                pageHtml = html,
                iframes = iframes,
                videoSources = videos,
                videoUrls = allVideoUrls.distinct(),
                scriptContents = scripts,
            )
        } catch (e: Exception) {
            Log.e(scraperTag, "Failed to parse JS result: ${e.message}")
            null
        }
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

    private fun toList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val s = arr.optString(i, "").trim()
            s.takeIf { it.isNotBlank() }
        }
    }

    private fun extractVideoUrlsFromHtml(html: String, baseUrl: String): List<String> {
        val urls = mutableListOf<String>()
        val patterns = listOf(
            Regex("""https?://[^"'\s<>]+\.(?:m3u8|mp4)[^"'\s<>]*""", RegexOption.IGNORE_CASE),
            Regex("""src=["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""data-src=["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""(?:file|url):\s*["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""["']([^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            for (match in pattern.findAll(html)) {
                var videoUrl = match.groupValues[1]
                if (videoUrl.startsWith("//")) videoUrl = "https:$videoUrl"
                else if (!videoUrl.startsWith("http")) videoUrl = normalizeUrl(videoUrl, baseUrl)
                urls.add(videoUrl)
            }
        }
        return urls.distinct()
    }

    private fun normalizeUrl(href: String, baseUrl: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        val base = baseUrl.trimEnd('/')
        return when {
            href.startsWith("/") -> {
                val uri = URI(base)
                "${uri.scheme}://${uri.host}${if (uri.port > 0 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""}$href"
            }
            else -> "$base/$href"
        }
    }
}

private fun parseCookieMap(cookie: String): Map<String, String> =
    cookie.split(";").associate {
        val split = it.split("=", limit = 2)
        (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
    }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
