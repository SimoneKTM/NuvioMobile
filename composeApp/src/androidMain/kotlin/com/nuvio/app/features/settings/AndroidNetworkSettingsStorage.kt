package com.nuvio.app.features.settings

import android.content.Context
import android.content.SharedPreferences

class AndroidNetworkSettingsStorage(context: Context) : NetworkSettingsStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences("nuvio_network_settings", Context.MODE_PRIVATE)
    private val DNS_PROVIDER_KEY = "dns_provider"
    private val CUSTOM_USER_AGENT_KEY = "custom_user_agent"
    private val OVERRIDE_FOR_ADDONS_KEY = "override_for_addons"
    private val OVERRIDE_FOR_PLUGINS_KEY = "override_for_plugins"
    private val OVERRIDE_FOR_BOTH_KEY = "override_for_both"
    private val PROXY_ENABLED_KEY = "proxy_enabled"
    private val PROXY_URL_KEY = "proxy_url"
    private val PROXY_PASSWORD_KEY = "proxy_password"

    override fun getDnsProvider(): String? =
        prefs.getString(DNS_PROVIDER_KEY, null)

    override fun setDnsProvider(provider: String) {
        prefs.edit().putString(DNS_PROVIDER_KEY, provider).apply()
    }

    override fun getCustomUserAgent(): String? =
        prefs.getString(CUSTOM_USER_AGENT_KEY, null)

    override fun setCustomUserAgent(value: String) {
        prefs.edit().putString(CUSTOM_USER_AGENT_KEY, value).apply()
    }

    override fun getOverrideForAddons(): Boolean =
        prefs.getBoolean(OVERRIDE_FOR_ADDONS_KEY, false)

    override fun setOverrideForAddons(enabled: Boolean) {
        prefs.edit().putBoolean(OVERRIDE_FOR_ADDONS_KEY, enabled).apply()
    }

    override fun getOverrideForPlugins(): Boolean =
        prefs.getBoolean(OVERRIDE_FOR_PLUGINS_KEY, false)

    override fun setOverrideForPlugins(enabled: Boolean) {
        prefs.edit().putBoolean(OVERRIDE_FOR_PLUGINS_KEY, enabled).apply()
    }

    override fun getOverrideForBoth(): Boolean =
        prefs.getBoolean(OVERRIDE_FOR_BOTH_KEY, false)

    override fun setOverrideForBoth(enabled: Boolean) {
        prefs.edit().putBoolean(OVERRIDE_FOR_BOTH_KEY, enabled).apply()
    }

    override fun getProxyEnabled(): Boolean =
        prefs.getBoolean(PROXY_ENABLED_KEY, false)

    override fun setProxyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PROXY_ENABLED_KEY, enabled).apply()
    }

    override fun getProxyUrl(): String? =
        prefs.getString(PROXY_URL_KEY, null)

    override fun setProxyUrl(url: String) {
        prefs.edit().putString(PROXY_URL_KEY, url).apply()
    }

    override fun getProxyPassword(): String? =
        prefs.getString(PROXY_PASSWORD_KEY, null)

    override fun setProxyPassword(password: String) {
        prefs.edit().putString(PROXY_PASSWORD_KEY, password).apply()
    }
}
