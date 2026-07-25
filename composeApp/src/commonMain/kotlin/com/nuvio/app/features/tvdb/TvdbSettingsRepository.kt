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

    fun setUseTrailers(value: Boolean) = setBoolean(
        current = useTrailers,
        next = value,
        update = { useTrailers = it },
        persist = TvdbSettingsStorage::saveUseTrailers,
    )

    fun setUseArtwork(value: Boolean) = setBoolean(
        current = useArtwork,
        next = value,
        update = { useArtwork = it },
        persist = TvdbSettingsStorage::saveUseArtwork,
    )

    fun setUseBasicInfo(value: Boolean) = setBoolean(
        current = useBasicInfo,
        next = value,
        update = { useBasicInfo = it },
        persist = TvdbSettingsStorage::saveUseBasicInfo,
    )

    fun setUseCredits(value: Boolean) = setBoolean(
        current = useCredits,
        next = value,
        update = { useCredits = it },
        persist = TvdbSettingsStorage::saveUseCredits,
    )

    fun setUseEpisodes(value: Boolean) = setBoolean(
        current = useEpisodes,
        next = value,
        update = { useEpisodes = it },
        persist = TvdbSettingsStorage::saveUseEpisodes,
    )

    fun setUseSeasonPosters(value: Boolean) = setBoolean(
        current = useSeasonPosters,
        next = value,
        update = { useSeasonPosters = it },
        persist = TvdbSettingsStorage::saveUseSeasonPosters,
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
        apiKey = TvdbSettingsStorage.loadApiKey()?.trim().orEmpty()
        enabled = (TvdbSettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        useTrailers = TvdbSettingsStorage.loadUseTrailers() ?: true
        useArtwork = TvdbSettingsStorage.loadUseArtwork() ?: true
        useBasicInfo = TvdbSettingsStorage.loadUseBasicInfo() ?: true
        useCredits = TvdbSettingsStorage.loadUseCredits() ?: true
        useEpisodes = TvdbSettingsStorage.loadUseEpisodes() ?: true
        useSeasonPosters = TvdbSettingsStorage.loadUseSeasonPosters() ?: true
        publish()
    }

    private fun publish() {
        _uiState.value = TvdbSettings(
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
