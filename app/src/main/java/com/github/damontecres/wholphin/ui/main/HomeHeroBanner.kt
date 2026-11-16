@file:OptIn(ExperimentalCoilApi::class)

package com.github.damontecres.wholphin.ui.main

import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import coil3.request.transitionFactory
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.RemoteHeroConfig
import com.github.damontecres.wholphin.ui.CrossFadeFactory
import com.github.damontecres.wholphin.ui.cards.noButtonGlow
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomeHeroBanner(
    slides: List<HeroSlide>,
    config: RemoteHeroConfig,
    watchFocusRequester: FocusRequester,
    onPlay: (BaseItem) -> Unit,
    onDetails: (BaseItem) -> Unit,
    onToggleFavorite: (BaseItem, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    topEndContent: @Composable () -> Unit = {},
) {
    if (slides.isEmpty()) return

    // Saveable so the hero doesn't restart from the first slide after scrolling it out of view
    var currentIndex by rememberSaveable(slides.size) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, slides.lastIndex)
    val slide = slides[safeIndex]

    var isHeroFocused by remember { mutableStateOf(false) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.STARTED)

    LaunchedEffect(slides.size, config.enabled, config.intervalMs, isHeroFocused, isResumed) {
        if (config.enabled && slides.size > 1 && !isHeroFocused && isResumed) {
            val interval = config.intervalMs.coerceAtLeast(3_000L)
            while (true) {
                delay(interval)
                currentIndex = (currentIndex + 1) % slides.size
            }
        }
    }

    val background = MaterialTheme.colorScheme.background
    // AIOStreams hero-shade (packages/web/src/components/hero.tsx):
    // side: from-background via-background/70 at 35% to-transparent;
    // bottom: on the lower 2/3, from-background via-background/60 to-transparent
    val sideShade =
        remember(background) {
            Brush.horizontalGradient(
                0f to background,
                0.35f to background.copy(alpha = 0.7f),
                1f to background.copy(alpha = 0f),
            )
        }
    val bottomShade =
        remember(background) {
            Brush.verticalGradient(
                (1f / 3f) to background.copy(alpha = 0f),
                (2f / 3f) to background.copy(alpha = 0.6f),
                1f to background,
            )
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(NigiroDimens.AioHeroHeight)
                .onFocusChanged { focusState ->
                    isHeroFocused = focusState.hasFocus
                },
    ) {
        if (!slide.backdropUrl.isNullOrBlank()) {
            val context = LocalContext.current
            val enableAnimations = LocalInterfaceCustomization.current.enableAnimations
            // Remembered so that recompositions (eg focus entering the hero) don't restart the load & fade
            val request =
                remember(context, slide.backdropUrl, config.crossfadeMs, enableAnimations) {
                    ImageRequest
                        .Builder(context)
                        .data(slide.backdropUrl)
                        // Cap hero backdrop decode size and use RGB_565 (opaque) for 50% lighter GPU uploads
                        .size(960, 540)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .useExistingImageAsPlaceholder(true)
                        .apply {
                            if (enableAnimations) {
                                transitionFactory(
                                    CrossFadeFactory(config.crossfadeMs.coerceIn(100, 700).milliseconds),
                                )
                            }
                        }.build()
                }
            AsyncImage(
                model = request,
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(sideShade),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(bottomShade),
        )

        // hero-content: bottom-left, space-y-4, pb-8 (14 on large screens), max-w-3xl
        Column(
            verticalArrangement = Arrangement.spacedBy(px(16), Alignment.Bottom),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = NigiroDimens.ContentStart + NigiroDimens.HeroBleedStart,
                        end = NigiroDimens.ContentEnd + NigiroDimens.HeroBleedEnd,
                        bottom = px(56),
                    ),
        ) {
            var logoFailed by remember(slide.logoUrl) { mutableStateOf(false) }
            if (!slide.logoUrl.isNullOrBlank() && !logoFailed) {
                val context = LocalContext.current
                val logoRequest =
                    remember(context, slide.logoUrl) {
                        ImageRequest
                            .Builder(context)
                            .data(slide.logoUrl)
                            .size(768, 160)
                            .build()
                    }
                // hero-logo: max-h-20 max-w-[min(24rem,75%)] object-contain object-left
                AsyncImage(
                    model = logoRequest,
                    contentDescription = slide.title,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart,
                    onError = { logoFailed = true },
                    modifier = Modifier.size(width = px(384), height = px(112)),
                )
            } else {
                // hero-title: text-3xl font-bold leading-tight, 2 lines
                Text(
                    text = slide.title,
                    fontSize = pxSp(30),
                    lineHeight = pxSp(36),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(px(560)),
                )
            }
            HeroMeta(slide)
            if (!slide.overview.isNullOrBlank()) {
                // hero-overview: max-w-2xl text-gray-300, 2 lines (text-base from sm up)
                Text(
                    text = slide.overview,
                    fontSize = pxSp(16),
                    lineHeight = pxSp(22),
                    color = AioTokens.Gray300,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(px(560)),
                )
            }
            // hero-actions: flex gap-2
            Row(
                horizontalArrangement = Arrangement.spacedBy(px(8)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroButton(
                    primary = true,
                    onClick = { onPlay(slide.item) },
                    modifier = Modifier.focusRequester(watchFocusRequester),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(px(20)),
                    )
                    Spacer(Modifier.width(px(8)))
                    Text(text = stringResource(R.string.play), fontWeight = FontWeight.SemiBold, fontSize = pxSp(14))
                }
                HeroButton(
                    primary = false,
                    onClick = { onDetails(slide.item) },
                    modifier =
                        Modifier.onPreviewKeyEvent { event ->
                            // Right on the last button shows the next slide
                            if (slides.size > 1 &&
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionRight
                            ) {
                                currentIndex = (safeIndex + 1) % slides.size
                                true
                            } else {
                                false
                            }
                        },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(px(20)),
                    )
                    Spacer(Modifier.width(px(8)))
                    Text(text = stringResource(R.string.hero_more_info), fontWeight = FontWeight.SemiBold, fontSize = pxSp(14))
                }
            }
        }

        // hero-dots: absolute bottom-6 right-4, gap-1.5; h-1.5 rounded-full, current w-6 white, others w-1.5 white/40
        if (slides.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(px(6)),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = px(56), end = px(16) + NigiroDimens.HeroBleedEnd),
            ) {
                repeat(slides.size) { idx ->
                    val active = idx == safeIndex
                    Box(
                        modifier =
                            Modifier
                                .size(width = if (active) px(24) else px(6), height = px(6))
                                .clip(RoundedCornerShape(50))
                                .background(if (active) Color.White else Color.White.copy(alpha = 0.4f)),
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = px(14), end = px(140))
                    // Readable over bright backdrops
                    .clip(RoundedCornerShape(px(18)))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(px(3)),
        ) {
            topEndContent()
        }
    }
}

