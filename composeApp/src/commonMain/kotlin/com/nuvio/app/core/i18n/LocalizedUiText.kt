package com.nuvio.app.core.i18n

import com.nuvio.app.core.ui.EpisodeCodeFormat
import com.nuvio.app.core.ui.formatEpisodeCode
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.action_play_episode
import nuvio.composeapp.generated.resources.action_resume
import nuvio.composeapp.generated.resources.action_resume_episode
import nuvio.composeapp.generated.resources.cloudstream_desktop_unsupported
import nuvio.composeapp.generated.resources.compose_player_episode_code_episode_only
import nuvio.composeapp.generated.resources.compose_player_episode_code_full
import nuvio.composeapp.generated.resources.compose_player_no_subtitle_lines_found
import nuvio.composeapp.generated.resources.compose_player_subtitle_lines_load_error
import nuvio.composeapp.generated.resources.continue_watching_up_next
import nuvio.composeapp.generated.resources.continue_watching_up_next_episode
import nuvio.composeapp.generated.resources.date_month_april
import nuvio.composeapp.generated.resources.date_month_august
import nuvio.composeapp.generated.resources.date_month_december
import nuvio.composeapp.generated.resources.date_month_february
import nuvio.composeapp.generated.resources.date_month_january
import nuvio.composeapp.generated.resources.date_month_july
import nuvio.composeapp.generated.resources.date_month_june
import nuvio.composeapp.generated.resources.date_month_march
import nuvio.composeapp.generated.resources.date_month_may
import nuvio.composeapp.generated.resources.date_month_november
import nuvio.composeapp.generated.resources.date_month_october
import nuvio.composeapp.generated.resources.date_month_september
import nuvio.composeapp.generated.resources.date_month_short_apr
import nuvio.composeapp.generated.resources.date_month_short_aug
import nuvio.composeapp.generated.resources.date_month_short_dec
import nuvio.composeapp.generated.resources.date_month_short_feb
import nuvio.composeapp.generated.resources.date_month_short_jan
import nuvio.composeapp.generated.resources.date_month_short_jul
import nuvio.composeapp.generated.resources.date_month_short_jun
import nuvio.composeapp.generated.resources.date_month_short_mar
import nuvio.composeapp.generated.resources.date_month_short_may
import nuvio.composeapp.generated.resources.date_month_short_nov
import nuvio.composeapp.generated.resources.date_month_short_oct
import nuvio.composeapp.generated.resources.date_month_short_sep
import nuvio.composeapp.generated.resources.downloads_cancelled
import nuvio.composeapp.generated.resources.downloads_cannot_access_location
import nuvio.composeapp.generated.resources.downloads_failed_create_file
import nuvio.composeapp.generated.resources.downloads_failed_open_stream
import nuvio.composeapp.generated.resources.downloads_failed_open_stream_generic
import nuvio.composeapp.generated.resources.downloads_hls_unsupported_desktop
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_action
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_adventure
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_animation
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_comedy
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_crime
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_documentary
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_drama
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_family
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_fantasy
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_history
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_horror
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_music
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_mystery
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_reality
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_romance
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_science_fiction
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_scifi
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_thriller
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_tv_movie
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_war
import nuvio.composeapp.generated.resources.collections_editor_tmdb_genre_western
import nuvio.composeapp.generated.resources.generic_unknown_error
import nuvio.composeapp.generated.resources.live_tv_read_m3u_failed
import nuvio.composeapp.generated.resources.live_tv_present_picker_failed
import nuvio.composeapp.generated.resources.live_tv_resolve_m3u_failed
import nuvio.composeapp.generated.resources.media_anime
import nuvio.composeapp.generated.resources.media_channels
import nuvio.composeapp.generated.resources.media_movie
import nuvio.composeapp.generated.resources.media_movies
import nuvio.composeapp.generated.resources.media_series
import nuvio.composeapp.generated.resources.media_tv
import nuvio.composeapp.generated.resources.nowplaying_channel_description
import nuvio.composeapp.generated.resources.nowplaying_channel_name
import nuvio.composeapp.generated.resources.nowplaying_forward_10s
import nuvio.composeapp.generated.resources.nowplaying_forward_short
import nuvio.composeapp.generated.resources.nowplaying_pause
import nuvio.composeapp.generated.resources.nowplaying_play
import nuvio.composeapp.generated.resources.nowplaying_rewind_10s
import nuvio.composeapp.generated.resources.nowplaying_rewind_short
import nuvio.composeapp.generated.resources.p2p_add_torrent_failed
import nuvio.composeapp.generated.resources.p2p_error_unknown
import nuvio.composeapp.generated.resources.plugins_error_prefix
import nuvio.composeapp.generated.resources.plugins_provider_settings
import nuvio.composeapp.generated.resources.settings_stream_badge_enter_url
import nuvio.composeapp.generated.resources.settings_stream_badge_import_failed
import nuvio.composeapp.generated.resources.settings_stream_badge_import_limit
import nuvio.composeapp.generated.resources.settings_stream_badge_url_scheme_invalid
import nuvio.composeapp.generated.resources.unit_bytes_b
import nuvio.composeapp.generated.resources.unit_bytes_gb
import nuvio.composeapp.generated.resources.unit_bytes_kb
import nuvio.composeapp.generated.resources.unit_bytes_mb
import org.jetbrains.compose.resources.getString

