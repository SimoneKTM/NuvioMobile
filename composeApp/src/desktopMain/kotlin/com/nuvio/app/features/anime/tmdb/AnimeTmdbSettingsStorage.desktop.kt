package com.nuvio.app.features.anime.tmdb

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object AnimeTmdbSettingsStorage {
    private val store = DesktopStorage.store("nuvio_anime_tmdb_settings")

    private fun bool(key: String): Boolean? =
        if (store.contains(key)) store.getBoolean(key) else null

    actual fun loadEnabled(): Boolean? = bool("anime_tmdb_enabled")

    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("anime_tmdb_enabled", enabled) }

    actual fun loadApiKey(): String? = store.getString("anime_tmdb_api_key")

    actual fun saveApiKey(apiKey: String) { store.putString("anime_tmdb_api_key", apiKey) }

    actual fun loadLanguage(): String? = store.getString("anime_tmdb_language")

    actual fun saveLanguage(language: String) { store.putString("anime_tmdb_language", language) }

    actual fun loadUseTrailers(): Boolean? = bool("anime_tmdb_use_trailers")

    actual fun saveUseTrailers(enabled: Boolean) { store.putBoolean("anime_tmdb_use_trailers", enabled) }

    actual fun loadUseArtwork(): Boolean? = bool("anime_tmdb_use_artwork")

    actual fun saveUseArtwork(enabled: Boolean) { store.putBoolean("anime_tmdb_use_artwork", enabled) }

    actual fun loadUseBasicInfo(): Boolean? = bool("anime_tmdb_use_basic_info")

    actual fun saveUseBasicInfo(enabled: Boolean) { store.putBoolean("anime_tmdb_use_basic_info", enabled) }

    actual fun loadUseDetails(): Boolean? = bool("anime_tmdb_use_details")

    actual fun saveUseDetails(enabled: Boolean) { store.putBoolean("anime_tmdb_use_details", enabled) }

    actual fun loadUseCredits(): Boolean? = bool("anime_tmdb_use_credits")

    actual fun saveUseCredits(enabled: Boolean) { store.putBoolean("anime_tmdb_use_credits", enabled) }

    actual fun loadUseProductions(): Boolean? = bool("anime_tmdb_use_productions")

    actual fun saveUseProductions(enabled: Boolean) { store.putBoolean("anime_tmdb_use_productions", enabled) }

    actual fun loadUseNetworks(): Boolean? = bool("anime_tmdb_use_networks")

    actual fun saveUseNetworks(enabled: Boolean) { store.putBoolean("anime_tmdb_use_networks", enabled) }

    actual fun loadUseEpisodes(): Boolean? = bool("anime_tmdb_use_episodes")

    actual fun saveUseEpisodes(enabled: Boolean) { store.putBoolean("anime_tmdb_use_episodes", enabled) }

    actual fun loadUseSeasonPosters(): Boolean? = bool("anime_tmdb_use_season_posters")

    actual fun saveUseSeasonPosters(enabled: Boolean) { store.putBoolean("anime_tmdb_use_season_posters", enabled) }

    actual fun loadUseMoreLikeThis(): Boolean? = bool("anime_tmdb_use_more_like_this")

    actual fun saveUseMoreLikeThis(enabled: Boolean) { store.putBoolean("anime_tmdb_use_more_like_this", enabled) }

    actual fun loadUseCollections(): Boolean? = bool("anime_tmdb_use_collections")

    actual fun saveUseCollections(enabled: Boolean) { store.putBoolean("anime_tmdb_use_collections", enabled) }

    private fun elem(key: String, value: Boolean): Pair<String, JsonElement> =
        key to JsonPrimitive(value)

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put("anime_tmdb_enabled", it) }
        loadApiKey()?.let { put("anime_tmdb_api_key", it) }
        loadLanguage()?.let { put("anime_tmdb_language", it) }
        loadUseTrailers()?.let { put("anime_tmdb_use_trailers", it) }
        loadUseArtwork()?.let { put("anime_tmdb_use_artwork", it) }
        loadUseBasicInfo()?.let { put("anime_tmdb_use_basic_info", it) }
        loadUseDetails()?.let { put("anime_tmdb_use_details", it) }
        loadUseCredits()?.let { put("anime_tmdb_use_credits", it) }
        loadUseProductions()?.let { put("anime_tmdb_use_productions", it) }
        loadUseNetworks()?.let { put("anime_tmdb_use_networks", it) }
        loadUseEpisodes()?.let { put("anime_tmdb_use_episodes", it) }
        loadUseSeasonPosters()?.let { put("anime_tmdb_use_season_posters", it) }
        loadUseMoreLikeThis()?.let { put("anime_tmdb_use_more_like_this", it) }
        loadUseCollections()?.let { put("anime_tmdb_use_collections", it) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        val keys = listOf(
            "anime_tmdb_enabled", "anime_tmdb_api_key", "anime_tmdb_language",
            "anime_tmdb_use_trailers", "anime_tmdb_use_artwork", "anime_tmdb_use_basic_info",
            "anime_tmdb_use_details", "anime_tmdb_use_credits", "anime_tmdb_use_productions",
            "anime_tmdb_use_networks", "anime_tmdb_use_episodes", "anime_tmdb_use_season_posters",
            "anime_tmdb_use_more_like_this", "anime_tmdb_use_collections",
        )
        keys.forEach { store.remove(it) }

        payload["anime_tmdb_enabled"]?.jsonPrimitive?.booleanOrNull?.let(::saveEnabled)
        payload["anime_tmdb_api_key"]?.jsonPrimitive?.contentOrNull?.let(::saveApiKey)
        payload["anime_tmdb_language"]?.jsonPrimitive?.contentOrNull?.let(::saveLanguage)
        payload["anime_tmdb_use_trailers"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseTrailers)
        payload["anime_tmdb_use_artwork"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseArtwork)
        payload["anime_tmdb_use_basic_info"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseBasicInfo)
        payload["anime_tmdb_use_details"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseDetails)
        payload["anime_tmdb_use_credits"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseCredits)
        payload["anime_tmdb_use_productions"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseProductions)
        payload["anime_tmdb_use_networks"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseNetworks)
        payload["anime_tmdb_use_episodes"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseEpisodes)
        payload["anime_tmdb_use_season_posters"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseSeasonPosters)
        payload["anime_tmdb_use_more_like_this"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseMoreLikeThis)
        payload["anime_tmdb_use_collections"]?.jsonPrimitive?.booleanOrNull?.let(::saveUseCollections)
    }
}
