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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.anime.AnimeCollectionRepository
import com.nuvio.app.features.anime.AnimeHomeCatalogSettingsRepository
import com.nuvio.app.features.collection.Collection
import kotlin.uuid.ExperimentalUuidApi
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.features.home.HomeCatalogSettingsItem
import com.nuvio.app.features.vezie.EasyProxyAddonBridge
import com.nuvio.app.features.vezie.VeezieEasyProxy
import com.nuvio.app.features.vezie.VeezieStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.collections_header
import nuvio.composeapp.generated.resources.compose_settings_page_addons
import nuvio.composeapp.generated.resources.compose_settings_page_anime_layout
import nuvio.composeapp.generated.resources.compose_settings_page_anime_profile
import nuvio.composeapp.generated.resources.compose_settings_page_content_discovery
import nuvio.composeapp.generated.resources.compose_settings_page_continue_watching
import nuvio.composeapp.generated.resources.compose_settings_page_homescreen
import nuvio.composeapp.generated.resources.compose_settings_page_integrations
import nuvio.composeapp.generated.resources.compose_settings_page_meta_screen
import nuvio.composeapp.generated.resources.compose_settings_page_plugins
import nuvio.composeapp.generated.resources.compose_settings_page_streams
import nuvio.composeapp.generated.resources.compose_settings_root_streams_description
import nuvio.composeapp.generated.resources.layout_catalog_type
import nuvio.composeapp.generated.resources.layout_catalog_type_sub
import nuvio.composeapp.generated.resources.layout_hide_unreleased
import nuvio.composeapp.generated.resources.layout_hide_unreleased_sub
import nuvio.composeapp.generated.resources.settings_appearance_continue_watching_description
import nuvio.composeapp.generated.resources.settings_content_discovery_collections_description
import nuvio.composeapp.generated.resources.settings_content_discovery_homescreen_description
import nuvio.composeapp.generated.resources.settings_content_discovery_meta_screen_description
import nuvio.composeapp.generated.resources.settings_content_discovery_section_sources
import nuvio.composeapp.generated.resources.settings_homescreen_hide_catalog_underline
import nuvio.composeapp.generated.resources.settings_homescreen_hide_catalog_underline_description
import nuvio.composeapp.generated.resources.settings_homescreen_section_hero
import nuvio.composeapp.generated.resources.settings_homescreen_show_hero
import nuvio.composeapp.generated.resources.settings_homescreen_show_hero_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.animeRootSettingsContent(
    isTablet: Boolean,
    onContentDiscoveryClick: () -> Unit,
    onLayoutClick: () -> Unit,
    onIntegrationsClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = "GENERALI",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_content_discovery),
                    description = "Gestisci componenti aggiuntivi e plugin per la scheda Anime",
                    icon = Icons.Rounded.Extension,
                    isTablet = isTablet,
                    onClick = onContentDiscoveryClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_anime_layout),
                    description = "Layout Home, raccolte, streaming e altro per la scheda Anime",
                    icon = Icons.Rounded.Home,
                    isTablet = isTablet,
                    onClick = onLayoutClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_integrations),
                    description = "Gestisci le integrazioni per la scheda Anime",
                    icon = Icons.Rounded.Link,
                    isTablet = isTablet,
                    onClick = onIntegrationsClick,
                )
            }
        }
    }
}

internal fun LazyListScope.animeContentDiscoveryContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_sources),
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
            }
        }
    }
}

internal fun LazyListScope.animeLayoutSettingsContent(
    isTablet: Boolean,
    onHomescreenClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    onContinueWatchingClick: () -> Unit,
    onStreamsClick: () -> Unit = {},
    onMetaScreenClick: () -> Unit = {},
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.compose_settings_page_anime_layout).uppercase(),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_homescreen),
                    description = stringResource(Res.string.settings_content_discovery_homescreen_description),
                    icon = Icons.Rounded.Home,
                    isTablet = isTablet,
                    onClick = onHomescreenClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.collections_header),
                    description = stringResource(Res.string.settings_content_discovery_collections_description),
                    icon = Icons.Rounded.CollectionsBookmark,
                    isTablet = isTablet,
                    onClick = onCollectionsClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_continue_watching),
                    description = stringResource(Res.string.settings_appearance_continue_watching_description),
                    icon = Icons.Rounded.PlayCircle,
                    isTablet = isTablet,
                    onClick = onContinueWatchingClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_streams),
                    description = stringResource(Res.string.compose_settings_root_streams_description),
                    isTablet = isTablet,
                    onClick = onStreamsClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_meta_screen),
                    description = stringResource(Res.string.settings_content_discovery_meta_screen_description),
                    isTablet = isTablet,
                    onClick = onMetaScreenClick,
                )
            }
        }
    }
}

