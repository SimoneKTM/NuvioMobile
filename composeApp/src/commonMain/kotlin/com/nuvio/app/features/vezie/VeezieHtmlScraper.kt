package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger

internal object VeezieHtmlScraper : WebScraper {
    override val name = "VeezieHtmlScraper"

    override fun supports(url: String): Boolean = true
    private val log = Logger.withTag("VeezieHtmlScraper")

    override suspend fun searchLinks(
        siteUrl: String,
        title: String,
        season: Int?,
        episode: Int?,
    ): List<String> {
        return try {
            val searchTitle = if (season != null && episode != null) {
                "$title S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
            } else {
                title
            }
            val contentUrl = searchOnSite(siteUrl, searchTitle) ?: return emptyList()
            if (season != null && episode != null) {
                extractEpisodeHostUrls(contentUrl, season, episode)
            } else {
                extractHostUrls(contentUrl) ?: emptyList()
            }
        } catch (e: Exception) {
            log.e(e) { "VeezieHtmlScraper failed for $siteUrl" }
            emptyList()
        }
    }

    private suspend fun searchOnSite(siteUrl: String, query: String): String? {
        val baseUrl = siteUrl.trimEnd('/')
        val searchUrl = "$baseUrl/?s=${encodeUrl(query)}"

        val html = try {
            VeezieEasyProxy.httpGetViaProxy(searchUrl)
        } catch (_: Exception) {
            val searchUrlAlt = "$baseUrl/search/${encodeUrl(query)}"
            try { VeezieEasyProxy.httpGetViaProxy(searchUrlAlt) } catch (_: Exception) { return null }
        }

        return extractContentLink(html, baseUrl)
    }

