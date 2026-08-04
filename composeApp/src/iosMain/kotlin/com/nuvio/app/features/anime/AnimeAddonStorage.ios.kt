package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object AnimeAddonStorage {
    private const val installedAddonUrlsKey = "anime_installed_manifest_urls"
    private const val addonEnabledStatesKey = "anime_installed_manifest_enabled_states"

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> =
        NSUserDefaults.standardUserDefaults
            .stringForKey(ProfileScopedKey.of(installedAddonUrlsKey, profileId))
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        NSUserDefaults.standardUserDefaults.setObject(
            urls.joinToString(separator = "\n"),
            forKey = ProfileScopedKey.of(installedAddonUrlsKey, profileId),
        )
    }

    actual fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean> {
        val raw = NSUserDefaults.standardUserDefaults
            .stringForKey(ProfileScopedKey.of(addonEnabledStatesKey, profileId))
            .orEmpty()
        return raw
            .lineSequence()
            .mapNotNull(::parseAnimeEnabledStateLine)
            .toMap()
    }

    actual fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>) {
        val payload = states.entries.joinToString(separator = "\n") { (url, enabled) ->
            "$url\t$enabled"
        }
        NSUserDefaults.standardUserDefaults.setObject(
            payload,
            forKey = ProfileScopedKey.of(addonEnabledStatesKey, profileId),
        )
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