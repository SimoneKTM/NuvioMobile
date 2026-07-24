package com.nuvio.app.features.anime

internal expect object AnimePosterCardStyleStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
