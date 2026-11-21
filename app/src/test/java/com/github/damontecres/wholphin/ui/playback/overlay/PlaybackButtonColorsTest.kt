package com.github.damontecres.wholphin.ui.playback.overlay

import androidx.compose.ui.graphics.Color
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.theme.colors.NigiroThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.OledThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.PurpleThemeColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PlaybackButtonColorsTest {
    @Test
    fun nigiroFocusedIconUsesDarkOnPrimaryWithHighContrast() {
        val scheme = NigiroThemeColors.darkScheme
        val focusedIconColor = playbackButtonFocusedContentColor(AppThemeColors.NIGIRO, scheme)
        val unrecognizedIconColor = playbackButtonFocusedContentColor(AppThemeColors.UNRECOGNIZED, scheme)

        assertEquals(NigiroPalette.OnPink, focusedIconColor)
        assertEquals(NigiroPalette.OnPink, unrecognizedIconColor)

        val ratio = contrastRatio(focusedIconColor, scheme.border)
        assertTrue("Expected contrast >= 7.0 on Nigiro focused button, got $ratio", ratio >= 7.0)
    }

    @Test
    fun oledFocusedIconUsesInverseOnSurfaceWithHighContrast() {
        val scheme = OledThemeColors.darkScheme
        val focusedIconColor = playbackButtonFocusedContentColor(AppThemeColors.OLED_BLACK, scheme)

        assertEquals(scheme.inverseOnSurface, focusedIconColor)
        assertEquals(Color.Black, focusedIconColor)

        val compositedBorder = compositeOverBlack(scheme.border)
        val ratio = contrastRatio(focusedIconColor, compositedBorder)
        assertTrue("Expected contrast >= 7.0 on OLED focused button, got $ratio", ratio >= 7.0)
    }

    @Test
    fun darkAccentThemesKeepOnSurfaceFocusedIcon() {
        val scheme = PurpleThemeColors.darkScheme
        val focusedIconColor = playbackButtonFocusedContentColor(AppThemeColors.PURPLE, scheme)

        assertEquals(scheme.onSurface, focusedIconColor)
    }

    private fun compositeOverBlack(color: Color): Color =
        Color(
            red = color.red * color.alpha,
            green = color.green * color.alpha,
            blue = color.blue * color.alpha,
            alpha = 1f,
        )

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }

    private fun contrastRatio(
        foreground: Color,
        background: Color,
    ): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
