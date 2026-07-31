package com.nuvio.app.features.kitsu

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object KitsuStorage {
    private const val authPayloadKey = "kitsu_auth_payload"
    private const val settingsPayloadKey = "kitsu_settings_payload"
    private const val libraryPayloadKey = "kitsu_library_payload"
    private const val menuPrefsPayloadKey = "kitsu_menu_prefs_payload"

    private val store = DesktopStorage.store("nuvio_kitsu")

    actual fun loadAuthPayload(): String? =
        store.getString(ProfileScopedKey.of(authPayloadKey))

    actual fun saveAuthPayload(payload: String) {
        store.putString(ProfileScopedKey.of(authPayloadKey), payload)
    }

    actual fun loadSettingsPayload(): String? =
        store.getString(ProfileScopedKey.of(settingsPayloadKey))

    actual fun saveSettingsPayload(payload: String) {
        store.putString(ProfileScopedKey.of(settingsPayloadKey), payload)
    }

    actual fun loadLibraryPayload(): String? =
        store.getString(ProfileScopedKey.of(libraryPayloadKey))

    actual fun saveLibraryPayload(payload: String) {
        store.putString(ProfileScopedKey.of(libraryPayloadKey), payload)
    }

    actual fun loadMenuPrefsPayload(): String? =
        store.getString(ProfileScopedKey.of(menuPrefsPayloadKey))

    actual fun saveMenuPrefsPayload(payload: String) {
        store.putString(ProfileScopedKey.of(menuPrefsPayloadKey), payload)
    }
}
