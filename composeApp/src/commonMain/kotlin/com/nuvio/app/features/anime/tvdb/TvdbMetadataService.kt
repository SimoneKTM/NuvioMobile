package com.nuvio.app.features.anime.tvdb

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.details.MetaCompany
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.tvdb.TvdbSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TvdbMetadataService {
    private val log = Logger.withTag("TvdbMetadata")
    private val imdbIdRegex = Regex("tt\\d+")

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
                val extended = TvdbApi.getSeriesExtended(seriesId) ?: return@withContext meta

                var enriched = meta

                val tvdbImdbId = extended.remoteIds
                    .firstOrNull { it.sourceName == "IMDB" }
                    ?.id?.takeIf { it.isNotBlank() }
                if (tvdbImdbId != null && imdbIdRegex.find(meta.id) == null && imdbIdRegex.find(fallbackItemId) == null) {
                    enriched = enriched.copy(id = tvdbImdbId)
                }

                if (settings.useBasicInfo) {
                    if (extended.overview != null && extended.overview.isNotBlank()) {
                        if (enriched.description.isNullOrBlank()) {
                            enriched = enriched.copy(description = extended.overview)
                        }
                    }
                    if (extended.status?.name != null && enriched.status.isNullOrBlank()) {
                        enriched = enriched.copy(status = extended.status.name)
                    }
                    if (extended.year != null && enriched.releaseInfo.isNullOrBlank()) {
                        enriched = enriched.copy(releaseInfo = extended.year)
                    }
                }

                if (settings.useArtwork) {
                    if (extended.image != null && extended.image.isNotBlank()) {
                        enriched = enriched.copy(background = extended.image.takeIf { enriched.background.isNullOrBlank() } ?: enriched.background)
                    }
                    if (extended.artwork.isNotEmpty()) {
                        val iconArt = extended.artwork.firstOrNull { it.type == 6 || it.type == 7 }
                        if (iconArt != null && enriched.logo.isNullOrBlank()) {
                            enriched = enriched.copy(logo = iconArt.image)
                        }
                    }
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
                    val existingNames = enriched.cast.map { it.name }.toSet()
                    val tvdbCast = extended.characters.mapNotNull { character ->
                        val actorName = character.personName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        if (actorName in existingNames) return@mapNotNull null
                        MetaPerson(
                            name = actorName,
                            role = character.name.takeIf { it.isNotBlank() },
                            photo = character.image?.takeIf { it.isNotBlank() },
                        )
                    }
                    if (tvdbCast.isNotEmpty()) {
                        enriched = enriched.copy(cast = enriched.cast + tvdbCast)
                    }
                }

                if (settings.useArtwork && extended.companies.isNotEmpty()) {
                    if (enriched.productionCompanies.isEmpty()) {
                        val companies = extended.companies.mapNotNull { company ->
                            company.name.takeIf { it.isNotBlank() }?.let { MetaCompany(name = it) }
                        }
                        if (companies.isNotEmpty()) {
                            enriched = enriched.copy(productionCompanies = companies)
                        }
                    }
                }

                if (settings.useEpisodes && extended.seasons.isNotEmpty() && enriched.videos.isEmpty()) {
                    val episodeArtwork = extended.artwork.firstOrNull { it.type == 14 }
                    if (episodeArtwork != null && enriched.logo.isNullOrBlank()) {
                        enriched = enriched.copy(logo = episodeArtwork.image)
                    }
                }

                if (settings.useSeasonPosters && extended.seasons.isNotEmpty()) {
                    val seasonArtwork = extended.artwork.filter { it.type == 3 || it.type == 14 }
                    if (seasonArtwork.isNotEmpty()) {
                        val seasonMap = seasonArtwork.groupBy { it.id }
                        enriched = enriched.copy(
                            videos = enriched.videos.map { video ->
                                if (video.seasonPoster != null) video
                                else {
                                    val poster = seasonMap[video.season]?.firstOrNull()?.image
                                    if (poster != null) video.copy(seasonPoster = poster) else video
                                }
                            },
                        )
                    }
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
        tryRemoteIdSearch(fallbackItemId)?.let { return it }
        tryRemoteIdSearch(meta.id)?.let { return it }

        val tmdbImdbId = searchImdbIdViaTmdb(meta.name, meta.releaseInfo)
        if (tmdbImdbId != null) {
            tryRemoteIdSearch(tmdbImdbId)?.let { return it }
        }

        val scrapedImdbId = searchImdbByTitle(meta.name, meta.releaseInfo)
        if (scrapedImdbId != null) {
            tryRemoteIdSearch(scrapedImdbId)?.let { return it }
        }

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

    suspend fun enrichMeta(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: TvdbSettings,
    ): MetaDetails {
        return enrichMeta(meta, fallbackItemId, settings.toAnimeTvdbSettings())
    }

    private suspend fun tryRemoteIdSearch(itemId: String): Int? {
        val remoteId = when {
            itemId.startsWith("imdb:") || itemId.startsWith("tmdb:") -> itemId
            itemId.matches(Regex("^\\d+$")) -> "tmdb:$itemId"
            else -> {
                val imdbMatch = imdbIdRegex.find(itemId)
                if (imdbMatch != null) "imdb:${imdbMatch.value}"
                else null
            }
        } ?: return null
        val results = TvdbApi.searchByRemoteId(remoteId)
        return results.firstOrNull()?.id
    }

    private suspend fun searchImdbIdViaTmdb(title: String, year: String?): String? {
        if (title.isBlank()) return null
        return try {
            val results = TmdbService.search(title, "tv")
            val match = if (year != null) {
                results.firstOrNull { it.year == year.take(4) } ?: results.firstOrNull()
            } else {
                results.firstOrNull()
            } ?: return null
            TmdbService.tmdbToImdb(tmdbId = match.id.toIntOrNull() ?: return null, mediaType = "tv")
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun searchImdbByTitle(title: String, year: String?): String? {
        if (title.isBlank()) return null
        return try {
            val query = if (year != null) "$title $year" else title
            val url = "https://www.imdb.com/find?q=${encodeQuery(query)}&s=tt"
            val html = httpGetText(url)
            imdbIdRegex.find(html)?.value
        } catch (e: Exception) {
            null
        }
    }

    private fun encodeQuery(query: String): String =
        query.replace(" ", "+")
            .replace(",", "%2C")
            .replace(":", "%3A")
            .replace("&", "%26")
            .replace("?", "%3F")
}

internal fun TvdbSettings.toAnimeTvdbSettings(): AnimeTvdbSettings = AnimeTvdbSettings(
    enabled = enabled,
    apiKey = apiKey,
    useTrailers = useTrailers,
    useArtwork = useArtwork,
    useBasicInfo = useBasicInfo,
    useCredits = useCredits,
    useEpisodes = useEpisodes,
    useSeasonPosters = useSeasonPosters,
)
