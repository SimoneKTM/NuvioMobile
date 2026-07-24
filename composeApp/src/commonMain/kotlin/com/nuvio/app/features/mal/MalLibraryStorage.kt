package com.nuvio.app.features.mal

internal expect object MalLibraryStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
    fun loadSettingsPayload(): String?
    fun saveSettingsPayload(payload: String)
}
