package com.nuvio.app.features.vezie

import com.nuvio.app.core.network.PageScrapeResult
import com.nuvio.app.features.addons.AddonBehaviorHints
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.registerUrlInterceptor
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

object EasyProxyAddonBridge {
    const val BRIDGE_PREFIX = "https://easyproxy.bridge.local"

    data class ScraperEntry(
        val id: String,
        val websiteUrl: String,
        val name: String,
    )

    private val scrapers = mutableMapOf<String, ScraperEntry>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var registered = false

    fun register() {
        if (registered) return
        registered = true
        registerUrlInterceptor(::interceptHttpGetText)
    }

    fun reloadFromManifestUrls(urls: List<String>) {
        for (url in urls) {
            if (url.startsWith(BRIDGE_PREFIX)) {
                val path = url.removePrefix(BRIDGE_PREFIX).trimStart('/')
                val id = extractIdFromPath(path)
                if (id != null && !scrapers.containsKey(id)) {
                    val websiteUrl = idToWebsiteUrl(id)
                    val name = websiteUrl
                        .removePrefix("https://").removePrefix("http://")
                        .trimEnd('/').substringBefore("/").substringBefore("?")
                    scrapers[id] = ScraperEntry(id = id, websiteUrl = websiteUrl, name = name)
                }
            }
        }
    }

    fun addOrUpdateScraper(websiteUrl: String): String {
        val id = websiteUrlToId(websiteUrl)
        val name = websiteUrl
            .removePrefix("https://").removePrefix("http://")
            .trimEnd('/').substringBefore("/")
            .substringBefore("?")
        scrapers[id] = ScraperEntry(id = id, websiteUrl = websiteUrl.trimEnd('/'), name = name)
        return buildManifestUrl(id)
    }

    fun removeScraper(id: String) {
        scrapers.remove(id)
    }

    fun getScrapers(): List<ScraperEntry> = scrapers.values.toList()

    fun getScraper(id: String): ScraperEntry? = scrapers[id]

    fun getManifestUrlFor(websiteUrl: String): String {
        val id = websiteUrlToId(websiteUrl)
        return buildManifestUrl(id)
    }

    private fun websiteUrlToId(websiteUrl: String): String {
        val clean = websiteUrl.trimEnd('/').removePrefix("https://").removePrefix("http://")
        val safeId = clean.lowercase()
            .replace(Regex("[^a-z0-9.\\-]"), "")
            .replace(".", "_")
        return "web_$safeId"
    }

    private fun idToWebsiteUrl(id: String): String {
        val domain = id.removePrefix("web_").replace("_", ".")
        return "https://$domain"
    }

    private fun buildManifestUrl(id: String): String {
        return "$BRIDGE_PREFIX/$id/manifest.json"
    }

    private suspend fun interceptHttpGetText(url: String): String? {
        if (!url.startsWith(BRIDGE_PREFIX)) return null
        val path = url.substringBefore("?").trimEnd('/')
        val resourcePath = path.removePrefix(BRIDGE_PREFIX).trimStart('/')
        return when {
            resourcePath.endsWith("manifest.json") -> handleManifest(resourcePath)
            resourcePath.contains("/stream/") -> handleStreamRequest(path)
            resourcePath.contains("/catalog/") -> handleCatalogRequest(path)
            else -> null
        }
    }

    private fun extractIdFromPath(resourcePath: String): String? {
        val parts = resourcePath.split("/")
        return parts.firstOrNull { it.startsWith("web_") }
    }

    private fun handleManifest(resourcePath: String): String? {
        val id = extractIdFromPath(resourcePath) ?: return null
        val scraper = scrapers[id] ?: return null
        return buildManifestJson(scraper)
    }

