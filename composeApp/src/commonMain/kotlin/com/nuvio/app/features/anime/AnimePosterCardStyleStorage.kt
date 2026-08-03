package com.nuvio.app.features.anime

internal expect object AnimePosterCardStyleStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
}
