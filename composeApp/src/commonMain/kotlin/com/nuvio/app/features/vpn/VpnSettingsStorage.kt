package com.nuvio.app.features.vpn

internal expect object VpnSettingsStorage {
    fun loadProfilesJson(): String?
    fun saveProfilesJson(json: String)
    fun loadActiveProfileId(): String?
    fun saveActiveProfileId(profileId: String?)
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
}
