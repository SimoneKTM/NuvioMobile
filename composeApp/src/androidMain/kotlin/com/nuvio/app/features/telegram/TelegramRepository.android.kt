package com.nuvio.app.features.telegram

internal object TelegramRepository {
    fun isAvailable(): Boolean = TelegramClient.isReady()

    suspend fun searchMessages(
        query: String,
        limit: Int = 50,
    ): List<TelegramMessageResult> = emptyList()

    suspend fun getMessageMedia(messageId: Long, chatId: Long): ByteArray? = null
}

internal data class TelegramMessageResult(
    val messageId: Long,
    val chatId: Long,
    val chatTitle: String,
    val text: String,
    val fileSize: Long = 0,
    val mimeType: String = "",
)
