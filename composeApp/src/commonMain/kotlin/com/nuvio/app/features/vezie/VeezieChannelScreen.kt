package com.nuvio.app.features.vezie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun VeezieSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val state by VeezieChannelRepository.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            VeezieChannelRepository.initialize()
        }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var newChannelName by remember { mutableStateOf("") }
    var newChannelUrl by remember { mutableStateOf("") }

    var easyProxyUrl by remember { mutableStateOf("") }
    var easyProxyEmail by remember { mutableStateOf("") }
    var easyProxyPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val url = VeezieStorage.loadEasyProxyUrl()
            val email = VeezieStorage.loadEasyProxyEmail()
            val password = VeezieStorage.loadEasyProxyPassword()
            easyProxyUrl = url
            easyProxyEmail = email
            easyProxyPassword = password
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NuvioSectionLabel("Canali Veezie (Scraper)")

        NuvioSurfaceCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Rounded.LiveTv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "EasyProxy",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "Proxy per bypassare Cloudflare/JS challenge sui canali HTML. Tutte le richieste HTTP vengono instradate attraverso EasyProxy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                NuvioInputField(
                    value = easyProxyUrl,
                    onValueChange = { easyProxyUrl = it },
                    placeholder = "https://mio-server-easyproxy.com",
                )
                Spacer(Modifier.height(8.dp))
                NuvioInputField(
                    value = easyProxyPassword,
                    onValueChange = { easyProxyPassword = it },
                    placeholder = "la-tua-password-api",
                )
                Spacer(Modifier.height(8.dp))
                NuvioPrimaryButton(
                    text = "Salva EasyProxy",
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            VeezieStorage.saveEasyProxyUrl(easyProxyUrl.trim())
                            VeezieStorage.saveEasyProxyPassword(easyProxyPassword.trim())
                            VeezieEasyProxy.configure(easyProxyUrl.trim(), easyProxyEmail.trim(), easyProxyPassword.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NuvioSectionLabel("Canali")

        if (state.channels.isEmpty()) {
            Text(
                "Nessun canale configurato. Aggiungi un canale Veezie (URL HTML o JSON).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.channels.forEach { channel ->
            ChannelCard(
                channel = channel,
                onToggle = { scope.launch(Dispatchers.IO) { VeezieChannelRepository.toggleChannel(channel.id, it) } },
                onRemove = { scope.launch(Dispatchers.IO) { VeezieChannelRepository.removeChannel(channel.id) } },
            )
        }

        NuvioPrimaryButton(
            text = "Aggiungi Canale",
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
        )

        NuvioSectionLabel("Come funziona")

        Text(
            "I canali Veezie possono essere sia JSON strutturati che siti HTML. " +
                    "Per i siti HTML (cb01, cineblog, animeworld, ecc.), l'app esegue scraping " +
                    "locale con supporto EasyProxy per bypassare Cloudflare. " +
                    "L'add-on cerca il titolo, estrae gli host video (Mixdrop, VOE, Supervideo) " +
                    "e risolve il flusso diretto .mp4/.m3u8 direttamente nell'app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Veezie Channel") },
            text = {
                Column {
                    Text(
                        "Enter the channel details:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    NuvioInputField(
                        value = newChannelName,
                        onValueChange = { newChannelName = it },
                        placeholder = "Channel Name",
                    )
                    Spacer(Modifier.height(8.dp))
                    NuvioInputField(
                        value = newChannelUrl,
                        onValueChange = { newChannelUrl = it },
                        placeholder = "https://cb01uno.lol o https://example.com/channel.json",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newChannelName.isNotBlank() && newChannelUrl.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                VeezieChannelRepository.addChannel(
                                    name = newChannelName.trim(),
                                    url = newChannelUrl.trim(),
                                )
                            }
                            newChannelName = ""
                            newChannelUrl = ""
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
private fun ChannelCard(
    channel: VeezieChannelConfig,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    NuvioSurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                Icons.Rounded.Tv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    channel.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(checked = channel.enabled, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
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
