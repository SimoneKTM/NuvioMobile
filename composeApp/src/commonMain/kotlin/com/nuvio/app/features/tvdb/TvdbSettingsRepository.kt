package com.nuvio.app.features.tvdb

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TvdbSettingsRepository {
    private val _uiState = MutableStateFlow(TvdbSettings())
    val uiState: StateFlow<TvdbSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    private var enabled = false
    private var apiKey = ""

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): TvdbSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (value && apiKey.isBlank()) return
        if (enabled == value) return
        enabled = value
        publish()
        TvdbSettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (apiKey == normalized) return
        apiKey = normalized
        if (apiKey.isBlank()) {
            enabled = false
            TvdbSettingsStorage.saveEnabled(false)
        }
        publish()
        TvdbSettingsStorage.saveApiKey(normalized)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        apiKey = TvdbSettingsStorage.loadApiKey()?.trim().orEmpty()
        enabled = (TvdbSettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        publish()
    }

    private fun publish() {
        _uiState.value = TvdbSettings(
            enabled = enabled,
            apiKey = apiKey,
        )
    }
}
