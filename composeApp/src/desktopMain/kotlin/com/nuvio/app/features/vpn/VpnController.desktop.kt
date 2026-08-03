package com.nuvio.app.features.vpn

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

internal const val VPN_HELPER_ARG = "--vpn-helper"

actual object VpnController {
    private const val TAG = "NuvioVpnController"
    private const val INTERFACE_NAME = "nuvio0"
    private const val ACTIVATION_TIMEOUT_MS = 30_000L
    private const val STOP_TIMEOUT_MS = 6_000L

    private val _runtimeState = MutableStateFlow(VpnRuntimeState.OFF)
    actual val runtimeState: StateFlow<VpnRuntimeState> = _runtimeState.asStateFlow()

    private val _pendingPermission = MutableStateFlow(false)
    actual val pendingPermission: StateFlow<Boolean> = _pendingPermission.asStateFlow()

    private var pendingConfigText: String? = null

    actual fun activate(configText: String): VpnActivationResult {
        if (!isWindows()) return VpnActivationResult.FAILED
        _pendingPermission.value = false

        val vpnDir = vpnDirFile()
        vpnDir.mkdirs()
        val stopMarker = File(vpnDir, "stop.marker")
        val statusFile = File(vpnDir, "status.txt")
        val confFile = File(vpnDir, "active.conf")

        stopMarker.writeText("1")
        waitUntil({ statusFile.readTextOrEmpty().trim() == "STOPPED" }, STOP_TIMEOUT_MS)
        stopMarker.delete()
        statusFile.delete()
        confFile.writeText(configText)

        _runtimeState.value = VpnRuntimeState.CONNECTING
        pendingConfigText = configText

        val launched = launchElevatedHelper(confFile.absolutePath, vpnDir.absolutePath)
        if (!launched) {
            _runtimeState.value = VpnRuntimeState.OFF
            _pendingPermission.value = true
            return VpnActivationResult.UNAUTHORIZED
        }

        val deadline = System.currentTimeMillis() + ACTIVATION_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val status = statusFile.readTextOrEmpty().trim()
            if (status == "ACTIVE") {
                _runtimeState.value = VpnRuntimeState.ACTIVE
                pendingConfigText = null
                return VpnActivationResult.ACTIVE
            }
            if (status.startsWith("ERROR")) {
                _runtimeState.value = VpnRuntimeState.OFF
                pendingConfigText = null
                return VpnActivationResult.FAILED
            }
            Thread.sleep(250)
        }

        _runtimeState.value = VpnRuntimeState.OFF
        return VpnActivationResult.FAILED
    }

    actual fun deactivate(): Boolean {
        val vpnDir = vpnDirFile()
        File(vpnDir, "stop.marker").writeText("1")
        val statusFile = File(vpnDir, "status.txt")
        waitUntil({ statusFile.readTextOrEmpty().trim() == "STOPPED" }, STOP_TIMEOUT_MS)
        File(vpnDir, "stop.marker").delete()
        File(vpnDir, "active.conf").delete()
        _runtimeState.value = VpnRuntimeState.OFF
        _pendingPermission.value = false
        pendingConfigText = null
        return true
    }

    actual fun requestPendingPermission() {
        val configText = pendingConfigText ?: return
        activate(configText)
    }

    private fun launchElevatedHelper(confPath: String, vpnDirPath: String): Boolean {
        val command = buildList {
            val packagedExe = System.getProperty("jpackage.app-path")
                ?.let(::File)
                ?.takeIf { it.exists() }
            if (packagedExe != null) {
                add(packagedExe.absolutePath)
            } else {
                val javaBin = File(File(System.getProperty("java.home"), "bin"), "java.exe")
                add(javaBin.absolutePath)
                add("-cp")
                add(System.getProperty("java.class.path").orEmpty())
                add("com.nuvio.app.MainKt")
            }
            add(VPN_HELPER_ARG)
            add(confPath)
            add(vpnDirPath)
        }
        val quotedArgs = command.map(::psQuote).joinToString(",")
        val script = "Start-Process -FilePath ${psQuote(command[0])} -ArgumentList $quotedArgs -Verb RunAs"
        return try {
            val process = ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script,
            ).start()
            if (process.waitFor(30, TimeUnit.SECONDS)) {
                process.exitValue() == 0
            } else {
                process.destroyForcibly()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun vpnDirFile(): File =
        File(DesktopStorage.rootDir.toFile(), "vpn")

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().contains("win", ignoreCase = true)

    private fun psQuote(value: String): String =
        "'" + value.replace("'", "''") + "'"

    private fun waitUntil(condition: () -> Boolean, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(200)
        }
    }
}

private fun File.readTextOrEmpty(): String =
    if (exists()) runCatching { readText() }.getOrDefault("") else ""

