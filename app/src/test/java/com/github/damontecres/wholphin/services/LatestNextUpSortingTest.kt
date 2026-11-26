package com.github.damontecres.wholphin.services

import com.github.damontecres.wholphin.data.model.BaseItem
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class LatestNextUpSortingTest {
    private fun episode(
        id: UUID = UUID.randomUUID(),
        seriesId: UUID,
        name: String,
        premiereDate: LocalDateTime? = null,
        lastPlayed: LocalDateTime? = null,
        dateCreated: LocalDateTime? = null,
    ): BaseItem =
        BaseItem(
            BaseItemDto(
                id = id,
                type = BaseItemKind.EPISODE,
                name = name,
                seriesId = seriesId,
                premiereDate = premiereDate,
                dateCreated = dateCreated,
                userData =
                    UserItemDataDto(
                        playbackPositionTicks = 0L,
                        playCount = 0,
                        isFavorite = false,
                        lastPlayedDate = lastPlayed,
                        played = false,
                        key = id.toString(),
                        itemId = id,
                    ),
            ),
            useSeriesForPrimary = true,
        )

    @Test
    fun `sorts newly aired nextUp ahead of older resume, deduplicates series, and puts unreleased last`() {
        val now = LocalDateTime.of(2026, 11, 26, 15, 0)
        val empSeries = UUID.randomUUID()
        val haloSeries = UUID.randomUUID()
        val maxtonSeries = UUID.randomUUID()

        val empResume =
            episode(
                seriesId = empSeries,
                name = "Empress S02E02",
                premiereDate = now.minusDays(4),
                lastPlayed = now.minusHours(2),
            )
        val movieResume =
            BaseItem(
                BaseItemDto(
                    id = UUID.randomUUID(),
                    type = BaseItemKind.MOVIE,
                    name = "Dune",
                    userData =
                        UserItemDataDto(
                            playbackPositionTicks = 100L,
                            playCount = 0,
                            isFavorite = false,
                            lastPlayedDate = now.minusDays(1),
                            played = false,
                            key = "dune",
                            itemId = UUID.randomUUID(),
                        ),
                ),
                useSeriesForPrimary = false,
            )

        val empNextUpDup =
            episode(
                seriesId = empSeries,
                name = "Empress S02E02 (dup)",
                premiereDate = now.minusDays(4),
            )
        val haloAiredToday =
            episode(
                seriesId = haloSeries,
                name = "Halo S02E08",
                premiereDate = now.minusHours(1),
            )
        val maxtonUnreleased =
            episode(
                seriesId = maxtonSeries,
                name = "Maxton Future",
                premiereDate = now.plusDays(13),
                lastPlayed = now.minusMinutes(10),
            )

        val sorted =
            sortContinueWatchingCombined(
                resume = listOf(empResume, movieResume),
                nextUp = listOf(empNextUpDup, haloAiredToday, maxtonUnreleased),
                nextUpLastPlayedById =
                    mapOf(
                        empNextUpDup.id to now.minusHours(2),
                        haloAiredToday.id to now.minusDays(3),
                        maxtonUnreleased.id to now.minusMinutes(10),
                    ),
                now = now,
            )

        assertEquals(
            listOf(haloAiredToday.id, empResume.id, movieResume.id, maxtonUnreleased.id),
            sorted.map { it.id },
        )
    }
}
