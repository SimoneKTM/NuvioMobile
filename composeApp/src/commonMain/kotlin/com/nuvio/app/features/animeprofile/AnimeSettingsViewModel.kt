package com.nuvio.app.features.animeprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AnimeSettingsUiState {
    data object Loading : AnimeSettingsUiState()
    data class Success(val plugins: List<AnimePluginItem>) : AnimeSettingsUiState()
    data class Error(val message: String) : AnimeSettingsUiState()
}

class AnimeSettingsViewModel(
    private val animePluginRegistry: AnimePluginRegistry = AnimePluginRegistry(),
) : ViewModel() {

    private val log = Logger.withTag("AnimeSettingsViewModel")
    private val _uiState = MutableStateFlow<AnimeSettingsUiState>(AnimeSettingsUiState.Loading)
    val uiState: StateFlow<AnimeSettingsUiState> = _uiState.asStateFlow()

    fun loadAnimePlugins() {
        _uiState.value = AnimeSettingsUiState.Loading
        avviaInizializzazioneSicura()
    }

    private fun avviaInizializzazioneSicura() {
        _uiState.value = AnimeSettingsUiState.Loading

        Thread {
            try {
                log.d { "Avvio inizializzazione in thread nativo..." }

                val plugins = animePluginRegistry.getAvailableAnimePluginsFromSandbox()

                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.value = AnimeSettingsUiState.Success(plugins)
                    log.d { "Caricamento completato: ${plugins.size} plugins" }
                }
            } catch (e: Exception) {
                log.e(e) { "Errore inizializzazione sandbox: ${e.message}" }
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.value = AnimeSettingsUiState.Error(
                        e.localizedMessage ?: "Errore interno Sandbox",
                    )
                }
            }
        }.start()
    }

    fun togglePlugin(pluginId: String, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                animePluginRegistry.updatePluginStatusInSandbox(pluginId, isEnabled)
                loadAnimePlugins()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnimeSettingsUiState.Error(
                        e.localizedMessage ?: "Errore aggiornamento plugin",
                    )
                }
            }
        }
    }
}
