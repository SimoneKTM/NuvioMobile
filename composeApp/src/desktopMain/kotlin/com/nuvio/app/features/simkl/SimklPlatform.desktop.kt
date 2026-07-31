package com.nuvio.app.features.simkl

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import java.security.MessageDigest
import java.security.SecureRandom

internal actual object SimklPlatformClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}

internal actual object SimklPkceCrypto {
    private val secureRandom = SecureRandom()

    actual fun secureRandomBytes(size: Int): ByteArray =
        ByteArray(size).also(secureRandom::nextBytes)

    actual fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)
}

internal actual object SimklAuthStorage {
    private const val METADATA_KEY = "simkl_auth_metadata"
    private const val ACCESS_TOKEN_KEY = "simkl_access_token"
    private const val CODE_VERIFIER_KEY = "simkl_code_verifier"

    private val store = DesktopStorage.store("nuvio_simkl_auth")

    actual fun loadMetadataPayload(): String? =
        store.getString(ProfileScopedKey.of(METADATA_KEY))

    actual fun saveMetadataPayload(payload: String) {
        store.putString(ProfileScopedKey.of(METADATA_KEY), payload)
    }

    actual fun loadAccessToken(): String? =
        store.getString(ProfileScopedKey.of(ACCESS_TOKEN_KEY))

    actual fun saveAccessToken(value: String?) {
        val key = ProfileScopedKey.of(ACCESS_TOKEN_KEY)
        if (value.isNullOrBlank()) store.remove(key) else store.putString(key, value)
    }

    actual fun loadCodeVerifier(): String? =
        store.getString(ProfileScopedKey.of(CODE_VERIFIER_KEY))

    actual fun saveCodeVerifier(value: String?) {
        val key = ProfileScopedKey.of(CODE_VERIFIER_KEY)
        if (value.isNullOrBlank()) store.remove(key) else store.putString(key, value)
    }

    actual fun removeProfile(profileId: Int) {
        store.remove(ProfileScopedKey.of(METADATA_KEY, profileId))
        store.remove(ProfileScopedKey.of(ACCESS_TOKEN_KEY, profileId))
        store.remove(ProfileScopedKey.of(CODE_VERIFIER_KEY, profileId))
    }
}
