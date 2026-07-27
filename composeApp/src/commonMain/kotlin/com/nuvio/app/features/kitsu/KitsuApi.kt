package com.nuvio.app.features.kitsu

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import com.nuvio.app.features.addons.httpRequestRaw
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object KitsuApi {
    private val log = Logger.withTag("KitsuApi")
    private const val API_BASE = "https://kitsu.app/api/edge"
    private const val TOKEN_URL = "https://kitsu.app/api/oauth/token"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private fun authHeaders(token: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $token",
        "Content-Type" to "application/vnd.api+json",
        "Accept" to "application/vnd.api+json"
    )

    private fun jsonApiHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "application/vnd.api+json",
        "Accept" to "application/vnd.api+json"
    )

    suspend fun exchangeCodeForToken(code: String): KitsuOAuthTokenResponse? {
        val body = buildString {
            append("grant_type=authorization_code")
            append("&client_id=").append(KitsuConfig.CLIENT_ID)
            if (KitsuConfig.CLIENT_SECRET.isNotBlank()) {
                append("&client_secret=").append(KitsuConfig.CLIENT_SECRET)
            }
            append("&redirect_uri=").append(KitsuConfig.REDIRECT_URI)
            append("&code=").append(code)
        }
        return try {
            val response = httpPostJsonWithHeaders(
                url = TOKEN_URL,
                body = body,
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
            )
            json.decodeFromString<KitsuOAuthTokenResponse>(response)
        } catch (e: Exception) {
            log.e { "Failed to exchange Kitsu OAuth code: ${e.message}" }
            null
        }
    }

    suspend fun authenticateWithPassword(email: String, password: String): KitsuOAuthTokenResponse? {
        val body = buildString {
            append("grant_type=password")
            append("&username=").append(email.encodeURLParameter())
            append("&password=").append(password.encodeURLParameter())
            if (KitsuConfig.CLIENT_ID.isNotBlank()) {
                append("&client_id=").append(KitsuConfig.CLIENT_ID.encodeURLParameter())
            }
            if (KitsuConfig.CLIENT_SECRET.isNotBlank()) {
                append("&client_secret=").append(KitsuConfig.CLIENT_SECRET.encodeURLParameter())
            }
        }
        return try {
            val response = httpPostJsonWithHeaders(
                url = TOKEN_URL,
                body = body,
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
            )
            json.decodeFromString<KitsuOAuthTokenResponse>(response)
        } catch (e: Exception) {
            log.e { "Failed to authenticate with Kitsu password grant: ${e.message}" }
            null
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): KitsuOAuthTokenResponse? {
        val body = buildString {
            append("grant_type=refresh_token")
            append("&client_id=").append(KitsuConfig.CLIENT_ID)
            if (KitsuConfig.CLIENT_SECRET.isNotBlank()) {
                append("&client_secret=").append(KitsuConfig.CLIENT_SECRET)
            }
            append("&refresh_token=").append(refreshToken)
        }
        return try {
            val response = httpPostJsonWithHeaders(
                url = TOKEN_URL,
                body = body,
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
            )
            json.decodeFromString<KitsuOAuthTokenResponse>(response)
        } catch (e: Exception) {
            log.e { "Failed to refresh Kitsu token: ${e.message}" }
            null
        }
    }

    suspend fun fetchUser(token: String): KitsuUserData? {
        return try {
            val response = httpGetTextWithHeaders(
                url = "$API_BASE/users?filter[self]=true",
                headers = authHeaders(token)
            )
            val parsed = json.decodeFromString<KitsuApiUserResponse>(response)
            parsed.data?.firstOrNull()
        } catch (e: Exception) {
            log.e { "Failed to fetch Kitsu user: ${e.message}" }
            null
        }
    }

    suspend fun fetchLibraryEntries(
        token: String,
        userId: Long,
        status: String? = null,
        pageLimit: Int = 500,
        pageOffset: Int = 0
    ): KitsuLibraryEntriesResponse {
        val url = buildString {
            append("$API_BASE/library-entries?filter[user_id]=$userId&include=anime&page[limit]=$pageLimit&page[offset]=$pageOffset")
            if (!status.isNullOrBlank()) {
                append("&filter[status]=$status")
            }
        }
        val response = httpGetTextWithHeaders(url, authHeaders(token))
        return json.decodeFromString(response)
    }

    suspend fun saveLibraryEntry(
        token: String,
        kitsuMediaId: Long,
        userId: Long,
        status: String,
        progress: Int,
        rating: Double? = null
    ): Boolean {
        val attributes = KitsuLibraryEntryAttributes(
            status = status.toKitsuStatus(),
            progress = progress,
            rating = rating
        )
        val body = json.encodeToString(
            KitsuCreateLibraryEntryRequest(
                data = KitsuCreateLibraryEntryData(
                    attributes = attributes,
                    relationships = KitsuCreateLibraryRelationships(
                        media = KitsuRelationship(
                            data = KitsuRelationshipData(id = kitsuMediaId.toString(), type = "anime")
                        ),
                        user = KitsuRelationship(
                            data = KitsuRelationshipData(id = userId.toString(), type = "users")
                        )
                    )
                )
            )
        )
        return try {
            httpPostJsonWithHeaders(
                url = "$API_BASE/library-entries",
                body = body,
                headers = authHeaders(token)
            )
            true
        } catch (e: Exception) {
            log.e { "Failed to create Kitsu library entry: ${e.message}" }
            false
        }
    }

    suspend fun updateLibraryEntry(
        token: String,
        entryId: String,
        status: String? = null,
        progress: Int? = null,
        rating: Double? = null
    ): Boolean {
        val attrs = KitsuLibraryEntryAttributes(
            status = status?.toKitsuStatus(),
            progress = progress,
            rating = rating
        )
        val body = json.encodeToString(
            KitsuPatchLibraryEntryRequest(
                data = KitsuPatchLibraryEntryData(
                    id = entryId,
                    attributes = attrs
                )
            )
        )
        return try {
            httpRequestRaw(
                method = "PATCH",
                url = "$API_BASE/library-entries/$entryId",
                body = body,
                headers = authHeaders(token) + mapOf("Accept" to "application/vnd.api+json")
            )
            true
        } catch (e: Exception) {
            log.e { "Failed to update Kitsu library entry $entryId: ${e.message}" }
            false
        }
    }

    suspend fun deleteLibraryEntry(token: String, entryId: String): Boolean {
        return try {
            httpRequestRaw(
                method = "DELETE",
                url = "$API_BASE/library-entries/$entryId",
                body = "",
                headers = authHeaders(token)
            )
            true
        } catch (e: Exception) {
            log.e { "Failed to delete Kitsu library entry $entryId: ${e.message}" }
            false
        }
    }
}

private fun String.toKitsuStatus(): String = when (uppercase()) {
    "WATCHING", "CURRENT" -> "current"
    "COMPLETED" -> "completed"
    "PLANNING", "PLAN_TO_WATCH", "PLANNED" -> "planned"
    "PAUSED", "ON_HOLD" -> "on_hold"
    "DROPPED" -> "dropped"
    "REPEATING", "REWATCHING" -> "current"
    else -> lowercase()
}
