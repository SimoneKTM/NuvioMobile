package com.nuvio.app.features.vezie

import platform.Foundation.NSUserDefaults

internal actual object VeezieStorage {
    private const val channelsKey = "nuvio_vezie_channels"
    private const val serverUrlKey = "nuvio_veezie_server_url"

    actual fun load(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(channelsKey)

    actual fun save(data: String) {
        NSUserDefaults.standardUserDefaults.setObject(data, forKey = channelsKey)
    }

    actual fun loadServerUrl(): String =
        NSUserDefaults.standardUserDefaults.stringForKey(serverUrlKey) ?: ""

    actual fun saveServerUrl(url: String) {
        NSUserDefaults.standardUserDefaults.setObject(url, forKey = serverUrlKey)
    }

    private const val easyProxyUrlKey = "nuvio_veezie_easyproxy_url"
    private const val easyProxyPasswordKey = "nuvio_veezie_easyproxy_password"

    actual fun loadEasyProxyUrl(): String =
        NSUserDefaults.standardUserDefaults.stringForKey(easyProxyUrlKey) ?: ""

    actual fun saveEasyProxyUrl(url: String) {
        NSUserDefaults.standardUserDefaults.setObject(url, forKey = easyProxyUrlKey)
    }

    actual fun loadEasyProxyPassword(): String =
        NSUserDefaults.standardUserDefaults.stringForKey(easyProxyPasswordKey) ?: ""

    actual fun saveEasyProxyPassword(password: String) {
        NSUserDefaults.standardUserDefaults.setObject(password, forKey = easyProxyPasswordKey)
    }
}
