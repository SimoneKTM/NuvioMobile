package com.nuvio.app.features.vezie

import android.content.Context
import android.content.SharedPreferences

internal actual object VeezieStorage {
    private const val preferencesName = "nuvio_vezie"
    private const val channelsKey = "channels"
    private const val serverUrlKey = "server_url"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun load(): String? =
        preferences?.getString(channelsKey, null)

    actual fun save(data: String) {
        preferences?.edit()?.putString(channelsKey, data)?.apply()
    }

    actual fun loadServerUrl(): String =
        preferences?.getString(serverUrlKey, "") ?: ""

    actual fun saveServerUrl(url: String) {
        preferences?.edit()?.putString(serverUrlKey, url)?.apply()
    }

    private const val easyProxyUrlKey = "easyproxy_url"
    private const val easyProxyEmailKey = "easyproxy_email"
    private const val easyProxyPasswordKey = "easyproxy_password"

    actual fun loadEasyProxyUrl(): String =
        preferences?.getString(easyProxyUrlKey, "") ?: ""

    actual fun saveEasyProxyUrl(url: String) {
        preferences?.edit()?.putString(easyProxyUrlKey, url)?.apply()
    }

    actual fun loadEasyProxyEmail(): String =
        preferences?.getString(easyProxyEmailKey, "") ?: ""

    actual fun saveEasyProxyEmail(email: String) {
        preferences?.edit()?.putString(easyProxyEmailKey, email)?.apply()
    }

    actual fun loadEasyProxyPassword(): String =
        preferences?.getString(easyProxyPasswordKey, "") ?: ""

    actual fun saveEasyProxyPassword(password: String) {
        preferences?.edit()?.putString(easyProxyPasswordKey, password)?.apply()
    }
}
