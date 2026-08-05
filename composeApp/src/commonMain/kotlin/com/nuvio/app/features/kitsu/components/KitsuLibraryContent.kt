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
import nuvio.composeapp.generated.resources.action_retry
import nuvio.composeapp.generated.resources.kitsu_library_empty_message
import nuvio.composeapp.generated.resources.kitsu_library_not_connected_message
import nuvio.composeapp.generated.resources.kitsu_library_offline_message
import nuvio.composeapp.generated.resources.library_connect_now
import nuvio.composeapp.generated.resources.library_kitsu_not_connected
import nuvio.composeapp.generated.resources.library_refresh_now
import nuvio.composeapp.generated.resources.library_you_are_offline
import nuvio.composeapp.generated.resources.library_your_lists_empty
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
                    title = stringResource(Res.string.library_kitsu_not_connected),
                    message = stringResource(Res.string.kitsu_library_not_connected_message),
                    actionLabel = stringResource(Res.string.library_connect_now),
                    onActionClick = onConnectKitsuClick
                )
            }
        }

        isOffline -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(Res.string.library_you_are_offline),
                    message = stringResource(Res.string.kitsu_library_offline_message),
                    actionLabel = stringResource(Res.string.action_retry),
                    onActionClick = onRefresh
                )
            }
        }

        else -> {
            val sections = sectionsConfig.mapNotNull { sectionConfig ->
                if (!sectionConfig.enabled) return@mapNotNull null
                val rawList = when {
                    sectionConfig.type.equals("current", ignoreCase = true) ||
                        sectionConfig.type.equals("in corso", ignoreCase = true) -> uiState.current
                    sectionConfig.type.equals("completed", ignoreCase = true) ||
                        sectionConfig.type.equals("completato", ignoreCase = true) -> uiState.completed
                    sectionConfig.type.equals("planned", ignoreCase = true) ||
                        sectionConfig.type.equals("pianificato", ignoreCase = true) -> uiState.planned
                    sectionConfig.type.equals("on hold", ignoreCase = true) ||
                        sectionConfig.type.equals("in pausa", ignoreCase = true) -> uiState.onHold
                    sectionConfig.type.equals("dropped", ignoreCase = true) ||
                        sectionConfig.type.equals("abbandonato", ignoreCase = true) -> uiState.dropped
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
                        title = stringResource(Res.string.library_your_lists_empty),
                        message = stringResource(Res.string.kitsu_library_empty_message),
                        actionLabel = stringResource(Res.string.library_refresh_now),
                        onActionClick = onRefresh
                    )
                }
            }
        }
    }
}
