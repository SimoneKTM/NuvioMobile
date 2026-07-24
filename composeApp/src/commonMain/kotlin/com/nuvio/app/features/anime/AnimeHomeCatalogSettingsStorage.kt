package com.nuvio.app.features.anime

internal expect object AnimeHomeCatalogSettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
