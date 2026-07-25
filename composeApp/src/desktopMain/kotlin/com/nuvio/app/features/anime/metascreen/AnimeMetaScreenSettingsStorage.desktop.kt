package com.nuvio.app.features.anime.metascreen

import com.nuvio.app.core.storage.DesktopStorage

internal actual object AnimeMetaScreenSettingsStorage {
    private val store = DesktopStorage.store("nuvio_anime_meta_screen_settings")

    actual fun loadPayload(): String? =
        store.getString("anime_meta_screen_payload")

    actual fun savePayload(payload: String) {
        store.putString("anime_meta_screen_payload", payload)
    }
}
