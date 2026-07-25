package com.nuvio.app.features.tvdb

data class TvdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
