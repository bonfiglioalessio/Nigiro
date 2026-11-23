package com.github.damontecres.wholphin.ui.cards

import android.content.Context
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.theme.AioTokens
import kotlin.math.ceil

private const val TOP_10_GLYPH_HEIGHT_FRACTION = 0.80f
private const val TOP_10_OVERLAP_ONE_FRACTION = 0.04f
private const val TOP_10_OVERLAP_FRACTION = 0.075f
private const val TOP_10_MIN_OFFSET_FRACTION = 0.22f
private const val REFERENCE_TEXT_SIZE_PX = 200f

private val UNFOCUSED_STROKE_WIDTH = 4.dp
private val FOCUSED_STROKE_WIDTH = 5.5.dp

private val UnfocusedFillColor = Color(0xFF0C0C0C)
private val UnfocusedStrokeColor = Color(0xFF6B6B6B)
private val FocusedFillColor = AioTokens.OnBrand
private val FocusedStrokeColor = AioTokens.Ring

@Volatile
private var cachedInterBold: Typeface? = null

private fun loadInterBoldTypeface(context: Context): Typeface =
    cachedInterBold
        ?: runCatching { ResourcesCompat.getFont(context.applicationContext, R.font.inter_bold) }
            .getOrNull()
            ?.also { cachedInterBold = it }
        ?: Typeface.DEFAULT_BOLD

internal data class Top10RankLayout(
    val scale: Float,
    val canvasWidthPx: Float,
    val posterOffsetPx: Float,
    val baselineBottomPx: Float,
)

/**
 * Computes geometry for a Netflix-style rank numeral tucked behind the left edge of a poster.
 */
internal fun computeTop10RankLayout(
    rank: Int,
    rawWidth: Float,
    rawHeight: Float,
    cardHeightPx: Float,
    strokePadPx: Float,
): Top10RankLayout? {
    if (rawWidth <= 0f || rawHeight <= 0f || cardHeightPx <= 0f) return null
    val scale = (cardHeightPx * TOP_10_GLYPH_HEIGHT_FRACTION) / rawHeight
    val canvasWidthPx = rawWidth * scale + strokePadPx * 2f
    val overlapPx = cardHeightPx * (if (rank == 1) TOP_10_OVERLAP_ONE_FRACTION else TOP_10_OVERLAP_FRACTION)
    val posterOffsetPx = (canvasWidthPx - overlapPx).coerceAtLeast(cardHeightPx * TOP_10_MIN_OFFSET_FRACTION)
    val baselineBottomPx = cardHeightPx - strokePadPx
    return Top10RankLayout(
        scale = scale,
        canvasWidthPx = canvasWidthPx,
        posterOffsetPx = posterOffsetPx,
        baselineBottomPx = baselineBottomPx,
    )
}

@Immutable
internal data class Top10RankGlyph(
    val path: Path,
    val canvasWidth: Dp,
    val posterOffset: Dp,
)

@Composable
internal fun rememberTop10RankGlyph(
    rank: Int,
    cardHeight: Dp,
): Top10RankGlyph? {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cardHeightPx = with(density) { cardHeight.toPx() }
    // Fixed max padding from focused stroke keeps layout identical across focus changes
    val strokePadPx = ceil(with(density) { (FOCUSED_STROKE_WIDTH / 2f).toPx() }) + 1f
    val typeface = remember(context) { loadInterBoldTypeface(context) }

    return remember(rank, cardHeightPx, strokePadPx, typeface) {
        val text = rank.toString()
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = REFERENCE_TEXT_SIZE_PX
                textScaleX = if (rank >= 10) 0.74f else 0.88f
                letterSpacing = if (rank >= 10) -0.06f else 0f
            }
        val rawPath = android.graphics.Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, rawPath)
        val bounds = RectF()
        rawPath.computeBounds(bounds, true)
        val layout =
            computeTop10RankLayout(
                rank = rank,
                rawWidth = bounds.width(),
                rawHeight = bounds.height(),
                cardHeightPx = cardHeightPx,
                strokePadPx = strokePadPx,
            ) ?: return@remember null
        val matrix =
            Matrix().apply {
                postTranslate(-bounds.left, -bounds.bottom)
                postScale(layout.scale, layout.scale)
                postTranslate(strokePadPx, layout.baselineBottomPx)
            }
        rawPath.transform(matrix)
        Top10RankGlyph(
            path = rawPath.asComposePath(),
            canvasWidth = with(density) { layout.canvasWidthPx.toDp() },
            posterOffset = with(density) { layout.posterOffsetPx.toDp() },
        )
    }
}

/**
 * Single-pass vector rank numeral drawn with an outer stroke and opaque fill.
 */
@Composable
internal fun Top10RankNumber(
    glyph: Top10RankGlyph,
    focused: Boolean,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val strokeWidth = if (focused) FOCUSED_STROKE_WIDTH else UNFOCUSED_STROKE_WIDTH
    val strokeColor = if (focused) FocusedStrokeColor else UnfocusedStrokeColor
    val fillColor = if (focused) FocusedFillColor else UnfocusedFillColor
    val strokeStyle =
        remember(density, strokeWidth) {
            Stroke(
                width = with(density) { strokeWidth.toPx() },
                join = StrokeJoin.Round,
            )
        }
    Canvas(modifier = modifier.size(glyph.canvasWidth, cardHeight)) {
        drawPath(
            path = glyph.path,
            color = strokeColor,
            style = strokeStyle,
        )
        drawPath(
            path = glyph.path,
            color = fillColor,
        )
    }
}
