package com.github.damontecres.wholphin.util

import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.VideoRangeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRankingTest {
    private val gb = 1024L * 1024 * 1024

    // 2 hours
    private val runtimeTicks = 2L * 60 * 60 * 10_000_000

    private fun source(
        id: String,
        name: String?,
        isRemote: Boolean = true,
        streams: List<MediaStream>? = null,
        size: Long? = null,
    ) = MediaSourceInfo(
        id = id,
        name = name,
        path = if (isRemote) "https://example.com/$id" else "/data/Film/$id.mkv",
        size = size,
        mediaStreams = streams,
        protocol = if (isRemote) MediaProtocol.HTTP else MediaProtocol.FILE,
        type = MediaSourceType.DEFAULT,
        isRemote = isRemote,
        readAtNativeFramerate = false,
        ignoreDts = false,
        ignoreIndex = false,
        genPtsInput = false,
        supportsTranscoding = true,
        supportsDirectStream = true,
        supportsDirectPlay = true,
        isInfiniteStream = false,
        requiresOpening = false,
        requiresClosing = false,
        requiresLooping = false,
        supportsProbing = false,
        transcodingSubProtocol = MediaStreamProtocol.HLS,
        hasSegments = false,
    )

    private fun stream(
        index: Int,
        type: MediaStreamType,
        codec: String? = null,
        language: String? = null,
        height: Int? = null,
        range: VideoRangeType = VideoRangeType.UNKNOWN,
    ) = MediaStream(
        index = index,
        type = type,
        codec = codec,
        language = language,
        height = height,
        videoRangeType = range,
        isExternal = false,
        deliveryMethod = SubtitleDeliveryMethod.EMBED,
        isInterlaced = false,
        isDefault = false,
        isForced = false,
        isHearingImpaired = false,
        isTextSubtitleStream = false,
        supportsExternalStream = false,
    )

    @Test
    fun parsesNigiroAioStreamsFormat() {
        val traits = StreamSourceParser.parse("⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 6.49 GB")
        assertEquals(1080, traits.height)
        assertEquals(SourceVideoCodec.HEVC, traits.videoCodec)
        assertTrue(traits.cached)
        assertFalse(traits.uncached)
        assertTrue(traits.italian)
        assertEquals((6.49 * gb).toLong(), traits.sizeBytes)
    }

    @Test
    fun parsesStreamVixFormat() {
        val traits = StreamSourceParser.parse("🌐 WEB · 🇮🇹 ITA")
        assertTrue(traits.italian)
        assertFalse(traits.cached)
        assertNull(traits.height)
        assertNull(traits.sizeBytes)
    }

    @Test
    fun parsesGenericReleaseNames() {
        val remux = StreamSourceParser.parse("[RD+] Torrentio 4k Dolby Vision\nMovie.2023.2160p.BluRay.REMUX.HEVC.DV.TrueHD.Atmos-GRP 💾 58,3 GB")
        assertEquals(2160, remux.height)
        assertEquals(SourceHdr.DOLBY_VISION, remux.hdr)
        assertEquals(SourceQuality.REMUX, remux.quality)
        assertTrue(remux.cached)
        assertEquals((58.3 * gb).toLong(), remux.sizeBytes)

        val web = StreamSourceParser.parse("Movie 2023 1080p WEB-DL H.264 MULTI")
        assertEquals(SourceVideoCodec.H264, web.videoCodec)
        assertEquals(SourceQuality.WEB, web.quality)
        assertTrue(web.multiAudio)
        assertFalse(web.italian)

        val uncached = StreamSourceParser.parse("⏳ RD download · 1080p · x265")
        assertTrue(uncached.uncached)
        assertFalse(uncached.cached)
        assertEquals(SourceVideoCodec.HEVC, uncached.videoCodec)

        assertEquals(SourceQuality.CAM, StreamSourceParser.parse("Movie 2025 HDCAM x264").quality)
    }

    @Test
    fun wordBoundariesAvoidFalsePositives() {
        val traits = StreamSourceParser.parse("La Dolce Vita SDR DTS 5.1")
        assertFalse(traits.italian)
        assertNull(traits.height)
        assertNull(traits.hdr)
    }

    @Test
    fun serverMetadataWinsOverName() {
        val local =
            source(
                id = "local",
                name = "Movie",
                isRemote = false,
                size = 12 * gb,
                streams =
                    listOf(
                        stream(0, MediaStreamType.VIDEO, codec = "hevc", height = 1600, range = VideoRangeType.HDR10),
                        stream(1, MediaStreamType.AUDIO, language = "ita"),
                        stream(2, MediaStreamType.AUDIO, language = "eng"),
                    ),
            )
        val traits = StreamSourceParser.parse(local)
        assertEquals(2160, traits.height)
        assertEquals(SourceVideoCodec.HEVC, traits.videoCodec)
        assertEquals(SourceHdr.HDR10, traits.hdr)
        assertTrue(traits.italian)
        assertTrue(traits.multiAudio)
        assertEquals(12 * gb, traits.sizeBytes)
        assertFalse(traits.isRemote)
    }

    @Test
    fun keepsServerOrderWhenNothingStandsOut() {
        val sources =
            listOf(
                source("a", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 6.49 GB"),
                source("b", "⚡ RD · 🇮🇹 ITA · 2160p (HEVC) · 15.2 GB"),
                source("c", "⚡ RD · 🇮🇹 ITA · 720p (H264) · 2.1 GB"),
            )
        val ranked = SourceRanking.rank(sources, DecodeCapabilities(), runtimeTicks)
        assertEquals(listOf("a", "b", "c"), ranked.map { it.id })
    }

    @Test
    fun localFileComesFirst() {
        val sources =
            listOf(
                source("remote", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 6.49 GB"),
                source("local", "Movie", isRemote = false),
            )
        assertEquals("local", SourceRanking.rank(sources, DecodeCapabilities(), runtimeTicks).first().id)
    }

    @Test
    fun demotesSourcesThatWouldNotPlayWell() {
        val sources =
            listOf(
                // ~66 Mbps over 2 hours: too much for the TV's Wi-Fi
                source("remux", "⚡ RD · 🇮🇹 ITA · 2160p (HEVC) · 58.3 GB · REMUX"),
                source("cam", "⚡ RD · 🇮🇹 ITA · 1080p · HDCAM · 1.5 GB"),
                source("uncached", "⏳ RD download · 🇮🇹 ITA · 1080p (HEVC) · 5 GB"),
                source("av1", "⚡ RD · 🇮🇹 ITA · 1080p (AV1) · 3 GB"),
                source("good", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 6.49 GB"),
            )
        val noAv1 = DecodeCapabilities(av1 = false)
        assertEquals("good", SourceRanking.rank(sources, noAv1, runtimeTicks).first().id)
        // Without AV1 support it ranks below the playable ones
        val ranked = SourceRanking.rank(sources, noAv1, runtimeTicks).map { it.id }
        assertTrue(ranked.indexOf("av1") > ranked.indexOf("good"))
        assertTrue(ranked.indexOf("remux") > ranked.indexOf("good"))
        assertTrue(ranked.indexOf("cam") > ranked.indexOf("good"))
        assertTrue(ranked.indexOf("uncached") > ranked.indexOf("good"))
    }

    @Test
    fun prefersItalianOverOtherLanguages() {
        val sources =
            listOf(
                source("eng", "⚡ RD · 1080p (HEVC) · 6 GB"),
                source("ita", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 6 GB"),
            )
        assertEquals("ita", SourceRanking.rank(sources, DecodeCapabilities(), runtimeTicks).first().id)
    }

    @Test
    fun respectsUserMaxBitrate() {
        val sources =
            listOf(
                // ~19 Mbps
                source("big", "⚡ RD · 🇮🇹 ITA · 2160p (HEVC) · 16 GB"),
                // ~5 Mbps
                source("small", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 4.5 GB"),
            )
        assertEquals("big", SourceRanking.rank(sources, DecodeCapabilities(), runtimeTicks).first().id)
        assertEquals(
            "small",
            SourceRanking.rank(sources, DecodeCapabilities(), runtimeTicks, maxBitrate = 10_000_000).first().id,
        )
    }

    // Play's own choice (SourceRanking.best): 2 hours at the 10 Mbps Wi-Fi budget is about 8.4 GB

    @Test
    fun bestTakesItalian4kWhenItFitsTheWifi() {
        val hd = source("hd", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC)", size = 2 * gb)
        val uhd = source("uhd", "⚡ RD · 🇮🇹 ITA · 2160p (HEVC)", size = 7 * gb)
        assertEquals("uhd", SourceRanking.best(listOf(hd, uhd), null, runtimeTicks)?.id)
    }

    @Test
    fun bestFallsBackTo1080pWhen4kIsTooHeavy() {
        val uhd = source("uhd", "⚡ RD · 🇮🇹 ITA · 2160p WEB-DL (HEVC)", size = 12 * gb)
        val hd = source("hd", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC)", size = 2 * gb)
        assertEquals("hd", SourceRanking.best(listOf(uhd, hd), null, runtimeTicks)?.id)
    }

    @Test
    fun bestPrefersItalianOverAHigherResolution() {
        val english = source("en", "⚡ RD · 🇬🇧 ORIG · 2160p (HEVC)", size = 8 * gb)
        val italian = source("it", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC)", size = 2 * gb)
        assertEquals("it", SourceRanking.best(listOf(english, italian), null, runtimeTicks)?.id)
    }

    @Test
    fun bestTakesTheFileOnTheServerFirst() {
        val stream = source("uhd", "⚡ RD · 🇮🇹 ITA · 2160p (HEVC)", size = 8 * gb)
        val local = source("local", "Film", isRemote = false, size = 4 * gb)
        assertEquals("local", SourceRanking.best(listOf(stream, local), null, runtimeTicks)?.id)
    }

    @Test
    fun bestSkipsUncachedStreams() {
        val uncached = source("uhd", "⏳ RD · 🇮🇹 ITA · 2160p (HEVC)", size = 8 * gb)
        val cached = source("hd", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC)", size = 2 * gb)
        assertEquals("hd", SourceRanking.best(listOf(uncached, cached), null, runtimeTicks)?.id)
    }

    @Test
    fun bestTakesTheLightestItalianWhenNothingFits() {
        val uhd = source("uhd", "⚡ RD · 🇮🇹 ITA · 2160p (HEVC)", size = 40 * gb)
        val hd = source("hd", "⚡ RD · 🇮🇹 ITA · 1080p Remux", size = 20 * gb)
        assertEquals("hd", SourceRanking.best(listOf(uhd, hd), null, runtimeTicks)?.id)
    }

    @Test
    fun bestPutsCachedFirstWithinAResolution() {
        val plain = source("plain", "RD · 🇮🇹 ITA · 1080p (HEVC)", size = 2 * gb)
        val cached = source("cached", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC)", size = 3 * gb)
        assertEquals("cached", SourceRanking.best(listOf(plain, cached), null, runtimeTicks)?.id)
    }

    @Test
    fun bestOfNothingIsNull() {
        assertNull(SourceRanking.best(emptyList(), null, runtimeTicks))
    }

    /** A Gelato source as the server reports it: a "local file" at /stub, probed figures that mean nothing */
    private fun gelato(
        id: String,
        name: String,
        height: Int? = null,
        codec: String? = null,
        bitrate: Int? = null,
    ) = source(id, name, isRemote = false)
        .copy(
            path = "/stub",
            bitrate = bitrate,
            mediaStreams = height?.let { listOf(stream(0, MediaStreamType.VIDEO, codec = codec, height = it)) },
        )

    @Test
    fun gelatoStubsAreStreamsNotFilesOnTheServer() {
        assertTrue(gelato("a", "⚡ RD · 🇮🇹 ITA · 1080p").isStreamed())
        assertFalse(source("b", "Film", isRemote = false).isStreamed())
    }

    // The Christmas Switch (1h43): AIOStreams' order, the server's probe gave the WEB stream 2 b/s and the AVC one 31 kb/s
    private val christmasRuntime = 61_800_000_000L
    private val christmas =
        listOf(
            gelato("hevc", "⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 1.48 GB\n🗣️ Italian", height = 960, codec = "hevc", bitrate = 1_909_376),
            gelato("web", "🌐 WEB · 🇮🇹 ITA\n🗣️ Italian", height = 720, codec = "h264", bitrate = 2),
            gelato("orig1080", "⚡ RD · 🇬🇧 ORIG · 1080p · 2.47 GB\n🗣️ Latino, Dual Audio"),
            gelato("orig2160", "⚡ RD · 🇬🇧 ORIG · 2160p WEB-DL (HEVC) · 14.98 GB\n🗣️ Swedish, English"),
            gelato("avc", "⚡ RD · 🇮🇹 ITA · 1080p WEB-DL (AVC) · 2.82 GB\n🗣️ Italian, Swedish", height = 1080, codec = "h264", bitrate = 31_664),
        )

    @Test
    fun bestOfGelatoStreamsIsTheItalian1080p() {
        assertEquals("hevc", SourceRanking.best(christmas, null, christmasRuntime)?.id)
    }

    @Test
    fun bestAfterAFailureKeepsTheResolutionOverAnUnsizedStream() {
        val remaining = christmas.filter { it.id != "hevc" }
        assertEquals("avc", SourceRanking.best(remaining, null, christmasRuntime)?.id)
    }

    @Test
    fun parsesAudioFormats() {
        fun audio(text: String) = StreamSourceParser.parse(text).let { it.audio to it.audioChannels }
        assertEquals(SourceAudio.DDP_ATMOS to 6, audio("🗣️ Italian · 🔊 DD+ Atmos 5.1"))
        assertEquals(SourceAudio.LOSSLESS to 8, audio("TrueHD Atmos 7.1"))
        assertEquals(SourceAudio.DD to 6, audio("🔊 DD 5.1"))
        assertEquals(SourceAudio.DDP to null, audio("1080p WEB-DL EAC3"))
        assertEquals(SourceAudio.LOSSLESS to null, audio("DTS-HD MA"))
        assertEquals(SourceAudio.DTS to 6, audio("DTS 5.1"))
        assertEquals(SourceAudio.BASIC to 2, audio("AAC 2.0"))
        assertEquals(null to null, audio("⚡ RD · 🇮🇹 ITA · 1080p (HEVC) · 1.48 GB"))
    }

    @Test
    fun probedItalianTrackDecidesTheAudio() {
        val probed =
            source(
                "p",
                "⚡ RD · 🇮🇹 ITA · 1080p",
                streams =
                    listOf(
                        stream(0, MediaStreamType.AUDIO, codec = "truehd", language = "eng"),
                        stream(1, MediaStreamType.AUDIO, codec = "eac3", language = "ita"),
                    ),
            )
        assertEquals(SourceAudio.DDP, StreamSourceParser.parse(probed).audio)
    }

    @Test
    fun bestTakesTheBetterSoundAtTheSameResolution() {
        val stereo = source("stereo", "⚡ RD · 🇮🇹 ITA · 1080p · AAC 2.0", size = 2 * gb)
        val atmos = source("atmos", "⚡ RD · 🇮🇹 ITA · 1080p · DD+ Atmos 5.1", size = 5 * gb)
        assertEquals("atmos", SourceRanking.best(listOf(stereo, atmos), null, runtimeTicks)?.id)
    }

    @Test
    fun bestPrefersDolbyOverLosslessTheTvMustDecode() {
        val truehd = source("truehd", "⚡ RD · 🇮🇹 ITA · 1080p · TrueHD 7.1", size = 6 * gb)
        val dd = source("dd", "⚡ RD · 🇮🇹 ITA · 1080p · DD 5.1", size = 3 * gb)
        assertEquals("dd", SourceRanking.best(listOf(truehd, dd), null, runtimeTicks)?.id)
    }

    @Test
    fun bestSoundNeverTakesAFileTooHeavyForTheWifi() {
        val atmos = source("atmos", "⚡ RD · 🇮🇹 ITA · 1080p Remux · DD+ Atmos 5.1", size = 25 * gb)
        val dd = source("dd", "⚡ RD · 🇮🇹 ITA · 1080p · DD 5.1", size = 3 * gb)
        assertEquals("dd", SourceRanking.best(listOf(atmos, dd), null, runtimeTicks)?.id)
    }

    @Test
    fun bestKeepsItalianEvenWhenOnlyAnotherLanguageIsCached() {
        val english = source("en", "⚡ RD · 🇬🇧 ORIG · 1080p", size = 2 * gb)
        val italian = source("it", "⏳ RD · 🇮🇹 ITA · 1080p", size = 2 * gb)
        assertEquals("it", SourceRanking.best(listOf(english, italian), null, runtimeTicks)?.id)
    }
}
