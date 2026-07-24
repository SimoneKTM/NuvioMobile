package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.features.anime.AnimePosterCardStyleRepository

internal fun LazyListScope.animePosterCustomizationSettingsContent(
    isTablet: Boolean,
    uiState: PosterCardStyleUiState = AnimePosterCardStyleRepository.uiState.value,
) {
    posterCustomizationSettingsContent(
        isTablet = isTablet,
        uiState = uiState,
    )
}
