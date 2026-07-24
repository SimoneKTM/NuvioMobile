package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.streams.epochMs
import com.nuvio.app.features.tmdb.TmdbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object VeezieChannelRepository {
    private val log = Logger.withTag("VeezieChannelRepository")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _state = MutableStateFlow(VeezieChannelState())
    val state: StateFlow<VeezieChannelState> = _state.asStateFlow()

    private val channelStore = mutableListOf<VeezieChannelConfig>()

    private const val PREFS_KEY = "vezie_channels"
    private const val SERVER_URL_KEY = "veezie_server_url"

    private var serverUrl: String? = null

    fun getServerUrl(): String? = serverUrl

    fun setServerUrl(url: String?) {
        serverUrl = url
        VeezieStorage.saveServerUrl(url ?: "")
    }

    fun initialize() {
        if (_state.value.isLoaded) return
        serverUrl = VeezieStorage.loadServerUrl().takeIf { it.isNotBlank() }
        loadEasyProxy()
        loadChannels()
        _state.value = _state.value.copy(isLoaded = true)
    }

    private fun loadEasyProxy() {
        val epUrl = VeezieStorage.loadEasyProxyUrl().takeIf { it.isNotBlank() } ?: return
        val epEmail = VeezieStorage.loadEasyProxyEmail().takeIf { it.isNotBlank() } ?: return
        val epPassword = VeezieStorage.loadEasyProxyPassword().takeIf { it.isNotBlank() } ?: return
        VeezieEasyProxy.configure(epUrl, epEmail, epPassword)
    }

    fun addChannel(name: String, url: String): VeezieChannelConfig {
        val channel = VeezieChannelConfig(
            id = "vezie_${channelStore.size}_${epochMs()}",
            name = name,
            url = url,
        )
        channelStore.add(channel)
        saveChannels()
        refreshState()
        return channel
    }

    fun removeChannel(channelId: String) {
        channelStore.removeAll { it.id == channelId }
        saveChannels()
        refreshState()
    }

    fun toggleChannel(channelId: String, enabled: Boolean) {
        val index = channelStore.indexOfFirst { it.id == channelId }
        if (index >= 0) {
            channelStore[index] = channelStore[index].copy(enabled = enabled)
            saveChannels()
            refreshState()
        }
    }

    fun updateChannel(channelId: String, config: VeezieChannelConfig) {
        val index = channelStore.indexOfFirst { it.id == channelId }
        if (index >= 0) {
            channelStore[index] = config
            saveChannels()
            refreshState()
        }
    }

    fun getEnabledChannels(): List<VeezieChannelConfig> =
        channelStore.filter { it.enabled }

    suspend fun fetchChannelContent(channel: VeezieChannelConfig): VeezieChannel? = withContext(Dispatchers.Default) {
        try {
            val payload = httpGetText(channel.url)
            json.decodeFromString(payload)
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch channel: ${channel.name}" }
            null
        }
    }

    suspend fun fetchSeriesList(url: String): VeezieSeriesList? = withContext(Dispatchers.Default) {
        try {
            val payload = httpGetText(url)
            json.decodeFromString(payload)
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch series list" }
            null
        }
    }

    suspend fun fetchMovieList(url: String): VeezieMovieList? = withContext(Dispatchers.Default) {
        try {
            val payload = httpGetText(url)
            json.decodeFromString(payload)
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch movie list" }
            null
        }
    }

    suspend fun fetchOtherList(url: String): VeezieOtherList? = withContext(Dispatchers.Default) {
        try {
            val payload = httpGetText(url)
            json.decodeFromString(payload)
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch other list" }
            null
        }
    }

    suspend fun resolveTmdbForTitle(
        title: String,
        isSeries: Boolean,
    ): Pair<String?, String?> = withContext(Dispatchers.Default) {
        try {
            val mediaType = if (isSeries) "tv" else "movie"
            val searchResult = TmdbService.search(title, mediaType)
            val result = searchResult.firstOrNull()
            if (result != null) {
                result.id to mediaType
            } else {
                null to null
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to resolve TMDB for: $title" }
            null to null
        }
    }

    fun parseLinks(links: String): List<String> {
        return links.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun clear() {
        channelStore.clear()
        _state.value = VeezieChannelState()
    }

    private fun loadChannels() {
        val raw = VeezieStorage.load()
        if (raw.isNullOrBlank()) return
        try {
            val items = json.decodeFromString<List<VeezieChannelConfig>>(raw)
            channelStore.clear()
            channelStore.addAll(items)
            refreshState()
        } catch (e: Exception) {
            log.e(e) { "Failed to load channels" }
        }
    }

    private fun saveChannels() {
        try {
            VeezieStorage.save(json.encodeToString(channelStore.toList()))
        } catch (e: Exception) {
            log.e(e) { "Failed to save channels" }
        }
    }

    private fun refreshState() {
        _state.value = _state.value.copy(channels = channelStore.toList())
    }
}

internal expect object VeezieStorage {
    fun load(): String?
    fun save(data: String)
    fun loadServerUrl(): String
    fun saveServerUrl(url: String)
    fun loadEasyProxyUrl(): String
    fun saveEasyProxyUrl(url: String)
    fun loadEasyProxyEmail(): String
    fun saveEasyProxyEmail(email: String)
    fun loadEasyProxyPassword(): String
    fun saveEasyProxyPassword(password: String)
}


