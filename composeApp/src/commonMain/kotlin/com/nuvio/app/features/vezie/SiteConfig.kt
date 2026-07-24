package com.nuvio.app.features.vezie

data class SiteConfig(
    val domain: String,
    val searchUrlCandidates: List<String> = DEFAULT_SEARCH_CANDIDATES,
    val contentLinkPatterns: List<String> = DEFAULT_LINK_PATTERNS,
    val videoHostPatterns: List<String> = DEFAULT_VIDEO_HOSTS,
    val usesCsrf: Boolean = false,
    val csrfPattern: String? = null,
    val usesApi: Boolean = false,
    val apiSearchUrl: String? = null,
    val apiMethod: String = "GET",
    val apiContentType: String? = null,
    val apiResponsePath: String? = null,
    val searchResultSelector: String? = null,
    val episodeNavType: EpisodeNavType = EpisodeNavType.FLAT,
    val seasonEpisodePattern: String? = null,
) {
    enum class EpisodeNavType { FLAT, TABBED, DATA_ATTRS, API }
}

internal val DEFAULT_SEARCH_CANDIDATES = listOf(
    "/?s={query}",
    "/search/{query}",
    "/cerca/{query}",
    "/?search={query}",
    "/search?q={query}",
    "/s/{query}",
    "/?name={query}",
)

internal val DEFAULT_LINK_PATTERNS = listOf(
    """post-title""",
    """entry-title""",
    """article""",
    """post""",
    """movie""",
    """item""",
    """card""",
    """title""",
    """link""",
    """thumbnail""",
)

internal val DEFAULT_VIDEO_HOSTS = listOf(
    "mixdrop", "supervideo", "voe.sx", "streamtape",
    "doodstream", "dood.", "uqload", "vidmoly",
    "clipwatching", "cloudvideo", "cloudstream",
    "filemoon", "file-moon", "streamwish", "wish.",
    "mp4upload", "speedostream", "speedo.",
    "kwik", "kwik.cx", "mystream", "mystream.",
    "mangoplayer", "mango.", "embedsito",
    "dailymotion", "youtube", "youtu.be",
    "yourupload", "uptobox", "streamhub",
    "streamlocker", "vixcloud", "fileupload",
)
