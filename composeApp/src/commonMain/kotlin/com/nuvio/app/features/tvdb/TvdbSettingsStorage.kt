package com.nuvio.app.features.tvdb

import kotlinx.serialization.json.JsonObject

internal expect object TvdbSettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
    fun loadApiKey(): String?
    fun saveApiKey(apiKey: String)
    fun loadLanguage(): String?
    fun saveLanguage(language: String)
    fun loadUseTrailers(): Boolean?
    fun saveUseTrailers(enabled: Boolean)
    fun loadUseArtwork(): Boolean?
    fun saveUseArtwork(enabled: Boolean)
    fun loadUseBasicInfo(): Boolean?
    fun saveUseBasicInfo(enabled: Boolean)
    fun loadUseCredits(): Boolean?
    fun saveUseCredits(enabled: Boolean)
    fun loadUseEpisodes(): Boolean?
    fun saveUseEpisodes(enabled: Boolean)
    fun loadUseSeasonPosters(): Boolean?
    fun saveUseSeasonPosters(enabled: Boolean)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
