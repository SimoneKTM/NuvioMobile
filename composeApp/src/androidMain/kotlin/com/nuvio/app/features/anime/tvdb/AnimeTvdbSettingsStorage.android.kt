package com.nuvio.app.features.anime.tvdb

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual object AnimeTvdbSettingsStorage {
    private const val preferencesName = "nuvio_anime_tvdb_settings"
    private const val enabledKey = "anime_tvdb_enabled"
    private const val apiKeyKey = "anime_tvdb_api_key"
    private const val useTrailersKey = "anime_tvdb_use_trailers"
    private const val useArtworkKey = "anime_tvdb_use_artwork"
    private const val useBasicInfoKey = "anime_tvdb_use_basic_info"
    private const val useCreditsKey = "anime_tvdb_use_credits"
    private const val useEpisodesKey = "anime_tvdb_use_episodes"
    private const val useSeasonPostersKey = "anime_tvdb_use_season_posters"
    private val syncKeys = listOf(enabledKey, apiKeyKey)

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)

    actual fun saveEnabled(enabled: Boolean) {
        saveBoolean(enabledKey, enabled)
    }

    actual fun loadApiKey(): String? =
        preferences?.getString(ProfileScopedKey.of(apiKeyKey), null)

    actual fun saveApiKey(apiKey: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(apiKeyKey), apiKey)
            ?.apply()
    }

    actual fun loadUseTrailers(): Boolean? = loadBoolean(useTrailersKey)
    actual fun saveUseTrailers(enabled: Boolean) { saveBoolean(useTrailersKey, enabled) }
    actual fun loadUseArtwork(): Boolean? = loadBoolean(useArtworkKey)
    actual fun saveUseArtwork(enabled: Boolean) { saveBoolean(useArtworkKey, enabled) }
    actual fun loadUseBasicInfo(): Boolean? = loadBoolean(useBasicInfoKey)
    actual fun saveUseBasicInfo(enabled: Boolean) { saveBoolean(useBasicInfoKey, enabled) }
    actual fun loadUseCredits(): Boolean? = loadBoolean(useCreditsKey)
    actual fun saveUseCredits(enabled: Boolean) { saveBoolean(useCreditsKey, enabled) }
    actual fun loadUseEpisodes(): Boolean? = loadBoolean(useEpisodesKey)
    actual fun saveUseEpisodes(enabled: Boolean) { saveBoolean(useEpisodesKey, enabled) }
    actual fun loadUseSeasonPosters(): Boolean? = loadBoolean(useSeasonPostersKey)
    actual fun saveUseSeasonPosters(enabled: Boolean) { saveBoolean(useSeasonPostersKey, enabled) }

    private fun loadBoolean(key: String): Boolean? =
        preferences?.let { sharedPreferences ->
            val scopedKey = ProfileScopedKey.of(key)
            if (sharedPreferences.contains(scopedKey)) {
                sharedPreferences.getBoolean(scopedKey, false)
            } else {
                null
            }
        }

    private fun saveBoolean(key: String, enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(key), enabled)
            ?.apply()
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKeyKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        preferences?.edit()?.apply {
            syncKeys.forEach { remove(ProfileScopedKey.of(it)) }
        }?.apply()

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKeyKey)?.let(::saveApiKey)
    }
}
