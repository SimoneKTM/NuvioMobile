package com.nuvio.app.features.vezie

import com.nuvio.app.features.addons.AddonBehaviorHints
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.httpGetText
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

object VeezieAddonBridge {
    const val PLACEHOLDER_TRANSPORT_URL = "https://veezie.bridge.local"
    const val MANIFEST_URL = "$PLACEHOLDER_TRANSPORT_URL/manifest.json"

    val manifest: AddonManifest by lazy {
        AddonManifest(
            id = "com.nuvio.veezie.scraper",
            name = "Canali Veezie (Scraper)",
            description = "Cerca flussi video sui canali Veezie configurati. Risolve host video come Mixdrop, Supervideo, VOE direttamente nell'app.",
            version = "1.0.0",
            logoUrl = null,
            resources = listOf(
                AddonResource(name = "stream", types = listOf("movie", "series")),
            ),
            types = listOf("movie", "series"),
            idPrefixes = emptyList(),
            catalogs = emptyList(),
            behaviorHints = AddonBehaviorHints(configurable = true),
            transportUrl = PLACEHOLDER_TRANSPORT_URL,
        )
    }

    private val manifestJson: String by lazy {
        buildJsonObject {
            put("id", "com.nuvio.veezie.scraper")
            put("name", "Canali Veezie (Scraper)")
            put("description", "Cerca flussi video sui canali Veezie configurati. Risolve host video come Mixdrop, Supervideo, VOE direttamente nell'app.")
            put("version", "1.0.0")
            putJsonArray("resources") { add(JsonPrimitive("stream")) }
            putJsonArray("types") { add(JsonPrimitive("movie")); add(JsonPrimitive("series")) }
        }.toString()
    }

    fun register() {
        registerUrlInterceptor(::interceptHttpGetText)
    }

    fun getManifestJsonString(): String = manifestJson

    private suspend fun interceptHttpGetText(url: String): String? {
        val path = url.substringBefore("?").trimEnd('/')
        if (!path.startsWith(PLACEHOLDER_TRANSPORT_URL)) return null

        val resourcePath = path.removePrefix(PLACEHOLDER_TRANSPORT_URL).trimStart('/')
        return when {
            resourcePath == "manifest.json" -> manifestJson
            resourcePath.contains("/stream/") -> handleStreamLocal(path)
            else -> null
        }
    }

    private suspend fun handleStreamLocal(fullPath: String): String {
        val typeAndId = fullPath.substringAfter("/stream/").removeSuffix(".json")
        val slashIndex = typeAndId.indexOf('/')
        if (slashIndex < 0) return """{"streams":[]}"""

        val videoId = typeAndId.substring(slashIndex + 1)

        VeezieChannelRepository.initialize()
        val channels = VeezieChannelRepository.getEnabledChannels()
        if (channels.isEmpty()) return """{"streams":[]}"""

        val title = resolveTitle(videoId) ?: return """{"streams":[]}"""

        val resolvedStreams = mutableListOf<String>()
        for (channel in channels) {
            try {
                val urls = if (isHtmlSite(channel.url)) {
                    VeezieHtmlScraper.searchLinks(channel.url.trimEnd('/'), title)
                } else {
                    val channelData = VeezieChannelRepository.fetchChannelContent(channel)
                    if (channelData != null) searchContentLinks(channel, channelData, title) else emptyList()
                }
                resolvedStreams.addAll(urls)
            } catch (_: Exception) { }
        }

        if (resolvedStreams.isEmpty()) return """{"streams":[]}"""

        val streamEntries = buildJsonArray {
            var index = 0
            for (link in resolvedStreams.distinct()) {
                val resolvedUrl = VeezieHostResolver.resolveStreamUrl(link, title)
                if (resolvedUrl != null) {
                    index++
                    add(buildJsonObject {
                        put("url", resolvedUrl)
                        put("title", "Veezie #$index")
                        putJsonArray("sources") { add(JsonPrimitive("Veezie")) }
                        put("behaviorHints", buildJsonObject {
                            put("notWebReady", true)
                        })
                    })
                }
            }
        }

        return buildJsonObject {
            putJsonArray("streams") {
                streamEntries.forEach { add(it) }
            }
        }.toString()
    }

    private fun isHtmlSite(url: String): Boolean {
        val path = url.substringBefore("?").trimEnd('/')
        return !path.endsWith(".json") && !path.contains("/api/")
    }

    private suspend fun resolveTitle(videoId: String): String? {
        val cleanId = videoId.removePrefix("tmdb:").split(":").first().trim()
        if (cleanId.isBlank()) return null

        val apiKey = TmdbSettingsRepository.snapshot().apiKey
        if (apiKey.isBlank()) return null

        val isNumeric = cleanId.all { it.isDigit() }
        if (!isNumeric) return cleanId // assume it's already a title

        return try {
            val movieUrl = "https://api.themoviedb.org/3/movie/$cleanId?api_key=$apiKey&language=it"
            val response = httpGetText(movieUrl)
            val obj = Json.parseToJsonElement(response).jsonObject
            obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) {
            try {
                val tvUrl = "https://api.themoviedb.org/3/tv/$cleanId?api_key=$apiKey&language=it"
                val response = httpGetText(tvUrl)
                val obj = Json.parseToJsonElement(response).jsonObject
                obj["name"]?.jsonPrimitive?.contentOrNull
            } catch (_: Exception) { null }
        }
    }

    private suspend fun searchContentLinks(
        channel: VeezieChannelConfig,
        channelData: VeezieChannel,
        title: String,
    ): List<String> {
        val links = mutableListOf<String>()
        val titleLower = title.lowercase()

        val seriesUrl = channelData.series?.takeIf { it.isNotBlank() }
            ?: channel.seriesUrl
        if (seriesUrl != null) {
            try {
                val seriesList = VeezieChannelRepository.fetchSeriesList(seriesUrl)
                if (seriesList != null) {
                    for (item in seriesList.items) {
                        if (item.title.lowercase().contains(titleLower)) {
                            links.addAll(VeezieChannelRepository.parseLinks(item.url))
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        val moviesUrl = channelData.movies?.takeIf { it.isNotBlank() }
            ?: channel.moviesUrl
        if (moviesUrl != null) {
            try {
                val movieList = VeezieChannelRepository.fetchMovieList(moviesUrl)
                if (movieList != null) {
                    for (item in movieList.items) {
                        if (item.title.lowercase().contains(titleLower)) {
                            links.addAll(VeezieChannelRepository.parseLinks(item.links))
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        val otherUrl = channelData.other?.takeIf { it.isNotBlank() }
            ?: channel.otherUrl
        if (otherUrl != null) {
            try {
                val otherList = VeezieChannelRepository.fetchOtherList(otherUrl)
                if (otherList != null) {
                    for (item in otherList.items) {
                        if (item.title.lowercase().contains(titleLower)) {
                            links.addAll(VeezieChannelRepository.parseLinks(item.links))
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        return links
    }
}
