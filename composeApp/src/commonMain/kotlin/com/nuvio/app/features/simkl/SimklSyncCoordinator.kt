package com.nuvio.app.features.simkl

import co.touchlab.kermit.Logger
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TrackingHistoryItem
import com.nuvio.app.features.tracking.TrackingListStatus
import com.nuvio.app.features.tracking.TrackingScrobbleAction
import com.nuvio.app.features.tracking.TrackingScrobbleEvent
import com.nuvio.app.features.tracking.buildTrackingMediaReference
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SimklSyncCoordinator {
    private val log = Logger.withTag("SimklSync")
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val lastSyncedProgress = mutableMapOf<String, Pair<Int, String>>()

    fun handlePlaybackProgressUpdated(entry: WatchProgressEntry) {
        syncScope.launch {
            SimklAuthRepository.ensureLoaded()
            if (!SimklAuthRepository.isAuthenticated.value) return@launch

            val isSeriesOrAnime = entry.parentMetaType.equals("series", ignoreCase = true) ||
                entry.parentMetaType.equals("anime", ignoreCase = true)
            if (!isSeriesOrAnime) return@launch

            val episodeNumber = entry.episodeNumber ?: return@launch
            val seasonNumber = entry.seasonNumber ?: 1

            val mediaRef = buildTrackingMediaReference(
                contentType = entry.contentType,
                parentMetaId = entry.parentMetaId,
                videoId = entry.videoId,
                title = entry.title,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = entry.episodeTitle,
            )

            SimklSyncRepository.ensureLoaded()
            val snapshot = SimklSyncRepository.state.value.snapshot

            val targetProgress = if (entry.isCompleted) episodeNumber else maxOf(0, episodeNumber - 1)

            val libraryEntry = snapshot.entries.firstOrNull { item ->
                val simklId = mediaRef.ids.simkl?.toString()
                if (simklId != null) {
                    item.media?.ids?.simklIdValue() == simklId
                } else {
                    item.media?.title?.equals(mediaRef.title, ignoreCase = true) == true
                }
            }

            val totalEpisodes = libraryEntry?.totalEpisodesCount ?: 0
            val isEntireShowCompleted = entry.isCompleted && totalEpisodes > 0 && targetProgress >= totalEpisodes
            val targetStatus = if (isEntireShowCompleted) TrackingListStatus.COMPLETED else TrackingListStatus.WATCHING

            val cacheKey = mediaRef.ids.simkl?.toString() ?: mediaRef.title ?: return@launch
            val lastSynced = lastSyncedProgress[cacheKey]
            if (lastSynced != null && lastSynced.first == targetProgress && lastSynced.second == targetStatus.wireValue) {
                return@launch
            }

            if (libraryEntry != null &&
                libraryEntry.watchedEpisodesCount >= targetProgress &&
                libraryEntry.status?.apiValue == targetStatus.wireValue
            ) {
                return@launch
            }

            lastSyncedProgress[cacheKey] = Pair(targetProgress, targetStatus.wireValue)

            val profileId = ProfileRepository.activeProfileId

            try {
                SimklMutationRepository.scrobble(
                    profileId = profileId,
                    action = TrackingScrobbleAction.STOP,
                    event = TrackingScrobbleEvent(
                        media = mediaRef,
                        progressPercent = 100.0,
                    ),
                )

                SimklMutationRepository.addToHistory(
                    profileId = profileId,
                    items = listOf(TrackingHistoryItem(media = mediaRef)),
                )

                SimklMutationRepository.moveToList(
                    profileId = profileId,
                    items = listOf(mediaRef),
                    destination = targetStatus,
                )

                log.i { "Simkl synced ${entry.title} ep $targetProgress -> ${targetStatus.wireValue}" }
            } catch (e: Exception) {
                log.w(e) { "Simkl sync failed for ${entry.title}" }
                lastSyncedProgress.remove(cacheKey)
            }
        }
    }
}
