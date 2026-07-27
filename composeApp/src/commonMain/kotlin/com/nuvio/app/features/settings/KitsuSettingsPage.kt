package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import coil3.compose.AsyncImage
import com.nuvio.app.core.i18n.localizedShortMonthName
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.kitsu.KitsuAuthRepository
import com.nuvio.app.features.kitsu.KitsuConnectionMode
import com.nuvio.app.features.kitsu.KitsuSectionSettings
import com.nuvio.app.features.kitsu.KitsuSettingsRepository
import com.nuvio.app.features.kitsu.KitsuSyncCoordinator
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_kitsu_attribution_body
import nuvio.composeapp.generated.resources.settings_kitsu_attribution_title
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

internal fun LazyListScope.kitsuSettingsContent(
    isTablet: Boolean,
) {
    item {
        SettingsGroup(isTablet = isTablet) {
            KitsuBrandIntro(isTablet = isTablet)
        }
    }

    item {
        SettingsSection(
            title = "Impostazioni Integrazione Kitsu",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                KitsuAuthRepository.ensureLoaded()
                KitsuConnectionCard(isTablet = isTablet)
            }
        }
    }

    item {
        val authUiState by KitsuAuthRepository.uiState.collectAsState()
        val settingsUiState by KitsuSettingsRepository.uiState.collectAsState()

        if (authUiState.mode == KitsuConnectionMode.CONNECTED) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSection(
                title = "Riproduzione",
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    KitsuWatchedThresholdRow(
                        isTablet = isTablet,
                        threshold = settingsUiState.markWatchedThreshold
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 16.dp))
                    SettingsSwitchRow(
                        title = "Aggiungi Automaticamente Nuovi Anime a Kitsu",
                        description = "Se attivo, guardare un anime non nella tua libreria Kitsu lo aggiungerà automaticamente alla tua lista In Corso e sincronizzerà i progressi.",
                        checked = settingsUiState.autoAddNewAnime,
                        enabled = settingsUiState.enableSync,
                        isTablet = isTablet,
                        onCheckedChange = { KitsuSettingsRepository.setAutoAddNewAnime(it) }
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
                        onClick = { KitsuSettingsRepository.resetLibrarySections() }
                    )
                }
            ) {
                KitsuSectionsList(
                    isTablet = isTablet,
                    items = settingsUiState.librarySections
                )
            }
        }
    }
}

@Composable
private fun KitsuBrandIntro(isTablet: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = if (isTablet) 24.dp else 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(Res.string.settings_kitsu_attribution_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.settings_kitsu_attribution_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KitsuConnectionCard(isTablet: Boolean) {
    val authUiState by KitsuAuthRepository.uiState.collectAsState()
    val settingsUiState by KitsuSettingsRepository.uiState.collectAsState()
    val isSyncing by KitsuSyncCoordinator.isSyncing.collectAsState()
    val syncMessage by KitsuSyncCoordinator.syncMessage.collectAsState()

    var showDisconnectConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (authUiState.mode) {
            KitsuConnectionMode.CONNECTED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!authUiState.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = authUiState.avatarUrl,
                            contentDescription = authUiState.username,
                            modifier = Modifier.size(48.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authUiState.username?.take(1)?.uppercase() ?: "K",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authUiState.username ?: "Utente Kitsu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connesso",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val expiresAt = authUiState.tokenExpiresAtEpochMs
                        if (expiresAt != null && expiresAt > 0L) {
                            val remainingMs = expiresAt - androidx.compose.runtime.remember {
                                com.nuvio.app.features.watchprogress.WatchProgressClock.nowEpochMs()
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { showDisconnectConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Disconnetti")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Abilita Sync Kitsu",
                    description = "Controllo principale sync per i progressi di visione Kitsu.",
                    checked = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { KitsuSettingsRepository.setEnableSync(it) }
                )

                SettingsSwitchRow(
                    title = "Sync Progresso Visione",
                    description = "Sincronizza le modifiche dello stato di visione con Kitsu.",
                    checked = settingsUiState.syncWatching,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { KitsuSettingsRepository.setSyncWatching(it) }
                )

                SettingsSwitchRow(
                    title = "Sync Automatico Durante la Visione",
                    description = "Carica i progressi automaticamente mentre guardi i video.",
                    checked = settingsUiState.autoSync,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { KitsuSettingsRepository.setAutoSync(it) }
                )

                SettingsSwitchRow(
                    title = "Sync all'Avvio dell'App",
                    description = "Esegue un sync completo ogni volta che l'app viene aperta.",
                    checked = settingsUiState.syncOnLaunch,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { KitsuSettingsRepository.setSyncOnLaunch(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (settingsUiState.enableSync) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Button(
                            onClick = { KitsuSyncCoordinator.syncNow() },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (settingsUiState.lastSyncTimestamp > 0L) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ultimo Sync: Pochi secondi fa",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            KitsuConnectionMode.DISCONNECTED -> {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                Text(
                    text = "Accedi con email e password Kitsu per sincronizzare le librerie anime e lo stato di tracciamento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { KitsuAuthRepository.loginWithPassword(email.trim(), password) },
                            enabled = email.isNotBlank() && password.isNotBlank() && !authUiState.isLoading,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (authUiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Accesso in corso...")
                            } else {
                                Text("Accedi", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                if (!authUiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = authUiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            KitsuConnectionMode.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showDisconnectConfirm) {
        BasicAlertDialog(onDismissRequest = { showDisconnectConfirm = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Disconnettere Kitsu?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sei sicuro di voler disconnettere l'account Kitsu? Verranno cancellate le impostazioni locali di sync dei progressi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showDisconnectConfirm = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text("Annulla") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                KitsuAuthRepository.disconnect()
                                showDisconnectConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) { Text("Disconnetti") }
                    }
                }
            }
        }
    }


}

@Composable
private fun KitsuSectionSettingsRow(
    item: KitsuSectionSettings,
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
private fun KitsuSectionsList(
    isTablet: Boolean,
    items: List<KitsuSectionSettings>,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        KitsuSettingsRepository.moveSection(from.index, to.index)
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
                            KitsuSectionSettingsRow(
                                item = item,
                                isTablet = isTablet,
                                onEnabledChange = { enabled -> KitsuSettingsRepository.setSectionEnabled(item.type, enabled) },
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
private fun KitsuWatchedThresholdRow(isTablet: Boolean, threshold: Float) {
    val percent = (threshold * 100f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 12.dp)
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
            onValueChange = { KitsuSettingsRepository.setMarkWatchedThreshold(it) },
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
