package com.nuvio.app.features.artwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalArtwork(
    val id: String,
    val videoId: String,
    val mediaType: String,
    @SerialName("artwork_type")
    val artworkType: ArtworkType,
    @SerialName("local_path")
    val localPath: String,
    @SerialName("added_at")
    val addedAt: Long = 0L,
    @SerialName("is_active")
    val isActive: Boolean = true,
)

enum class ArtworkType(val value: String) {
    POSTER("poster"),
    BACKGROUND("background"),
    LOGO("logo"),
    THUMBNAIL("thumbnail"),
    CLEARART("clearart"),
    BANNER("banner");

    companion object {
        fun fromValue(value: String): ArtworkType =
            entries.find { it.value == value } ?: POSTER
    }
}

data class LocalArtworkState(
    val artworkMap: Map<String, List<LocalArtwork>> = emptyMap(),
    val isLoaded: Boolean = false,
    val currentSelection: LocalArtworkSelection? = null,
)

data class LocalArtworkSelection(
    val videoId: String,
    val mediaType: String,
    val currentArtwork: List<LocalArtwork> = emptyList(),
)

data class ArtworkPickResult(
    val wasSuccessful: Boolean = false,
    val localPath: String? = null,
    val errorMessage: String? = null,
)
