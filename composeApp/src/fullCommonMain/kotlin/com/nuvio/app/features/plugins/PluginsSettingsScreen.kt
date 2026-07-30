package com.nuvio.app.features.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.addons.AddonStorage
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.settings.globalNetworkSettingsRepository
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.vezie.EasyProxyAddonBridge
import com.nuvio.app.features.vezie.FlareSolverr
import com.nuvio.app.features.vezie.VeezieEasyProxy
import com.nuvio.app.features.plugins.runtime.PluginRuntime
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.plugins_badge_disabled
import nuvio.composeapp.generated.resources.plugins_badge_enabled
import nuvio.composeapp.generated.resources.plugins_badge_providers
import nuvio.composeapp.generated.resources.plugins_badge_refreshing
import nuvio.composeapp.generated.resources.plugins_badge_repos
import nuvio.composeapp.generated.resources.plugins_badge_tmdb_key_missing
import nuvio.composeapp.generated.resources.plugins_badge_tmdb_key_set
import nuvio.composeapp.generated.resources.plugins_button_install_repo
import nuvio.composeapp.generated.resources.plugins_button_installing
import nuvio.composeapp.generated.resources.plugins_button_test_provider
import nuvio.composeapp.generated.resources.plugins_button_testing
import nuvio.composeapp.generated.resources.plugins_cd_delete_repo
import nuvio.composeapp.generated.resources.plugins_cd_refresh_repo
import nuvio.composeapp.generated.resources.plugins_empty_providers
import nuvio.composeapp.generated.resources.plugins_empty_repos_subtitle
import nuvio.composeapp.generated.resources.plugins_empty_repos_title
import nuvio.composeapp.generated.resources.plugins_enable_globally_desc
import nuvio.composeapp.generated.resources.plugins_enable_globally_title
import nuvio.composeapp.generated.resources.plugins_error_enter_repo_url
import nuvio.composeapp.generated.resources.plugins_group_by_repo_desc
import nuvio.composeapp.generated.resources.plugins_group_by_repo_title
import nuvio.composeapp.generated.resources.plugins_input_manifest_placeholder
import nuvio.composeapp.generated.resources.plugins_message_installed
import nuvio.composeapp.generated.resources.plugins_provider_disabled_by_repo
import nuvio.composeapp.generated.resources.plugins_provider_no_description
import nuvio.composeapp.generated.resources.plugins_provider_version
import nuvio.composeapp.generated.resources.plugins_repo_fallback_label
import nuvio.composeapp.generated.resources.plugins_repo_version
import nuvio.composeapp.generated.resources.plugins_section_add_repo
import nuvio.composeapp.generated.resources.plugins_section_installed_repos
import nuvio.composeapp.generated.resources.plugins_section_overview
import nuvio.composeapp.generated.resources.plugins_section_providers
import nuvio.composeapp.generated.resources.plugins_test_error_title
import nuvio.composeapp.generated.resources.plugins_test_failed
import nuvio.composeapp.generated.resources.plugins_test_results_count
import nuvio.composeapp.generated.resources.plugins_tmdb_required_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun PluginsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        PluginRepository.initialize()
    }

    val uiState by PluginRepository.uiState.collectAsStateWithLifecycle()
    val tmdbSettings by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    var testingScraperId by remember { mutableStateOf<String?>(null) }
    val testResults = remember { mutableStateMapOf<String, List<PluginRuntimeResult>>() }

    var configuringScraper by remember { mutableStateOf<PluginScraper?>(null) }
    var configuringLayout by remember { mutableStateOf<String?>(null) }

    val sortedRepos = remember(uiState.repositories) {
        uiState.repositories.sortedBy { it.name.lowercase() }
    }
    val hasTmdbApiKey = tmdbSettings.hasApiKey
    val repositoryNameByUrl = remember(sortedRepos) {
        sortedRepos.associate { it.manifestUrl to it.name }
    }
    val sortedScrapers = remember(uiState.scrapers, repositoryNameByUrl) {
        uiState.scrapers.sortedWith(
            compareBy<PluginScraper>(
                { repositoryNameByUrl[it.repositoryUrl]?.lowercase() ?: it.repositoryUrl.lowercase() },
                { it.name.lowercase() },
            ),
        )
    }

    val repoFallbackLabel = stringResource(Res.string.plugins_repo_fallback_label)
    val testFailedDefault = stringResource(Res.string.plugins_test_failed)
    val testErrorTitle = stringResource(Res.string.plugins_test_error_title)
    val installedTemplate = stringResource(Res.string.plugins_message_installed)
    val enterRepoUrlError = stringResource(Res.string.plugins_error_enter_repo_url)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EasyProxySection()
        Spacer(modifier = Modifier.height(12.dp))
        FlareSolverrSection()
        Spacer(modifier = Modifier.height(12.dp))
        ScraperSitesSection()
        Spacer(modifier = Modifier.height(12.dp))
        CloudStreamSettingsSection()
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        NuvioSectionLabel(stringResource(Res.string.plugins_section_overview))
        NuvioSurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_repos, sortedRepos.size))
                NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, sortedScrapers.size))
                NuvioInfoBadge(
                    text = if (uiState.pluginsEnabled) {
                        stringResource(Res.string.plugins_badge_enabled)
                    } else {
                        stringResource(Res.string.plugins_badge_disabled)
                    },
                )
                NuvioInfoBadge(
                    text = if (hasTmdbApiKey) {
                        stringResource(Res.string.plugins_badge_tmdb_key_set)
                    } else {
                        stringResource(Res.string.plugins_badge_tmdb_key_missing)
                    },
                )
            }
            if (!hasTmdbApiKey) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.plugins_tmdb_required_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.plugins_enable_globally_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.plugins_enable_globally_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = uiState.pluginsEnabled,
                    onCheckedChange = { PluginRepository.setPluginsEnabled(it) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.plugins_group_by_repo_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.plugins_group_by_repo_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = uiState.groupStreamsByRepository,
                    onCheckedChange = { PluginRepository.setGroupStreamsByRepository(it) },
                )
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_add_repo))
        NuvioSurfaceCard {
            NuvioInputField(
                value = repositoryUrl,
                onValueChange = {
                    repositoryUrl = it
                    message = null
                },
                placeholder = stringResource(Res.string.plugins_input_manifest_placeholder),
            )
            Spacer(modifier = Modifier.height(16.dp))
            NuvioPrimaryButton(
                text = if (isAdding) {
                    stringResource(Res.string.plugins_button_installing)
                } else {
                    stringResource(Res.string.plugins_button_install_repo)
                },
                enabled = repositoryUrl.isNotBlank() && !isAdding,
                onClick = {
                    val requested = repositoryUrl.trim()
                    if (requested.isBlank()) {
                        message = enterRepoUrlError
                        return@NuvioPrimaryButton
                    }
                    isAdding = true
                    message = null
                    coroutineScope.launch {
                        try {
                            when (val result = PluginRepository.addRepository(requested)) {
                                is AddPluginRepositoryResult.Success -> {
                                    repositoryUrl = ""
                                    message = installedTemplate.replace("%1\$s", result.repository.name)
                                }
                                is AddPluginRepositoryResult.Error -> {
                                    message = result.message
                                }
                            }
                        } catch (e: Exception) {
                            message = "Error: ${e.message}"
                        } finally {
                            isAdding = false
                        }
                    }
                },
            )
            message?.let { text ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_installed_repos))
        if (sortedRepos.isEmpty()) {
            NuvioSurfaceCard {
                Text(
                    text = stringResource(Res.string.plugins_empty_repos_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.plugins_empty_repos_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            sortedRepos.forEach { repo ->
                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = repo.name,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            repo.version?.let { version ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(Res.string.plugins_repo_version, version),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = repo.manifestUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NuvioIconActionButton(
                                icon = Icons.Rounded.Refresh,
                                contentDescription = stringResource(Res.string.plugins_cd_refresh_repo),
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { PluginRepository.refreshRepository(repo.manifestUrl, pushAfterRefresh = true) },
                            )
                            NuvioIconActionButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = stringResource(Res.string.plugins_cd_delete_repo),
                                tint = MaterialTheme.colorScheme.error,
                                onClick = { PluginRepository.removeRepository(repo.manifestUrl) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, repo.scraperCount))
                        if (sortedScrapers.any { it.repositoryUrl == repo.manifestUrl && it.code.isBlank() }) {
                            NuvioInfoBadge(
                                text = "Cloudstream",
                                backgroundColor = Color(0xFF2D0A4E),
                                contentColor = Color(0xFFD8B4FE),
                            )
                        }
                        if (repo.isRefreshing) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_refreshing))
                        }
                    }
                    repo.errorMessage?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_providers))
        if (sortedScrapers.isEmpty()) {
            NuvioSurfaceCard {
                Text(
                    text = stringResource(Res.string.plugins_empty_providers),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            sortedScrapers.forEach { scraper ->
                val scraperResults = testResults[scraper.id]
                val isTestingThisScraper = testingScraperId == scraper.id
                val repositoryName = repositoryNameByUrl[scraper.repositoryUrl]
                    ?: scraper.repositoryUrl.fallbackRepositoryLabel(repoFallbackLabel)

                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = null,
                                tint = if (scraper.enabled) Color(0xFF68B76A) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repositoryName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = scraper.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = scraper.description.ifBlank {
                                        stringResource(Res.string.plugins_provider_no_description)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (scraper.hasSettings) {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        val layout = PluginRuntime.getPluginSettingsLayout(scraper.code, scraper.id)
                                        if (layout != null) {
                                            configuringScraper = scraper
                                            configuringLayout = layout
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Provider settings",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Switch(
                                checked = scraper.enabled,
                                onCheckedChange = { PluginRepository.toggleScraper(scraper.id, it) },
                                enabled = scraper.manifestEnabled,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NuvioInfoBadge(text = scraper.supportedTypes.joinToString(" | "))
                        if (scraper.version.isNotBlank()) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_provider_version, scraper.version))
                        }
                        if (!scraper.manifestEnabled) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_provider_disabled_by_repo))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    NuvioPrimaryButton(
                        text = if (isTestingThisScraper) {
                            stringResource(Res.string.plugins_button_testing)
                        } else {
                            stringResource(Res.string.plugins_button_test_provider)
                        },
                        enabled = hasTmdbApiKey && !isTestingThisScraper,
                        onClick = {
                            testingScraperId = scraper.id
                            coroutineScope.launch {
                                PluginRepository.testScraper(scraper.id)
                                    .onSuccess { results ->
                                        testResults[scraper.id] = results
                                    }
                                    .onFailure { error ->
                                        testResults[scraper.id] = listOf(
                                            PluginRuntimeResult(
                                                title = testErrorTitle,
                                                name = error.message ?: testFailedDefault,
                                                url = "about:error",
                                            ),
                                        )
                                    }
                                testingScraperId = null
                            }
                        },
                    )

                    if (!scraperResults.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.plugins_test_results_count, scraperResults.size),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        scraperResults.take(8).forEach { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = result.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    if (configuringScraper != null && configuringLayout != null) {
        PluginSettingsDialog(
            scraperId = configuringScraper!!.id,
            scraperName = configuringScraper!!.name,
            layoutJson = configuringLayout!!,
            onDismiss = {
                configuringScraper = null
                configuringLayout = null
            }
        )
    }
}

@Composable
private fun EasyProxySection() {
    var proxyUrl by rememberSaveable { mutableStateOf(
        VeezieEasyProxy.getConfig()?.proxyUrl ?: ""
    ) }
    var proxyPassword by rememberSaveable { mutableStateOf(
        VeezieEasyProxy.getConfig()?.apiPassword ?: ""
    ) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "EasyProxy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Proxy per risolvere link video diretti (streamingcommunity, altadefinizione, ecc.)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NuvioSurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = proxyUrl,
                    onValueChange = { proxyUrl = it; saved = false },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("https://kittemuort-easytwelve.hf.space") },
                    label = { Text("URL Proxy") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = proxyPassword,
                    onValueChange = { proxyPassword = it; saved = false },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Password") },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                NuvioActionLabel(
                    text = if (saved) "Salvato" else "Salva",
                    onClick = {
                        if (proxyUrl.isNotBlank()) {
                            VeezieEasyProxy.configure(
                                proxyUrl = proxyUrl.trim(),
                                email = "",
                                apiPassword = proxyPassword.trim(),
                            )
                            saved = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FlareSolverrSection() {
    var flareUrl by rememberSaveable { mutableStateOf(
        FlareSolverr.getUrl() ?: ""
    ) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "FlareSolverr",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Proxy Cloudflare per siti con challenge JS (es. https://Krishna8287-My-flaresolverr.hf.space). Se configurato, viene usato come fallback dopo HTTP diretto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NuvioSurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = flareUrl,
                    onValueChange = { flareUrl = it; saved = false },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("https://Krishna8287-My-flaresolverr.hf.space") },
                    label = { Text("URL FlareSolverr") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                NuvioActionLabel(
                    text = if (saved) "Salvato" else "Salva",
                    onClick = {
                        FlareSolverr.configure(flareUrl.trim())
                        saved = true
                    },
                )
            }
        }
    }
}

@Composable
private fun ScraperSitesSection() {
    var newUrl by rememberSaveable { mutableStateOf("") }
    var scraperUrls by remember { mutableStateOf(
        EasyProxyAddonBridge.getScrapers().map { it.websiteUrl }
    ) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Siti Scraper",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Aggiungi URL di siti streaming (es. https://streamingcommunityz.team). Appaiono come addon nel catalogo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NuvioSurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("https://streamingcommunityz.team") },
                    label = { Text("URL sito") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                NuvioActionLabel(
                    text = "Aggiungi",
                    onClick = if (newUrl.isNotBlank()) ({
                        val url = if (newUrl.startsWith("http")) newUrl else "https://$newUrl"
                        val manifestUrl = EasyProxyAddonBridge.addOrUpdateScraper(url.trimEnd('/'))
                        val profileId = ProfileRepository.activeProfileId
                        val existing = AddonStorage.loadInstalledAddonUrls(profileId)
                        if (manifestUrl !in existing) {
                            AddonStorage.saveInstalledAddonUrls(profileId, existing + manifestUrl)
                        }
                        scraperUrls = EasyProxyAddonBridge.getScrapers().map { it.websiteUrl }
                        newUrl = ""
                    }) else null,
                )
            }
        }

        scraperUrls.forEach { url ->
            NuvioSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    NuvioActionLabel(
                        text = "Rimuovi",
                        onClick = {
                            val id = urlToId(url)
                            EasyProxyAddonBridge.removeScraper(id)
                            val manifestUrl = EasyProxyAddonBridge.getManifestUrlFor(url)
                            val profileId = ProfileRepository.activeProfileId
                            val existing = AddonStorage.loadInstalledAddonUrls(profileId)
                            AddonStorage.saveInstalledAddonUrls(profileId, existing.filter { it != manifestUrl })
                            scraperUrls = EasyProxyAddonBridge.getScrapers().map { it.websiteUrl }
                        },
                    )
                }
            }
        }
    }
}

private fun urlToId(websiteUrl: String): String {
    val clean = websiteUrl.trimEnd('/').removePrefix("https://").removePrefix("http://")
    return "web_${clean.lowercase().replace(Regex("[^a-z0-9.\\-]"), "").replace(".", "_")}"
}

private fun String.fallbackRepositoryLabel(fallback: String): String {
    val withoutQuery = substringBefore("?")
    val withoutManifest = withoutQuery.removeSuffix("/manifest.json")
    val host = withoutManifest.substringAfter("://", withoutManifest).substringBefore('/')
    return host.ifBlank {
        withoutManifest.substringAfterLast('/').ifBlank { fallback }
    }
}