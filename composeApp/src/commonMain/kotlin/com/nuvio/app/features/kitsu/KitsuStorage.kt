package com.nuvio.app.features.kitsu

internal expect object KitsuStorage {
    fun loadAuthPayload(): String?
    fun saveAuthPayload(payload: String)

    fun loadSettingsPayload(): String?
    fun saveSettingsPayload(payload: String)

    fun loadLibraryPayload(): String?
    fun saveLibraryPayload(payload: String)

    fun loadMenuPrefsPayload(): String?
    fun saveMenuPrefsPayload(payload: String)
}
