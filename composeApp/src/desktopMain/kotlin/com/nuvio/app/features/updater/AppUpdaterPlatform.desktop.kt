package com.nuvio.app.features.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.updates_download_failed_http
import nuvio.composeapp.generated.resources.updates_downloaded_file_missing
import nuvio.composeapp.generated.resources.updates_empty_download_body
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.prefs.Preferences

actual object AppUpdaterPlatform {
    private const val preferencesName = "nuvio/updater"
    private const val ignoredTagKey = "ignored_release_tag"

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().contains("win", ignoreCase = true)

    private fun preferredExtension(): String? =
        when {
            isWindows() -> "msi"
            else -> null
        }

    actual val isSupported: Boolean get() = preferredExtension() != null

    actual fun getSupportedAbis(): List<String> = emptyList()

    actual fun getPreferredAssetExtension(): String? = preferredExtension()

    actual fun getIgnoredTag(): String? =
        preferences().get(ignoredTagKey, null)

    actual fun setIgnoredTag(tag: String?) {
        val prefs = preferences()
        if (tag == null) {
            prefs.remove(ignoredTagKey)
        } else {
            prefs.put(ignoredTagKey, tag)
        }
    }

    actual suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = assetName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destination = File(
                File(System.getProperty("java.io.tmpdir"), "nuvio-updates"),
                safeName,
            ).apply {
                parentFile?.mkdirs()
                if (exists()) delete()
            }

            val connection = URL(assetUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "NuvioMobile")
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000

            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    error(runBlocking { getString(Res.string.updates_download_failed_http, responseCode) })
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
                val input = connection.inputStream ?: error(runBlocking { getString(Res.string.updates_empty_download_body) })
                input.use { source ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloadedBytes = 0L
                        while (true) {
                            val read = source.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            onProgress(downloadedBytes, totalBytes)
                        }
                        output.flush()
                    }
                }
            } finally {
                connection.disconnect()
            }

            destination.absolutePath
        }
    }

    actual fun canRequestPackageInstalls(): Boolean = true

    actual fun openUnknownSourcesSettings() = Unit

    actual fun installDownloadedApk(path: String): Result<Unit> = runCatching {
        val file = File(path)
        check(file.exists()) { runBlocking { getString(Res.string.updates_downloaded_file_missing) } }

        if (isWindows()) {
            ProcessBuilder("msiexec.exe", "/i", file.absolutePath)
                .start()
        } else {
            java.awt.Desktop.getDesktop().open(file)
        }
    }

    private fun preferences(): Preferences =
        Preferences.userRoot().node(preferencesName)
}