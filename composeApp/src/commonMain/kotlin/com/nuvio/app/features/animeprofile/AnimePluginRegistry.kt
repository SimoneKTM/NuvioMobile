package com.nuvio.app.features.animeprofile

import com.nuvio.app.features.plugins.PluginRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.anime_plugin_anilist_desc
import nuvio.composeapp.generated.resources.anime_plugin_kitsu_desc
import nuvio.composeapp.generated.resources.anime_plugin_mal_desc
import nuvio.composeapp.generated.resources.anime_plugin_profile_desc
import nuvio.composeapp.generated.resources.anime_plugin_providers
import nuvio.composeapp.generated.resources.anime_plugin_tmdb_desc
import org.jetbrains.compose.resources.getString

class AnimePluginRegistry(
    private val animeProfileRepository: AnimeProfileRepository = AnimeProfileRepository,
    private val pluginRepository: PluginRepository = PluginRepository,
) {
    suspend fun getAvailableAnimePluginsFromSandbox(): List<AnimePluginItem> {
        animeProfileRepository.initialize()
        pluginRepository.initialize()

        val config = animeProfileRepository.state.value.config

        val items = mutableListOf<AnimePluginItem>()

        items.add(
            AnimePluginItem(
                id = "anime_profile_toggle",
                name = "Anime Profile",
                description = getString(Res.string.anime_plugin_profile_desc),
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
                    description = getString(Res.string.anime_plugin_tmdb_desc),
                    isEnabled = config.tmdbConfig.enabled,
                    hasConfiguration = true,
                ),
                AnimePluginItem(
                    id = "anilist_metadata",
                    name = "AniList Metadata",
                    description = getString(Res.string.anime_plugin_anilist_desc),
                    isEnabled = config.useAnilistMetadata,
                    hasConfiguration = false,
                ),
                AnimePluginItem(
                    id = "mal_metadata",
                    name = "MyAnimeList Metadata",
                    description = getString(Res.string.anime_plugin_mal_desc),
                    isEnabled = config.useMalMetadata,
                    hasConfiguration = false,
                ),
                AnimePluginItem(
                    id = "kitsu_metadata",
                    name = "Kitsu Metadata",
                    description = getString(Res.string.anime_plugin_kitsu_desc),
                    isEnabled = config.useKitsuMetadata,
                    hasConfiguration = false,
                ),
            ),
        )

        val pluginState = pluginRepository.uiState.value
        val enabledPluginRepos = config.enabledPluginRepos
        for (repo in pluginState.repositories) {
            items.add(
                AnimePluginItem(
                    id = "plugin_repo:${repo.manifestUrl}",
                    name = repo.name,
                    description = getString(Res.string.anime_plugin_providers, repo.scraperCount),
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
