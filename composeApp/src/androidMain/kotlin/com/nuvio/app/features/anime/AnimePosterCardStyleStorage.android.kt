package com.nuvio.app.features.anime

import android.content.Context
import android.content.SharedPreferences

actual object AnimePosterCardStyleStorage {
    private const val preferencesName = "nuvio_anime_poster_card_style"
    private const val payloadKey = "anime_poster_card_style_payload"

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
