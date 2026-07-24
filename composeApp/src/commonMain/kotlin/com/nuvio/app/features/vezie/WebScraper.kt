package com.nuvio.app.features.vezie

interface WebScraper {
    val name: String
    fun supports(url: String): Boolean

    suspend fun searchLinks(
        siteUrl: String,
        title: String,
        season: Int? = null,
        episode: Int? = null,
    ): List<String>
}

internal object ScraperDispatcher : WebScraper {
    private val scrapers = listOf(
        AnimeUnityScraper,
    )

    private val autoScraper = AutoScraper

    override val name = "ScraperDispatcher"

    override fun supports(url: String): Boolean = true

    override suspend fun searchLinks(
        siteUrl: String,
        title: String,
        season: Int?,
        episode: Int?,
    ): List<String> {
        val scraper = scrapers.firstOrNull { it.supports(siteUrl) } ?: autoScraper
        return scraper.searchLinks(siteUrl, title, season, episode)
    }
}
