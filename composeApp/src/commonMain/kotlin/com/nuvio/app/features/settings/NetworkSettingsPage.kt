package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.nuvio.app.core.ui.NuvioThemeTokens
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.profiles.ProfileRepository
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

            urlScraperSection(isTablet, tokens, repository)
        }
    }
}

@Composable
private fun urlScraperSection(
    isTablet: Boolean,
    tokens: NuvioThemeTokens,
    repository: NetworkSettingsRepository,
) {
    val scope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf("") }
    var isScraping by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PageScrapeResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var registerMessage by remember { mutableStateOf<String?>(null) }
    val scraperUrls by repository.scraperUrls.collectAsState()

    SettingsSection(
        title = "URL Scraper",
        isTablet = isTablet,
    ) {
        Text(
            text = "Incolla l'URL di un sito streaming (es. https://streamingcommunityz.team) e premi \"Registra addon\": il sito viene aggiunto come addon con flussi riproducibili. \"Scrape\" serve solo per la diagnostica della pagina.",
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
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text("https://streamingcommunityz.team")
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
                    text = "Registra addon",
                    onClick = {
                        val url = urlInput.trim()
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            registerMessage = "URL non valido: deve iniziare con http(s)://"
                            return@NuvioActionLabel
                        }
                        repository.addScraperUrl(url)
                        AddonRepository.onProfileChanged(ProfileRepository.activeProfileId)
                        AddonRepository.initialize()
                        registerMessage = "Registrato come addon: $url"
                    },
                )
            }

            if (registerMessage != null) {
                SettingsGroupDivider(isTablet = isTablet)
                Text(
                    text = registerMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.success,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp
                    ),
                )
            }

            if (scraperUrls.isNotEmpty()) {
                SettingsGroupDivider(isTablet = isTablet)
                Text(
                    text = "Siti registrati (addon attivi):",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 4.dp
                    ),
                )
                scraperUrls.forEach { scraperUrl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isTablet) 20.dp else 16.dp,
                                vertical = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = scraperUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.accent,
                            modifier = Modifier.weight(1f),
                        )
                        NuvioActionLabel(
                            text = "Rimuovi",
                            onClick = {
                                repository.removeScraperUrl(scraperUrl)
                                registerMessage = "Rimosso: $scraperUrl"
                            },
                        )
                    }
                }
            }

            SettingsGroupDivider(isTablet = isTablet)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Diagnostica:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.weight(1f),
                )
                if (isScraping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = tokens.colors.accent,
                    )
                } else {
                    NuvioActionLabel(
                        text = "Scrape",
                        onClick = {
                            val url = urlInput.trim()
                            if (url.isBlank()) return@NuvioActionLabel
                            isScraping = true
                            result = null
                            errorMessage = null
                            scope.launch {
                                try {
                                    val scraped = CloudflareSolver.scrapePageWithIframeFollow(
                                        url = url,
                                        maxDepth = 3,
                                        jsRenderDelayMs = 3000L,
                                    )
                                    result = scraped
                                    if (scraped == null) {
                                        errorMessage = "Nessun risultato"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Errore: ${e.message}"
                                } finally {
                                    isScraping = false
                                }
                            }
                        },
                    )
                }
            }

            if (errorMessage != null) {
                SettingsGroupDivider(isTablet = isTablet)
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.danger,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp
                    ),
                )
            }

            if (result != null) {
                SettingsGroupDivider(isTablet = isTablet)
                val r = result ?: return@SettingsGroup

                Text(
                    text = "Titolo: ${r.pageTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textPrimary,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 4.dp
                    ),
                )

                if (r.iframes.isNotEmpty()) {
                    Text(
                        text = "Iframe trovati (${r.iframes.size}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                        modifier = Modifier.padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = 4.dp
                        ),
                    )
                    r.iframes.take(3).forEach { iframe ->
                        Text(
                            text = iframe,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.accent,
                            modifier = Modifier.padding(
                                horizontal = if (isTablet) 20.dp else 16.dp,
                                vertical = 2.dp
                            ),
                        )
                    }
                }

                if (r.videoUrls.isNotEmpty()) {
                    Text(
                        text = "Video trovati (${r.videoUrls.size}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                        modifier = Modifier.padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = 4.dp
                        ),
                    )
                    r.videoUrls.take(5).forEach { video ->
                        Text(
                            text = video,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.accent,
                            modifier = Modifier.padding(
                                horizontal = if (isTablet) 20.dp else 16.dp,
                                vertical = 2.dp
                            ),
                        )
                    }
                }

                if (r.hasVideo) {
                    Text(
                        text = "Stream pronto per la riproduzione!",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.success,
                        modifier = Modifier.padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = 8.dp
                        ),
                    )
                }
            }
        }
    }
}
