package com.nuvio.app.features.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object VpnController {
    actual val runtimeState: StateFlow<VpnRuntimeState> =
        MutableStateFlow(VpnRuntimeState.OFF).asStateFlow()

    actual fun activate(configText: String): VpnActivationResult = VpnActivationResult.FAILED

    actual fun deactivate(): Boolean = true
}
