package com.nuvio.app.features.collection

import kotlinx.coroutines.flow.StateFlow

interface CollectionRepositoryContract {
    val collections: StateFlow<List<Collection>>
    fun ensureLoaded()
    fun getCollection(id: String): Collection?
    fun addCollection(collection: Collection)
    fun updateCollection(collection: Collection)
    fun removeCollection(id: String)
    fun setCollections(collections: List<Collection>)
    fun moveByIndex(fromIndex: Int, toIndex: Int)
    fun exportToJson(): String
    fun validateJson(jsonString: String): ValidationResult
    fun importFromJson(jsonString: String): Result<List<Collection>>
    fun generateId(): String
    fun getAvailableCatalogs(): List<AvailableCatalog>
}
