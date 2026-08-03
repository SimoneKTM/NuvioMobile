package com.nuvio.app.features.settings

import com.nuvio.app.core.storage.DesktopStorage

class DesktopNetworkSettingsStorage : NetworkSettingsStorage {
    private val store = DesktopStorage.store("nuvio_network_settings")
    private val DNS_PROVIDER_KEY = "dns_provider"
    private val CUSTOM_USER_AGENT_KEY = "custom_user_agent"
    private val OVERRIDE_FOR_ADDONS_KEY = "override_for_addons"
    private val OVERRIDE_FOR_PLUGINS_KEY = "override_for_plugins"
    private val OVERRIDE_FOR_BOTH_KEY = "override_for_both"

    override fun getDnsProvider(): String? =
        store.getString(DNS_PROVIDER_KEY)

    override fun setDnsProvider(provider: String) {
        store.putString(DNS_PROVIDER_KEY, provider)
    }

    override fun getCustomUserAgent(): String? =
        store.getString(CUSTOM_USER_AGENT_KEY)

    override fun setCustomUserAgent(value: String) {
        store.putString(CUSTOM_USER_AGENT_KEY, value)
    }

    override fun getOverrideForAddons(): Boolean =
        store.getBoolean(OVERRIDE_FOR_ADDONS_KEY) ?: false

    override fun setOverrideForAddons(enabled: Boolean) {
        store.putBoolean(OVERRIDE_FOR_ADDONS_KEY, enabled)
    }

    override fun getOverrideForPlugins(): Boolean =
        store.getBoolean(OVERRIDE_FOR_PLUGINS_KEY) ?: false

    override fun setOverrideForPlugins(enabled: Boolean) {
        store.putBoolean(OVERRIDE_FOR_PLUGINS_KEY, enabled)
    }

    override fun getOverrideForBoth(): Boolean =
        store.getBoolean(OVERRIDE_FOR_BOTH_KEY) ?: false

    override fun setOverrideForBoth(enabled: Boolean) {
        store.putBoolean(OVERRIDE_FOR_BOTH_KEY, enabled)
    }
}
