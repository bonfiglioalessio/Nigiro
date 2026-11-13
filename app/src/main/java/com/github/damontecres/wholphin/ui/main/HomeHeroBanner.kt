@file:OptIn(ExperimentalCoilApi::class)

package com.github.damontecres.wholphin.ui.main

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import coil3.request.transitionFactory
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.RemoteHeroConfig
import com.github.damontecres.wholphin.ui.CrossFadeFactory
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
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
    // Static scrims, built once per background color
    val horizontalScrim =
        remember(background) {
            Brush.horizontalGradient(
                0f to background,
                0.38f to background.copy(alpha = 0.85f),
                0.75f to background.copy(alpha = 0f),
            )
        }
    val bottomScrim =
        remember(background) {
            Brush.verticalGradient(
                0.62f to background.copy(alpha = 0f),
                1f to background,
            )
        }
    val meta =
        remember(slide) {
            listOfNotNull(slide.yearText, slide.typeLabel, slide.ratingText?.let { "★ $it" }, runtimeText(slide.item))
                .joinToString(" · ")
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(NigiroDimens.HeroHeight)
                .onFocusChanged { focusState ->
                    isHeroFocused = focusState.hasFocus
                },
    ) {
        if (!slide.backdropUrl.isNullOrBlank()) {
            val context = LocalContext.current
            // Remembered so that recompositions (eg focus entering the hero) don't restart the load & fade
            val request =
                remember(slide.backdropUrl, config.crossfadeMs) {
                    ImageRequest
                        .Builder(context)
                        .data(slide.backdropUrl)
                        .useExistingImageAsPlaceholder(true)
                        .transitionFactory(
                            CrossFadeFactory(config.crossfadeMs.coerceIn(100, 600).milliseconds),
                        ).build()
                }
            AsyncImage(
                model = request,
                contentDescription = slide.title,
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

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = NigiroDimens.ContentStart + NigiroDimens.HeroBleedStart,
                        end = NigiroDimens.ContentEnd + NigiroDimens.HeroBleedEnd,
                        bottom = NigiroDimens.HeroBottomPadding,
                    ),
        ) {
            if (slide.badgeText.isNotBlank()) {
                Text(
                    text = slide.badgeText.uppercase(),
                    style = NigiroDimens.Badge,
                    color = NigiroPalette.PinkSoft,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NigiroPalette.Pink.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            var logoFailed by remember(slide.logoUrl) { mutableStateOf(false) }
            if (!slide.logoUrl.isNullOrBlank() && !logoFailed) {
                AsyncImage(
                    model = slide.logoUrl,
                    contentDescription = slide.title,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart,
                    onError = { logoFailed = true },
                    // Kept short so badge, logo, details, overview and buttons fit the hero's height
                    modifier = Modifier.size(width = 360.dp, height = 84.dp),
                )
            } else {
                Text(
                    text = slide.title,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(560.dp),
                )
            }
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = NigiroDimens.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!slide.overview.isNullOrBlank()) {
                Text(
                    text = slide.overview,
                    style = NigiroDimens.Body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(470.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
            ) {
                HeroButton(
                    onClick = { onPlay(slide.item) },
                    modifier = Modifier.focusRequester(watchFocusRequester),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(R.string.play), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                HeroButton(onClick = { onDetails(slide.item) }) {
                    Text(text = stringResource(R.string.hero_details), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                HeroButton(
                    onClick = { onToggleFavorite(slide.item, !slide.isFavorite) },
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
                    // Outline when not a favorite, filled pink when it is
                    Icon(
                        imageVector = if (slide.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription =
                            stringResource(
                                if (slide.isFavorite) R.string.hero_favorite_remove else R.string.hero_favorite_add,
                            ),
                        tint = if (slide.isFavorite) NigiroPalette.Pink else LocalContentColor.current,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (slides.size > 1) {
                    Spacer(Modifier.weight(1f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(slides.size) { idx ->
                            val active = idx == safeIndex
                            Box(
                                modifier =
                                    Modifier
                                        .size(width = if (active) 18.dp else 8.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (active) Color.White else Color.White.copy(alpha = 0.35f),
                                        ),
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 140.dp)
                    // Readable over bright backdrops
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(3.dp),
        ) {
            topEndContent()
        }
    }
}

/**
 * Hero button: translucent when idle, solid white with dark text when focused, so focus is obvious from the sofa
 */
@Composable
private fun HeroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
        colors =
            ButtonDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.10f),
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = NigiroPalette.Background,
            ),
        border =
            ButtonDefaults.border(
                border = Border(BorderStroke(2.dp, Color.White.copy(alpha = 0.35f)), shape = RoundedCornerShape(8.dp)),
                focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(8.dp)),
            ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        contentPadding = PaddingValues(horizontal = 22.dp),
        modifier = modifier.height(NigiroDimens.ButtonHeight),
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
