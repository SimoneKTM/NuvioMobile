package com.nuvio.app.features.kitsu.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.BouncingDots
import com.nuvio.app.features.kitsu.KitsuLibraryItem
import com.nuvio.app.features.kitsu.KitsuLibraryUiState
import com.nuvio.app.features.kitsu.KitsuSectionSettings
import com.nuvio.app.features.kitsu.KitsuSortBy
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_kitsu_attribution_body
import nuvio.composeapp.generated.resources.settings_kitsu_attribution_title
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.kitsuLibraryContent(
    uiState: KitsuLibraryUiState,
    sectionsConfig: List<KitsuSectionSettings>,
    sortBy: KitsuSortBy,
    sortAscending: Boolean,
    onPosterClick: (KitsuLibraryItem) -> Unit,
    onEditClick: (KitsuLibraryItem) -> Unit,
    onConnectKitsuClick: () -> Unit,
    onRefresh: () -> Unit,
    isOffline: Boolean
) {
    when {
        uiState.isLoading && !uiState.isLoaded -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BouncingDots()
                }
            }
        }

        !uiState.isLoaded -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = "Kitsu Not Connected",
                    message = "Connect your Kitsu account in Settings to view your anime shelves here.",
                    actionLabel = "Connect Now",
                    onActionClick = onConnectKitsuClick
                )
            }
        }

        isOffline -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = "You are Offline",
                    message = "Internet connection is required to view your Kitsu library shelves.",
                    actionLabel = "Retry",
                    onActionClick = onRefresh
                )
            }
        }

        else -> {
            val sections = sectionsConfig.mapNotNull { sectionConfig ->
                if (!sectionConfig.enabled) return@mapNotNull null
                val rawList = when (sectionConfig.type) {
                    "Current" -> uiState.current
                    "Completed" -> uiState.completed
                    "Planned" -> uiState.planned
                    "On Hold" -> uiState.onHold
                    "Dropped" -> uiState.dropped
                    else -> emptyList()
                }
                val sorted = rawList.sortedWith(
                    when (sortBy) {
                        KitsuSortBy.LAST_UPDATED -> compareBy { it.updatedAt ?: "" }
                        KitsuSortBy.RATING -> compareBy { it.rating ?: 0.0 }
                        KitsuSortBy.TITLE -> compareBy { it.title.lowercase() }
                        KitsuSortBy.PROGRESS -> compareBy { it.progress }
                    }
                ).let { if (sortAscending) it else it.reversed() }
                Pair(sectionConfig.type, sorted)
            }

            var displayedAnySection = false

            sections.forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    displayedAnySection = true
                    item {
                        NuvioShelfSection(
                            title = title,
                            entries = list,
                            headerHorizontalPadding = 16.dp,
                            rowContentPadding = PaddingValues(horizontal = 16.dp),
                            key = { entry -> entry.id }
                        ) { entry ->
                            KitsuPosterCard(
                                item = entry,
                                onClick = { onPosterClick(entry) },
                                onEditClick = { onEditClick(entry) }
                            )
                        }
                    }
                }
            }

            if (!displayedAnySection) {
                item {
                    HomeEmptyStateCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Your Lists are Empty",
                        message = "Start watching or planning anime on Kitsu to see them show up here.",
                        actionLabel = "Refresh Now",
                        onActionClick = onRefresh
                    )
                }
            }
        }
    }
}
