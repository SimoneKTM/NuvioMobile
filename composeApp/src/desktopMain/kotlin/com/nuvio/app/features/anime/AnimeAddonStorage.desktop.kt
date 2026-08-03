package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

actual object AnimeAddonStorage {
    private val store = DesktopStorage.store("nuvio_anime_addons")
    private const val legacyAddonUrlsKey = "anime_installed_manifest_urls"
    private const val legacyAddonEnabledStatesKey = "anime_installed_manifest_enabled_states"
    private fun addonUrlsKey(profileId: Int) = "anime_installed_manifest_urls_$profileId"
    private fun addonEnabledStatesKey(profileId: Int) = "anime_installed_manifest_enabled_states_$profileId"

    init {
        migrateLegacyPayloadToProfileOne()
    }

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> =
        store.getString(addonUrlsKey(profileId))
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        store.putString(addonUrlsKey(profileId), urls.joinToString(separator = "\n"))
    }

    actual fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean> =
        store.getString(addonEnabledStatesKey(profileId))
            .orEmpty()
            .lineSequence()
            .mapNotNull(::parseAnimeEnabledStateLine)
            .toMap()

    actual fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>) {
        val payload = states.entries.joinToString(separator = "\n") { (url, enabled) ->
            "$url\t$enabled"
        }
        store.putString(addonEnabledStatesKey(profileId), payload)
    }

    private fun migrateLegacyPayloadToProfileOne() {
        val legacyUrls = store.getString(legacyAddonUrlsKey)
        val legacyStates = store.getString(legacyAddonEnabledStatesKey)
        if (legacyUrls != null && store.getString(addonUrlsKey(1)) == null) {
            store.putString(addonUrlsKey(1), legacyUrls)
        }
        if (legacyStates != null && store.getString(addonEnabledStatesKey(1)) == null) {
            store.putString(addonEnabledStatesKey(1), legacyStates)
        }
        store.remove(legacyAddonUrlsKey)
        store.remove(legacyAddonEnabledStatesKey)
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
