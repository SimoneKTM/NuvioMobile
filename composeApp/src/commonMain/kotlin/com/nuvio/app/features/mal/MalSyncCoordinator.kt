package com.nuvio.app.features.mal

import co.touchlab.kermit.Logger
import com.nuvio.app.features.watchprogress.WatchProgressClock
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.WatchedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.sync_completed_successfully
import nuvio.composeapp.generated.resources.sync_failed
import nuvio.composeapp.generated.resources.sync_progress_mal
import nuvio.composeapp.generated.resources.sync_unknown_error
import org.jetbrains.compose.resources.getString

object MalSyncCoordinator {
    private val log = Logger.withTag("MalSync")
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var syncJob: Job? = null

    fun syncNow() {
        if (_isSyncing.value) return
        syncJob = syncScope.launch {
            runSync(force = true)
        }
    }

    fun syncOnLaunchIfNeeded() {
        syncScope.launch {
            MalSettingsRepository.ensureLoaded()
            MalAuthRepository.ensureLoaded()
            val settings = MalSettingsRepository.uiState.value
            val isAuth = MalAuthRepository.isAuthenticated.value
            if (isAuth && settings.enableSync && settings.syncOnLaunch) {
                log.i { "MAL sync on launch triggered" }
                runSync(force = false)
            }
        }
    }

    fun handlePlaybackProgressUpdated(entry: WatchProgressEntry) {
        syncScope.launch {
            MalSettingsRepository.ensureLoaded()
            MalAuthRepository.ensureLoaded()
            val settings = MalSettingsRepository.uiState.value
            val isAuth = MalAuthRepository.isAuthenticated.value
            if (!isAuth || !settings.enableSync || !settings.autoSync || !settings.syncWatching) {
                return@launch
            }

            if (entry.parentMetaType.equals("series", ignoreCase = true) || entry.parentMetaType.equals("anime", ignoreCase = true)) {
                val progress = entry.episodeNumber ?: return@launch
                val malId = extractMalId(entry.parentMetaId, entry.videoId) ?: return@launch

                MalLibraryRepository.ensureLoaded()

                if (!settings.autoAddNewAnime) {
                    val inLibrary = MalLibraryRepository.uiState.value.allItems.any { it.id == malId }
                    if (!inLibrary) return@launch
                }

                val token = MalAuthRepository.currentAccessToken() ?: return@launch
                MalAuthRepository.refreshTokenIfNeeded(force = false)
                val freshToken = MalAuthRepository.currentAccessToken() ?: return@launch

                val libraryItem = MalLibraryRepository.uiState.value.allItems.find { it.id == malId }
                val targetProgress = if (entry.isCompleted) {
                    progress
                } else {
                    maxOf(libraryItem?.episodesWatched ?: 0, progress - 1)
                }
                val targetStatus = if (entry.isCompleted) "completed" else "watching"

                val success = MalApiClient.updateAnimeListStatus(
                    accessToken = freshToken,
                    animeId = malId,
                    status = targetStatus,
                    numWatchedEpisodes = targetProgress,
                )

                if (success) {
                    log.i { "MAL progress synced for anime $malId: ep $targetProgress, status $targetStatus" }
                    MalLibraryRepository.refreshNow()
                } else {
                    log.w { "MAL sync failed for anime $malId" }
                }
            }
        }
    }

    private suspend fun runSync(force: Boolean) {
        syncMutex.withLock {
            MalSettingsRepository.ensureLoaded()
            MalAuthRepository.ensureLoaded()
            val settings = MalSettingsRepository.uiState.value
            val isAuth = MalAuthRepository.isAuthenticated.value
            if (!isAuth || !settings.enableSync) return

            _isSyncing.value = true
            _syncMessage.value = getString(Res.string.sync_progress_mal)

            try {
                MalLibraryRepository.refreshNow()
                val malItems = MalLibraryRepository.uiState.value.allItems

                val token = MalAuthRepository.currentAccessToken() ?: return@withLock
                MalAuthRepository.refreshTokenIfNeeded(force = false)
                val freshToken = MalAuthRepository.currentAccessToken() ?: return@withLock

                val lastSyncTimestampMs = settings.lastSyncTimestamp
                var processedCount = 0

                val itemsToProcess = malItems.filter { item ->
                    (item.updatedAtEpochMs ?: 0L) > lastSyncTimestampMs
                }

                log.d { "MAL sync: ${malItems.size} items, queue: ${itemsToProcess.size}" }

                for (item in itemsToProcess) {
                    processedCount++
                    if (itemsToProcess.size > 3) {
                        _syncMessage.value = "Synchronizing MyAnimeList ($processedCount/${itemsToProcess.size})..."
                    }

                    val watched = WatchedItem(
                        id = item.id.toString(),
                        type = "series",
                        name = item.title,
                        poster = item.posterUrl,
                        season = 1,
                        episode = item.episodesWatched ?: 0,
                        markedAtEpochMs = item.updatedAtEpochMs ?: 0L,
                    )
                    if (item.listStatus.equals("completed", ignoreCase = true)) {
                        WatchedRepository.markWatchedFromPlaybackCompletion(watched, syncRemote = false)
                    }
                }

                MalSettingsRepository.updateLastSyncTimestamp(WatchProgressClock.nowEpochMs())
                MalLibraryRepository.refreshNow()
                _syncMessage.value = getString(Res.string.sync_completed_successfully)
            } catch (e: Exception) {
                log.e(e) { "Error during MAL sync" }
                _syncMessage.value = getString(Res.string.sync_failed, e.message ?: getString(Res.string.sync_unknown_error))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun extractMalId(parentMetaId: String, videoId: String?): Long? {
        if (parentMetaId.startsWith("mal:", ignoreCase = true)) {
            return parentMetaId.removePrefix("mal:").substringBefore(":").toLongOrNull()
        }
        if (videoId != null && videoId.startsWith("mal:", ignoreCase = true)) {
            return videoId.removePrefix("mal:").substringBefore(":").toLongOrNull()
        }
        return parentMetaId.toLongOrNull()
    }
}
