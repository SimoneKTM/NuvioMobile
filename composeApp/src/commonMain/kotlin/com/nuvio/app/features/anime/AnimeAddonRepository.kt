package com.nuvio.app.features.anime

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddAddonResult
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonManifestParser
import com.nuvio.app.features.addons.AddonsUiState
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.registerUrlInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AnimeAddonRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("AnimeAddonRepository")
    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    private var initialized = false
    private val activeRefreshJobs = mutableMapOf<String, Job>()

    fun initialize() {
        if (initialized) return
        initialized = true
        log.d { "initialize() — loading local anime addons" }

        val storedUrls = dedupeManifestUrls(AnimeAddonStorage.loadInstalledAddonUrls())
        val enabledByUrl = loadLocalEnabledStates()
        log.d { "initialize() — local anime addon count: ${storedUrls.size}" }

        if (storedUrls.isEmpty()) return

        val existingByUrl = _uiState.value.addons.associateBy(ManagedAddon::manifestUrl)
        _uiState.value = AddonsUiState(
            addons = storedUrls.map { manifestUrl ->
                existingByUrl[manifestUrl].toAnimePendingAddon(
                    manifestUrl = manifestUrl,
                    enabled = enabledByUrl[manifestUrl],
                )
            },
        )

        storedUrls.forEach { manifestUrl ->
            val existing = existingByUrl[manifestUrl]
            val addon = _uiState.value.addons.firstOrNull { it.manifestUrl == manifestUrl }
            if (addon?.enabled == true && (existing == null || (addon.manifest == null && !addon.isRefreshing))) {
                refreshAddon(manifestUrl)
            }
        }
    }

    fun clearLocalState() {
        cancelActiveRefreshes()
        initialized = false
        _uiState.value = AddonsUiState()
    }

    suspend fun addAddon(rawUrl: String): AddAddonResult {
        log.i { "addAddon() — rawUrl=$rawUrl" }
        val manifestUrl = try {
            normalizeManifestUrl(rawUrl)
        } catch (error: IllegalArgumentException) {
            return AddAddonResult.Error(error.message ?: "Invalid URL")
        }

        if (_uiState.value.addons.any { it.manifestUrl == manifestUrl }) {
            return AddAddonResult.Error("Addon already installed")
        }

        val manifest = try {
            withContext(Dispatchers.Default) {
                val payload = httpGetText(manifestUrl)
                AddonManifestParser.parse(
                    manifestUrl = manifestUrl,
                    payload = payload,
                )
            }
        } catch (error: Throwable) {
            return AddAddonResult.Error(error.message ?: "Failed to load manifest")
        }

        _uiState.value = _uiState.value.copy(
            addons = _uiState.value.addons + ManagedAddon(
                manifestUrl = manifestUrl,
                manifest = manifest,
                isRefreshing = false,
                errorMessage = null,
            ),
        )
        persist()
        return AddAddonResult.Success(manifest)
    }

    fun removeAddon(manifestUrl: String) {
        log.i { "removeAddon() — $manifestUrl" }
        _uiState.value = _uiState.value.copy(
            addons = _uiState.value.addons.filterNot { it.manifestUrl == manifestUrl },
        )
        persist()
    }

    fun moveAddon(fromIndex: Int, toIndex: Int) {
        val addons = _uiState.value.addons
        if (fromIndex !in addons.indices || toIndex !in addons.indices || fromIndex == toIndex) {
            return
        }
        val reordered = addons.toMutableList()
        val movingAddon = reordered.removeAt(fromIndex)
        reordered.add(toIndex, movingAddon)
        _uiState.value = _uiState.value.copy(addons = reordered)
        persist()
    }

    fun setAddonEnabled(manifestUrl: String, enabled: Boolean) {
        var shouldRefresh = false
        _uiState.value = _uiState.value.copy(
            addons = _uiState.value.addons.map { addon ->
                if (addon.manifestUrl != manifestUrl || addon.enabled == enabled) {
                    addon
                } else {
                    shouldRefresh = enabled && addon.manifest == null && !addon.isRefreshing
                    addon.copy(enabled = enabled)
                }
            },
        )
        persist()
        if (shouldRefresh) {
            refreshAddon(manifestUrl)
        }
    }

    fun refreshAll() {
        _uiState.value.addons.filter { it.enabled }.distinctBy { it.manifestUrl }.forEach { addon ->
            refreshAddon(addon.manifestUrl)
        }
    }

    fun refreshAddon(manifestUrl: String) {
        val existingJob = activeRefreshJobs[manifestUrl]
        if (existingJob?.isActive == true) return

        markRefreshing(manifestUrl)
        var refreshJob: Job? = null
        refreshJob = scope.launch {
            try {
                val result = runCatching {
                    val payload = httpGetText(manifestUrl)
                    AddonManifestParser.parse(
                        manifestUrl = manifestUrl,
                        payload = payload,
                    )
                }

                _uiState.value = _uiState.value.copy(
                    addons = _uiState.value.addons.map { addon ->
                        if (addon.manifestUrl != manifestUrl) {
                            addon
                        } else {
                            result.fold(
                                onSuccess = { manifest ->
                                    addon.copy(
                                        manifest = manifest,
                                        isRefreshing = false,
                                        errorMessage = null,
                                    )
                                },
                                onFailure = { error ->
                                    addon.copy(
                                        isRefreshing = false,
                                        errorMessage = error.message ?: "Failed to load manifest",
                                    )
                                },
                            )
                        }
                    },
                )
            } finally {
                if (activeRefreshJobs[manifestUrl] === refreshJob) {
                    activeRefreshJobs.remove(manifestUrl)
                }
            }
        }
        activeRefreshJobs[manifestUrl] = refreshJob
    }

    private fun markRefreshing(manifestUrl: String) {
        _uiState.value = _uiState.value.copy(
            addons = _uiState.value.addons.map { addon ->
                if (addon.manifestUrl == manifestUrl) {
                    addon.copy(
                        isRefreshing = true,
                        errorMessage = null,
                    )
                } else {
                    addon
                }
            },
        )
    }

    private fun persist() {
        AnimeAddonStorage.saveInstalledAddonUrls(
            dedupeManifestUrls(_uiState.value.addons.map { it.manifestUrl }),
        )
        AnimeAddonStorage.saveAddonEnabledStates(
            _uiState.value.addons.associate { it.manifestUrl to it.enabled },
        )
    }

    private fun loadLocalEnabledStates(): Map<String, Boolean> =
        AnimeAddonStorage.loadAddonEnabledStates()
            .mapKeys { (url, _) -> ensureManifestSuffix(url) }

    private fun cancelActiveRefreshes() {
        activeRefreshJobs.values.forEach(Job::cancel)
        activeRefreshJobs.clear()
    }
}

