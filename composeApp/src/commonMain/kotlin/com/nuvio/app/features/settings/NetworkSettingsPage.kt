package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.network.CloudflareSolver
import com.nuvio.app.core.network.PageScrapeResult
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.custom_user_agent_input_placeholder
import nuvio.composeapp.generated.resources.custom_user_agent_not_set
import nuvio.composeapp.generated.resources.custom_user_agent_override_addons
import nuvio.composeapp.generated.resources.custom_user_agent_override_addons_description
import nuvio.composeapp.generated.resources.custom_user_agent_override_both
import nuvio.composeapp.generated.resources.custom_user_agent_override_both_description
import nuvio.composeapp.generated.resources.custom_user_agent_override_plugins
import nuvio.composeapp.generated.resources.custom_user_agent_override_plugins_description
import nuvio.composeapp.generated.resources.custom_user_agent_save
import nuvio.composeapp.generated.resources.custom_user_agent_section_description
import nuvio.composeapp.generated.resources.custom_user_agent_section_title
import nuvio.composeapp.generated.resources.dns_section_title
import nuvio.composeapp.generated.resources.settings_network_dns_custom
import nuvio.composeapp.generated.resources.settings_network_dns_default
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.networkSettingsContent(
    isTablet: Boolean,
) {
    item {
        val repository = globalNetworkSettingsRepository ?: return@item
        val currentProvider by repository.dnsProvider.collectAsState()
        val currentUserAgent by repository.customUserAgent.collectAsState()
        val overrideForAddons by repository.overrideForAddons.collectAsState()
        val overrideForPlugins by repository.overrideForPlugins.collectAsState()
        val overrideForBoth by repository.overrideForBoth.collectAsState()
        var userAgentDraft by remember(currentUserAgent) { mutableStateOf(currentUserAgent) }

        val tokens = MaterialTheme.nuvio

        Column(
            modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 0.dp)
        ) {
            SettingsSection(
                title = stringResource(Res.string.dns_section_title),
                isTablet = isTablet
            ) {
                SettingsGroup(isTablet = isTablet) {
                    DnsProvider.entries.forEachIndexed { index, provider ->
                        SettingsRadioRow(
                            title = provider.displayName,
                            description = if (provider == DnsProvider.SYSTEM) stringResource(Res.string.settings_network_dns_default) else stringResource(
                                Res.string.settings_network_dns_custom,
                                provider.name.lowercase()
                            ),
                            selected = currentProvider == provider,
                            onClick = { repository.setDnsProvider(provider) },
                            isTablet = isTablet
                        )
                        if (index < DnsProvider.entries.lastIndex) {
                            SettingsGroupDivider(isTablet = isTablet)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

            SettingsSection(
                title = stringResource(Res.string.custom_user_agent_section_title),
                isTablet = isTablet
            ) {
                Text(
                    text = stringResource(Res.string.custom_user_agent_section_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp
                    ),
                )

                SettingsGroup(isTablet = isTablet) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isTablet) 20.dp else 16.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = userAgentDraft,
                            onValueChange = { userAgentDraft = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    stringResource(Res.string.custom_user_agent_input_placeholder)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tokens.colors.borderFocus.copy(alpha = tokens.opacity.strong),
                                unfocusedBorderColor = tokens.colors.borderDefault.copy(alpha = tokens.opacity.medium),
                                focusedContainerColor = tokens.colors.surface,
                                unfocusedContainerColor = tokens.colors.surface,
                            ),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        NuvioActionLabel(
                            text = stringResource(Res.string.custom_user_agent_save),
                            onClick = { repository.setCustomUserAgent(userAgentDraft) },
                        )
                    }

                    SettingsGroupDivider(isTablet = isTablet)

                    Text(
                        text = if (currentUserAgent.isBlank()) {
                            stringResource(Res.string.custom_user_agent_not_set)
                        } else {
                            currentUserAgent
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentUserAgent.isBlank()) tokens.colors.textMuted else tokens.colors.textPrimary,
                        modifier = Modifier.padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = 12.dp
                        ),
                    )

                    SettingsGroupDivider(isTablet = isTablet)

                    SettingsSwitchRow(
                        title = stringResource(Res.string.custom_user_agent_override_addons),
                        description = stringResource(Res.string.custom_user_agent_override_addons_description),
                        checked = overrideForAddons,
                        enabled = !overrideForBoth,
                        isTablet = isTablet,
                        onCheckedChange = { repository.setOverrideForAddons(it) },
                    )

                    SettingsGroupDivider(isTablet = isTablet)

                    SettingsSwitchRow(
                        title = stringResource(Res.string.custom_user_agent_override_plugins),
                        description = stringResource(Res.string.custom_user_agent_override_plugins_description),
                        checked = overrideForPlugins,
                        enabled = !overrideForBoth,
                        isTablet = isTablet,
                        onCheckedChange = { repository.setOverrideForPlugins(it) },
                    )

                    SettingsGroupDivider(isTablet = isTablet)

                    SettingsSwitchRow(
                        title = stringResource(Res.string.custom_user_agent_override_both),
                        description = stringResource(Res.string.custom_user_agent_override_both_description),
                        checked = overrideForBoth,
                        enabled = !overrideForAddons && !overrideForPlugins,
                        isTablet = isTablet,
                        onCheckedChange = { repository.setOverrideForBoth(it) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

            ScraperSitesSection(repository = repository, isTablet = isTablet)

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

            UrlScraperSection(isTablet = isTablet)
        }
    }
}

@Composable
private fun UrlScraperSection(isTablet: Boolean) {
    val tokens = MaterialTheme.nuvio
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PageScrapeResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var followIframes by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    SettingsSection(
        title = "URL Scraper Test",
        isTablet = isTablet,
    ) {
        Text(
            text = "Inserisci un URL per testare lo scraper via WebView (bypassa Cloudflare)",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
            modifier = Modifier.padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = 12.dp,
            ),
        )

        SettingsGroup(isTablet = isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it; errorMessage = null },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("https://example.com/page") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.colors.borderFocus.copy(alpha = tokens.opacity.strong),
                        unfocusedBorderColor = tokens.colors.borderDefault.copy(alpha = tokens.opacity.medium),
                        focusedContainerColor = tokens.colors.surface,
                        unfocusedContainerColor = tokens.colors.surface,
                    ),
                )
                Spacer(modifier = Modifier.width(12.dp))
                NuvioActionLabel(
                    text = if (isLoading) "..." else "Scrape",
                    onClick = {
                        if (!isLoading && urlInput.isNotBlank()) {
                            isLoading = true
                            result = null
                            errorMessage = null
                            scope.launch {
                                try {
                                    val res = if (followIframes) {
                                        CloudflareSolver.scrapePageWithIframeFollow(urlInput)
                                    } else {
                                        CloudflareSolver.scrapePage(urlInput)
                                    }
                                    if (res != null) {
                                        result = res
                                    } else {
                                        errorMessage = "Nessun risultato (WebView non disponibile o timeout)"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Errore: ${e.message ?: e.javaClass.simpleName}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                )
            }

            SettingsGroupDivider(isTablet = isTablet)

            Row(
                modifier = Modifier.padding(
                    horizontal = if (isTablet) 20.dp else 16.dp,
                    vertical = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Checkbox(
                    checked = followIframes,
                    onCheckedChange = { followIframes = it },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Segui iframe",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (isLoading) {
                Text(
                    text = "Caricamento in corso...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp,
                    ),
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp,
                    ),
                )
            }

            if (result != null) {
                val res = result!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Text(
                        text = "URL: ${res.url}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textPrimary,
                    )
                    Text(
                        text = "Iframe trovati: ${res.iframes.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textPrimary,
                    )
                    Text(
                        text = "Video URL trovati: ${res.videoUrls.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (res.videoUrls.isNotEmpty()) MaterialTheme.colorScheme.primary else tokens.colors.textPrimary,
                    )

                    if (res.iframes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Iframe:",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.colors.textMuted,
                        )
                        for (iframe in res.iframes) {
                            Text(
                                text = iframe,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.colors.textPrimary,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                                maxLines = 1,
                            )
                        }
                    }

                    if (res.videoUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Video URL:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        for (videoUrl in res.videoUrls) {
                            Text(
                                text = videoUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScraperSitesSection(repository: NetworkSettingsRepository, isTablet: Boolean) {
    val tokens = MaterialTheme.nuvio
    val scraperUrls by repository.scraperUrls.collectAsState()
    var newUrl by remember { mutableStateOf("") }
    var showAddInput by remember { mutableStateOf(false) }

    SettingsSection(
        title = "Siti Scraper",
        isTablet = isTablet,
    ) {
        Text(
            text = "Aggiungi siti streaming da usare come scraper con TMDB. " +
                    "Il sistema cerca automaticamente film/serie su questi siti usando il titolo TMDB.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
            modifier = Modifier.padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = 12.dp,
            ),
        )

        SettingsGroup(isTablet = isTablet) {
            if (scraperUrls.isEmpty()) {
                Text(
                    text = "Nessun sito configurato",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp,
                    ),
                )
            } else {
                scraperUrls.forEachIndexed { index, url ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isTablet) 20.dp else 16.dp,
                                vertical = 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NuvioActionLabel(
                            text = "Rimuovi",
                            onClick = { repository.removeScraperUrl(url) },
                        )
                    }
                    if (index < scraperUrls.lastIndex) {
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                }
            }

            SettingsGroupDivider(isTablet = isTablet)

            if (showAddInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = 8.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("https://streamingcommunityz.team") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.colors.borderFocus.copy(alpha = tokens.opacity.strong),
                            unfocusedBorderColor = tokens.colors.borderDefault.copy(alpha = tokens.opacity.medium),
                            focusedContainerColor = tokens.colors.surface,
                            unfocusedContainerColor = tokens.colors.surface,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NuvioActionLabel(
                        text = "Aggiungi",
                        onClick = {
                            if (newUrl.isNotBlank()) {
                                val url = if (newUrl.startsWith("http")) newUrl else "https://$newUrl"
                                repository.addScraperUrl(url)
                                newUrl = ""
                                showAddInput = false
                            }
                        },
                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 8.dp,
                    )
                ) {
                    NuvioActionLabel(
                        text = "+ Aggiungi sito",
                        onClick = { showAddInput = true },
                    )
                }
            }
        }
    }
}
