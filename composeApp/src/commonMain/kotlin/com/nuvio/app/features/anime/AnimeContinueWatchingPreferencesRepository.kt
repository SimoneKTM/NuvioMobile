package com.nuvio.app.features.anime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesUiState
import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle
import com.nuvio.app.features.watchprogress.ContinueWatchingSortMode

@Serializable
private data class StoredAnimeContinueWatchingPreferences(
    val isVisible: Boolean = true,
    val style: ContinueWatchingSectionStyle = ContinueWatchingSectionStyle.Card,
    val upNextFromFurthestEpisode: Boolean = true,
    @SerialName("use_episode_thumbnails_in_cw")
    val useEpisodeThumbnails: Boolean = true,
    @SerialName("show_unaired_next_up")
    val showUnairedNextUp: Boolean = true,
    @SerialName("blur_continue_watching_next_up")
    val blurNextUp: Boolean = false,
    val dismissedNextUpKeys: Set<String> = emptySet(),
    val showResumePromptOnLaunch: Boolean = true,
    @SerialName("sort_mode")
    val sortMode: ContinueWatchingSortMode = ContinueWatchingSortMode.DEFAULT,
)

object AnimeContinueWatchingPreferencesRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(ContinueWatchingPreferencesUiState())
    val uiState: StateFlow<ContinueWatchingPreferencesUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = ContinueWatchingPreferencesUiState()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = AnimeContinueWatchingPreferencesStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) {
            _uiState.value = ContinueWatchingPreferencesUiState()
            return
        }
        val stored = runCatching {
            json.decodeFromString<StoredAnimeContinueWatchingPreferences>(payload)
        }.getOrNull()
        _uiState.value = if (stored != null) {
            ContinueWatchingPreferencesUiState(
                isVisible = stored.isVisible,
                style = stored.style,
                upNextFromFurthestEpisode = stored.upNextFromFurthestEpisode,
                useEpisodeThumbnails = stored.useEpisodeThumbnails,
                showUnairedNextUp = stored.showUnairedNextUp,
                blurNextUp = stored.blurNextUp,
                dismissedNextUpKeys = stored.dismissedNextUpKeys,
                showResumePromptOnLaunch = stored.showResumePromptOnLaunch,
                sortMode = stored.sortMode,
            )
        } else {
            ContinueWatchingPreferencesUiState()
        }
    }

    fun setVisible(isVisible: Boolean) {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(isVisible = isVisible)
        persist()
    }

    fun setStyle(style: ContinueWatchingSectionStyle) {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(style = style)
        persist()
    }

    fun setUpNextFromFurthestEpisode(enabled: Boolean) {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(upNextFromFurthestEpisode = enabled)
        persist()
    }

    fun setUseEpisodeThumbnails(enabled: Boolean) {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(useEpisodeThumbnails = enabled)
        persist()
    }

    fun setShowUnairedNextUp(enabled: Boolean) {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(showUnairedNextUp = enabled)
        persist()
    }

    fun setBlurNextUp(enabled: Boolean) {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(blurNextUp = enabled)
        persist()
    }

    fun addDismissedNextUpKey(key: String) {
        ensureLoaded()
        val normalizedKey = key.trim()
        if (normalizedKey.isBlank()) return
        val current = _uiState.value.dismissedNextUpKeys
        if (normalizedKey in current) return
        _uiState.value = _uiState.value.copy(dismissedNextUpKeys = current + normalizedKey)
        persist()
    }

    fun setShowResumePromptOnLaunch(enabled: Boolean) {
        ensureLoaded()
        _uiState.value = _uiState.value.copy(showResumePromptOnLaunch = enabled)
        persist()
    }

    fun setSortMode(mode: ContinueWatchingSortMode) {
        ensureLoaded()
        if (_uiState.value.sortMode == mode) return
        _uiState.value = _uiState.value.copy(sortMode = mode)
        persist()
    }

    fun removeDismissedNextUpKeysForContent(contentId: String) {
        ensureLoaded()
        val normalizedContentId = contentId.trim()
        if (normalizedContentId.isBlank()) return
        val prefix = "$normalizedContentId|"
        val filtered = _uiState.value.dismissedNextUpKeys.filterNot { it.startsWith(prefix) }.toSet()
        if (filtered == _uiState.value.dismissedNextUpKeys) return
        _uiState.value = _uiState.value.copy(dismissedNextUpKeys = filtered)
        persist()
    }

    private fun persist() {
        AnimeContinueWatchingPreferencesStorage.savePayload(
            json.encodeToString(
                StoredAnimeContinueWatchingPreferences(
                    isVisible = _uiState.value.isVisible,
                    style = _uiState.value.style,
                    upNextFromFurthestEpisode = _uiState.value.upNextFromFurthestEpisode,
                    useEpisodeThumbnails = _uiState.value.useEpisodeThumbnails,
                    showUnairedNextUp = _uiState.value.showUnairedNextUp,
                    blurNextUp = _uiState.value.blurNextUp,
                    dismissedNextUpKeys = _uiState.value.dismissedNextUpKeys,
                    showResumePromptOnLaunch = _uiState.value.showResumePromptOnLaunch,
                    sortMode = _uiState.value.sortMode,
                ),
            ),
        )
    }
}
