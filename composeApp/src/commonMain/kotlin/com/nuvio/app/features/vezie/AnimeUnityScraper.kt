package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal object AnimeUnityScraper : WebScraper {
    private val log = Logger.withTag("AnimeUnityScraper")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var savedCookieHeader = ""
    private var savedCsrfToken = ""

    override val name = "AnimeUnity"

    override fun supports(url: String): Boolean {
        val host = url.substringAfter("://").substringBefore("/").substringBefore(":")
        return host.contains("animeunity", ignoreCase = true)
    }

    override suspend fun searchLinks(
        siteUrl: String,
        title: String,
        season: Int?,
        episode: Int?,
    ): List<String> {
        val baseUrl = siteUrl.trimEnd('/')

        val animeInfo = searchAnime(baseUrl, title) ?: return emptyList()
        val animeId = animeInfo["id"]?.jsonPrimitive?.contentOrNull ?: return emptyList()
        val slug = animeInfo["slug"]?.jsonPrimitive?.contentOrNull ?: return emptyList()
        val animeFullId = "$animeId-$slug"

        val episodeId = findEpisodeId(baseUrl, animeFullId, animeId, episode) ?: return emptyList()

        val embedUrl = getEmbedUrl(baseUrl, episodeId) ?: return emptyList()

        val resolvedUrl = VeezieEasyProxy.extractHostUrl("vixcloud", embedUrl)
            ?: VixcloudExtractor.extractPlaylistUrl(embedUrl)
            ?: embedUrl

        return listOf(resolvedUrl)
    }

    private suspend fun ensureSession(baseUrl: String) {
        if (savedCsrfToken.isNotEmpty()) return
        try {
            val html = httpGetTextWithHeaders(
                "$baseUrl/archivio",
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept-Language" to "it-IT,it;q=0.9",
                ),
            )
            val csrfRegex = Regex("""<meta\s+name\s*=\s*["']csrf-token["'][^>]*content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            savedCsrfToken = csrfRegex.find(html)?.groupValues?.getOrNull(1) ?: ""

            val cookieRegex = Regex("""Set-Cookie:\s*([^;\r\n]+)""", RegexOption.IGNORE_CASE)
            savedCookieHeader = cookieRegex.findAll(html)
                .map { it.groupValues[1] }
                .joinToString("; ")
        } catch (e: Exception) {
            log.e(e) { "Failed to initialize AnimeUnity session" }
        }
    }

    private suspend fun searchAnime(baseUrl: String, title: String): JsonObject? {
        return try {
            ensureSession(baseUrl)

            val payload = buildJsonObject {
                put("title", JsonPrimitive(title))
                put("type", JsonPrimitive(false))
                put("year", JsonPrimitive(false))
                put("order", JsonPrimitive(false))
                put("status", JsonPrimitive(false))
                put("offset", JsonPrimitive(0))
                put("dubbed", JsonPrimitive(false))
                put("season", JsonPrimitive(false))
                put("genres", JsonPrimitive(false))
            }

            val headers = mutableMapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Accept-Language" to "it-IT,it;q=0.9",
                "Content-Type" to "application/json",
            )
            if (savedCsrfToken.isNotEmpty()) headers["X-CSRF-TOKEN"] = savedCsrfToken
            if (savedCookieHeader.isNotEmpty()) headers["Cookie"] = savedCookieHeader

            val response = httpPostJsonWithHeaders("$baseUrl/archivio/get-animes", payload.toString(), headers)

            val obj = json.parseToJsonElement(response).jsonObject
            val records = obj["records"]?.jsonArray ?: return null
            if (records.isEmpty()) return null

            records[0].jsonObject
        } catch (e: Exception) {
            log.e(e) { "AnimeUnity search failed for $title" }
            null
        }
    }

    private suspend fun findEpisodeId(
        baseUrl: String,
        animeFullId: String,
        animeId: String,
        episode: Int?,
    ): String? {
        return try {
            val html = httpGetTextWithHeaders(
                "$baseUrl/anime/$animeFullId",
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept-Language" to "it-IT,it;q=0.9",
                ),
            )

            val videoPlayerRegex = Regex(
                """<video-player[^>]*episodes\s*=\s*["']([^"']+)["'][^>]*>""",
                RegexOption.DOT_MATCHES_ALL,
            )
            val videoPlayerMatch = videoPlayerRegex.find(html)
            if (videoPlayerMatch == null) return null

            val encodedEpisodes = videoPlayerMatch.groupValues[1]
            val decodedEpisodes = decodeHtmlEntities(encodedEpisodes)
            val episodesArray = json.parseToJsonElement(decodedEpisodes).jsonArray

            if (episode != null && episode > 0) {
                for (item in episodesArray) {
                    val epObj = item.jsonObject
                    val epNumber = epObj["number"]?.jsonPrimitive?.contentOrNull
                    if (epNumber == episode.toString()) {
                        return epObj["id"]?.jsonPrimitive?.contentOrNull
                    }
                }
            }

            if (episode == null || episode == 1) {
                val embedUrlMatch = Regex(
                    """<video-player[^>]*embed_url\s*=\s*["']([^"']+)["']""",
                ).find(html)
                if (embedUrlMatch != null) {
                    val embedUrl = decodeHtmlEntities(embedUrlMatch.groupValues[1])
                    return "embed:$embedUrl"
                }
            }

            if (episodesArray.isNotEmpty()) {
                return episodesArray[0].jsonObject["id"]?.jsonPrimitive?.contentOrNull
            }

            null
        } catch (e: Exception) {
            log.e(e) { "Failed to get episodes for $animeFullId" }
            null
        }
    }

    private suspend fun getEmbedUrl(baseUrl: String, episodeId: String): String? {
        if (episodeId.startsWith("embed:")) {
            return episodeId.removePrefix("embed:")
        }
        return try {
            val html = httpGetTextWithHeaders(
                "$baseUrl/embed-url/$episodeId",
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept-Language" to "it-IT,it;q=0.9",
                ),
            )

            val iframeRegex = Regex(
                """<iframe[^>]*src\s*=\s*["']([^"']+)["']""",
                RegexOption.IGNORE_CASE,
            )
            iframeRegex.find(html)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            log.e(e) { "Failed to get embed URL for $episodeId" }
            null
        }
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
