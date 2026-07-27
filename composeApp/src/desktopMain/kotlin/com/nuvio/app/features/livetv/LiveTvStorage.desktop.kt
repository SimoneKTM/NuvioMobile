package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage

internal actual object LiveTvStorage {
    private val store = DesktopStorage.store("nuvio_live_tv")

    actual fun loadPlaylistUrl(): String? = store.getString("playlist_url")

    actual fun savePlaylistUrl(url: String) = store.putString("playlist_url", url)

    actual fun loadPlaylistsBlob(): String? = store.getString("playlists_blob")

    actual fun savePlaylistsBlob(blob: String) = store.putString("playlists_blob", blob)

    actual fun loadFavoriteChannelIdsBlob(): String? = store.getString("favorite_channel_ids_blob")

    actual fun saveFavoriteChannelIdsBlob(blob: String) = store.putString("favorite_channel_ids_blob", blob)

    actual fun loadLastWatchedChannelId(): String? = store.getString("last_watched_channel_id")

    actual fun saveLastWatchedChannelId(channelId: String) = store.putString("last_watched_channel_id", channelId)

    actual fun loadNavigationEnabled(): Boolean? {
        val key = "navigation_enabled"
        return if (store.contains(key)) store.getBoolean(key) else null
    }

    actual fun saveNavigationEnabled(enabled: Boolean) = store.putBoolean("navigation_enabled", enabled)

    actual fun publishNavigationVisibility(visible: Boolean) = Unit
}
