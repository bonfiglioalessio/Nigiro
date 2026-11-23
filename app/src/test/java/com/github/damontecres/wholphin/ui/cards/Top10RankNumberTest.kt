package com.github.damontecres.wholphin.ui.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Top10RankNumberTest {
    @Test
    fun `computeTop10RankLayout scales glyph to 80 percent of card height and aligns bottom`() {
        val cardHeightPx = 378f
        val strokePadPx = 7f

        val layout =
            computeTop10RankLayout(
                rank = 2,
                rawWidth = 90f,
                rawHeight = 140f,
                cardHeightPx = cardHeightPx,
                strokePadPx = strokePadPx,
            )

        assertNotNull(layout)
        layout!!
        assertEquals(cardHeightPx * 0.80f / 140f, layout.scale, 0.0001f)
        assertEquals(cardHeightPx - strokePadPx, layout.baselineBottomPx, 0.0001f)
        assertEquals(90f * layout.scale + strokePadPx * 2f, layout.canvasWidthPx, 0.0001f)
    }

    @Test
    fun `computeTop10RankLayout uses smaller overlap for rank 1 than ranks 2 to 10`() {
        val cardHeightPx = 378f
        val strokePadPx = 7f
        val rawWidth = 90f
        val rawHeight = 140f

        val layout1 = computeTop10RankLayout(1, rawWidth, rawHeight, cardHeightPx, strokePadPx)!!
        val layout2 = computeTop10RankLayout(2, rawWidth, rawHeight, cardHeightPx, strokePadPx)!!

        val overlap1 = layout1.canvasWidthPx - layout1.posterOffsetPx
        val overlap2 = layout2.canvasWidthPx - layout2.posterOffsetPx
        assertTrue(overlap1 < overlap2)
        assertTrue(layout1.posterOffsetPx > layout2.posterOffsetPx)
    }

    @Test
    fun `computeTop10RankLayout produces positive poster offset and wider offset for double digit 10`() {
        val cardHeightPx = 378f
        val strokePadPx = 7f

        val singleDigit = computeTop10RankLayout(8, rawWidth = 95f, rawHeight = 140f, cardHeightPx, strokePadPx)!!
        val doubleDigit = computeTop10RankLayout(10, rawWidth = 155f, rawHeight = 140f, cardHeightPx, strokePadPx)!!

        assertTrue(singleDigit.posterOffsetPx > 0f)
        assertTrue(doubleDigit.posterOffsetPx > singleDigit.posterOffsetPx)
    }

    @Test
    fun `computeTop10RankLayout returns null for degenerate dimensions`() {
        assertNull(computeTop10RankLayout(1, rawWidth = 0f, rawHeight = 140f, cardHeightPx = 378f, strokePadPx = 7f))
        assertNull(computeTop10RankLayout(1, rawWidth = 90f, rawHeight = 0f, cardHeightPx = 378f, strokePadPx = 7f))
        assertNull(computeTop10RankLayout(1, rawWidth = 90f, rawHeight = 140f, cardHeightPx = 0f, strokePadPx = 7f))
    }
}
