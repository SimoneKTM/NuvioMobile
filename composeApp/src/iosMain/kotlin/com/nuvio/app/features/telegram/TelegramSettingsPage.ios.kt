package com.nuvio.app.features.telegram

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
internal actual fun telegramSettingsContent(isTablet: Boolean) {
    Text(
        text = "Telegram integration is not yet available on iOS.",
        modifier = Modifier.padding(16.dp),
    )
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

    actual fun sendCode(code: String) {}
    actual fun sendPassword(password: String) {}
    actual fun sendPhone(phone: String) {}
    actual fun logout() {
        _uiState.value = TelegramAuthState.Idle
    }
}
