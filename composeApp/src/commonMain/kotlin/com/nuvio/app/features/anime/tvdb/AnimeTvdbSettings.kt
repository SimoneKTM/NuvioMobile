package com.nuvio.app.features.anime.tvdb

data class AnimeTvdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
