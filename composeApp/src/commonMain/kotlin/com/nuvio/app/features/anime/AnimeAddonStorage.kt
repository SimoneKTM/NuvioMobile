package com.nuvio.app.features.anime

internal expect object AnimeAddonStorage {
    fun loadInstalledAddonUrls(): List<String>
    fun saveInstalledAddonUrls(urls: List<String>)
    fun loadAddonEnabledStates(): Map<String, Boolean>
    fun saveAddonEnabledStates(states: Map<String, Boolean>)
}
