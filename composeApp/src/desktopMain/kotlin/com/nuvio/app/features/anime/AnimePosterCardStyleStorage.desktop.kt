package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

actual object AnimePosterCardStyleStorage {
    private val store = DesktopStorage.store("nuvio_anime_poster_card_style")
    private const val payloadKey = "anime_poster_card_style_payload"

    actual fun loadPayload(): String? =
        store.getString(payloadKey)

    actual fun savePayload(payload: String) {
        store.putString(payloadKey, payload)
    }
}
