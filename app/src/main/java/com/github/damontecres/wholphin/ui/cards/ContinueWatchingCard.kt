package com.github.damontecres.wholphin.ui.cards

import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.AioIcons
import com.github.damontecres.wholphin.ui.theme.pxSp
import android.graphics.Bitmap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import com.github.damontecres.wholphin.ui.theme.AioTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.main.episodeSubtitle
import com.github.damontecres.wholphin.ui.main.remainingLabel
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import kotlin.math.roundToInt

/**
 * Continue watching / next up: a 16:9 still (the episode's own image, or the movie's thumb/backdrop) with the
 * progress at the bottom; below it the title, "S02E01 · Name" for episodes and how much is left.
 */
@Composable
fun ContinueWatchingCard(
    item: BaseItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val imageUrlService = LocalImageUrlService.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val width = cardHeight * AspectRatios.WIDE
    val data = item?.data
    val isEpisode = item?.type == BaseItemKind.EPISODE
    val request =
        remember(item?.id, data?.imageTags, cardHeight) {
            val w = with(density) { width.toPx() }.roundToInt()
            val h = with(density) { cardHeight.toPx() }.roundToInt()
            val url =
                when {
                    item == null -> {
                        null
                    }

                    isEpisode && data?.imageTags?.containsKey(ImageType.PRIMARY) == true -> {
                        imageUrlService.getItemImageUrl(item, ImageType.PRIMARY, fillWidth = w, useSeriesForPrimary = false)
                    }

                    data?.imageTags?.containsKey(ImageType.THUMB) == true -> {
                        imageUrlService.getItemImageUrl(item, ImageType.THUMB, fillWidth = w)
                    }

                    else -> {
                        imageUrlService.getItemImageUrl(item, ImageType.BACKDROP, fillWidth = w)
                    }
                }
            url?.let {
                ImageRequest
                    .Builder(context)
                    .data(it)
                    .size(w, h)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build()
            }
        }
    val title = if (isEpisode) data?.seriesName ?: data?.name else data?.name
    val subtitle =
        if (isEpisode) {
            episodeSubtitle(data?.parentIndexNumber, data?.indexNumber, data?.name)
        } else {
            data?.productionYear?.toString()
        }
    val remaining = remember(item?.id, data?.userData) { remainingLabel(data?.runTimeTicks, data?.userData?.playbackPositionTicks) }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        verticalArrangement = Arrangement.spacedBy(px(4)),
        modifier = modifier.width(width),
    ) {
        // wide-card-image: rounded-xl bg-gray-900 ring-1 ring-white/5
        val shape = RoundedCornerShape(AioTokens.RadiusXl)
        val focused by interactionSource.collectIsFocusedAsState()
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (request != null) {
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // wide-card-play: bg-black/30 with a play icon while focused
                AioFocusShade(visible = focused, alpha = .3f) {
                    Icon(
                        imageVector = AioIcons.Play,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = .9f),
                        modifier = Modifier.size(px(48)),
                    )
                }
                AioProgressBar(data?.userData?.playedPercentage ?: 0.0)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(px(8)),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // wide-card-title: font-semibold; wide-card-meta: text-xs muted
            Text(
                text = title.orEmpty(),
                fontSize = pxSp(16),
                fontWeight = FontWeight.SemiBold,
                color = AioTokens.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            remaining?.let {
                Text(
                    text = it,
                    fontSize = pxSp(12),
                    color = AioTokens.Muted,
                    maxLines = 1,
                )
            }
        }
        subtitle?.let {
            // wide-card-subtitle: text-sm muted
            Text(
                text = it,
                fontSize = pxSp(14),
                color = AioTokens.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = px(4)),
            )
        }
    }
}
