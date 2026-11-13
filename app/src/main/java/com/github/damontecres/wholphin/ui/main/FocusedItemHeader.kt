package com.github.damontecres.wholphin.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.Locale

/**
 * Title, details and overview of the focused card, so the cards themselves can be poster only
 */
@Composable
fun FocusedItemHeader(
    item: BaseItem?,
    modifier: Modifier = Modifier,
    height: Dp = FocusedItemHeaderHeight,
    contentStart: Dp = NigiroDimens.ContentStart,
) {
    val background =
        remember {
            Brush.verticalGradient(
                0f to NigiroPalette.BackgroundTop,
                0.88f to NigiroPalette.BackgroundTop,
                1f to NigiroPalette.BackgroundTop.copy(alpha = 0f),
            )
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .background(background)
                .padding(start = contentStart, end = NigiroDimens.ContentEnd, top = 26.dp),
    ) {
        if (item == null) return@Column
        val details = remember(item) { focusedItemDetails(item) }
        Text(
            text = details.title,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                modifier = Modifier.width(560.dp),
            )
        }
    }
}

val FocusedItemHeaderHeight = 140.dp

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
