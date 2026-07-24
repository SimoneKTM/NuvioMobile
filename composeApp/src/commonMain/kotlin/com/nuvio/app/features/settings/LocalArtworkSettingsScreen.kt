package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.artwork.LocalArtworkSettingsContent
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_local_artwork
import org.jetbrains.compose.resources.stringResource

@Composable
fun LocalArtworkSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NuvioScreen(modifier = modifier.fillMaxSize()) {
        stickyHeader {
            NuvioScreenHeader(
                title = stringResource(Res.string.compose_settings_page_local_artwork),
                onBack = onBack,
            )
        }
        item {
            LocalArtworkSettingsContent()
        }
    }
}
