package com.nuvio.app.features.anime.tvdb

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.MetaTrailer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TvdbMetadataService {
    private val log = Logger.withTag("TvdbMetadata")

    suspend fun enrichMeta(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: AnimeTvdbSettings,
    ): MetaDetails {
        if (!settings.enabled || !settings.hasApiKey) return meta

        return withContext(Dispatchers.Default) {
            try {
                val seriesId = findSeriesId(meta, fallbackItemId) ?: return@withContext meta
                val extended = TvdbApi.getSeriesExtended(seriesId) ?: return@withContext meta

                var enriched = meta

                if (extended.overview != null && extended.overview.isNotBlank()) {
                    if (enriched.description.isNullOrBlank()) {
                        enriched = enriched.copy(description = extended.overview)
                    }
                }

                if (extended.image != null && extended.image.isNotBlank()) {
                    enriched = enriched.copy(background = extended.image.takeIf { enriched.background.isNullOrBlank() } ?: enriched.background)
                }

                if (extended.contentRatings.isNotEmpty()) {
                    val existingRatings = enriched.externalRatings.toMutableList()
                    val tvdbRatingValue = extended.contentRatings.firstOrNull()?.rating?.toDouble()
                    if (tvdbRatingValue != null && existingRatings.none { it.source == "tvdb" }) {
                        existingRatings.add(
                            MetaExternalRating(
                                source = "tvdb",
                                value = tvdbRatingValue,
                            ),
                        )
                    }
                    enriched = enriched.copy(externalRatings = existingRatings)
                }

                if (extended.trailers.isNotEmpty() && enriched.trailers.isEmpty()) {
                    val trailers = extended.trailers.mapIndexedNotNull { index, trailer ->
                        trailer.url?.let { url ->
                            MetaTrailer(
                                id = "tvdb:${seriesId}:trailer:$index",
                                key = url,
                                name = trailer.name ?: "Trailer",
                                site = "youtube",
                            )
                        }
                    }
                    enriched = enriched.copy(trailers = trailers)
                }

                log.d { "TVDB enriched ${meta.name}: seriesId=$seriesId" }
                enriched
            } catch (e: Exception) {
                log.w { "TVDB enrichment failed: ${e.message}" }
                meta
            }
        }
    }

    private suspend fun findSeriesId(meta: MetaDetails, fallbackItemId: String): Int? {
        val name = meta.name.takeIf { it.isNotBlank() } ?: return null

        val results = TvdbApi.searchSeries(name)
        if (results.isNotEmpty()) {
            return results.first().id
        }

        val simplifiedName = name
            .replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "")
            .trim()
        if (simplifiedName != name) {
            val retryResults = TvdbApi.searchSeries(simplifiedName)
            if (retryResults.isNotEmpty()) {
                return retryResults.first().id
            }
        }

        return null
    }
}
