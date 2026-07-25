package com.nuvio.app.features.anime.tvdb

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object AnimeTvdbSettingsStorage {
    private val store = DesktopStorage.store("nuvio_anime_tvdb_settings")

    private fun bool(key: String): Boolean? =
        if (store.contains(key)) store.getBoolean(key) else null

    actual fun loadEnabled(): Boolean? = bool("anime_tvdb_enabled")

    actual fun saveEnabled(enabled: Boolean) { store.putBoolean("anime_tvdb_enabled", enabled) }

    actual fun loadApiKey(): String? = store.getString("anime_tvdb_api_key")

    actual fun saveApiKey(apiKey: String) { store.putString("anime_tvdb_api_key", apiKey) }

    private fun elem(key: String, value: Boolean): Pair<String, JsonElement> =
        key to JsonPrimitive(value)

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put("anime_tvdb_enabled", it) }
        loadApiKey()?.let { put("anime_tvdb_api_key", it) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        val keys = listOf("anime_tvdb_enabled", "anime_tvdb_api_key")
        keys.forEach { store.remove(it) }

        payload["anime_tvdb_enabled"]?.jsonPrimitive?.booleanOrNull?.let(::saveEnabled)
        payload["anime_tvdb_api_key"]?.jsonPrimitive?.contentOrNull?.let(::saveApiKey)
    }
}
