package com.nuvio.app.features.kitsu

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object KitsuStorage {
    private const val authPayloadKey = "kitsu_auth_payload"
    private const val settingsPayloadKey = "kitsu_settings_payload"
    private const val libraryPayloadKey = "kitsu_library_payload"
    private const val menuPrefsPayloadKey = "kitsu_menu_prefs_payload"

    actual fun loadAuthPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(authPayloadKey))

    actual fun saveAuthPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(authPayloadKey))
    }

    actual fun loadSettingsPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(settingsPayloadKey))

    actual fun saveSettingsPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(settingsPayloadKey))
    }

    actual fun loadLibraryPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(libraryPayloadKey))

    actual fun saveLibraryPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(libraryPayloadKey))
    }

    actual fun loadMenuPrefsPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(menuPrefsPayloadKey))

    actual fun saveMenuPrefsPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(menuPrefsPayloadKey))
    }
}