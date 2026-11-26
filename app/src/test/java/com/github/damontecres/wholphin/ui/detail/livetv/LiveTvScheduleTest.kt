package com.github.damontecres.wholphin.ui.detail.livetv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class LiveTvScheduleTest {
    private fun channel(
        name: String,
        number: String = "1",
        favorite: Boolean = false,
    ): TvChannel =
        TvChannel(
            id = UUID.randomUUID(),
            number = number,
            name = name,
            imageUrl = null,
            favorite = favorite,
        )

    private fun program(
        channelId: UUID,
        start: LocalDateTime,
        end: LocalDateTime,
        name: String,
        fake: Boolean = false,
    ): TvProgram =
        TvProgram(
            id = UUID.randomUUID(),
            channelId = channelId,
            start = start,
            end = end,
            startHours = 0f,
            endHours = 1f,
            duration = java.time.Duration.between(start, end).toMinutes().toInt().minutes,
            name = name,
            subtitle = null,
            seasonEpisode = null,
            isRecording = false,
            isSeriesRecording = false,
            isRepeat = false,
            category = if (fake) ProgramCategory.FAKE else null,
        )

    @Test
    fun `airing, progress, remaining minutes and formatting work accurately`() {
        val chId = UUID.randomUUID()
        val start = LocalDateTime.of(2026, 11, 26, 18, 0)
        val end = LocalDateTime.of(2026, 11, 26, 19, 0)
        val show = program(chId, start, end, "TG1")
        val fakeShow = program(chId, start, end, "No guide", fake = true)

        val midpoint = LocalDateTime.of(2026, 11, 26, 18, 30)
        assertTrue(isProgramAiring(show, midpoint))
        assertTrue(isProgramAiring(show, start))
        assertFalse(isProgramAiring(show, end))
        assertFalse(isProgramAiring(fakeShow, midpoint))

        assertTrue(isProgramPast(show, end))
        assertFalse(isProgramPast(show, midpoint))

        assertEquals(0.5f, programProgress(show, midpoint), 0.001f)
        assertEquals(30, programRemainingMinutes(show, midpoint))
        assertEquals(0, programRemainingMinutes(show, end))

        assertEquals("18:00", formatTimeHHmm(start))
        assertEquals("18:00–19:00", formatTimeRange(start, end))
    }

    @Test
    fun `buildChannelScheduleRow splits current, upcoming and tonight skipping fake programs`() {
        val ch = channel(name = "TV Italia · Rai 1", number = "1", favorite = true)
        val now = LocalDateTime.of(2026, 11, 26, 18, 15)

        val past = program(ch.id, now.withHour(17).withMinute(0), now.withHour(18).withMinute(0), "Past Show")
        val airing = program(ch.id, now.withHour(18).withMinute(0), now.withHour(18).withMinute(45), "L'Eredita")
        val fakeGap = program(ch.id, now.withHour(18).withMinute(45), now.withHour(19).withMinute(0), "No guide", fake = true)
        val next1 = program(ch.id, now.withHour(19).withMinute(0), now.withHour(20).withMinute(0), "TG1 Sera")
        val prime1 = program(ch.id, now.withHour(20).withMinute(30), now.withHour(21).withMinute(30), "Affari Tuoi")
        val prime2 = program(ch.id, now.withHour(21).withMinute(30), now.withHour(23).withMinute(45), "Don Matteo")
        val late = program(ch.id, now.withHour(23).withMinute(45), now.plusDays(1).withHour(0).withMinute(30), "Porta a Porta")

        val row = buildChannelScheduleRow(ch, listOf(late, prime1, past, fakeGap, airing, next1, prime2), now)

        assertEquals(airing.id, row.current?.id)
        assertEquals(listOf(next1.id, prime1.id, prime2.id), row.upcoming.map { it.id })
        assertEquals(listOf(prime1.id, prime2.id), row.tonight.map { it.id })
    }

    @Test
    fun `tonightWindow advances to next day after 23_30 and handles no-EPG channel`() {
        val lateNight = LocalDateTime.of(2026, 11, 26, 23, 35)
        val (start, end) = tonightWindow(lateNight)
        assertEquals(LocalDateTime.of(2026, 11, 27, 20, 0), start)
        assertEquals(LocalDateTime.of(2026, 11, 27, 23, 30), end)

        val ch = channel(name = "Sport ITA · Sky Sport 24")
        val fakeOnly = listOf(program(ch.id, lateNight, lateNight.plusHours(1), "No guide", fake = true))
        val emptyRow = buildChannelScheduleRow(ch, fakeOnly, lateNight)
        assertNull(emptyRow.current)
        assertTrue(emptyRow.upcoming.isEmpty())
        assertTrue(emptyRow.tonight.isEmpty())
    }

    @Test
    fun `filterChannelsByGroup handles all, favorites and named groups`() {
        val c1 = channel(name = "TV Italia · Rai 1", number = "1", favorite = true)
        val c2 = channel(name = "Sport ITA · Sky Sport Uno", number = "201", favorite = false)
        val c3 = channel(name = "Film · Sky Cinema Uno", number = "301", favorite = true)
        val all = listOf(c1, c2, c3)

        assertEquals(all, filterChannelsByGroup(all, null))
        assertEquals(listOf(c1, c3), filterChannelsByGroup(all, FAVORITES_GROUP_KEY))
        assertEquals(listOf(c2), filterChannelsByGroup(all, "Sport ITA"))
    }
}
