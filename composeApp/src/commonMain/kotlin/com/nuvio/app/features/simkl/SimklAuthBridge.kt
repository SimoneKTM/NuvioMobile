package com.nuvio.app.features.simkl

fun handleSimklAuthCallbackUrl(url: String) {
    SimklAuthRepository.handleAuthCallback(url)
}
