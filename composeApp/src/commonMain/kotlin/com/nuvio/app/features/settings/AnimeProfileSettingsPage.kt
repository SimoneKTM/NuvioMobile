package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.Modifier
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NuvioTokens
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_addons
import nuvio.composeapp.generated.resources.compose_settings_page_homescreen_anime
import nuvio.composeapp.generated.resources.compose_settings_page_meta_screen
import nuvio.composeapp.generated.resources.compose_settings_page_plugins
import nuvio.composeapp.generated.resources.settings_content_discovery_addons_description
import nuvio.composeapp.generated.resources.settings_content_discovery_addons_description_appstore
import nuvio.composeapp.generated.resources.settings_content_discovery_homescreen_description
import nuvio.composeapp.generated.resources.settings_content_discovery_meta_screen_description
import nuvio.composeapp.generated.resources.settings_content_discovery_plugins_description
import nuvio.composeapp.generated.resources.settings_content_discovery_section_home
import nuvio.composeapp.generated.resources.settings_content_discovery_section_sources
import nuvio.composeapp.generated.resources.top10_description
import nuvio.composeapp.generated.resources.top10_header
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.animeProfileSettingsContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
    onHomescreenClick: () -> Unit,
    onMetaScreenClick: () -> Unit,
    onTop10CatalogClick: () -> Unit = {},
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_sources),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_addons),
                    description = stringResource(
                        if (AppFeaturePolicy.personalMediaAddonCopyEnabled) {
                            Res.string.settings_content_discovery_addons_description_appstore
                        } else {
                            Res.string.settings_content_discovery_addons_description
                        },
                    ),
                    isTablet = isTablet,
                    onClick = onAddonsClick,
                )
                if (showPluginsEntry) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_plugins),
                        description = stringResource(Res.string.settings_content_discovery_plugins_description),
                        isTablet = isTablet,
                        onClick = onPluginsClick,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_home),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_homescreen_anime),
                    description = stringResource(Res.string.settings_content_discovery_homescreen_description),
                    icon = Icons.Rounded.Home,
                    isTablet = isTablet,
                    onClick = onHomescreenClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_meta_screen),
                    description = stringResource(Res.string.settings_content_discovery_meta_screen_description),
                    icon = Icons.Rounded.Tune,
                    isTablet = isTablet,
                    onClick = onMetaScreenClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.top10_header),
                    description = stringResource(Res.string.top10_description),
                    icon = Icons.Rounded.Star,
                    isTablet = isTablet,
                    onClick = onTop10CatalogClick,
                )
            }
        }
    }
}
