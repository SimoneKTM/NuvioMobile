package com.nuvio.app.features.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object VpnController {
    const val VPN_PERMISSION_REQUEST = 721

    actual val runtimeState: StateFlow<VpnRuntimeState> =
        MutableStateFlow(VpnRuntimeState.OFF).asStateFlow()

    fun initialize(context: Context) {}

    fun hasPendingActivation(): Boolean = false

    fun permissionIntent(): Intent? = null

    fun handlePermissionIfNeeded(activity: Activity) {}

    fun retryPendingActivation() {}

    actual fun activate(configText: String): VpnActivationResult = VpnActivationResult.FAILED

    actual fun deactivate(): Boolean = true
}
