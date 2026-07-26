package com.nuvio.app.features.trakt

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.runBlocking

object TraktAuthRepository {
    private const val BASE_URL = "https://api.trakt.tv"
    private const val DEVICE_CODE_URL = "https://api.trakt.tv/oauth/device/code"
    private const val DEVICE_TOKEN_URL = "https://api.trakt.tv/oauth/device/token"
    private const val API_VERSION = "2"

    private val log = Logger.withTag("TraktAuth")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(TraktAuthUiState())
    val uiState: StateFlow<TraktAuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var hasLoaded = false
    private var authState = TraktAuthState()
    private var deviceFlowJob: Job? = null

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        authState = TraktAuthState()
        publish()
    }

    fun snapshot(): TraktAuthUiState {
        ensureLoaded()
        return _uiState.value
    }

    fun currentStateForSync(): TraktAuthState {
        ensureLoaded()
        return authState.copy()
    }

    fun replaceStateFromSync(state: TraktAuthState): Boolean {
        ensureLoaded()
        val syncedState = state.copy(
            pendingAuthorizationState = null,
            pendingAuthorizationStartedAtMillis = null,
            deviceCode = null,
            userCode = null,
            verificationUrl = null,
            deviceFlowExpiresAtMillis = null,
        )
        if (authState.syncSignature() == syncedState.syncSignature()) return false
        authState = syncedState
        persist()
        publish()
        return true
    }

    fun hasRequiredCredentials(): Boolean =
        TraktConfig.CLIENT_ID.isNotBlank() && TraktConfig.CLIENT_SECRET.isNotBlank()

    fun onConnectRequested() {
        ensureLoaded()
        if (!hasRequiredCredentials()) {
            publish(errorMessage = localizedString(Res.string.trakt_missing_credentials))
            return
        }

        cancelDeviceFlow()
        publish(isLoading = true, errorMessage = null)

        deviceFlowJob = scope.launch {
            startDeviceFlow()
        }
    }

    fun onCancelAuthorization() {
        ensureLoaded()
        cancelDeviceFlow()
        clearPendingAuthorization()
        persist()
        publish(statusMessage = null, errorMessage = null)
    }

    private suspend fun startDeviceFlow() {
        val body = json.encodeToString(
            TraktDeviceCodeRequest(
                clientId = TraktConfig.CLIENT_ID,
            ),
        )

        val response = runCatching {
            httpPostJsonWithHeaders(
                url = DEVICE_CODE_URL,
                body = body,
                headers = emptyMap(),
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.w { "Failed to start Trakt device flow: ${error.message}" }
        }.getOrNull()

        if (response == null) {
            publish(isLoading = false, errorMessage = localizedString(Res.string.trakt_sign_in_complete_failed))
            return
        }

        val parsed = runCatching {
            json.decodeFromString<TraktDeviceCodeResponse>(response)
        }.getOrNull()

        if (parsed == null) {
            publish(isLoading = false, errorMessage = localizedString(Res.string.trakt_invalid_token_response))
            return
        }

        val now = TraktPlatformClock.nowEpochMs()
        authState = authState.copy(
            deviceCode = parsed.deviceCode,
            userCode = parsed.userCode,
            verificationUrl = parsed.verificationUrl,
            deviceFlowExpiresAtMillis = now + (parsed.expiresIn * 1_000L),
            deviceFlowInterval = parsed.interval.coerceIn(1, 30),
            pendingAuthorizationStartedAtMillis = now,
        )
        persist()
        publish(isLoading = false, statusMessage = null, errorMessage = null)

        pollDeviceToken()
    }

    private suspend fun pollDeviceToken() {
        val deviceCode = authState.deviceCode ?: return
        var interval = authState.deviceFlowInterval.toLong()

        while (true) {
            val expiresAt = authState.deviceFlowExpiresAtMillis ?: return
            if (TraktPlatformClock.nowEpochMs() >= expiresAt) {
                publish(errorMessage = localizedString(Res.string.trakt_device_code_expired))
                onCancelAuthorization()
                return
            }

            delay(interval * 1_000L)

            if (deviceFlowJob?.isActive != true) return

            val body = json.encodeToString(
                TraktDeviceTokenRequest(
                    deviceCode = deviceCode,
                    clientId = TraktConfig.CLIENT_ID,
                    clientSecret = TraktConfig.CLIENT_SECRET,
                ),
            )

            val result = runCatching {
                httpPostJsonWithHeaders(
                    url = DEVICE_TOKEN_URL,
                    body = body,
                    headers = emptyMap(),
                )
            }

            if (result.isFailure) {
                val error = result.exceptionOrNull()
                if (error is CancellationException) throw error
                log.w { "Trakt device token poll failed: ${error.message}" }
                continue
            }

            val raw = result.getOrThrow()

            val errorField = runCatching {
                json.parseToJsonElement(raw).jsonObject["error"]?.jsonPrimitive?.content
            }.getOrNull()

            when (errorField) {
                null -> {
                    val parsed = runCatching {
                        json.decodeFromString<TraktTokenResponse>(raw)
                    }.getOrNull()

                    if (parsed != null) {
                        authState = authState.copy(
                            accessToken = parsed.accessToken,
                            refreshToken = parsed.refreshToken,
                            tokenType = parsed.tokenType,
                            createdAt = parsed.createdAt,
                            expiresIn = parsed.expiresIn,
                            deviceCode = null,
                            userCode = null,
                            verificationUrl = null,
                            deviceFlowExpiresAtMillis = null,
                            pendingAuthorizationState = null,
                            pendingAuthorizationStartedAtMillis = null,
                        )
                        persist()
                        refreshUserSettings()
                        TraktCredentialSync.pushCurrentToRemote()
                        publish(
                            statusMessage = localizedString(Res.string.trakt_connected_status),
                            errorMessage = null,
                        )
                        deviceFlowJob = null
                        return
                    }
                }
                "authorization_pending" -> {
                    publish(statusMessage = localizedString(Res.string.trakt_device_code_polling))
                }
                "slow_down" -> {
                    interval = (interval * 1.5).toLong().coerceAtMost(30L)
                }
                "expired_token" -> {
                    publish(errorMessage = localizedString(Res.string.trakt_device_code_expired))
                    onCancelAuthorization()
                    return
                }
                else -> {
                    log.w { "Trakt device token poll error: $errorField" }
                }
            }
        }
    }

    private fun cancelDeviceFlow() {
        deviceFlowJob?.cancel()
        deviceFlowJob = null
    }

    suspend fun authorizedHeaders(): Map<String, String>? {
        ensureLoaded()
        if (!authState.isAuthenticated) return null

        val hasValidToken = refreshTokenIfNeeded(force = false)
        if (!hasValidToken) return null

        val accessToken = authState.accessToken?.trim().orEmpty()
        if (accessToken.isBlank()) return null

        return mapOf(
            "trakt-api-version" to API_VERSION,
            "trakt-api-key" to TraktConfig.CLIENT_ID,
            "Authorization" to "Bearer $accessToken",
        )
    }

    suspend fun refreshUserSettings(): String? {
        ensureLoaded()
        val headers = authorizedHeaders() ?: return null
        val response = runCatching {
            httpGetTextWithHeaders(
                url = "$BASE_URL/users/settings",
                headers = headers,
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.w { "Failed to fetch Trakt user settings: ${error.message}" }
        }.getOrNull() ?: return null

        val parsed = runCatching {
            json.decodeFromString<TraktUserSettingsResponse>(response)
        }.getOrNull() ?: return null

        authState = authState.copy(
            username = parsed.user?.username,
            userSlug = parsed.user?.ids?.slug,
        )
        persist()
        publish()
        return authState.username
    }

    fun onDisconnectRequested() {
        ensureLoaded()
        scope.launch {
            disconnect()
        }
    }

    private suspend fun disconnect() {
        publish(isLoading = true, errorMessage = null)

        val token = authState.accessToken?.takeIf { it.isNotBlank() }
        if (!token.isNullOrBlank() && hasRequiredCredentials()) {
            val body = json.encodeToString(
                TraktRevokeRequest(
                    token = token,
                    clientId = TraktConfig.CLIENT_ID,
                    clientSecret = TraktConfig.CLIENT_SECRET,
                ),
            )
            runCatching {
                httpPostJsonWithHeaders(
                    url = "$BASE_URL/oauth/revoke",
                    body = body,
                    headers = emptyMap(),
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Failed to revoke Trakt token: ${error.message}" }
            }
        }

        cancelDeviceFlow()
        TraktCredentialSync.deleteRemote()
        authState = TraktAuthState()
        persist()
        publish(
            isLoading = false,
            statusMessage = localizedString(Res.string.trakt_disconnected_status),
            errorMessage = null,
        )
    }

    private suspend fun refreshTokenIfNeeded(force: Boolean): Boolean {
        if (!hasRequiredCredentials()) return false
        val refreshToken = authState.refreshToken?.takeIf { it.isNotBlank() } ?: return false

        if (!force && !isTokenExpiredOrExpiring(authState)) {
            return true
        }

        val body = json.encodeToString(
            TraktRefreshTokenRequest(
                refreshToken = refreshToken,
                clientId = TraktConfig.CLIENT_ID,
                clientSecret = TraktConfig.CLIENT_SECRET,
                redirectUri = TraktConfig.REDIRECT_URI,
            ),
        )

        val response = runCatching {
            httpPostJsonWithHeaders(
                url = "$BASE_URL/oauth/token",
                body = body,
                headers = emptyMap(),
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.w { "Trakt token refresh failed: ${error.message}" }
        }.getOrNull()

        if (response == null) {
            if (recoverFromRemoteCredentials(refreshToken)) return true
            return false
        }

        val parsed = runCatching {
            json.decodeFromString<TraktTokenResponse>(response)
        }.getOrNull()

        if (parsed == null) {
            if (recoverFromRemoteCredentials(refreshToken)) return true
            return false
        }

        authState = authState.copy(
            accessToken = parsed.accessToken,
            refreshToken = parsed.refreshToken,
            tokenType = parsed.tokenType,
            createdAt = parsed.createdAt,
            expiresIn = parsed.expiresIn,
        )
        persist()
        TraktCredentialSync.pushCurrentToRemote()
        publish()
        return true
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = TraktAuthStorage.loadPayload().orEmpty().trim()
        authState = if (payload.isBlank()) {
            TraktAuthState()
        } else {
            runCatching { json.decodeFromString<TraktAuthState>(payload) }
                .getOrElse {
                    log.w { "Failed to parse Trakt auth payload: ${it.message}" }
                    TraktAuthState()
                }
        }
        publish(statusMessage = null, errorMessage = null)
    }

    private fun clearPendingAuthorization() {
        authState = authState.copy(
            pendingAuthorizationState = null,
            pendingAuthorizationStartedAtMillis = null,
            deviceCode = null,
            userCode = null,
            verificationUrl = null,
            deviceFlowExpiresAtMillis = null,
        )
    }

    private fun publish(
        isLoading: Boolean = _uiState.value.isLoading,
        statusMessage: String? = _uiState.value.statusMessage,
        errorMessage: String? = _uiState.value.errorMessage,
    ) {
        val tokenExpiresAtMillis = authState.createdAt
            ?.let { createdAtSeconds ->
                authState.expiresIn?.let { expiresInSeconds ->
                    (createdAtSeconds + expiresInSeconds) * 1_000L
                }
            }

        val mode = when {
            authState.isAuthenticated -> TraktConnectionMode.CONNECTED
            !authState.deviceCode.isNullOrBlank() -> TraktConnectionMode.AWAITING_APPROVAL
            else -> TraktConnectionMode.DISCONNECTED
        }

        _isAuthenticated.value = authState.isAuthenticated
        _uiState.value = TraktAuthUiState(
            mode = mode,
            credentialsConfigured = hasRequiredCredentials(),
            isLoading = isLoading,
            username = authState.username,
            tokenExpiresAtMillis = tokenExpiresAtMillis,
            pendingAuthorizationStartedAtMillis = authState.pendingAuthorizationStartedAtMillis,
            deviceCode = authState.deviceCode,
            userCode = authState.userCode,
            verificationUrl = authState.verificationUrl,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    private fun persist() {
        TraktAuthStorage.savePayload(json.encodeToString(authState))
    }

    private fun isTokenExpiredOrExpiring(state: TraktAuthState): Boolean {
        val createdAt = state.createdAt ?: return true
        val expiresIn = state.expiresIn ?: return true
        val expiresAtSeconds = createdAt + expiresIn
        val nowSeconds = TraktPlatformClock.nowEpochMs() / 1_000L
        return nowSeconds >= (expiresAtSeconds - 60)
    }

    private suspend fun recoverFromRemoteCredentials(staleRefreshToken: String): Boolean {
        val pulled = TraktCredentialSync.pullFromRemote()
        if (!pulled) return false
        return authState.isAuthenticated && authState.refreshToken != staleRefreshToken
    }

    private fun TraktAuthState.syncSignature(): String =
        listOf(
            accessToken.orEmpty(),
            refreshToken.orEmpty(),
            tokenType.orEmpty(),
            createdAt?.toString().orEmpty(),
            expiresIn?.toString().orEmpty(),
            username.orEmpty(),
            userSlug.orEmpty(),
        ).joinToString("|")
}

@Serializable
private data class TraktDeviceCodeRequest(
    @SerialName("client_id") val clientId: String,
)

@Serializable
private data class TraktDeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_url") val verificationUrl: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("interval") val interval: Int,
)

@Serializable
private data class TraktDeviceTokenRequest(
    @SerialName("code") val deviceCode: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
)

@Serializable
private data class TraktRefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("redirect_uri") val redirectUri: String,
    @SerialName("grant_type") val grantType: String = "refresh_token",
)

@Serializable
private data class TraktRevokeRequest(
    @SerialName("token") val token: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
)

@Serializable
private data class TraktTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
private data class TraktUserSettingsResponse(
    val user: TraktUserDto? = null,
)

@Serializable
private data class TraktUserDto(
    val username: String? = null,
    val ids: TraktUserIdsDto? = null,
)

@Serializable
private data class TraktUserIdsDto(
    val slug: String? = null,
)

private fun localizedString(resource: StringResource): String = runBlocking { getString(resource) }
