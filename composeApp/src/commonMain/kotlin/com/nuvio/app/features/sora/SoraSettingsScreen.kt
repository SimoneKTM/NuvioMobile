package com.nuvio.app.features.sora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import kotlinx.coroutines.launch

@Composable
fun SoraSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        SoraPluginRepository.initialize()
    }

    val uiState by SoraPluginRepository.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var repoUrl by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.padding(16.dp)) {
        item {
            Spacer(Modifier.height(8.dp))

            NuvioSectionLabel("Sora Modules")

            Spacer(Modifier.height(8.dp))

            NuvioSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Rounded.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable Sora Modules",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Use Sora modules for streaming content",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.soraEnabled,
                            onCheckedChange = { SoraPluginRepository.toggleSoraEnabled(it) },
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NuvioInfoBadge(
                            text = "${uiState.modules.size} modules",
                        )
                        NuvioInfoBadge(
                            text = "${uiState.repositories.size} repositories",
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NuvioSectionLabel("Repositories")

            Spacer(Modifier.height(8.dp))

            uiState.repositories.forEach { repo ->
                RepositoryCard(
                    repo = repo,
                    onRefresh = { SoraPluginRepository.refreshRepository(repo.id) },
                    onRemove = { SoraPluginRepository.removeRepository(repo.id) },
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            NuvioPrimaryButton(
                text = "Add Repository",
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            NuvioSectionLabel("Installed Modules")

            Spacer(Modifier.height(8.dp))

            if (uiState.modules.isEmpty()) {
                Text(
                    "No modules installed. Add a repository to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            uiState.modules.forEach { module ->
                ModuleCard(module = module)
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Sora Repository") },
            text = {
                Column {
                    Text(
                        "Enter the URL of a Sora module repository:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    NuvioInputField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        placeholder = "https://example.com/modules.json",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (repoUrl.isNotBlank()) {
                            SoraPluginRepository.addRepository(repoUrl.trim())
                            repoUrl = ""
                            showAddDialog = false
                        }
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun RepositoryCard(
    repo: SoraRepository,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
) {
    NuvioSurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    repo.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (repo.errorMessage != null) {
                    Text(
                        repo.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: SoraModule,
) {
    val manifest = module.manifest

    NuvioSurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    manifest.sourceName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (manifest.author != null) {
                    Text(
                        "by ${manifest.author.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        manifest.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        manifest.quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        manifest.language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = module.enabled,
                onCheckedChange = { SoraPluginRepository.toggleModule(module.id, it) },
            )
        }
    }
}
