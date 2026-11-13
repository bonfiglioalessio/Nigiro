package com.github.damontecres.wholphin.ui.main

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.RemoteHomeConfig
import com.github.damontecres.wholphin.data.model.RemotePlatformConfig
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StremioHomeLogicTest {
    private fun mediaItem(
        name: String,
        kind: BaseItemKind = BaseItemKind.MOVIE,
        year: Int? = 2025,
        rating: Float? = 8.1f,
        favorite: Boolean = false,
    ): BaseItem =
        BaseItem(
            BaseItemDto(
                id = UUID.randomUUID(),
                type = kind,
                name = name,
                productionYear = year,
                communityRating = rating,
            ),
            useSeriesForPrimary = false,
        ).let { base ->
            if (!favorite) {
                base
            } else {
                BaseItem(
                    base.data.copy(
                        userData =
                            org.jellyfin.sdk.model.api.UserItemDataDto(
                                playbackPositionTicks = 0L,
                                playCount = 0,
                                isFavorite = true,
                                lastPlayedDate = null,
                                played = false,
                                key = base.id.toString(),
                                itemId = base.id,
                            ),
                    ),
                    useSeriesForPrimary = false,
                )
            }
        }

    @Test
    fun `parseHomeMediaFilter maps server defaults cleanly`() {
        assertEquals(HomeMediaFilter.ALL, parseHomeMediaFilter(null))
        assertEquals(HomeMediaFilter.ALL, parseHomeMediaFilter("all"))
        assertEquals(HomeMediaFilter.MOVIE, parseHomeMediaFilter("movie"))
        assertEquals(HomeMediaFilter.SERIES, parseHomeMediaFilter("series"))
    }

    @Test
    fun `resolvePlatformTargetCollectionId routes movie series or asks user on all`() {
        val netflix =
            PlatformTile(
                key = "netflix",
                name = "Netflix",
                movieCollectionId = "m-id",
                seriesCollectionId = "s-id",
                isHome = false,
            )
        assertNull(resolvePlatformTargetCollectionId(netflix, HomeMediaFilter.ALL))
        assertTrue(needsPlatformTypeChoice(netflix, HomeMediaFilter.ALL))
        assertEquals("m-id", resolvePlatformTargetCollectionId(netflix, HomeMediaFilter.MOVIE))
        assertEquals("s-id", resolvePlatformTargetCollectionId(netflix, HomeMediaFilter.SERIES))
        assertFalse(needsPlatformTypeChoice(netflix, HomeMediaFilter.MOVIE))
        assertFalse(needsPlatformTypeChoice(netflix, HomeMediaFilter.SERIES))

        val moviesOnly = netflix.copy(seriesCollectionId = null)
        assertEquals("m-id", resolvePlatformTargetCollectionId(moviesOnly, HomeMediaFilter.ALL))
        assertFalse(needsPlatformTypeChoice(moviesOnly, HomeMediaFilter.ALL))
    }

    @Test
    fun `interleavePlatformItems alternates movies and series without duplicates`() {
        val m1 = mediaItem("Movie 1", BaseItemKind.MOVIE)
        val m2 = mediaItem("Movie 2", BaseItemKind.MOVIE)
        val s1 = mediaItem("Series 1", BaseItemKind.SERIES)
        val s2 = mediaItem("Series 2", BaseItemKind.SERIES)

        val interleaved = interleavePlatformItems(listOf(m1, m2), listOf(s1, s2), maxItems = 3)
        assertEquals(listOf(m1, s1, m2), interleaved)
    }

    @Test
    fun `buildStremioHomeRowSpecs puts home Top 10 Italia before other platforms`() {
        val specs = buildStremioHomeRowSpecs(RemoteHomeConfig.DEFAULT_PLATFORMS, HomeMediaFilter.ALL)
        assertEquals("popular", specs.first().key)
        assertEquals("Top 10 Italia", specs.first().title)
        assertEquals("netflix", specs[1].key)

        val movieOnlySpecs =
            buildStremioHomeRowSpecs(
                listOf(
                    RemotePlatformConfig("p1", "Both", "m1", "s1", false),
                    RemotePlatformConfig("p2", "Series Only", null, "s2", false),
                ),
                HomeMediaFilter.MOVIE,
            )
        assertEquals(listOf("p1"), movieOnlySpecs.map { it.key })
    }

    @Test
    fun `buildHeroSlides extracts metadata rating badge and favorite flag`() {
        val item = mediaItem("Severance", BaseItemKind.SERIES, year = 2025, rating = 8.7f, favorite = true)
        val slides =
            buildHeroSlides(
                items = listOf(item),
                count = 6,
                badgeLabel = "Top 10 Italia",
                backdropUrlFor = { "https://backdrop/${it.id}" },
                typeLabelFor = { "Serie TV" },
            )
        assertEquals(1, slides.size)
        val slide = slides.first()
        assertEquals("#1 Top 10 Italia", slide.badgeText)
        assertEquals("2025", slide.yearText)
        assertEquals("8.7", slide.ratingText)
        assertEquals("Serie TV", slide.typeLabel)
        assertTrue(slide.isFavorite)
    }
}
