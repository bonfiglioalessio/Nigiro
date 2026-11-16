package com.github.damontecres.wholphin.ui.detail.movie

import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieVersionsTest {
    private fun audio(lang: String?) =
        MediaStream(
            index = 0,
            type = MediaStreamType.AUDIO,
            isExternal = false,
            deliveryMethod = SubtitleDeliveryMethod.EMBED,
            codec = null,
            language = lang,
            isInterlaced = false,
            isDefault = false,
            isForced = false,
            isHearingImpaired = false,
            isTextSubtitleStream = false,
            supportsExternalStream = false,
        )

    private fun source(
        id: String,
        name: String? = null,
        container: String? = null,
        audio: List<String?> = emptyList(),
    ) = MediaSourceInfo(
        id = id,
        name = name,
        container = container,
        mediaStreams = audio.map { audio(it) },
        protocol = MediaProtocol.HTTP,
        type = MediaSourceType.DEFAULT,
        isRemote = false,
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

    @Test
    fun `local copy comes first, then streamed versions, without duplicates`() {
        val local = source("L", name = "Film.mkv")
        val rd = source("A", name = "⚡ RD · 🇮🇹 ITA · 1080p")
        val web = source("B", name = "🌐 WEB · ITA")
        val options = versionOptions(streamed = listOf(rd, web, rd), local = listOf(local), localLabel = "Su disco")

        assertEquals(listOf("L", "A", "B"), options.map { it.source.id })
        assertTrue(options[0].isLocal)
        assertEquals("Su disco", options[0].title)
        assertFalse(options[1].isLocal)
        assertEquals("⚡ RD · 🇮🇹 ITA · 1080p", options[1].title)
    }

    @Test
    fun `title falls back to the container when the source has no name`() {
        val options = versionOptions(listOf(source("A", name = " ", container = "mkv")), emptyList(), "Su disco")
        assertEquals("MKV", options.single().title)
    }

    @Test
    fun `audio line lists distinct known languages in Italian`() {
        val options =
            versionOptions(
                listOf(source("A", name = "x", audio = listOf("ita", "eng", "ita", null))),
                emptyList(),
                "Su disco",
            )
        assertEquals("Italiano, Inglese", options.single().audio)
        assertNull(versionOptions(listOf(source("B", name = "y")), emptyList(), "Su disco").single().audio)
    }

    @Test
    fun `a single version plays straight away, several open the picker`() {
        val one = versionOptions(listOf(source("A", name = "x")), emptyList(), "Su disco")
        val none = versionOptions(emptyList(), emptyList(), "Su disco")
        val two = versionOptions(listOf(source("A", name = "x"), source("B", name = "y")), emptyList(), "Su disco")

        assertEquals(PlayDecision.PlayNow(one.single()), decidePlay(one))
        assertEquals(PlayDecision.PlayNow(null), decidePlay(none))
        assertEquals(PlayDecision.Pick(two), decidePlay(two))
    }

    @Test
    fun `light detail fields never ask for media sources`() {
        assertFalse(ItemFields.MEDIA_SOURCES in LightDetailFields)
        assertFalse(ItemFields.MEDIA_STREAMS in LightDetailFields)
        assertTrue(ItemFields.PEOPLE in LightDetailFields)
        assertTrue(ItemFields.OVERVIEW in LightDetailFields)
    }

    @Test
    fun `cast and crew are split by role kind`() {
        val (cast, crew) =
            splitCastAndCrew(
                listOf(
                    org.jellyfin.sdk.model.api.PersonKind.ACTOR,
                    org.jellyfin.sdk.model.api.PersonKind.DIRECTOR,
                    org.jellyfin.sdk.model.api.PersonKind.GUEST_STAR,
                    org.jellyfin.sdk.model.api.PersonKind.WRITER,
                ),
            ) { it }
        assertEquals(
            listOf(org.jellyfin.sdk.model.api.PersonKind.ACTOR, org.jellyfin.sdk.model.api.PersonKind.GUEST_STAR),
            cast,
        )
        assertEquals(
            listOf(org.jellyfin.sdk.model.api.PersonKind.DIRECTOR, org.jellyfin.sdk.model.api.PersonKind.WRITER),
            crew,
        )
    }
}
