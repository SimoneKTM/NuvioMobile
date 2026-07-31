package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.vpn.VpnController
import com.nuvio.app.features.vpn.VpnProfile
import com.nuvio.app.features.vpn.VpnRuntimeState
import com.nuvio.app.features.vpn.VpnSettings
import com.nuvio.app.features.vpn.VpnSettingsRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_vpn_add
import nuvio.composeapp.generated.resources.settings_vpn_add_profile
import nuvio.composeapp.generated.resources.settings_vpn_add_title
import nuvio.composeapp.generated.resources.settings_vpn_cancel
import nuvio.composeapp.generated.resources.settings_vpn_config_placeholder
import nuvio.composeapp.generated.resources.settings_vpn_empty_message
import nuvio.composeapp.generated.resources.settings_vpn_empty_title
import nuvio.composeapp.generated.resources.settings_vpn_enable
import nuvio.composeapp.generated.resources.settings_vpn_enable_description
import nuvio.composeapp.generated.resources.settings_vpn_invalid_config
import nuvio.composeapp.generated.resources.settings_vpn_name_optional
import nuvio.composeapp.generated.resources.settings_vpn_no_profile
import nuvio.composeapp.generated.resources.settings_vpn_remove
import nuvio.composeapp.generated.resources.settings_vpn_remove_message
import nuvio.composeapp.generated.resources.settings_vpn_remove_title
import nuvio.composeapp.generated.resources.settings_vpn_section_profiles
import nuvio.composeapp.generated.resources.settings_vpn_section_status
import nuvio.composeapp.generated.resources.settings_vpn_status_off
import nuvio.composeapp.generated.resources.settings_vpn_status_on
import nuvio.composeapp.generated.resources.settings_vpn_status_pending
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.vpnSettingsContent(
    isTablet: Boolean,
    settings: VpnSettings,
) {
    item {
        VpnSettingsPageContent(isTablet = isTablet, settings = settings)
    }
}

@Composable
private fun VpnSettingsPageContent(
    isTablet: Boolean,
    settings: VpnSettings,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var profileToRemove by rememberSaveable { mutableStateOf<String?>(null) }
    val runtimeState by VpnController.runtimeState.collectAsState()
    val activeProfile = settings.activeProfile
    val isActive = runtimeState == VpnRuntimeState.ACTIVE

    Column {
        SettingsSection(
            title = stringResource(Res.string.settings_vpn_section_status),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_vpn_enable),
                    description = activeProfile?.let { "${it.label} · ${it.serverName}" }
                        ?: stringResource(Res.string.settings_vpn_no_profile),
                    checked = settings.enabled,
                    enabled = settings.hasProfiles,
                    isTablet = isTablet,
                    onCheckedChange = VpnSettingsRepository::setEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                VpnStatusRow(
                    isActive = isActive && settings.enabled,
                    pendingPermission = settings.enabled && !isActive,
                    isTablet = isTablet,
                )
            }
        }
        SettingsSection(
            title = stringResource(Res.string.settings_vpn_section_profiles),
            isTablet = isTablet,
        ) {
            if (settings.profiles.isEmpty()) {
                SettingsGroup(isTablet = isTablet) {
                    VpnEmptyProfilesRow(isTablet = isTablet)
                }
            }
            settings.profiles.forEach { profile ->
                SettingsGroup(isTablet = isTablet) {
                    VpnProfileRow(
                        profile = profile,
                        selected = profile.id == settings.activeProfileId,
                        isTablet = isTablet,
                        onSelect = { VpnSettingsRepository.selectProfile(profile.id) },
                        onRemove = { profileToRemove = profile.id },
                    )
                }
            }
            SettingsGroupDivider(isTablet = isTablet)
            SettingsNavigationRow(
                title = stringResource(Res.string.settings_vpn_add_profile),
                description = stringResource(Res.string.settings_vpn_config_placeholder),
                icon = Icons.Rounded.Add,
                isTablet = isTablet,
                onClick = { showAddDialog = true },
            )
        }
    }

    if (showAddDialog) {
        VpnAddProfileDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { label, configText ->
                if (VpnSettingsRepository.addProfile(label, configText)) {
                    showAddDialog = false
                    true
                } else {
                    false
                }
            },
        )
    }
    profileToRemove?.let { profileId ->
        val profile = settings.profiles.firstOrNull { it.id == profileId }
        if (profile != null) {
            VpnRemoveProfileDialog(
                profile = profile,
                onDismiss = { profileToRemove = null },
                onConfirm = {
                    VpnSettingsRepository.removeProfile(profile.id)
                    profileToRemove = null
                },
            )
        }
    }
}

@Composable
private fun VpnStatusRow(
    isActive: Boolean,
    pendingPermission: Boolean,
    isTablet: Boolean,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val status = when {
        isActive -> stringResource(Res.string.settings_vpn_status_on)
        pendingPermission -> stringResource(Res.string.settings_vpn_status_pending)
        else -> stringResource(Res.string.settings_vpn_status_off)
    }
    val dotColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        pendingPermission -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = dotColor, shape = CircleShape),
        )
        Text(
            text = status,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun VpnProfileRow(
    profile: VpnProfile,
    selected: Boolean,
    isTablet: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 12.dp else 10.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = horizontalPadding, top = verticalPadding, bottom = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp)
                .widthIn(max = if (isTablet) 560.dp else Dp.Unspecified),
        ) {
            Text(
                text = profile.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = profile.serverName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.padding(end = if (isTablet) 8.dp else 4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(Res.string.settings_vpn_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VpnEmptyProfilesRow(isTablet: Boolean) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_vpn_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(Res.string.settings_vpn_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VpnAddProfileDialog(
    onDismiss: () -> Unit,
    onAdd: (label: String, configText: String) -> Boolean,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var configText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_vpn_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.settings_vpn_name_optional)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = configText,
                    onValueChange = { configText = it },
                    label = { Text(stringResource(Res.string.settings_vpn_config_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(min = 140.dp),
                )
                if (showError) {
                    Text(
                        text = stringResource(Res.string.settings_vpn_invalid_config),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.settings_vpn_cancel))
                    }
                    TextButton(
                        onClick = {
                            if (onAdd(name, configText)) {
                                name = ""
                                configText = ""
                                showError = false
                            } else {
                                showError = true
                            }
                        },
                    ) {
                        Text(stringResource(Res.string.settings_vpn_add))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VpnRemoveProfileDialog(
    profile: VpnProfile,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_vpn_remove_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.settings_vpn_remove_message, profile.label),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.settings_vpn_cancel))
                    }
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = stringResource(Res.string.settings_vpn_remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