fun localizedMediaTypeLabel(type: String): String {
    val fallback = type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    return when (type.trim().lowercase()) {
        "movie" -> resourceString("Movies") { getString(Res.string.media_movies) }
        "series" -> resourceString("Series") { getString(Res.string.media_series) }
        "anime" -> resourceString("Anime") { getString(Res.string.media_anime) }
        "channel" -> resourceString("Channels") { getString(Res.string.media_channels) }
        "tv" -> resourceString("TV") { getString(Res.string.media_tv) }
        else -> fallback
    }
}

fun localizedMovieTypeLabel(): String = resourceString("Movie") { getString(Res.string.media_movie) }

fun localizedGenreLabel(genre: String): String {
    val fallback = genre.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    return when (genre.trim().lowercase()) {
        "action" -> resourceString("Action") { getString(Res.string.collections_editor_tmdb_genre_action) }
        "adventure" -> resourceString("Adventure") { getString(Res.string.collections_editor_tmdb_genre_adventure) }
        "animation" -> resourceString("Animation") { getString(Res.string.collections_editor_tmdb_genre_animation) }
        "comedy" -> resourceString("Comedy") { getString(Res.string.collections_editor_tmdb_genre_comedy) }
        "crime" -> resourceString("Crime") { getString(Res.string.collections_editor_tmdb_genre_crime) }
        "documentary" -> resourceString("Documentary") { getString(Res.string.collections_editor_tmdb_genre_documentary) }
        "drama" -> resourceString("Drama") { getString(Res.string.collections_editor_tmdb_genre_drama) }
        "family" -> resourceString("Family") { getString(Res.string.collections_editor_tmdb_genre_family) }
        "fantasy" -> resourceString("Fantasy") { getString(Res.string.collections_editor_tmdb_genre_fantasy) }
        "history" -> resourceString("History") { getString(Res.string.collections_editor_tmdb_genre_history) }
        "horror" -> resourceString("Horror") { getString(Res.string.collections_editor_tmdb_genre_horror) }
        "music" -> resourceString("Music") { getString(Res.string.collections_editor_tmdb_genre_music) }
        "mystery" -> resourceString("Mystery") { getString(Res.string.collections_editor_tmdb_genre_mystery) }
        "reality" -> resourceString("Reality") { getString(Res.string.collections_editor_tmdb_genre_reality) }
        "romance" -> resourceString("Romance") { getString(Res.string.collections_editor_tmdb_genre_romance) }
        "science fiction" -> resourceString("Science Fiction") { getString(Res.string.collections_editor_tmdb_genre_science_fiction) }
        "scifi", "sci-fi" -> resourceString("Sci-Fi") { getString(Res.string.collections_editor_tmdb_genre_scifi) }
        "thriller" -> resourceString("Thriller") { getString(Res.string.collections_editor_tmdb_genre_thriller) }
        "tv movie" -> resourceString("TV Movie") { getString(Res.string.collections_editor_tmdb_genre_tv_movie) }
        "war" -> resourceString("War") { getString(Res.string.collections_editor_tmdb_genre_war) }
        "western" -> resourceString("Western") { getString(Res.string.collections_editor_tmdb_genre_western) }
        else -> fallback
    }
}

fun localizedSeasonEpisodeCode(seasonNumber: Int?, episodeNumber: Int?): String? =
    when {
        seasonNumber != null && episodeNumber != null ->
            resourceString("S${seasonNumber}E${episodeNumber}") {
                getString(Res.string.compose_player_episode_code_full, seasonNumber, episodeNumber)
            }
        episodeNumber != null ->
            resourceString("E${episodeNumber}") {
                getString(Res.string.compose_player_episode_code_episode_only, episodeNumber)
            }
        else -> null
    }

fun localizedSeasonEpisodeCode(
    seasonNumber: Int?,
    episodeNumber: Int?,
    format: EpisodeCodeFormat,
): String? = when {
    seasonNumber != null && episodeNumber != null ->
        formatEpisodeCode(seasonNumber, episodeNumber, format)
    episodeNumber != null -> "E${episodeNumber}"
    else -> null
}

