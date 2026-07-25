package com.nuvio.app.features.anime.tvdb

import kotlinx.serialization.json.JsonObject

internal expect object AnimeTvdbSettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
    fun loadApiKey(): String?
    fun saveApiKey(apiKey: String)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
