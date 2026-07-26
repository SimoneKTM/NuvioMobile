package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.anime.tvdb.AnimeTvdbSettings
import com.nuvio.app.features.anime.tvdb.AnimeTvdbSettingsRepository
import com.nuvio.app.features.tvdb.TvdbSettings
import com.nuvio.app.features.tvdb.TvdbSettingsRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.settings_tvdb_add_api_key_first
import nuvio.composeapp.generated.resources.settings_tvdb_api_key_description
import nuvio.composeapp.generated.resources.settings_tvdb_api_key_label
import nuvio.composeapp.generated.resources.settings_tvdb_api_key_title
import nuvio.composeapp.generated.resources.settings_tvdb_enable
import nuvio.composeapp.generated.resources.settings_tvdb_enable_description
import nuvio.composeapp.generated.resources.settings_tvdb_section_api_key
import nuvio.composeapp.generated.resources.settings_tvdb_section_modules
import nuvio.composeapp.generated.resources.settings_tvdb_section_title
import nuvio.composeapp.generated.resources.settings_tvdb_module_artwork
import nuvio.composeapp.generated.resources.settings_tvdb_module_artwork_description
import nuvio.composeapp.generated.resources.settings_tvdb_module_basic_info
import nuvio.composeapp.generated.resources.settings_tvdb_module_basic_info_description
import nuvio.composeapp.generated.resources.settings_tvdb_module_credits
import nuvio.composeapp.generated.resources.settings_tvdb_module_credits_description
import nuvio.composeapp.generated.resources.settings_tvdb_module_episodes
import nuvio.composeapp.generated.resources.settings_tvdb_module_episodes_description
import nuvio.composeapp.generated.resources.settings_tvdb_module_season_posters
import nuvio.composeapp.generated.resources.settings_tvdb_module_season_posters_description
import nuvio.composeapp.generated.resources.settings_tvdb_module_trailers
import nuvio.composeapp.generated.resources.settings_tvdb_module_trailers_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.tvdbSettingsContent(
    isTablet: Boolean,
    settings: TvdbSettings,
) {
    val enrichmentControlsEnabled = settings.enabled && settings.hasApiKey

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tvdb_section_title),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_enable),
                    description = stringResource(Res.string.settings_tvdb_enable_description),
                    checked = settings.enabled,
                    enabled = settings.hasApiKey,
                    isTablet = isTablet,
                    onCheckedChange = TvdbSettingsRepository::setEnabled,
                )
                if (!settings.hasApiKey) {
                    SettingsGroupDivider(isTablet = isTablet)
                    TvdbInfoRow(
                        isTablet = isTablet,
                        text = stringResource(Res.string.settings_tvdb_add_api_key_first),
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tvdb_section_api_key),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                TvdbApiKeyRow(
                    isTablet = isTablet,
                    value = settings.apiKey,
                    onApiKeyCommitted = TvdbSettingsRepository::setApiKey,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tvdb_section_modules),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_trailers),
                    description = stringResource(Res.string.settings_tvdb_module_trailers_description),
                    checked = settings.useTrailers,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = TvdbSettingsRepository::setUseTrailers,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_artwork),
                    description = stringResource(Res.string.settings_tvdb_module_artwork_description),
                    checked = settings.useArtwork,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = TvdbSettingsRepository::setUseArtwork,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_basic_info),
                    description = stringResource(Res.string.settings_tvdb_module_basic_info_description),
                    checked = settings.useBasicInfo,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = TvdbSettingsRepository::setUseBasicInfo,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_credits),
                    description = stringResource(Res.string.settings_tvdb_module_credits_description),
                    checked = settings.useCredits,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = TvdbSettingsRepository::setUseCredits,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_episodes),
                    description = stringResource(Res.string.settings_tvdb_module_episodes_description),
                    checked = settings.useEpisodes,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = TvdbSettingsRepository::setUseEpisodes,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_season_posters),
                    description = stringResource(Res.string.settings_tvdb_module_season_posters_description),
                    checked = settings.useSeasonPosters,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = TvdbSettingsRepository::setUseSeasonPosters,
                )
            }
        }
    }
}

internal fun LazyListScope.animeTvdbSettingsContent(
    isTablet: Boolean,
    settings: AnimeTvdbSettings,
) {
    val enrichmentControlsEnabled = settings.enabled && settings.hasApiKey

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tvdb_section_title),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_enable),
                    description = "Usa TVDB come fonte di metadati per migliorare i dati dei componenti aggiuntivi",
                    checked = settings.enabled,
                    enabled = settings.hasApiKey,
                    isTablet = isTablet,
                    onCheckedChange = AnimeTvdbSettingsRepository::setEnabled,
                )
                if (!settings.hasApiKey) {
                    SettingsGroupDivider(isTablet = isTablet)
                    TvdbInfoRow(
                        isTablet = isTablet,
                        text = "Aggiungi la tua chiave API TVDB qui sotto prima dell'arricchimento",
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tvdb_section_api_key),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                TvdbApiKeyRow(
                    isTablet = isTablet,
                    value = settings.apiKey,
                    onApiKeyCommitted = AnimeTvdbSettingsRepository::setApiKey,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tvdb_section_modules),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_trailers),
                    description = stringResource(Res.string.settings_tvdb_module_trailers_description),
                    checked = settings.useTrailers,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = AnimeTvdbSettingsRepository::setUseTrailers,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_artwork),
                    description = stringResource(Res.string.settings_tvdb_module_artwork_description),
                    checked = settings.useArtwork,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = AnimeTvdbSettingsRepository::setUseArtwork,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_basic_info),
                    description = stringResource(Res.string.settings_tvdb_module_basic_info_description),
                    checked = settings.useBasicInfo,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = AnimeTvdbSettingsRepository::setUseBasicInfo,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_credits),
                    description = stringResource(Res.string.settings_tvdb_module_credits_description),
                    checked = settings.useCredits,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = AnimeTvdbSettingsRepository::setUseCredits,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_episodes),
                    description = stringResource(Res.string.settings_tvdb_module_episodes_description),
                    checked = settings.useEpisodes,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = AnimeTvdbSettingsRepository::setUseEpisodes,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tvdb_module_season_posters),
                    description = stringResource(Res.string.settings_tvdb_module_season_posters_description),
                    checked = settings.useSeasonPosters,
                    enabled = enrichmentControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = AnimeTvdbSettingsRepository::setUseSeasonPosters,
                )
            }
        }
    }
}

@Composable
private fun TvdbApiKeyRow(
    isTablet: Boolean,
    value: String,
    onApiKeyCommitted: (String) -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    val normalizedDraft = draft.trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                    text = stringResource(Res.string.settings_tvdb_api_key_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(Res.string.settings_tvdb_api_key_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSecretTextField(
            value = draft,
            onValueChange = {
                draft = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.settings_tvdb_api_key_label),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    draft = normalizedDraft
                    onApiKeyCommitted(normalizedDraft)
                },
                enabled = normalizedDraft != value,
            ) {
                Text(stringResource(Res.string.action_save))
            }
        }
    }
}

@Composable
private fun TvdbInfoRow(
    isTablet: Boolean,
    text: String,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 14.dp else 12.dp

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
