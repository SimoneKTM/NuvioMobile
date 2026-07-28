package com.nuvio.app.features.anime.tvdb

data class AnimeTvdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val language: String = "en",
    val useTrailers: Boolean = true,
    val useArtwork: Boolean = true,
    val useBasicInfo: Boolean = true,
    val useCredits: Boolean = true,
    val useEpisodes: Boolean = true,
    val useSeasonPosters: Boolean = true,
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
