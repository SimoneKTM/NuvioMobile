package com.nuvio.app.features.mal

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.library.LibrarySourceMode
import com.nuvio.app.features.trakt.TraktSettingsRepository
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

object MalAuthRepository {
    private const val AUTH_BASE_URL = "https://myanimelist.net/v1/oauth2"
    private const val API_BASE_URL = "https://api.myanimelist.net/v2"

    private val log = Logger.withTag("MalAuth")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(MalAuthUiState())
    val uiState: StateFlow<MalAuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var hasLoaded = false
    private var authState = MalAuthState()
    private var codeVerifier: String? = null

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun currentAccessToken(): String? {
        ensureLoaded()
        return authState.accessToken?.takeIf { it.isNotBlank() }
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        authState = MalAuthState()
        codeVerifier = null
        publish()
    }

    fun snapshot(): MalAuthUiState {
        ensureLoaded()
        return _uiState.value
    }

    fun hasRequiredCredentials(): Boolean = MalConfig.CLIENT_ID.isNotBlank()

    fun onConnectRequested(): String? {
        ensureLoaded()
        if (!hasRequiredCredentials()) {
            publish(errorMessage = "ID client MAL non configurato")
            return null
        }

        clearPendingAuthorization()
        codeVerifier = null
        MalAuthStorage.saveCodeVerifier(null)

        val codeVerifierValue = generateOauthState(43)
        codeVerifier = codeVerifierValue
        MalAuthStorage.saveCodeVerifier(codeVerifierValue)
        val state = generateOauthState(16)
        authState = authState.copy(
            pendingAuthorizationState = state,
            pendingAuthorizationStartedAtMillis = nowEpochMs(),
        )
        persist()
        publish(
            statusMessage = "Completa l'accesso nel browser",
            errorMessage = null,
        )

        return buildAuthorizationUrl(state, codeVerifierValue)
    }

    fun pendingAuthorizationUrl(): String? {
        ensureLoaded()
        val state = authState.pendingAuthorizationState ?: return null
        val verifier = codeVerifier ?: MalAuthStorage.loadCodeVerifier() ?: return null
        return buildAuthorizationUrl(state, verifier)
    }

    fun onCancelAuthorization() {
        ensureLoaded()
        clearPendingAuthorization()
        codeVerifier = null
        persist()
        publish(statusMessage = null, errorMessage = null)
    }

    fun onCancelDeviceFlow() {
        onCancelAuthorization()
    }

    fun onAuthLaunchFailed(reason: String) {
        publish(errorMessage = reason)
    }

    fun onAuthCallbackReceived(callbackUrl: String) {
        ensureLoaded()
        if (!callbackUrl.startsWith("${MalConfig.REDIRECT_URI}?", ignoreCase = true) &&
            !callbackUrl.equals(MalConfig.REDIRECT_URI, ignoreCase = true)
        ) {
            return
        }

        scope.launch {
            completeAuthorizationFromCallback(callbackUrl)
        }
    }

