package com.nuvio.app.features.anime.tvdb

import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

actual object AnimeTvdbSettingsStorage {
    private const val enabledKey = "anime_tvdb_enabled"
    private const val apiKeyKey = "anime_tvdb_api_key"
    private const val useTrailersKey = "anime_tvdb_use_trailers"
    private const val useArtworkKey = "anime_tvdb_use_artwork"
    private const val useBasicInfoKey = "anime_tvdb_use_basic_info"
    private const val useCreditsKey = "anime_tvdb_use_credits"
    private const val useEpisodesKey = "anime_tvdb_use_episodes"
    private const val useSeasonPostersKey = "anime_tvdb_use_season_posters"
    private const val languageKey = "anime_tvdb_language"
    private val syncKeys = listOf(enabledKey, apiKeyKey)

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)

    actual fun saveEnabled(enabled: Boolean) {
        saveBoolean(enabledKey, enabled)
    }

    actual fun loadApiKey(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(apiKeyKey))

    actual fun saveApiKey(apiKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(apiKey, forKey = ProfileScopedKey.of(apiKeyKey))
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
    actual fun loadLanguage(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(languageKey))
    actual fun saveLanguage(language: String) {
        NSUserDefaults.standardUserDefaults.setObject(language, forKey = ProfileScopedKey.of(languageKey))
    }

    private fun loadBoolean(key: String): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val scopedKey = ProfileScopedKey.of(key)
        return if (defaults.objectForKey(scopedKey) != null) {
            defaults.boolForKey(scopedKey)
        } else {
            null
        }
    }

    private fun saveBoolean(key: String, enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(key))
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKeyKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        syncKeys.forEach { key ->
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(key))
        }

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKeyKey)?.let(::saveApiKey)
    }
}
