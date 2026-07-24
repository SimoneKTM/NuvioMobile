package com.nuvio.app.features.anime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.nuvio.app.features.collection.Collection

@Serializable
private data class StoredAnimeCollections(
    val collections: List<Collection> = emptyList(),
)

object AnimeCollectionRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _collections.value = emptyList()
    }

    fun addCollection(collection: Collection) {
        ensureLoaded()
        val current = _collections.value.toMutableList()
        current.add(collection)
        _collections.value = current
        persist()
    }

    fun updateCollection(collection: Collection) {
        ensureLoaded()
        val current = _collections.value.toMutableList()
        val index = current.indexOfFirst { it.id == collection.id }
        if (index >= 0) {
            current[index] = collection
            _collections.value = current
            persist()
        }
    }

    fun removeCollection(id: String) {
        ensureLoaded()
        val current = _collections.value.toMutableList()
        current.removeAll { it.id == id }
        _collections.value = current
        persist()
    }

    fun setCollections(collections: List<Collection>) {
        ensureLoaded()
        _collections.value = collections
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = AnimeCollectionStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) {
            _collections.value = emptyList()
            return
        }
        val stored = runCatching {
            json.decodeFromString<StoredAnimeCollections>(payload)
        }.getOrNull()
        _collections.value = stored?.collections ?: emptyList()
    }

    private fun persist() {
        AnimeCollectionStorage.savePayload(
            json.encodeToString(
                StoredAnimeCollections(
                    collections = _collections.value,
                ),
            ),
        )
    }
}
