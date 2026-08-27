package com.app.reelssdk.ui.sprite

import org.junit.Assert.assertEquals
import org.junit.Test

class SpriteUtilsTest {

    @Test
    fun `extracts image stream playlist from hls master manifest`() {
        val masterUrl =
            "https://cdn.example.com/video/u/hls/master.m3u8?token=test"
        val manifest = """
            #EXTM3U
            #EXT-X-MEDIA:URI="audio.m3u8",TYPE=AUDIO
            #EXT-X-IMAGE-STREAM-INF:CODECS="jpeg",RESOLUTION=312x176,BANDWIDTH=131436,URI="../../trickplay/trickplay.m3u8"
        """.trimIndent()

        val urls = SpriteUtils.parseSpriteUrlsFromManifest(manifest, masterUrl)

        assertEquals(
            "https://cdn.example.com/video/trickplay/trickplay.m3u8",
            urls.single()
        )
        assertEquals(urls.single(), SpriteUtils.preferredSpriteUrl(urls))
    }

    @Test
    fun `parses hls image only playlist into timed image cues`() {
        val playlistUrl =
            "https://cdn.example.com/video/trickplay/trickplay.m3u8"
        val playlist = """
            #EXTM3U
            #EXT-X-TARGETDURATION:3
            #EXT-X-IMAGES-ONLY
            #EXTINF:2.0,
            frame_0001.png
            #EXTINF:2.5,
            frame_0002.png
            #EXT-X-ENDLIST
        """.trimIndent()

        val cues = SpriteUtils.parseHlsImagePlaylist(playlist, playlistUrl)

        assertEquals(2, cues.size)
        assertEquals(0L, cues[0].startMs)
        assertEquals(2000L, cues[0].endMs)
        assertEquals(
            "https://cdn.example.com/video/trickplay/frame_0001.png",
            cues[0].imageUrl
        )
        assertEquals(2000L, cues[1].startMs)
        assertEquals(4500L, cues[1].endMs)
    }
}
