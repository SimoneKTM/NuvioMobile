package com.nuvio.app.features.kitsu

import co.touchlab.kermit.Logger
import com.nuvio.app.features.watchprogress.WatchProgressClock
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watched.WatchedItem
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

object KitsuSyncCoordinator {
    private val log = Logger.withTag("KitsuSync")
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var syncJob: Job? = null

    private val lastSyncedProgress = mutableMapOf<Long, Pair<Int, String>>()

    fun syncNow() {
        if (_isSyncing.value) return
        syncJob = syncScope.launch {
            runSync(force = true)
        }
    }

    fun syncOnLaunchIfNeeded() {
        syncScope.launch {
            KitsuSettingsRepository.ensureLoaded()
            KitsuAuthRepository.ensureLoaded()
            val settings = KitsuSettingsRepository.uiState.value
            val isAuth = KitsuAuthRepository.isAuthenticated.value
            if (isAuth && settings.enableSync && settings.syncOnLaunch) {
                log.i { "Kitsu sync on launch triggered" }
                runSync(force = false)
            }
        }
    }

    fun handlePlaybackProgressUpdated(entry: WatchProgressEntry) {
        syncScope.launch {
            KitsuSettingsRepository.ensureLoaded()
            KitsuAuthRepository.ensureLoaded()
            val settings = KitsuSettingsRepository.uiState.value
            val isAuth = KitsuAuthRepository.isAuthenticated.value
            if (!isAuth || !settings.enableSync || !settings.autoSync || !settings.syncWatching) {
                return@launch
            }

            if (entry.parentMetaType.equals("series", ignoreCase = true) || entry.parentMetaType.equals("anime", ignoreCase = true)) {
                val progress = entry.episodeNumber ?: return@launch
                val kitsuMediaId = extractKitsuMediaId(entry.parentMetaId, entry.videoId) ?: return@launch

                if (!settings.autoAddNewAnime && !KitsuLibraryRepository.isInLibrary(kitsuMediaId)) {
                    return@launch
                }

                val libraryItem = KitsuLibraryRepository.findKitsuItemById(kitsuMediaId)
                val targetProgress = if (entry.isCompleted) progress else maxOf(libraryItem?.progress ?: 0, progress - 1)

                val totalEpisodes = libraryItem?.totalEpisodes
                val isCompleted = entry.isCompleted && totalEpisodes != null && totalEpisodes > 0 && targetProgress >= totalEpisodes
                val targetStatus = if (isCompleted) "completed" else "current"

                val lastSynced = lastSyncedProgress[kitsuMediaId]
                if (lastSynced != null && lastSynced.first == targetProgress && lastSynced.second == targetStatus) {
                    return@launch
                }

                if (libraryItem != null && libraryItem.progress == targetProgress && libraryItem.status.equals(targetStatus, ignoreCase = true)) {
                    return@launch
                }

                lastSyncedProgress[kitsuMediaId] = Pair(targetProgress, targetStatus)

                val token = KitsuAuthRepository.getAccessTokenRefreshed() ?: return@launch

                val success = if (libraryItem?.entryId != null) {
                    KitsuApi.updateLibraryEntry(
                        token = token,
                        entryId = libraryItem.entryId,
                        status = targetStatus,
                        progress = targetProgress
                    )
                } else {
                    val userId = KitsuAuthRepository.getUserId() ?: return@launch
                    KitsuApi.saveLibraryEntry(
                        token = token,
                        kitsuMediaId = kitsuMediaId,
                        userId = userId,
                        status = targetStatus,
                        progress = targetProgress
                    )
                }

                if (success) {
                    KitsuLibraryRepository.refreshNow()
                } else {
                    lastSyncedProgress.remove(kitsuMediaId)
                }
            }
        }
    }

    private suspend fun runSync(force: Boolean) {
        syncMutex.withLock {
            KitsuSettingsRepository.ensureLoaded()
            KitsuAuthRepository.ensureLoaded()
            val settings = KitsuSettingsRepository.uiState.value
            val isAuth = KitsuAuthRepository.isAuthenticated.value

            if (!isAuth || !settings.enableSync) return

            _isSyncing.value = true
            _syncMessage.value = "Sincronizzazione Kitsu in corso..."

            try {
                KitsuLibraryRepository.refreshNow()
                val localEntries = WatchProgressRepository.uiState.value.entries
                val kitsuItems = KitsuLibraryRepository.uiState.value.current +
                    KitsuLibraryRepository.uiState.value.completed +
                    KitsuLibraryRepository.uiState.value.planned +
                    KitsuLibraryRepository.uiState.value.onHold +
                    KitsuLibraryRepository.uiState.value.dropped

                val token = KitsuAuthRepository.getAccessTokenRefreshed().orEmpty()

                val lastSyncTimestampMs = settings.lastSyncTimestamp
                val activeIds = KitsuLibraryRepository.uiState.value.current.map { it.kitsuMediaId }.toSet()

                val itemsToProcess = kitsuItems.filter { item ->
                    item.updatedAt?.let { parseKitsuDate(it) > lastSyncTimestampMs } ?: false ||
                    item.kitsuMediaId in activeIds
                }

                log.d { "Kitsu sync: ${localEntries.size} local, ${kitsuItems.size} kitsu items, queue: ${itemsToProcess.size}" }

                var processedCount = 0
                for (item in itemsToProcess) {
                    processedCount++
                    if (itemsToProcess.size > 3) {
                        _syncMessage.value = "Sincronizzazione Kitsu ($processedCount/${itemsToProcess.size})..."
                    }

                    val kitsuUpdatedMs = parseKitsuDate(item.updatedAt)

                    if (kitsuUpdatedMs > lastSyncTimestampMs) {
                        val watched = WatchedItem(
                            id = item.kitsuMediaId.toString(),
                            type = "series",
                            name = item.title,
                            poster = item.posterUrl,
                            season = 1,
                            episode = item.progress,
                            markedAtEpochMs = kitsuUpdatedMs
                        )
                        if (item.status.equals("completed", ignoreCase = true)) {
                            WatchedRepository.markWatchedFromPlaybackCompletion(watched, syncRemote = false)
                        }
                    }
                }

                KitsuSettingsRepository.updateLastSyncTimestamp(WatchProgressClock.nowEpochMs())
                KitsuLibraryRepository.refreshNow()
                _syncMessage.value = "Sincronizzazione completata con successo."
            } catch (e: Exception) {
                log.e(e) { "Error during Kitsu sync" }
                _syncMessage.value = "Sync fallito: ${e.message ?: "Errore sconosciuto"}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun extractKitsuMediaId(parentMetaId: String, videoId: String?): Long? {
        if (parentMetaId.startsWith("kitsu:", ignoreCase = true)) {
            return parentMetaId.removePrefix("kitsu:").substringBefore(":").toLongOrNull()
        }
        if (videoId != null && videoId.startsWith("kitsu:", ignoreCase = true)) {
            return videoId.removePrefix("kitsu:").substringBefore(":").toLongOrNull()
        }
        return parentMetaId.toLongOrNull()
    }

    private fun parseKitsuDate(dateString: String?): Long {
        if (dateString.isNullOrBlank()) return 0L
        return runCatching {
            java.time.Instant.parse(dateString).toEpochMilli()
        }.getOrNull() ?: 0L
    }
}
