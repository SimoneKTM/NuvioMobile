package com.nuvio.app.features.animeprofile

import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.sora.SoraPluginRepository

class AnimePluginRegistry(
    private val animeProfileRepository: AnimeProfileRepository = AnimeProfileRepository,
    private val pluginRepository: PluginRepository = PluginRepository,
    private val soraPluginRepository: SoraPluginRepository = SoraPluginRepository,
) {
    fun getAvailableAnimePluginsFromSandbox(): List<AnimePluginItem> {
        animeProfileRepository.initialize()
        soraPluginRepository.initialize()
        pluginRepository.initialize()

        val config = animeProfileRepository.state.value.config

        val items = mutableListOf<AnimePluginItem>()

        items.add(
            AnimePluginItem(
                id = "anime_profile_toggle",
                name = "Anime Profile",
                description = "Separate profile for anime with custom catalogs, addons, plugins, and metadata",
                isEnabled = config.enabled,
                hasConfiguration = false,
            ),
        )

        if (!config.enabled) return items

        items.addAll(
            listOf(
                AnimePluginItem(
                    id = "tmdb_metadata",
                    name = "TMDB Metadata",
                    description = "Use TMDB for anime metadata",
                    isEnabled = config.tmdbConfig.enabled,
                    hasConfiguration = true,
                ),
                AnimePluginItem(
                    id = "anilist_metadata",
                    name = "AniList Metadata",
                    description = "Use AniList for anime metadata and library",
                    isEnabled = config.useAnilistMetadata,
                    hasConfiguration = false,
                ),
                AnimePluginItem(
                    id = "mal_metadata",
                    name = "MyAnimeList Metadata",
                    description = "Use MAL for anime metadata",
                    isEnabled = config.useMalMetadata,
                    hasConfiguration = false,
                ),
                AnimePluginItem(
                    id = "kitsu_metadata",
                    name = "Kitsu Metadata",
                    description = "Use Kitsu for anime metadata",
                    isEnabled = config.useKitsuMetadata,
                    hasConfiguration = false,
                ),
            ),
        )

        val soraState = soraPluginRepository.uiState.value
        if (soraState.soraEnabled) {
            items.add(
                AnimePluginItem(
                    id = "sora_modules",
                    name = "Sora Modules",
                    description = "${soraState.modules.size} modules from ${soraState.repositories.size} repositories",
                    isEnabled = soraState.modules.any { it.enabled },
                    hasConfiguration = true,
                ),
            )
        }

        val pluginState = pluginRepository.uiState.value
        val enabledPluginRepos = config.enabledPluginRepos
        for (repo in pluginState.repositories) {
            items.add(
                AnimePluginItem(
                    id = "plugin_repo:${repo.manifestUrl}",
                    name = repo.name,
                    description = "${repo.scraperCount} providers",
                    isEnabled = enabledPluginRepos.isEmpty() || enabledPluginRepos.contains(repo.manifestUrl),
                    hasConfiguration = false,
                ),
            )
        }

        return items
    }

    fun updatePluginStatusInSandbox(pluginId: String, isEnabled: Boolean) {
        val config = animeProfileRepository.state.value.config

        when {
            pluginId == "anime_profile_toggle" -> {
                animeProfileRepository.setEnabled(isEnabled)
            }
            pluginId == "tmdb_metadata" -> {
                animeProfileRepository.setTmdbConfig(config.tmdbConfig.copy(enabled = isEnabled))
            }
            pluginId == "anilist_metadata" -> {
                animeProfileRepository.setUseAnilist(isEnabled)
            }
            pluginId == "mal_metadata" -> {
                animeProfileRepository.setUseMal(isEnabled)
            }
            pluginId == "kitsu_metadata" -> {
                animeProfileRepository.setUseKitsu(isEnabled)
            }
            pluginId == "sora_modules" -> {
                soraPluginRepository.toggleSoraEnabled(isEnabled)
            }
            pluginId.startsWith("plugin_repo:") -> {
                val repoUrl = pluginId.removePrefix("plugin_repo:")
                val currentRepos = config.enabledPluginRepos.toMutableList()
                if (isEnabled) {
                    if (repoUrl !in currentRepos) currentRepos.add(repoUrl)
                } else {
                    currentRepos.remove(repoUrl)
                }
                animeProfileRepository.setEnabledPluginRepos(currentRepos)
            }
        }
    }
}
