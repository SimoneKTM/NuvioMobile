package com.nuvio.app.features.artwork

import co.touchlab.kermit.Logger
import com.nuvio.app.features.streams.epochMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LocalArtworkRepository {
    private val log = Logger.withTag("LocalArtworkRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _state = MutableStateFlow(LocalArtworkState())
    val state: StateFlow<LocalArtworkState> = _state.asStateFlow()

    private val artworkStore = mutableMapOf<String, MutableList<LocalArtwork>>()

    private const val PREFS_KEY = "local_artwork_store"

    fun initialize() {
        if (_state.value.isLoaded) return
        loadArtwork()
        _state.value = _state.value.copy(isLoaded = true)
    }

    fun getArtworkForVideo(videoId: String, mediaType: String): List<LocalArtwork> {
        val key = artworkKey(videoId, mediaType)
        return artworkStore[key]?.filter { it.isActive } ?: emptyList()
    }

    fun getArtworkByType(videoId: String, mediaType: String, type: ArtworkType): LocalArtwork? {
        return getArtworkForVideo(videoId, mediaType).find { it.artworkType == type }
    }

    fun addArtwork(
        videoId: String,
        mediaType: String,
        artworkType: ArtworkType,
        localPath: String,
    ) {
        val key = artworkKey(videoId, mediaType)
        val artworks = artworkStore.getOrPut(key) { mutableListOf() }

        artworks.removeAll { it.artworkType == artworkType }

        val artwork = LocalArtwork(
            id = "${videoId}_${artworkType.value}",
            videoId = videoId,
            mediaType = mediaType,
            artworkType = artworkType,
            localPath = localPath,
            addedAt = currentEpochMillis(),
            isActive = true,
        )
        artworks.add(artwork)
        saveArtwork()

        _state.update { state ->
            val newMap = state.artworkMap.toMutableMap()
            newMap[key] = artworks.toList()
            state.copy(artworkMap = newMap)
        }
    }

    fun removeArtwork(videoId: String, mediaType: String, artworkType: ArtworkType) {
        val key = artworkKey(videoId, mediaType)
        val artworks = artworkStore[key] ?: return
        artworks.removeAll { it.artworkType == artworkType }
        saveArtwork()

        _state.update { state ->
            val newMap = state.artworkMap.toMutableMap()
            newMap[key] = artworks.toList()
            state.copy(artworkMap = newMap)
        }
    }

    fun clearArtworkForVideo(videoId: String, mediaType: String) {
        val key = artworkKey(videoId, mediaType)
        artworkStore.remove(key)
        saveArtwork()

        _state.update { state ->
            val newMap = state.artworkMap.toMutableMap()
            newMap.remove(key)
            state.copy(artworkMap = newMap)
        }
    }

    suspend fun pickArtworkFromGallery(): ArtworkPickResult {
        return withContext(Dispatchers.Default) {
            try {
                val platformPath = PlatformArtworkPicker.pickImage()
                if (platformPath != null) {
                    ArtworkPickResult(wasSuccessful = true, localPath = platformPath)
                } else {
                    ArtworkPickResult(wasSuccessful = false, errorMessage = "No image selected")
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to pick artwork" }
                ArtworkPickResult(wasSuccessful = false, errorMessage = e.message)
            }
        }
    }

    private fun artworkKey(videoId: String, mediaType: String): String =
        "${mediaType}:${videoId}"

    private fun loadArtwork() {
        val raw = LocalArtworkStorage.load()
        if (raw.isNullOrBlank()) return
        try {
            val items = json.decodeFromString<List<LocalArtwork>>(raw)
            artworkStore.clear()
            for (item in items) {
                val key = artworkKey(item.videoId, item.mediaType)
                artworkStore.getOrPut(key) { mutableListOf() }.add(item)
            }
            val map = artworkStore.mapValues { it.value.toList() }
            _state.value = _state.value.copy(artworkMap = map)
        } catch (e: Exception) {
            log.e(e) { "Failed to load artwork" }
        }
    }

    private fun saveArtwork() {
        val allItems = artworkStore.values.flatten()
        try {
            LocalArtworkStorage.save(json.encodeToString(allItems))
        } catch (e: Exception) {
            log.e(e) { "Failed to save artwork" }
        }
    }

    private fun currentEpochMillis(): Long = epochMs()
}

internal object PlatformArtworkPicker {
    var pickImageImpl: (suspend () -> String?)? = null

    suspend fun pickImage(): String? {
        return pickImageImpl?.invoke()
    }
}

internal object LocalArtworkStorage {
    private var store: String? = null

    fun load(): String? = store

    fun save(data: String) {
        store = data
    }
}
