package com.nuvio.app.features.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object VpnController {
    private const val TAG = "NuvioVpnController"
    const val VPN_PERMISSION_REQUEST = 721

    private val _runtimeState = MutableStateFlow(VpnRuntimeState.OFF)
    actual val runtimeState: StateFlow<VpnRuntimeState> = _runtimeState.asStateFlow()

    private val _pendingPermission = MutableStateFlow(false)
    actual val pendingPermission: StateFlow<Boolean> = _pendingPermission.asStateFlow()

    private var appContext: Context? = null
    private var backend: GoBackend? = null
    private var tunnel: Tunnel? = null
    private var pendingConfigText: String? = null
    private var currentActivity: Activity? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        backend = GoBackend(appContext)
    }

    fun bindActivity(activity: Activity) {
        currentActivity = activity
    }

    fun unbindActivity(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    fun hasPendingActivation(): Boolean = pendingConfigText != null

    fun permissionIntent(): Intent? = appContext?.let { VpnService.prepare(it) }

    fun handlePermissionIfNeeded(activity: Activity) {
        bindActivity(activity)
        if (!hasPendingActivation()) return
        requestPendingPermission()
    }

    actual fun requestPendingPermission() {
        if (!hasPendingActivation()) return
        val activity = currentActivity ?: return
        val intent = permissionIntent()
        if (intent == null) {
            retryPendingActivation()
        } else {
            activity.startActivityForResult(intent, VPN_PERMISSION_REQUEST)
        }
    }

    fun retryPendingActivation() {
        val configText = pendingConfigText ?: return
        pendingConfigText = null
        _pendingPermission.value = false
        activate(configText)
    }

    actual fun activate(configText: String): VpnActivationResult {
        val currentBackend = backend ?: return VpnActivationResult.FAILED
        val config = try {
            Config.parse(configText.byteInputStream(Charsets.UTF_8))
        } catch (e: Exception) {
            Log.w(TAG, "Invalid WireGuard config", e)
            return VpnActivationResult.INVALID_CONFIG
        }
        val newTunnel = object : Tunnel {
            override fun getName(): String = "nuvio-vpn"
            override fun onStateChange(newState: Tunnel.State) {
                _runtimeState.value = when (newState) {
                    Tunnel.State.UP -> VpnRuntimeState.ACTIVE
                    Tunnel.State.TOGGLE -> VpnRuntimeState.CONNECTING
                    Tunnel.State.DOWN -> VpnRuntimeState.OFF
                }
            }
        }
        _runtimeState.value = VpnRuntimeState.CONNECTING
        return try {
            currentBackend.setState(newTunnel, Tunnel.State.UP, config)
            tunnel = newTunnel
            pendingConfigText = null
            _runtimeState.value = VpnRuntimeState.ACTIVE
            VpnActivationResult.ACTIVE
        } catch (e: BackendException) {
            if (e.reason == BackendException.Reason.VPN_NOT_AUTHORIZED) {
                pendingConfigText = configText
                _pendingPermission.value = true
                _runtimeState.value = VpnRuntimeState.OFF
                VpnActivationResult.UNAUTHORIZED
            } else {
                Log.w(TAG, "Failed to start tunnel: ${e.reason}", e)
                _runtimeState.value = VpnRuntimeState.OFF
                VpnActivationResult.FAILED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start tunnel", e)
            _runtimeState.value = VpnRuntimeState.OFF
            VpnActivationResult.FAILED
        }
    }

    actual fun deactivate(): Boolean {
        val currentBackend = backend
        val currentTunnel = tunnel
        pendingConfigText = null
        _pendingPermission.value = false
        if (currentBackend == null || currentTunnel == null) {
            _runtimeState.value = VpnRuntimeState.OFF
            return true
        }
        return try {
            currentBackend.setState(currentTunnel, Tunnel.State.DOWN, null)
            tunnel = null
            _runtimeState.value = VpnRuntimeState.OFF
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop tunnel", e)
            false
        }
    }
}