fun localizedPlayLabel(seasonNumber: Int?, episodeNumber: Int?): String {
    val episodeCode = localizedSeasonEpisodeCode(seasonNumber, episodeNumber)
    return if (episodeCode != null) {
        resourceString("Play $episodeCode") { getString(Res.string.action_play_episode, episodeCode) }
    } else {
        resourceString("Play") { getString(Res.string.action_play) }
    }
}

fun localizedPlayLabel(seasonNumber: Int?, episodeNumber: Int?, format: EpisodeCodeFormat): String {
    val episodeCode = localizedSeasonEpisodeCode(seasonNumber, episodeNumber, format)
    return if (episodeCode != null) {
        resourceString("Play $episodeCode") { getString(Res.string.action_play_episode, episodeCode) }
    } else {
        resourceString("Play") { getString(Res.string.action_play) }
    }
}

fun localizedResumeLabel(seasonNumber: Int?, episodeNumber: Int?): String {
    val episodeCode = localizedSeasonEpisodeCode(seasonNumber, episodeNumber)
    return if (episodeCode != null) {
        resourceString("Resume $episodeCode") { getString(Res.string.action_resume_episode, episodeCode) }
    } else {
        resourceString("Resume") { getString(Res.string.action_resume) }
    }
}

fun localizedResumeLabel(seasonNumber: Int?, episodeNumber: Int?, format: EpisodeCodeFormat): String {
    val episodeCode = localizedSeasonEpisodeCode(seasonNumber, episodeNumber, format)
    return if (episodeCode != null) {
        resourceString("Resume $episodeCode") { getString(Res.string.action_resume_episode, episodeCode) }
    } else {
        resourceString("Resume") { getString(Res.string.action_resume) }
    }
}

fun localizedUpNextLabel(seasonNumber: Int?, episodeNumber: Int?): String =
    if (seasonNumber != null && episodeNumber != null) {
        resourceString("Next Up • S${seasonNumber}E${episodeNumber}") {
            getString(Res.string.continue_watching_up_next_episode, seasonNumber, episodeNumber)
        }
    } else {
        resourceString("Next Up") { getString(Res.string.continue_watching_up_next) }
    }

fun localizedUpNextLabel(seasonNumber: Int?, episodeNumber: Int?, format: EpisodeCodeFormat): String =
    if (seasonNumber != null && episodeNumber != null) {
        val code = formatEpisodeCode(seasonNumber, episodeNumber, format)
        resourceString("Up Next • $code") {
            getString(Res.string.continue_watching_up_next_episode, code)
        }
    } else {
        resourceString("Up Next") { getString(Res.string.continue_watching_up_next) }
    }

fun localizedMonthName(month: Int): String =
    when (month) {
        1 -> resourceString("January") { getString(Res.string.date_month_january) }
        2 -> resourceString("February") { getString(Res.string.date_month_february) }
        3 -> resourceString("March") { getString(Res.string.date_month_march) }
        4 -> resourceString("April") { getString(Res.string.date_month_april) }
        5 -> resourceString("May") { getString(Res.string.date_month_may) }
        6 -> resourceString("June") { getString(Res.string.date_month_june) }
        7 -> resourceString("July") { getString(Res.string.date_month_july) }
        8 -> resourceString("August") { getString(Res.string.date_month_august) }
        9 -> resourceString("September") { getString(Res.string.date_month_september) }
        10 -> resourceString("October") { getString(Res.string.date_month_october) }
        11 -> resourceString("November") { getString(Res.string.date_month_november) }
        12 -> resourceString("December") { getString(Res.string.date_month_december) }
        else -> month.toString()
    }

fun localizedShortMonthName(month: Int): String =
    when (month) {
        1 -> resourceString("Jan") { getString(Res.string.date_month_short_jan) }
        2 -> resourceString("Feb") { getString(Res.string.date_month_short_feb) }
        3 -> resourceString("Mar") { getString(Res.string.date_month_short_mar) }
        4 -> resourceString("Apr") { getString(Res.string.date_month_short_apr) }
        5 -> resourceString("May") { getString(Res.string.date_month_short_may) }
        6 -> resourceString("Jun") { getString(Res.string.date_month_short_jun) }
        7 -> resourceString("Jul") { getString(Res.string.date_month_short_jul) }
        8 -> resourceString("Aug") { getString(Res.string.date_month_short_aug) }
        9 -> resourceString("Sep") { getString(Res.string.date_month_short_sep) }
        10 -> resourceString("Oct") { getString(Res.string.date_month_short_oct) }
        11 -> resourceString("Nov") { getString(Res.string.date_month_short_nov) }
        12 -> resourceString("Dec") { getString(Res.string.date_month_short_dec) }
        else -> month.toString()
    }

fun localizedNoSubtitleLinesFound(): String =
    resourceString("No subtitle lines found") { getString(Res.string.compose_player_no_subtitle_lines_found) }

fun localizedSubtitleLinesLoadError(): String =
    resourceString("Unable to load subtitle lines") { getString(Res.string.compose_player_subtitle_lines_load_error) }

