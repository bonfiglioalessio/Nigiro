@file:OptIn(ExperimentalCoilApi::class)

package com.github.damontecres.wholphin.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
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
import com.github.damontecres.wholphin.ui.FontAwesome
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomeHeroBanner(
    slides: List<HeroSlide>,
    config: RemoteHeroConfig,
    watchFocusRequester: FocusRequester,
    onWatchNow: (BaseItem) -> Unit,
    onToggleFavorite: (BaseItem, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (slides.isEmpty()) return

    var currentIndex by remember(slides.size) { mutableIntStateOf(0) }
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

    val shape = RoundedCornerShape(14.dp)
    val surfaceColor = Color(0xFF101218)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(256.dp)
                .clip(shape)
                .background(surfaceColor)
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                .onFocusChanged { focusState ->
                    isHeroFocused = focusState.hasFocus
                },
    ) {
        if (!slide.backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(slide.backdropUrl)
                        .useExistingImageAsPlaceholder(true)
                        .transitionFactory(
                            CrossFadeFactory(config.crossfadeMs.coerceIn(100, 600).milliseconds),
                        ).build(),
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Lightweight linear scrims (no Offscreen compositing / radial gradients)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops =
                                arrayOf(
                                    0.0f to Color(0xEC0B0D12),
                                    0.48f to Color(0xC00B0D12),
                                    1.0f to Color(0x330B0D12),
                                ),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0.0f to Color.Transparent,
                                    0.65f to Color.Transparent,
                                    1.0f to Color(0xCC0B0D12),
                                ),
                        ),
                    ),
        )

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(0.62f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeroPill(
                        text = slide.badgeText,
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    slide.typeLabel?.let { typeLabel ->
                        HeroPill(text = typeLabel)
                    }
                    slide.yearText?.let { year ->
                        HeroPill(text = year)
                    }
                    slide.ratingText?.let { rating ->
                        HeroPill(text = "★ $rating")
                    }
                }

                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!slide.overview.isNullOrBlank()) {
                    Text(
                        text = slide.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { onWatchNow(slide.item) },
                        colors =
                            ButtonDefaults.colors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        modifier = Modifier.focusRequester(watchFocusRequester),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.hero_watch_now),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    OutlinedButton(
                        onClick = { onToggleFavorite(slide.item, !slide.isFavorite) },
                    ) {
                        Text(
                            text = stringResource(R.string.fa_heart),
                            fontFamily = FontAwesome,
                            color =
                                if (slide.isFavorite) {
                                    Color(0xFFFF5252)
                                } else {
                                    Color.White.copy(alpha = 0.7f)
                                },
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text =
                                stringResource(
                                    if (slide.isFavorite) {
                                        R.string.hero_favorite_remove
                                    } else {
                                        R.string.hero_favorite_add
                                    },
                                ),
                        )
                    }

                    if (slides.size > 1) {
                        OutlinedButton(
                            onClick = { currentIndex = (safeIndex + 1) % slides.size },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.hero_next_slide),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                if (slides.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(slides.size) { idx ->
                            val active = idx == safeIndex
                            Box(
                                modifier =
                                    Modifier
                                        .size(if (active) 10.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) Color.White else Color.White.copy(alpha = 0.35f),
                                        ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPill(
    text: String,
    backgroundColor: Color = Color.White.copy(alpha = 0.16f),
    contentColor: Color = Color.White,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}
