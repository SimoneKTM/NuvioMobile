package com.nuvio.app.features.tvdb

import kotlinx.serialization.json.JsonObject

internal expect object TvdbSettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
    fun loadApiKey(): String?
    fun saveApiKey(apiKey: String)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
