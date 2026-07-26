package com.nuvio.app.features.telegram

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
internal actual fun telegramSettingsContent(isTablet: Boolean) {
    val state by TelegramSettingsRepository.uiState.collectAsState()
    var codeInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isTablet) 0.dp else 16.dp),
    ) {
        when (val s = state) {
            is TelegramAuthState.Idle -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Connect your Telegram account to search channels for movies and shows.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone number (including country code)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { TelegramSettingsRepository.sendPhone(phoneInput) },
                    enabled = phoneInput.isNotBlank(),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Send Code")
                }
            }
            is TelegramAuthState.Initializing -> {
                Spacer(Modifier.height(16.dp))
                Text("Initializing Telegram client...")
            }
            is TelegramAuthState.WaitPhone -> {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone number (including country code)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { TelegramSettingsRepository.sendPhone(phoneInput) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Send Code")
                }
            }
            is TelegramAuthState.WaitQr -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Scan this QR code or use the link to log in:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        text = s.link,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            is TelegramAuthState.WaitCode -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Enter the code sent to your Telegram:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= 8) codeInput = it },
                    label = { Text("Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { TelegramSettingsRepository.sendCode(codeInput) },
                    enabled = codeInput.isNotBlank(),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Verify")
                }
            }
            is TelegramAuthState.WaitPassword -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Enter your 2FA password:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("2FA Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { TelegramSettingsRepository.sendPassword(passwordInput) },
                    enabled = passwordInput.isNotBlank(),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Submit")
                }
            }
            is TelegramAuthState.Ready -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Connected as ${s.firstName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Telegram channels can now be searched for content.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { TelegramSettingsRepository.logout() },
                ) {
                    Text("Disconnect")
                }
            }
            is TelegramAuthState.Error -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = s.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { TelegramSettingsRepository.initialize() },
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

internal actual object TelegramSettingsRepository {
    private val _uiState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Idle)
    private var isLoaded = false

    actual val uiState: StateFlow<TelegramAuthState> = _uiState.asStateFlow()

    actual fun ensureLoaded() {
        if (!isLoaded) {
            isLoaded = true
            _uiState.value = TelegramAuthState.Idle
        }
    }

    actual fun initialize() {
        _uiState.value = TelegramAuthState.Initializing
    }

    actual fun requestQr() {
        _uiState.value = TelegramAuthState.WaitQr(link = "tg://login?token=placeholder")
    }

    actual fun sendCode(code: String) {
        _uiState.value = TelegramAuthState.Idle
    }

    actual fun sendPassword(password: String) {
        _uiState.value = TelegramAuthState.Idle
    }

    actual fun sendPhone(phone: String) {
        _uiState.value = TelegramAuthState.Idle
    }

    actual fun logout() {
        _uiState.value = TelegramAuthState.Idle
    }
}
