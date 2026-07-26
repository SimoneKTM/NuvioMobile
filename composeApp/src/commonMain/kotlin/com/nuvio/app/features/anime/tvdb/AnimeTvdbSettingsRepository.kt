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
    private var useTrailers = true
    private var useArtwork = true
    private var useBasicInfo = true
    private var useCredits = true
    private var useEpisodes = true
    private var useSeasonPosters = true

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

    fun setUseTrailers(value: Boolean) = setBoolean(
        current = useTrailers,
        next = value,
        update = { useTrailers = it },
        persist = AnimeTvdbSettingsStorage::saveUseTrailers,
    )

    fun setUseArtwork(value: Boolean) = setBoolean(
        current = useArtwork,
        next = value,
        update = { useArtwork = it },
        persist = AnimeTvdbSettingsStorage::saveUseArtwork,
    )

    fun setUseBasicInfo(value: Boolean) = setBoolean(
        current = useBasicInfo,
        next = value,
        update = { useBasicInfo = it },
        persist = AnimeTvdbSettingsStorage::saveUseBasicInfo,
    )

    fun setUseCredits(value: Boolean) = setBoolean(
        current = useCredits,
        next = value,
        update = { useCredits = it },
        persist = AnimeTvdbSettingsStorage::saveUseCredits,
    )

    fun setUseEpisodes(value: Boolean) = setBoolean(
        current = useEpisodes,
        next = value,
        update = { useEpisodes = it },
        persist = AnimeTvdbSettingsStorage::saveUseEpisodes,
    )

    fun setUseSeasonPosters(value: Boolean) = setBoolean(
        current = useSeasonPosters,
        next = value,
        update = { useSeasonPosters = it },
        persist = AnimeTvdbSettingsStorage::saveUseSeasonPosters,
    )

    private fun setBoolean(
        current: Boolean,
        next: Boolean,
        update: (Boolean) -> Unit,
        persist: (Boolean) -> Unit,
    ) {
        ensureLoaded()
        if (current == next) return
        update(next)
        publish()
        persist(next)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        apiKey = AnimeTvdbSettingsStorage.loadApiKey()?.trim().orEmpty()
        enabled = (AnimeTvdbSettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        useTrailers = AnimeTvdbSettingsStorage.loadUseTrailers() ?: true
        useArtwork = AnimeTvdbSettingsStorage.loadUseArtwork() ?: true
        useBasicInfo = AnimeTvdbSettingsStorage.loadUseBasicInfo() ?: true
        useCredits = AnimeTvdbSettingsStorage.loadUseCredits() ?: true
        useEpisodes = AnimeTvdbSettingsStorage.loadUseEpisodes() ?: true
        useSeasonPosters = AnimeTvdbSettingsStorage.loadUseSeasonPosters() ?: true
        publish()
    }

    private fun publish() {
        _uiState.value = AnimeTvdbSettings(
            enabled = enabled,
            apiKey = apiKey,
            useTrailers = useTrailers,
            useArtwork = useArtwork,
            useBasicInfo = useBasicInfo,
            useCredits = useCredits,
            useEpisodes = useEpisodes,
            useSeasonPosters = useSeasonPosters,
        )
    }
}
