package com.nuvio.app.features.settings

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.setAlternateIconName
import platform.UIKit.supportsAlternateIcons
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal actual object NuvioAppIconSwitcher {
    private val alternateIconNames = mapOf(
        NuvioAppIconOption.Enhanced.id to "IconEnhanced",
        NuvioAppIconOption.Monochrome.id to "IconMonochrome",
        NuvioAppIconOption.Neon.id to "IconNeon",
        NuvioAppIconOption.Gear.id to "IconGear",
        NuvioAppIconOption.Chrome.id to "IconChrome",
        NuvioAppIconOption.Aurora.id to "IconAurora",
        NuvioAppIconOption.Emerald.id to "IconEmerald",
    )

    @OptIn(ExperimentalForeignApi::class)
    actual fun apply(iconId: String): Boolean {
        val application = UIApplication.sharedApplication
        if (!application.supportsAlternateIcons) return false
        val iconName = alternateIconNames[iconId]
        dispatch_async(dispatch_get_main_queue()) {
            application.setAlternateIconName(iconName, completionHandler = null)
        }
        return true
    }

    actual fun reapply(iconId: String): Boolean = apply(iconId)

    actual fun closeAfterApply() = Unit
}
