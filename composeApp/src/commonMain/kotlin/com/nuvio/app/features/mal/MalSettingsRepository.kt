package com.nuvio.app.features.mal

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MalSettingsUiState(
    val enableSync: Boolean = false,
    val syncWatching: Boolean = true,
    val autoSync: Boolean = true,
    val syncOnLaunch: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val autoAddNewAnime: Boolean = false,
)

object MalSettingsRepository {
    private val log = Logger.withTag("MalSettings")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _uiState = MutableStateFlow(MalSettingsUiState())
    val uiState: StateFlow<MalSettingsUiState> = _uiState.asStateFlow()
    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = MalSettingsUiState()
        persist()
    }

    fun setEnableSync(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.enableSync == enabled) return
        _uiState.value = _uiState.value.copy(enableSync = enabled)
        persist()
    }

    fun setSyncWatching(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.syncWatching == enabled) return
        _uiState.value = _uiState.value.copy(syncWatching = enabled)
        persist()
    }

    fun setAutoSync(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.autoSync == enabled) return
        _uiState.value = _uiState.value.copy(autoSync = enabled)
        persist()
    }

    fun setSyncOnLaunch(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.syncOnLaunch == enabled) return
        _uiState.value = _uiState.value.copy(syncOnLaunch = enabled)
        persist()
    }

    fun updateLastSyncTimestamp(timestamp: Long) {
        ensureLoaded()
        if (_uiState.value.lastSyncTimestamp == timestamp) return
        _uiState.value = _uiState.value.copy(lastSyncTimestamp = timestamp)
        persist()
    }

    fun setAutoAddNewAnime(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.autoAddNewAnime == enabled) return
        _uiState.value = _uiState.value.copy(autoAddNewAnime = enabled)
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = MalLibraryStorage.loadSettingsPayload().orEmpty().trim()
        _uiState.value = if (payload.isBlank()) {
            MalSettingsUiState()
        } else {
            runCatching { json.decodeFromString<MalSettingsUiState>(payload) }
                .getOrElse {
                    log.w { "Failed to parse MAL settings payload: ${it.message}" }
                    MalSettingsUiState()
                }
        }
    }

    private fun persist() {
        runCatching {
            val payload = json.encodeToString(_uiState.value)
            MalLibraryStorage.saveSettingsPayload(payload)
        }.onFailure {
            log.w { "Failed to persist MAL settings state: ${it.message}" }
        }
    }
}
