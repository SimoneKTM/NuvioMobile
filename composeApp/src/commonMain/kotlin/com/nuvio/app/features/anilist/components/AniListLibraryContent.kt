package com.nuvio.app.features.anilist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.features.anilist.AniListLibraryUiState
import com.nuvio.app.features.anilist.AniListLibraryItem
import com.nuvio.app.features.anilist.AniListSortBy
import com.nuvio.app.features.home.components.BouncingDots
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.aniListLibraryContent(
    uiState: AniListLibraryUiState,
    sectionsConfig: List<com.nuvio.app.features.anilist.AniListSectionSettings>,
    sortBy: AniListSortBy,
    sortAscending: Boolean,
    onPosterClick: (AniListLibraryItem) -> Unit,
    onEditClick: (AniListLibraryItem) -> Unit,
    onConnectAniListClick: () -> Unit,
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
                    title = stringResource(Res.string.library_anilist_not_connected),
                    message = stringResource(Res.string.anilist_library_not_connected_message),
                    actionLabel = stringResource(Res.string.library_connect_now),
                    onActionClick = onConnectAniListClick
                )
            }
        }

        isOffline -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(Res.string.library_you_are_offline),
                    message = stringResource(Res.string.anilist_library_offline_message),
                    actionLabel = stringResource(Res.string.action_retry),
                    onActionClick = onRefresh
                )
            }
        }

        else -> {
            val sections = sectionsConfig.mapNotNull { sectionConfig ->
                if (!sectionConfig.enabled) return@mapNotNull null
                val rawList = when (sectionConfig.type) {
                    "In Visione" -> uiState.watching
                    "Completato" -> uiState.completed
                    "Pianificato" -> uiState.planning
                    "In Pausa" -> uiState.paused
                    "Abbandonato" -> uiState.dropped
                    "Rivisione" -> uiState.rewatching
                    else -> emptyList()
                }
                val sorted = rawList.sortedWith(
                    when (sortBy) {
                        AniListSortBy.LAST_UPDATED -> compareBy { it.updatedAt }
                        AniListSortBy.SCORE -> compareBy { it.score ?: 0 }
                        AniListSortBy.TITLE -> compareBy { it.title.lowercase() }
                        AniListSortBy.RELEASE_DATE -> compareBy { it.updatedAt } // updatedAt as proxy
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
                            AniListPosterCard(
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
                        message = stringResource(Res.string.anilist_library_empty_message),
                        actionLabel = stringResource(Res.string.library_refresh_now),
                        onActionClick = onRefresh
                    )
                }
            }
        }
    }
}
