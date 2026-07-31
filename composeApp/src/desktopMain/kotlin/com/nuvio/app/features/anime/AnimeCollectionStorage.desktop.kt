package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

actual object AnimeCollectionStorage {
    private val store = DesktopStorage.store("nuvio_anime_collections")
    private const val payloadKey = "anime_collections_payload"

    actual fun loadPayload(): String? =
        store.getString(payloadKey)

    actual fun savePayload(payload: String) {
        store.putString(payloadKey, payload)
    }
}
