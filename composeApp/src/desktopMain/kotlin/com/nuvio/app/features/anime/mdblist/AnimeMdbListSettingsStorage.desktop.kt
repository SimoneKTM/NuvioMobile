package com.nuvio.app.features.anime.mdblist

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

internal actual object AnimeMdbListSettingsStorage {
    private val store = DesktopStorage.store("nuvio_anime_mdblist_settings")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun loadEnabled(): Boolean? = store.getBoolean("anime_mdblist_enabled")
    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("anime_mdblist_enabled", enabled) }
    actual fun loadApiKey(): String? = store.getString("anime_mdblist_api_key")
    actual fun saveApiKey(apiKey: String) { store.putString("anime_mdblist_api_key", apiKey) }
    actual fun loadUseImdb(): Boolean? = store.getBoolean("anime_mdblist_use_imdb")
    actual fun saveUseImdb(enabled: Boolean) { store.putBoolean("anime_mdblist_use_imdb", enabled) }
    actual fun loadUseTmdb(): Boolean? = store.getBoolean("anime_mdblist_use_tmdb")
    actual fun saveUseTmdb(enabled: Boolean) { store.putBoolean("anime_mdblist_use_tmdb", enabled) }
    actual fun loadUseTomatoes(): Boolean? = store.getBoolean("anime_mdblist_use_tomatoes")
    actual fun saveUseTomatoes(enabled: Boolean) { store.putBoolean("anime_mdblist_use_tomatoes", enabled) }
    actual fun loadUseMetacritic(): Boolean? = store.getBoolean("anime_mdblist_use_metacritic")
    actual fun saveUseMetacritic(enabled: Boolean) { store.putBoolean("anime_mdblist_use_metacritic", enabled) }
    actual fun loadUseTrakt(): Boolean? = store.getBoolean("anime_mdblist_use_trakt")
    actual fun saveUseTrakt(enabled: Boolean) { store.putBoolean("anime_mdblist_use_trakt", enabled) }
    actual fun loadUseLetterboxd(): Boolean? = store.getBoolean("anime_mdblist_use_letterboxd")
    actual fun saveUseLetterboxd(enabled: Boolean) { store.putBoolean("anime_mdblist_use_letterboxd", enabled) }
    actual fun loadUseAudience(): Boolean? = store.getBoolean("anime_mdblist_use_audience")
    actual fun saveUseAudience(enabled: Boolean) { store.putBoolean("anime_mdblist_use_audience", enabled) }

    actual fun exportToSyncPayload(): JsonObject {
        val map = mutableMapOf<String, String>()
        fun putOpt(key: String, value: Any?) { if (value != null) map[key] = value.toString() }
        putOpt("anime_mdblist_enabled", loadEnabled())
        putOpt("anime_mdblist_use_imdb", loadUseImdb())
        putOpt("anime_mdblist_use_tmdb", loadUseTmdb())
        putOpt("anime_mdblist_use_tomatoes", loadUseTomatoes())
        putOpt("anime_mdblist_use_metacritic", loadUseMetacritic())
        putOpt("anime_mdblist_use_trakt", loadUseTrakt())
        putOpt("anime_mdblist_use_letterboxd", loadUseLetterboxd())
        putOpt("anime_mdblist_use_audience", loadUseAudience())
        return json.decodeFromString(json.encodeToString(map))
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        fun bool(key: String) = payload[key]?.toString()?.toBooleanStrictOrNull()
        bool("anime_mdblist_enabled")?.let { saveEnabled(it) }
        bool("anime_mdblist_use_imdb")?.let { saveUseImdb(it) }
        bool("anime_mdblist_use_tmdb")?.let { saveUseTmdb(it) }
        bool("anime_mdblist_use_tomatoes")?.let { saveUseTomatoes(it) }
        bool("anime_mdblist_use_metacritic")?.let { saveUseMetacritic(it) }
        bool("anime_mdblist_use_trakt")?.let { saveUseTrakt(it) }
        bool("anime_mdblist_use_letterboxd")?.let { saveUseLetterboxd(it) }
        bool("anime_mdblist_use_audience")?.let { saveUseAudience(it) }
    }
}
