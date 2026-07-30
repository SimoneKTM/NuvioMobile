package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal object SiteProber {
    private val log = Logger.withTag("SiteProber")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = mutableMapOf<String, SiteConfig>()
    private val fetcher = PageFetcher

    suspend fun getConfig(url: String): SiteConfig {
        val domain = url.substringAfter("://").substringBefore("/").substringBefore(":").lowercase()
        cache[domain]?.let { return it }
        val config = probeSite(url)
        cache[domain] = config
        return config
    }

    fun clearCache() { cache.clear() }

    private suspend fun probeSite(url: String): SiteConfig {
        val baseUrl = url.trimEnd('/')
        val domain = baseUrl.substringAfter("://").substringBefore("/").substringBefore(":").lowercase()

        log.d { "Probing $domain..." }

        val homepageHtml = fetcher.fetch(baseUrl) ?: ""

        val animeUnityResult = checkAnimeUnityApi(baseUrl, domain)
        if (animeUnityResult != null) {
            log.d { "$domain uses AnimeUnity-style API" }
            return animeUnityResult
        }

        val inertiaResult = checkInertiaJs(homepageHtml)
        if (inertiaResult) {
            log.d { "$domain uses Inertia.js" }
        }

        val searchUrlCandidates = detectSearchUrl(baseUrl, homepageHtml)

        val contentPatterns = detectContentPatterns(homepageHtml)

        val csrf = detectCsrf(homepageHtml)

        val episodeType = detectEpisodeNavType(homepageHtml)

        return SiteConfig(
            domain = domain,
            searchUrlCandidates = searchUrlCandidates,
            contentLinkPatterns = contentPatterns,
            usesCsrf = csrf != null,
            csrfPattern = csrf,
            episodeNavType = episodeType,
        ).also { log.d { "Probed $domain: search=$searchUrlCandidates, patterns=${contentPatterns.size}" } }
    }

    private suspend fun checkAnimeUnityApi(baseUrl: String, domain: String): SiteConfig? {
        if (domain.contains("animeunity")) {
            return SiteConfig(
                domain = domain,
                usesApi = true,
                apiSearchUrl = "$baseUrl/archivio/get-animes",
                apiMethod = "POST",
                apiContentType = "application/json",
                apiResponsePath = "records",
                usesCsrf = true,
                csrfPattern = """<meta\s+name\s*=\s*["']csrf-token["'][^>]*content\s*=\s*["']([^"']+)["']""",
                episodeNavType = SiteConfig.EpisodeNavType.DATA_ATTRS,
            )
        }
        return try {
            val testPayload = """{"title":"test","type":false,"year":false,"order":false,"status":false,"offset":0,"dubbed":false,"season":false,"genres":false}"""
            val response = httpPostJson("$baseUrl/archivio/get-animes", testPayload)
            val obj = json.parseToJsonElement(response).jsonObject
            if (obj.containsKey("records")) {
                SiteConfig(
                    domain = domain,
                    usesApi = true,
                    apiSearchUrl = "$baseUrl/archivio/get-animes",
                    apiMethod = "POST",
                    apiContentType = "application/json",
                    apiResponsePath = "records",
                    usesCsrf = true,
                    csrfPattern = """<meta\s+name\s*=\s*["']csrf-token["'][^>]*content\s*=\s*["']([^"']+)["']""",
                    episodeNavType = SiteConfig.EpisodeNavType.DATA_ATTRS,
                )
            } else null
        } catch (_: Exception) { null }
    }

    private fun checkInertiaJs(html: String): Boolean {
        return html.contains("x-inertia", ignoreCase = true) ||
               html.contains("inertia", ignoreCase = true) ||
               html.contains("__inertia", ignoreCase = true)
    }

    private suspend fun detectSearchUrl(baseUrl: String, html: String): List<String> {
        val found = mutableListOf<String>()

        val formActions = Regex("""<form[^>]*action\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1] }.toList()

        for (action in formActions) {
            val clean = if (action.startsWith("/")) action else "/$action"
            if (clean.contains("search", ignoreCase = true) || clean.contains("cerca", ignoreCase = true) || clean.contains("s=")) {
                found.add("$clean{query}")
            }
        }

        if (found.isEmpty()) {
            for (candidate in listOf("/?s={query}", "/search/{query}", "/?search={query}", "/cerca/{query}")) {
                val testUrl = baseUrl + candidate.replace("{query}", "test")
                val testHtml = fetcher.fetch(testUrl) ?: continue
                if (testHtml.length > 500 && !testHtml.contains("404", ignoreCase = true) && !testHtml.contains("not found", ignoreCase = true)) {
                    found.add(candidate)
                    break
                }
            }
        }

        return found.ifEmpty { DEFAULT_SEARCH_CANDIDATES }
    }

    private fun detectContentPatterns(html: String): List<String> {
        val patterns = mutableListOf<String>()

        val classes = Regex("""class\s*=\s*["']([^"']*?)(?:post|entry|article|movie|item|card|title|link|thumbnail)(?:[^"']*?)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1] }.toSet()

        for (cls in classes) {
            val clean = cls.trim()
            if (clean.isNotEmpty() && clean.length < 50) {
                patterns.add(clean)
            }
        }

        if (html.contains("post-title", ignoreCase = true)) patterns.add("post-title")
        if (html.contains("entry-title", ignoreCase = true)) patterns.add("entry-title")
        if (html.contains("article", ignoreCase = true)) patterns.add("article")

        return patterns.ifEmpty { DEFAULT_LINK_PATTERNS }
    }

    private fun detectCsrf(html: String): String? {
        val patterns = listOf(
            """<meta\s+name\s*=\s*["']csrf-token["'][^>]*content\s*=\s*["']([^"']+)["']""",
            """<meta\s+name\s*=\s*["']csrf-token["'][^>]*value\s*=\s*["']([^"']+)["']""",
            """<input[^>]*name\s*=\s*["']_csrf_token["'][^>]*value\s*=\s*["']([^"']+)["']""",
            """<input[^>]*name\s*=\s*["']_token["'][^>]*value\s*=\s*["']([^"']+)["']""",
        )
        for (pattern in patterns) {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).find(html)
            if (match != null) return pattern
        }
        return null
    }

    private fun detectEpisodeNavType(html: String): SiteConfig.EpisodeNavType {
        if (html.contains("stagsione", ignoreCase = true) || html.contains("season", ignoreCase = true)) {
            if (html.contains("data-season", ignoreCase = true) || html.contains("data-episode", ignoreCase = true)) {
                return SiteConfig.EpisodeNavType.DATA_ATTRS
            }
            return SiteConfig.EpisodeNavType.TABBED
        }
        if (html.contains("video-player", ignoreCase = true)) {
            return SiteConfig.EpisodeNavType.DATA_ATTRS
        }
        return SiteConfig.EpisodeNavType.FLAT
    }
}
