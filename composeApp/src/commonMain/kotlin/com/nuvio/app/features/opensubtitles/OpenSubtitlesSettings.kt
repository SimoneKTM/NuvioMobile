package com.nuvio.app.features.opensubtitles

data class OpenSubtitlesSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val userToken: String = "",
    val languages: Set<String> = emptySet(),
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
    val hasUserCredentials: Boolean
        get() = username.isNotBlank() && password.isNotBlank()
}
