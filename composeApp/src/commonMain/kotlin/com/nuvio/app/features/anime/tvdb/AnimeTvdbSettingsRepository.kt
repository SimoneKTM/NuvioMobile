package com.nuvio.app.features.anime.tvdb

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AnimeTvdbSettingsRepository {
    private val _uiState = MutableStateFlow(AnimeTvdbSettings())
    val uiState: StateFlow<AnimeTvdbSettings> = _uiState.asStateFlow()

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

    fun snapshot(): AnimeTvdbSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (value && apiKey.isBlank()) return
        if (enabled == value) return
        enabled = value
        publish()
        AnimeTvdbSettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (apiKey == normalized) return
        apiKey = normalized
        if (apiKey.isBlank()) {
            enabled = false
            AnimeTvdbSettingsStorage.saveEnabled(false)
        }
        publish()
        AnimeTvdbSettingsStorage.saveApiKey(normalized)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        apiKey = AnimeTvdbSettingsStorage.loadApiKey()?.trim().orEmpty()
        enabled = (AnimeTvdbSettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        publish()
    }

    private fun publish() {
        _uiState.value = AnimeTvdbSettings(
            enabled = enabled,
            apiKey = apiKey,
        )
    }
}
