package com.nuvio.app.features.sora

import com.nuvio.app.features.addons.AddonBehaviorHints
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.registerUrlInterceptor
import com.nuvio.app.features.plugins.PluginRuntimeResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

object SoraAddonBridge {
    const val TRANSPORT_URL = "https://jm26.net"
    const val MANIFEST_URL = "$TRANSPORT_URL/manifest.json"

    val manifest: AddonManifest by lazy {
        AddonManifest(
            id = "com.sora.nuvioprovider",
            name = "Sora Anime",
            description = "Provider anime basato su Sora",
            version = "1.0.0",
            logoUrl = null,
            resources = listOf(
                AddonResource(name = "catalog", types = listOf("movie", "series", "anime")),
                AddonResource(name = "stream", types = listOf("movie", "series", "anime")),
            ),
            types = listOf("movie", "series", "anime"),
            idPrefixes = emptyList(),
            catalogs = listOf(
                AddonCatalog(type = "anime", id = "sora_anime_trends", name = "Sora Anime Trends"),
            ),
            behaviorHints = AddonBehaviorHints(configurable = false),
            transportUrl = TRANSPORT_URL,
        )
    }

    private val manifestJson: String by lazy {
        buildJsonObject {
            put("id", "com.sora.nuvioprovider")
            put("name", "Sora Anime")
            put("description", "Provider anime basato su Sora")
            put("version", "1.0.0")
            putJsonArray("resources") { add(JsonPrimitive("catalog")); add(JsonPrimitive("stream")) }
            putJsonArray("types") { add(JsonPrimitive("movie")); add(JsonPrimitive("series")); add(JsonPrimitive("anime")) }
            putJsonArray("catalogs") {
                add(buildJsonObject {
                    put("type", "anime")
                    put("id", "sora_anime_trends")
                    put("name", "Sora Anime Trends")
                })
            }
        }.toString()
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun register() {
        registerUrlInterceptor(::interceptHttpGetText)
    }

    fun getManifestJsonString(): String = manifestJson

    private suspend fun interceptHttpGetText(url: String): String? {
        val path = url.substringBefore("?").trimEnd('/')
        if (!path.startsWith(TRANSPORT_URL)) return null

        val resourcePath = path.removePrefix(TRANSPORT_URL).trimStart('/')
        return when {
            resourcePath == "manifest.json" -> manifestJson
            resourcePath.contains("/stream/") -> handleStreamRequest(path)
            resourcePath.contains("/catalog/") -> handleCatalogRequest(path)
            else -> null
        }
    }

    private suspend fun handleStreamRequest(fullPath: String): String {
        val typeAndId = fullPath.substringAfter("/stream/").removeSuffix(".json")
        val slashIndex = typeAndId.indexOf('/')
        if (slashIndex < 0) return """{"streams":[]}"""
        val addonType = typeAndId.substring(0, slashIndex)
        val encodedId = typeAndId.substring(slashIndex + 1)

        val (cleanId, season, episode) = parseStreamVideoId(encodedId)
        val soraType = addonTypeToSoraType(addonType)

        if (soraType == null) return """{"streams":[]}"""

        SoraPluginRepository.initialize()
        val modules = SoraPluginRepository.getEnabledModulesForType(soraType)
        if (modules.isEmpty()) return """{"streams":[]}"""

        val streamEntries = buildJsonArray {
            for (module in modules) {
                val results = try {
                    SoraPluginRepository.executeSoraModule(module, cleanId, soraType, season, episode)
                } catch (_: Exception) {
                    emptyList()
                }
                for (result in results) {
                    add(buildStreamEntry(result))
                }
            }
        }

        return buildJsonObject {
            putJsonArray("streams") {
                streamEntries.forEach { add(it) }
            }
        }.toString()
    }

    private fun buildStreamEntry(result: PluginRuntimeResult): JsonObject = buildJsonObject {
        put("url", result.url)
        val sourceName = result.provider ?: result.name ?: "Sora"
        val displayTitle = buildString {
            append(sourceName)
            result.quality?.let { append(" | "); append(it) }
            result.language?.let { append(" | "); append(it) }
        }
        put("title", displayTitle)
        putJsonArray("sources") { add(JsonPrimitive(sourceName)) }

        result.subtitles?.let { subs ->
            if (subs.isNotEmpty()) {
                putJsonArray("subtitles") {
                    for (sub in subs) {
                        add(buildJsonObject {
                            put("url", sub.url)
                            put("lang", sub.language)
                        })
                    }
                }
            }
        }

        put("behaviorHints", buildJsonObject {
            put("notWebReady", true)
        })
    }

    private suspend fun handleCatalogRequest(fullPath: String): String {
        val typeAndCatalog = fullPath.substringAfter("/catalog/").removeSuffix(".json")
        val slashIndex = typeAndCatalog.indexOf('/')
        if (slashIndex < 0) return """{"metas":[]}"""
        val addonType = typeAndCatalog.substring(0, slashIndex)
        val catalogId = typeAndCatalog.substring(slashIndex + 1)

        val soraType = addonTypeToSoraType(addonType) ?: return """{"metas":[]}"""

        SoraPluginRepository.initialize()

        val metasJson = buildJsonArray {
            val modules = SoraPluginRepository.getEnabledModulesForType(soraType)
            for (module in modules) {
                try {
                    val result = SoraPluginRepository.executeSoraModule(
                        module = module,
                        tmdbId = "550",
                        mediaType = soraType,
                        season = null,
                        episode = null,
                    )
                    for (stream in result) {
                        add(buildJsonObject {
                            put("id", stream.url)
                            put("type", addonType)
                            put("name", stream.title)
                            put("poster", "")
                            put("posterShape", "poster")
                            put("genres", buildJsonArray { add(JsonPrimitive("Anime")) })
                        })
                    }
                } catch (_: Exception) { }
            }
        }

        return buildJsonObject {
            putJsonArray("metas") {
                metasJson.forEach { add(it) }
            }
        }.toString()
    }

    private fun parseStreamVideoId(encodedId: String): Triple<String, Int?, Int?> {
        val parts = encodedId.split(":")
        return when (parts.size) {
            1 -> Triple(parts[0], null, null)
            3 -> Triple(parts[0], parts[1].toIntOrNull(), parts[2].toIntOrNull())
            else -> Triple(encodedId, null, null)
        }
    }

    private fun addonTypeToSoraType(addonType: String): String? = when (addonType.lowercase()) {
        "movie" -> "movie"
        "series", "tv" -> "tv"
        "anime" -> "tv"
        else -> null
    }
}
