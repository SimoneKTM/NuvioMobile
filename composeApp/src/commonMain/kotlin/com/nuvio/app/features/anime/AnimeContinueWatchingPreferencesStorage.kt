package com.nuvio.app.features.anime

internal expect object AnimeContinueWatchingPreferencesStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
