package com.nuvio.app.features.p2p

import com.nuvio.app.core.i18n.localizedP2pAddTorrentFailed
import com.nuvio.app.core.i18n.localizedP2pUnknownTorrentError
import com.nuvio.app.core.logging.InAppLogger
import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.features.player.desktop.DesktopHostOs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Files

private const val TAG = "P2pStreamingEngine"
private val VIDEO_EXTENSIONS = setOf("mkv", "mp4", "avi", "webm", "ts", "m4v", "mov", "wmv", "flv")

actual object P2pStreamingEngine {
    private val _state = MutableStateFlow<P2pStreamingState>(P2pStreamingState.Idle)
    actual val state: StateFlow<P2pStreamingState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleLock = Any()
    private var statsJob: Job? = null
    private var cleanupJob: Job? = null
    private var currentHash: String? = null
    private var streamGeneration = 0L
    private val binary = TorrServerBinary()
    private val api = TorrServerApi(binary)

    actual suspend fun startStream(request: P2pStreamRequest): String = withContext(Dispatchers.IO) {
        stopStreamNow(stopBinary = false)
        val generation = nextStreamGeneration()
        _state.value = P2pStreamingState.Connecting

        try {
            binary.start()
            ensureCurrentGeneration(generation)

            val magnetLink = buildMagnetUri(request.infoHash, request.trackers)
            InAppLogger.info("P2P", "Starting stream: $magnetLink")

            val hash = api.addTorrent(magnetLink)
                ?: throw P2pStreamingException(localizedP2pAddTorrentFailed())
            if (!attachTorrentIfCurrent(generation, hash)) {
                api.dropTorrent(hash)
                throw CancellationException("P2P stream start was cancelled")
            }

            val resolvedIdx = resolveFileIndex(
                hash = hash,
                requestedIdx = request.fileIdx,
                filename = request.filename,
            )
            ensureCurrentGeneration(generation)

            val streamUrl = api.getStreamUrl(magnetLink, resolvedIdx)
            InAppLogger.info("P2P", "Stream URL: $streamUrl")

            startStatsPolling(hash, generation)

            ensureCurrentGeneration(generation)
            _state.value = P2pStreamingState.Streaming(
                localUrl = streamUrl,
                downloadSpeed = 0,
                uploadSpeed = 0,
                peers = 0,
                seeds = 0,
                bufferProgress = 0f,
                totalProgress = 0f,
            )

            streamUrl
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isCurrentGeneration(generation)) {
                _state.value = P2pStreamingState.Error(e.message ?: localizedP2pUnknownTorrentError())
            }
            throw e
        }
    }

    actual fun stopStream() {
        scheduleStop(stopBinary = false)
    }

    actual fun shutdown() {
        scheduleStop(stopBinary = true)
    }

    private fun scheduleStop(stopBinary: Boolean) {
        val hash = detachActiveStream()
        val previousCleanup = cleanupJob
        cleanupJob = scope.launch {
            previousCleanup?.join()
            cleanupDetachedStream(hash, stopBinary)
        }
    }

    private suspend fun stopStreamNow(stopBinary: Boolean) {
        cleanupJob?.join()
        val hash = detachActiveStream()
        cleanupDetachedStream(hash, stopBinary)
    }

    private fun detachActiveStream(): String? {
        val detached = synchronized(lifecycleLock) {
            streamGeneration += 1
            val hash = currentHash
            val job = statsJob
            currentHash = null
            statsJob = null
            hash to job
        }
        detached.second?.cancel()
        _state.value = P2pStreamingState.Idle
        return detached.first
    }

    private suspend fun cleanupDetachedStream(hash: String?, stopBinary: Boolean) {
        hash?.let {
            try {
                api.dropTorrent(it)
            } catch (e: Exception) {
                InAppLogger.warn("P2P", "Error dropping torrent: ${e.message}")
            }
        }

        if (stopBinary) {
            try {
                binary.stop()
            } catch (e: Exception) {
                InAppLogger.warn("P2P", "Error stopping TorrServer: ${e.message}")
            }
        }
    }

    private fun nextStreamGeneration(): Long =
        synchronized(lifecycleLock) {
            streamGeneration += 1
            streamGeneration
        }

    private fun attachTorrentIfCurrent(generation: Long, hash: String): Boolean =
        synchronized(lifecycleLock) {
            if (streamGeneration != generation) return@synchronized false
            currentHash = hash
            true
        }

    private fun isCurrentGeneration(generation: Long): Boolean =
        synchronized(lifecycleLock) { streamGeneration == generation }

    private fun ensureCurrentGeneration(generation: Long) {
        if (!isCurrentGeneration(generation)) {
            throw CancellationException("P2P stream start was cancelled")
        }
    }

    private fun buildMagnetUri(infoHash: String, extraTrackers: List<String>): String {
        val trackers = (DEFAULT_TRACKERS + extraTrackers).distinct()
        val trackerParams = trackers.joinToString("") { "&tr=$it" }
        return "magnet:?xt=urn:btih:$infoHash$trackerParams"
    }

    private suspend fun resolveFileIndex(hash: String, requestedIdx: Int?, filename: String?): Int {
        val deadline = System.currentTimeMillis() + 15_000L
        var files: List<TorrServerFile> = emptyList()

        while (System.currentTimeMillis() < deadline) {
            files = api.getTorrentStats(hash)?.files ?: emptyList()
            if (files.isNotEmpty()) break
            delay(1_000L)
        }

        if (files.isEmpty()) {
            InAppLogger.warn("P2P", "No files after metadata timeout, guessing index ${requestedIdx?.plus(1) ?: 1}")
            return requestedIdx?.plus(1) ?: 1
        }

        if (!filename.isNullOrBlank()) {
            val name = filename.trim()
            val exact = files.firstOrNull { file ->
                file.path.substringAfterLast('/').equals(name, ignoreCase = true)
            }
            if (exact != null) {
                InAppLogger.info("P2P", "File resolved by exact filename match: ${exact.path} -> id=${exact.id}")
                return exact.id
            }

            val contains = files.firstOrNull { file ->
                file.path.contains(name, ignoreCase = true)
            }
            if (contains != null) {
                InAppLogger.info("P2P", "File resolved by filename contains match: ${contains.path} -> id=${contains.id}")
                return contains.id
            }
        }

        if (requestedIdx != null) {
            val torrServerIndex = requestedIdx + 1
            if (files.any { it.id == torrServerIndex }) {
                return torrServerIndex
            }
        }

        if (requestedIdx != null && requestedIdx in files.indices) {
            val positionalFile = files[requestedIdx]
            return positionalFile.id
        }

        val videoFile = files
            .filter { file ->
                val ext = file.path.substringAfterLast('.', "").lowercase()
                ext in VIDEO_EXTENSIONS
            }
            .maxByOrNull { it.length }

        return videoFile?.id ?: files.maxByOrNull { it.length }?.id ?: 1
    }

    private fun startStatsPolling(hash: String, generation: Long) {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                if (!isCurrentGeneration(generation)) return@launch
                try {
                    val stats = api.getTorrentStats(hash)
                    val currentState = _state.value
                    if (
                        stats != null &&
                        currentState is P2pStreamingState.Streaming &&
                        isCurrentGeneration(generation)
                    ) {
                        _state.value = currentState.copy(
                            downloadSpeed = stats.downloadSpeed,
                            uploadSpeed = stats.uploadSpeed,
                            peers = stats.peers,
                            seeds = stats.seeds,
                            preloadedBytes = stats.preloadedBytes,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    InAppLogger.warn("P2P", "Stats polling error: ${e.message}")
                }
                delay(1_000L)
            }
        }
    }

    private val DEFAULT_TRACKERS = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://tracker.torrent.eu.org:451/announce",
    )

    private class TorrServerBinary {
        private var process: Process? = null

        val baseUrl: String get() = "http://127.0.0.1:$PORT"

        private fun binaryFile(): File {
            val toolsDir = DesktopStorage.rootDir.resolve("tools").toFile().apply { mkdirs() }
            val target = File(toolsDir, "torrserver.exe")
            if (!target.exists() || target.length() != expectedResourceSize()) {
                extractResource(target)
            }
            if (!target.exists()) {
                throw P2pStreamingException("TorrServer binary is not available on $platformName")
            }
            return target
        }

        private fun expectedResourceSize(): Long {
            val resource = "/native/windows/torrserver.exe"
            val input = P2pStreamingEngine::class.java.getResourceAsStream(resource) ?: return -1L
            return input.use { it.readBytes().size.toLong() }
        }

        private fun extractResource(target: File) {
            val resource = "/native/windows/torrserver.exe"
            val input = P2pStreamingEngine::class.java.getResourceAsStream(resource)
                ?: throw P2pStreamingException("Missing bundled torrserver.exe for $platformName")
            input.use { source ->
                val tmp = File(target.parentFile, "${target.name}.tmp")
                tmp.outputStream().use { output -> source.copyTo(output) }
                Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
        }

        private val platformName: String
            get() = when (DesktopHostOs.current) {
                DesktopHostOs.WINDOWS -> "Windows"
                DesktopHostOs.MACOS -> "macOS"
                DesktopHostOs.LINUX -> "Linux"
                else -> "this platform"
            }

        suspend fun start() = withContext(Dispatchers.IO) {
            if (isRunning()) {
                InAppLogger.info("P2P", "TorrServer already running")
                return@withContext
            }

            killOrphanedProcess()

            val binary = binaryFile()

            val configDir = DesktopStorage.rootDir.resolve("torrserver").toFile().also { it.mkdirs() }
            val processBuilder = ProcessBuilder(
                binary.absolutePath,
                "--port",
                PORT.toString(),
                "--path",
                configDir.absolutePath,
            )
            processBuilder.directory(configDir)
            processBuilder.redirectErrorStream(true)

            InAppLogger.info("P2P", "Starting TorrServer on port $PORT from ${binary.absolutePath}")
            process = processBuilder.start()

            val proc = process!!
            Thread {
                try {
                    proc.inputStream.bufferedReader().forEachLine { line ->
                        InAppLogger.debug("P2P/Server", line)
                    }
                } catch (_: Exception) {
                }
            }.apply {
                isDaemon = true
                start()
            }

            val deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                if (isRunning()) {
                    InAppLogger.info("P2P", "TorrServer started successfully")
                    return@withContext
                }
                if (!isProcessAlive(process)) {
                    val exitCode = process?.exitValue() ?: -1
                    process = null
                    throw P2pStreamingException("TorrServer process died on startup (exit code $exitCode)")
                }
                delay(HEALTH_CHECK_INTERVAL_MS)
            }

            stop()
            throw P2pStreamingException("TorrServer failed to start within ${STARTUP_TIMEOUT_MS / 1000}s")
        }

        fun isRunning(): Boolean {
            return try {
                val response = SimpleHttp.get("$baseUrl/echo", timeoutMs = 2_000)
                response != null && response
            } catch (e: Exception) {
                false
            }
        }

        fun stop() {
            try {
                SimpleHttp.get("$baseUrl/shutdown", timeoutMs = 2_000)
            } catch (_: Exception) {
            }

            process?.let { proc ->
                try {
                    Thread.sleep(3_000L)
                    if (isProcessAlive(proc)) {
                        proc.destroyForcibly()
                    }
                } catch (_: Exception) {
                    proc.destroyForcibly()
                }
            }
            process = null
            InAppLogger.info("P2P", "TorrServer stopped")
        }

        private fun killOrphanedProcess() {
            try {
                SimpleHttp.get("$baseUrl/shutdown", timeoutMs = 2_000)
                Thread.sleep(1_000L)
                InAppLogger.info("P2P", "Shut down orphaned TorrServer instance")
            } catch (_: Exception) {
            }
        }

        private fun isProcessAlive(proc: Process?): Boolean {
            if (proc == null) return false
            return try {
                proc.exitValue()
                false
            } catch (_: IllegalThreadStateException) {
                true
            } catch (_: Exception) {
                false
            }
        }

        companion object {
            const val PORT = 8091
            private const val STARTUP_TIMEOUT_MS = 15_000L
            private const val HEALTH_CHECK_INTERVAL_MS = 200L
        }
    }

    private data class TorrServerFile(
        val id: Int,
        val path: String,
        val length: Long,
    )

    private data class TorrServerStats(
        val downloadSpeed: Long,
        val uploadSpeed: Long,
        val peers: Int,
        val seeds: Int,
        val preloadedBytes: Long,
        val loadedSize: Long,
        val torrentSize: Long,
        val files: List<TorrServerFile>,
    )

    private class TorrServerApi(
        private val binary: TorrServerBinary,
    ) {
        private val baseUrl: String get() = binary.baseUrl

        suspend fun addTorrent(magnetLink: String, title: String? = null): String? = withContext(Dispatchers.IO) {
            val body = buildJsonObjectString {
                put("action", "add")
                put("link", magnetLink)
                put("save_to_db", false)
                if (title != null) put("title", title)
            }

            try {
                val response = SimpleHttp.postJson("$baseUrl/torrents", body, timeoutMs = 30_000)
                    ?: return@withContext null
                val json = Json.parseToJsonElement(response).jsonObject
                val hash = json["hash"]?.jsonPrimitive?.content.orEmpty()
                if (hash.isNotBlank()) {
                    InAppLogger.info("P2P", "Torrent added: $hash")
                } else {
                    InAppLogger.error("P2P", "addTorrent failed: missing hash")
                }
                hash.ifEmpty { null }
            } catch (e: Exception) {
                InAppLogger.error("P2P", "addTorrent error: ${e.message}")
                null
            }
        }

        suspend fun getTorrentStats(hash: String): TorrServerStats? = withContext(Dispatchers.IO) {
            val body = buildJsonObjectString {
                put("action", "get")
                put("hash", hash)
            }

            try {
                val response = SimpleHttp.postJson("$baseUrl/torrents", body, timeoutMs = 30_000)
                    ?: return@withContext null
                val json = Json.parseToJsonElement(response).jsonObject

                val files = mutableListOf<TorrServerFile>()
                val fileList = json["file_stats"] as? JsonArray ?: emptyList()
                for ((index, element) in fileList.withIndex()) {
                    val file = (element as? JsonObject) ?: continue
                    files.add(
                        TorrServerFile(
                            id = file["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: (index + 1),
                            path = file["path"]?.jsonPrimitive?.content.orEmpty(),
                            length = file["length"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        ),
                    )
                }

                TorrServerStats(
                    downloadSpeed = json["download_speed"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    uploadSpeed = json["upload_speed"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    peers = json["active_peers"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    seeds = json["connected_seeders"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    preloadedBytes = json["preloaded_bytes"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    loadedSize = json["loaded_size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    torrentSize = json["torrent_size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    files = files,
                )
            } catch (e: Exception) {
                InAppLogger.warn("P2P", "getTorrentStats error: ${e.message}")
                null
            }
        }

        suspend fun dropTorrent(hash: String) = withContext(Dispatchers.IO) {
            val body = buildJsonObjectString {
                put("action", "drop")
                put("hash", hash)
            }

            try {
                SimpleHttp.postJson("$baseUrl/torrents", body, timeoutMs = 10_000)
                InAppLogger.info("P2P", "Torrent dropped: $hash")
            } catch (e: Exception) {
                InAppLogger.warn("P2P", "dropTorrent error: ${e.message}")
            }
        }

        fun getStreamUrl(magnetLink: String, fileIdx: Int): String {
            val encodedLink = URLEncoder.encode(magnetLink, "UTF-8")
            return "$baseUrl/stream?link=$encodedLink&index=$fileIdx&play"
        }
    }
}

private object SimpleHttp {
    fun get(url: String, timeoutMs: Int): Boolean {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        try {
            val code = connection.responseCode
            connection.inputStream?.use { it.readBytes() }
            return code in 200..299
        } finally {
            connection.disconnect()
        }
    }

    fun postJson(url: String, body: String, timeoutMs: Int): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) return null
            return connection.inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

private fun buildJsonObjectString(block: MutableMap<String, Any>.() -> Unit): String {
    val map = LinkedHashMap<String, Any>()
    map.block()
    val entries = map.entries.joinToString(",") { (key, value) ->
        "\"$key\":${toJsonValue(value)}"
    }
    return "{$entries}"
}

private fun toJsonValue(value: Any): String = when (value) {
    is Boolean -> if (value) "true" else "false"
    is Number -> value.toString()
    is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    else -> "\"$value\""
}