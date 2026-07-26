package com.nuvio.app.features.telegram

import android.content.Context

internal object TelegramClient {
    private var isInitialized = false
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext
        isInitialized = true
    }

    fun isReady(): Boolean = isInitialized

    fun destroy() {
        isInitialized = false
        context = null
    }
}
