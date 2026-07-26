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
        if (!settings.enabled || !settings.hasApiKey) {
            log.d { "TVDB skip: disabled or no api key, enabled=${settings.enabled}, hasApiKey=${settings.hasApiKey}" }
            return meta
        }

        return withContext(Dispatchers.Default) {
            try {
                log.d { "TVDB start: name=${meta.name}, fallbackId=$fallbackItemId" }
                if (TvdbApi.ensureAuthenticated(settings.apiKey) == null) {
                    log.d { "TVDB fail: auth failed" }
                    return@withContext meta
                }
                val seriesId = findSeriesId(meta, fallbackItemId)
                if (seriesId == null) {
                    log.d { "TVDB fail: findSeriesId returned null for name=${meta.name}, fallbackId=$fallbackItemId, meta.id=${meta.id}" }
                    return@withContext meta
                }
                log.d { "TVDB match: seriesId=$seriesId for name=${meta.name}" }
                val extended = TvdbApi.getSeriesExtended(seriesId)
                if (extended == null) {
                    log.d { "TVDB fail: getSeriesExtended returned null for seriesId=$seriesId" }
                    return@withContext meta
                }
                log.d { "TVDB data: seriesId=$seriesId, name=${extended.name}, hasOverview=${extended.overview != null}, hasImage=${extended.image != null}, artwork=${extended.artwork.size}, tags=${extended.tags.size}, cast=${extended.characters.size} trailers=${extended.trailers.size}" }

                var enriched = meta

                val tvdbImdbId = extended.remoteIds
                    .firstOrNull { it.sourceName == "IMDB" }
                    ?.id?.takeIf { it.isNotBlank() }
                if (tvdbImdbId != null && imdbIdRegex.find(meta.id) == null && imdbIdRegex.find(fallbackItemId) == null) {
                    enriched = enriched.copy(id = tvdbImdbId)
                }

                val posterArt = extended.artwork.firstOrNull { it.type == 2 }
                val bgArt = extended.image?.takeIf { it.isNotBlank() }
                val iconArt = extended.artwork.firstOrNull { it.type == 6 || it.type == 7 }
                val genres = extended.tags.mapNotNull { tag ->
                    tag.name.takeIf { it.isNotBlank() }
                }

                if (settings.useBasicInfo) {
                    enriched = enriched.copy(
                        name = extended.name.takeIf { it.isNotBlank() } ?: enriched.name,
                        description = extended.overview?.takeIf { it.isNotBlank() } ?: enriched.description,
                        releaseInfo = extended.year?.takeIf { it.isNotBlank() } ?: enriched.releaseInfo,
                        status = extended.status?.name?.takeIf { it.isNotBlank() } ?: enriched.status,
                        genres = genres.ifEmpty { enriched.genres },
                    )
                }

                if (settings.useArtwork) {
                    enriched = enriched.copy(
                        background = bgArt ?: enriched.background,
                        poster = posterArt?.image?.takeIf { it.isNotBlank() } ?: enriched.poster,
                        logo = iconArt?.image?.takeIf { it.isNotBlank() } ?: enriched.logo,
                    )
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

                if (settings.useTrailers && extended.trailers.isNotEmpty()) {
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
                    if (trailers.isNotEmpty()) {
                        enriched = enriched.copy(trailers = trailers)
                    }
                }

                if (settings.useCredits && extended.characters.isNotEmpty()) {
                    val tvdbCast = extended.characters.mapNotNull { character ->
                        val actorName = character.personName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        MetaPerson(
                            name = actorName,
                            role = character.name.takeIf { it.isNotBlank() },
                            photo = character.image?.takeIf { it.isNotBlank() },
                        )
                    }
                    if (tvdbCast.isNotEmpty()) {
                        enriched = enriched.copy(cast = tvdbCast)
                    }
                }

                if (settings.useArtwork && extended.companies.isNotEmpty()) {
                    val companies = extended.companies.mapNotNull { company ->
                        company.name.takeIf { it.isNotBlank() }?.let { MetaCompany(name = it) }
                    }
                    if (companies.isNotEmpty()) {
                        enriched = enriched.copy(productionCompanies = companies)
                    }
                }

                if (settings.useSeasonPosters && extended.seasons.isNotEmpty()) {
                    val seasonArtwork = extended.artwork.filter { it.type == 3 || it.type == 14 }
                    if (seasonArtwork.isNotEmpty()) {
                        val seasonMap = seasonArtwork.groupBy { it.id }
                        enriched = enriched.copy(
                            videos = enriched.videos.map { video ->
                                val poster = seasonMap[video.season]?.firstOrNull()?.image
                                if (poster != null) video.copy(seasonPoster = poster) else video
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
        tryRemoteIdSearch("fallbackItemId", fallbackItemId)?.let { return it }
        tryRemoteIdSearch("meta.id", meta.id)?.let { return it }

        log.d { "TVDB findSeriesId: trying TMDB search for name=${meta.name}, year=${meta.releaseInfo}" }
        val tmdbImdbId = searchImdbIdViaTmdb(meta.name, meta.releaseInfo)
        if (tmdbImdbId != null) {
            log.d { "TVDB findSeriesId: got IMDB from TMDB: $tmdbImdbId" }
            tryRemoteIdSearch("tmdbImdb", tmdbImdbId)?.let { return it }
        }

        log.d { "TVDB findSeriesId: trying IMDB scrape for name=${meta.name}" }
        val scrapedImdbId = searchImdbByTitle(meta.name, meta.releaseInfo)
        if (scrapedImdbId != null) {
            log.d { "TVDB findSeriesId: got IMDB from scrape: $scrapedImdbId" }
            tryRemoteIdSearch("scrapedImdb", scrapedImdbId)?.let { return it }
        }

        val name = meta.name.takeIf { it.isNotBlank() } ?: return null.also { log.d { "TVDB findSeriesId: name blank, giving up" } }

        log.d { "TVDB findSeriesId: searching TVDB by name: $name" }
        val results = TvdbApi.searchSeries(name)
        if (results.isNotEmpty()) {
            log.d { "TVDB findSeriesId: found by name: id=${results.first().id}, name=${results.first().name}" }
            return results.first().id
        }

        val simplifiedName = name
            .replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "")
            .trim()
        if (simplifiedName != name) {
            log.d { "TVDB findSeriesId: retrying with simplified name: $simplifiedName" }
            val retryResults = TvdbApi.searchSeries(simplifiedName)
            if (retryResults.isNotEmpty()) {
                log.d { "TVDB findSeriesId: found by simplified name: id=${retryResults.first().id}" }
                return retryResults.first().id
            }
        }

        log.d { "TVDB findSeriesId: all attempts failed for name=${meta.name}" }
        return null
    }

    private suspend fun tryRemoteIdSearch(source: String, itemId: String): Int? {
        log.d { "TVDB tryRemoteIdSearch: source=$source, itemId=$itemId" }
        val remoteId = when {
            itemId.startsWith("imdb:") || itemId.startsWith("tmdb:") -> itemId
            itemId.matches(Regex("^\\d+$")) -> "tmdb:$itemId"
            else -> {
                val imdbMatch = imdbIdRegex.find(itemId)
                if (imdbMatch != null) "imdb:${imdbMatch.value}"
                else null
            }
        }
        if (remoteId == null) {
            log.d { "TVDB tryRemoteIdSearch: source=$source, no remoteId parseable from $itemId" }
            return null
        }
        log.d { "TVDB tryRemoteIdSearch: source=$source, searching remote_id=$remoteId" }
        val results = TvdbApi.searchByRemoteId(remoteId)
        if (results.isNotEmpty()) {
            log.d { "TVDB tryRemoteIdSearch: source=$source, found id=${results.first().id}" }
            return results.first().id
        }
        log.d { "TVDB tryRemoteIdSearch: source=$source, no results for $remoteId" }
        return null
    }

    suspend fun enrichMeta(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: TvdbSettings,
    ): MetaDetails {
        return enrichMeta(meta, fallbackItemId, settings.toAnimeTvdbSettings())
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
