package com.nuvio.app.core.network

import kotlin.concurrent.AtomicReference
import kotlin.concurrent.Volatile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
actual object CloudflareSolver {
    private val savedCookies = AtomicReference<Map<String, Map<String, String>>>(emptyMap())
    @Volatile
    private var webViewUserAgent: String? = null

    actual fun getWebViewUserAgent(): String? = webViewUserAgent

    actual fun getCookies(host: String): Map<String, String> =
        savedCookies.value[host] ?: emptyMap()

    actual fun clear() {
        savedCookies.value = emptyMap()
    }

    actual suspend fun solve(url: String): Boolean = withContext(Dispatchers.Main) {
        try {
            val config = WKWebViewConfiguration()
            val webView = WKWebView(
                frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 0.0, 0.0),
                configuration = config,
            )

            getUserAgent(webView)

            val nsUrl = NSURL(string = url) ?: return@withContext false
            val request = NSURLRequest(
                nsUrl,
                cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
                timeoutInterval = 60.0,
            )
            webView.loadRequest(request)

            withTimeout(60_000L) {
                while (true) {
                    val cookieString = evaluateJs(webView, "document.cookie") ?: ""
                    val host = extractHost(url)
                    if (cookieString.contains("cf_clearance")) {
                        while (true) {
                            val current = savedCookies.value
                            val updated = current + (host to parseCookieMap(cookieString))
                            if (savedCookies.compareAndSet(current, updated)) break
                        }
                        break
                    }
                    delay(500)
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun getUserAgent(webView: WKWebView) {
        evaluateJs(webView, "navigator.userAgent")?.let {
            webViewUserAgent = it
        }
    }

    private suspend fun evaluateJs(webView: WKWebView, script: String): String? =
        suspendCoroutine { cont ->
            webView.evaluateJavaScript(script) { result, error ->
                if (error != null) {
                    cont.resume(null)
                } else {
                    cont.resume(result as? String)
                }
            }
        }
}

private fun extractHost(url: String): String =
    url.removePrefix("https://").removePrefix("http://").substringBefore("/").substringBefore(":")

private fun parseCookieMap(cookie: String): Map<String, String> =
    cookie.split(";").associate {
        val split = it.split("=", limit = 2)
        (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
    }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
