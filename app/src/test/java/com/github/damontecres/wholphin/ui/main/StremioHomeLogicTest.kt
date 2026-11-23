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
        played: Boolean = false,
        hasBackdrop: Boolean = true,
        overview: String? = "Synopsis",
    ): BaseItem =
        BaseItem(
            BaseItemDto(
                id = UUID.randomUUID(),
                type = kind,
                name = name,
                productionYear = year,
                communityRating = rating,
                overview = overview,
                backdropImageTags = if (hasBackdrop) listOf("tag") else null,
            ),
            useSeriesForPrimary = false,
        ).let { base ->
            if (!favorite && !played) {
                base
            } else {
                BaseItem(
                    base.data.copy(
                        userData =
                            org.jellyfin.sdk.model.api.UserItemDataDto(
                                playbackPositionTicks = 0L,
                                playCount = if (played) 1 else 0,
                                isFavorite = favorite,
                                lastPlayedDate = null,
                                played = played,
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

    @Test
    fun `maxItemsForRow caps home Top 10 row at 10 while preserving other platforms`() {
        val top10Spec =
            StremioRowSpec(
                key = "popular",
                title = "Top 10 Italia",
                movieCollectionId = "m-id",
                seriesCollectionId = "s-id",
                isHome = true,
            )
        val netflixSpec = top10Spec.copy(key = "netflix", title = "Netflix", isHome = false)

        assertEquals(TOP_10_LIMIT, maxItemsForRow(top10Spec, maxItemsPerRow = 24))
        assertEquals(6, maxItemsForRow(top10Spec, maxItemsPerRow = 6))
        assertEquals(24, maxItemsForRow(netflixSpec, maxItemsPerRow = 24))
    }

    @Test
    fun `selectPlatformRowItems with maxItemsForRow produces 10 items for Top 10 Italia`() {
        val top10Spec =
            StremioRowSpec(
                key = "popular",
                title = "Top 10 Italia",
                movieCollectionId = "m-id",
                seriesCollectionId = "s-id",
                isHome = true,
            )
        val movies = (1..12).map { mediaItem("Movie $it", BaseItemKind.MOVIE) }
        val series = (1..12).map { mediaItem("Series $it", BaseItemKind.SERIES) }
        val cached = CachedPlatformCatalog(movies = movies, series = series)
        val rowLimit = maxItemsForRow(top10Spec, maxItemsPerRow = 24)

        val allItems = selectPlatformRowItems(cached, HomeMediaFilter.ALL, rowLimit)
        assertEquals(10, allItems.size)
        assertEquals(
            listOf(
                movies[0],
                series[0],
                movies[1],
                series[1],
                movies[2],
                series[2],
                movies[3],
                series[3],
                movies[4],
                series[4],
            ),
            allItems,
        )

        assertEquals(movies.take(10), selectPlatformRowItems(cached, HomeMediaFilter.MOVIE, rowLimit))
        assertEquals(series.take(10), selectPlatformRowItems(cached, HomeMediaFilter.SERIES, rowLimit))
    }

    @Test
    fun `selectRecentHeroItems excludes Top 10 by id or title and prioritizes unwatched quality items`() {
        val topMovie = mediaItem("Top Movie", BaseItemKind.MOVIE)
        val topSeriesTwin = mediaItem("Top Series", BaseItemKind.SERIES)

        val recentMovieWatched = mediaItem("Watched Movie", BaseItemKind.MOVIE, played = true)
        val recentMovieNoBackdrop = mediaItem("No Backdrop", BaseItemKind.MOVIE, hasBackdrop = false)
        val recentMovieLowRated = mediaItem("Low Rated", BaseItemKind.MOVIE, rating = 4.2f)
        val recentMovieTwinByTitle = mediaItem("  top movie ", BaseItemKind.MOVIE)
        val recentMovieGood1 = mediaItem("Fresh Movie 1", BaseItemKind.MOVIE, rating = 7.8f)
        val recentMovieGoodDup = mediaItem("Fresh Movie 1", BaseItemKind.MOVIE, rating = 7.8f)
        val recentMovieGood2 = mediaItem("Fresh Movie 2", BaseItemKind.MOVIE, rating = null)

        val recentSeriesExcluded = mediaItem("Top Series", BaseItemKind.SERIES)
        val recentSeriesGood = mediaItem("Fresh Series 1", BaseItemKind.SERIES, rating = 8.4f)

        val pickedAll =
            selectRecentHeroItems(
                recentMovies =
                    listOf(
                        topMovie,
                        recentMovieTwinByTitle,
                        recentMovieWatched,
                        recentMovieNoBackdrop,
                        recentMovieLowRated,
                        recentMovieGood1,
                        recentMovieGoodDup,
                        recentMovieGood2,
                    ),
                recentSeries = listOf(recentSeriesExcluded, recentSeriesGood),
                excludedItems = listOf(topMovie, topSeriesTwin),
                filter = HomeMediaFilter.ALL,
                count = 5,
            )

        // Tier 0 (unwatched + backdrop + overview + good rating), then Tier 1 (unwatched + backdrop), then Tier 2 (backdrop)
        assertEquals(
            listOf(recentMovieGood1, recentSeriesGood, recentMovieGood2, recentMovieLowRated, recentMovieWatched),
            pickedAll,
        )

        assertEquals(
            listOf(recentMovieGood1, recentMovieGood2),
            selectRecentHeroItems(
                recentMovies = listOf(recentMovieWatched, recentMovieGood1, recentMovieGood2),
                recentSeries = listOf(recentSeriesGood),
                excludedItems = emptyList(),
                filter = HomeMediaFilter.MOVIE,
                count = 2,
            ),
        )
        assertEquals(
            listOf(recentSeriesGood),
            selectRecentHeroItems(
                recentMovies = listOf(recentMovieGood1),
                recentSeries = listOf(recentSeriesGood),
                excludedItems = emptyList(),
                filter = HomeMediaFilter.SERIES,
                count = 2,
            ),
        )
    }
}
