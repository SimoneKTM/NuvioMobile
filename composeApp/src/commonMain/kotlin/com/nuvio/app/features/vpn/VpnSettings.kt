package com.nuvio.app.features.vpn

import kotlinx.serialization.Serializable

@Serializable
data class VpnProfile(
    val id: String,
    val label: String,
    val serverName: String,
    val configText: String,
)

data class VpnSettings(
    val enabled: Boolean = false,
    val activeProfileId: String? = null,
    val profiles: List<VpnProfile> = emptyList(),
) {
    val activeProfile: VpnProfile?
        get() = profiles.firstOrNull { it.id == activeProfileId }

    val hasProfiles: Boolean
        get() = profiles.isNotEmpty()
}
