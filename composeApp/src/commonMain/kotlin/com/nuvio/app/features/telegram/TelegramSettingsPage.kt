package com.nuvio.app.features.telegram

import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyListScope

@Composable
internal expect fun telegramSettingsContent(isTablet: Boolean)

internal expect object TelegramSettingsRepository {
    fun ensureLoaded()
    val uiState: kotlinx.coroutines.flow.StateFlow<TelegramAuthState>
    fun initialize()
    fun requestQr()
    fun sendCode(code: String)
    fun sendPassword(password: String)
    fun sendPhone(phone: String)
    fun logout()
}
