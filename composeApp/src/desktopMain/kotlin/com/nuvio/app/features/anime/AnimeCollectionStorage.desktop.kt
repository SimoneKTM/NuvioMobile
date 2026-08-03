package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

actual object AnimeCollectionStorage {
    private val store = DesktopStorage.store("nuvio_anime_collections")
    private const val legacyPayloadKey = "anime_collections_payload"
    private fun payloadKey(profileId: Int) = "anime_collections_payload_$profileId"

    init {
        migrateLegacyPayloadToProfileOne()
    }

    actual fun loadPayload(profileId: Int): String? =
        store.getString(payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        store.putString(payloadKey(profileId), payload)
    }

    private fun migrateLegacyPayloadToProfileOne() {
        val legacy = store.getString(legacyPayloadKey) ?: return
        if (store.getString(payloadKey(1)) == null) {
            store.putString(payloadKey(1), legacy)
        }
        store.remove(legacyPayloadKey)
    }
}
