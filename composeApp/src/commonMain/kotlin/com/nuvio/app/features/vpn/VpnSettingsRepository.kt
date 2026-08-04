package com.nuvio.app.features.vpn

import com.nuvio.app.features.streams.epochMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object VpnSettingsRepository {
    private val _uiState = MutableStateFlow(VpnSettings())
    val uiState: StateFlow<VpnSettings> = _uiState.asStateFlow()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): VpnSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun addProfile(label: String, configText: String): Boolean {
        ensureLoaded()
        val trimmed = configText.trim()
        val parsed = WireGuardConfigParser.parse(trimmed) ?: return false
        val current = _uiState.value
        val profile = VpnProfile(
            id = "vpn-${epochMs()}-${current.profiles.size}",
            label = label.trim().ifBlank { parsed.serverHost },
            serverName = parsed.serverHost,
            configText = trimmed,
        )
        val profiles = current.profiles + profile
        _uiState.value = current.copy(profiles = profiles)
        VpnSettingsStorage.saveProfilesJson(json.encodeToString(profiles))
        return true
    }

    fun removeProfile(profileId: String) {
        ensureLoaded()
        val current = _uiState.value
        if (current.profiles.none { it.id == profileId }) return
        val wasActive = current.activeProfileId == profileId
        if (wasActive) VpnController.deactivate()
        _uiState.value = VpnSettings(
            enabled = if (wasActive) false else current.enabled,
            activeProfileId = if (wasActive) null else current.activeProfileId,
            profiles = current.profiles.filterNot { it.id == profileId },
        )
        persist()
    }

    fun selectProfile(profileId: String): Boolean {
        ensureLoaded()
        val current = _uiState.value
        val profile = current.profiles.firstOrNull { it.id == profileId } ?: return false
        if (current.activeProfileId == profileId) {
            if (current.enabled && VpnController.runtimeState.value != VpnRuntimeState.ACTIVE) {
                val result = VpnController.activate(profile.configText)
                if (result == VpnActivationResult.INVALID_CONFIG || result == VpnActivationResult.FAILED) return false
            }
            return true
        }
        if (current.enabled) VpnController.deactivate()
        val result = VpnController.activate(profile.configText)
        if (result == VpnActivationResult.INVALID_CONFIG || result == VpnActivationResult.FAILED) return false
        _uiState.value = current.copy(activeProfileId = profileId, enabled = true)
        persist()
        return true
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        val current = _uiState.value
        if (current.enabled == value) return
        if (value) {
            val profile = current.activeProfile ?: current.profiles.firstOrNull() ?: return
            val result = VpnController.activate(profile.configText)
            if (result == VpnActivationResult.INVALID_CONFIG || result == VpnActivationResult.FAILED) return
            _uiState.value = current.copy(
                enabled = true,
                activeProfileId = current.activeProfileId ?: profile.id,
            )
        } else {
            VpnController.deactivate()
            _uiState.value = current.copy(enabled = false)
        }
        persist()
    }

    fun restoreActiveTunnel(): Boolean {
        ensureLoaded()
        val current = _uiState.value
        if (!current.enabled) return false
        val profile = current.activeProfile ?: return false
        if (VpnController.runtimeState.value == VpnRuntimeState.ACTIVE) return true
        if (VpnController.pendingPermission.value) return false
        return VpnController.activate(profile.configText) == VpnActivationResult.ACTIVE
    }

    private fun loadFromDisk() {
        val profiles = VpnSettingsStorage.loadProfilesJson()
            ?.let { runCatching { json.decodeFromString<List<VpnProfile>>(it) }.getOrNull() }
            ?: emptyList()
        val activeProfileId = VpnSettingsStorage.loadActiveProfileId()
            ?.takeIf { id -> profiles.any { it.id == id } }
        val enabled = VpnSettingsStorage.loadEnabled() ?: false
        _uiState.value = VpnSettings(
            enabled = enabled,
            activeProfileId = activeProfileId,
            profiles = profiles,
        )
        hasLoaded = true
    }

    private fun persist() {
        val settings = _uiState.value
        VpnSettingsStorage.saveProfilesJson(json.encodeToString(settings.profiles))
        VpnSettingsStorage.saveActiveProfileId(settings.activeProfileId)
        VpnSettingsStorage.saveEnabled(settings.enabled)
    }
}
