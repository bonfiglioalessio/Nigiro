package com.github.damontecres.wholphin.ui.theme.colors

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme
import com.github.damontecres.wholphin.ui.theme.ThemeColors

/**
 * The AIOStreams app's dark palette (packages/ui/theme.css): near-black background, gray-950/900/800 surfaces,
 * brand (violet) accent and lavender focus. Dark only: the app is always used on a TV.
 *
 * The accent keeps its historical name "Pink" so the rest of the code needs no change: it is now brand violet.
 */
object NigiroPalette {
    /** Top of the page gradient: the same as the background, AIOStreams pages are a flat #070707 */
    val BackgroundTop = Color(0xFF070707)

    /** --background (dark) */
    val Background = Color(0xFF070707)

    /** --paper (dark) = gray-950 */
    val Surface = Color(0xFF0B0B0B)

    /** gray-900 */
    val SurfaceRaised = Color(0xFF101010)

    /** gray-800 */
    val Card = Color(0xFF1C1C1C)

    /** gray-100 for titles, gray-300 for secondary text, --muted-highlight for muted */
    val Text = Color(0xFFE1E1E1)
    val TextSecondary = Color(0xFFCACACA)
    val TextMuted = Color(0xFF9C9C9C)

    /** The accent (Nigiro beige, named Pink from the AIOStreams brand it replaces), a lighter one and the text on it */
    val Pink = Color(0xFFDCD3CA)
    val PinkSoft = Color(0xFFEDE7E1)
    val OnPink = Color(0xFF1A1714)

    val Disk = Color(0xFFDCD3CA)

    /** --ring (dark) = brand-200 */
    val Focus = Color(0xFFDCD3CA)
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
