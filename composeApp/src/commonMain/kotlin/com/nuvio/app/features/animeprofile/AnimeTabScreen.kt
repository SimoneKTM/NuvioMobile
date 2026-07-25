package com.nuvio.app.features.animeprofile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun AnimeTabScreen(
    modifier: Modifier = Modifier,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onNavigateToDetail: (type: String, id: String) -> Unit = { _: String, _: String -> },
    onNavigateToSettings: () -> Unit = {},
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
) {
    AnimeHomeScreen(
        modifier = modifier.fillMaxSize(),
        scrollToTopRequests = scrollToTopRequests,
        onCatalogClick = { },
        onPosterClick = { preview ->
            onNavigateToDetail(preview.type, preview.id)
        },
        onPosterLongClick = { },
        onContinueWatchingClick = onContinueWatchingClick,
        onFolderClick = { _, _ -> },
        onNavigateToSettings = onNavigateToSettings,
    )
}
