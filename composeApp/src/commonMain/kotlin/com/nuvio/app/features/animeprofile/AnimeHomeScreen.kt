package com.nuvio.app.features.animeprofile

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioViewAllPillSize
import com.nuvio.app.features.anime.AnimeAddonRepository
import com.nuvio.app.features.anime.AnimeContinueWatchingPreferencesRepository
import com.nuvio.app.features.anime.AnimeHomeCatalogSettingsRepository
import com.nuvio.app.features.anime.AnimeHomeRepository
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.canOpenCatalog
import com.nuvio.app.features.home.components.HomeContinueWatchingSection
import com.nuvio.app.features.home.components.HomeContinueWatchingSectionBottomPadding
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomeHeroSection
import com.nuvio.app.features.home.components.HomeHeroReservedSpace
import com.nuvio.app.features.home.components.HomeSkeletonHero
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.home.components.homeSectionHorizontalPaddingForWidth
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.rememberContinueWatchingLayout
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.continueWatchingEntries
import com.nuvio.app.features.watchprogress.toContinueWatchingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val ANIME_CATALOG_PREVIEW_LIMIT = 18

@Composable
fun AnimeHomeScreen(
    modifier: Modifier = Modifier,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
) {
    val addonsUiState by AnimeAddonRepository.uiState.collectAsStateWithLifecycle()
    val homeUiState by AnimeHomeRepository.uiState.collectAsStateWithLifecycle()
    val homeSettingsUiState by remember {
        AnimeHomeCatalogSettingsRepository.ensureLoaded()
        AnimeHomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val continueWatchingListState = rememberLazyListState()
    val continueWatchingPreferences by remember {
        AnimeContinueWatchingPreferencesRepository.ensureLoaded()
        AnimeContinueWatchingPreferencesRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchProgressUiState by WatchProgressRepository.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        AnimeAddonRepository.initialize()
        WatchProgressRepository.ensureLoaded()
    }

    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect {
            listState.animateScrollToItem(0)
        }
    }

    val enabledAddons = addonsUiState.addons.filter { it.enabled }

    val catalogRefreshKey = remember(enabledAddons) {
        enabledAddons
            .sortedBy { it.manifestUrl }
            .joinToString(separator = "|") { addon ->
                "${addon.manifestUrl}:${addon.manifest?.catalogs?.size ?: 0}:${addon.manifest?.id.orEmpty()}"
            }
    }

    LaunchedEffect(catalogRefreshKey) {
        if (catalogRefreshKey.isEmpty()) return@LaunchedEffect
        AnimeHomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        AnimeHomeRepository.refresh(enabledAddons)
    }

    val showHeroSlot = homeSettingsUiState.heroEnabled
    val isResolvingHeroSources = enabledAddons.any { it.isRefreshing } || homeUiState.isLoading
    val showHeroSkeleton = showHeroSlot &&
        homeUiState.heroItems.isEmpty() &&
        isResolvingHeroSources

    val hasActiveAddons = enabledAddons.any { it.manifest != null }
    val sectionsMap = remember(homeUiState.sections) {
        homeUiState.sections.associateBy(HomeCatalogSection::key)
    }
    val enabledHomeItems = remember(homeSettingsUiState.items) {
        homeSettingsUiState.items.filter { it.enabled }
    }

    val animeContinueWatchingItems = remember(watchProgressUiState.entries, continueWatchingPreferences.isVisible) {
        if (!continueWatchingPreferences.isVisible) {
            emptyList()
        } else {
            val animeEntries = watchProgressUiState.entries.filter { entry ->
                entry.parentMetaType.startsWith("anime", ignoreCase = true)
            }
            animeEntries
                .continueWatchingEntries()
                .map { entry ->
                    entry.toContinueWatchingItem()
                }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val homeSectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value)

        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = 0.dp,
            topPadding = if (showHeroSlot) 0.dp else null,
            listState = listState,
        ) {
            if (showHeroSlot) {
                item {
                    when {
                        showHeroSkeleton -> HomeSkeletonHero(
                            modifier = Modifier,
                            viewportHeight = maxHeight,
                        )

                        homeUiState.heroItems.isNotEmpty() -> HomeHeroSection(
                            items = homeUiState.heroItems,
                            modifier = Modifier,
                            viewportHeight = maxHeight,
                            listState = listState,
                            onItemClick = onPosterClick,
                        )

                        else -> HomeHeroReservedSpace(
                            modifier = Modifier,
                            viewportHeight = maxHeight,
                        )
                    }
                }
            }

            when {
                !hasActiveAddons -> {
                    item {
                        HomeEmptyStateCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = "No anime addons configured",
                            message = "Add anime addons in the Anime settings to start browsing anime content.",
                            actionLabel = "Open Anime Settings",
                            onActionClick = onNavigateToSettings,
                        )
                    }
                }

                homeUiState.isLoading && homeUiState.sections.isEmpty() -> {
                    items(3) {
                        HomeSkeletonRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            showHeaderAccent = !homeSettingsUiState.hideCatalogUnderline,
                        )
                    }
                }

                homeUiState.sections.isEmpty() && homeUiState.heroItems.isEmpty() -> {
                    item {
                        HomeEmptyStateCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = "No anime content available",
                            message = homeUiState.errorMessage ?: "Your anime addons are configured but no content was found.",
                        )
                    }
                }

                else -> {
                    if (animeContinueWatchingItems.isNotEmpty()) {
                        item(key = "anime_continue_watching") {
                            HomeContinueWatchingSection(
                                items = animeContinueWatchingItems,
                                style = continueWatchingPreferences.style,
                                useEpisodeThumbnails = continueWatchingPreferences.useEpisodeThumbnails,
                                blurNextUp = continueWatchingPreferences.blurNextUp,
                                modifier = Modifier.padding(bottom = HomeContinueWatchingSectionBottomPadding),
                                sectionPadding = homeSectionPadding,
                                layout = rememberContinueWatchingLayout(maxWidth.value),
                                listState = continueWatchingListState,
                                onItemClick = onContinueWatchingClick,
                            )
                        }
                    }
                    enabledHomeItems.forEach { settingsItem ->
                        val section = sectionsMap[settingsItem.key]
                        if (section != null && section.items.isNotEmpty()) {
                            item(key = settingsItem.key) {
                                AnimeCatalogRowSection(
                                    section = section,
                                    entries = section.items.take(ANIME_CATALOG_PREVIEW_LIMIT),
                                    modifier = Modifier.padding(bottom = 12.dp),
                                    sectionPadding = homeSectionPadding,
                                    hideCatalogUnderline = homeSettingsUiState.hideCatalogUnderline,
                                    onViewAllClick = if (section.canOpenCatalog(ANIME_CATALOG_PREVIEW_LIMIT)) {
                                        onCatalogClick?.let { { it(section) } }
                                    } else {
                                        null
                                    },
                                    onPosterClick = onPosterClick,
                                    onPosterLongClick = onPosterLongClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeCatalogRowSection(
    section: HomeCatalogSection,
    entries: List<MetaPreview>,
    modifier: Modifier = Modifier,
    sectionPadding: Dp,
    hideCatalogUnderline: Boolean,
    onViewAllClick: (() -> Unit)?,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onPosterLongClick: ((MetaPreview) -> Unit)?,
) {
    NuvioShelfSection(
        title = section.title,
        entries = entries,
        modifier = modifier.fillMaxWidth(),
        headerHorizontalPadding = sectionPadding,
        rowContentPadding = PaddingValues(horizontal = sectionPadding),
        showHeaderAccent = !hideCatalogUnderline,
        onViewAllClick = onViewAllClick,
        viewAllPillSize = NuvioViewAllPillSize.Compact,
        key = { item -> item.stableKey() },
    ) { item ->
        HomePosterCard(
            item = item,
            onClick = onPosterClick?.let { { it(item) } },
            onLongClick = onPosterLongClick?.let { { it(item) } },
        )
    }
}