/**
 * hero-meta: text-sm text-gray-200, parts separated by a gray-500 "•": year, ★ score, 🍅 critics, genres
 */
@Composable
private fun HeroMeta(slide: HeroSlide) {
    val data = slide.item.data
    val parts =
        remember(slide) {
            listOfNotNull(
                slide.yearText,
                data.communityRating?.takeIf { it > 0f }?.let { "★ " + String.format(java.util.Locale.ROOT, "%.1f", it) },
                data.criticRating?.takeIf { it > 0f }?.let { "\uD83C\uDF45 ${it.toInt()}%" },
                data.genres?.take(3)?.takeIf { it.isNotEmpty() }?.joinToString(", "),
            )
        }
    if (parts.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(px(10)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                Text(text = "•", fontSize = pxSp(14), color = Color(0xFF5A5A5A))
            }
            if (part.startsWith("★ ")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFACC15),
                        modifier = Modifier.size(px(16)),
                    )
                    Spacer(Modifier.width(px(4)))
                    Text(text = part.removePrefix("★ "), fontSize = pxSp(14), color = AioTokens.Foreground)
                }
            } else {
                Text(text = part, fontSize = pxSp(14), color = AioTokens.Foreground, maxLines = 1)
            }
        }
    }
}

/**
 * AIOStreams Button, size md (h-10), rounded-full. On the TV only the focused button is white with black text (and a
 * little larger): with play always white, focus on another button was hard to see. Unfocused, play is a frosted
 * pill and the rest are outlined.
 */
@Composable
private fun HeroButton(
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val pill = RoundedCornerShape(50)
    val container = if (primary) Color.White.copy(alpha = 0.18f) else Color.Transparent
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(pill),
        colors =
            ButtonDefaults.colors(
                containerColor = container,
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = Color.Black,
                pressedContainerColor = Color.White,
                pressedContentColor = Color.Black,
            ),
        border =
            ButtonDefaults.border(
                border =
                    Border(
                        BorderStroke(px(1), if (primary) Color.Transparent else Color.White.copy(alpha = 0.3f)),
                        shape = pill,
                    ),
                focusedBorder = Border(BorderStroke(px(1), Color.Transparent), shape = pill),
            ),
        scale = ButtonDefaults.scale(focusedScale = 1.06f),
        glow = noButtonGlow(),
        contentPadding = PaddingValues(horizontal = px(16)),
        modifier = modifier.height(px(40)),
    ) {
        // The button has a fixed height, so the content has to fill it to be centered
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
            content = content,
        )
    }
}

private fun runtimeText(item: BaseItem): String? {
    val minutes = (item.data.runTimeTicks ?: return null) / 600_000_000L
    if (minutes <= 0) return null
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "$hours h $rest min" else "$rest min"
}
