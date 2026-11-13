package com.github.damontecres.wholphin.ui.theme.colors

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme
import com.github.damontecres.wholphin.ui.theme.ThemeColors

/**
 * Warm grays of the Nigiro logo with its pink accent. Dark only: the app is always used on a TV.
 */
object NigiroPalette {
    val BackgroundTop = Color(0xFF2E2B29)
    val Background = Color(0xFF151413)
    val Surface = Color(0xFF1C1A19)
    val SurfaceRaised = Color(0xFF2A2826)
    val Card = Color(0xFF363331)
    val Text = Color(0xFFF4F1ED)
    val TextSecondary = Color(0xFFC9C2BA)
    val TextMuted = Color(0xFFB3ACA5)
    val Pink = Color(0xFFF2A0AE)
    val PinkSoft = Color(0xFFF7C3CC)
    val OnPink = Color(0xFF3A1820)
    val Disk = Color(0xFF9FEBC4)
    val Focus = Color(0xFFFFFFFF)
}

val NigiroThemeColors =
    object : ThemeColors {
        private val p = NigiroPalette

        override val darkSchemeMaterial =
            androidx.compose.material3.darkColorScheme(
                primary = p.Pink,
                onPrimary = p.OnPink,
                primaryContainer = p.SurfaceRaised,
                onPrimaryContainer = p.Text,
                secondary = p.TextSecondary,
                onSecondary = p.Background,
                secondaryContainer = p.SurfaceRaised,
                onSecondaryContainer = p.Text,
                tertiary = p.Pink,
                onTertiary = p.OnPink,
                tertiaryContainer = p.SurfaceRaised,
                onTertiaryContainer = p.Text,
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
                background = p.Background,
                onBackground = p.Text,
                surface = p.Surface,
                onSurface = p.Text,
                surfaceVariant = p.SurfaceRaised,
                onSurfaceVariant = p.TextSecondary,
                scrim = Color(0xFF000000),
                inverseSurface = p.Text,
                inverseOnSurface = p.Background,
                inversePrimary = p.Pink,
            )

        override val lightSchemeMaterial = darkSchemeMaterial

        override val darkScheme =
            darkColorScheme(
                primary = p.Pink,
                onPrimary = p.OnPink,
                primaryContainer = p.SurfaceRaised,
                onPrimaryContainer = p.Text,
                secondary = p.TextSecondary,
                onSecondary = p.Background,
                secondaryContainer = p.SurfaceRaised,
                onSecondaryContainer = p.Text,
                tertiary = p.Pink,
                onTertiary = p.OnPink,
                tertiaryContainer = p.SurfaceRaised,
                onTertiaryContainer = p.Text,
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
                background = p.Background,
                onBackground = p.Text,
                surface = p.Surface,
                onSurface = p.Text,
                surfaceVariant = p.SurfaceRaised,
                onSurfaceVariant = p.TextSecondary,
                scrim = Color(0xFF000000),
                inverseSurface = p.Text,
                inverseOnSurface = p.Background,
                inversePrimary = p.Pink,
                // Focus outline: plain white is the clearest cue from the sofa
                border = p.Focus,
            )

        override val lightScheme = darkScheme
    }
