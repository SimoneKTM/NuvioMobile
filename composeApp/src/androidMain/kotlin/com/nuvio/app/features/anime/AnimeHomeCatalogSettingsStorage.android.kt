package com.nuvio.app.features.anime

import android.content.Context
import android.content.SharedPreferences

actual object AnimeHomeCatalogSettingsStorage {
    private const val preferencesName = "nuvio_anime_home_catalog_settings"
    private const val payloadKey = "anime_catalog_settings_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(): String? =
        preferences?.getString(payloadKey, null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey, payload)
            ?.apply()
    }
}
