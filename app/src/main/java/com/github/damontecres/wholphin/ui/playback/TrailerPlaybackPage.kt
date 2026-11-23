package com.github.damontecres.wholphin.ui.playback

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.services.TrailerService
import com.github.damontecres.wholphin.ui.AioIcons
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.components.TextButton
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TRAILER_SEEK_MS = 10_000L
private const val TRAILER_OSD_TIMEOUT_MS = 3_500L

@OptIn(UnstableApi::class)
@Composable
fun TrailerPlaybackPage(
    title: String,
    url: String,
    modifier: Modifier = Modifier,
    viewModel: TrailerPlaybackViewModel =
        hiltViewModel<TrailerPlaybackViewModel, TrailerPlaybackViewModel.Factory>(
            creationCallback = { it.create(title, url) },
        ),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val player = viewModel.player

    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            player.pause()
        }
    }

    var externalLaunchFailed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state is TrailerPlaybackState.FallbackToExternal) {
            val launched = TrailerService.openExternal(context, url)
            if (launched) {
                Toast.makeText(context, R.string.trailer_opening_external, Toast.LENGTH_SHORT).show()
                viewModel.navigationManager.goBack()
            } else {
                externalLaunchFailed = true
            }
        } else {
            externalLaunchFailed = false
        }
    }

    BackHandler {
        player.stop()
        viewModel.navigationManager.goBack()
    }

    if (state is TrailerPlaybackState.FallbackToExternal && externalLaunchFailed) {
        TrailerFallbackError(
            title = title,
            reason = (state as TrailerPlaybackState.FallbackToExternal).reason,
            onRetry = viewModel::retry,
            onBack = { viewModel.navigationManager.goBack() },
            modifier = modifier,
        )
        return
    }

    val presentationState = rememberPresentationState(player, false)
    val playbackState by rememberPlayerState(player)
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    LaunchedEffect(player) {
        player.listenTo(Player.EVENT_IS_PLAYING_CHANGED) {
            isPlaying = player.isPlaying
        }
    }

    var showBuffering by remember { mutableStateOf(false) }
    LaunchedEffect(playbackState) {
        if (playbackState == PlayerState.BUFFERING) {
            delay(250)
            showBuffering = true
        } else {
            showBuffering = false
        }
    }

    var osdToken by remember { mutableLongStateOf(0L) }
    var osdVisible by remember { mutableStateOf(true) }
    LaunchedEffect(osdToken, isPlaying, state) {
        if (state !is TrailerPlaybackState.Playing) {
            osdVisible = false
        } else if (!isPlaying) {
            osdVisible = true
        } else {
            osdVisible = true
            delay(TRAILER_OSD_TIMEOUT_MS)
            osdVisible = false
        }
    }

    var positionMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player, osdVisible) {
        if (!osdVisible) return@LaunchedEffect
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val isToggleKey =
                        isEnterKey(event) ||
                            event.key == Key.MediaPlayPause ||
                            event.key == Key.MediaPlay ||
                            event.key == Key.MediaPause
                    val isLeft = event.key == Key.DirectionLeft || event.key == Key.MediaRewind
                    val isRight = event.key == Key.DirectionRight || event.key == Key.MediaFastForward
                    val isReveal = event.key == Key.DirectionUp || event.key == Key.DirectionDown
                    val isStop = event.key == Key.MediaStop

                    if (isToggleKey || isLeft || isRight || isReveal || isStop) {
                        if (event.type == KeyEventType.KeyUp) {
                            when {
                                isStop -> {
                                    player.stop()
                                    viewModel.navigationManager.goBack()
                                }

                                isToggleKey -> {
                                    Util.handlePlayPauseButtonAction(player)
                                    osdToken++
                                }

                                isLeft -> {
                                    val target = (player.currentPosition - TRAILER_SEEK_MS).coerceAtLeast(0L)
                                    player.seekTo(target)
                                    positionMs = target
                                    osdToken++
                                }

                                isRight -> {
                                    val maxPos = player.duration.coerceAtLeast(0L)
                                    val target = (player.currentPosition + TRAILER_SEEK_MS).let { if (maxPos > 0) it.coerceAtMost(maxPos) else it }
                                    player.seekTo(target)
                                    positionMs = target
                                    osdToken++
                                }

                                isReveal -> {
                                    osdToken++
                                }
                            }
                        }
                        true
                    } else {
                        false
                    }
                },
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .resizeWithContentScale(ContentScale.Fit, presentationState.videoSizeDp),
        )

        if (state is TrailerPlaybackState.Loading || presentationState.coverSurface || showBuffering) {
            LoadingPage(
                focusEnabled = false,
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
            )
        }

        AnimatedVisibility(
            visible = osdVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TrailerBottomOverlay(
                title = title,
                isPlaying = isPlaying,
                positionMs = positionMs,
                bufferedMs = bufferedMs,
                durationMs = durationMs,
            )
        }
    }
}

@Composable
private fun TrailerBottomOverlay(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
) {
    val scrim =
        remember {
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.35f to Color.Black.copy(alpha = 0.55f),
                1f to Color.Black.copy(alpha = 0.88f),
            )
        }
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(scrim)
                .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isPlaying) {
                Icon(
                    painter = painterResource(R.drawable.baseline_pause_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Icon(
                    imageVector = AioIcons.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = stringResource(R.string.trailers_title),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AioTokens.Foreground,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f)),
        ) {
            if (buffered > 0f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(buffered)
                            .background(Color.White.copy(alpha = 0.35f)),
                )
            }
            if (progress > 0f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(AioTokens.Brand),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "${formatTrailerDuration(positionMs)} / ${formatTrailerDuration(durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AioTokens.Foreground,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.trailer_controls_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TrailerFallbackError(
    title: String,
    reason: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { retryFocus.tryRequestFocus() }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .fillMaxSize()
                .background(AioTokens.Background)
                .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.trailer_load_error),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AioTokens.Foreground,
        )
        reason?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(
                stringRes = R.string.retry,
                onClick = onRetry,
                modifier = Modifier.focusRequester(retryFocus),
            )
            TextButton(
                stringRes = R.string.go_back,
                onClick = onBack,
            )
        }
    }
}

private fun formatTrailerDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
