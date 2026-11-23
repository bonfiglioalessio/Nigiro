package com.github.damontecres.wholphin.services

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeTrailerExtractorTest {
    @Test
    fun `isYouTubeUrl matches youtube domains and rejects others`() {
        assertTrue(isYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(isYouTubeUrl("https://youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(isYouTubeUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(isYouTubeUrl("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"))

        assertFalse(isYouTubeUrl("https://vimeo.com/123456"))
        assertFalse(isYouTubeUrl("https://example.com/trailer.mp4"))
        assertFalse(isYouTubeUrl(""))
        assertFalse(isYouTubeUrl("not a url"))
    }

    @Test
    fun `selectBestTrailerStream prefers 1080p H264 over 4K and AV1 and selects best audio`() {
        val av1 =
            TrailerVideoCandidate(
                url = "https://video/1080-av1",
                height = 1080,
                bitrate = 2_000_000,
                fps = 24,
                codec = "av01.0.08M.08",
                formatName = "MPEG_4",
            )
        val fourK =
            TrailerVideoCandidate(
                url = "https://video/2160-vp9",
                height = 2160,
                bitrate = 8_000_000,
                fps = 24,
                codec = "vp09.00.51.08",
                formatName = "WEBM",
            )
        val vp9 =
            TrailerVideoCandidate(
                url = "https://video/1080-vp9",
                height = 1080,
                bitrate = 3_000_000,
                fps = 24,
                codec = "vp9",
                formatName = "WEBM",
            )
        val h264 =
            TrailerVideoCandidate(
                url = "https://video/1080-h264",
                height = 1080,
                bitrate = 2_500_000,
                fps = 24,
                codec = "avc1.640028",
                formatName = "MPEG_4",
            )
        val audioEnWebm =
            TrailerAudioCandidate(
                url = "https://audio/en-webm",
                averageBitrate = 160_000,
                language = "en",
                isOriginal = true,
                formatName = "WEBMA_OPUS",
            )
        val audioItM4a =
            TrailerAudioCandidate(
                url = "https://audio/it-m4a",
                averageBitrate = 128_000,
                language = "it",
                isOriginal = false,
                formatName = "M4A",
            )

        val resolved =
            selectBestTrailerStream(
                videoOnly = listOf(av1, fourK, vp9, h264),
                audio = listOf(audioEnWebm, audioItM4a),
                muxed = emptyList(),
                hlsUrl = "https://hls/master.m3u8",
                dashUrl = "https://dash/manifest.mpd",
            )

        assertEquals(
            ResolvedTrailerStream.Merged(
                videoUrl = "https://video/1080-h264",
                audioUrl = "https://audio/it-m4a",
            ),
            resolved,
        )
    }

    @Test
    fun `selectBestTrailerStream prefers M4A over WEBM at equal audio priority and falls back cleanly`() {
        val video720 =
            TrailerVideoCandidate(
                url = "https://video/720",
                height = 720,
                bitrate = 1_500_000,
                fps = 24,
                codec = "avc1.4d401f",
                formatName = "MPEG_4",
            )
        val audioOrigWebm =
            TrailerAudioCandidate(
                url = "https://audio/orig-webm",
                averageBitrate = 160_000,
                language = "en",
                isOriginal = true,
                formatName = "WEBMA_OPUS",
            )
        val audioOrigM4a =
            TrailerAudioCandidate(
                url = "https://audio/orig-m4a",
                averageBitrate = 128_000,
                language = "en",
                isOriginal = true,
                formatName = "M4A",
            )

        assertEquals(
            ResolvedTrailerStream.Merged("https://video/720", "https://audio/orig-m4a"),
            selectBestTrailerStream(listOf(video720), listOf(audioOrigWebm, audioOrigM4a), emptyList(), null, null),
        )

        val muxed =
            TrailerVideoCandidate(
                url = "https://muxed/720",
                height = 720,
                bitrate = 1_500_000,
                fps = 24,
                codec = "avc1.42001E",
                formatName = "MPEG_4",
            )
        assertEquals(
            ResolvedTrailerStream.Single("https://muxed/720"),
            selectBestTrailerStream(listOf(video720), emptyList(), listOf(muxed), "https://hls/a.m3u8", null),
        )

        assertEquals(
            ResolvedTrailerStream.Single("https://hls/a.m3u8", MimeTypes.APPLICATION_M3U8),
            selectBestTrailerStream(emptyList(), emptyList(), emptyList(), "https://hls/a.m3u8", "https://dash/a.mpd"),
        )

        assertEquals(
            ResolvedTrailerStream.Single("https://dash/a.mpd", MimeTypes.APPLICATION_MPD),
            selectBestTrailerStream(emptyList(), emptyList(), emptyList(), null, "https://dash/a.mpd"),
        )

        assertNull(selectBestTrailerStream(emptyList(), emptyList(), emptyList(), null, null))
    }

    @Test
    fun `extractYouTubeVideoId handles watch, short, embed, shorts, live urls and rejects invalid ones`() {
        assertEquals("7g7ODDlhPQ0", extractYouTubeVideoId("https://www.youtube.com/watch?v=7g7ODDlhPQ0"))
        assertEquals("IhtRswxOn9Q", extractYouTubeVideoId("https://youtube.com/watch?feature=share&v=IhtRswxOn9Q&t=10s"))
        assertEquals("HLT9e9mC354", extractYouTubeVideoId("https://youtu.be/HLT9e9mC354?si=abc"))
        assertEquals("7g7ODDlhPQ0", extractYouTubeVideoId("https://www.youtube-nocookie.com/embed/7g7ODDlhPQ0"))
        assertEquals("7g7ODDlhPQ0", extractYouTubeVideoId("https://m.youtube.com/shorts/7g7ODDlhPQ0"))
        assertEquals("7g7ODDlhPQ0", extractYouTubeVideoId("https://www.youtube.com/live/7g7ODDlhPQ0?feature=shared"))

        assertNull(extractYouTubeVideoId("https://vimeo.com/12345678901"))
        assertNull(extractYouTubeVideoId("https://www.youtube.com/watch?v=too_short"))
        assertNull(extractYouTubeVideoId("not a url"))
    }

    @Test
    fun `parseInnerTubePlayerResponse extracts 1080p stream, HLS fallback, and rejects UNPLAYABLE`() {
        val okMergedJson =
            """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {
                "adaptiveFormats": [
                  {
                    "itag": 136,
                    "url": "https://cdn/video-720.mp4",
                    "mimeType": "video/mp4; codecs=\"avc1.64001F\"",
                    "bitrate": 1500000,
                    "height": 720,
                    "fps": 24
                  },
                  {
                    "itag": 137,
                    "url": "https://cdn/video-1080.mp4",
                    "mimeType": "video/mp4; codecs=\"avc1.640028\"",
                    "bitrate": 3200000,
                    "height": 1080,
                    "fps": 24
                  },
                  {
                    "itag": 999,
                    "signatureCipher": "s=abc",
                    "mimeType": "video/mp4; codecs=\"avc1.640028\"",
                    "height": 1080
                  },
                  {
                    "itag": 140,
                    "url": "https://cdn/audio.m4a",
                    "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                    "bitrate": 129000,
                    "averageBitrate": 128000,
                    "audioTrack": { "id": "it.1", "displayName": "Italiano", "audioIsDefault": true }
                  }
                ],
                "hlsManifestUrl": "https://cdn/master.m3u8"
              }
            }
            """.trimIndent()
        assertEquals(
            ResolvedTrailerStream.Merged("https://cdn/video-1080.mp4", "https://cdn/audio.m4a"),
            parseInnerTubePlayerResponse(okMergedJson),
        )

        val hlsOnlyJson =
            """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {
                "adaptiveFormats": [],
                "hlsManifestUrl": "https://cdn/only.m3u8"
              }
            }
            """.trimIndent()
        assertEquals(
            ResolvedTrailerStream.Single("https://cdn/only.m3u8", MimeTypes.APPLICATION_M3U8),
            parseInnerTubePlayerResponse(hlsOnlyJson),
        )

        val unplayableJson =
            """
            {
              "playabilityStatus": { "status": "UNPLAYABLE", "reason": "Video unavailable" },
              "streamingData": {}
            }
            """.trimIndent()
        assertNull(parseInnerTubePlayerResponse(unplayableJson))
    }
}
