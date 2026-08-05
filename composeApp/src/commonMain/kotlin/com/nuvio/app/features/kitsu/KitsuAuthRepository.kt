package com.nuvio.app.features.kitsu

import co.touchlab.kermit.Logger
import com.nuvio.app.features.watchprogress.WatchProgressClock
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.auth_denied_by_user
import nuvio.composeapp.generated.resources.kitsu_auth_exchange_failed
import nuvio.composeapp.generated.resources.kitsu_auth_invalid_callback_url
import nuvio.composeapp.generated.resources.kitsu_auth_no_code
import org.jetbrains.compose.resources.getString

object KitsuAuthRepository {
    private val log = Logger.withTag("KitsuAuth")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(KitsuAuthUiState())
    val uiState: StateFlow<KitsuAuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var hasLoaded = false
    private var authState = KitsuAuthState()

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        authState = KitsuAuthState()
        _isAuthenticated.value = false
        _uiState.value = KitsuAuthUiState()
        persist()
    }

    fun loginWithPassword(email: String, password: String) {
        ensureLoaded()
        scope.launch {
            publish(isLoading = true, errorMessage = null)
            val result = KitsuApi.authenticateWithPassword(email, password)
            if (result != null) {
                completeAuthWithToken(
                    token = result.accessToken,
                    refreshToken = result.refreshToken,
                    expiresInSeconds = result.expiresIn
                )
            } else {
                publish(isLoading = false, errorMessage = "Login fallito. Controlla email e password.")
            }
        }
    }

    fun onConnectRequested(): String {
        ensureLoaded()
        return "https://kitsu.app/api/oauth/authorize?client_id=${KitsuConfig.CLIENT_ID}&redirect_uri=${KitsuConfig.REDIRECT_URI}&response_type=code"
    }

    fun onAuthCallbackReceived(callbackUrl: String) {
        ensureLoaded()
        if (!callbackUrl.startsWith(KitsuConfig.REDIRECT_URI, ignoreCase = true)) {
            return
        }

        scope.launch {
            publish(isLoading = true, errorMessage = null)

            val normalizedUrlString = if (callbackUrl.contains("#")) {
                callbackUrl.replace("#", "?")
            } else {
                callbackUrl
            }

            val parsedUrl = runCatching { Url(normalizedUrlString) }
                .onFailure { log.w { "Invalid Kitsu callback URL: ${it.message}" } }
                .getOrNull()

            if (parsedUrl == null) {
                publish(isLoading = false, errorMessage = getString(Res.string.kitsu_auth_invalid_callback_url))
                return@launch
            }

            val error = parsedUrl.parameters["error"]
            if (!error.isNullOrBlank()) {
                val errorDesc = parsedUrl.parameters["error_description"] ?: getString(Res.string.auth_denied_by_user)
                publish(isLoading = false, errorMessage = errorDesc)
                return@launch
            }

            val implicitToken = parsedUrl.parameters["access_token"]
            if (!implicitToken.isNullOrBlank()) {
                val expiresInSeconds = parsedUrl.parameters["expires_in"]?.toLongOrNull() ?: 31536000L
                completeAuthWithToken(implicitToken, null, expiresInSeconds)
                return@launch
            }

            val code = parsedUrl.parameters["code"]
            if (!code.isNullOrBlank()) {
                val tokenResult = KitsuApi.exchangeCodeForToken(code)
                if (tokenResult != null) {
                    completeAuthWithToken(
                        token = tokenResult.accessToken,
                        refreshToken = tokenResult.refreshToken,
                        expiresInSeconds = tokenResult.expiresIn
                    )
                } else {
                    publish(isLoading = false, errorMessage = getString(Res.string.kitsu_auth_exchange_failed))
                }
                return@launch
            }

            publish(isLoading = false, errorMessage = getString(Res.string.kitsu_auth_no_code))
        }
    }

    private suspend fun completeAuthWithToken(token: String, refreshToken: String?, expiresInSeconds: Long) {
        val user = KitsuApi.fetchUser(token)
        if (user == null) {
            publish(isLoading = false, errorMessage = "Impossibile recuperare il profilo utente da Kitsu.")
            return
        }

        val expiresAt = WatchProgressClock.nowEpochMs() + (expiresInSeconds * 1000L)
        authState = KitsuAuthState(
            accessToken = token,
            refreshToken = refreshToken,
            username = user.attributes?.name ?: user.attributes?.slug,
            avatarUrl = user.attributes?.avatar?.large ?: user.attributes?.avatar?.medium,
            userId = user.id?.toLongOrNull(),
            tokenExpiresAtEpochMs = expiresAt
        )
        persist()
        _isAuthenticated.value = true
        publish(isLoading = false, errorMessage = null)

        scope.launch {
            runCatching {
                KitsuLibraryRepository.refreshNow()
                KitsuSyncCoordinator.syncNow()
            }.onFailure {
                log.e { "Initial Kitsu sync after login failed: ${it.message}" }
            }
        }
    }

    private suspend fun refreshTokenIfNeeded(): Boolean {
        val state = authState
        val refreshTok = state.refreshToken
        if (refreshTok.isNullOrBlank()) return true

        val expiresAt = state.tokenExpiresAtEpochMs
        if (expiresAt != null && WatchProgressClock.nowEpochMs() < expiresAt - 60_000L) {
            return true
        }

        log.i { "Kitsu access token expired or expiring. Refreshing..." }
        publish(isLoading = false, errorMessage = null)

        return try {
            val result = KitsuApi.refreshAccessToken(refreshTok)
            if (result != null) {
                val newExpiresAt = WatchProgressClock.nowEpochMs() + (result.expiresIn * 1000L)
                authState = authState.copy(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken ?: refreshTok,
                    tokenExpiresAtEpochMs = newExpiresAt
                )
                persist()
                publish()
                log.i { "Kitsu token refreshed successfully." }
                true
            } else {
                log.e { "Failed to refresh Kitsu token." }
                false
            }
        } catch (e: Exception) {
            log.e { "Failed to refresh Kitsu token: ${e.message}" }
            false
        }
    }

    fun disconnect() {
        clearLocalState()
        scope.launch {
            KitsuLibraryRepository.clearLocalState()
        }
    }

    fun getAccessToken(): String? {
        ensureLoaded()
        return authState.accessToken
    }

    suspend fun getAccessTokenRefreshed(): String? {
        ensureLoaded()
        return if (refreshTokenIfNeeded()) authState.accessToken else null
    }

    fun getUserId(): Long? {
        ensureLoaded()
        return authState.userId
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = KitsuStorage.loadAuthPayload().orEmpty().trim()
        authState = if (payload.isBlank()) {
            KitsuAuthState()
        } else {
            runCatching { json.decodeFromString<KitsuAuthState>(payload) }
                .getOrElse {
                    log.w { "Failed to parse Kitsu auth payload: ${it.message}" }
                    KitsuAuthState()
                }
        }
        _isAuthenticated.value = authState.isAuthenticated
        publish()
    }

    private fun persist() {
        runCatching {
            val payload = json.encodeToString(authState)
            KitsuStorage.saveAuthPayload(payload)
        }.onFailure {
            log.w { "Failed to persist Kitsu auth state: ${it.message}" }
        }
    }

    private fun publish(
        isLoading: Boolean = _uiState.value.isLoading,
        errorMessage: String? = _uiState.value.errorMessage
    ) {
        val mode = when {
            authState.isAuthenticated -> KitsuConnectionMode.CONNECTED
            isLoading -> KitsuConnectionMode.LOADING
            else -> KitsuConnectionMode.DISCONNECTED
        }

        _uiState.value = KitsuAuthUiState(
            mode = mode,
            username = authState.username,
            avatarUrl = authState.avatarUrl,
            tokenExpiresAtEpochMs = authState.tokenExpiresAtEpochMs,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }
}
