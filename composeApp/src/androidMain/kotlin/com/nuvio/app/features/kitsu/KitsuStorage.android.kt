package com.nuvio.app.features.kitsu

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object KitsuStorage {
    private const val preferencesName = "nuvio_kitsu"
    private const val authPayloadKey = "kitsu_auth_payload"
    private const val settingsPayloadKey = "kitsu_settings_payload"
    private const val libraryPayloadKey = "kitsu_library_payload"
    private const val menuPrefsPayloadKey = "kitsu_menu_prefs_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadAuthPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(authPayloadKey), null)

    actual fun saveAuthPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(authPayloadKey), payload)?.apply()
    }

    actual fun loadSettingsPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(settingsPayloadKey), null)

    actual fun saveSettingsPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(settingsPayloadKey), payload)?.apply()
    }

    actual fun loadLibraryPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(libraryPayloadKey), null)

    actual fun saveLibraryPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(libraryPayloadKey), payload)?.apply()
    }

    actual fun loadMenuPrefsPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(menuPrefsPayloadKey), null)

    actual fun saveMenuPrefsPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(menuPrefsPayloadKey), payload)?.apply()
    }
}