    private fun extractContentLink(html: String, baseUrl: String): String? {
        val patterns = listOf(
            Regex("""<h2[^>]*class\s*=\s*["'][^"']*post-title[^"']*["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<h3[^>]*class\s*=\s*["'][^"']*post-title[^"']*["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*class\s*=\s*["'][^"']*post-title[^"']*["']""", RegexOption.IGNORE_CASE),
            Regex("""<article[^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<div\s+class\s*=\s*["'][^"']*post[^"']*["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*>\s*<img[^>]*class\s*=\s*["'][^"']*attachment[^"']*["']""", RegexOption.IGNORE_CASE),
            Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']bookmark["']""", RegexOption.IGNORE_CASE),
            Regex("""entry-title"><a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )

        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                var href = match.groupValues[1]
                href = normalizeUrl(href, baseUrl)
                return href
            }
        }

        val anyLinkRegex = Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val links = anyLinkRegex.findAll(html).map { it.groupValues[1] }.toList()
        for (link in links) {
            if (link.contains(baseUrl.substringAfter("://").substringBefore("/"))) {
                val clean = normalizeUrl(link, baseUrl)
                if (clean != baseUrl && !clean.contains("/page/") && !clean.contains("?s=") && !clean.contains("#respond")) {
                    return clean
                }
            }
        }

        return null
    }

    private suspend fun extractHostUrls(contentUrl: String): List<String>? {
        val html = try {
            VeezieEasyProxy.httpGetViaProxy(contentUrl)
        } catch (_: Exception) { return null }

        val hostPatterns = listOf(
            Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<iframe[^>]*src\s*=\s*([^"'\s>]+)""", RegexOption.IGNORE_CASE),
            Regex("""<a\s+href\s*=\s*["']([^"']*(?:mixdrop|supervideo|voe\.sx|streamtape|doodstream|embedsito)[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""data-src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""data-lazy-src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )

        val hosts = mutableListOf<String>()

        for (pattern in hostPatterns) {
            for (match in pattern.findAll(html)) {
                var url = match.groupValues[1]
                if (url.startsWith("//")) url = "https:$url"
                if (!url.startsWith("http")) url = normalizeUrl(url, contentUrl)
                if (isVideoHost(url)) {
                    hosts.add(url)
                }
            }
        }

        if (hosts.isEmpty()) {
            val allSrc = Regex("""src\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                .findAll(html).map { it.groupValues[1] }.toList()
            for (src in allSrc) {
                val url = if (src.startsWith("//")) "https:$src" else if (src.startsWith("http")) src else normalizeUrl(src, contentUrl)
                if (isVideoHost(url)) hosts.add(url)
            }
        }

        return hosts.ifEmpty { null }
    }

    private fun isVideoHost(url: String): Boolean {
        val hosts = listOf(
            "mixdrop", "supervideo", "voe.sx", "streamtape",
            "doodstream", "dood.", "uqload", "vidmoly",
            "clipwatching", "cloudvideo", "filemoon", "file-moon",
            "streamwish", "wish.", "mp4upload", "speedostream",
            "kwik", "kwik.cx", "mystream", "mystream.",
            "mangoplayer", "mango.", "embedsito",
            "dailymotion", "youtube", "youtu.be",
            "yourupload", "uptobox", "streamhub",
            "streamlocker", "vixcloud", "fileupload",
        )
        return hosts.any { url.contains(it, ignoreCase = true) }
    }

    private suspend fun extractEpisodeHostUrls(contentUrl: String, season: Int, episode: Int): List<String> {
        val html = try {
            VeezieEasyProxy.httpGetViaProxy(contentUrl)
        } catch (_: Exception) { return emptyList() }

        val baseUrl = contentUrl.substringBeforeLast("/")
        val seasonStr = season.toString()
        val episodeStr = episode.toString().padStart(2, '0')

        val episodePatterns = listOf(
            Regex("""<a[^>]*href\s*=\s*["']([^"']+)["'][^>]*>[^<]*(?:Episodio|Episode|E|)[^\d]*$episodeStr[^<]*</a>""", RegexOption.IGNORE_CASE),
            Regex("""$episodeStr[^<]*</a>\s*</\w+>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<a[^>]*href\s*=\s*["']([^"']+/(?:$seasonStr/)?$episodeStr[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""<a[^>]*href\s*=\s*["']([^"']+(?:episode|ep|e|p)[^"']*$episodeStr[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""<div[^>]*class\s*=\s*["'][^"']*episode[^"']*["'][^>]*data-episode\s*=\s*["']$episodeStr["'][^>]*data-season\s*=\s*["']$seasonStr["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )

        for (pattern in episodePatterns) {
            val match = pattern.find(html)
            if (match != null) {
                var episodeUrl = match.groupValues[1]
                episodeUrl = normalizeUrl(episodeUrl, baseUrl)
                val hosts = extractHostUrls(episodeUrl)
                if (hosts != null && hosts.isNotEmpty()) return hosts
            }
        }

        val seasonPattern = Regex(
            """<div[^>]*class\s*=\s*["'][^"']*season[^"']*["'][^>]*(?:data-season\s*=\s*["']$seasonStr["']|id\s*=\s*["'][^"']*$seasonStr[^"']*["'])""",
            RegexOption.IGNORE_CASE,
        )
        val seasonMatch = seasonPattern.find(html)
        if (seasonMatch != null) {
            val fromSeason = html.substring(seasonMatch.range.last)
            val episodeLinkPattern = Regex(
                """<a\s+href\s*=\s*["']([^"']+)["'][^>]*>(?:[^<]*<[^>]*>)*[^<]*$episodeStr[^<]*</a>""",
                RegexOption.IGNORE_CASE,
            )
            val episodeMatch = episodeLinkPattern.find(fromSeason)
            if (episodeMatch != null) {
                var episodeUrl = episodeMatch.groupValues[1]
                episodeUrl = normalizeUrl(episodeUrl, baseUrl)
                val hosts = extractHostUrls(episodeUrl)
                if (hosts != null && hosts.isNotEmpty()) return hosts
            }
        }

        return extractHostUrls(contentUrl) ?: emptyList()
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

    private fun encodeUrl(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "+")
    }
}