private fun ManagedAddon?.toAnimePendingAddon(
    manifestUrl: String,
    userSetName: String? = null,
    enabled: Boolean? = null,
): ManagedAddon =
    when {
        this == null -> ManagedAddon(
            manifestUrl = manifestUrl,
            isRefreshing = enabled ?: true,
            userSetName = userSetName,
            enabled = enabled ?: true,
        )
        manifest != null -> copy(
            manifestUrl = manifestUrl,
            isRefreshing = false,
            userSetName = userSetName ?: this.userSetName,
            enabled = enabled ?: this.enabled,
        )
        isRefreshing -> copy(
            manifestUrl = manifestUrl,
            userSetName = userSetName ?: this.userSetName,
            enabled = enabled ?: this.enabled,
        )
        else -> copy(
            manifestUrl = manifestUrl,
            isRefreshing = enabled ?: this.enabled,
            errorMessage = null,
            userSetName = userSetName ?: this.userSetName,
            enabled = enabled ?: this.enabled,
        )
    }

private fun dedupeManifestUrls(urls: List<String>): List<String> =
    urls.map(::ensureManifestSuffix).distinct()

private fun ensureManifestSuffix(url: String): String {
    val path = url.substringBefore("?").trimEnd('/')
    val query = url.substringAfter("?", "")
    val withSuffix = if (path.endsWith("/manifest.json")) path else "$path/manifest.json"
    return if (query.isEmpty()) withSuffix else "$withSuffix?$query"
}

private fun normalizeManifestUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    require(trimmed.isNotEmpty()) { "Enter a valid URL" }

    val normalizedScheme = when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("stremio://") -> "https://${trimmed.removePrefix("stremio://")}"
        else -> "https://$trimmed"
    }

    val withoutFragment = normalizedScheme.substringBefore("#")
    val query = withoutFragment.substringAfter("?", "")
    val path = withoutFragment.substringBefore("?").trimEnd('/')
    val manifestPath = if (path.endsWith("/manifest.json")) {
        path
    } else {
        "$path/manifest.json"
    }

    return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
}
