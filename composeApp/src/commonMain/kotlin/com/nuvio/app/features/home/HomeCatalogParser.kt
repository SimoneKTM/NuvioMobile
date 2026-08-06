package com.nuvio.app.features.home

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object HomeCatalogParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parseCatalog(
        payload: String,
        maxItems: Int? = null,
    ): List<MetaPreview> {
        return parseCatalogResponse(
            payload = payload,
            maxItems = maxItems,
        ).items
    }

    fun parseCatalogResponse(
        payload: String,
        maxItems: Int? = null,
    ): ParsedCatalogResponse {
        val root = json.parseToJsonElement(payload).jsonObject
        val metas = root.array("metas")
        val parsedItems = buildList {
            val seenKeys = mutableSetOf<String>()
            metas.forEach { element ->
                if (maxItems != null && size >= maxItems) return@forEach

                val meta = element as? JsonObject ?: return@forEach
                val id = meta.string("id")
                val type = meta.string("type")
                val name = meta.string("name")

                if (id.isNullOrBlank() || type.isNullOrBlank() || name.isNullOrBlank()) {
                    return@forEach
                }

                val cleanedName = cleanTitle(name)

                val item = MetaPreview(
                    id = id,
                    type = type,
                    name = cleanedName,
                    poster = meta.string("poster"),
                    banner = meta.string("banner") ?: meta.string("background"),
                    logo = meta.string("logo"),
                    posterShape = meta.string("posterShape").toPosterShape(),
                    description = meta.string("description"),
                    releaseInfo = meta.string("releaseInfo"),
                    rawReleaseDate = meta.string("released"),
                    popularity = meta.string("popularity")?.toDoubleOrNull(),
                    imdbRating = meta.string("imdbRating"),
                    genres = meta.array("genres").mapNotNull { genre ->
                        genre.jsonPrimitive.contentOrNull?.takeIf { it.isNotBlank() }
                    },
                )
                if (seenKeys.add(item.stableKey())) {
                    add(item)
                }
            }
        }
        return ParsedCatalogResponse(
            items = parsedItems,
            rawItemCount = metas.size,
        )
    }

    internal fun cleanTitle(raw: String): String {
        val decoded = raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&nbsp;", " ")
        val trimmed = decoded.trim()
        val singleSpaced = trimmed.replace(Regex("\\s+"), " ")
        val cleaned = singleSpaced.replace(Regex("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]"), "")
        return cleaned.ifBlank { raw.trim() }
    }

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun String?.toPosterShape(): PosterShape =
        when (this?.lowercase()) {
            "square" -> PosterShape.Square
            "landscape" -> PosterShape.Landscape
            else -> PosterShape.Poster
        }
}

data class ParsedCatalogResponse(
    val items: List<MetaPreview>,
    val rawItemCount: Int,
)
