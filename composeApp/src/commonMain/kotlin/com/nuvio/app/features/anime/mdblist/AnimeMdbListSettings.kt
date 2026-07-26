package com.nuvio.app.features.anime.mdblist

import com.nuvio.app.features.mdblist.MdbListMetadataService
import com.nuvio.app.features.mdblist.MdbListSettings

data class AnimeMdbListSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val useImdb: Boolean = true,
    val useTmdb: Boolean = true,
    val useTomatoes: Boolean = true,
    val useMetacritic: Boolean = true,
    val useTrakt: Boolean = true,
    val useLetterboxd: Boolean = true,
    val useAudience: Boolean = true,
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    fun isProviderEnabled(providerId: String): Boolean =
        when (providerId) {
            MdbListMetadataService.PROVIDER_IMDB -> useImdb
            MdbListMetadataService.PROVIDER_TMDB -> useTmdb
            MdbListMetadataService.PROVIDER_TOMATOES -> useTomatoes
            MdbListMetadataService.PROVIDER_METACRITIC -> useMetacritic
            MdbListMetadataService.PROVIDER_TRAKT -> useTrakt
            MdbListMetadataService.PROVIDER_LETTERBOXD -> useLetterboxd
            MdbListMetadataService.PROVIDER_AUDIENCE -> useAudience
            else -> false
        }

    fun enabledProvidersInPriorityOrder(): List<String> =
        MdbListMetadataService.PROVIDER_PRIORITY_ORDER.filter(::isProviderEnabled)

    fun toMdbListSettings(): MdbListSettings = MdbListSettings(
        enabled = enabled,
        apiKey = apiKey,
        useImdb = useImdb,
        useTmdb = useTmdb,
        useTomatoes = useTomatoes,
        useMetacritic = useMetacritic,
        useTrakt = useTrakt,
        useLetterboxd = useLetterboxd,
        useAudience = useAudience,
    )
}
