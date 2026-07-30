package com.nuvio.app.features.vezie

import co.touchlab.kermit.Logger
import com.nuvio.app.core.network.CloudflareSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object VeezieHostResolver {
    private val log = Logger.withTag("VeezieHostResolver")

    suspend fun resolveViaCloudflare(videoPageUrl: String): String? {
        log.d { "resolveViaCloudflare: $videoPageUrl" }
        return try {
            val result = CloudflareSolver.scrapePageWithIframeFollow(videoPageUrl)
            if (result != null && result.videoUrls.isNotEmpty()) {
                result.videoUrls.first()
            } else {
                null
            }
        } catch (e: Exception) {
            log.e(e) { "resolveViaCloudflare failed for $videoPageUrl" }
            null
        }
    }

    suspend fun resolveStreamUrl(
        videoUrl: String,
        title: String? = null,
    ): String? = withContext(Dispatchers.Default) {
        try {
            when {
                videoUrl.endsWith(".mp4") || videoUrl.endsWith(".m3u8") -> videoUrl
                videoUrl.contains("mixdrop") -> resolveMixdrop(videoUrl)
                videoUrl.contains("supervideo") -> resolveSupervideo(videoUrl)
                videoUrl.contains("voe.sx") -> resolveVoeSx(videoUrl)
                videoUrl.contains("streamtape") -> resolveStreamtape(videoUrl)
                videoUrl.contains("doodstream") || videoUrl.contains("dood.") -> resolveDoodStream(videoUrl)
                videoUrl.contains("uqload") -> resolveUqload(videoUrl)
                videoUrl.contains("vidmoly") -> resolveVidmoly(videoUrl)
                videoUrl.contains("clipwatching") || videoUrl.contains("cloudvideo") || videoUrl.contains("cloudstream") -> resolveClipWatching(videoUrl)
                videoUrl.contains("filemoon") || videoUrl.contains("file-moon") -> resolveFileMoon(videoUrl)
                videoUrl.contains("streamwish") || videoUrl.contains("wish") -> resolveStreamWish(videoUrl)
                videoUrl.contains("mp4upload") -> resolveMp4Upload(videoUrl)
                videoUrl.contains("speedostream") || videoUrl.contains("speedo") -> resolveSpeedoStream(videoUrl)
                videoUrl.contains("kwik") || videoUrl.contains("kwik.cx") -> resolveKwik(videoUrl)
                videoUrl.contains("mystream") || videoUrl.contains("mystream.") -> resolveMystream(videoUrl)
                videoUrl.contains("mangoplayer") || videoUrl.contains("mango.") -> resolveMangoplayer(videoUrl)
                videoUrl.contains("embedsito") -> resolveEmbedsito(videoUrl)
                videoUrl.contains("dailymotion") -> resolveDailymotion(videoUrl)
                videoUrl.contains("youtube") || videoUrl.contains("youtu.be") -> resolveYouTube(videoUrl)
                videoUrl.contains("yourupload") -> resolveYourUpload(videoUrl)
                videoUrl.contains("uptobox") -> resolveUpToBox(videoUrl)
                videoUrl.contains("streamhub") || videoUrl.contains("hub.") -> resolveStreamHub(videoUrl)
                videoUrl.contains("streamlocker") -> resolveStreamLocker(videoUrl)
                videoUrl.contains("vixcloud") -> resolveVixcloudDirect(videoUrl)
                videoUrl.contains("fileupload") || videoUrl.contains("upload") -> resolveGenericUpload(videoUrl)
                else -> resolveGeneric(videoUrl)
            }

            null
        } catch (e: Exception) {
            log.e(e) { "Failed to resolve: $videoUrl" }
            null
        }
    }

    private suspend fun resolveMixdrop(url: String): String? {
        val embedUrl = url.replace("/v/", "/e/").let {
            if (it.startsWith("//")) "https:$it" else if (!it.startsWith("http")) "https:$it" else it
        }
        val html = VeezieEasyProxy.httpGetViaProxy(embedUrl)
        val unpacked = unpackPacker(html) ?: return null
        val wurlRegex = Regex("""wurl\s*=\s*["']([^"']+)["']""")
        val match = wurlRegex.find(unpacked) ?: return null
        var directUrl = match.groupValues[1]
        if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
        return directUrl
    }

    private suspend fun resolveSupervideo(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val unpacked = unpackPacker(html) ?: html
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(unpacked)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        return null
    }

    private suspend fun resolveVoeSx(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regex = Regex("""(?:let|const|var)\s+url\s*=\s*["']([^"']+)["']""")
        val match = regex.find(html) ?: return null
        var directUrl = match.groupValues[1]
        if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
        return directUrl
    }

    private suspend fun resolveStreamtape(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regex = Regex("""innerHTML\s*=\s*["'][^"']*\/\/[^"']*videowood[^"']*["']""")
        val match = regex.find(html)
        if (match != null) {
            val linkRegex = Regex("""https?://[^"']+\.(?:mp4|m3u8)[^"']*""")
            val linkMatch = linkRegex.find(match.value)
            if (linkMatch != null) return linkMatch.value
        }
        val redirectRegex = Regex("""<a\s+href\s*=\s*["']([^"']+)["'][^>]*id\s*=\s*["']link["']""", RegexOption.IGNORE_CASE)
        val redirectMatch = redirectRegex.find(html)
        if (redirectMatch != null) {
            var redirectUrl = redirectMatch.groupValues[1]
            if (!redirectUrl.startsWith("http")) redirectUrl = "https:$redirectUrl"
            return resolveStreamUrl(redirectUrl)
        }
        return null
    }

    private suspend fun resolveDoodStream(url: String): String? {
        val embedUrl = url.replace("/d/", "/e/").let {
            if (!it.startsWith("http")) "https:$it" else it
        }
        val html = VeezieEasyProxy.httpGetViaProxy(embedUrl)
        val passMd5Regex = Regex("""/pass_md5/[^"']+""")
        val passMatch = passMd5Regex.find(html)?.value
        if (passMatch != null) {
            val baseHost = embedUrl.substringAfter("://").substringBefore("/")
            val passUrl = "https://$baseHost$passMatch"
            try {
                val passResult = VeezieEasyProxy.httpGetViaProxy(passUrl)
                val directRegex = Regex("""https?://[^"']+\.(?:mp4|m3u8)[^"']*""")
                val directMatch = directRegex.find(passResult)
                if (directMatch != null) return directMatch.value
            } catch (_: Exception) {}
        }
        val dsRegex = Regex("""ds\s*\(\s*["']([^"']+)["']\s*\)""")
        val dsMatch = dsRegex.find(html)
        if (dsMatch != null) {
            var tokenUrl = dsMatch.groupValues[1]
            if (!tokenUrl.startsWith("http")) {
                val doodHost = embedUrl.substringAfter("://").substringBefore("/")
                tokenUrl = "https://$doodHost$tokenUrl"
            }
            try {
                val tokenResult = VeezieEasyProxy.httpGetViaProxy(tokenUrl)
                val directRegex = Regex("""https?://[^"']+\.(?:mp4|m3u8)[^"']*""")
                val directMatch = directRegex.find(tokenResult)
                if (directMatch != null) return directMatch.value
            } catch (_: Exception) {}
        }
        val directLinkRegex = Regex("""https?://[^"']*doodstream[^"']*\.(?:mp4|m3u8)[^"']*""")
        val directMatch = directLinkRegex.find(html)
        if (directMatch != null) return directMatch.value
        return null
    }

    private suspend fun resolveUqload(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val iframeRegex = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        for (iframeMatch in iframeRegex.findAll(html)) {
            var iframeSrc = iframeMatch.groupValues[1]
            if (!iframeSrc.startsWith("http")) iframeSrc = "https:$iframeSrc"
            val iframeHtml = VeezieEasyProxy.httpGetViaProxy(iframeSrc)
            val videoRegex = Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""")
            val videoMatch = videoRegex.find(iframeHtml)
            if (videoMatch != null) {
                var videoUrl = videoMatch.groupValues[1]
                if (videoUrl.startsWith("//")) videoUrl = "https:$videoUrl"
                return videoUrl
            }
        }
        return null
    }

    private suspend fun resolveVidmoly(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val scriptRegex = Regex(
            """<script[^>]*>([\s\S]*?window\.video[\s\S]*?)</script>""",
            RegexOption.IGNORE_CASE,
        )
        val scriptMatch = scriptRegex.find(html)
        if (scriptMatch != null) {
            val script = scriptMatch.groupValues[1]
            val filePatterns = listOf(
                Regex("""file:\s*"([^"]+\.(?:mp4|m3u8)[^"]*)"""),
                Regex("""file:\s*'([^']+\.(?:mp4|m3u8)[^']*)'"""),
                Regex(""""file"\s*:\s*"([^"]+\.(?:mp4|m3u8)[^"]*)"""),
            )
            for (pattern in filePatterns) {
                val match = pattern.find(script)
                if (match != null) {
                    var directUrl = match.groupValues[1]
                    if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                    return directUrl
                }
            }
        }
        val anyFileRegex = Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""")
        val anyMatch = anyFileRegex.find(html)
        if (anyMatch != null) {
            var directUrl = anyMatch.groupValues[1]
            if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
            return directUrl
        }
        return null
    }

    private suspend fun resolveClipWatching(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val iframeRegex = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        for (iframeMatch in iframeRegex.findAll(html)) {
            var iframeSrc = iframeMatch.groupValues[1]
            if (!iframeSrc.startsWith("http")) iframeSrc = "https:$iframeSrc"
            val iframeHtml = VeezieEasyProxy.httpGetViaProxy(iframeSrc)
            val videoRegex = Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""")
            val videoMatch = videoRegex.find(iframeHtml)
            if (videoMatch != null) {
                var videoUrl = videoMatch.groupValues[1]
                if (videoUrl.startsWith("//")) videoUrl = "https:$videoUrl"
                return videoUrl
            }
        }
        return null
    }

    private suspend fun resolveFileMoon(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val unpacked = unpackPacker(html) ?: html
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(unpacked)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val postUrlRegex = Regex("""<form[^>]*action\s*=\s*["']([^"']+)["'][^>]*method\s*=\s*["']post["']""", RegexOption.IGNORE_CASE)
        val postMatch = postUrlRegex.find(html)
        if (postMatch != null) {
            var postUrl = postMatch.groupValues[1]
            if (!postUrl.startsWith("http")) postUrl = normalizeUrl(postUrl, url)
            val postHtml = VeezieEasyProxy.httpGetViaProxy(postUrl)
            val unpackedPost = unpackPacker(postHtml) ?: postHtml
            for (regex in regexes) {
                val match = regex.find(unpackedPost)
                if (match != null) {
                    var directUrl = match.groupValues[1]
                    if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                    return directUrl
                }
            }
        }
        return null
    }

    private suspend fun resolveStreamWish(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val unpacked = unpackPacker(html) ?: html
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(unpacked)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val iframeRegex = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        for (iframeMatch in iframeRegex.findAll(html)) {
            var iframeSrc = iframeMatch.groupValues[1]
            if (!iframeSrc.startsWith("http")) iframeSrc = "https:$iframeSrc"
            val iframeHtml = VeezieEasyProxy.httpGetViaProxy(iframeSrc)
            val unpackedIframe = unpackPacker(iframeHtml) ?: iframeHtml
            for (regex in regexes) {
                val match = regex.find(unpackedIframe)
                if (match != null) {
                    var directUrl = match.groupValues[1]
                    if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                    return directUrl
                }
            }
        }
        return null
    }

    private suspend fun resolveMp4Upload(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val downloadLinkRegex = Regex("""<a\s+href\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)
        val downloadMatch = downloadLinkRegex.find(html)
        if (downloadMatch != null) {
            var directUrl = downloadMatch.groupValues[1]
            if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
            return directUrl
        }
        return null
    }

    private suspend fun resolveSpeedoStream(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val unpacked = unpackPacker(html) ?: html
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(unpacked)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val videoJsRegex = Regex("""<script[^>]*src\s*=\s*["'][^"']*video[^"']*\.js["']""", RegexOption.IGNORE_CASE)
        val videoJsMatch = videoJsRegex.find(html)
        if (videoJsMatch != null) {
            val linkRegex = Regex("""https?://[^"']+\.(?:mp4|m3u8)[^"']*""")
            val linkMatch = linkRegex.find(html)
            if (linkMatch != null) return linkMatch.value
        }
        return null
    }

    private suspend fun resolveKwik(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val postMessageRegex = Regex("""postMessage\s*\(\s*["']([^"']+)["']""")
        val postMatch = postMessageRegex.find(html)
        if (postMatch != null) {
            var postUrl = postMatch.groupValues[1]
            if (!postUrl.startsWith("http")) postUrl = normalizeUrl(postUrl, url)
            return postUrl
        }
        return null
    }

    private suspend fun resolveMystream(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val scriptRegex = Regex(
            """<script[^>]*>([\s\S]*?player[\s\S]*?source[\s\S]*?)</script>""",
            RegexOption.IGNORE_CASE,
        )
        val scriptMatch = scriptRegex.find(html)
        val searchHtml = if (scriptMatch != null) scriptMatch.groupValues[1] else html
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex(""""url"\s*:\s*"([^"]+\.(?:mp4|m3u8)[^"]*)"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(searchHtml)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        return null
    }

    private suspend fun resolveMangoplayer(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val hlsRegex = Regex("""https?://[^"']+\.m3u8[^"']*""")
        val hlsMatch = hlsRegex.find(html)
        if (hlsMatch != null) return hlsMatch.value
        return null
    }

    private suspend fun resolveEmbedsito(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val iframeRegex = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        for (iframeMatch in iframeRegex.findAll(html)) {
            var iframeSrc = iframeMatch.groupValues[1]
            if (!iframeSrc.startsWith("http")) iframeSrc = "https:$iframeSrc"
            val result = resolveStreamUrl(iframeSrc)
            if (result != null) return result
        }
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        return null
    }

    private suspend fun resolveDailymotion(url: String): String? {
        val videoIdRegex = Regex("""(?:dailymotion\.com/video/|dai\.ly/|dailymotion\.com/embed/video/)([a-zA-Z0-9]+)""")
        val videoId = videoIdRegex.find(url)?.groupValues?.getOrNull(1)
        if (videoId != null) {
            val apiUrl = "https://www.dailymotion.com/player/metadata/video/$videoId"
            try {
                val apiResult = VeezieEasyProxy.httpGetViaProxy(apiUrl)
                val qualitiesRegex = Regex(""""url"\s*:\s*"([^"]+\.(?:mp4|m3u8)[^"]*)"""")
                val qualitiesMatch = qualitiesRegex.find(apiResult)
                if (qualitiesMatch != null) {
                    var directUrl = qualitiesMatch.groupValues[1]
                    directUrl = directUrl.replace("\\/", "/").replace("\\u002F", "/")
                    return directUrl
                }
            } catch (_: Exception) {}
        }
        val embedUrl = if (videoId != null) "https://www.dailymotion.com/embed/video/$videoId" else url
        val html = VeezieEasyProxy.httpGetViaProxy(embedUrl)
        val hlsRegex = Regex("""https?://[^"']+\.m3u8[^"']*""")
        val hlsMatch = hlsRegex.find(html)
        return hlsMatch?.value
    }

    private suspend fun resolveYouTube(url: String): String? {
        val videoIdRegex = Regex("""(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/)([a-zA-Z0-9_-]{11})""")
        val videoId = videoIdRegex.find(url)?.groupValues?.getOrNull(1)
        if (videoId != null) {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            try {
                val oembedResult = VeezieEasyProxy.httpGetViaProxy(oembedUrl)
                val htmlUrl = oembedResult
                if (htmlUrl.isNotBlank()) {
                    return url
                }
            } catch (_: Exception) {}
        }
        return url
    }

    private suspend fun resolveYourUpload(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val downloadLinkRegex = Regex("""<a\s+href\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)
        val downloadMatch = downloadLinkRegex.find(html)
        if (downloadMatch != null) {
            var directUrl = downloadMatch.groupValues[1]
            if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
            return directUrl
        }
        return null
    }

    private suspend fun resolveUpToBox(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<a\s+href\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        val downloadFormRegex = Regex("""<form[^>]*action\s*=\s*["']([^"']+)["'][^>]*>[\s\S]*?download""", RegexOption.IGNORE_CASE)
        val formMatch = downloadFormRegex.find(html)
        if (formMatch != null) {
            var formAction = formMatch.groupValues[1]
            if (!formAction.startsWith("http")) formAction = normalizeUrl(formAction, url)
            val formHtml = VeezieEasyProxy.httpGetViaProxy(formAction)
            val directRegex = Regex("""https?://[^"']+\.(?:mp4|m3u8)[^"']*""")
            val directMatch = directRegex.find(formHtml)
            if (directMatch != null) return directMatch.value
        }
        return null
    }

    private suspend fun resolveStreamHub(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val unpacked = unpackPacker(html) ?: html
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(unpacked)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        return null
    }

    private suspend fun resolveStreamLocker(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        return null
    }

    private suspend fun resolveVixcloudDirect(url: String): String? {
        val local = VixcloudExtractor.extractPlaylistUrl(url)
        if (local != null) return local
        val remote = VeezieEasyProxy.extractHostUrl("vixcloud", url)
        if (remote != null) return remote
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        return null
    }

    private suspend fun resolveGenericUpload(url: String): String? {
        val html = VeezieEasyProxy.httpGetViaProxy(url)
        val regexes = listOf(
            Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            Regex("""<a\s+href\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE),
        )
        for (regex in regexes) {
            val match = regex.find(html)
            if (match != null) {
                var directUrl = match.groupValues[1]
                if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                return directUrl
            }
        }
        return null
    }

    private suspend fun resolveGeneric(url: String): String? {
        return try {
            val html = VeezieEasyProxy.httpGetViaProxy(url)
            val unpacked = unpackPacker(html) ?: html
            val regexes = listOf(
                Regex("""wurl\s*=\s*["']([^"']+)["']"""),
                Regex("""(?:file|src):\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
                Regex("""<source\s+src\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
                Regex("""["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
            )
            for (regex in regexes) {
                val match = regex.find(unpacked)
                if (match != null) {
                    var directUrl = match.groupValues[1]
                    if (directUrl.startsWith("//")) directUrl = "https:$directUrl"
                    return directUrl
                }
            }
            val iframeRegex = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            for (iframeMatch in iframeRegex.findAll(html)) {
                var iframeSrc = iframeMatch.groupValues[1]
                if (iframeSrc.startsWith("//")) iframeSrc = "https:$iframeSrc"
                if (!iframeSrc.startsWith("http")) iframeSrc = normalizeUrl(iframeSrc, url)
                val result = resolveStreamUrl(iframeSrc)
                if (result != null) return result
            }
            null
        } catch (_: Exception) { null }
    }

    private fun unpackPacker(html: String): String? {
        val packerRegex = Regex(
            """eval\(function\(p,a,c,k,e,d\)\{.*?return p\}\((.+?)\)\)""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val match = packerRegex.find(html) ?: return null
        val raw = match.groupValues[1]
        return decodePacker(raw)
    }

    private fun decodePacker(raw: String): String? {
        try {
            val argsRegex = Regex(
                """['"](.*?)['"]\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*['"](.*?)['"]\.split""",
            )
            val match = argsRegex.find(raw) ?: return null
            var p = match.groupValues[1]
            val a = match.groupValues[2].toIntOrNull() ?: 62
            val c = match.groupValues[3].toIntOrNull() ?: 0
            val k = match.groupValues[4].split("|")
            for (i in 0 until c.coerceAtMost(k.size)) {
                if (k[i].isNotEmpty()) {
                    p = p.replace(Regex("\\b${i.toString(a)}\\b"), k[i])
                }
            }
            return p
        } catch (_: Exception) { return null }
    }

    private fun extractHost(url: String): String {
        return url.substringAfter("://").substringBefore("/").substringBefore(":")
    }

    private fun normalizeUrl(href: String, baseUrl: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        val base = baseUrl.trimEnd('/')
        return when {
            href.startsWith("/") -> "${base.substringBefore("://").let { proto -> "$proto://${base.substringAfter("://").substringBefore("/")}" }}$href"
            else -> "$base/$href"
        }
    }
}
