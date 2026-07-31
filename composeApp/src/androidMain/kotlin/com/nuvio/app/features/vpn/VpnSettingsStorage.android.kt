package com.nuvio.app.features.vpn

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object VpnSettingsStorage {
    private const val preferencesName = "nuvio_vpn_settings"
    private const val profilesJsonKey = "vpn_profiles"
    private const val activeProfileIdKey = "vpn_active_profile_id"
    private const val enabledKey = "vpn_enabled"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadProfilesJson(): String? = loadString(profilesJsonKey)

    actual fun saveProfilesJson(json: String) {
        saveString(profilesJsonKey, json)
    }

    actual fun loadActiveProfileId(): String? = loadString(activeProfileIdKey)

    actual fun saveActiveProfileId(profileId: String?) {
        preferences?.edit()?.apply {
            val scopedKey = ProfileScopedKey.of(activeProfileIdKey)
            if (profileId == null) remove(scopedKey) else putString(scopedKey, profileId)
        }?.apply()
    }

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)

    actual fun saveEnabled(enabled: Boolean) {
        saveBoolean(enabledKey, enabled)
    }

    private fun loadString(key: String): String? =
        preferences?.getString(ProfileScopedKey.of(key), null)

    private fun saveString(key: String, value: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(key), value)
            ?.apply()
    }

    private fun loadBoolean(key: String): Boolean? =
        preferences?.let { sharedPreferences ->
            val scopedKey = ProfileScopedKey.of(key)
            if (sharedPreferences.contains(scopedKey)) {
                sharedPreferences.getBoolean(scopedKey, false)
            } else {
                null
            }
        }

    private fun saveBoolean(key: String, value: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(key), value)
            ?.apply()
    }
}
