package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.simkl.SimklAuthError
import com.nuvio.app.features.simkl.SimklAuthRepository
import com.nuvio.app.features.simkl.SimklAuthUiState
import com.nuvio.app.features.simkl.SimklConnectionMode
import com.nuvio.app.features.simkl.SimklSyncRepository
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_simkl_attribution_body
import nuvio.composeapp.generated.resources.settings_simkl_attribution_title
import nuvio.composeapp.generated.resources.settings_simkl_authorization_expired
import nuvio.composeapp.generated.resources.settings_simkl_authorization_revoked
import nuvio.composeapp.generated.resources.settings_simkl_connected_as
import nuvio.composeapp.generated.resources.settings_simkl_connect
import nuvio.composeapp.generated.resources.settings_simkl_disconnect
import nuvio.composeapp.generated.resources.settings_simkl_finish_sign_in
import nuvio.composeapp.generated.resources.settings_simkl_invalid_callback
import nuvio.composeapp.generated.resources.settings_simkl_missing_credentials
import nuvio.composeapp.generated.resources.settings_simkl_sign_in_failed
import nuvio.composeapp.generated.resources.settings_simkl_sync_now
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.simklSettingsContent(
    isTablet: Boolean,
) {
    item {
        val horizontalPadding = if (isTablet) 20.dp else 16.dp
        val verticalPadding = if (isTablet) 16.dp else 14.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            Text(
                text = stringResource(Res.string.settings_simkl_attribution_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.settings_simkl_attribution_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    item {
        val authUiState by remember {
            SimklAuthRepository.ensureLoaded()
            SimklAuthRepository.uiState
        }.collectAsState()
        SimklConnectionCard(
            isTablet = isTablet,
            uiState = authUiState,
        )
    }
}

@Composable
private fun SimklConnectionCard(
    isTablet: Boolean,
    uiState: SimklAuthUiState,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val uriHandler = LocalUriHandler.current
    val syncState by SimklSyncRepository.state.collectAsState()

    val errorText = simklErrorMessage(uiState.error)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!uiState.credentialsConfigured) {
            Text(
                text = stringResource(Res.string.settings_simkl_missing_credentials),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
            return
        }

        when (uiState.mode) {
            SimklConnectionMode.DISCONNECTED -> {
                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            val authUrl = SimklAuthRepository.onConnectRequested()
                            if (authUrl != null) {
                                runCatching { uriHandler.openUri(authUrl) }
                            }
                        },
                    ) {
                        Text(stringResource(Res.string.settings_simkl_connect))
                    }
                }
            }

            SimklConnectionMode.AWAITING_APPROVAL -> {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = stringResource(Res.string.settings_simkl_finish_sign_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { SimklAuthRepository.onCancelAuthorization() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Annulla")
                    }
                    Button(
                        onClick = {
                            val url = SimklAuthRepository.pendingAuthorizationUrl()
                                ?: SimklAuthRepository.onConnectRequested()
                            if (url != null) {
                                runCatching { uriHandler.openUri(url) }
                            }
                        },
                    ) {
                        Text("Riapri Browser")
                    }
                }
            }

            SimklConnectionMode.CONNECTED -> {
                Text(
                    text = stringResource(Res.string.settings_simkl_connected_as, uiState.username ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )

                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { SimklSyncRepository.refreshAsync(TrackingRefreshIntent.USER_INITIATED) },
                        enabled = !syncState.isLoading,
                    ) {
                        if (syncState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(stringResource(Res.string.settings_simkl_sync_now))
                    }
                    OutlinedButton(
                        onClick = { SimklAuthRepository.onDisconnectRequested() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(Res.string.settings_simkl_disconnect))
                    }
                }
            }
        }

        if (errorText != null && uiState.mode != SimklConnectionMode.CONNECTED) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun simklErrorMessage(error: SimklAuthError?): String? = when (error) {
    null, SimklAuthError.MISSING_CLIENT_ID -> null
    SimklAuthError.INVALID_CALLBACK,
    SimklAuthError.INVALID_CALLBACK_STATE,
    -> stringResource(Res.string.settings_simkl_invalid_callback)
    SimklAuthError.AUTHORIZATION_EXPIRED ->
        stringResource(Res.string.settings_simkl_authorization_expired)
    SimklAuthError.TOKEN_EXCHANGE_FAILED,
    SimklAuthError.INVALID_TOKEN_RESPONSE,
    -> stringResource(Res.string.settings_simkl_sign_in_failed)
    SimklAuthError.AUTHORIZATION_REVOKED ->
        stringResource(Res.string.settings_simkl_authorization_revoked)
}
