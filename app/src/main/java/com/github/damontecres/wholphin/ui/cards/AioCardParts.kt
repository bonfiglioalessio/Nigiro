package com.github.damontecres.wholphin.ui.cards

import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.theme.AioTokens

/*
 * Pieces of the AIOStreams cards (packages/web/src/components/cards.tsx), written for Compose.
 */

/** ring-1 ring-white/5 at rest, the TV focus ring ([data-tv] ring-[3px], --ring) when focused */
@Composable
internal fun aioCardBorder(shape: Shape) =
    CardDefaults.border(
        border = Border(BorderStroke(px(1), AioTokens.BorderFaint), shape = shape),
        focusedBorder = Border(BorderStroke(px(3), AioTokens.Ring), shape = shape),
        pressedBorder = Border(BorderStroke(px(3), AioTokens.Ring), shape = shape),
    )

/** Watched: a small dark glass circle with a white check, top right (quieter than a brand-coloured disc) */
@Composable
internal fun BoxScope.AioWatchedMark() {
    CardMark(Alignment.TopEnd) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(px(16)))
    }
}

/** Favourite: the same circle with a red heart, top left */
@Composable
internal fun BoxScope.AioFavoriteMark() {
    CardMark(Alignment.TopStart) {
        Text(text = stringResource(R.string.fa_heart), fontFamily = FontAwesome, fontSize = pxSp(11), color = FavoriteRed)
    }
}

private val FavoriteRed = Color(0xFFFF4D6D)

@Composable
private fun BoxScope.CardMark(
    alignment: Alignment,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .align(alignment)
                .padding(px(8))
                .size(px(26))
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = .6f))
                .border(px(1), Color.White.copy(alpha = .18f), CircleShape),
    ) {
        content()
    }
}

/** unwatched-count: rounded-full bg-brand-500 px-2 py-0.5 text-xs font-semibold, top right */
@Composable
internal fun BoxScope.AioUnwatchedCount(text: String) {
    Text(
        text = text,
        fontSize = pxSp(12),
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(px(8))
                .clip(RoundedCornerShape(50))
                .background(AioTokens.Brand)
                .padding(horizontal = px(8), vertical = px(2)),
    )
}

/** ProgressBar: h-1 bg-black/60 across the bottom, filled bg-brand-500 (at least 2%) */
@Composable
internal fun BoxScope.AioProgressBar(percent: Double) {
    if (percent <= 0 || percent >= 100) return
    Box(
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(px(4))
                .background(Color.Black.copy(alpha = .6f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent.coerceIn(2.0, 100.0) / 100).toFloat())
                    .background(AioTokens.Brand),
        )
    }
}

/** poster-card-shade / wide-card-play: a black veil shown while focused */
@Composable
internal fun BoxScope.AioFocusShade(
    visible: Boolean,
    alpha: Float,
    content: @Composable BoxScope.() -> Unit = {},
) {
    if (!visible) return
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha)),
        content = content,
    )
}
