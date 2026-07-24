package com.nuvio.app.features.animeprofile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeProfileConfig(
    val enabled: Boolean = false,
    val name: String = "Anime",
    val catalogSources: List<String> = emptyList(),
    val enabledAddons: List<String> = emptyList(),
    val enabledPluginRepos: List<String> = emptyList(),
    val metadataSource: String = "tmdb",
    val useAnilistMetadata: Boolean = true,
    val useMalMetadata: Boolean = false,
    val useKitsuMetadata: Boolean = false,
    val preferredLanguage: String = "sub",
    val excludeDubs: Boolean = false,
    val showOnHomeTab: Boolean = true,
    @SerialName("tmdb_config")
    val tmdbConfig: AnimeTmdbConfig = AnimeTmdbConfig(),
)

@Serializable
data class AnimeTmdbConfig(
    val enabled: Boolean = true,
    val preferAnimeMovieResults: Boolean = true,
    val preferAnimeTvResults: Boolean = true,
)

data class AnimeProfileState(
    val config: AnimeProfileConfig = AnimeProfileConfig(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
)

data class AnimeCatalogItem(
    val id: String,
    val name: String,
    val type: AnimeCatalogType,
    val enabled: Boolean = true,
)

enum class AnimeCatalogType {
    ADDON,
    PLUGIN,
    ANILIST,
    MAL,
    KITSU,
}

data class AnimePluginItem(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String? = null,
    val isEnabled: Boolean = false,
    val hasConfiguration: Boolean = false,
)

data class AnimeStreamResult(
    val url: String,
    val title: String,
    val quality: String? = null,
    val provider: String? = null,
    val language: String? = null,
    val isDub: Boolean = false,
    val isSoftSub: Boolean = false,
)
