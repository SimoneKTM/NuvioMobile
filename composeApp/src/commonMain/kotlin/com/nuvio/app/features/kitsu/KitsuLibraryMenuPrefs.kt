package com.nuvio.app.features.kitsu

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object KitsuLibraryMenuPrefs {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(KitsuLibraryMenuPrefsState())
    val state: StateFlow<KitsuLibraryMenuPrefsState> = _state.asStateFlow()

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val raw = KitsuStorage.loadMenuPrefsPayload().orEmpty().trim()
            if (raw.isNotBlank()) _state.value = json.decodeFromString(raw)
        }
    }

    fun setSortBy(sortBy: KitsuSortBy) {
        ensureLoaded()
        if (_state.value.sortBy == sortBy) return
        _state.value = _state.value.copy(sortBy = sortBy)
        persist()
    }

    fun setSortAscending(ascending: Boolean) {
        ensureLoaded()
        if (_state.value.sortAscending == ascending) return
        _state.value = _state.value.copy(sortAscending = ascending)
        persist()
    }

    fun setOpenByCatalogUrl(url: String?) {
        ensureLoaded()
        if (_state.value.openByCatalogUrl == url) return
        _state.value = _state.value.copy(openByCatalogUrl = url)
        persist()
    }

    private fun persist() {
        runCatching { KitsuStorage.saveMenuPrefsPayload(json.encodeToString(_state.value)) }
    }
}
