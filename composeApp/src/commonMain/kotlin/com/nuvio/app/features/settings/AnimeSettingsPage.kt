package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.features.vezie.EasyProxyAddonBridge
import com.nuvio.app.features.vezie.VeezieEasyProxy
import com.nuvio.app.features.vezie.VeezieStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_addons
import nuvio.composeapp.generated.resources.compose_settings_page_anime_profile
import nuvio.composeapp.generated.resources.compose_settings_page_plugins
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.animeRootSettingsContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
    onWebScraperClick: () -> Unit,
    onAdvancedClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = "Anime",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_addons),
                    description = "Gestisci i componenti aggiuntivi per la scheda Anime",
                    icon = Icons.Rounded.Extension,
                    isTablet = isTablet,
                    onClick = onAddonsClick,
                )
                if (showPluginsEntry) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_plugins),
                        description = "Gestisci i plugin per la scheda Anime",
                        icon = Icons.Rounded.Extension,
                        isTablet = isTablet,
                        onClick = onPluginsClick,
                    )
                }
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "Web Scraper Anime",
                    description = "Web scraper con EasyProxy per Anime",
                    icon = Icons.Rounded.Language,
                    isTablet = isTablet,
                    onClick = onWebScraperClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "Anime Advanced",
                    description = "Impostazioni avanzate Anime",
                    icon = Icons.Rounded.Tune,
                    isTablet = isTablet,
                    onClick = onAdvancedClick,
                )
            }
        }
    }
}

internal fun LazyListScope.animeWebScraperSettingsContent(
    isTablet: Boolean,
) {
    item {
        AnimeWebScraperSection()
    }
}

internal fun LazyListScope.animeAdvancedSettingsContent(
    isTablet: Boolean,
) {
    item {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NuvioSectionLabel("Impostazioni Avanzate Anime")

            NuvioSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Opzioni avanzate per la configurazione della scheda Anime.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(12.dp))

                    val scraperCount = EasyProxyAddonBridge.getScrapers().size
                    Text(
                        if (scraperCount > 0) {
                            "Web scraper attivi: $scraperCount"
                        } else {
                            "Nessun web scraper configurato"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Stato EasyProxy: ${if (VeezieEasyProxy.isConfigured()) "Configurato" else "Non configurato"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(NuvioTokens.Space.s24))
        }
    }
}

@Composable
private fun AnimeWebScraperSection() {
    val scope = remember { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    var scraperUrl by remember { mutableStateOf("") }
    var easyProxyUrl by remember { mutableStateOf("") }
    var easyProxyPassword by remember { mutableStateOf("") }
    var addonError by remember { mutableStateOf<String?>(null) }
    var addonSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            easyProxyUrl = VeezieStorage.loadEasyProxyUrl()
            easyProxyPassword = VeezieStorage.loadEasyProxyPassword()
        }
    }

    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NuvioSectionLabel("Web Scraper Anime con EasyProxy")

        NuvioSurfaceCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Inserisci un URL di un sito web (es. streamingcommunityz.run) per aggiungerlo come fonte Anime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                NuvioInputField(
                    value = scraperUrl,
                    onValueChange = { scraperUrl = it; addonError = null; addonSuccess = false },
                    placeholder = "https://streamingcommunityz.run/",
                )

                addonError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                addonSuccess.let { if (it) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Web scraper aggiunto con successo!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }}

                Spacer(Modifier.height(8.dp))

                NuvioPrimaryButton(
                    text = "Aggiungi Web Scraper Anime",
                    onClick = {
                        scope.launch(Dispatchers.Default) {
                            try {
                                val url = scraperUrl.trim()
                                if (url.isEmpty()) {
                                    addonError = "Inserisci un URL valido"
                                    return@launch
                                }
                                EasyProxyAddonBridge.addOrUpdateScraper(url)
                                addonSuccess = true
                                addonError = null
                                scraperUrl = ""
                            } catch (e: Exception) {
                                addonError = e.message ?: "Errore sconosciuto"
                                addonSuccess = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Configurazione EasyProxy",
                    style = MaterialTheme.typography.titleSmall,
                )

                Spacer(Modifier.height(8.dp))

                NuvioInputField(
                    value = easyProxyUrl,
                    onValueChange = { easyProxyUrl = it },
                    placeholder = "URL server EasyProxy (es. https://kittemuort-easytwelve.hf.space)",
                )

                Spacer(Modifier.height(8.dp))

                NuvioInputField(
                    value = easyProxyPassword,
                    onValueChange = { easyProxyPassword = it },
                    placeholder = "Password API (es. Simonekittemuort001)",
                )

                Spacer(Modifier.height(8.dp))

                NuvioPrimaryButton(
                    text = "Salva EasyProxy Anime",
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            VeezieStorage.saveEasyProxyUrl(easyProxyUrl.trim())
                            VeezieStorage.saveEasyProxyPassword(easyProxyPassword.trim())
                            VeezieEasyProxy.configure(easyProxyUrl.trim(), "", easyProxyPassword.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val installedScrapers = EasyProxyAddonBridge.getScrapers()
        if (installedScrapers.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            NuvioSectionLabel("Web Scraper Installati")
            for (scraper in installedScrapers) {
                NuvioSurfaceCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    scraper.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    scraper.websiteUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(NuvioTokens.Space.s24))
    }
}
