package com.nuvio.app.features.anime.tvdb

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJson
import com.nuvio.app.features.tvdb.TvdbSettingsRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object TvdbApi {
    private val log = Logger.withTag("TvdbApi")
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE_URL = "https://api4.thetvdb.com/v4"

    private var cachedToken: String? = null
    private var tokenApiKey: String? = null

    suspend fun ensureAuthenticated(apiKey: String? = null): String? {
        val resolvedKey = apiKey?.takeIf { it.isNotBlank() }
            ?: AnimeTvdbSettingsRepository.snapshot().apiKey.trim().takeIf { it.isNotBlank() }
            ?: TvdbSettingsRepository.snapshot().apiKey.trim().takeIf { it.isNotBlank() }
            ?: return null
        if (cachedToken != null && tokenApiKey == resolvedKey) return cachedToken
        val response = login(resolvedKey) ?: return null
        cachedToken = response.token
        tokenApiKey = resolvedKey
        return cachedToken
    }

    fun clearAuth() {
        cachedToken = null
        tokenApiKey = null
    }

    private suspend fun login(apiKey: String): TvdbLoginResponse? {
        return runCatching {
            val body = """{"apikey":"$apiKey"}"""
            val responseText = httpPostJson("$BASE_URL/login", body)
            json.decodeFromString<TvdbLoginResponse>(responseText)
        }.onFailure { e ->
            log.w { "TVDB login failed: ${e.message}" }
        }.getOrNull()
    }

    suspend fun searchSeries(query: String): List<TvdbSearchResult> {
        val token = ensureAuthenticated() ?: return emptyList()
        return runCatching {
            val url = "$BASE_URL/search?query=${encodeQuery(query)}&type=series"
            val responseText = httpGetTextWithHeaders(url, headers = authHeaders(token))
            val response = json.decodeFromString<TvdbSearchResponse>(responseText)
            response.data
        }.onFailure { e ->
            log.w { "TVDB search failed: ${e.message}" }
        }.getOrNull().orEmpty()
    }

    suspend fun searchByRemoteId(remoteId: String): List<TvdbSearchResult> {
        val token = ensureAuthenticated() ?: return emptyList()
        return runCatching {
            val url = "$BASE_URL/search?remote_id=${encodeQuery(remoteId)}&type=series"
            val responseText = httpGetTextWithHeaders(url, headers = authHeaders(token))
            val response = json.decodeFromString<TvdbSearchResponse>(responseText)
            response.data
        }.onFailure { e ->
            log.w { "TVDB remote search failed: ${e.message}" }
        }.getOrNull().orEmpty()
    }

    suspend fun getSeriesExtended(id: Int): TvdbSeriesExtended? {
        val token = ensureAuthenticated() ?: return null
        return runCatching {
            val url = "$BASE_URL/series/$id/extended"
            val responseText = httpGetTextWithHeaders(url, headers = authHeaders(token))
            val response = json.decodeFromString<TvdbSeriesExtendedResponse>(responseText)
            response.data
        }.onFailure { e ->
            log.w { "TVDB series $id failed: ${e.message}" }
        }.getOrNull()
    }

    suspend fun getSeriesEpisodes(id: Int, page: Int = 0): TvdbEpisodesResponse? {
        val token = ensureAuthenticated() ?: return null
        return runCatching {
            val url = "$BASE_URL/series/$id/episodes${if (page > 0) "?page=$page" else ""}"
            val responseText = httpGetTextWithHeaders(url, headers = authHeaders(token))
            json.decodeFromString<TvdbEpisodesResponse>(responseText)
        }.onFailure { e ->
            log.w { "TVDB episodes for $id failed: ${e.message}" }
        }.getOrNull()
    }

    suspend fun getArtwork(id: Int): TvdbArtworkResponse? {
        val token = ensureAuthenticated() ?: return null
        return runCatching {
            val url = "$BASE_URL/artwork/$id"
            val responseText = httpGetTextWithHeaders(url, headers = authHeaders(token))
            json.decodeFromString<TvdbArtworkResponse>(responseText)
        }.onFailure { e ->
            log.w { "TVDB artwork $id failed: ${e.message}" }
        }.getOrNull()
    }

    private fun authHeaders(token: String): Map<String, String> =
        mapOf("Authorization" to "Bearer $token")

    private fun encodeQuery(query: String): String =
        query.replace(" ", "%20")
            .replace(",", "%2C")
            .replace(":", "%3A")

    @Serializable
    data class TvdbLoginResponse(
        val token: String = "",
    )

    @Serializable
    data class TvdbSearchResponse(
        val data: List<TvdbSearchResult> = emptyList(),
    )

    @Serializable
    data class TvdbSearchResult(
        val id: Int = 0,
        val name: String = "",
        @SerialName("aliases") val aliases: List<String> = emptyList(),
        @SerialName("first_air_time") val firstAirTime: String? = null,
        val image: String? = null,
        @SerialName("image_type") val imageType: Int? = null,
        @SerialName("is_official") val isOfficial: Boolean = true,
        val nameTranslations: List<String> = emptyList(),
        val overviewTranslations: List<String> = emptyList(),
        @SerialName("remote_ids") val remoteIds: List<TvdbRemoteId> = emptyList(),
        val slug: String? = null,
        val status: TvdbStatus? = null,
        val year: String? = null,
    )

    @Serializable
    data class TvdbRemoteId(
        val id: String = "",
        val type: Int = 0,
        @SerialName("sourceName") val sourceName: String = "",
    )

    @Serializable
    data class TvdbStatus(
        val id: Int = 0,
        val name: String = "",
        @SerialName("recordType") val recordType: String = "",
    )

    @Serializable
    data class TvdbSeriesExtendedResponse(
        val data: TvdbSeriesExtended? = null,
    )

    @Serializable
    data class TvdbSeriesExtended(
        val id: Int = 0,
        val name: String = "",
        val slug: String? = null,
        val overview: String? = null,
        val year: String? = null,
        val image: String? = null,
        val status: TvdbStatus? = null,
        val artwork: List<TvdbArtwork> = emptyList(),
        val seasons: List<TvdbSeason> = emptyList(),
        @SerialName("remote_ids") val remoteIds: List<TvdbRemoteId> = emptyList(),
        @SerialName("first_air_time") val firstAirTime: String? = null,
        val trailers: List<TvdbTrailer> = emptyList(),
        val companies: List<TvdbCompany> = emptyList(),
        @SerialName("content_ratings") val contentRatings: List<TvdbContentRating> = emptyList(),
        val tags: List<TvdbTag> = emptyList(),
        val characters: List<TvdbCharacter> = emptyList(),
        val lists: List<TvdbList> = emptyList(),
        @SerialName("season_types") val seasonTypes: List<TvdbSeasonType> = emptyList(),
    )

    @Serializable
    data class TvdbArtwork(
        val id: Int = 0,
        val image: String = "",
        val type: Int = 0,
        @SerialName("thumbnail") val thumbnail: String? = null,
        @SerialName("language") val language: String? = null,
        val score: Int = 0,
        val width: Int = 0,
        val height: Int = 0,
        val includesText: Boolean = false,
    )

    @Serializable
    data class TvdbSeason(
        val id: Int = 0,
        val number: Int = 0,
        val name: String? = null,
        @SerialName("image") val image: String? = null,
        @SerialName("image_type") val imageType: Int? = null,
        val overview: String? = null,
        val companies: List<TvdbCompany> = emptyList(),
        val seasons: List<TvdbSeason>? = null,
        val trailers: List<TvdbTrailer>? = null,
        val artwork: List<TvdbArtwork>? = null,
        val episodeCount: Int = 0,
    )

    @Serializable
    data class TvdbTrailer(
        val id: Int = 0,
        val name: String? = null,
        val url: String? = null,
        val language: String? = null,
        val runtime: Int = 0,
    )

    @Serializable
    data class TvdbCompany(
        val id: Int = 0,
        val name: String = "",
        val slug: String? = null,
        @SerialName("primary_company_type") val primaryCompanyType: Int? = null,
        val activeDate: String? = null,
        val inactiveDate: String? = null,
        val description: String? = null,
        val country: String? = null,
        val parentCompany: TvdbCompany? = null,
    )

    @Serializable
    data class TvdbContentRating(
        val id: Int = 0,
        val name: String = "",
        val country: String = "",
        @SerialName("content_type") val contentType: String = "",
        val rating: Float = 0f,
        val description: String? = null,
    )

    @Serializable
    data class TvdbTag(
        val id: Int = 0,
        val name: String = "",
        val tag: Int = 0,
        val helpText: String? = null,
    )

    @Serializable
    data class TvdbCharacter(
        val id: Int = 0,
        val name: String = "",
        val image: String? = null,
        val episode: TvdbCharacterEpisode? = null,
        val peopleId: Int = 0,
        val seriesId: Int = 0,
        val sort: Int = 0,
        val tagOptions: List<TvdbTagOption> = emptyList(),
        val type: Int = 0,
        val url: String? = null,
        val personName: String? = null,
    )

    @Serializable
    data class TvdbCharacterEpisode(
        val episodeId: Int = 0,
        val image: String? = null,
        val name: String = "",
        val number: Int = 0,
        val seasonNumber: Int = 0,
        val absoluteNumber: Int? = null,
        val overview: String? = null,
    )

    @Serializable
    data class TvdbTagOption(
        @SerialName("help_text") val helpText: String? = null,
        val id: Int = 0,
        val name: String = "",
        val tag: Int = 0,
        val tagName: String? = null,
    )

    @Serializable
    data class TvdbList(
        val id: Int = 0,
        val name: String = "",
        val overview: String? = null,
        val url: String? = null,
        val isOfficial: Boolean = false,
        val nameTranslations: List<String> = emptyList(),
        val overviewTranslations: List<String> = emptyList(),
        val image: String? = null,
        val score: Int = 0,
    )

    @Serializable
    data class TvdbSeasonType(
        val id: Int = 0,
        val name: String = "",
        val type: String = "",
        val seasons: List<TvdbSeason> = emptyList(),
        @SerialName("alternate_name") val alternateName: String? = null,
    )

    @Serializable
    data class TvdbEpisodesResponse(
        val data: List<TvdbEpisode> = emptyList(),
        val status: TvdbResponseStatus? = null,
        val links: TvdbLinks? = null,
    )

    @Serializable
    data class TvdbEpisode(
        val id: Int = 0,
        val name: String = "",
        val overview: String? = null,
        val number: Int = 0,
        @SerialName("season_number") val seasonNumber: Int = 0,
        @SerialName("absolute_number") val absoluteNumber: Int? = null,
        val image: String? = null,
        val thumbnail: String? = null,
        val airDate: String? = null,
        val runtime: Int? = null,
        val characters: List<TvdbCharacter> = emptyList(),
        val companies: List<TvdbCompany> = emptyList(),
        val trailers: List<TvdbTrailer> = emptyList(),
        val artwork: List<TvdbArtwork>? = null,
        val remoteIds: List<TvdbRemoteId> = emptyList(),
    )

    @Serializable
    data class TvdbResponseStatus(
        val type: String = "",
        val detail: String = "",
    )

    @Serializable
    data class TvdbLinks(
        val previous: Int? = null,
        val current: Int = 0,
        val next: Int? = null,
        val totalPages: Int = 0,
    )

    @Serializable
    data class TvdbArtworkResponse(
        val data: TvdbArtwork? = null,
    )
}
