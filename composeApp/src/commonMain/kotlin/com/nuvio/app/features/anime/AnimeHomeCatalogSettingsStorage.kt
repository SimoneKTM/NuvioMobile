package com.nuvio.app.features.anime

internal expect object AnimeHomeCatalogSettingsStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
}
