package com.nuvio.app.features.anime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.nuvio.app.core.ui.DefaultPosterCardWidthDp
import com.nuvio.app.core.ui.DefaultPosterCardHeightDp
import com.nuvio.app.core.ui.DefaultPosterCardCornerRadiusDp
import com.nuvio.app.core.ui.PosterCardStyleUiState

@Serializable
private data class StoredAnimePosterCardStylePreferences(
    val widthDp: Int = DefaultPosterCardWidthDp,
    val heightDp: Int = DefaultPosterCardHeightDp,
    val cornerRadiusDp: Int = DefaultPosterCardCornerRadiusDp,
    val catalogLandscapeModeEnabled: Boolean = false,
    val hideLabelsEnabled: Boolean = false,
)

object AnimePosterCardStyleRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(PosterCardStyleUiState())
    val uiState: StateFlow<PosterCardStyleUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = PosterCardStyleUiState()
    }

    fun setWidthDp(widthDp: Int) {
        ensureLoaded()
        val nextWidth = widthDp
        val nextHeight = (nextWidth * 3) / 2
        if (_uiState.value.widthDp == nextWidth && _uiState.value.heightDp == nextHeight) return
        _uiState.value = _uiState.value.copy(
            widthDp = nextWidth,
            heightDp = nextHeight,
        )
        persist()
    }

    fun setCornerRadiusDp(cornerRadiusDp: Int) {
        ensureLoaded()
        if (_uiState.value.cornerRadiusDp == cornerRadiusDp) return
        _uiState.value = _uiState.value.copy(cornerRadiusDp = cornerRadiusDp)
        persist()
    }

    fun setCatalogLandscapeModeEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.catalogLandscapeModeEnabled == enabled) return
        _uiState.value = _uiState.value.copy(catalogLandscapeModeEnabled = enabled)
        persist()
    }

    fun setHideLabelsEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.hideLabelsEnabled == enabled) return
        _uiState.value = _uiState.value.copy(hideLabelsEnabled = enabled)
        persist()
    }

    fun resetToDefaults() {
        ensureLoaded()
        if (_uiState.value == PosterCardStyleUiState()) return
        _uiState.value = PosterCardStyleUiState()
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = AnimePosterCardStyleStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) {
            _uiState.value = PosterCardStyleUiState()
            return
        }
        val stored = runCatching {
            json.decodeFromString<StoredAnimePosterCardStylePreferences>(payload)
        }.getOrNull()
        _uiState.value = if (stored != null) {
            val widthDp = stored.widthDp.takeIf { it > 0 } ?: DefaultPosterCardWidthDp
            val heightDp = stored.heightDp.takeIf { it > 0 } ?: ((widthDp * 3) / 2)
            val cornerRadiusDp = stored.cornerRadiusDp.coerceAtLeast(0)
            PosterCardStyleUiState(
                widthDp = widthDp,
                heightDp = heightDp,
                cornerRadiusDp = cornerRadiusDp,
                catalogLandscapeModeEnabled = stored.catalogLandscapeModeEnabled,
                hideLabelsEnabled = stored.hideLabelsEnabled,
            )
        } else {
            PosterCardStyleUiState()
        }
    }

    private fun persist() {
        AnimePosterCardStyleStorage.savePayload(
            json.encodeToString(
                StoredAnimePosterCardStylePreferences(
                    widthDp = _uiState.value.widthDp,
                    heightDp = _uiState.value.heightDp,
                    cornerRadiusDp = _uiState.value.cornerRadiusDp,
                    catalogLandscapeModeEnabled = _uiState.value.catalogLandscapeModeEnabled,
                    hideLabelsEnabled = _uiState.value.hideLabelsEnabled,
                ),
            ),
        )
    }
}
