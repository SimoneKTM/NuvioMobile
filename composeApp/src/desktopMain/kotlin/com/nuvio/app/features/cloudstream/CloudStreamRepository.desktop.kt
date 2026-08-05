package com.nuvio.app.features.cloudstream

import com.nuvio.app.core.i18n.localizedCloudStreamDesktopUnsupported
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object CloudStreamRepository {
    private val _uiState = MutableStateFlow(CloudStreamUiState())
    actual val uiState: StateFlow<CloudStreamUiState> = _uiState.asStateFlow()

    actual fun initialize() {}

    actual fun onProfileChanged(profileId: Int) {}

    actual fun clearLocalState() {}

    actual fun acceptSecurityWarning() {}

    actual suspend fun addRepository(rawUrl: String): AddCloudStreamRepositoryResult =
        AddCloudStreamRepositoryResult.Error(localizedCloudStreamDesktopUnsupported())

    actual fun refreshRepository(manifestUrl: String) {}

    actual fun refreshAll() {}

    actual fun removeRepository(manifestUrl: String) {}

    actual suspend fun installPlugin(pluginId: String): CloudStreamInstallResult =
        CloudStreamInstallResult.Error(localizedCloudStreamDesktopUnsupported())

    actual suspend fun updatePlugin(pluginId: String): CloudStreamInstallResult =
        CloudStreamInstallResult.Error(localizedCloudStreamDesktopUnsupported())

    actual suspend fun installAndEnablePlugins(pluginIds: List<String>): CloudStreamBulkInstallResult =
        CloudStreamBulkInstallResult(pluginIds.size, 0, 0, pluginIds.size)

    actual fun setPluginEnabled(pluginId: String, enabled: Boolean) {}

    actual fun removePlugin(pluginId: String) {}

    actual suspend fun getMainPage(providerId: String, page: Int): Result<List<Pair<String, List<CloudStreamSearchItem>>>> =
        Result.failure(UnsupportedOperationException(localizedCloudStreamDesktopUnsupported()))

    actual suspend fun search(query: String, providerId: String?): List<Result<List<CloudStreamSearchItem>>> = emptyList()

    actual suspend fun loadByExternalId(providerId: String, externalId: String): Result<CloudStreamLoadItem?> =
        Result.failure(UnsupportedOperationException(localizedCloudStreamDesktopUnsupported()))

    actual suspend fun load(providerId: String, data: String): Result<CloudStreamLoadItem> =
        Result.failure(UnsupportedOperationException(localizedCloudStreamDesktopUnsupported()))

    actual suspend fun loadLinks(providerId: String, data: String): Result<List<CloudStreamPlaybackSource>> =
        Result.failure(UnsupportedOperationException(localizedCloudStreamDesktopUnsupported()))
}
