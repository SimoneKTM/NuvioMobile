package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

actual object AnimeAddonStorage {
    private val store = DesktopStorage.store("nuvio_anime_addons")
    private const val addonUrlsKey = "anime_installed_manifest_urls"
    private const val addonEnabledStatesKey = "anime_installed_manifest_enabled_states"

    actual fun loadInstalledAddonUrls(): List<String> =
        store.getString(addonUrlsKey)
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    actual fun saveInstalledAddonUrls(urls: List<String>) {
        store.putString(addonUrlsKey, urls.joinToString(separator = "\n"))
    }

    actual fun loadAddonEnabledStates(): Map<String, Boolean> =
        store.getString(addonEnabledStatesKey)
            .orEmpty()
            .lineSequence()
            .mapNotNull(::parseAnimeEnabledStateLine)
            .toMap()

    actual fun saveAddonEnabledStates(states: Map<String, Boolean>) {
        val payload = states.entries.joinToString(separator = "\n") { (url, enabled) ->
            "$url\t$enabled"
        }
        store.putString(addonEnabledStatesKey, payload)
    }
}

private fun parseAnimeEnabledStateLine(line: String): Pair<String, Boolean>? {
    val url = line.substringBefore("\t").trim().takeIf { it.isNotEmpty() } ?: return null
    val rawEnabled = line.substringAfter("\t", "true").trim().lowercase()
    val enabled = when (rawEnabled) {
        "false" -> false
        else -> true
    }
    return url to enabled
}
