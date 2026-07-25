package com.nuvio.app.features.anime.mdblist

import com.nuvio.app.features.mdblist.MdbListMetadataService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AnimeMdbListSettingsRepository {
    private val _uiState = MutableStateFlow(AnimeMdbListSettings())
    val uiState: StateFlow<AnimeMdbListSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    private var enabled = false
    private var apiKey = ""
    private var useImdb = true
    private var useTmdb = true
    private var useTomatoes = true
    private var useMetacritic = true
    private var useTrakt = true
    private var useLetterboxd = true
    private var useAudience = true

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): AnimeMdbListSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (value && apiKey.isBlank()) return
        if (enabled == value) return
        enabled = value
        publish()
        AnimeMdbListSettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (apiKey == normalized) return
        apiKey = normalized
        if (apiKey.isBlank()) {
            enabled = false
            AnimeMdbListSettingsStorage.saveEnabled(false)
        }
        publish()
        AnimeMdbListSettingsStorage.saveApiKey(normalized)
        MdbListMetadataService.clearCache()
    }

    fun setProviderEnabled(providerId: String, value: Boolean) {
        ensureLoaded()
        when (providerId) {
            MdbListMetadataService.PROVIDER_IMDB -> if (useImdb != value) {
                useImdb = value
                AnimeMdbListSettingsStorage.saveUseImdb(value)
            } else return
            MdbListMetadataService.PROVIDER_TMDB -> if (useTmdb != value) {
                useTmdb = value
                AnimeMdbListSettingsStorage.saveUseTmdb(value)
            } else return
            MdbListMetadataService.PROVIDER_TOMATOES -> if (useTomatoes != value) {
                useTomatoes = value
                AnimeMdbListSettingsStorage.saveUseTomatoes(value)
            } else return
            MdbListMetadataService.PROVIDER_METACRITIC -> if (useMetacritic != value) {
                useMetacritic = value
                AnimeMdbListSettingsStorage.saveUseMetacritic(value)
            } else return
            MdbListMetadataService.PROVIDER_TRAKT -> if (useTrakt != value) {
                useTrakt = value
                AnimeMdbListSettingsStorage.saveUseTrakt(value)
            } else return
            MdbListMetadataService.PROVIDER_LETTERBOXD -> if (useLetterboxd != value) {
                useLetterboxd = value
                AnimeMdbListSettingsStorage.saveUseLetterboxd(value)
            } else return
            MdbListMetadataService.PROVIDER_AUDIENCE -> if (useAudience != value) {
                useAudience = value
                AnimeMdbListSettingsStorage.saveUseAudience(value)
            } else return
            else -> return
        }
        publish()
        MdbListMetadataService.clearCache()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        apiKey = AnimeMdbListSettingsStorage.loadApiKey().orEmpty().trim()
        enabled = (AnimeMdbListSettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        useImdb = AnimeMdbListSettingsStorage.loadUseImdb() ?: true
        useTmdb = AnimeMdbListSettingsStorage.loadUseTmdb() ?: true
        useTomatoes = AnimeMdbListSettingsStorage.loadUseTomatoes() ?: true
        useMetacritic = AnimeMdbListSettingsStorage.loadUseMetacritic() ?: true
        useTrakt = AnimeMdbListSettingsStorage.loadUseTrakt() ?: true
        useLetterboxd = AnimeMdbListSettingsStorage.loadUseLetterboxd() ?: true
        useAudience = AnimeMdbListSettingsStorage.loadUseAudience() ?: true
        publish()
    }

    private fun publish() {
        _uiState.value = AnimeMdbListSettings(
            enabled = enabled,
            apiKey = apiKey,
            useImdb = useImdb,
            useTmdb = useTmdb,
            useTomatoes = useTomatoes,
            useMetacritic = useMetacritic,
            useTrakt = useTrakt,
            useLetterboxd = useLetterboxd,
            useAudience = useAudience,
        )
    }
}
