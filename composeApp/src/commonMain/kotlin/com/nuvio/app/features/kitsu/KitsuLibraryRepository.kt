package com.nuvio.app.features.kitsu

import co.touchlab.kermit.Logger
import com.nuvio.app.features.watchprogress.WatchProgressClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object KitsuLibraryRepository {
    private val log = Logger.withTag("KitsuLibrary")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()

    private val _uiState = MutableStateFlow(KitsuLibraryUiState())
    val uiState: StateFlow<KitsuLibraryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var lastRefreshAtMs = 0L
    private const val CACHE_TTL_MS = 60_000L * 5

    fun ensureLoaded() {
        if (hasLoaded) return
        loadSnapshotFromDisk()
    }

    fun onProfileChanged() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = KitsuLibraryUiState()
        ensureLoaded()
    }

    fun clearLocalState() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = KitsuLibraryUiState()
        runCatching { KitsuStorage.saveLibraryPayload("") }
    }

    fun isInLibrary(kitsuMediaId: Long): Boolean {
        val state = _uiState.value
        return (state.current + state.completed + state.planned + state.onHold + state.dropped)
            .any { it.kitsuMediaId == kitsuMediaId }
    }

    fun findKitsuItemById(kitsuMediaId: Long): KitsuLibraryItem? {
        val state = _uiState.value
        return (state.current + state.completed + state.planned + state.onHold + state.dropped)
            .find { it.kitsuMediaId == kitsuMediaId }
    }

    suspend fun refreshNow() {
        refresh(force = true)
    }

    suspend fun ensureFresh() {
        refresh(force = false)
    }

    private suspend fun refresh(force: Boolean) {
        ensureLoaded()
        refreshMutex.withLock {
            val now = WatchProgressClock.nowEpochMs()
            if (!force && _uiState.value.isLoaded && now - lastRefreshAtMs <= CACHE_TTL_MS) {
                return
            }

            val token = KitsuAuthRepository.getAccessTokenRefreshed()
            val userId = KitsuAuthRepository.getUserId()

            if (token.isNullOrBlank() || userId == null) {
                _uiState.value = KitsuLibraryUiState()
                lastRefreshAtMs = 0L
                return
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val allItems = fetchAllPages(token, userId)
                val current = allItems.filter { it.status.equals("current", ignoreCase = true) }
                val completed = allItems.filter { it.status.equals("completed", ignoreCase = true) }
                val planned = allItems.filter { it.status.equals("planned", ignoreCase = true) }
                val onHold = allItems.filter { it.status.equals("on_hold", ignoreCase = true) }
                val dropped = allItems.filter { it.status.equals("dropped", ignoreCase = true) }

                val newState = KitsuLibraryUiState(
                    current = current,
                    completed = completed,
                    planned = planned,
                    onHold = onHold,
                    dropped = dropped,
                    isLoading = false,
                    isLoaded = true,
                    errorMessage = null
                )

                _uiState.value = newState
                lastRefreshAtMs = now
                persistSnapshot(allItems)
            } catch (e: Exception) {
                log.e { "Failed to refresh Kitsu library: ${e.message}" }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Impossibile aggiornare le librerie Kitsu."
                )
            }
        }
    }

    private suspend fun fetchAllPages(token: String, userId: Long): List<KitsuLibraryItem> {
        val allItems = mutableListOf<KitsuLibraryItem>()
        var offset = 0
        val limit = 500

        while (true) {
            val response = KitsuApi.fetchLibraryEntries(
                token = token,
                userId = userId,
                pageLimit = limit,
                pageOffset = offset
            )

            val includedMap = response.included?.associateBy { it.id } ?: emptyMap()

            val mapped = response.data.mapNotNull { entry ->
                val attrs = entry.attributes ?: return@mapNotNull null
                val animeId = entry.relationships?.anime?.data?.id
                    ?: entry.relationships?.media?.data?.id
                    ?: return@mapNotNull null

                val included = includedMap[animeId]
                val incAttrs = included?.attributes

                val title = incAttrs?.titles?.en
                    ?: incAttrs?.titles?.enJp
                    ?: incAttrs?.titles?.canonical
                    ?: incAttrs?.slug
                    ?: "Unknown Title"

                val poster = incAttrs?.posterImage?.large
                    ?: incAttrs?.posterImage?.medium
                    ?: incAttrs?.posterImage?.original

                val nextEpisodeAtEpochMs = if (attrs.status == "current" && incAttrs?.startDate != null) {
                    val startEpochMs = parseKitsuDateToEpochMs(incAttrs.startDate)
                    if (startEpochMs != null) {
                        val nowMs = WatchProgressClock.nowEpochMs()
                        val daysSinceStart = (nowMs - startEpochMs) / 86400000L
                        if (daysSinceStart >= 0) {
                            val weeksSinceStart = daysSinceStart / 7
                            val nextEpNumber = weeksSinceStart + 1
                            val totalEp = incAttrs.episodeCount
                            if (totalEp == null || nextEpNumber <= totalEp) {
                                startEpochMs + (nextEpNumber * 7 * 86400000L)
                            } else null
                        } else null
                    } else null
                } else null

                KitsuLibraryItem(
                    id = entry.id.toLongOrNull() ?: 0L,
                    kitsuMediaId = animeId.toLongOrNull() ?: 0L,
                    title = title,
                    posterUrl = poster,
                    progress = attrs.progress ?: 0,
                    totalEpisodes = incAttrs?.episodeCount,
                    rating = attrs.rating,
                    status = attrs.status ?: "current",
                    updatedAt = attrs.updatedAt,
                    entryId = entry.id,
                    synopsis = incAttrs?.synopsis,
                    startDate = incAttrs?.startDate,
                    nextEpisodeAtEpochMs = nextEpisodeAtEpochMs,
                )
            }

            allItems.addAll(mapped)

            if (response.links?.next == null || mapped.size < limit) break
            offset += limit
        }

        return allItems
    }

    private fun loadSnapshotFromDisk() {
        hasLoaded = true
        val payload = KitsuStorage.loadLibraryPayload().orEmpty().trim()
        if (payload.isBlank()) {
            _uiState.value = KitsuLibraryUiState()
            return
        }

        runCatching {
            val items = json.decodeFromString<List<KitsuLibraryItem>>(payload)
            val current = items.filter { it.status.equals("current", ignoreCase = true) }
            val completed = items.filter { it.status.equals("completed", ignoreCase = true) }
            val planned = items.filter { it.status.equals("planned", ignoreCase = true) }
            val onHold = items.filter { it.status.equals("on_hold", ignoreCase = true) }
            val dropped = items.filter { it.status.equals("dropped", ignoreCase = true) }

            _uiState.value = KitsuLibraryUiState(
                current = current,
                completed = completed,
                planned = planned,
                onHold = onHold,
                dropped = dropped,
                isLoading = false,
                isLoaded = true,
                errorMessage = null
            )
        }.onFailure {
            log.w { "Failed to parse cached Kitsu library items: ${it.message}" }
            _uiState.value = KitsuLibraryUiState()
        }
    }

    private fun parseKitsuDateToEpochMs(dateStr: String): Long? {
        val parts = dateStr.split('-')
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        val daysFromEpoch = daysFrom0(year, month, day) - daysFrom0(1970, 1, 1)
        return daysFromEpoch * 86400000L
    }

    private fun daysFrom0(year: Int, month: Int, day: Int): Long {
        var y = year
        var m = month
        if (m <= 2) { y--; m += 12 }
        val era = (y / 400).toLong()
        val yoe = (y - 400 * era).toLong()
        val doy = (153 * (m - 3) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe
    }

    private fun persistSnapshot(items: List<KitsuLibraryItem>) {
        runCatching {
            val payload = json.encodeToString(items)
            KitsuStorage.saveLibraryPayload(payload)
        }.onFailure {
            log.w { "Failed to save cached Kitsu library: ${it.message}" }
        }
    }
}