    suspend fun fetchUserProfile(): String? {
        ensureLoaded()
        val token = authState.accessToken?.takeIf { it.isNotBlank() } ?: return null
        val hasValidToken = refreshTokenIfNeeded(force = false)
        if (!hasValidToken) return null

        val response = runCatching {
            httpGetTextWithHeaders(
                url = "$API_BASE_URL/users/@me",
                headers = mapOf("Authorization" to "Bearer ${authState.accessToken?.trim().orEmpty()}"),
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            log.w { "Failed to fetch MAL user: ${e.message}" }
        }.getOrNull() ?: return null

        val parsed = runCatching {
            json.decodeFromString<MalUserResponse>(response)
        }.getOrNull()

        parsed?.let {
            authState = authState.copy(username = it.name)
            persist()
            publish()
        }

        return parsed?.name
    }

    fun onDisconnectRequested() {
        ensureLoaded()
        scope.launch {
            disconnect()
        }
    }

    private suspend fun completeAuthorizationFromCallback(callbackUrl: String) {
        publish(isLoading = true, errorMessage = null)

        val parsedUrl = runCatching { Url(callbackUrl) }
            .onFailure {
                log.w { "Invalid MAL callback URL: ${it.message}" }
            }
            .getOrNull()

        if (parsedUrl == null) {
            clearPendingAuthorization()
            codeVerifier = null
            MalAuthStorage.saveCodeVerifier(null)
            persist()
            publish(isLoading = false, errorMessage = "URL di callback non valido")
            return
        }

        val errorCode = parsedUrl.parameters["error"]
        if (!errorCode.isNullOrBlank()) {
            clearPendingAuthorization()
            codeVerifier = null
            MalAuthStorage.saveCodeVerifier(null)
            persist()
            publish(isLoading = false, errorMessage = "Accesso negato")
            return
        }

        val code = parsedUrl.parameters["code"].orEmpty().trim()
        if (code.isBlank()) {
            clearPendingAuthorization()
            codeVerifier = null
            MalAuthStorage.saveCodeVerifier(null)
            persist()
            publish(isLoading = false, errorMessage = "Codice di autorizzazione mancante")
            return
        }

        val expectedState = authState.pendingAuthorizationState
        val callbackState = parsedUrl.parameters["state"].orEmpty().trim()
        if (!expectedState.isNullOrBlank() && callbackState != expectedState) {
            clearPendingAuthorization()
            codeVerifier = null
            MalAuthStorage.saveCodeVerifier(null)
            persist()
            publish(isLoading = false, errorMessage = "Mismatch dello stato - accesso annullato")
            return
        }

        exchangeAuthorizationCode(code)
    }

    private suspend fun exchangeAuthorizationCode(code: String) {
        val verifier = codeVerifier ?: MalAuthStorage.loadCodeVerifier() ?: run {
            clearPendingAuthorization()
            persist()
            publish(isLoading = false, errorMessage = "Codice di verifica mancante")
            return
        }

        val body = buildString {
            append("client_id=").append(MalConfig.CLIENT_ID.encodeURLParameter())
            append("&code=").append(code.encodeURLParameter())
            append("&code_verifier=").append(verifier.encodeURLParameter())
            append("&grant_type=authorization_code")
            append("&redirect_uri=").append(MalConfig.REDIRECT_URI.encodeURLParameter())
            if (MalConfig.CLIENT_SECRET.isNotBlank()) {
                append("&client_secret=").append(MalConfig.CLIENT_SECRET.encodeURLParameter())
            }
        }

        val response = runCatching {
            httpRequestRaw(
                method = "POST",
                url = "$AUTH_BASE_URL/token",
                body = body,
                headers = mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/x-www-form-urlencoded",
                ),
                followRedirects = false,
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            log.w { "Failed to exchange MAL auth code: ${e.message}" }
        }.getOrNull()

        codeVerifier = null
        MalAuthStorage.saveCodeVerifier(null)

        if (response == null) {
            clearPendingAuthorization()
            persist()
            publish(isLoading = false, errorMessage = "Accesso non riuscito")
            return
        }

        if (response.status != 200) {
            val errorDetail = response.body.trim().take(200)
            log.w { "MAL token exchange failed: HTTP ${response.status} - $errorDetail" }
            clearPendingAuthorization()
            persist()
            val malError = runCatching {
                json.decodeFromString<MalTokenErrorResponse>(response.body)
            }.getOrNull()
            publish(
                isLoading = false,
                errorMessage = malError?.error?.replaceFirstChar { it.uppercase() }
                    ?: "Accesso non riuscito (HTTP ${response.status})",
            )
            return
        }

        val parsed = runCatching {
            json.decodeFromString<MalTokenResponse>(response.body)
        }.getOrNull()

        if (parsed == null) {
            clearPendingAuthorization()
            persist()
            publish(isLoading = false, errorMessage = "Risposta del token non valida")
            return
        }

        authState = authState.copy(
            accessToken = parsed.accessToken,
            refreshToken = parsed.refreshToken,
            tokenType = parsed.tokenType,
            createdAt = nowEpochMs() / 1_000L,
            expiresIn = parsed.expiresIn,
            pendingAuthorizationState = null,
            pendingAuthorizationStartedAtMillis = null,
        )
        persist()
        fetchUserProfile()
        TraktSettingsRepository.setLibrarySourceMode(LibrarySourceMode.MAL)
        scope.launch {
            runCatching {
                MalLibraryRepository.refreshNow()
                MalSyncCoordinator.syncNow()
            }.onFailure {
                log.e { "Initial MAL sync after login failed: ${it.message}" }
            }
        }
        publish(
            isLoading = false,
            statusMessage = "Connesso a MyAnimeList",
            errorMessage = null,
        )
    }

    private suspend fun disconnect() {
        publish(isLoading = true, errorMessage = null)

        authState = MalAuthState()
        persist()
        publish(
            isLoading = false,
            statusMessage = "Disconnesso da MyAnimeList",
            errorMessage = null,
        )
    }

    suspend fun refreshTokenIfNeeded(force: Boolean = false): Boolean {
        val refreshToken = authState.refreshToken?.takeIf { it.isNotBlank() } ?: return false

        if (!force && !isTokenExpiredOrExpiring(authState)) {
            return true
        }

        val body = buildString {
            append("client_id=").append(MalConfig.CLIENT_ID.encodeURLParameter())
            append("&grant_type=refresh_token")
            append("&refresh_token=").append(refreshToken.encodeURLParameter())
            if (MalConfig.CLIENT_SECRET.isNotBlank()) {
                append("&client_secret=").append(MalConfig.CLIENT_SECRET.encodeURLParameter())
            }
        }

        val response = runCatching {
            httpPostJsonWithHeaders(
                url = "$AUTH_BASE_URL/token",
                body = body,
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            log.w { "MAL token refresh failed: ${e.message}" }
        }.getOrNull() ?: return false

        val parsed = runCatching {
            json.decodeFromString<MalTokenResponse>(response)
        }.getOrNull() ?: return false

        authState = authState.copy(
            accessToken = parsed.accessToken,
            refreshToken = parsed.refreshToken,
            tokenType = parsed.tokenType,
            createdAt = nowEpochMs() / 1_000L,
            expiresIn = parsed.expiresIn,
        )
        persist()
        publish()
        return true
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = MalAuthStorage.loadPayload().orEmpty().trim()
        authState = if (payload.isBlank()) {
            MalAuthState()
        } else {
            runCatching { json.decodeFromString<MalAuthState>(payload) }
                .getOrElse {
                    log.w { "Failed to parse MAL auth payload: ${it.message}" }
                    MalAuthState()
                }
        }
        publish(statusMessage = null, errorMessage = null)
    }

    private fun clearPendingAuthorization() {
        authState = authState.copy(
            pendingAuthorizationState = null,
            pendingAuthorizationStartedAtMillis = null,
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
            authState.isAuthenticated -> MalConnectionMode.CONNECTED
            !authState.pendingAuthorizationState.isNullOrBlank() -> MalConnectionMode.AWAITING_APPROVAL
            else -> MalConnectionMode.DISCONNECTED
        }

        _isAuthenticated.value = authState.isAuthenticated
        _uiState.value = MalAuthUiState(
            mode = mode,
            credentialsConfigured = hasRequiredCredentials(),
            isLoading = isLoading,
            username = authState.username,
            tokenExpiresAtMillis = tokenExpiresAtMillis,
            pendingAuthorizationStartedAtMillis = authState.pendingAuthorizationStartedAtMillis,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    private fun persist() {
        MalAuthStorage.savePayload(json.encodeToString(authState))
    }

    private fun buildAuthorizationUrl(state: String, codeChallenge: String): String {
        val responseType = "code"
        val encodedClientId = MalConfig.CLIENT_ID.encodeURLParameter()
        val encodedRedirectUri = MalConfig.REDIRECT_URI.encodeURLParameter()
        val encodedState = state.encodeURLParameter()
        val encodedChallenge = codeChallenge.encodeURLParameter()
        val scope = "write:users:read+write:anime:read"
        return "${AUTH_BASE_URL}/authorize?response_type=$responseType&client_id=$encodedClientId&redirect_uri=$encodedRedirectUri&state=$encodedState&code_challenge=$encodedChallenge&code_challenge_method=plain&scope=$scope"
    }

    private fun generateOauthState(minLength: Int = 16): String {
        val nowPart = (nowEpochMs()).toString(16)
        val parts = mutableListOf(nowPart)
        while (parts.sumOf { it.length } < minLength) {
            parts.add(Random.nextLong().toULong().toString(16))
        }
        return parts.joinToString("")
    }

    private fun isTokenExpiredOrExpiring(state: MalAuthState): Boolean {
        val createdAt = state.createdAt ?: return true
        val expiresIn = state.expiresIn ?: return true
        val expiresAtSeconds = createdAt + expiresIn
        val nowSeconds = nowEpochMs() / 1_000L
        return nowSeconds >= (expiresAtSeconds - 60)
    }

    private fun nowEpochMs(): Long = MalPlatformClock.nowEpochMs()
}
