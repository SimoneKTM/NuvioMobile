package com.nuvio.app.features.simkl

import com.nuvio.app.features.tracking.TrackingMediaReference

internal fun SimklSyncSnapshot.reconcileWatchedPlayback(): SimklSyncSnapshot = this

internal fun SimklMedia.canonicalContentId(): String? = null

internal fun SimklSyncSnapshot.enrichMediaReference(ref: TrackingMediaReference): TrackingMediaReference = ref

internal fun TrackingMediaReference.resolveAnimeEpisodeForSimkl(): TrackingMediaReference = this

internal fun isWatchedByAnimeVideoId(
    snapshot: SimklSyncSnapshot,
    videoId: String,
    episode: Int,
): Boolean = false

internal fun simklSyncLog(tag: String, message: String) = Unit
