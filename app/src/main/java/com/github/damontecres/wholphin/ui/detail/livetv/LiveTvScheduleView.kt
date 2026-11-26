package com.github.damontecres.wholphin.ui.detail.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.components.CircularProgress
import com.github.damontecres.wholphin.ui.detail.movie.AioPillButton
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

val LiveRed = Color(0xFFE53935)

@Composable
fun LiveClockBadge(
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(999.dp))
                .background(LiveRed.copy(alpha = 0.16f))
                .border(1.dp, LiveRed.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(LiveRed),
        )
        Text(
            text = stringResource(R.string.live_tv_clock_now, formatTimeHHmm(now)),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
fun LiveTvModePills(
    selected: LiveTvViewMode,
    onSelect: (LiveTvViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        val modes =
            listOf(
                LiveTvViewMode.NOW to R.string.live_tv_mode_now,
                LiveTvViewMode.TONIGHT to R.string.live_tv_mode_tonight,
                LiveTvViewMode.GRID to R.string.live_tv_mode_grid,
            )
        modes.forEach { (mode, labelRes) ->
            AioPillButton(
                white = mode == selected,
                onClick = { if (mode != selected) onSelect(mode) },
            ) {
                Text(
                    text = stringResource(labelRes),
                    fontSize = 13.sp,
                    fontWeight = if (mode == selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun LiveTvScheduleList(
    mode: LiveTvViewMode,
    now: LocalDateTime,
    refreshing: Boolean,
    channels: List<TvChannel>,
    programsByChannel: Map<UUID, List<TvProgram>>,
    onPlayChannel: (TvChannel) -> Unit,
    onLongClickChannel: (TvChannel) -> Unit,
    onFocusRow: (Int) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var focusedRow by rememberInt(0)

    BackHandler(focusedRow > 0) {
        scope.launch {
            focusedRow = 0
            listState.animateScrollToItem(0)
            focusRequester.tryRequestFocus()
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = channels,
                key = { _, ch -> ch.id },
            ) { index, channel ->
                val row =
                    remember(channel, programsByChannel[channel.id], now) {
                        buildChannelScheduleRow(
                            channel = channel,
                            programs = programsByChannel[channel.id].orEmpty(),
                            now = now,
                        )
                    }
                LiveTvScheduleRowCard(
                    row = row,
                    mode = mode,
                    now = now,
                    onClick = { onPlayChannel(channel) },
                    onLongClick = { onLongClickChannel(channel) },
                    modifier =
                        Modifier
                            .ifElse(index == 0, Modifier.focusRequester(focusRequester))
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedRow = index
                                    onFocusRow(index)
                                }
                            },
                )
            }
        }
        if (refreshing) {
            CircularProgress(
                Modifier
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = .5f),
                        shape = CircleShape,
                    ).size(64.dp)
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
fun LiveTvScheduleRowCard(
    row: ChannelScheduleRow,
    mode: LiveTvViewMode,
    now: LocalDateTime,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val channel = row.channel
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.012f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = NigiroPalette.SurfaceRaised,
                focusedContainerColor = NigiroPalette.Card,
                contentColor = NigiroPalette.Text,
                focusedContentColor = Color.White,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = shape),
                focusedBorder = Border(BorderStroke(2.dp, NigiroPalette.Focus), shape = shape),
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .height(88.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // Left: Logo + LCN + Name + Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(220.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(width = 76.dp, height = 44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141414)),
                ) {
                    var showImage by remember(channel.imageUrl) { mutableStateOf(channel.imageUrl != null) }
                    if (showImage) {
                        AsyncImage(
                            model = channel.imageUrl,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                            onError = { showImage = false },
                        )
                    } else {
                        Text(
                            text = (channel.shortName ?: channel.name ?: "").take(8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NigiroPalette.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    modifier = Modifier.weight(1f),
                ) {
                    if (channel.number != null || channel.favorite) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            channel.number?.let { number ->
                                Text(
                                    text = number,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NigiroPalette.TextMuted,
                                    maxLines = 1,
                                )
                            }
                            if (channel.favorite) {
                                Text(
                                    text = stringResource(R.string.fa_heart),
                                    fontFamily = FontAwesome,
                                    fontSize = 11.sp,
                                    color = LiveRed,
                                )
                            }
                        }
                    }
                    Text(
                        text = channel.shortName ?: channel.name ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    channel.group?.let { group ->
                        Text(
                            text = group,
                            fontSize = 11.sp,
                            color = NigiroPalette.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Center: Primary slot ("IN ONDA" with exact time + progress bar, or "STASERA")
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                modifier = Modifier.weight(1.35f),
            ) {
                if (mode == LiveTvViewMode.NOW) {
                    val current = row.current
                    if (current != null) {
                        val remainingMin = programRemainingMinutes(current, now)
                        val progress = programProgress(current, now)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LiveRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.live_tv_on_air_badge),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            Text(
                                text = formatTimeRange(current.start, current.end),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NigiroPalette.Text,
                            )
                            if (remainingMin > 0) {
                                Text(
                                    text = "· ${stringResource(R.string.live_tv_min_left, remainingMin)}",
                                    fontSize = 11.sp,
                                    color = NigiroPalette.TextMuted,
                                    maxLines = 1,
                                )
                            }
                        }
                        val fullTitle =
                            if (!current.subtitle.isNullOrBlank()) {
                                "${current.name ?: ""} · ${current.subtitle}"
                            } else {
                                current.name ?: ""
                            }
                        Text(
                            text = fullTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.14f)),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(LiveRed),
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LiveRed.copy(alpha = 0.85f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            Text(
                                text = stringResource(R.string.live_tv_live_stream),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NigiroPalette.TextSecondary,
                            )
                        }
                        Text(
                            text = channel.shortName ?: channel.name ?: "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.live_tv_press_ok_watch),
                            fontSize = 11.sp,
                            color = NigiroPalette.TextMuted,
                            maxLines = 1,
                        )
                    }
                } else {
                    val primeSlot = row.tonight.firstOrNull()
                    if (primeSlot != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NigiroPalette.Pink)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.live_tv_tonight_badge),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NigiroPalette.OnPink,
                                )
                            }
                            Text(
                                text = formatTimeRange(primeSlot.start, primeSlot.end),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NigiroPalette.Text,
                            )
                        }
                        Text(
                            text = primeSlot.name ?: "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        row.current?.let { current ->
                            Text(
                                text =
                                    stringResource(
                                        R.string.live_tv_now_prefix,
                                        formatTimeHHmm(current.start),
                                        current.name ?: "",
                                    ),
                                fontSize = 11.sp,
                                color = NigiroPalette.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.live_tv_no_tonight),
                            fontSize = 13.sp,
                            color = NigiroPalette.TextMuted,
                            maxLines = 1,
                        )
                        row.current?.let { current ->
                            Text(
                                text =
                                    stringResource(
                                        R.string.live_tv_now_prefix,
                                        formatTimeHHmm(current.start),
                                        current.name ?: "",
                                    ),
                                fontSize = 11.sp,
                                color = NigiroPalette.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Thin divider + Right: Up Next / Later schedule
            Box(
                modifier =
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.75f)
                        .background(Color.White.copy(alpha = 0.08f)),
            )

            val rightHeaderRes =
                if (mode == LiveTvViewMode.TONIGHT) R.string.live_tv_later else R.string.live_tv_up_next
            val rightSlots =
                if (mode == LiveTvViewMode.TONIGHT) {
                    row.tonight.drop(1).take(2)
                } else {
                    row.upcoming.take(2)
                }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                modifier = Modifier.weight(1.0f),
            ) {
                Text(
                    text = stringResource(rightHeaderRes),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NigiroPalette.TextMuted,
                    maxLines = 1,
                )
                if (rightSlots.isEmpty()) {
                    Text(
                        text = "—",
                        fontSize = 12.sp,
                        color = NigiroPalette.TextMuted,
                    )
                } else {
                    rightSlots.forEach { slot ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatTimeHHmm(slot.start),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NigiroPalette.Pink,
                                modifier = Modifier.width(42.dp),
                            )
                            Text(
                                text = slot.name ?: "",
                                fontSize = 12.sp,
                                color = NigiroPalette.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
