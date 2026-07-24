package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJson
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object AutoScraper : WebScraper {
    override val name = "AutoScraper"
    private val log = Logger.withTag("AutoScraper")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val browserHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language" to "it-IT,it;q=0.9",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    )

    override fun supports(url: String): Boolean = true

    override suspend fun searchLinks(
        siteUrl: String,
        title: String,
        season: Int?,
        episode: Int?,
    ): List<String> {
        val baseUrl = siteUrl.trimEnd('/')
        val config = SiteProber.getConfig(siteUrl)

        if (config.usesApi) {
            return searchViaApi(baseUrl, config, title, season, episode)
        }
        return searchViaHtml(baseUrl, config, title, season, episode)
    }

    private suspend fun searchViaHtml(
        baseUrl: String,
        config: SiteConfig,
        title: String,
        season: Int?,
        episode: Int?,
    ): List<String> {
        val searchQuery = if (season != null && episode != null) {
            "$title S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
        } else {
            title
        }

        val contentUrl = findContentUrl(baseUrl, config, searchQuery) ?: return emptyList()

        val pageHtml = try {
            httpGetTextWithHeaders(contentUrl, browserHeaders)
        } catch (_: Exception) { return emptyList() }

        val targetUrl = if (season != null && episode != null) {
            navigateToEpisode(contentUrl, pageHtml, season, episode)
        } else {
            contentUrl
        }

        val episodeHtml = if (targetUrl != contentUrl) {
            try { httpGetTextWithHeaders(targetUrl, browserHeaders) } catch (_: Exception) { pageHtml }
        } else {
            pageHtml
        }

        return extractVideoUrls(episodeHtml, baseUrl)
    }

    private suspend fun searchViaApi(
        baseUrl: String,
        config: SiteConfig,
        title: String,
        season: Int?,
        episode: Int?,
    ): List<String> {
        return try {
            val csrfToken = if (config.usesCsrf && config.csrfPattern != null) {
                val html = httpGetTextWithHeaders("$baseUrl/archivio", browserHeaders)
                Regex(config.csrfPattern, RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1) ?: ""
            } else ""

            val headers = mutableMapOf(
                "User-Agent" to browserHeaders["User-Agent"]!!,
                "Accept-Language" to browserHeaders["Accept-Language"]!!,
                "Content-Type" to "application/json",
            )
            if (csrfToken.isNotEmpty()) headers["X-CSRF-TOKEN"] = csrfToken

            val payload = buildApiPayload(config, title)
            val response = httpPostJsonWithHeaders(
                config.apiSearchUrl ?: "$baseUrl/archivio/get-animes",
                payload,
                headers,
            )

            val results = extractApiResults(response, config.apiResponsePath)
            val animeId = results.firstOrNull() ?: return emptyList()

            fetchEpisodeUrl(baseUrl, animeId, season, episode)
        } catch (e: Exception) {
            log.e(e) { "API search failed for $title" }
            emptyList()
        }
    }

    private suspend fun findContentUrl(
        baseUrl: String,
        config: SiteConfig,
        query: String,
    ): String? {
        val encodedQuery = query.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "+")

        for (candidate in config.searchUrlCandidates) {
            val searchUrl = baseUrl + candidate.replace("{query}", encodedQuery)
            try {
                val html = httpGetTextWithHeaders(searchUrl, browserHeaders)
                val link = extractContentLink(html, baseUrl, config.contentLinkPatterns)
                if (link != null) return link
            } catch (_: Exception) {}
        }

        for (fallbackUrl in listOf(
            "$baseUrl/?s=$encodedQuery",
            "$baseUrl/search/$encodedQuery",
        )) {
            try {
                val html = httpGetTextWithHeaders(fallbackUrl, browserHeaders)
                val link = extractContentLink(html, baseUrl, config.contentLinkPatterns)
                if (link != null) return link
            } catch (_: Exception) {}
        }

        return null
    }

    private fun extractContentLink(html: String, baseUrl: String, patterns: List<String>): String? {
        val hrefPatterns = patterns.mapNotNull { p ->
            try {
                Regex("""<a[^>]*href\s*=\s*["']([^"']+)["'][^>]*class\s*=\s*["'][^"']*${Regex.escape(p)}[^"']*["']""", RegexOption.IGNORE_CASE)
            } catch (_: Exception) { null }
        }

        val titlePatterns = listOf(
            Regex("""<h2[^>]*class\s*=\s*["'][^"']*(?:post-title|entry-title|title)[^"']*["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<h3[^>]*class\s*=\s*["'][^"']*(?:post-title|entry-title|title)[^"']*["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*class\s*=\s*["'][^"']*post-title[^"']*["']""", RegexOption.IGNORE_CASE),
            Regex("""<article[^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""entry-title"><a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']bookmark["']""", RegexOption.IGNORE_CASE),
        )

        val allPatterns = hrefPatterns + titlePatterns
        for (pattern in allPatterns) {
            val match = pattern.find(html)
            if (match != null) {
                val href = normalizeUrl(match.groupValues[1], baseUrl)
                if (href != baseUrl) return href
            }
        }

        val anyLinkRegex = Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val domain = baseUrl.substringAfter("://").substringBefore("/")
        for (match in anyLinkRegex.findAll(html)) {
            val href = normalizeUrl(match.groupValues[1], baseUrl)
            if (href.contains(domain) && href != baseUrl &&
                !href.contains("/page/") && !href.contains("?s=") && !href.contains("#respond")) {
                return href
            }
        }

        return null
    }

    private suspend fun navigateToEpisode(
        contentUrl: String,
        pageHtml: String,
        season: Int,
        episode: Int,
    ): String {
        val episodeStr = episode.toString().padStart(2, '0')
        val seasonStr = season.toString()

        val patterns = listOf(
            Regex("""<a[^>]*href\s*=\s*["']([^"']+)["'][^>]*>[^<]*$episodeStr[^<]*</a>""", RegexOption.IGNORE_CASE),
            Regex("""$episodeStr[^<]*</a>\s*</\w+>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<a[^>]*href\s*=\s*["']([^"']+/(?:$seasonStr/)?$episodeStr[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""<div[^>]*class\s*=\s*["'][^"']*episode[^"']*["'][^>]*data-episode\s*=\s*["']$episodeStr["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<div[^>]*class\s*=\s*["'][^"']*season[^"']*["'][^>]*data-season\s*=\s*["']$seasonStr["'][^>]*[\s\S]{0,2000}?$episodeStr[^<]*</a>\s*</\w+>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )

        for (pattern in patterns) {
            val match = pattern.find(pageHtml)
            if (match != null) {
                val url = normalizeUrl(match.groupValues[1], contentUrl)
                if (url != contentUrl) return url
            }
        }

        val seasonPattern = Regex("""<div[^>]*class\s*=\s*["'][^"']*season[^"']*["'][^>]*(?:data-season\s*=\s*["']$seasonStr["']|id\s*=\s*["'][^"']*$seasonStr[^"']*["'])""", RegexOption.IGNORE_CASE)
        val seasonMatch = seasonPattern.find(pageHtml)
        if (seasonMatch != null) {
            val afterSeason = pageHtml.substring(seasonMatch.range.last)
            val epLinkPattern = Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*>(?:[^<]*<[^>]*>)*[^<]*$episodeStr[^<]*</a>""", RegexOption.IGNORE_CASE)
            val epMatch = epLinkPattern.find(afterSeason)
            if (epMatch != null) {
                val url = normalizeUrl(epMatch.groupValues[1], contentUrl)
                if (url != contentUrl) return url
            }
        }

        return contentUrl
    }

    private fun extractVideoUrls(html: String, baseUrl: String): List<String> {
        val urls = mutableListOf<String>()

        val srcPatterns = listOf(
            Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<iframe[^>]*src\s*=\s*([^"'\s>]+)""", RegexOption.IGNORE_CASE),
            Regex("""data-src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""data-lazy-src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""src\s*=\s*["']([^"']*\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        )

        for (pattern in srcPatterns) {
            for (match in pattern.findAll(html)) {
                var url = match.groupValues[1]
                if (url.startsWith("//")) url = "https:$url"
                if (!url.startsWith("http")) url = normalizeUrl(url, baseUrl)
                urls.add(url)
            }
        }

        val hostPatterns = listOf(
            Regex("""mixdrop|supervideo|voe\.sx|streamtape|doodstream|uqload|vidmoly|clipwatching|cloudvideo|filemoon|streamwish|mp4upload|speedostream|kwik|mystream|mangoplayer|embedsito|dailymotion|youtube|yourupload|uptobox|streamhub|streamlocker|vixcloud|fileupload""", RegexOption.IGNORE_CASE),
        )
        val resolved = mutableSetOf<String>()
        for (url in urls) {
            for (pattern in hostPatterns) {
                if (pattern.containsMatchIn(url)) {
                    resolved.add(url)
                    break
                }
            }
        }

        return resolved.toList().ifEmpty {
            urls.filter { it.contains(baseUrl.substringAfter("://").substringBefore("/")) }
        }
    }

    private suspend fun fetchEpisodeUrl(
        baseUrl: String,
        animeFullId: String,
        season: Int?,
        episode: Int?,
    ): List<String> {
        return try {
            val html = httpGetTextWithHeaders("$baseUrl/anime/$animeFullId", browserHeaders)

            val videoPlayerMatch = Regex("""<video-player[^>]*episodes\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.DOT_MATCHES_ALL).find(html)
            val episodesData = videoPlayerMatch?.groupValues?.getOrNull(1)
                ?.replace("&quot;", "\"")?.replace("&#039;", "'") ?: "[]"

            val episodesArray = json.parseToJsonElement(episodesData).jsonArray

            val episodeId = if (episode != null && episode > 0) {
                episodesArray.firstOrNull { item ->
                    item.jsonObject["number"]?.jsonPrimitive?.contentOrNull == episode.toString()
                }?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
            } else null

            if (episodeId != null) {
                val embedHtml = httpGetTextWithHeaders("$baseUrl/embed-url/$episodeId", browserHeaders)
                val iframeSrc = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(embedHtml)?.groupValues?.getOrNull(1)
                if (iframeSrc != null) {
                    val resolved = VeezieEasyProxy.extractHostUrl("vixcloud", iframeSrc)
                        ?: VixcloudExtractor.extractPlaylistUrl(iframeSrc)
                        ?: iframeSrc
                    return listOf(resolved)
                }
            }

            if (episode == null || episode == 1) {
                val embedUrlMatch = Regex("""<video-player[^>]*embed_url\s*=\s*["']([^"']+)["']""").find(html)
                if (embedUrlMatch != null) {
                    val embedUrl = embedUrlMatch.groupValues[1].replace("&quot;", "\"")
                    val resolved = VeezieEasyProxy.extractHostUrl("vixcloud", embedUrl)
                        ?: VixcloudExtractor.extractPlaylistUrl(embedUrl)
                        ?: embedUrl
                    return listOf(resolved)
                }
            }

            emptyList()
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch episode URL for $animeFullId" }
            emptyList()
        }
    }

    private fun extractApiResults(response: String, responsePath: String?): List<String> {
        return try {
            val root = json.parseToJsonElement(response).jsonObject
            val records = if (responsePath != null) {
                val keys = responsePath.split(".")
                var current: kotlinx.serialization.json.JsonElement = root
                for (key in keys) {
                    current = current.jsonObject[key] ?: return emptyList()
                }
                current.jsonArray
            } else {
                root["records"]?.jsonArray ?: return emptyList()
            }

            records.mapNotNull { item ->
                val obj = item.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val slug = obj["slug"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                "$id-$slug"
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun buildApiPayload(config: SiteConfig, title: String): String {
        return when {
            config.apiContentType == "application/json" -> {
                kotlinx.serialization.json.buildJsonObject {
                    put("title", JsonPrimitive(title))
                    put("type", JsonPrimitive(false))
                    put("year", JsonPrimitive(false))
                    put("order", JsonPrimitive(false))
                    put("status", JsonPrimitive(false))
                    put("offset", JsonPrimitive(0))
                    put("dubbed", JsonPrimitive(false))
                    put("season", JsonPrimitive(false))
                    put("genres", JsonPrimitive(false))
                }.toString()
            }
            else -> """{"title":"$title"}"""
        }
    }

    private fun normalizeUrl(href: String, baseUrl: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        val base = baseUrl.trimEnd('/')
        return when {
            href.startsWith("/") -> "${base.substringBefore("://").let { proto -> "$proto://${base.substringAfter("://").substringBefore("/")}" }}$href"
            else -> "$base/$href"
        }
    }
}
