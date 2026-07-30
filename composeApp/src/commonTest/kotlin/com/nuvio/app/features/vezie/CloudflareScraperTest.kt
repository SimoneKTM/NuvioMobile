package com.nuvio.app.features.vezie

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudflareScraperTest {

    @Test
    fun `extractIframes trova iframe con src`() {
        val html = """
            <html>
                <iframe src="https://v.vidxgo.co/tt0141842"></iframe>
                <iframe src="https://mixdrop.co/e/abc123"></iframe>
                <iframe data-src="https://lazy.com/iframe"></iframe>
            </html>
        """.trimIndent()
        val iframes = extractIframes(html)
        assertEquals(2, iframes.size)
        assertTrue(iframes.contains("https://v.vidxgo.co/tt0141842"))
        assertTrue(iframes.contains("https://mixdrop.co/e/abc123"))
    }

    @Test
    fun `extractIframes normalizza protocollo`() {
        val html = """<iframe src="//fast.com/video"></iframe>"""
        val iframes = extractIframes(html)
        assertEquals("https://fast.com/video", iframes.first())
    }

    @Test
    fun `extractIframes vuoto quando nessun iframe`() {
        val html = "<html><body><p>No iframes here</p></body></html>"
        assertTrue(extractIframes(html).isEmpty())
    }

    @Test
    fun `extractVideoUrls trova mp4 diretti`() {
        val html = """
            <video>
                <source src="https://cdn.example.com/video.mp4" type="video/mp4">
                <source src="https://cdn.example.com/720p.mp4">
            </video>
        """.trimIndent()
        val urls = extractVideoUrls(html)
        assertTrue(urls.contains("https://cdn.example.com/video.mp4"))
        assertTrue(urls.contains("https://cdn.example.com/720p.mp4"))
    }

    @Test
    fun `extractVideoUrls trova m3u8`() {
        val html = """
            <script>
                var player = new Player({file: "https://stream.example.com/playlist.m3u8"});
            </script>
        """.trimIndent()
        val urls = extractVideoUrls(html)
        assertTrue(urls.contains("https://stream.example.com/playlist.m3u8"))
    }

    @Test
    fun `extractVideoUrls trova data-src e data-lazy-src`() {
        val html = """
            <iframe data-src="https://player.com/video.mp4"></iframe>
            <div data-lazy-src="https://slow.com/movie.m3u8"></div>
        """.trimIndent()
        val urls = extractVideoUrls(html)
        assertTrue(urls.contains("https://player.com/video.mp4"))
        assertTrue(urls.contains("https://slow.com/movie.m3u8"))
    }

    @Test
    fun `extractVideoUrls trova URL inline nel testo`() {
        val html = """
            var videoUrl = "https://cdn.example.com/movie.mp4";
            const hls = 'https://hls.example.com/stream.m3u8';
        """.trimIndent()
        val urls = extractVideoUrls(html)
        assertTrue(urls.contains("https://cdn.example.com/movie.mp4"))
        assertTrue(urls.contains("https://hls.example.com/stream.m3u8"))
    }

    @Test
    fun `extractVideoUrls normalizza protocollo`() {
        val html = """<source src="//cdn.example.com/video.mp4">"""
        val urls = extractVideoUrls(html)
        assertEquals("https://cdn.example.com/video.mp4", urls.first())
    }

    @Test
    fun `extractVideoUrls non duplica URL`() {
        val html = """
            <source src="https://cdn.example.com/video.mp4">
            <source src="https://cdn.example.com/video.mp4">
        """.trimIndent()
        val urls = extractVideoUrls(html)
        assertEquals(1, urls.size)
    }

    @Test
    fun `extractVideoUrls vuoto quando nessun video`() {
        val html = "<html><body><p>Solo testo</p></body></html>"
        assertTrue(extractVideoUrls(html).isEmpty())
    }

    @Test
    fun `PageScrapeResult dati corretti`() {
        val result = com.nuvio.app.core.network.PageScrapeResult(
            url = "https://example.com",
            html = "<html></html>",
            iframes = listOf("https://iframe.com"),
            videoUrls = listOf("https://video.com/stream.mp4"),
        )
        assertEquals("https://example.com", result.url)
        assertEquals(1, result.iframes.size)
        assertEquals(1, result.videoUrls.size)
        assertEquals("https://iframe.com", result.iframes[0])
        assertEquals("https://video.com/stream.mp4", result.videoUrls[0])
    }

    @Test
    fun `extractIframes con attributi extra`() {
        val html = """<iframe frameborder="0" src="https://vidmoly.to/embed/abc" allowfullscreen></iframe>"""
        val iframes = extractIframes(html)
        assertEquals("https://vidmoly.to/embed/abc", iframes.first())
    }

    @Test
    fun `extractVideoUrls trova URL con file in js`() {
        val html = """
            <script>
                file: "https://player.com/stream.m3u8",
                url: "https://backup.com/video.mp4"
            </script>
        """.trimIndent()
        val urls = extractVideoUrls(html)
        assertTrue(urls.contains("https://player.com/stream.m3u8"))
        assertTrue(urls.contains("https://backup.com/video.mp4"))
    }

    @Test
    fun `navigateToEpisode trova episodio nel HTML`() {
        val html = """
            <div class="episodes">
                <a href="/serie/soprano/2/3">Episodio 03</a>
                <a href="/serie/soprano/2/4">Episodio 04</a>
            </div>
        """.trimIndent()

        val episodeStr = "03"
        val pattern = Regex("""<a[^>]*href\s*=\s*["']([^"']+)["'][^>]*>[^<]*$episodeStr[^<]*</a>""", RegexOption.IGNORE_CASE)
        val match = pattern.find(html)
        assertTrue(match != null)
        assertEquals("/serie/soprano/2/3", match!!.groupValues[1])
    }

    @Test
    fun `findContentUrl con pattern post-title`() {
        val html = """
            <article>
                <h2 class="post-title"><a href="https://site.com/movie/12345">Film Title</a></h2>
            </article>
        """.trimIndent()

        val pattern = Regex("""<h2[^>]*class\s*=\s*["'][^"']*(?:post-title|entry-title|title)[^"']*["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val match = pattern.find(html)
        assertTrue(match != null)
        assertEquals("https://site.com/movie/12345", match!!.groupValues[1])
    }

    @Test
    fun `navigateToEpisode con data-episode`() {
        val html = """
            <div class="episode-item" data-episode="03" data-season="2">
                <a href="/watch/episode/2/3">S02E03</a>
            </div>
        """.trimIndent()

        val episodeStr = "03"
        val pattern = Regex("""<div[^>]*class\s*=\s*["'][^"']*episode[^"']*["'][^>]*data-episode\s*=\s*["']$episodeStr["'][^>]*>\s*<a\s+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val match = pattern.find(html)
        assertTrue(match != null, "Pattern should match data-episode")
        assertEquals("/watch/episode/2/3", match!!.groupValues[1])
    }
}
