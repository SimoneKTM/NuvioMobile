package com.nuvio.app.features.sora

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SoraAuthor(
    val name: String,
    val icon: String? = null,
)

@Serializable
data class SoraManifest(
    @SerialName("sourceName") val sourceName: String,
    @SerialName("iconUrl") val iconUrl: String? = null,
    val author: SoraAuthor? = null,
    val version: String = "1.0.0",
    val language: String = "English",
    @SerialName("streamType") val streamType: String = "HLS",
    val quality: String = "720p",
    @SerialName("baseUrl") val baseUrl: String = "",
    @SerialName("searchBaseUrl") val searchBaseUrl: String = "",
    @SerialName("scriptUrl") val scriptUrl: String = "",
    @SerialName("asyncJS") val asyncJS: Boolean = false,
    @SerialName("streamAsyncJS") val streamAsyncJS: Boolean = false,
    val softsub: Boolean = false,
    val type: String = "anime",
)

data class SoraModule(
    val id: String,
    val manifest: SoraManifest,
    val scriptCode: String,
    val enabled: Boolean = true,
    val repositoryUrl: String = "",
)

data class SoraRepository(
    val id: String,
    val name: String,
    val url: String,
    val modules: List<SoraModule> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

data class SoraSearchResult(
    val title: String,
    val image: String = "",
    val href: String = "",
)

data class SoraDetailsResult(
    val description: String = "",
    val aliases: String = "",
    val airdate: String = "",
)

data class SoraEpisodeResult(
    val href: String,
    val number: String,
)

data class SoraStreamResult(
    val stream: String? = null,
    val subtitles: String? = null,
)

data class SoraUiState(
    val repositories: List<SoraRepository> = emptyList(),
    val modules: List<SoraModule> = emptyList(),
    val soraEnabled: Boolean = true,
    val isLoading: Boolean = false,
)

sealed interface SoraOperationResult {
    data class Success(val message: String = "") : SoraOperationResult
    data class Error(val message: String) : SoraOperationResult
}

internal fun soraTypeToMediaType(soraType: String): List<String> = when (soraType.lowercase()) {
    "anime" -> listOf("movie", "tv")
    "movies" -> listOf("movie")
    "shows" -> listOf("tv")
    "novels" -> listOf("tv")
    else -> listOf("movie", "tv")
}
