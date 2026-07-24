package com.nuvio.app.features.vezie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VeezieChannel(
    val series: String? = null,
    val movies: String? = null,
    val other: String? = null,
)

@Serializable
data class VeezieSeriesList(
    val items: List<VeezieSeriesItem> = emptyList(),
    val next: String? = null,
)

@Serializable
data class VeezieSeriesItem(
    val title: String,
    val url: String,
    @SerialName("isSerieTv") val isSerieTv: Boolean = true,
    val img: String? = null,
)

@Serializable
data class VeezieMovieList(
    val items: List<VeezieMovieItem> = emptyList(),
    val next: String? = null,
)

@Serializable
data class VeezieMovieItem(
    val title: String,
    val links: String = "",
    val img: String? = null,
)

@Serializable
data class VeezieOtherList(
    val items: List<VeezieOtherItem> = emptyList(),
    val next: String? = null,
)

@Serializable
data class VeezieOtherItem(
    val title: String,
    val links: String = "",
    val img: String? = null,
)

data class VeezieChannelConfig(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val useTmdbMetadata: Boolean = true,
    val seriesUrl: String? = null,
    val moviesUrl: String? = null,
    val otherUrl: String? = null,
)

data class VeezieChannelState(
    val channels: List<VeezieChannelConfig> = emptyList(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
)

data class VeezieParsedContent(
    val title: String,
    val type: VeezieContentType,
    val links: List<String> = emptyList(),
    val imageUrl: String? = null,
    val seriesUrl: String? = null,
    val isSeries: Boolean = false,
)

enum class VeezieContentType {
    MOVIE,
    SERIES,
    OTHER,
}
