package com.github.damontecres.wholphin.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One CSS pixel of the AIOStreams app on the TV. Its WebView lays pages out 1280 px wide on the 960x540 dp screen
 * (so the `lg` Tailwind variants apply): measured on the emulator, a 40 px button is 30 dp and the 224 px movie
 * poster 168 dp. Write sizes as the CSS values and convert them with [px] / [pxSp].
 */
const val AIO_PX = 0.75f

fun px(cssPx: Number): Dp = (cssPx.toFloat() * AIO_PX).dp

fun pxSp(cssPx: Number): TextUnit = (cssPx.toFloat() * AIO_PX).sp

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

    /** Focus outlines: Nigiro's beige (the icon's), not AIOStreams' lilac */
    val Ring = Color(0xFFDCD3CA)

    /** The accent: Nigiro's beige, so the palette is black, greys, white and beige */
    val Brand = Color(0xFFDCD3CA)

    /** Text and icons on the beige accent */
    val OnBrand = Color(0xFF1A1714)

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
    val Radius = px(8)
    val RadiusXl = px(12)
    val Radius2xl = px(16)

    /** focus-visible:ring-2, and the TV ring ([data-tv] ring-[3px]) */
    val RingWidth = px(2)
    val TvRingWidth = px(3)

    /** group-focus-visible:scale-[1.04] (posters) and scale-[1.03] (wide cards) */
    const val PosterFocusScale = 1.04f
    const val WideFocusScale = 1.03f

    /** The version picker's art: opacity-25 under a paper gradient */
    const val BannerArtAlpha = .25f
}
