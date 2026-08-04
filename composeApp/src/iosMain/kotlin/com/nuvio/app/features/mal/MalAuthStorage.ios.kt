package com.nuvio.app.features.mal

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object MalAuthStorage {
    private const val payloadKey = "mal_auth_payload"
    private const val codeVerifierKey = "mal_code_verifier"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    actual fun loadCodeVerifier(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(codeVerifierKey))

    actual fun saveCodeVerifier(value: String?) {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = ProfileScopedKey.of(codeVerifierKey)
        if (value.isNullOrBlank()) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
    }
}
