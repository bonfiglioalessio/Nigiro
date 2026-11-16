package com.github.damontecres.wholphin.ui.cards

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
        border = Border(BorderStroke(1.dp, AioTokens.BorderFaint), shape = shape),
        focusedBorder = Border(BorderStroke(3.dp, AioTokens.Ring), shape = shape),
        pressedBorder = Border(BorderStroke(3.dp, AioTokens.Ring), shape = shape),
    )

/** WatchedMark: size-6 rounded-full bg-brand-500, white check, top right */
@Composable
internal fun BoxScope.AioWatchedMark() {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(AioTokens.Brand),
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** unwatched-count: rounded-full bg-brand-500 px-2 py-0.5 text-xs font-semibold, top right */
@Composable
internal fun BoxScope.AioUnwatchedCount(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(AioTokens.Brand)
                .padding(horizontal = 8.dp, vertical = 2.dp),
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
                .height(4.dp)
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
