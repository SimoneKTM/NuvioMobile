package com.nuvio.app.features.anime.tvdb

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.features.tvdb.TvdbSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun TvdbSettings.toAnimeTvdbSettings(): AnimeTvdbSettings = AnimeTvdbSettings(
    enabled = enabled,
    apiKey = apiKey,
    language = language,
    useTrailers = useTrailers,
    useArtwork = useArtwork,
    useBasicInfo = useBasicInfo,
    useCredits = useCredits,
    useEpisodes = useEpisodes,
    useSeasonPosters = useSeasonPosters,
)

private data class TvdbEpisodeEnrichment(
    val title: String? = null,
    val overview: String? = null,
    val thumbnail: String? = null,
    val seasonPoster: String? = null,
    val airDate: String? = null,
    val runtimeMinutes: Int? = null,
)

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
                if (TvdbApi.ensureAuthenticated(settings.apiKey) == null) return@withContext meta
                val seriesId = findSeriesId(meta, fallbackItemId) ?: return@withContext meta
                val apiLanguage = settings.language.takeIf { it.isNotBlank() }?.trim()

                val extended = TvdbApi.getSeriesExtended(seriesId, language = apiLanguage) ?: return@withContext meta

                val needsEpisodes = settings.useEpisodes || settings.useSeasonPosters
                val episodeMap = if (needsEpisodes && extended.id > 0) {
                    val seasonPosterMap = if (settings.useSeasonPosters) {
                        extended.seasons
                            .filter { it.image != null }
                            .associate { it.number to it.image }
                    } else {
                        emptyMap()
                    }
                    fetchEpisodeEnrichment(extended.id, seasonPosterMap, language = apiLanguage)
                } else {
                    emptyMap()
                }

                var enriched = meta

                if (settings.useBasicInfo && apiLanguage != null && apiLanguage != "en") {
                    val localizedName = extended.aliases
                        .firstOrNull { it.language == apiLanguage && it.name.isNotBlank() }
                        ?.name
                        ?: extended.name.takeIf { it.isNotBlank() }
                    if (localizedName != null) {
                        enriched = enriched.copy(name = localizedName)
                    }
                }

                if (extended.aliases.isNotEmpty()) {
                    val aliasNames = extended.aliases.map { it.name }.filter { it.isNotBlank() }
                    if (aliasNames.isNotEmpty()) {
                        enriched = enriched.copy(aliases = (enriched.aliases + aliasNames).distinct())
                    }
                }

                if (settings.useBasicInfo && extended.overview != null && extended.overview.isNotBlank()) {
                    if (enriched.description.isNullOrBlank()) {
                        enriched = enriched.copy(description = extended.overview)
                    }
                }

                if (settings.useArtwork && extended.image != null && extended.image.isNotBlank()) {
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

                if (settings.useTrailers && extended.trailers.isNotEmpty() && enriched.trailers.isEmpty()) {
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

                if (settings.useCredits && extended.characters.isNotEmpty()) {
                    val people = extended.characters.mapNotNull { character ->
                        val actorName = character.personName?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        MetaPerson(
                            name = actorName,
                            role = character.name.trim().takeIf { it.isNotBlank() },
                            photo = character.image?.takeIf { it.isNotBlank() },
                            tmdbId = null,
                        )
                    }
                    enriched = enriched.copy(cast = people)
                }

                if (episodeMap.isNotEmpty()) {
                    enriched = enriched.copy(
                        videos = meta.videos.map { video ->
                            val key = video.season?.let { season ->
                                video.episode?.let { episode -> season to episode }
                            }
                            val episodeData = key?.let(episodeMap::get)
                            if (episodeData == null) {
                                video
                            } else {
                                video.copy(
                                    title = if (settings.useEpisodes) {
                                        episodeData.title ?: video.title
                                    } else {
                                        video.title
                                    },
                                    overview = if (settings.useEpisodes) {
                                        episodeData.overview ?: video.overview
                                    } else {
                                        video.overview
                                    },
                                    thumbnail = if (settings.useEpisodes) {
                                        episodeData.thumbnail ?: video.thumbnail
                                    } else {
                                        video.thumbnail
                                    },
                                    released = if (settings.useEpisodes) {
                                        episodeData.airDate ?: video.released
                                    } else {
                                        video.released
                                    },
                                    runtime = if (settings.useEpisodes) {
                                        episodeData.runtimeMinutes ?: video.runtime
                                    } else {
                                        video.runtime
                                    },
                                    seasonPoster = if (settings.useSeasonPosters) {
                                        episodeData.seasonPoster ?: video.seasonPoster
                                    } else {
                                        video.seasonPoster
                                    },
                                )
                            }
                        },
                    )
                }

                log.d { "TVDB enriched ${meta.name}: seriesId=$seriesId" }
                enriched
            } catch (e: Exception) {
                log.w { "TVDB enrichment failed: ${e.message}" }
                meta
            }
        }
    }

    private suspend fun fetchEpisodeEnrichment(
        seriesId: Int,
        seasonPosterMap: Map<Int, String?>,
        language: String? = null,
    ): Map<Pair<Int, Int>, TvdbEpisodeEnrichment> {
        val result = mutableMapOf<Pair<Int, Int>, TvdbEpisodeEnrichment>()
        var page = 0
        while (true) {
            val response = TvdbApi.getSeriesEpisodes(seriesId, page, language = language) ?: break
            for (episode in response.data) {
                val key = episode.seasonNumber to episode.number
                if (key !in result) {
                    result[key] = TvdbEpisodeEnrichment(
                        title = episode.name?.trim()?.takeIf(String::isNotBlank),
                        overview = episode.overview?.trim()?.takeIf(String::isNotBlank),
                        thumbnail = episode.image?.takeIf(String::isNotBlank),
                        seasonPoster = seasonPosterMap[episode.seasonNumber],
                        airDate = episode.airDate?.trim()?.takeIf(String::isNotBlank),
                        runtimeMinutes = episode.runtime,
                    )
                }
            }
            val next = response.links?.next ?: break
            page = next
        }
        return result
    }

    private suspend fun findSeriesId(meta: MetaDetails, fallbackItemId: String): String? {
        val remoteResult = tryRemoteIdSearch(fallbackItemId)
        if (remoteResult != null) return remoteResult

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

    private suspend fun tryRemoteIdSearch(itemId: String): String? {
        val remoteId = when {
            itemId.matches(Regex("^tt\\d+$")) -> "imdb:$itemId"
            itemId.matches(Regex("^\\d+$")) -> "tmdb:$itemId"
            else -> return null
        }
        val results = TvdbApi.searchByRemoteId(remoteId)
        return results.firstOrNull()?.id
    }
}