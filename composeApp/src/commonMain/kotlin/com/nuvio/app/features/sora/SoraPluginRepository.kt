package com.nuvio.app.features.sora

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.plugins.PluginRuntimeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

object SoraPluginRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("SoraPluginRepository")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _uiState = MutableStateFlow(SoraUiState())
    val uiState: StateFlow<SoraUiState> = _uiState.asStateFlow()

    private const val SORA_LIBRARY_URL = "https://library.cufiy.net/library"
    private const val SORA_DEFAULT_REPO = "https://raw.githubusercontent.com/50n50/sources/main"

    private val activeJobs = mutableMapOf<String, Job>()

    fun initialize() {
        if (_uiState.value.repositories.isNotEmpty()) return

        addDefaultRepository()
    }

    private fun addDefaultRepository() {
        val defaultRepo = SoraRepository(
            id = "sora-default",
            name = "Sora Modules Library",
            url = SORA_LIBRARY_URL,
        )
        _uiState.update { it.copy(repositories = listOf(defaultRepo)) }
    }

    fun addRepository(url: String) {
        val existing = _uiState.value.repositories.find { it.url == url }
        if (existing != null) return

        val repo = SoraRepository(
            id = "sora-repo-${_uiState.value.repositories.size}",
            name = url.substringAfterLast("/").substringBefore("?"),
            url = url,
        )
        _uiState.update { it.copy(repositories = it.repositories + repo) }
        refreshRepository(repo.id)
    }

    fun removeRepository(repoId: String) {
        activeJobs[repoId]?.cancel()
        activeJobs.remove(repoId)
        _uiState.update { state ->
            state.copy(
                repositories = state.repositories.filterNot { it.id == repoId },
                modules = state.modules.filterNot { it.repositoryUrl == repoId },
            )
        }
    }

    fun refreshRepository(repoId: String) {
        val existing = activeJobs[repoId]
        if (existing?.isActive == true) return

        val job = scope.launch {
            markRefreshing(repoId, true)
            try {
                val repo = _uiState.value.repositories.find { it.id == repoId } ?: return@launch
                val modules = fetchModulesFromRepository(repo.url)

                _uiState.update { state ->
                    state.copy(
                        repositories = state.repositories.map {
                            if (it.id == repoId) it.copy(isRefreshing = false, errorMessage = null)
                            else it
                        },
                        modules = state.modules.filterNot { it.repositoryUrl == repoId } + modules,
                    )
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to refresh Sora repository: $repoId" }
                _uiState.update { state ->
                    state.copy(
                        repositories = state.repositories.map {
                            if (it.id == repoId) it.copy(
                                isRefreshing = false,
                                errorMessage = e.message ?: "Unknown error"
                            ) else it
                        },
                    )
                }
            }
        }
        activeJobs[repoId] = job
    }

    fun refreshAll() {
        _uiState.value.repositories.forEach { repo ->
            refreshRepository(repo.id)
        }
    }

    fun toggleModule(moduleId: String, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                modules = state.modules.map {
                    if (it.id == moduleId) it.copy(enabled = enabled) else it
                },
            )
        }
    }

    fun toggleSoraEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soraEnabled = enabled) }
    }

    fun getEnabledModulesForType(mediaType: String): List<SoraModule> {
        if (!_uiState.value.soraEnabled) return emptyList()
        return _uiState.value.modules.filter { module ->
            module.enabled && soraTypeToMediaType(module.manifest.type).contains(mediaType)
        }
    }

    suspend fun executeSoraModule(
        module: SoraModule,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<PluginRuntimeResult> {
        if (!SoraRuntimeAdapter.isAvailable()) {
            log.w { "Sora execution not available - runtime not initialized" }
            return emptyList()
        }
        return SoraRuntimeAdapter.executeSoraModule(module, tmdbId, mediaType, season, episode)
    }

    private suspend fun fetchModulesFromRepository(repoUrl: String): List<SoraModule> {
        return withContext(Dispatchers.Default) {
            try {
                val payload = httpGetText(repoUrl)
                val modules = parseModuleList(payload, repoUrl)
                modules.mapNotNull { manifestJson ->
                    val manifest = parseModuleManifest(manifestJson)
                    val scriptCode = downloadScript(manifest.scriptUrl)
                    if (scriptCode == null) {
                        log.w { "Failed to download script for ${manifest.sourceName}" }
                        null
                    } else {
                        SoraModule(
                            id = "sora:${manifest.sourceName.lowercase().replace(" ", "-")}",
                            manifest = manifest,
                            scriptCode = scriptCode,
                            repositoryUrl = repoUrl,
                        )
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to fetch modules from $repoUrl" }
                emptyList()
            }
        }
    }

    private suspend fun parseModuleList(payload: String, repoUrl: String): List<String> {
        val manifestJsonList = mutableListOf<String>()

        try {
            val trimmed = payload.trim()
            if (trimmed.startsWith("[")) {
                val array = json.parseToJsonElement(trimmed).jsonArray
                for (element in array) {
                    manifestJsonList.add(element.toString())
                }
            } else if (trimmed.startsWith("{")) {
                manifestJsonList.add(trimmed)
            } else {
                val lines = trimmed.lines().filter { it.isNotBlank() }
                for (line in lines) {
                    val url = line.trim().removeSurrounding("\"")
                    if (url.startsWith("http")) {
                        try {
                            val manifestPayload = httpGetText(url)
                            manifestJsonList.add(manifestPayload)
                        } catch (e: Exception) {
                            log.w { "Failed to fetch manifest from $url" }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to parse module list" }
        }

        return manifestJsonList
    }

    private fun parseModuleManifest(rawJson: String): SoraManifest {
        return SoraModuleParser.parseManifest(rawJson)
    }

    private suspend fun downloadScript(scriptUrl: String): String? {
        if (scriptUrl.isBlank()) return null
        return try {
            httpGetText(scriptUrl)
        } catch (e: Exception) {
            log.e(e) { "Failed to download script: $scriptUrl" }
            null
        }
    }

    private fun markRefreshing(repoId: String, refreshing: Boolean) {
        _uiState.update { state ->
            state.copy(
                repositories = state.repositories.map {
                    if (it.id == repoId) it.copy(isRefreshing = refreshing) else it
                },
            )
        }
    }

    fun clear() {
        activeJobs.values.forEach(Job::cancel)
        activeJobs.clear()
        _uiState.value = SoraUiState()
    }
}
