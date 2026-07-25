package com.nuvio.app.features.anime.tmdb

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AnimeTmdbSettingsRepository {
    private val _uiState = MutableStateFlow(AnimeTmdbSettings())
    val uiState: StateFlow<AnimeTmdbSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    private var enabled = false
    private var apiKey = ""
    private var language = "en"
    private var useTrailers = true
    private var useArtwork = true
    private var useBasicInfo = true
    private var useDetails = true
    private var useCredits = true
    private var useProductions = true
    private var useNetworks = true
    private var useEpisodes = true
    private var useSeasonPosters = true
    private var useMoreLikeThis = true
    private var useCollections = true

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): AnimeTmdbSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (value && apiKey.isBlank()) return
        if (enabled == value) return
        enabled = value
        publish()
        AnimeTmdbSettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (apiKey == normalized) return
        apiKey = normalized
        if (apiKey.isBlank()) {
            enabled = false
            AnimeTmdbSettingsStorage.saveEnabled(false)
        }
        publish()
        AnimeTmdbSettingsStorage.saveApiKey(normalized)
    }

    fun setLanguage(value: String) {
        ensureLoaded()
        val normalized = normalizeAnimeTmdbLanguage(value)
        if (language == normalized) return
        language = normalized
        publish()
        AnimeTmdbSettingsStorage.saveLanguage(normalized)
    }

    fun setUseTrailers(value: Boolean) = setBoolean(
        current = useTrailers,
        next = value,
        update = { useTrailers = it },
        persist = AnimeTmdbSettingsStorage::saveUseTrailers,
    )

    fun setUseArtwork(value: Boolean) = setBoolean(
        current = useArtwork,
        next = value,
        update = { useArtwork = it },
        persist = AnimeTmdbSettingsStorage::saveUseArtwork,
    )

    fun setUseBasicInfo(value: Boolean) = setBoolean(
        current = useBasicInfo,
        next = value,
        update = { useBasicInfo = it },
        persist = AnimeTmdbSettingsStorage::saveUseBasicInfo,
    )

    fun setUseDetails(value: Boolean) = setBoolean(
        current = useDetails,
        next = value,
        update = { useDetails = it },
        persist = AnimeTmdbSettingsStorage::saveUseDetails,
    )

    fun setUseCredits(value: Boolean) = setBoolean(
        current = useCredits,
        next = value,
        update = { useCredits = it },
        persist = AnimeTmdbSettingsStorage::saveUseCredits,
    )

    fun setUseProductions(value: Boolean) = setBoolean(
        current = useProductions,
        next = value,
        update = { useProductions = it },
        persist = AnimeTmdbSettingsStorage::saveUseProductions,
    )

    fun setUseNetworks(value: Boolean) = setBoolean(
        current = useNetworks,
        next = value,
        update = { useNetworks = it },
        persist = AnimeTmdbSettingsStorage::saveUseNetworks,
    )

    fun setUseEpisodes(value: Boolean) = setBoolean(
        current = useEpisodes,
        next = value,
        update = { useEpisodes = it },
        persist = AnimeTmdbSettingsStorage::saveUseEpisodes,
    )

    fun setUseSeasonPosters(value: Boolean) = setBoolean(
        current = useSeasonPosters,
        next = value,
        update = { useSeasonPosters = it },
        persist = AnimeTmdbSettingsStorage::saveUseSeasonPosters,
    )

    fun setUseMoreLikeThis(value: Boolean) = setBoolean(
        current = useMoreLikeThis,
        next = value,
        update = { useMoreLikeThis = it },
        persist = AnimeTmdbSettingsStorage::saveUseMoreLikeThis,
    )

    fun setUseCollections(value: Boolean) = setBoolean(
        current = useCollections,
        next = value,
        update = { useCollections = it },
        persist = AnimeTmdbSettingsStorage::saveUseCollections,
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
        apiKey = AnimeTmdbSettingsStorage.loadApiKey()?.trim().orEmpty()
        enabled = (AnimeTmdbSettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        val storedLanguage = AnimeTmdbSettingsStorage.loadLanguage()
        language = if (storedLanguage == null) "en" else normalizeAnimeTmdbLanguage(storedLanguage)
        useTrailers = AnimeTmdbSettingsStorage.loadUseTrailers() ?: true
        useArtwork = AnimeTmdbSettingsStorage.loadUseArtwork() ?: true
        useBasicInfo = AnimeTmdbSettingsStorage.loadUseBasicInfo() ?: true
        useDetails = AnimeTmdbSettingsStorage.loadUseDetails() ?: true
        useCredits = AnimeTmdbSettingsStorage.loadUseCredits() ?: true
        useProductions = AnimeTmdbSettingsStorage.loadUseProductions() ?: true
        useNetworks = AnimeTmdbSettingsStorage.loadUseNetworks() ?: true
        useEpisodes = AnimeTmdbSettingsStorage.loadUseEpisodes() ?: true
        useSeasonPosters = AnimeTmdbSettingsStorage.loadUseSeasonPosters() ?: true
        useMoreLikeThis = AnimeTmdbSettingsStorage.loadUseMoreLikeThis() ?: true
        useCollections = AnimeTmdbSettingsStorage.loadUseCollections() ?: true
        publish()
    }

    private fun publish() {
        _uiState.value = AnimeTmdbSettings(
            enabled = enabled,
            apiKey = apiKey,
            language = language,
            useTrailers = useTrailers,
            useArtwork = useArtwork,
            useBasicInfo = useBasicInfo,
            useDetails = useDetails,
            useCredits = useCredits,
            useProductions = useProductions,
            useNetworks = useNetworks,
            useEpisodes = useEpisodes,
            useSeasonPosters = useSeasonPosters,
            useMoreLikeThis = useMoreLikeThis,
            useCollections = useCollections,
        )
    }
}

internal fun normalizeAnimeTmdbLanguage(value: String?): String {
    val trimmed = value?.trim()?.replace('_', '-') ?: return ""
    return trimmed.takeIf { it.isNotBlank() } ?: ""
}
