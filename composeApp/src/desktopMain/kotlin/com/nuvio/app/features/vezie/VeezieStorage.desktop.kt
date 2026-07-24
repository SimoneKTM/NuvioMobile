package com.nuvio.app.features.vezie

import com.nuvio.app.core.storage.DesktopStorage

internal actual object VeezieStorage {
    private val store = DesktopStorage.store("nuvio_vezie")

    actual fun load(): String? = store.getString("channels")

    actual fun save(data: String) {
        store.putString("channels", data)
    }

    actual fun loadServerUrl(): String =
        store.getString("server_url") ?: ""

    actual fun saveServerUrl(url: String) {
        store.putString("server_url", url)
    }

    actual fun loadEasyProxyUrl(): String =
        store.getString("easyproxy_url") ?: ""

    actual fun saveEasyProxyUrl(url: String) {
        store.putString("easyproxy_url", url)
    }

    actual fun loadEasyProxyPassword(): String =
        store.getString("easyproxy_password") ?: ""

    actual fun saveEasyProxyPassword(password: String) {
        store.putString("easyproxy_password", password)
    }
}
