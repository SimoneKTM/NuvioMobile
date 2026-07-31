package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

actual object AnimeHomeCatalogSettingsStorage {
    private val store = DesktopStorage.store("nuvio_anime_home_catalog_settings")
    private const val payloadKey = "anime_catalog_settings_payload"

    actual fun loadPayload(): String? =
        store.getString(payloadKey)

    actual fun savePayload(payload: String) {
        store.putString(payloadKey, payload)
    }
}
