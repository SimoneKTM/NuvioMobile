package com.nuvio.app.features.anime

internal expect object AnimeCollectionStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
