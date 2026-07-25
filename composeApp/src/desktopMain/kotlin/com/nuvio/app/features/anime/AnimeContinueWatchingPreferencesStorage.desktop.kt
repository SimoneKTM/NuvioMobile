package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

internal actual object AnimeContinueWatchingPreferencesStorage {
    private val store = DesktopStorage.store("nuvio_anime_continue_watching_preferences")

    actual fun loadPayload(): String? = store.getString("anime_continue_watching_preferences_payload")

    actual fun savePayload(payload: String) = store.putString("anime_continue_watching_preferences_payload", payload)
}
