package com.nuvio.app.features.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DnsProvider(val displayName: String) {
    SYSTEM("System Default (IPv4 Preferred)"),
    CLOUDFLARE("Cloudflare (DoH)"),
    GOOGLE("Google (DoH)"),
    QUAD9("Quad9 (DoH)"),
    ADGUARD("AdGuard (DoH)"),
    NEXTDNS("NextDNS (DoH)"),
    MULLVAD("Mullvad (DoH)"),
    OPEN_DNS("OpenDNS (DoH)")
}

interface NetworkSettingsStorage {
    fun getDnsProvider(): String?
    fun setDnsProvider(provider: String)
    fun getCustomUserAgent(): String?
    fun setCustomUserAgent(value: String)
    fun getOverrideForAddons(): Boolean
    fun setOverrideForAddons(enabled: Boolean)
    fun getOverrideForPlugins(): Boolean
    fun setOverrideForPlugins(enabled: Boolean)
    fun getOverrideForBoth(): Boolean
    fun setOverrideForBoth(enabled: Boolean)
    fun getProxyEnabled(): Boolean
    fun setProxyEnabled(enabled: Boolean)
    fun getProxyUrl(): String?
    fun setProxyUrl(url: String)
    fun getProxyPassword(): String?
    fun setProxyPassword(password: String)
}

class NetworkSettingsRepository(
    private val storage: NetworkSettingsStorage
) {
    private val _dnsProvider = MutableStateFlow(
        runCatching {
            val name = storage.getDnsProvider() ?: DnsProvider.SYSTEM.name
            DnsProvider.valueOf(name)
        }.getOrDefault(DnsProvider.SYSTEM)
    )
    val dnsProvider: StateFlow<DnsProvider> = _dnsProvider.asStateFlow()

    private val _customUserAgent = MutableStateFlow(storage.getCustomUserAgent() ?: "")
    val customUserAgent: StateFlow<String> = _customUserAgent.asStateFlow()

    private val _overrideForAddons = MutableStateFlow(storage.getOverrideForAddons())
    val overrideForAddons: StateFlow<Boolean> = _overrideForAddons.asStateFlow()

    private val _overrideForPlugins = MutableStateFlow(storage.getOverrideForPlugins())
    val overrideForPlugins: StateFlow<Boolean> = _overrideForPlugins.asStateFlow()

    private val _overrideForBoth = MutableStateFlow(storage.getOverrideForBoth())
    val overrideForBoth: StateFlow<Boolean> = _overrideForBoth.asStateFlow()

    private val _proxyEnabled = MutableStateFlow(storage.getProxyEnabled())
    val proxyEnabled: StateFlow<Boolean> = _proxyEnabled.asStateFlow()

    private val _proxyUrl = MutableStateFlow(storage.getProxyUrl() ?: "")
    val proxyUrl: StateFlow<String> = _proxyUrl.asStateFlow()

    private val _proxyPassword = MutableStateFlow(storage.getProxyPassword() ?: "")
    val proxyPassword: StateFlow<String> = _proxyPassword.asStateFlow()

    fun setDnsProvider(provider: DnsProvider) {
        storage.setDnsProvider(provider.name)
        _dnsProvider.value = provider
    }

    fun setCustomUserAgent(value: String) {
        storage.setCustomUserAgent(value)
        _customUserAgent.value = value
    }

    fun setOverrideForAddons(enabled: Boolean) {
        if (enabled) {
            storage.setOverrideForPlugins(false)
            storage.setOverrideForBoth(false)
            _overrideForPlugins.value = false
            _overrideForBoth.value = false
        }
        storage.setOverrideForAddons(enabled)
        _overrideForAddons.value = enabled
    }

    fun setOverrideForPlugins(enabled: Boolean) {
        if (enabled) {
            storage.setOverrideForAddons(false)
            storage.setOverrideForBoth(false)
            _overrideForAddons.value = false
            _overrideForBoth.value = false
        }
        storage.setOverrideForPlugins(enabled)
        _overrideForPlugins.value = enabled
    }

    fun setOverrideForBoth(enabled: Boolean) {
        if (enabled) {
            storage.setOverrideForAddons(false)
            storage.setOverrideForPlugins(false)
            _overrideForAddons.value = false
            _overrideForPlugins.value = false
        }
        storage.setOverrideForBoth(enabled)
        _overrideForBoth.value = enabled
    }

    fun setProxyEnabled(enabled: Boolean) {
        storage.setProxyEnabled(enabled)
        _proxyEnabled.value = enabled
    }

    fun setProxyUrl(url: String) {
        storage.setProxyUrl(url)
        _proxyUrl.value = url
    }

    fun setProxyPassword(password: String) {
        storage.setProxyPassword(password)
        _proxyPassword.value = password
    }

    fun buildProxyUrl(target: String): String {
        val base = _proxyUrl.value.trimEnd('/')
        val encoded = encodeUrlComponent(target)
        val password = _proxyPassword.value
        return if (password.isBlank()) {
            "$base/proxy/manifest.m3u8?url=$encoded"
        } else {
            "$base/proxy/manifest.m3u8?url=$encoded&api_password=$password"
        }
    }

    private fun encodeUrlComponent(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '~' -> sb.append(ch)
                ' ' -> sb.append("%20")
                '/' -> sb.append("%2F")
                ':' -> sb.append("%3A")
                '?' -> sb.append("%3F")
                '&' -> sb.append("%26")
                '=' -> sb.append("%3D")
                '#' -> sb.append("%23")
                '@' -> sb.append("%40")
                '%' -> sb.append("%25")
                '+' -> sb.append("%2B")
                ',' -> sb.append("%2C")
                ';' -> sb.append("%3B")
                '\'' -> sb.append("%27")
                '"' -> sb.append("%22")
                '<' -> sb.append("%3C")
                '>' -> sb.append("%3E")
                '{' -> sb.append("%7B")
                '}' -> sb.append("%7D")
                '|' -> sb.append("%7C")
                '\\' -> sb.append("%5C")
                '^' -> sb.append("%5E")
                '`' -> sb.append("%60")
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) sb.append("%${b.toUByte().toString(16).uppercase().padStart(2, '0')}")
                }
            }
        }
        return sb.toString()
    }
}

var globalNetworkSettingsRepository: NetworkSettingsRepository? = null
