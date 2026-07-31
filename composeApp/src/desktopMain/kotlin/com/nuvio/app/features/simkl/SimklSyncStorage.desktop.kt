package com.nuvio.app.features.simkl

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object SimklSyncStorage {
    private const val PAYLOAD_KEY = "simkl_sync_snapshot"

    private val store = DesktopStorage.store("nuvio_simkl_sync")

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of(PAYLOAD_KEY))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of(PAYLOAD_KEY), payload)
    }

    actual fun removeProfile(profileId: Int) {
        store.remove(ProfileScopedKey.of(PAYLOAD_KEY, profileId))
    }
}
