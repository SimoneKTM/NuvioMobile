package com.nuvio.app.features.tvdb

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object TvdbSettingsStorage {
    private val store = DesktopStorage.store("nuvio_tvdb_settings")

    private fun bool(key: String): Boolean? =
        if (store.contains(key)) store.getBoolean(key) else null

    actual fun loadEnabled(): Boolean? = bool("tvdb_enabled")

    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("tvdb_enabled", enabled) }

    actual fun loadApiKey(): String? = store.getString("tvdb_api_key")

    actual fun saveApiKey(apiKey: String) { store.putString("tvdb_api_key", apiKey) }

    actual fun loadUseTrailers(): Boolean? = bool("tvdb_use_trailers")
    actual fun saveUseTrailers(enabled: Boolean) { store.putBoolean("tvdb_use_trailers", enabled) }
    actual fun loadUseArtwork(): Boolean? = bool("tvdb_use_artwork")
    actual fun saveUseArtwork(enabled: Boolean) { store.putBoolean("tvdb_use_artwork", enabled) }
    actual fun loadUseBasicInfo(): Boolean? = bool("tvdb_use_basic_info")
    actual fun saveUseBasicInfo(enabled: Boolean) { store.putBoolean("tvdb_use_basic_info", enabled) }
    actual fun loadUseCredits(): Boolean? = bool("tvdb_use_credits")
    actual fun saveUseCredits(enabled: Boolean) { store.putBoolean("tvdb_use_credits", enabled) }
    actual fun loadUseEpisodes(): Boolean? = bool("tvdb_use_episodes")
    actual fun saveUseEpisodes(enabled: Boolean) { store.putBoolean("tvdb_use_episodes", enabled) }
    actual fun loadUseSeasonPosters(): Boolean? = bool("tvdb_use_season_posters")
    actual fun saveUseSeasonPosters(enabled: Boolean) { store.putBoolean("tvdb_use_season_posters", enabled) }
    actual fun loadLanguage(): String? = store.getString("tvdb_language")
    actual fun saveLanguage(language: String) { store.putString("tvdb_language", language) }

    private fun extractBoolean(element: JsonElement?): Boolean? =
        (element as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put("tvdb_enabled", it) }
        loadApiKey()?.let { put("tvdb_api_key", it) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        val keys = listOf("tvdb_enabled", "tvdb_api_key")
        keys.forEach { store.remove(it) }

        extractBoolean(payload["tvdb_enabled"])?.let(::saveEnabled)
        loadApiKey()
        (payload["tvdb_api_key"] as? JsonPrimitive)?.content?.let(::saveApiKey)
    }
}