    private suspend fun handleCatalogRequest(fullPath: String): String {
        val pathWithoutPrefix = fullPath.removePrefix(BRIDGE_PREFIX).trimStart('/')
        val id = extractIdFromPath(pathWithoutPrefix) ?: return """{"metas":[]}"""
        val scraper = scrapers[id] ?: return """{"metas":[]}"""

        val typeAndCatalog = pathWithoutPrefix.substringAfter("/catalog/").removeSuffix(".json")
        val searchQuery = if (typeAndCatalog.contains("/search=")) {
            typeAndCatalog.substringAfter("search=").trim()
        } else {
            null
        }

        if (searchQuery.isNullOrBlank()) return """{"metas":[]}"""

        val streamUrls = AutoScraper.searchLinks(scraper.websiteUrl, searchQuery, null, null)

        return buildJsonObject {
            putJsonArray("metas") {
                streamUrls.distinct().forEachIndexed { index, url ->
                    add(buildJsonObject {
                        put("id", "scraper:${scraper.id}:$index")
                        put("type", "movie")
                        put("name", "$searchQuery - Fonte #${index + 1}")
                        put("poster", "")
                        put("posterShape", "poster")
                    })
                }
            }
        }.toString()
    }

    private fun buildManifestJson(scraper: ScraperEntry): String {
        return buildJsonObject {
            put("id", "com.nuvio.easyproxy.$scraper.id")
            put("name", scraper.name)
            put("description", "Web scraper per ${scraper.websiteUrl} tramite EasyProxy")
            put("version", "1.0.0")
            putJsonArray("resources") {
                add(JsonPrimitive("catalog"))
                add(JsonPrimitive("stream"))
            }
            putJsonArray("types") { add(JsonPrimitive("movie")); add(JsonPrimitive("series")) }
            putJsonArray("catalogs") {
                add(buildJsonObject {
                    put("type", "movie")
                    put("id", "${scraper.id}_trending")
                    put("name", "${scraper.name} - Trending")
                })
                add(buildJsonObject {
                    put("type", "series")
                    put("id", "${scraper.id}_trending")
                    put("name", "${scraper.name} - Trending")
                })
            }
            put("behaviorHints", buildJsonObject {
                put("configurable", true)
            })
        }.toString()
    }

    val manifest: AddonManifest?
        get() {
            val scraper = scrapers.values.firstOrNull() ?: return null
            return AddonManifest(
                id = "com.nuvio.easyproxy.${scraper.id}",
                name = scraper.name,
                description = "Web scraper per ${scraper.websiteUrl} tramite EasyProxy",
                version = "1.0.0",
                logoUrl = null,
                resources = listOf(
                    AddonResource(name = "catalog", types = listOf("movie", "series")),
                    AddonResource(name = "stream", types = listOf("movie", "series")),
                ),
                types = listOf("movie", "series"),
                idPrefixes = emptyList(),
                catalogs = listOf(
                    AddonCatalog(type = "movie", id = "${scraper.id}_trending", name = "${scraper.name} - Trending"),
                    AddonCatalog(type = "series", id = "${scraper.id}_trending", name = "${scraper.name} - Trending"),
                ),
                behaviorHints = AddonBehaviorHints(configurable = true),
                transportUrl = "$BRIDGE_PREFIX/${scraper.id}",
            )
        }

    private suspend fun handleStreamRequest(fullPath: String): String {
        val pathWithoutPrefix = fullPath.removePrefix(BRIDGE_PREFIX).trimStart('/')
        val id = extractIdFromPath(pathWithoutPrefix) ?: return """{"streams":[]}"""
        val scraper = scrapers[id] ?: return """{"streams":[]}"""

        val typeAndId = pathWithoutPrefix.substringAfter("/stream/").removeSuffix(".json")
        val slashIndex = typeAndId.indexOf('/')
        if (slashIndex < 0) return """{"streams":[]}"""

        val videoId = decodeUrlEncodedString(typeAndId.substring(slashIndex + 1))
        val idAfterPrefix = videoId.substringAfter("tmdb:")
        val idParts = idAfterPrefix.split(":")
        val season = idParts.getOrNull(1)?.toIntOrNull()
        val episode = idParts.getOrNull(2)?.toIntOrNull()
        val title = resolveTitle(videoId) ?: return """{"streams":[]}"""

        val streamUrls = AutoScraper.searchLinks(scraper.websiteUrl, title, season, episode)

        if (streamUrls.isEmpty()) return """{"streams":[]}"""

        val streamEntries = buildJsonArray {
            var index = 0
            for (link in streamUrls.distinct()) {
                val resolvedUrl = VeezieHostResolver.resolveStreamUrl(link, title)
                index++
                add(buildJsonObject {
                    put("url", resolvedUrl ?: link)
                    put("title", "Scraper #$index - ${scraper.name}")
                    putJsonArray("sources") { add(JsonPrimitive("Scraper")) }
                    put("behaviorHints", buildJsonObject {
                        put("notWebReady", true)
                    })
                })
            }
        }

        return buildJsonObject {
            putJsonArray("streams") {
                streamEntries.forEach { add(it) }
            }
        }.toString()
    }

