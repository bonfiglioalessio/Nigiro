package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import org.jellyfin.sdk.model.api.ImageType

/**
 * Overrides for rows (title style and extra start inset) inside a page, so detail pages line up with the Home
 * without passing paddings through every row
 */
@Immutable
data class RowStyle(
    val startInset: Dp = 0.dp,
    val titleStyle: TextStyle? = null,
    // Start the cards where the row title starts
    val alignContentToTitle: Boolean = false,
)

val LocalRowStyle = staticCompositionLocalOf { RowStyle() }

// Pages already start 20 dp after the sidebar (12 from the nav drawer + 8 from the rows), the Home 36 + 20
val DetailContentInset = NigiroDimens.ContentStart - 8.dp

private val DetailRowStyle =
    RowStyle(startInset = DetailContentInset, titleStyle = NigiroDimens.RowTitle, alignContentToTitle = true)

private const val MIN_BACKDROP_ALPHA = 0.3f
private const val BACKDROP_FADE_PX = 600f

/**
 * Backdrop opacity for a page scrolled by a [LazyListState] whose first item is the header: it fades as the
 * header scrolls away, so the rows below aren't drawn over a busy image
 */
fun LazyListState.headerBackdropAlpha(): Float =
    if (firstVisibleItemIndex > 0) {
        MIN_BACKDROP_ALPHA
    } else {
        (1f - firstVisibleItemScrollOffset / BACKDROP_FADE_PX).coerceIn(MIN_BACKDROP_ALPHA, 1f)
    }

fun ScrollState.headerBackdropAlpha(): Float = (1f - value / BACKDROP_FADE_PX).coerceIn(MIN_BACKDROP_ALPHA, 1f)

/**
 * Detail page frame: a static backdrop of the item at the top right (one image, fixed scrims, no animation)
 * behind the content, with rows aligned to the Home
 */
@Composable
fun DetailPageFrame(
    item: BaseItem?,
    modifier: Modifier = Modifier,
    // Read while drawing only: scrolling doesn't recompose the page
    backdropAlpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        DetailBackdrop(item, backdropAlpha)
        CompositionLocalProvider(LocalRowStyle provides DetailRowStyle) {
            content()
        }
    }
}

@Composable
private fun BoxScope.DetailBackdrop(
    item: BaseItem?,
    alpha: () -> Float,
) {
    val imageUrlService = LocalImageUrlService.current
    val url =
        remember(item?.id) {
            item?.let { imageUrlService.getItemImageUrl(it, ImageType.BACKDROP, fillWidth = 1280) }
        } ?: return
    MaskedBackdrop(
        model = url,
        alpha = alpha,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                // The page is inset 16 dp from the right edge: bleed to it
                .offset(x = 16.dp)
                .fillMaxWidth(.74f)
                .fillMaxHeight(.78f),
    )
}

/**
 * A backdrop image faded to the page background along its left and bottom edges using single-pass SrcOver
 * scrims (avoids offscreen FBO allocation and BlendMode.DstIn compositing on TV GPUs)
 */
@Composable
fun MaskedBackdrop(
    model: Any?,
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
    alignment: Alignment = Alignment.TopCenter,
) {
    val background = MaterialTheme.colorScheme.background
    val leftFade =
        remember(background) {
            Brush.horizontalGradient(
                0f to background,
                0.5f to background.copy(alpha = 0.2f),
                1f to Color.Transparent,
            )
        }
    val bottomFade =
        remember(background) {
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to background.copy(alpha = 0.15f),
                1f to background,
            )
        }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = alignment,
        modifier =
            modifier
                .graphicsLayer {
                    this.alpha = alpha()
                }.drawWithContent {
                    drawContent()
                    drawRect(TopScrim)
                    drawRect(leftFade)
                    drawRect(bottomFade)
                },
    )
}

// Keeps the clock readable on bright images
private val TopScrim =
    Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.35f),
        0.18f to Color.Transparent,
    )
