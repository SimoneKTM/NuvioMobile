package com.nuvio.app.features.anime.metascreen

internal expect object AnimeMetaScreenSettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
