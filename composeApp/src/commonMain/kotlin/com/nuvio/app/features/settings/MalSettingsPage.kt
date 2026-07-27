package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.mal.MalAuthRepository
import com.nuvio.app.features.mal.MalAuthUiState
import com.nuvio.app.features.mal.MalConnectionMode
import com.nuvio.app.features.mal.MalSectionSettings
import com.nuvio.app.features.mal.MalSettingsRepository
import com.nuvio.app.features.mal.MalSyncCoordinator
import com.nuvio.app.features.mal.defaultMalLibrarySections
import com.nuvio.app.features.watchprogress.WatchProgressClock
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.settings_mal_connected_as
import nuvio.composeapp.generated.resources.settings_mal_connect
import nuvio.composeapp.generated.resources.settings_mal_disconnect
import nuvio.composeapp.generated.resources.settings_mal_finish_sign_in
import nuvio.composeapp.generated.resources.settings_mal_not_configured
import nuvio.composeapp.generated.resources.settings_mal_section_authentication
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

internal fun LazyListScope.malSettingsContent(
    isTablet: Boolean,
    uiState: MalAuthUiState,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_mal_section_authentication),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                MalAuthRepository.ensureLoaded()
                MalSettingsRepository.ensureLoaded()
                MalConnectionCard(isTablet = isTablet)
            }
        }
    }

    item {
        val authUiState by MalAuthRepository.uiState.collectAsState()
        val settingsUiState by MalSettingsRepository.uiState.collectAsState()

        if (authUiState.mode == MalConnectionMode.CONNECTED) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSection(
                title = "Riproduzione",
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    MalWatchedThresholdRow(
                        isTablet = isTablet,
                        threshold = settingsUiState.markWatchedThreshold,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 16.dp))
                    SettingsSwitchRow(
                        title = "Aggiungi Automaticamente Nuovi Anime a MAL",
                        description = "Se attivo, guardare un anime non nella tua libreria MAL lo aggiungerà automaticamente alla tua lista In Corso e sincronizzerà i progressi.",
                        checked = settingsUiState.autoAddNewAnime,
                        enabled = settingsUiState.enableSync,
                        isTablet = isTablet,
                        onCheckedChange = { MalSettingsRepository.setAutoAddNewAnime(it) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSection(
                title = "Personalizza Sezioni Libreria",
                isTablet = isTablet,
                actions = {
                    NuvioActionLabel(
                        text = "Reimposta",
                        onClick = { MalSettingsRepository.setLibrarySections(defaultMalLibrarySections) },
                    )
                },
            ) {
                MalSectionsList(
                    isTablet = isTablet,
                    items = settingsUiState.librarySections,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MalConnectionCard(isTablet: Boolean) {
    val authUiState by MalAuthRepository.uiState.collectAsState()
    val settingsUiState by MalSettingsRepository.uiState.collectAsState()
    val isSyncing by MalSyncCoordinator.isSyncing.collectAsState()
    val syncMessage by MalSyncCoordinator.syncMessage.collectAsState()

    var showDisconnectConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (authUiState.mode) {
            MalConnectionMode.CONNECTED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authUiState.username ?: "Utente MAL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Connesso",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        val expiresAt = authUiState.tokenExpiresAtMillis
                        if (expiresAt != null && expiresAt > 0L) {
                            val remainingMs = expiresAt - remember {
                                WatchProgressClock.nowEpochMs()
                            }
                            val label = when {
                                remainingMs <= 0 -> "Token scaduto"
                                remainingMs < 60_000L -> "Scade tra <1 min"
                                remainingMs < 3_600_000L -> "Scade tra ${remainingMs / 60_000L} min"
                                remainingMs < 86_400_000L -> "Scade tra ${remainingMs / 3_600_000L}h ${(remainingMs % 3_600_000L) / 60_000L}m"
                                else -> "Scade tra ${remainingMs / 86_400_000L}d"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Button(
                        onClick = { showDisconnectConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(stringResource(Res.string.settings_mal_disconnect))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val errMsg = authUiState.errorMessage
                if (errMsg != null) {
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                SettingsSwitchRow(
                    title = "Abilita Sync MAL",
                    description = "Controllo principale sync per i progressi di visione MAL.",
                    checked = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { MalSettingsRepository.setEnableSync(it) },
                )

                SettingsSwitchRow(
                    title = "Sync Progresso Visione",
                    description = "Sincronizza le modifiche dello stato di visione con MAL.",
                    checked = settingsUiState.syncWatching,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { MalSettingsRepository.setSyncWatching(it) },
                )

                SettingsSwitchRow(
                    title = "Sync Automatico Durante la Visione",
                    description = "Carica i progressi automaticamente mentre guardi i video.",
                    checked = settingsUiState.autoSync,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { MalSettingsRepository.setAutoSync(it) },
                )

                SettingsSwitchRow(
                    title = "Sync all'Avvio dell'App",
                    description = "Esegue un sync completo ogni volta che l'app viene aperta.",
                    checked = settingsUiState.syncOnLaunch,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { MalSettingsRepository.setSyncOnLaunch(it) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (settingsUiState.enableSync) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Button(
                            onClick = { MalSyncCoordinator.syncNow() },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizzazione...")
                            } else {
                                Text("Sync Ora")
                            }
                        }

                        if (!syncMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = syncMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (settingsUiState.lastSyncTimestamp > 0L) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ultimo Sync: Pochi secondi fa",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            MalConnectionMode.AWAITING_APPROVAL -> {
                if (authUiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = stringResource(Res.string.settings_mal_finish_sign_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { MalAuthRepository.onCancelAuthorization() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }

            MalConnectionMode.DISCONNECTED -> {
                if (!authUiState.credentialsConfigured) {
                    Text(
                        text = stringResource(Res.string.settings_mal_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    return@Column
                }

                val uriHandler = LocalUriHandler.current

                Text(
                    text = "Accedi con MyAnimeList via OAuth per sincronizzare le librerie anime e lo stato di tracciamento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val authUrl = MalAuthRepository.onConnectRequested()
                        if (authUrl != null) {
                            runCatching { uriHandler.openUri(authUrl) }
                                .onFailure { MalAuthRepository.onAuthLaunchFailed(it.message ?: "Errore sconosciuto") }
                        }
                    },
                ) {
                    Text(stringResource(Res.string.settings_mal_connect))
                }
            }
        }

        val errMsg = authUiState.errorMessage
        if (errMsg != null && authUiState.mode != MalConnectionMode.CONNECTED) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showDisconnectConfirm) {
        BasicAlertDialog(onDismissRequest = { showDisconnectConfirm = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Disconnettere MAL?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Sei sicuro di voler disconnettere l'account MyAnimeList? Verranno cancellate le impostazioni locali di sync dei progressi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { showDisconnectConfirm = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) { Text("Annulla") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                MalAuthRepository.onDisconnectRequested()
                                showDisconnectConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text("Disconnetti") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MalSectionSettingsRow(
    item: MalSectionSettings,
    isTablet: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.type,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(
                checked = item.enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = tokens.colors.textMuted,
                    uncheckedTrackColor = tokens.colors.borderDefault,
                ),
            )
            IconButton(
                modifier = with(dragHandleScope) {
                    Modifier.draggableHandle(
                        onDragStarted = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragStopped = { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    )
                },
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Riordina",
                    tint = tokens.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun MalSectionsList(
    isTablet: Boolean,
    items: List<MalSectionSettings>,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        val mutable = items.toMutableList()
        val moved = mutable.removeAt(from.index)
        mutable.add(to.index, moved)
        MalSettingsRepository.setLibrarySections(mutable)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    SettingsGroup(isTablet = isTablet) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isTablet) 550.dp else 400.dp),
            state = lazyListState,
        ) {
            itemsIndexed(items, key = { _, item -> item.type }) { index, item ->
                ReorderableItem(reorderableLazyListState, key = item.type) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    Surface(shadowElevation = elevation) {
                        Column {
                            if (index > 0) SettingsGroupDivider(isTablet = isTablet)
                            MalSectionSettingsRow(
                                item = item,
                                isTablet = isTablet,
                                onEnabledChange = { enabled ->
                                    val updated = items.mapIndexed { i, s ->
                                        if (i == index) s.copy(enabled = enabled) else s
                                    }
                                    MalSettingsRepository.setLibrarySections(updated)
                                },
                                dragHandleScope = this@ReorderableItem,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MalWatchedThresholdRow(isTablet: Boolean, threshold: Float) {
    val percent = (threshold * 100f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Segna Episodio Come Visto al",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "L'episodio viene segnato come visto dopo aver raggiunto questo punto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = threshold,
            onValueChange = { MalSettingsRepository.setMarkWatchedThreshold(it) },
            valueRange = 0f..1f,
            steps = 19,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("100%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
