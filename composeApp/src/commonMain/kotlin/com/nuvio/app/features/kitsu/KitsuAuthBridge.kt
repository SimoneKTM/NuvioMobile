package com.nuvio.app.features.kitsu

fun handleKitsuAuthCallbackUrl(url: String) {
    KitsuAuthRepository.onAuthCallbackReceived(url)
}