internal fun LazyListScope.animeHomescreenSettingsContent(
    isTablet: Boolean,
    heroEnabled: Boolean,
    showCatalogType: Boolean,
    hideUnreleasedContent: Boolean,
    hideCatalogUnderline: Boolean,
    items: List<HomeCatalogSettingsItem>,
) {
    val selectedHeroSourceCount = items.count { it.heroSourceEnabled }
    val enabledCatalogCount = items.count { it.enabled }
    item {
        HomescreenSummaryCard(
            isTablet = isTablet,
            enabledCatalogCount = enabledCatalogCount,
            totalCatalogCount = items.size,
            selectedHeroSourceCount = selectedHeroSourceCount,
        )
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_homescreen_section_hero),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_homescreen_show_hero),
                    description = stringResource(Res.string.settings_homescreen_show_hero_description),
                    checked = heroEnabled,
                    isTablet = isTablet,
                    onCheckedChange = AnimeHomeCatalogSettingsRepository::setHeroEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.layout_catalog_type),
                    description = stringResource(Res.string.layout_catalog_type_sub),
                    checked = showCatalogType,
                    isTablet = isTablet,
                    onCheckedChange = AnimeHomeCatalogSettingsRepository::setShowCatalogType,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.layout_hide_unreleased),
                    description = stringResource(Res.string.layout_hide_unreleased_sub),
                    checked = hideUnreleasedContent,
                    isTablet = isTablet,
                    onCheckedChange = AnimeHomeCatalogSettingsRepository::setHideUnreleasedContent,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_homescreen_hide_catalog_underline),
                    description = stringResource(Res.string.settings_homescreen_hide_catalog_underline_description),
                    checked = hideCatalogUnderline,
                    isTablet = isTablet,
                    onCheckedChange = AnimeHomeCatalogSettingsRepository::setHideCatalogUnderline,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
internal fun LazyListScope.animeCollectionsSettingsContent(
    isTablet: Boolean,
) {
    item {
        val collections by AnimeCollectionRepository.collections.collectAsStateWithLifecycle()
        var showCreateDialog by remember { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
        var newCollectionTitle by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            AnimeCollectionRepository.ensureLoaded()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NuvioSectionLabel("Collezioni Anime")

            NuvioSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Hai ${collections.size} collezioni con ${collections.sumOf { it.folders.size }} cartelle.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            NuvioPrimaryButton(
                text = "Nuova Collezione",
                onClick = { showCreateDialog = true },
            )

            if (collections.isNotEmpty()) {
                NuvioSectionLabel("Le tue collezioni")
            }

            collections.forEach { collection ->
                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = collection.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${collection.folders.size} cartelle",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = collection.id }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Elimina",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(NuvioTokens.Space.s24))
        }

        if (showCreateDialog) {
            androidx.compose.material3.BasicAlertDialog(
                onDismissRequest = { showCreateDialog = false; newCollectionTitle = "" },
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Nuova Collezione Anime",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newCollectionTitle,
                            onValueChange = { newCollectionTitle = it },
                            placeholder = { Text("Titolo collezione") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    if (newCollectionTitle.isNotBlank()) {
                                        AnimeCollectionRepository.addCollection(
                                            Collection(
                                                id = kotlin.uuid.Uuid.random().toString(),
                                                title = newCollectionTitle.trim(),
                                            ),
                                        )
                                        showCreateDialog = false
                                        newCollectionTitle = ""
                                    }
                                },
                            ),
                            shape = RoundedCornerShape(14.dp),
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = { showCreateDialog = false; newCollectionTitle = "" },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            ) {
                                Text("Annulla")
                            }
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    AnimeCollectionRepository.addCollection(
                                        Collection(
                                            id = kotlin.uuid.Uuid.random().toString(),
                                            title = newCollectionTitle.trim(),
                                        ),
                                    )
                                    showCreateDialog = false
                                    newCollectionTitle = ""
                                },
                                enabled = newCollectionTitle.isNotBlank(),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text("Crea")
                            }
                        }
                    }
                }
            }
        }

        val deleteId = showDeleteConfirm
        val deleteCollection = deleteId?.let { id -> collections.find { it.id == id } }
        if (deleteId != null) {
            NuvioStatusModal(
                title = "Elimina Collezione",
                message = "Rimuovere la collezione \"${deleteCollection?.title}\"?",
                isVisible = true,
                confirmText = "Elimina",
                dismissText = "Annulla",
                onConfirm = {
                    AnimeCollectionRepository.removeCollection(deleteId)
                    showDeleteConfirm = null
                },
                onDismiss = { showDeleteConfirm = null },
            )
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
