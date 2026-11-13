package com.github.damontecres.wholphin.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import java.util.Locale

/**
 * A small hero for the focused card: its backdrop, clear logo (or title), details and overview,
 * so the cards themselves can be poster only
 */
@Composable
fun FocusedItemHeader(
    item: BaseItem?,
    modifier: Modifier = Modifier,
    height: Dp = FocusedItemHeaderHeight,
    contentStart: Dp = NigiroDimens.ContentStart,
) {
    val background = MaterialTheme.colorScheme.background
    val horizontalScrim =
        remember(background) {
            Brush.horizontalGradient(
                0f to background,
                0.35f to background.copy(alpha = 0.85f),
                0.75f to background.copy(alpha = 0f),
            )
        }
    val bottomScrim =
        remember(background) {
            Brush.verticalGradient(
                0.55f to background.copy(alpha = 0f),
                1f to background,
            )
        }
    val imageUrlService = LocalImageUrlService.current
    val context = LocalContext.current

    // Text follows focus right away; the backdrop only loads once focus rests on a card, so fast scrolling
    // doesn't download an image per card
    var backdropItem by remember { mutableStateOf<BaseItem?>(null) }
    LaunchedEffect(item?.id) {
        delay(300)
        backdropItem = item
    }
    val backdropRequest =
        remember(backdropItem?.id) {
            backdropItem?.let {
                imageUrlService.getItemImageUrl(it, ImageType.BACKDROP, fillWidth = 960)?.let { url ->
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        // Keep showing the previous backdrop until the new one is ready: no flash
                        .useExistingImageAsPlaceholder(true)
                        .build()
                }
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .background(background),
    ) {
        if (backdropRequest != null) {
            AsyncImage(
                model = backdropRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(horizontalScrim),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(bottomScrim),
        )
        if (item == null) return@Box
        val details = remember(item) { focusedItemDetails(item) }
        val logoUrl =
            remember(item) {
                if (item.data.imageTags?.containsKey(ImageType.LOGO) == true) {
                    imageUrlService.getItemImageUrl(item, ImageType.LOGO, fillWidth = 520, fillHeight = 140)
                } else {
                    null
                }
            }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = contentStart, end = NigiroDimens.ContentEnd, bottom = 18.dp),
        ) {
            var logoFailed by remember(logoUrl) { mutableStateOf(false) }
            if (logoUrl != null && !logoFailed) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = details.title,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart,
                    onError = { logoFailed = true },
                    modifier = Modifier.size(width = 260.dp, height = 56.dp),
                )
            } else {
                Text(
                    text = details.title,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
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
                    modifier = Modifier.width(520.dp),
                )
            }
        }
    }
}

val FocusedItemHeaderHeight = 196.dp

@Composable
fun OnDiskBadge(modifier: Modifier = Modifier) {
    Text(
        text = "SU DISCO",
        style = NigiroDimens.Badge,
        color = NigiroPalette.Disk,
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NigiroPalette.Disk.copy(alpha = 0.18f))
                .padding(horizontal = 9.dp, vertical = 3.dp),
    )
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
    val meta = listOfNotNull(episode, data.productionYear?.toString(), rating, runtime).joinToString(" · ")
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
