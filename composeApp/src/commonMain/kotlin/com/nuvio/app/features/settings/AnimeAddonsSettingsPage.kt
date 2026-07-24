package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.app.features.addons.AddonsSettingsPageContent
import com.nuvio.app.features.anime.AnimeAddonRepository

internal fun LazyListScope.animeAddonsSettingsContent() {
    item {
        AnimeAddonsSettingsPageContent(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun AnimeAddonsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    AddonsSettingsPageContent(
        modifier = modifier,
        initialize = { AnimeAddonRepository.initialize() },
        uiStateFlow = AnimeAddonRepository.uiState,
        onAddAddon = AnimeAddonRepository::addAddon,
        onMoveAddon = AnimeAddonRepository::moveAddon,
        onRefreshAddon = AnimeAddonRepository::refreshAddon,
        onSetAddonEnabled = AnimeAddonRepository::setAddonEnabled,
        onRemoveAddon = AnimeAddonRepository::removeAddon,
    )
}
