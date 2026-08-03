package com.nuvio.app.features.anime

import android.content.Context
import android.content.SharedPreferences

actual object AnimeAddonStorage {
    private const val preferencesName = "nuvio_anime_addons"
    private const val legacyAddonUrlsKey = "anime_installed_manifest_urls"
    private const val legacyAddonEnabledStatesKey = "anime_installed_manifest_enabled_states"
    private fun addonUrlsKey(profileId: Int) = "anime_installed_manifest_urls_$profileId"
    private fun addonEnabledStatesKey(profileId: Int) = "anime_installed_manifest_enabled_states_$profileId"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        migrateLegacyPayloadToProfileOne()
    }

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> =
        preferences
            ?.getString(addonUrlsKey(profileId), null)
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        preferences
            ?.edit()
            ?.putString(addonUrlsKey(profileId), urls.joinToString(separator = "\n"))
            ?.apply()
    }

    actual fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean> =
        preferences
            ?.getString(addonEnabledStatesKey(profileId), null)
            .orEmpty()
            .lineSequence()
            .mapNotNull(::parseAnimeEnabledStateLine)
            .toMap()

    actual fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>) {
        val payload = states.entries.joinToString(separator = "\n") { (url, enabled) ->
            "$url\t$enabled"
        }
        preferences
            ?.edit()
            ?.putString(addonEnabledStatesKey(profileId), payload)
            ?.apply()
    }

    private fun migrateLegacyPayloadToProfileOne() {
        val prefs = preferences ?: return
        val legacyUrls = prefs.getString(legacyAddonUrlsKey, null)
        val legacyStates = prefs.getString(legacyAddonEnabledStatesKey, null)
        if (legacyUrls != null && prefs.getString(addonUrlsKey(1), null) == null) {
            prefs.edit().putString(addonUrlsKey(1), legacyUrls).apply()
        }
        if (legacyStates != null && prefs.getString(addonEnabledStatesKey(1), null) == null) {
            prefs.edit().putString(addonEnabledStatesKey(1), legacyStates).apply()
        }
        prefs.edit()
            .remove(legacyAddonUrlsKey)
            .remove(legacyAddonEnabledStatesKey)
            .apply()
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
