package com.nuvio.app.features.mal

internal expect object MalAuthStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
    fun loadCodeVerifier(): String?
    fun saveCodeVerifier(value: String?)
}
