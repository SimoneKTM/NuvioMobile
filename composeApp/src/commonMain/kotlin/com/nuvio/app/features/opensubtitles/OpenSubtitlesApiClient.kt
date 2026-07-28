package com.nuvio.app.features.opensubtitles

import com.nuvio.app.core.logging.InAppLogger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.serialization.json.Json

object OpenSubtitlesApiClient {
    private const val BASE_URL = "https://api.opensubtitles.com/api/v1"
    private val json = Json { ignoreUnknownKeys = true }

    private fun authHeaders(apiKey: String, userToken: String? = null): Map<String, String> {
        val headers = mutableMapOf(
            "Api-Key" to apiKey,
            "User-Agent" to "Nuvio v1.0",
            "Accept" to "application/json",
        )
        if (!userToken.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $userToken"
        }
        return headers
    }

    suspend fun login(
        apiKey: String,
        username: String,
        password: String,
    ): OpenSubtitlesLoginResponse {
        val url = "$BASE_URL/login"
        val body = """{"username":"$username","password":"$password"}"""
        val responseBody = httpPostJsonWithHeaders(url, body, authHeaders(apiKey))
        val result = json.decodeFromString<OpenSubtitlesLoginResponse>(responseBody)
        println("[OpenSubtitles] login: token=${result.token != null} status=${result.status}")
        InAppLogger.info("OpenSubtitles", "login: token=${result.token != null} status=${result.status}")
        return result
    }

    suspend fun searchSubtitles(
        apiKey: String,
        imdbId: String?,
        type: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        languages: List<String>,
        page: Int = 1,
    ): OpenSubtitlesSearchResponse {
        val queryParams = mutableListOf<String>()
        if (imdbId != null) queryParams.add("imdb_id=$imdbId")
        if (type != null) {
            val normalizedType = if (type.equals("tv", ignoreCase = true)) "episode" else "movie"
            queryParams.add("type=$normalizedType")
        }
        if (seasonNumber != null) queryParams.add("season_number=$seasonNumber")
        if (episodeNumber != null) queryParams.add("episode_number=$episodeNumber")
        if (languages.isNotEmpty()) queryParams.add("languages=${languages.joinToString(",")}")
        queryParams.add("page=$page")

        val url = "$BASE_URL/subtitles?${queryParams.joinToString("&")}"
        val responseBody = httpGetTextWithHeaders(url, authHeaders(apiKey))
        val result = json.decodeFromString<OpenSubtitlesSearchResponse>(responseBody)
        println("[OpenSubtitles] searchSubtitles: imdbId=$imdbId type=$type languages=$languages → ${result.totalCount} total, ${result.data.size} items")
        InAppLogger.info("OpenSubtitles", "searchSubtitles: imdbId=$imdbId type=$type languages=$languages → ${result.totalCount} total, ${result.data.size} items")
        return result
    }

    suspend fun searchSubtitlesByQuery(
        apiKey: String,
        query: String,
        type: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        languages: List<String>,
        page: Int = 1,
    ): OpenSubtitlesSearchResponse {
        val queryParams = mutableListOf("query=${java.net.URLEncoder.encode(query, "UTF-8")}")
        if (type != null) {
            val normalizedType = if (type.equals("tv", ignoreCase = true)) "episode" else "movie"
            queryParams.add("type=$normalizedType")
        }
        if (seasonNumber != null) queryParams.add("season_number=$seasonNumber")
        if (episodeNumber != null) queryParams.add("episode_number=$episodeNumber")
        if (languages.isNotEmpty()) queryParams.add("languages=${languages.joinToString(",")}")
        queryParams.add("page=$page")

        val url = "$BASE_URL/subtitles?${queryParams.joinToString("&")}"
        val responseBody = httpGetTextWithHeaders(url, authHeaders(apiKey))
        val result = json.decodeFromString<OpenSubtitlesSearchResponse>(responseBody)
        println("[OpenSubtitles] searchSubtitlesByQuery: query=$query languages=$languages → ${result.totalCount} total, ${result.data.size} items")
        InAppLogger.info("OpenSubtitles", "searchSubtitlesByQuery: query=$query languages=$languages → ${result.totalCount} total, ${result.data.size} items")
        return result
    }

    suspend fun downloadSubtitle(
        apiKey: String,
        userToken: String,
        fileId: Int,
    ): OpenSubtitlesDownloadResponse {
        val url = "$BASE_URL/download"
        val body = """{"file_id":$fileId}"""
        val responseBody = httpPostJsonWithHeaders(url, body, authHeaders(apiKey, userToken))
        val result = json.decodeFromString<OpenSubtitlesDownloadResponse>(responseBody)
        println("[OpenSubtitles] downloadSubtitle: fileId=$fileId → link=${result.link != null} remaining=${result.remaining}")
        InAppLogger.info("OpenSubtitles", "downloadSubtitle: fileId=$fileId → link=${result.link != null} remaining=${result.remaining}")
        return result
    }
}
