package com.nuvio.app.core

enum class ContentType(val key: String) {
    MOVIE("movie"),
    SERIES("series"),
    ANIME("anime"),
    TV("tv"),
    CHANNEL("channel"),
    LIVE("live"),
    OTHER("other");

    fun isSeriesLike(): Boolean = this in SERIES_GROUP

    companion object {
        private val SERIES_GROUP = setOf(SERIES, ANIME, TV)

        fun fromKey(key: String): ContentType {
            val normalized = key.trim().lowercase()
            return entries.firstOrNull { it.key == normalized } ?: OTHER
        }

        fun streamMatchKey(key: String): String = when (fromKey(key)) {
            ANIME, TV -> "series"
            CHANNEL -> "live"
            else -> key.trim().lowercase()
        }
    }
}
