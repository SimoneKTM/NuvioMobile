package com.nuvio.app.features.anime

import android.content.Context
import android.content.SharedPreferences

actual object AnimeAddonStorage {
    private const val preferencesName = "nuvio_anime_addons"
    private const val addonUrlsKey = "anime_installed_manifest_urls"
    private const val addonEnabledStatesKey = "anime_installed_manifest_enabled_states"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadInstalledAddonUrls(): List<String> =
        preferences
            ?.getString(addonUrlsKey, null)
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    actual fun saveInstalledAddonUrls(urls: List<String>) {
        preferences
            ?.edit()
            ?.putString(addonUrlsKey, urls.joinToString(separator = "\n"))
            ?.apply()
    }

    actual fun loadAddonEnabledStates(): Map<String, Boolean> =
        preferences
            ?.getString(addonEnabledStatesKey, null)
            .orEmpty()
            .lineSequence()
            .mapNotNull(::parseAnimeEnabledStateLine)
            .toMap()

    actual fun saveAddonEnabledStates(states: Map<String, Boolean>) {
        val payload = states.entries.joinToString(separator = "\n") { (url, enabled) ->
            "$url\t$enabled"
        }
        preferences
            ?.edit()
            ?.putString(addonEnabledStatesKey, payload)
            ?.apply()
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
