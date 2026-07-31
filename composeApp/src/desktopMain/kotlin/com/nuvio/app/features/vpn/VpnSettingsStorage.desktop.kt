package com.nuvio.app.features.vpn

import com.nuvio.app.core.storage.DesktopStorage

internal actual object VpnSettingsStorage {
    private val store = DesktopStorage.store("nuvio_vpn_settings")

    actual fun loadProfilesJson(): String? = store.getString("vpn_profiles")

    actual fun saveProfilesJson(json: String) {
        store.putString("vpn_profiles", json)
    }

    actual fun loadActiveProfileId(): String? = store.getString("vpn_active_profile_id")

    actual fun saveActiveProfileId(profileId: String?) {
        if (profileId == null) {
            store.remove("vpn_active_profile_id")
        } else {
            store.putString("vpn_active_profile_id", profileId)
        }
    }

    actual fun loadEnabled(): Boolean? = store.getBoolean("vpn_enabled")

    actual fun saveEnabled(enabled: Boolean) {
        store.putBoolean("vpn_enabled", enabled)
    }
}
