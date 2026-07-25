package com.nuvio.app.features.anime

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.collection.AvailableCatalog
import com.nuvio.app.features.collection.Collection
import com.nuvio.app.features.collection.CollectionRepositoryContract
import com.nuvio.app.features.collection.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private data class StoredAnimeCollections(
    val collections: List<Collection> = emptyList(),
)

object AnimeCollectionRepository : CollectionRepositoryContract {
    private val log = Logger.withTag("AnimeCollectionRepository")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    override val collections: StateFlow<List<Collection>> = _collections.asStateFlow()

    private var hasLoaded = false

    fun clearLocalState() {
        hasLoaded = false
        _collections.value = emptyList()
    }

    override fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    override fun getCollection(id: String): Collection? =
        _collections.value.find { it.id == id }

    override fun addCollection(collection: Collection) {
        ensureLoaded()
        val current = _collections.value.toMutableList()
        current.add(collection)
        _collections.value = current
        persist()
    }

    override fun updateCollection(collection: Collection) {
        ensureLoaded()
        val current = _collections.value.toMutableList()
        val index = current.indexOfFirst { it.id == collection.id }
        if (index >= 0) {
            current[index] = collection
            _collections.value = current
            persist()
        }
    }

    override fun removeCollection(id: String) {
        ensureLoaded()
        val current = _collections.value.toMutableList()
        current.removeAll { it.id == id }
        _collections.value = current
        persist()
    }

    override fun setCollections(collections: List<Collection>) {
        ensureLoaded()
        _collections.value = collections
        persist()
    }

    override fun moveByIndex(fromIndex: Int, toIndex: Int) {
        ensureLoaded()
        val list = _collections.value.toMutableList()
        if (fromIndex == toIndex) return
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _collections.value = list
        persist()
    }

    override fun exportToJson(): String {
        ensureLoaded()
        return json.encodeToString(_collections.value)
    }

    override fun validateJson(jsonString: String): ValidationResult {
        if (jsonString.isBlank()) {
            return ValidationResult(valid = false, error = "Empty JSON")
        }
        return try {
            val collections = json.decodeFromString<List<Collection>>(jsonString)
            ValidationResult(
                valid = true,
                collectionCount = collections.size,
                folderCount = collections.sumOf { it.folders.size },
            )
        } catch (e: Exception) {
            ValidationResult(valid = false, error = e.message)
        }
    }

    override fun importFromJson(jsonString: String): Result<List<Collection>> {
        return runCatching {
            val validation = validateJson(jsonString)
            if (!validation.valid) {
                throw IllegalArgumentException(validation.error.orEmpty())
            }
            val imported = json.decodeFromString<List<Collection>>(jsonString)
            _collections.value = imported
            persist()
            imported
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun generateId(): String = Uuid.random().toString()

    override fun getAvailableCatalogs(): List<AvailableCatalog> {
        val addons = AddonRepository.uiState.value.addons.enabledAddons()
        return addons.mapNotNull { addon ->
            val manifest = addon.manifest ?: return@mapNotNull null
            addon to manifest
        }.flatMap { (addon, manifest) ->
            manifest.catalogs
                .filter { catalog -> catalog.extra.none { it.isRequired && it.name != "genre" } }
                .map { catalog ->
                    val genreExtra = catalog.extra.firstOrNull { it.name == "genre" }
                    AvailableCatalog(
                        addonId = manifest.id,
                        addonName = addon.displayTitle,
                        type = catalog.type,
                        catalogId = catalog.id,
                        catalogName = catalog.name,
                        genreOptions = genreExtra?.options.orEmpty(),
                        genreRequired = genreExtra?.isRequired == true,
                    )
                }
        }
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
