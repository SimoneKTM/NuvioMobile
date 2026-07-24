package com.nuvio.app.features.sora

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal object SoraRuntimeAdapter {
    private val log = Logger.withTag("SoraRuntimeAdapter")

    private var scriptExecutor: (suspend (String, String) -> String?)? = null

    fun registerExecutor(executor: suspend (String, String) -> String?) {
        scriptExecutor = executor
    }

    fun isAvailable(): Boolean = scriptExecutor != null

    private suspend fun executeScript(code: String, fn: String): String? {
        return scriptExecutor?.invoke(code, fn)
    }

    suspend fun executeSoraModule(
        module: SoraModule,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<com.nuvio.app.features.plugins.PluginRuntimeResult> = withContext(Dispatchers.Default) {
        if (scriptExecutor == null) {
            log.w { "Sora runtime not available (scriptExecutor not registered)" }
            return@withContext emptyList()
        }

        try {
            val manifest = module.manifest
            val scriptCode = module.scriptCode
            val searchKeyword = resolveMediaTitle(tmdbId, mediaType) ?: return@withContext emptyList()
            val mode = resolveMode(manifest)

            val searchResultsJson = executeSearch(mode, scriptCode, manifest, searchKeyword) ?: return@withContext emptyList()
            val searchResults = SoraModuleParser.parseSearchResults(searchResultsJson)
            if (searchResults.isEmpty()) return@withContext emptyList()

            val firstResult = searchResults.first()
            val detailUrl = firstResult.href

            if (mediaType == "movie") {
                val streamUrl = executeMovieStream(mode, scriptCode, manifest, detailUrl) ?: return@withContext emptyList()
                listOf(
                    com.nuvio.app.features.plugins.PluginRuntimeResult(
                        title = firstResult.title,
                        name = module.manifest.sourceName,
                        url = streamUrl,
                        quality = manifest.quality,
                        language = manifest.language,
                        provider = module.manifest.sourceName,
                        type = manifest.streamType,
                    )
                )
            } else {
                val episodesJson = executeEpisodes(mode, scriptCode, manifest, detailUrl) ?: return@withContext emptyList()
                val episodes = SoraModuleParser.parseEpisodes(episodesJson)
                if (episodes.isEmpty()) return@withContext emptyList()

                val targetEpisode = episodes.find { ep ->
                    ep.number.toIntOrNull() == episode
                } ?: episodes.firstOrNull() ?: return@withContext emptyList()

                val episodeUrl = targetEpisode.href
                val streamUrl = executeStreamUrl(mode, scriptCode, manifest, episodeUrl) ?: return@withContext emptyList()

                val subtitleResult = if (manifest.softsub) {
                    executeSoftSub(scriptCode, manifest, episodeUrl)
                } else null

                listOf(
                    com.nuvio.app.features.plugins.PluginRuntimeResult(
                        title = "${firstResult.title} - Ep. ${targetEpisode.number}",
                        name = module.manifest.sourceName,
                        url = streamUrl,
                        quality = manifest.quality,
                        language = manifest.language,
                        provider = module.manifest.sourceName,
                        type = manifest.streamType,
                        subtitles = subtitleResult?.subtitles?.let { subUrl ->
                            listOf(com.nuvio.app.features.plugins.PluginSubtitleResult(url = subUrl, language = "English"))
                        },
                    )
                )
            }
        } catch (e: Exception) {
            log.e(e) { "Sora module execution failed: ${module.id}" }
            emptyList()
        }
    }

    private suspend fun executeSearch(
        mode: SoraExecutionMode,
        code: String,
        manifest: SoraManifest,
        keyword: String,
    ): String? {
        return when (mode) {
            SoraExecutionMode.ASYNC, SoraExecutionMode.STREAM_ASYNC -> {
                val wrappedCode = wrapAsyncSearchCode(code, keyword, manifest.searchBaseUrl)
                executeScript(wrappedCode, "searchResults")
            }
            SoraExecutionMode.NORMAL -> {
                val searchUrl = manifest.searchBaseUrl.replace("%s", keyword)
                val html = httpGetText(searchUrl)
                val wrappedCode = wrapNormalSearchCode(code, html)
                executeScript(wrappedCode, "searchResults")
            }
        }
    }

    private suspend fun executeMovieStream(
        mode: SoraExecutionMode,
        code: String,
        manifest: SoraManifest,
        url: String,
    ): String? {
        return when (mode) {
            SoraExecutionMode.ASYNC, SoraExecutionMode.STREAM_ASYNC -> {
                val wrappedCode = wrapAsyncStreamCode(code, url)
                val result = executeScript(wrappedCode, "extractStreamUrl")
                result?.let { parseStreamString(it) }
            }
            SoraExecutionMode.NORMAL -> {
                val html = httpGetText(url)
                val wrappedCode = wrapNormalStreamCode(code, html)
                val result = executeScript(wrappedCode, "extractStreamUrl")
                result?.let { parseStreamString(it) }
            }
        }
    }

    private suspend fun executeEpisodes(
        mode: SoraExecutionMode,
        code: String,
        manifest: SoraManifest,
        url: String,
    ): String? {
        return when (mode) {
            SoraExecutionMode.ASYNC, SoraExecutionMode.STREAM_ASYNC -> {
                val wrappedCode = wrapAsyncEpisodesCode(code, url)
                executeScript(wrappedCode, "extractEpisodes")
            }
            SoraExecutionMode.NORMAL -> {
                val html = httpGetText(url)
                val wrappedCode = wrapNormalEpisodesCode(code, html)
                executeScript(wrappedCode, "extractEpisodes")
            }
        }
    }

    private suspend fun executeStreamUrl(
        mode: SoraExecutionMode,
        code: String,
        manifest: SoraManifest,
        url: String,
    ): String? {
        return when (mode) {
            SoraExecutionMode.ASYNC, SoraExecutionMode.STREAM_ASYNC -> {
                val wrappedCode = wrapAsyncStreamCode(code, url)
                executeScript(wrappedCode, "extractStreamUrl")
            }
            SoraExecutionMode.NORMAL -> {
                val html = httpGetText(url)
                val wrappedCode = wrapNormalStreamCode(code, html)
                executeScript(wrappedCode, "extractStreamUrl")
            }
        }
    }

    private suspend fun executeSoftSub(
        code: String,
        manifest: SoraManifest,
        url: String,
    ): SoraStreamResult? {
        if (!manifest.softsub) return null
        val hasAsync = manifest.asyncJS || manifest.streamAsyncJS
        val wrappedCode = if (hasAsync) {
            wrapAsyncSoftSubCode(code, url)
        } else {
            val html = httpGetText(url)
            wrapNormalSoftSubCode(code, html)
        }
        val result = executeScript(wrappedCode, "extractStreamUrl") ?: return null
        return SoraModuleParser.parseStreamResult(result)
    }

    private fun parseStreamString(result: String): String {
        val trimmed = result.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val parsed = SoraModuleParser.parseStreamResult(trimmed)
            return parsed.stream ?: trimmed
        }
        return trimmed
    }

    private fun resolveMode(manifest: SoraManifest): SoraExecutionMode {
        return when {
            manifest.asyncJS -> SoraExecutionMode.ASYNC
            manifest.streamAsyncJS -> SoraExecutionMode.STREAM_ASYNC
            else -> SoraExecutionMode.NORMAL
        }
    }

    private fun wrapNormalSearchCode(code: String, html: String): String {
        val escapedHtml = html.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.searchResults !== 'undefined') ? module.exports.searchResults : (globalThis.searchResults || null);
                if (typeof fn !== 'function') {
                    JSON.stringify([]);
                    return;
                }
                var html = `$escapedHtml`;
                var result = fn(html);
                return JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapAsyncSearchCode(code: String, keyword: String, searchBaseUrl: String): String {
        val escapedKeyword = keyword.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        val escapedBaseUrl = searchBaseUrl.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (async function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.searchResults !== 'undefined') ? module.exports.searchResults : (globalThis.searchResults || null);
                if (typeof fn !== 'function') {
                    return JSON.stringify([]);
                }
                var result = await fn("$escapedKeyword");
                return typeof result === 'string' ? result : JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapNormalDetailsCode(code: String, html: String): String {
        val escapedHtml = html.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.extractDetails !== 'undefined') ? module.exports.extractDetails : (globalThis.extractDetails || null);
                if (typeof fn !== 'function') return JSON.stringify([]);
                var html = `$escapedHtml`;
                var result = fn(html);
                return JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapNormalEpisodesCode(code: String, html: String): String {
        val escapedHtml = html.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.extractEpisodes !== 'undefined') ? module.exports.extractEpisodes : (globalThis.extractEpisodes || null);
                if (typeof fn !== 'function') return JSON.stringify([]);
                var html = `$escapedHtml`;
                var result = fn(html);
                return JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapAsyncEpisodesCode(code: String, url: String): String {
        val escapedUrl = url.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (async function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.extractEpisodes !== 'undefined') ? module.exports.extractEpisodes : (globalThis.extractEpisodes || null);
                if (typeof fn !== 'function') return JSON.stringify([]);
                var result = await fn("$escapedUrl");
                return typeof result === 'string' ? result : JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapNormalStreamCode(code: String, html: String): String {
        val escapedHtml = html.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.extractStreamUrl !== 'undefined') ? module.exports.extractStreamUrl : (globalThis.extractStreamUrl || null);
                if (typeof fn !== 'function') return null;
                var html = `$escapedHtml`;
                var result = fn(html);
                return typeof result === 'string' ? result : JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapAsyncStreamCode(code: String, url: String): String {
        val escapedUrl = url.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (async function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.extractStreamUrl !== 'undefined') ? module.exports.extractStreamUrl : (globalThis.extractStreamUrl || null);
                if (typeof fn !== 'function') return null;
                var result = await fn("$escapedUrl");
                return typeof result === 'string' ? result : JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapNormalSoftSubCode(code: String, html: String): String {
        val escapedHtml = html.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.extractStreamUrl !== 'undefined') ? module.exports.extractStreamUrl : (globalThis.extractStreamUrl || null);
                if (typeof fn !== 'function') return JSON.stringify({stream: null, subtitles: null});
                var html = `$escapedHtml`;
                var result = fn(html);
                return JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private fun wrapAsyncSoftSubCode(code: String, url: String): String {
        val escapedUrl = url.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        return """
            (async function() {
                var module = { exports: {} };
                var exports = module.exports;
                (function() {
                    $code
                })();
                var fn = (typeof module.exports.extractStreamUrl !== 'undefined') ? module.exports.extractStreamUrl : (globalThis.extractStreamUrl || null);
                if (typeof fn !== 'function') return JSON.stringify({stream: null, subtitles: null});
                var result = await fn("$escapedUrl");
                return typeof result === 'string' ? result : JSON.stringify(result);
            })()
        """.trimIndent()
    }

    private suspend fun resolveMediaTitle(tmdbId: String, mediaType: String): String? {
        val cleanId = tmdbId.removePrefix("tmdb:").split(":").first().trim()
        if (cleanId.isBlank()) return null
        if (!cleanId.all { it.isDigit() }) return null

        try {
            val tmdbUrl = buildTmdbItemUrl(cleanId, mediaType)
            val response = httpGetText(tmdbUrl)
            val json = Json { ignoreUnknownKeys = true }
            val obj = json.parseToJsonElement(response).jsonObject
            val title = obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
            return title
        } catch (e: Exception) {
            log.e(e) { "Failed to resolve media title for tmdbId=$tmdbId" }
            return null
        }
    }

    private fun buildTmdbItemUrl(tmdbId: String, mediaType: String): String {
        val type = when {
            mediaType == "tv" -> "tv"
            else -> "movie"
        }
        val apiKey = TmdbSettingsRepository.snapshot().apiKey
        return "https://api.themoviedb.org/3/$type/$tmdbId?api_key=$apiKey&language=en"
    }
}

internal enum class SoraExecutionMode {
    NORMAL,
    ASYNC,
    STREAM_ASYNC,
}
