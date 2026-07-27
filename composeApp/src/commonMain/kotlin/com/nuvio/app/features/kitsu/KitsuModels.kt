package com.nuvio.app.features.kitsu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuAuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val userId: Long? = null,
    val tokenExpiresAtEpochMs: Long? = null
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}

enum class KitsuConnectionMode {
    CONNECTED, DISCONNECTED, LOADING
}

data class KitsuAuthUiState(
    val mode: KitsuConnectionMode = KitsuConnectionMode.DISCONNECTED,
    val username: String? = null,
    val avatarUrl: String? = null,
    val tokenExpiresAtEpochMs: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class KitsuSectionSettings(
    val type: String,
    val enabled: Boolean = true
)

@Serializable
data class KitsuSettingsUiState(
    val enableSync: Boolean = false,
    val syncWatching: Boolean = true,
    val autoSync: Boolean = true,
    val syncOnLaunch: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val librarySections: List<KitsuSectionSettings> = defaultLibrarySections,
    val markWatchedThreshold: Float = 0.90f,
    val autoAddNewAnime: Boolean = false
) {
    companion object {
        val defaultLibrarySections = listOf(
            KitsuSectionSettings("In Corso", true),
            KitsuSectionSettings("Completato", true),
            KitsuSectionSettings("Pianificato", true),
            KitsuSectionSettings("In Pausa", true),
            KitsuSectionSettings("Abbandonato", true),
        )
    }
}

@Serializable
data class KitsuLibraryItem(
    val id: Long,
    val kitsuMediaId: Long,
    val title: String,
    val posterUrl: String? = null,
    val progress: Int = 0,
    val totalEpisodes: Int? = null,
    val rating: Double? = null,
    val status: String,
    val updatedAt: String? = null,
    val entryId: String? = null,
    val synopsis: String? = null,
    val startDate: String? = null,
    val nextEpisodeAtEpochMs: Long? = null,
)

data class KitsuLibraryUiState(
    val current: List<KitsuLibraryItem> = emptyList(),
    val completed: List<KitsuLibraryItem> = emptyList(),
    val planned: List<KitsuLibraryItem> = emptyList(),
    val onHold: List<KitsuLibraryItem> = emptyList(),
    val dropped: List<KitsuLibraryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class KitsuOAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("created_at") val createdAt: Long? = null
)

@Serializable
data class KitsuApiUserResponse(
    val data: List<KitsuUserData>? = null
)

@Serializable
data class KitsuUserData(
    val id: String? = null,
    val attributes: KitsuUserAttributes? = null
)

@Serializable
data class KitsuUserAttributes(
    val name: String? = null,
    val slug: String? = null,
    val avatar: KitsuUserAvatar? = null
)

@Serializable
data class KitsuUserAvatar(
    val large: String? = null,
    val medium: String? = null,
    val small: String? = null
)

@Serializable
data class KitsuLibraryEntriesResponse(
    val data: List<KitsuLibraryEntryData> = emptyList(),
    val included: List<KitsuIncludedResource>? = null,
    val links: KitsuPaginationLinks? = null
)

@Serializable
data class KitsuLibraryEntryData(
    val id: String,
    val type: String = "library-entries",
    val attributes: KitsuLibraryEntryAttributes? = null,
    val relationships: KitsuLibraryEntryRelationships? = null
)

@Serializable
data class KitsuLibraryEntryAttributes(
    val status: String? = null,
    val progress: Int? = null,
    val rating: Double? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("progressed_at") val progressedAt: String? = null
)

@Serializable
data class KitsuLibraryEntryRelationships(
    val anime: KitsuRelationship? = null,
    val media: KitsuRelationship? = null
)

@Serializable
data class KitsuRelationship(
    val data: KitsuRelationshipData? = null
)

@Serializable
data class KitsuRelationshipData(
    val id: String,
    val type: String
)

@Serializable
data class KitsuIncludedResource(
    val id: String,
    val type: String,
    val attributes: KitsuIncludedAttributes? = null
)

@Serializable
data class KitsuIncludedAttributes(
    val slug: String? = null,
    val synopsis: String? = null,
    @SerialName("episodeCount") val episodeCount: Int? = null,
    @SerialName("posterImage") val posterImage: KitsuPosterImage? = null,
    val titles: KitsuTitles? = null,
    @SerialName("averageRating") val averageRating: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("endDate") val endDate: String? = null,
    val status: String? = null,
    @SerialName("subtype") val subtype: String? = null
)

@Serializable
data class KitsuPosterImage(
    val tiny: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val original: String? = null
)

@Serializable
data class KitsuTitles(
    val en: String? = null,
    @SerialName("en_jp") val enJp: String? = null,
    @SerialName("ja_jp") val jaJp: String? = null,
    val canonical: String? = null
)

@Serializable
data class KitsuPaginationLinks(
    val first: String? = null,
    val next: String? = null,
    val last: String? = null
)

@Serializable
data class KitsuCreateLibraryEntryRequest(
    val data: KitsuCreateLibraryEntryData
)

@Serializable
data class KitsuCreateLibraryEntryData(
    val type: String = "library-entries",
    val attributes: KitsuLibraryEntryAttributes,
    val relationships: KitsuCreateLibraryRelationships
)

@Serializable
data class KitsuCreateLibraryRelationships(
    val media: KitsuRelationship,
    val user: KitsuRelationship
)

@Serializable
data class KitsuPatchLibraryEntryRequest(
    val data: KitsuPatchLibraryEntryData
)

@Serializable
data class KitsuPatchLibraryEntryData(
    val id: String,
    val type: String = "library-entries",
    val attributes: KitsuLibraryEntryAttributes
)

@Serializable
data class KitsuSingleLibraryEntryResponse(
    val data: KitsuLibraryEntryData? = null
)

enum class KitsuSortBy { LAST_UPDATED, RATING, TITLE, PROGRESS }

@Serializable
data class KitsuLibraryMenuPrefsState(
    val sortBy: KitsuSortBy = KitsuSortBy.LAST_UPDATED,
    val sortAscending: Boolean = false,
    val openByCatalogUrl: String? = null
)
