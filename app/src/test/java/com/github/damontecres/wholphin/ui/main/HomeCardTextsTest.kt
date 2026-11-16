package com.github.damontecres.wholphin.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCardTextsTest {
    private val minute = 600_000_000L

    @Test
    fun `episode code is zero padded and needs both numbers`() {
        assertEquals("S02E01", episodeCode(2, 1))
        assertEquals("S10E112", episodeCode(10, 112))
        assertNull(episodeCode(null, 3))
        assertNull(episodeCode(1, null))
    }

    @Test
    fun `episode subtitle joins code and name`() {
        assertEquals("S02E01 · Devastato", episodeSubtitle(2, 1, "Devastato"))
        assertEquals("S02E01", episodeSubtitle(2, 1, " "))
        assertEquals("Pilot", episodeSubtitle(null, null, "Pilot"))
        assertNull(episodeSubtitle(null, null, null))
    }

    @Test
    fun `remaining time from runtime and position`() {
        assertEquals("8 min rimasti", remainingLabel(runTimeTicks = 50 * minute, positionTicks = 42 * minute))
        assertEquals("1 h 5 min rimasti", remainingLabel(runTimeTicks = 120 * minute, positionTicks = 55 * minute))
        assertEquals("2 h rimasti", remainingLabel(runTimeTicks = 150 * minute, positionTicks = 30 * minute))
        // Under a minute left, nothing started, or no runtime: no label
        assertNull(remainingLabel(runTimeTicks = 50 * minute, positionTicks = 50 * minute - 1))
        assertNull(remainingLabel(runTimeTicks = 50 * minute, positionTicks = 0))
        assertNull(remainingLabel(runTimeTicks = null, positionTicks = 10 * minute))
    }

    @Test
    fun `card meta skips what is missing`() {
        assertEquals(
            "2021 · ★ 7.8 · 🍅 83% · 2 h 35 min · PG-13",
            cardMeta(year = 2021, communityRating = 7.84f, criticRating = 83f, runTimeTicks = 155 * minute, officialRating = "PG-13"),
        )
        assertEquals("2024 · 48 min", cardMeta(2024, null, null, 48 * minute, " "))
        assertEquals("", cardMeta(null, 0f, null, null, null))
    }
}
