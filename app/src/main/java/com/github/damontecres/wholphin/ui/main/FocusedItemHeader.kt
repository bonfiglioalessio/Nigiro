package com.github.damontecres.wholphin.ui.main

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import java.util.Locale

/**
 * Backdrop of the focused card, drawn full-width across the top ~60% with lightweight SrcOver gradient scrims
 * (avoids offscreen FBO / DstIn passes) and faded cleanly into the background. The image only loads once focus
 * rests on a card, so fast scrolling doesn't download an image per card.
 */
@Composable
fun FocusedItemBackdrop(
    item: BaseItem?,
    modifier: Modifier = Modifier,
) {
    val imageUrlService = LocalImageUrlService.current
    val context = LocalContext.current
    var backdropItem by remember { mutableStateOf<BaseItem?>(null) }
    LaunchedEffect(item?.id) {
        if (item == null) {
            backdropItem = null
        } else {
            delay(300)
            backdropItem = item
        }
    }
    val backdropRequest =
        remember(backdropItem?.id) {
            backdropItem?.let {
                imageUrlService.getItemImageUrl(it, ImageType.BACKDROP, fillWidth = 1920)?.let { url ->
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        .size(1920, 1080)
                        // Keep showing the previous backdrop until the new one is ready: no flash
                        .useExistingImageAsPlaceholder(true)
                        .build()
                }
            }
        } ?: return
    val background = MaterialTheme.colorScheme.background
    val horizontalScrim =
        remember(background) {
            Brush.horizontalGradient(
                0f to background,
                0.38f to background.copy(alpha = 0.85f),
                0.75f to Color.Transparent,
            )
        }
    val verticalScrim =
        remember(background) {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.35f),
                0.18f to Color.Transparent,
                0.55f to Color.Transparent,
                1f to background,
            )
        }
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = backdropRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth()
                    .fillMaxHeight(.60f)
                    .drawWithContent {
                        drawContent()
                        drawRect(horizontalScrim)
                        drawRect(verticalScrim)
                    },
        )
    }
}

/**
 * Clear logo (or title), details and overview of the focused card, with no background: drawn over its backdrop
 * in the top part of the screen, so the cards themselves can be poster only
 */
@Composable
fun FocusedItemInfo(
    item: BaseItem?,
    modifier: Modifier = Modifier,
    height: Dp = FocusedItemHeaderHeight,
    contentStart: Dp = NigiroDimens.ContentStart,
) {
    if (item == null) return
    val imageUrlService = LocalImageUrlService.current
    val details = remember(item.id) { focusedItemDetails(item) }
    var debouncedLogoItem by remember { mutableStateOf<BaseItem?>(null) }
    LaunchedEffect(item.id) {
        debouncedLogoItem = null
        delay(150)
        debouncedLogoItem = item
    }
    val logoUrl =
        remember(debouncedLogoItem?.id) {
            debouncedLogoItem
                ?.takeIf { it.data.imageTags?.containsKey(ImageType.LOGO) == true }
                ?.let { imageUrlService.getItemImageUrl(it, ImageType.LOGO, fillWidth = 520, fillHeight = 140) }
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .padding(start = contentStart, end = NigiroDimens.ContentEnd, bottom = 12.dp),
    ) {
        var logoFailed by remember(logoUrl) { mutableStateOf(false) }
        if (logoUrl != null && !logoFailed) {
            AsyncImage(
                model = logoUrl,
                contentDescription = details.title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onError = { logoFailed = true },
                modifier = Modifier.size(width = 280.dp, height = 64.dp),
            )
        } else {
            Text(
                text = details.title,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(560.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (details.meta.isNotBlank()) {
                Text(
                    text = details.meta,
                    style = NigiroDimens.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (details.onDisk) {
                OnDiskBadge()
            }
        }
        details.overview?.let {
            Text(
                text = it,
                style = NigiroDimens.Body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(540.dp),
            )
        }
    }
}

// Compact top region on 540 dp screen so focused row + next row title fit cleanly without bottom clipping
val FocusedItemHeaderHeight = 232.dp

@Composable
fun OnDiskBadge(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(R.string.on_disk),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = NigiroPalette.Disk,
        )
    }
}


private data class FocusedItemDetails(
    val title: String,
    val meta: String,
    val overview: String?,
    val onDisk: Boolean,
)

private fun focusedItemDetails(item: BaseItem): FocusedItemDetails {
    val data = item.data
    val isEpisode = item.type == BaseItemKind.EPISODE
    val title = (if (isEpisode) data.seriesName else null) ?: data.name.orEmpty()
    val episode =
        if (isEpisode && data.parentIndexNumber != null && data.indexNumber != null) {
            "S${data.parentIndexNumber} · E${data.indexNumber} ${data.name.orEmpty()}".trim()
        } else {
            null
        }
    val rating = data.communityRating?.let { String.format(Locale.ITALIAN, "★ %.1f", it) }
    val minutes = (data.runTimeTicks ?: 0L) / 600_000_000L
    val runtime =
        when {
            minutes <= 0 -> null
            minutes >= 60 -> "${minutes / 60} h ${minutes % 60} min"
            else -> "$minutes min"
        }
    val genres = data.genres?.take(3)?.takeIf { it.isNotEmpty() }?.joinToString(", ")
    val meta = listOfNotNull(episode, data.productionYear?.toString(), rating, genres, runtime).joinToString(" · ")
    return FocusedItemDetails(
        title = title,
        meta = meta,
        overview = data.overview?.takeIf { it.isNotBlank() },
        onDisk = item.isOnDisk(),
    )
}

/**
 * Whether the item is a downloaded file rather than a streamed (Gelato) one. Needs the PATH field.
 */
fun BaseItem.isOnDisk(): Boolean {
    val path = data.path ?: return false
    return !path.startsWith("gelato://") && !path.startsWith("http://") && !path.startsWith("https://") &&
        !path.endsWith(".strm", ignoreCase = true)
}