    fun isCloudflareSite(url: String): Boolean {
        val host = url.removePrefix("https://").removePrefix("http://")
            .substringBefore("/").substringBefore(":").lowercase()
        return host.contains("cloudflare") || host.contains("watchluna") ||
               host.contains("streaming") || host.contains("altadefinizione")
    }

    suspend fun buildCloudflareUrl(
        tmdbId: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): PageScrapeResult? {
        val title = resolveTitle("tmdb:$tmdbId:$season:$episode") ?: return null

        val scrapers = getScrapers()
        for (entry in scrapers) {
            try {
                val links = AutoScraper.searchLinks(entry.websiteUrl, title, season, episode)
                if (links.isNotEmpty()) {
                    val allVideoUrls = mutableListOf<String>()
                    for (link in links) {
                        val resolved = VeezieHostResolver.resolveStreamUrl(link, title)
                        if (resolved != null) allVideoUrls.add(resolved)
                    }
                    if (allVideoUrls.isNotEmpty()) {
                        val followResult = PageFetcher.fetchWithIframes(entry.websiteUrl)
                        return PageScrapeResult(
                            url = entry.websiteUrl,
                            html = followResult?.html ?: "",
                            iframes = followResult?.iframes ?: emptyList(),
                            videoUrls = allVideoUrls,
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        for (entry in scrapers) {
            try {
                val result = PageFetcher.fetchWithIframes(entry.websiteUrl)
                if (result != null && result.videoUrls.isNotEmpty()) return result
            } catch (_: Exception) {}
        }

        return null
    }

    private fun decodeUrlEncodedString(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            when (val c = input[i]) {
                '%' -> {
                    if (i + 2 < input.length) {
                        val hex = input.substring(i + 1, i + 3)
                        sb.append(hex.toIntOrNull(16)?.toChar() ?: c)
                        i += 3
                    } else {
                        sb.append(c)
                        i++
                    }
                }
                '+' -> { sb.append(' '); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }

    private suspend fun resolveTitle(videoId: String): String? {
        val cleanId = videoId.removePrefix("tmdb:").split(":").first().trim()
        if (cleanId.isBlank()) return null

        val isNumeric = cleanId.all { it.isDigit() }
        if (!isNumeric) return cleanId

        val apiKey = TmdbSettingsRepository.snapshot().apiKey
        if (apiKey.isBlank()) return cleanId

        return try {
            val movieUrl = "https://api.themoviedb.org/3/movie/$cleanId?api_key=$apiKey&language=it"
            val response = com.nuvio.app.features.addons.httpGetText(movieUrl)
            val obj = Json.parseToJsonElement(response).jsonObject
            obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull ?: cleanId
        } catch (_: Exception) {
            try {
                val tvUrl = "https://api.themoviedb.org/3/tv/$cleanId?api_key=$apiKey&language=it"
                val response = com.nuvio.app.features.addons.httpGetText(tvUrl)
                val obj = Json.parseToJsonElement(response).jsonObject
                obj["name"]?.jsonPrimitive?.contentOrNull ?: cleanId
            } catch (_: Exception) { cleanId }
        }
    }
}