internal fun runVpnHelperMain(args: Array<String>) {
    if (args.size < 3 || args[0] != VPN_HELPER_ARG) return
    val confPath = args[1]
    val vpnDirPath = args[2]
    val vpnDir = File(vpnDirPath)
    vpnDir.mkdirs()
    val logFile = File(vpnDir, "helper.log")
    logLine(logFile, "helper started, conf=$confPath")

    val toolsDir = File(DesktopStorage.rootDir.toFile(), "tools").apply { mkdirs() }
    try {
        extractIfMissing("wireguard-go.exe", toolsDir)
        extractIfMissing("wg.exe", toolsDir)
        extractIfMissing("wintun.dll", toolsDir)

        runProcess("taskkill.exe", "/f", "/im", "wireguard-go.exe", timeoutMs = 5_000)

        val wireguardGo = File(toolsDir, "wireguard-go.exe")
        val process = ProcessBuilder(wireguardGo.absolutePath, INTERFACE_NAME)
            .directory(toolsDir)
            .redirectErrorStream(true)
            .redirectOutput(File(vpnDir, "wireguard-go.log"))
            .start()
        logLine(logFile, "wireguard-go started")

        val parsed = sanitizeConfig(File(confPath).readText())
        val sanitizedFile = File(vpnDir, "active-sanitized.conf")
        sanitizedFile.writeText(parsed.confText)

        val wg = File(toolsDir, "wg.exe")
        val applied = withRetry(attempts = 40, delayMs = 500) {
            runProcess(wg.absolutePath, "setconf", INTERFACE_NAME, sanitizedFile.absolutePath, timeoutMs = 15_000) == 0
        }
        if (!applied) {
            logLine(logFile, "wg setconf failed")
            writeStatus(vpnDir, "ERROR: wg setconf failed")
            process.destroyForcibly()
            return
        }
        logLine(logFile, "wg setconf ok")

        configureNetwork(parsed, vpnDir, logFile)
        writeStatus(vpnDir, "ACTIVE")
        logLine(logFile, "tunnel ACTIVE")

        val stopMarker = File(vpnDir, "stop.marker")
        while (!stopMarker.exists() && File(confPath).exists()) {
            Thread.sleep(500)
        }

        process.destroy()
        process.waitFor(5, TimeUnit.SECONDS)
        writeStatus(vpnDir, "STOPPED")
        logLine(logFile, "helper stopped")
    } catch (e: Exception) {
        logLine(logFile, "helper error: ${e.message}")
        writeStatus(vpnDir, "ERROR: ${e.message}")
    }
}

private const val INTERFACE_NAME = "nuvio0"

private fun configureNetwork(config: ParsedTunnelConfig, vpnDir: File, logFile: File) {
    val name = INTERFACE_NAME
    config.address4?.let { address ->
        val mask = cidrToMask(config.address4Plen ?: 32)
        withRetry(attempts = 30, delayMs = 500) {
            runProcess("netsh", "interface", "ipv4", "add", "address", "name=$name", "address=$address", "mask=$mask") == 0
        }
    }
    config.address6?.let { address ->
        withRetry(attempts = 30, delayMs = 500) {
            runProcess("netsh", "interface", "ipv6", "add", "address", "interface=$name", "address=$address") == 0
        }
    }
    config.routes4.forEach { (network, prefix) ->
        withRetry(attempts = 30, delayMs = 500) {
            runProcess("netsh", "interface", "ipv4", "add", "route", "$network/$prefix", "interface=$name", "nexthop=0.0.0.0") == 0
        }
    }
    config.routes6.forEach { (network, prefix) ->
        withRetry(attempts = 30, delayMs = 500) {
            runProcess("netsh", "interface", "ipv6", "add", "route", "$network/$prefix", "interface=$name") == 0
        }
    }
    config.dns.firstOrNull()?.let { primary ->
        withRetry(attempts = 30, delayMs = 500) {
            runProcess("netsh", "interface", "ipv4", "set", "dnsservers", "name=$name", "static", primary, "primary") == 0
        }
    }
    logLine(logFile, "network configured (routes=${config.routes4.size + config.routes6.size}, dns=${config.dns.size})")
}

private data class ParsedTunnelConfig(
    val confText: String,
    val address4: String?,
    val address4Plen: Int?,
    val address6: String?,
    val dns: List<String>,
    val routes4: List<Pair<String, Int>>,
    val routes6: List<Pair<String, Int>>,
)

private val SECTION_REGEX = Regex("""^\s*\[([^\]]+)]\s*$""")
private val KEY_VALUE_REGEX = Regex("""^\s*([A-Za-z0-9]+)\s*=\s*(.+?)\s*$""")
private val IPV4_REGEX = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

