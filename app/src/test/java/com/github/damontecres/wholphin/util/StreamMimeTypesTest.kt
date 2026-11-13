package com.github.damontecres.wholphin.util

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamMimeTypesTest {
    @Test
    fun containerFromServerWins() {
        assertEquals(MimeTypes.APPLICATION_M3U8, StreamMimeTypes.infer("hls", false, "http://jellyfin/videos/1/master"))
        assertEquals(MimeTypes.APPLICATION_MPD, StreamMimeTypes.infer("dash", false, "http://jellyfin/videos/1/stream"))
    }

    @Test
    fun remoteHlsWithoutExtensionIsDetected() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamMimeTypes.infer(null, true, "https://vixsrc.to/playlist/12345?token=abc&expires=1&h=1"),
        )
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamMimeTypes.infer("mkv", true, "http://192.168.1.157:8888/proxy/hls/manifest.m3u8?d=https%3A%2F%2Fexample.com"),
        )
    }

    @Test
    fun progressiveFilesAreLeftToExoPlayer() {
        assertNull(StreamMimeTypes.infer("mkv", true, "https://abc.download.real-debrid.com/d/XYZ/Movie.2024.1080p.mkv"))
        // Only remote sources are guessed from the URL
        assertNull(StreamMimeTypes.infer(null, false, "http://jellyfin/hls/stream"))
    }
}
