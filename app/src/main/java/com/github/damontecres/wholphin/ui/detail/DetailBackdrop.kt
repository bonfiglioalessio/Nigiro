package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import org.jellyfin.sdk.model.api.ImageType

/**
 * Overrides for rows (title style and extra start inset) inside a page, so detail pages line up with the Home
 * without passing paddings through every row
 */
@Immutable
data class RowStyle(
    val startInset: Dp = 0.dp,
    val titleStyle: TextStyle? = null,
)

val LocalRowStyle = staticCompositionLocalOf { RowStyle() }

// Pages already start 20 dp after the sidebar (12 from the nav drawer + 8 from the rows), the Home 36 + 20
val DetailContentInset = NigiroDimens.ContentStart - 8.dp

private val DetailRowStyle = RowStyle(startInset = DetailContentInset, titleStyle = NigiroDimens.RowTitle)

/**
 * Detail page frame: a static backdrop of the item at the top right (one image, fixed scrims, no animation)
 * behind the content, with rows aligned to the Home
 */
@Composable
fun DetailPageFrame(
    item: BaseItem?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        DetailBackdrop(item)
        CompositionLocalProvider(LocalRowStyle provides DetailRowStyle) {
            content()
        }
    }
}

@Composable
private fun BoxScope.DetailBackdrop(item: BaseItem?) {
    val imageUrlService = LocalImageUrlService.current
    val url =
        remember(item?.id) {
            item?.let { imageUrlService.getItemImageUrl(it, ImageType.BACKDROP, fillWidth = 1280) }
        } ?: return
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                // The page is inset 16 dp from the right edge: bleed to it
                .offset(x = 16.dp)
                .fillMaxWidth(.74f)
                .fillMaxHeight(.78f),
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(LeftScrim))
        Box(Modifier.fillMaxSize().background(BottomScrim))
        Box(Modifier.fillMaxSize().background(TopScrim))
    }
}

private val LeftScrim =
    Brush.horizontalGradient(
        0f to NigiroPalette.BackgroundTop,
        0.35f to NigiroPalette.BackgroundTop.copy(alpha = 0.75f),
        0.7f to Color.Transparent,
    )

private val BottomScrim =
    Brush.verticalGradient(
        0.45f to Color.Transparent,
        1f to NigiroPalette.Background,
    )

private val TopScrim =
    Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.35f),
        0.18f to Color.Transparent,
    )