private fun sanitizeConfig(text: String): ParsedTunnelConfig {
    val interfaceKeys = mutableMapOf<String, String>()
    val peerKeys = LinkedHashMap<String, MutableList<String>>()
    var section = ""
    var address4: String? = null
    var address4Plen: Int? = null
    var address6: String? = null
    val dns = mutableListOf<String>()
    val routes4 = mutableListOf<Pair<String, Int>>()
    val routes6 = mutableListOf<Pair<String, Int>>()

    for (rawLine in text.lines()) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) continue
        SECTION_REGEX.matchEntire(line)?.let { match ->
            section = match.groupValues[1].trim().lowercase(Locale.ROOT)
            continue
        }
        val keyValue = KEY_VALUE_REGEX.matchEntire(line) ?: continue
        val key = keyValue.groupValues[1].lowercase(Locale.ROOT)
        val value = keyValue.groupValues[2].trim()
        when (section) {
            "interface" -> when (key) {
                "privatekey" -> interfaceKeys["private_key"] = value
                "listenport" -> interfaceKeys["listen_port"] = value
                "fwmark" -> interfaceKeys["fwmark"] = value
                "address" -> value.split(',').forEach { entry ->
                    val cidr = parseCidr(entry.trim())
                    if (cidr != null) {
                        if (cidr.first.contains(':') && address6 == null) {
                            address6 = cidr.first
                        } else if (!cidr.first.contains(':') && address4 == null) {
                            address4 = cidr.first
                            address4Plen = cidr.second
                        }
                    }
                }
                "dns" -> value.split(',')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { dns += it }
            }
            "peer" -> when (key) {
                "publickey" -> peerKeys.getOrPut("public_key") { mutableListOf() }.add(value)
                "presharedkey" -> peerKeys.getOrPut("preshared_key") { mutableListOf() }.add(value)
                "endpoint" -> peerKeys.getOrPut("endpoint") { mutableListOf() }.add(value)
                "persistentkeepalive" -> peerKeys.getOrPut("persistent_keepalive") { mutableListOf() }.add(value)
                "allowedips" -> value.split(',').forEach { entry ->
                    val cidr = parseCidr(entry.trim())
                    if (cidr != null) {
                        if (cidr.first.contains(':')) routes6 += cidr else routes4 += cidr
                    }
                }
            }
        }
    }

    val builder = StringBuilder()
    builder.append("[Interface]\n")
    interfaceKeys.forEach { (key, value) -> builder.append("$key = $value\n") }
    builder.append("\n")
    peerKeys.forEach { (key, values) ->
        builder.append("[Peer]\n")
        values.forEach { value -> builder.append("$key = $value\n") }
        routes4.forEach { (network, prefix) -> builder.append("allowed_ips = $network/$prefix\n") }
        routes6.forEach { (network, prefix) -> builder.append("allowed_ips = $network/$prefix\n") }
        builder.append("\n")
    }

    return ParsedTunnelConfig(
        confText = builder.toString(),
        address4 = address4,
        address4Plen = address4Plen,
        address6 = address6,
        dns = dns,
        routes4 = routes4,
        routes6 = routes6,
    )
}

private fun parseCidr(entry: String): Pair<String, Int>? {
    if (entry.isBlank()) return null
    val parts = entry.split('/')
    val address = parts[0].trim()
    if (!IPV4_REGEX.matches(address) && !address.contains(':')) return null
    val defaultPrefix = if (address.contains(':')) 128 else 32
    val prefix = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: defaultPrefix
    if (prefix !in 0..128) return null
    if (parts.size > 1 && parts[1].trim().toIntOrNull() == null) return null
    return address to prefix
}

private fun cidrToMask(prefix: Int): String {
    val bits = IntArray(4)
    var remaining = prefix
    for (i in 0..3) {
        when {
            remaining >= 8 -> {
                bits[i] = 255
                remaining -= 8
            }
            remaining > 0 -> {
                bits[i] = 0xFF shl (8 - remaining) and 0xFF
                remaining = 0
            }
        }
    }
    return bits.joinToString(".")
}

private fun writeStatus(vpnDir: File, status: String) {
    File(vpnDir, "status.txt").writeText(status)
}

private fun logLine(logFile: File, message: String) {
    runCatching {
        logFile.appendText("[${System.currentTimeMillis()}] $message\n")
    }
}

private fun extractIfMissing(name: String, dir: File) {
    val target = File(dir, name)
    if (target.exists() && target.length() > 0L) return
    val resource = "/native/windows/$name"
    val input = VpnController::class.java.getResourceAsStream(resource)
        ?: throw IllegalStateException("missing bundled resource $resource")
    target.parentFile?.mkdirs()
    input.use { source ->
        FileOutputStream(target).use { output ->
            source.copyTo(output)
        }
    }
    target.setExecutable(true, false)
}

private fun runProcess(vararg command: String, timeoutMs: Long = 30_000): Int {
    return try {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        if (process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.exitValue()
        } else {
            process.destroyForcibly()
            -1
        }
    } catch (e: Exception) {
        -1
    }
}

private fun withRetry(attempts: Int, delayMs: Long, block: () -> Boolean): Boolean {
    repeat(attempts) {
        if (block()) return true
        Thread.sleep(delayMs)
    }
    return false
}
