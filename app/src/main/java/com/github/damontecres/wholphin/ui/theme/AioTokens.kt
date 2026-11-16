package com.github.damontecres.wholphin.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design values of the AIOStreams app (dark mode), from its sources: packages/ui/theme.css (CSS variables),
 * packages/ui/src/modal/modal.tsx and packages/web/src/components (Tailwind classes). Only the values are
 * reused; the components are written for Compose.
 */
object AioTokens {
    /** --background (dark) */
    val Background = Color(0xFF070707)

    /** --paper (dark) = gray-950: dialogs */
    val Paper = Color(0xFF0B0B0B)

    /** gray-900: card placeholders */
    val Gray900 = Color(0xFF101010)

    /** gray-300: secondary text such as a version's description */
    val Gray300 = Color(0xFFCACACA)

    /** --foreground (dark) = gray-200 */
    val Foreground = Color(0xFFD1D1D1)

    /** --ring (dark) = brand-200: focus outlines */
    val Ring = Color(0xFFD4D0FF)

    /** brand-500: the accent */
    val Brand = Color(0xFF6152DF)

    /** --muted / --muted-highlight (dark) */
    val Muted = Color.White.copy(alpha = .4f)
    val MutedHighlight = Color.White.copy(alpha = .6f)

    /** --border (dark) and the 5% borders of dialogs and version rows */
    val Border = Color.White.copy(alpha = .1f)
    val BorderFaint = Color.White.copy(alpha = .05f)

    /** Version rows: bg-white/[0.03], hover/focus bg-white/[0.06] */
    val RowBackground = Color.White.copy(alpha = .03f)
    val RowBackgroundFocused = Color.White.copy(alpha = .06f)

    /** Modal overlay: bg-black/80 */
    const val OverlayAlpha = .8f

    /** --radius / rounded-xl / rounded-2xl */
    val Radius = 8.dp
    val RadiusXl = 12.dp
    val Radius2xl = 16.dp

    /** focus-visible:ring-2 */
    val RingWidth = 2.dp

    /** group-focus-visible:scale-[1.04] (posters) and scale-[1.03] (wide cards) */
    const val PosterFocusScale = 1.04f
    const val WideFocusScale = 1.03f

    /** The version picker's art: opacity-25 under a paper gradient */
    const val BannerArtAlpha = .25f
}