fun localizedBadgeImportFailed(): String =
    resourceString("Badge import failed.") { getString(Res.string.settings_stream_badge_import_failed) }

fun localizedBadgeEnterUrl(): String =
    resourceString("Enter a badge JSON URL.") { getString(Res.string.settings_stream_badge_enter_url) }

fun localizedBadgeUrlSchemeInvalid(): String =
    resourceString("Badge URL must start with http:// or https://.") { getString(Res.string.settings_stream_badge_url_scheme_invalid) }

fun localizedBadgeImportLimit(limit: Int): String =
    resourceString("You can import up to $limit badge URLs.") { getString(Res.string.settings_stream_badge_import_limit, limit) }

fun localizedP2pUnknownTorrentError(): String =
    resourceString("Unknown torrent error") { getString(Res.string.p2p_error_unknown) }

fun localizedLiveTvReadM3uFailed(): String =
    resourceString("Failed to read M3U file.") { getString(Res.string.live_tv_read_m3u_failed) }

fun localizedLiveTvPresentPickerFailed(): String =
    resourceString("Unable to present file picker.") { getString(Res.string.live_tv_present_picker_failed) }

fun localizedLiveTvResolveM3uFailed(): String =
    resourceString("Unable to resolve selected M3U file path.") { getString(Res.string.live_tv_resolve_m3u_failed) }

fun localizedP2pAddTorrentFailed(): String =
    resourceString("Failed to add torrent") { getString(Res.string.p2p_add_torrent_failed) }

fun localizedCloudStreamDesktopUnsupported(): String =
    resourceString("CloudStream is not supported on desktop") { getString(Res.string.cloudstream_desktop_unsupported) }

fun localizedHlsUnsupportedDesktop(): String =
    resourceString("HLS download not supported on desktop yet.") { getString(Res.string.downloads_hls_unsupported_desktop) }

fun localizedDownloadCancelled(): String =
    resourceString("Download cancelled") { getString(Res.string.downloads_cancelled) }

fun localizedDownloadFailedCreateFile(fileName: String): String =
    resourceString("Failed to create file $fileName") { getString(Res.string.downloads_failed_create_file, fileName) }

fun localizedDownloadFailedOpenStream(fileName: String): String =
    resourceString("Failed to open output stream for $fileName") { getString(Res.string.downloads_failed_open_stream, fileName) }

fun localizedDownloadFailedOpenStreamGeneric(): String =
    resourceString("Failed to open output stream") { getString(Res.string.downloads_failed_open_stream_generic) }

fun localizedDownloadCannotAccessLocation(): String =
    resourceString("Cannot access custom download location") { getString(Res.string.downloads_cannot_access_location) }

fun localizedUnknownError(): String =
    resourceString("Unknown error") { getString(Res.string.generic_unknown_error) }

fun localizedErrorPrefix(message: String): String =
    resourceString("Error: $message") { getString(Res.string.plugins_error_prefix, message) }

fun localizedProviderSettings(): String =
    resourceString("Provider settings") { getString(Res.string.plugins_provider_settings) }

fun localizedNowPlayingChannelName(): String =
    resourceString("Playback") { getString(Res.string.nowplaying_channel_name) }

fun localizedNowPlayingChannelDescription(): String =
    resourceString("Media playback controls") { getString(Res.string.nowplaying_channel_description) }

fun localizedNowPlayingPause(): String =
    resourceString("Pause") { getString(Res.string.nowplaying_pause) }

fun localizedNowPlayingPlay(): String =
    resourceString("Play") { getString(Res.string.nowplaying_play) }

fun localizedNowPlayingRewind10s(): String =
    resourceString("Rewind 10 seconds") { getString(Res.string.nowplaying_rewind_10s) }

fun localizedNowPlayingForward10s(): String =
    resourceString("Forward 10 seconds") { getString(Res.string.nowplaying_forward_10s) }

fun localizedNowPlayingRewindShort(): String =
    resourceString("Rewind 10s") { getString(Res.string.nowplaying_rewind_short) }

fun localizedNowPlayingForwardShort(): String =
    resourceString("Forward 10s") { getString(Res.string.nowplaying_forward_short) }

fun localizedByteUnit(unit: String): String =
    when (unit) {
        "GB" -> resourceString("GB") { getString(Res.string.unit_bytes_gb) }
        "MB" -> resourceString("MB") { getString(Res.string.unit_bytes_mb) }
        "KB" -> resourceString("KB") { getString(Res.string.unit_bytes_kb) }
        else -> resourceString("B") { getString(Res.string.unit_bytes_b) }
    }

private fun resourceString(
    fallback: String,
    provider: suspend () -> String,
): String = runCatching {
    runBlocking { provider() }
}.getOrDefault(fallback)
