package com.nuvio.app.features.anime

internal expect object AnimeCollectionStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
}
