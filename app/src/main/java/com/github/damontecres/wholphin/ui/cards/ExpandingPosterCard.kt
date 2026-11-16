package com.github.damontecres.wholphin.ui.cards

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.AppColors
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.main.cardMeta
import com.github.damontecres.wholphin.ui.theme.AioTokens
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.ImageType
import kotlin.math.roundToInt

/** How long focus has to rest on a poster before it opens up: scrolling through a row doesn't relayout it */
private const val EXPAND_DELAY_MS = 280L

/**
 * A poster with its title and year below. When focus rests on it, it widens to 16:9 and shows the backdrop,
 * logo (or title), details, genres and overview, like the focused cards of AIOStreams. Being a row item, the
 * widened card pushes the following ones to the right and is scrolled fully into view: never off screen.
 */
@Composable
fun ExpandingPosterCard(
    item: BaseItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
    cornerText: String? = null,
    onDisk: Boolean = false,
    // false: a plain poster card (AIOStreams PosterCard), eg for "More like this"
    expandable: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(focused, item?.id) {
        if (!focused || !expandable) {
            expanded = false
        } else {
            delay(EXPAND_DELAY_MS)
            expanded = true
        }
    }
    val posterWidth = cardHeight * AspectRatios.TALL
    val wideWidth = cardHeight * AspectRatios.WIDE
    val width = if (expanded) wideWidth else posterWidth

    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(expanded) {
        if (expanded) {
            // Once the wider card is laid out: the row scrolls so it is entirely visible
            delay(16)
            runCatching { bringIntoView.bringIntoView() }
        }
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val data = item?.data
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            modifier
                .width(width)
                .bringIntoViewRequester(bringIntoView),
    ) {
        // poster-card-image: rounded-lg bg-gray-900 ring-1 ring-white/5
        val shape = RoundedCornerShape(AioTokens.Radius)
        Card(
            onClick = { currentOnClick() },
            onLongClick = { currentOnLongClick() },
            interactionSource = interactionSource,
            shape = CardDefaults.shape(shape),
            border = aioCardBorder(shape),
            scale = cardScale(focusedScale = 1f),
            glow = noCardGlow(),
            colors = CardDefaults.colors(containerColor = AioTokens.Gray900),
            modifier = Modifier.size(width, cardHeight),
        ) {
            if (expanded && item != null) {
                ExpandedContent(item, wideWidth, cardHeight)
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    PosterContent(item, posterWidth, cardHeight, cornerText, onDisk)
                    // poster-card-shade: bg-black/20 while focused
                    AioFocusShade(visible = focused, alpha = .2f)
                }
            }
        }
        // poster-card-text: text-sm font-medium, then text-xs muted
        Text(
            text = item?.title ?: item?.name.orEmpty(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AioTokens.Foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        data?.productionYear?.let { year ->
            Text(
                text = year.toString(),
                fontSize = 12.sp,
                color = AioTokens.Muted,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun rememberImageRequest(
    url: String?,
    width: Dp,
    height: Dp,
): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(url, width, height) {
        url?.let {
            val w = with(density) { width.toPx() }.roundToInt()
            val h = with(density) { height.toPx() }.roundToInt()
            ImageRequest
                .Builder(context)
                .data(it)
                .size(w, h)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build()
        }
    }
}

@Composable
private fun PosterContent(
    item: BaseItem?,
    width: Dp,
    height: Dp,
    cornerText: String?,
    onDisk: Boolean,
) {
    val imageUrlService = LocalImageUrlService.current
    val density = LocalDensity.current
    val url =
        remember(item?.id, item?.data?.imageTags) {
            item?.imageUrlOverride
                ?: imageUrlService.getItemImageUrl(
                    item,
                    ImageType.PRIMARY,
                    fillHeight = with(density) { height.roundToPx() },
                )
        }
    val request = rememberImageRequest(url, width, height)
    val played = item?.data?.userData?.played == true
    val playPercent = item?.data?.userData?.playedPercentage ?: 0.0
    Box(modifier = Modifier.fillMaxSize()) {
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            NameFallback(item?.name)
        }
        if (played && (playPercent <= 0 || playPercent >= 100)) {
            AioWatchedMark()
        } else if (cornerText != null) {
            AioUnwatchedCount(cornerText)
        }
        if (onDisk) {
            OnDiskCardBadge(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
        }
        AioProgressBar(playPercent)
    }
}

@Composable
private fun ExpandedContent(
    item: BaseItem,
    width: Dp,
    height: Dp,
) {
    val imageUrlService = LocalImageUrlService.current
    val data = item.data
    val backdropUrl =
        remember(item.id) {
            imageUrlService.getItemImageUrl(item, ImageType.BACKDROP, fillWidth = 640)
                ?: imageUrlService.getItemImageUrl(item, ImageType.PRIMARY, fillWidth = 640)
        }
    val logoUrl =
        remember(item.id) {
            item
                .takeIf { data.imageTags?.containsKey(ImageType.LOGO) == true }
                ?.let { imageUrlService.getItemImageUrl(it, ImageType.LOGO, fillWidth = 360, fillHeight = 100) }
        }
    val meta =
        remember(item.id) {
            cardMeta(
                year = data.productionYear,
                communityRating = data.communityRating,
                criticRating = data.criticRating,
                runTimeTicks = data.runTimeTicks,
                officialRating = data.officialRating,
            )
        }
    val genres = remember(item.id) { data.genres?.take(3)?.joinToString(" · ") }
    val backdrop = rememberImageRequest(backdropUrl, width, height)
    val scrim =
        remember {
            Brush.verticalGradient(
                0f to Color.Transparent,
                .35f to Color.Black.copy(alpha = .25f),
                1f to Color.Black.copy(alpha = .9f),
            )
        }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AioTokens.Gray900),
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(scrim),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            var logoFailed by remember(logoUrl) { mutableStateOf(false) }
            if (logoUrl != null && !logoFailed) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart,
                    onError = { logoFailed = true },
                    modifier =
                        Modifier
                            .fillMaxWidth(.6f)
                            .height(height * .26f),
                )
            } else {
                Text(
                    text = item.title ?: item.name.orEmpty(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (meta.isNotEmpty()) {
                Text(text = meta, fontSize = 12.sp, color = Color.White.copy(alpha = .9f), maxLines = 1)
            }
            genres?.takeIf { it.isNotEmpty() }?.let {
                Text(text = it, fontSize = 12.sp, color = Color.White.copy(alpha = .7f), maxLines = 1)
            }
            data.overview?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White.copy(alpha = .9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AioProgressBar(data.userData?.playedPercentage ?: 0.0)
    }
}



@Composable
private fun BoxScope.NameFallback(name: String?) {
    Text(
        text = name.orEmpty(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleMedium,
        modifier =
            Modifier
                .align(Alignment.Center)
                .padding(12.dp),
    )
}
