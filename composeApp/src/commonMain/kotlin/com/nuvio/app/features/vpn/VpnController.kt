package com.nuvio.app.features.vpn

import kotlinx.coroutines.flow.StateFlow

enum class VpnActivationResult {
    ACTIVE,
    UNAUTHORIZED,
    INVALID_CONFIG,
    FAILED,
}

enum class VpnRuntimeState {
    OFF,
    CONNECTING,
    ACTIVE,
}

expect object VpnController {
    val runtimeState: StateFlow<VpnRuntimeState>
    fun activate(configText: String): VpnActivationResult
    fun deactivate(): Boolean
}
