package com.nuvio.app.features.tvdb

data class TvdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
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
