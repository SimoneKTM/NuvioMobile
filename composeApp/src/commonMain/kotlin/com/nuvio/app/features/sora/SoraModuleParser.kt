package com.nuvio.app.features.sora

import kotlinx.serialization.json.Json

internal object SoraModuleParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseManifest(payload: String): SoraManifest {
        return json.decodeFromString(payload)
    }

    fun parseSearchResults(payload: String): List<SoraSearchResult> {
        return try {
            json.decodeFromString<List<SoraSearchResult>>(payload)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseDetails(payload: String): List<SoraDetailsResult> {
        return try {
            json.decodeFromString<List<SoraDetailsResult>>(payload)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseEpisodes(payload: String): List<SoraEpisodeResult> {
        return try {
            json.decodeFromString<List<SoraEpisodeResult>>(payload)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseStreamResult(payload: String): SoraStreamResult {
        return try {
            val trimmed = payload.trim()
            if (trimmed.startsWith("{")) {
                json.decodeFromString(trimmed)
            } else {
                SoraStreamResult(stream = trimmed)
            }
        } catch (e: Exception) {
            SoraStreamResult()
        }
    }
}
