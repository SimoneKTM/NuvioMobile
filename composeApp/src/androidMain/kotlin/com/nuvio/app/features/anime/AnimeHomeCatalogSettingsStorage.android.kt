package com.nuvio.app.features.anime

import android.content.Context
import android.content.SharedPreferences

actual object AnimeHomeCatalogSettingsStorage {
    private const val preferencesName = "nuvio_anime_home_catalog_settings"
    private const val legacyPayloadKey = "anime_catalog_settings_payload"
    private fun payloadKey(profileId: Int) = "anime_catalog_settings_payload_$profileId"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        migrateLegacyPayloadToProfileOne()
    }

    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString(payloadKey(profileId), null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey(profileId), payload)
            ?.apply()
    }

    private fun migrateLegacyPayloadToProfileOne() {
        val prefs = preferences ?: return
        val legacy = prefs.getString(legacyPayloadKey, null) ?: return
        if (prefs.getString(payloadKey(1), null) == null) {
            prefs.edit().putString(payloadKey(1), legacy).apply()
        }
        prefs.edit().remove(legacyPayloadKey).apply()
    }
}
