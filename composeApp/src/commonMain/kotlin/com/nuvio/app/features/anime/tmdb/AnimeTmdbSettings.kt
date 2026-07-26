package com.nuvio.app.features.anime.tmdb

import com.nuvio.app.features.tmdb.TmdbSettings

data class AnimeTmdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val language: String = "en",
    val useTrailers: Boolean = true,
    val useArtwork: Boolean = true,
    val useBasicInfo: Boolean = true,
    val useDetails: Boolean = true,
    val useCredits: Boolean = true,
    val useProductions: Boolean = true,
    val useNetworks: Boolean = true,
    val useEpisodes: Boolean = true,
    val useSeasonPosters: Boolean = true,
    val useMoreLikeThis: Boolean = true,
    val useCollections: Boolean = true,
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    fun toTmdbSettings(): TmdbSettings = TmdbSettings(
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
