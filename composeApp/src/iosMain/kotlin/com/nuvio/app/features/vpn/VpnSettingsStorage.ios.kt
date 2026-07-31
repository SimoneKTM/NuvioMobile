package com.nuvio.app.features.vpn

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object VpnSettingsStorage {
    private const val profilesJsonKey = "vpn_profiles"
    private const val activeProfileIdKey = "vpn_active_profile_id"
    private const val enabledKey = "vpn_enabled"

    actual fun loadProfilesJson(): String? = loadString(profilesJsonKey)

    actual fun saveProfilesJson(json: String) {
        saveString(profilesJsonKey, json)
    }

    actual fun loadActiveProfileId(): String? = loadString(activeProfileIdKey)

    actual fun saveActiveProfileId(profileId: String?) {
        if (profileId == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(activeProfileIdKey))
        } else {
            saveString(activeProfileIdKey, profileId)
        }
    }

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)

    actual fun saveEnabled(enabled: Boolean) {
        saveBoolean(enabledKey, enabled)
    }

    private fun loadString(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(key))

    private fun saveString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = ProfileScopedKey.of(key))
    }

    private fun loadBoolean(key: String): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val scopedKey = ProfileScopedKey.of(key)
        return if (defaults.objectForKey(scopedKey) != null) {
            defaults.boolForKey(scopedKey)
        } else {
            null
        }
    }

    private fun saveBoolean(key: String, value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = ProfileScopedKey.of(key))
    }
}
