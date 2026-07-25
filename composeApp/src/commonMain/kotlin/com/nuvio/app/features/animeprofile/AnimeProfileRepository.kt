package com.nuvio.app.features.animeprofile

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.plugins.PluginRuntimeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AnimeProfileRepository {
    private val log = Logger.withTag("AnimeProfileRepository")
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(AnimeProfileState())
    val state: StateFlow<AnimeProfileState> = _state.asStateFlow()

    private var currentProfileId = 1

    private const val PREFS_KEY = "anime_profile_config"

    fun initialize(profileId: Int = 1) {
        currentProfileId = profileId
        loadConfig()
    }

    fun onProfileChanged(profileId: Int) {
        currentProfileId = profileId
        loadConfig()
    }

    fun updateConfig(config: AnimeProfileConfig) {
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setEnabled(enabled: Boolean) {
        val config = _state.value.config.copy(enabled = enabled)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setCatalogSources(sources: List<String>) {
        val config = _state.value.config.copy(catalogSources = sources)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setEnabledAddons(addons: List<String>) {
        val config = _state.value.config.copy(enabledAddons = addons)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setEnabledPluginRepos(repos: List<String>) {
        val config = _state.value.config.copy(enabledPluginRepos = repos)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setUseAnilist(use: Boolean) {
        val config = _state.value.config.copy(useAnilistMetadata = use)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setUseMal(use: Boolean) {
        val config = _state.value.config.copy(useMalMetadata = use)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setUseKitsu(use: Boolean) {
        val config = _state.value.config.copy(useKitsuMetadata = use)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setPreferredLanguage(lang: String) {
        val config = _state.value.config.copy(preferredLanguage = lang)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setExcludeDubs(exclude: Boolean) {
        val config = _state.value.config.copy(excludeDubs = exclude)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setShowOnHomeTab(show: Boolean) {
        val config = _state.value.config.copy(showOnHomeTab = show)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    fun setTmdbConfig(tmdbConfig: AnimeTmdbConfig) {
        val config = _state.value.config.copy(tmdbConfig = tmdbConfig)
        _state.value = _state.value.copy(config = config)
        saveConfig()
    }

    suspend fun executeAnimePlugin(
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<PluginRuntimeResult> = withContext(Dispatchers.Default) {
        if (!_state.value.config.enabled) return@withContext emptyList()

        val results = mutableListOf<PluginRuntimeResult>()

        val enabledScrapers = PluginRepository.getEnabledScrapersForType(mediaType)
        for (scraper in enabledScrapers) {
            if (_state.value.config.enabledPluginRepos.isEmpty() ||
                _state.value.config.enabledPluginRepos.contains(scraper.repositoryUrl)
            ) {
                try {
                    val result = PluginRepository.executeScraper(scraper, tmdbId, mediaType, season, episode)
                    result.getOrNull()?.let { results.addAll(it) }
                } catch (e: Exception) {
                    log.e(e) { "Anime plugin execution failed: ${scraper.name}" }
                }
            }
        }

        results.toList()
    }

    private fun loadConfig() {
        val saved = AnimeProfileStorage.load(currentProfileId)
        if (saved != null) {
            _state.value = AnimeProfileState(
                config = saved,
                isLoaded = true,
            )
        } else {
            _state.value = AnimeProfileState(
                config = AnimeProfileConfig(),
                isLoaded = true,
            )
        }
    }

    private fun saveConfig() {
        AnimeProfileStorage.save(currentProfileId, _state.value.config)
    }

    fun clear() {
        _state.value = AnimeProfileState()
    }
}

internal object AnimeProfileStorage {
    private val prefs = mutableMapOf<String, String>()
    private val json = Json { ignoreUnknownKeys = true }

    fun load(profileId: Int): AnimeProfileConfig? {
        val key = "anime_profile_$profileId"
        val raw = prefs[key] ?: return null
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun save(profileId: Int, config: AnimeProfileConfig) {
        val key = "anime_profile_$profileId"
        prefs[key] = json.encodeToString(config)
    }

    fun clear(profileId: Int) {
        prefs.remove("anime_profile_$profileId")
    }
}
