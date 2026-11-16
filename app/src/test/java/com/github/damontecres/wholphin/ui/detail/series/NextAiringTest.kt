package com.github.damontecres.wholphin.ui.detail.series

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class NextAiringTest {
    private val today = LocalDate.of(2026, 11, 16)

    private fun episode(
        name: String,
        premiere: LocalDateTime?,
    ) = BaseItemDto(
        id = UUID.nameUUIDFromBytes(name.toByteArray()),
        name = name,
        type = BaseItemKind.EPISODE,
        premiereDate = premiere,
    )

    @Test
    fun `yesterday's episode is skipped`() {
        val upcoming =
            listOf(
                episode("S04E03", LocalDateTime.of(2026, 11, 15, 21, 0)),
                episode("S04E04", LocalDateTime.of(2026, 11, 22, 21, 0)),
            )
        assertEquals("S04E04", firstStillToAir(upcoming, today)?.name)
    }

    @Test
    fun `an episode airing today is kept`() {
        val upcoming = listOf(episode("S01E05", LocalDateTime.of(2026, 11, 16, 3, 0)))
        assertEquals("S01E05", firstStillToAir(upcoming, today)?.name)
    }

    @Test
    fun `only aired or undated episodes give nothing`() {
        val upcoming =
            listOf(
                episode("old", LocalDateTime.of(2026, 11, 15, 23, 59)),
                episode("undated", null),
            )
        assertNull(firstStillToAir(upcoming, today))
    }
}
