package com.nuvio.app.features.opensubtitles

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.decodeSyncStringSet
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.sync.encodeSyncStringSet
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

actual object OpenSubtitlesSettingsStorage {
    private const val enabledKey = "opensubtitles_enabled"
    private const val apiKeyKey = "opensubtitles_api_key"
    private const val usernameKey = "opensubtitles_username"
    private const val passwordKey = "opensubtitles_password"
    private const val userTokenKey = "opensubtitles_user_token"
    private const val languagesKey = "opensubtitles_languages"
    private val syncKeys = listOf(enabledKey, apiKeyKey, usernameKey, passwordKey, userTokenKey, languagesKey)

    actual fun loadEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val scopedKey = ProfileScopedKey.of(enabledKey)
        return if (defaults.objectForKey(scopedKey) != null) defaults.boolForKey(scopedKey) else null
    }

    actual fun saveEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(enabledKey))
    }

    actual fun loadApiKey(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(apiKeyKey))

    actual fun saveApiKey(apiKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(apiKey, forKey = ProfileScopedKey.of(apiKeyKey))
    }

    actual fun loadUsername(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(usernameKey))

    actual fun saveUsername(username: String) {
        NSUserDefaults.standardUserDefaults.setObject(username, forKey = ProfileScopedKey.of(usernameKey))
    }

    actual fun loadPassword(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(passwordKey))

    actual fun savePassword(password: String) {
        NSUserDefaults.standardUserDefaults.setObject(password, forKey = ProfileScopedKey.of(passwordKey))
    }

    actual fun loadUserToken(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(userTokenKey))

    actual fun saveUserToken(token: String) {
        NSUserDefaults.standardUserDefaults.setObject(token, forKey = ProfileScopedKey.of(userTokenKey))
    }

    actual fun loadLanguages(): Set<String>? {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(languagesKey)) ?: return null
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    actual fun saveLanguages(languages: Set<String>) {
        val raw = if (languages.isEmpty()) "" else languages.joinToString(",")
        NSUserDefaults.standardUserDefaults.setObject(raw, forKey = ProfileScopedKey.of(languagesKey))
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKeyKey, encodeSyncString(it)) }
        loadUsername()?.let { put(usernameKey, encodeSyncString(it)) }
        loadPassword()?.let { put(passwordKey, encodeSyncString(it)) }
        loadUserToken()?.let { put(userTokenKey, encodeSyncString(it)) }
        loadLanguages()?.let { put(languagesKey, encodeSyncStringSet(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        val defaults = NSUserDefaults.standardUserDefaults
        syncKeys.forEach { defaults.removeObjectForKey(ProfileScopedKey.of(it)) }

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKeyKey)?.let(::saveApiKey)
        payload.decodeSyncString(usernameKey)?.let(::saveUsername)
        payload.decodeSyncString(passwordKey)?.let(::savePassword)
        payload.decodeSyncString(userTokenKey)?.let(::saveUserToken)
        payload.decodeSyncStringSet(languagesKey)?.let(::saveLanguages)
    }
}
