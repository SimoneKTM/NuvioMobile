package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import com.nuvio.app.features.player.PlayerPlaybackSnapshot
import com.nuvio.app.features.player.desktop.AwtNativeViewResolver
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.NativePlayerBridge
import com.nuvio.app.features.player.desktop.NativePlayerEventSink
import com.nuvio.app.features.player.desktop.NativePlayerHost
import javax.swing.SwingUtilities
import kotlin.concurrent.Volatile
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun HeroTrailerPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
) {
    if (DesktopHostOs.current != DesktopHostOs.WINDOWS) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val host = remember { NativePlayerHost() }
    val player = remember(host) { HeroTrailerPlayer(host) }
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnEnded = rememberUpdatedState(onEnded)
    val latestOnError = rememberUpdatedState(onError)
    val readyNotified = remember(sourceUrl) { mutableStateOf(false) }

    LaunchedEffect(player) {
        player.onEnded = { latestOnEnded.value() }
        player.onError = { latestOnError.value() }
    }

    DisposableEffect(player, sourceUrl) {
        onDispose {
            player.dispose()
            host.onPeerReady = null
        }
    }

    LaunchedEffect(sourceUrl) {
        readyNotified.value = false
        if (sourceUrl.isBlank()) return@LaunchedEffect
        player.attach(sourceUrl)
    }

    LaunchedEffect(player) {
        var lastEndedSeen = false
        while (isActive) {
            val snapshot = player.snapshot()
            if (snapshot.isEnded) {
                if (!lastEndedSeen) {
                    lastEndedSeen = true
                    latestOnEnded.value()
                }
            } else {
                lastEndedSeen = false
                if (!snapshot.isLoading && !readyNotified.value && snapshot.durationMs > 0L) {
                    readyNotified.value = true
                    latestOnReady.value()
                }
            }
            delay(250L)
        }
    }

    LaunchedEffect(player, muted) {
        player.requestMuted(muted)
    }

    LaunchedEffect(player, playWhenReady) {
        if (playWhenReady) player.requestPlay() else player.requestPause()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        SwingPanel(
            factory = { host },
            modifier = Modifier.fillMaxSize(),
            background = Color.Black,
        )
    }
}

private class HeroTrailerPlayer(private val host: NativePlayerHost) {
    @Volatile
    private var handle: Long = 0L
    private var pendingSource: String? = null
    private var desiredMuted = true
    private var desiredPlaying = true

    var onEnded: (() -> Unit)? = null
    var onError: (() -> Unit)? = null

    private val eventSink = NativePlayerEventSink { type, _ ->
        SwingUtilities.invokeLater { handleEvent(type) }
    }

    fun attach(sourceUrl: String) {
        pendingSource = sourceUrl
        host.onPeerReady = { attachNativePlayer() }
        if (host.isDisplayable) {
            attachNativePlayer()
        }
    }

    private fun attachNativePlayer() {
        val source = pendingSource ?: return
        SwingUtilities.invokeLater {
            if (!host.isDisplayable) return@invokeLater
            disposePlayerHandle()
            runCatching {
                val hostViewPtr = AwtNativeViewResolver.resolveNativeViewPointer(host)
                handle = NativePlayerBridge.createBare(
                    hostViewPtr = hostViewPtr,
                    sourceUrl = source,
                    headerLines = emptyArray(),
                    playWhenReady = true,
                    initialPositionMs = 0L,
                    eventSink = eventSink,
                )
                if (handle == 0L) error("Native hero trailer player did not return a handle.")
                NativePlayerBridge.setMuted(handle, desiredMuted)
                if (desiredPlaying) {
                    NativePlayerBridge.setPaused(handle, false)
                }
            }.onFailure {
                onError?.invoke()
            }
        }
    }

    fun requestMuted(muted: Boolean) {
        desiredMuted = muted
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.setMuted(it, muted) }
    }

    fun requestPlay() {
        desiredPlaying = true
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.setPaused(it, false) }
    }

    fun requestPause() {
        desiredPlaying = false
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.setPaused(it, true) }
    }

    fun snapshot(): PlayerPlaybackSnapshot {
        val current = handle
        if (current == 0L) return PlayerPlaybackSnapshot(isLoading = true)
        return runCatching {
            val isLoading = NativePlayerBridge.isLoading(current)
            val isEnded = NativePlayerBridge.isEnded(current)
            PlayerPlaybackSnapshot(
                isLoading = isLoading,
                isPlaying = !NativePlayerBridge.isPaused(current) && !isLoading && !isEnded,
                isEnded = isEnded,
                durationMs = NativePlayerBridge.durationMs(current),
                positionMs = NativePlayerBridge.positionMs(current),
                bufferedPositionMs = NativePlayerBridge.bufferedPositionMs(current),
            )
        }.getOrDefault(PlayerPlaybackSnapshot(isLoading = true))
    }

    fun dispose() {
        disposePlayerHandle()
        pendingSource = null
        host.onPeerReady = null
    }

    private fun disposePlayerHandle() {
        val current = handle
        handle = 0L
        if (current != 0L) {
            runCatching { NativePlayerBridge.dispose(current) }
        }
    }

    private fun handleEvent(type: String) {
        when (type) {
            "ended" -> onEnded?.invoke()
            "playbackError" -> onError?.invoke()
        }
    }
}