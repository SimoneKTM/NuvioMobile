package com.nuvio.app.features.anime.metascreen

import com.nuvio.app.features.details.MetaEpisodeCardStyle
import com.nuvio.app.features.details.MetaScreenBackgroundMode
import com.nuvio.app.features.details.MetaScreenSectionItem
import com.nuvio.app.features.details.MetaScreenSectionKey
import com.nuvio.app.features.details.MetaScreenSettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Serializable
private data class StoredAnimeMetaScreenSectionPreference(
    val key: String,
    val enabled: Boolean = true,
    val order: Int = 0,
    val tabGroup: Int? = null,
)

@Serializable
private data class StoredAnimeMetaScreenSettingsPayload(
    val items: List<StoredAnimeMetaScreenSectionPreference> = emptyList(),
    @SerialName("background_mode")
    val backgroundMode: String? = null,
    val cinematicBackground: Boolean = false,
    @SerialName("hero_trailer_playback")
    val heroTrailerPlayback: Boolean = false,
    @SerialName("tvStyleLayout")
    val tabLayout: Boolean = false,
    val episodeCardStyle: String = "horizontal",
    @SerialName("blur_unwatched_episodes")
    val blurUnwatchedEpisodes: Boolean = false,
)

private data class AnimeMetaScreenSectionDefinition(
    val key: MetaScreenSectionKey,
    val titleRes: StringResource,
    val descriptionRes: StringResource,
)

object AnimeMetaScreenSettingsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val definitions = listOf(
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.ACTIONS,
            titleRes = Res.string.meta_section_actions_title,
            descriptionRes = Res.string.meta_section_actions_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.OVERVIEW,
            titleRes = Res.string.meta_section_overview_title,
            descriptionRes = Res.string.meta_section_overview_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.PRODUCTION,
            titleRes = Res.string.meta_section_production_title,
            descriptionRes = Res.string.meta_section_production_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.CAST,
            titleRes = Res.string.settings_meta_cast,
            descriptionRes = Res.string.meta_section_cast_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.COMMENTS,
            titleRes = Res.string.settings_meta_comments,
            descriptionRes = Res.string.meta_section_comments_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.TRAILERS,
            titleRes = Res.string.settings_meta_trailers,
            descriptionRes = Res.string.meta_section_trailers_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.EPISODES,
            titleRes = Res.string.settings_meta_episodes,
            descriptionRes = Res.string.meta_section_episodes_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.DETAILS,
            titleRes = Res.string.meta_section_details_title,
            descriptionRes = Res.string.meta_section_details_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.COLLECTION,
            titleRes = Res.string.meta_section_collection_title,
            descriptionRes = Res.string.meta_section_collection_description,
        ),
        AnimeMetaScreenSectionDefinition(
            key = MetaScreenSectionKey.MORE_LIKE_THIS,
            titleRes = Res.string.meta_section_more_like_this_title,
            descriptionRes = Res.string.meta_section_more_like_this_description,
        ),
    )

    private val _uiState = MutableStateFlow(MetaScreenSettingsUiState())
    val uiState: StateFlow<MetaScreenSettingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var preferences: MutableMap<MetaScreenSectionKey, StoredAnimeMetaScreenSectionPreference> = mutableMapOf()
    private var backgroundMode: MetaScreenBackgroundMode = MetaScreenBackgroundMode.Normal
    private var heroTrailerPlayback: Boolean = false
    private var tabLayout: Boolean = false
    private var episodeCardStyle: MetaEpisodeCardStyle = MetaEpisodeCardStyle.Horizontal
    private var blurUnwatchedEpisodes: Boolean = false

    private fun localizedString(resource: StringResource): String = runBlocking { getString(resource) }

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true

        val payload = AnimeMetaScreenSettingsStorage.loadPayload().orEmpty().trim()
        if (payload.isNotEmpty()) {
            val parsed = runCatching {
                json.decodeFromString<StoredAnimeMetaScreenSettingsPayload>(payload)
            }.getOrNull()
            if (parsed != null) {
                backgroundMode = MetaScreenBackgroundMode.parse(parsed.backgroundMode)
                    ?: MetaScreenBackgroundMode.fromLegacyCinematic(parsed.cinematicBackground)
                heroTrailerPlayback = parsed.heroTrailerPlayback
                tabLayout = parsed.tabLayout
                episodeCardStyle = MetaEpisodeCardStyle.parse(parsed.episodeCardStyle)
                    ?: MetaEpisodeCardStyle.Horizontal
                blurUnwatchedEpisodes = parsed.blurUnwatchedEpisodes
                preferences = parsed.items.mapNotNull { item ->
                    val key = runCatching { MetaScreenSectionKey.valueOf(item.key) }.getOrNull() ?: return@mapNotNull null
                    key to item
                }.toMap().toMutableMap()
            }
        }

        normalizePreferences()
        publish()
        persist()
    }

    fun onProfileChanged() {
        hasLoaded = false
        preferences.clear()
        backgroundMode = MetaScreenBackgroundMode.Normal
        heroTrailerPlayback = false
        tabLayout = false
        episodeCardStyle = MetaEpisodeCardStyle.Horizontal
        blurUnwatchedEpisodes = false
        _uiState.value = MetaScreenSettingsUiState()
        ensureLoaded()
    }

    fun setBackgroundMode(mode: MetaScreenBackgroundMode) {
        ensureLoaded()
        backgroundMode = mode
        publish()
        persist()
    }

    fun setHeroTrailerPlayback(enabled: Boolean) {
        ensureLoaded()
        heroTrailerPlayback = enabled
        publish()
        persist()
    }

    fun setTabLayout(enabled: Boolean) {
        ensureLoaded()
        tabLayout = enabled
        publish()
        persist()
    }

    fun setEpisodeCardStyle(style: MetaEpisodeCardStyle) {
        ensureLoaded()
        episodeCardStyle = style
        publish()
        persist()
    }

    fun setBlurUnwatchedEpisodes(enabled: Boolean) {
        ensureLoaded()
        blurUnwatchedEpisodes = enabled
        publish()
        persist()
    }

    fun setTabGroup(key: MetaScreenSectionKey, groupId: Int?) {
        ensureLoaded()
        if (!key.canBeTabbed) return
        if (groupId != null) {
            val currentGroupCount = preferences.count { it.value.tabGroup == groupId && it.key != key }
            if (currentGroupCount >= 3) return
        }
        updatePreference(key) { preference ->
            preference.copy(tabGroup = groupId)
        }
    }

    fun setEnabled(key: MetaScreenSectionKey, enabled: Boolean) {
        updatePreference(key) { preference ->
            preference.copy(enabled = enabled)
        }
    }

    fun resetToDefaults() {
        ensureLoaded()
        preferences.clear()
        backgroundMode = MetaScreenBackgroundMode.Normal
        heroTrailerPlayback = false
        tabLayout = false
        episodeCardStyle = MetaEpisodeCardStyle.Horizontal
        blurUnwatchedEpisodes = false
        normalizePreferences()
        publish()
        persist()
    }

    fun moveByIndex(fromIndex: Int, toIndex: Int) {
        ensureLoaded()
        val orderedKeys = definitions
            .sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
            .map { it.key }
            .toMutableList()
        if (fromIndex !in orderedKeys.indices || toIndex !in orderedKeys.indices) return
        if (fromIndex == toIndex) return
        orderedKeys.add(toIndex, orderedKeys.removeAt(fromIndex))
        orderedKeys.forEachIndexed { newIndex, sectionKey ->
            val current = preferences[sectionKey] ?: return@forEachIndexed
            preferences[sectionKey] = current.copy(order = newIndex)
        }
        publish()
        persist()
    }

    private fun updatePreference(
        key: MetaScreenSectionKey,
        transform: (StoredAnimeMetaScreenSectionPreference) -> StoredAnimeMetaScreenSectionPreference,
    ) {
        ensureLoaded()
        val current = preferences[key] ?: return
        preferences[key] = transform(current)
        publish()
        persist()
    }

    private fun normalizePreferences() {
        val normalized = mutableMapOf<MetaScreenSectionKey, StoredAnimeMetaScreenSectionPreference>()
        definitions.sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
            .forEachIndexed { index, definition ->
                val stored = preferences[definition.key]
                normalized[definition.key] = StoredAnimeMetaScreenSectionPreference(
                    key = definition.key.name,
                    enabled = stored?.enabled ?: true,
                    order = index,
                    tabGroup = stored?.tabGroup,
                )
            }
        preferences = normalized
    }

    private fun publish() {
        _uiState.value = MetaScreenSettingsUiState(
            items = definitions
                .sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
                .map { definition ->
                    val preference = preferences[definition.key]
                    MetaScreenSectionItem(
                        key = definition.key,
                        title = localizedString(definition.titleRes),
                        description = localizedString(definition.descriptionRes),
                        enabled = preference?.enabled ?: true,
                        order = preference?.order ?: 0,
                        tabGroup = preference?.tabGroup,
                    )
                },
            backgroundMode = backgroundMode,
            cinematicBackground = backgroundMode.usesBackdropBackground,
            heroTrailerPlayback = heroTrailerPlayback,
            tabLayout = tabLayout,
            episodeCardStyle = episodeCardStyle,
            blurUnwatchedEpisodes = blurUnwatchedEpisodes,
        )
    }

    private fun persist() {
        AnimeMetaScreenSettingsStorage.savePayload(
            json.encodeToString(
                StoredAnimeMetaScreenSettingsPayload(
                    items = preferences.values.sortedBy { it.order },
                    backgroundMode = MetaScreenBackgroundMode.persist(backgroundMode),
                    cinematicBackground = backgroundMode.usesBackdropBackground,
                    heroTrailerPlayback = heroTrailerPlayback,
                    tabLayout = tabLayout,
                    episodeCardStyle = MetaEpisodeCardStyle.persist(episodeCardStyle),
                    blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                ),
            ),
        )
    }
}
