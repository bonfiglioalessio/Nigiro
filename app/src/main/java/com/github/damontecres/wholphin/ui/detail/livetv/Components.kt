package com.github.damontecres.wholphin.ui.detail.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.formatSeasonEpisode
import java.time.LocalDateTime

@Composable
fun Program(
    guideStart: LocalDateTime,
    now: LocalDateTime,
    program: TvProgram,
    colorCode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val startedBeforeGuide = program.start.isBefore(guideStart)
    val shape =
        remember(startedBeforeGuide) {
            val cornerSize = 4.dp
            if (startedBeforeGuide) {
                RoundedCornerShape(
                    topEnd = cornerSize,
                    bottomEnd = cornerSize,
                    topStart = 0.dp,
                    bottomStart = 0.dp,
                )
            } else {
                RoundedCornerShape(cornerSize)
            }
        }
    val title = program.name ?: program.id.toString()
    val airing = isProgramAiring(program, now)
    val past = isProgramPast(program, now)
    // Too narrow for readable text ("H...", "..."): leave the cell blank, the header shows the focused program
    val width = tvGuideDimensions.timelineHourWidth * (program.endHours - program.startHours)
    val showText = width >= 44.dp
    val showTime = width >= 72.dp && !program.isFake
    val baseColor = MaterialTheme.colorScheme.surfaceColorAtElevation(if (airing) 4.dp else 1.dp)
    val containerColor =
        when {
            program.isFake -> baseColor.copy(alpha = .35f)
            colorCode && program.category?.color != null -> program.category.color.copy(alpha = if (past) .18f else .3f)
            airing -> Color(0xFF231719)
            past -> baseColor.copy(alpha = .65f)
            else -> baseColor
        }
    val contentColor =
        MaterialTheme.colorScheme.onSurface.copy(
            alpha =
                when {
                    program.isFake -> .4f
                    past -> .6f
                    else -> 1f
                },
        )
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(1f, 1f, .95f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = containerColor,
                contentColor = contentColor,
                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ),
        modifier =
            modifier
                .padding(2.dp)
                .fillMaxSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (startedBeforeGuide && showText) {
                Text(
                    text = stringResource(R.string.fa_caret_left),
                    fontFamily = FontAwesome,
                    color = LocalContentColor.current.copy(alpha = .6f),
                    fontSize = 12.sp,
                    modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 4.dp),
                )
            }
            if (showText) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = title,
                        color = LocalContentColor.current,
                        fontSize = 13.sp,
                        fontWeight = if (airing) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showTime) {
                        val timeText =
                            if (width >= 112.dp) {
                                formatTimeRange(program.start, program.end)
                            } else {
                                formatTimeHHmm(program.start)
                            }
                        val seasonEpisodeLabel =
                            formatSeasonEpisode(
                                program.seasonEpisode?.season,
                                program.seasonEpisode?.episode,
                            )
                        val detailParts =
                            listOfNotNull(
                                timeText,
                                seasonEpisodeLabel,
                                program.subtitle,
                            ).joinToString(" · ")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (airing) {
                                androidx.compose.foundation.layout.Box(
                                    modifier =
                                        Modifier
                                            .height(6.dp)
                                            .width(6.dp)
                                            .clip(CircleShape)
                                            .background(LiveRed),
                                )
                            }
                            Text(
                                text = detailParts,
                                color = LocalContentColor.current.copy(alpha = if (airing) .82f else .62f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        val seasonEpisodeLabel =
                            formatSeasonEpisode(
                                program.seasonEpisode?.season,
                                program.seasonEpisode?.episode,
                            )
                        val subtitle =
                            remember(program, seasonEpisodeLabel) {
                                listOfNotNull(
                                    seasonEpisodeLabel,
                                    program.subtitle,
                                ).joinToString(" - ").ifBlank { null }
                            }
                        subtitle?.let {
                            Text(
                                text = it,
                                color = LocalContentColor.current.copy(alpha = .65f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        if (airing) {
            val progress = programProgress(program, now)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(LiveRed),
            )
        }
        RecordingMarker(
            isRecording = program.isRecording,
            isSeriesRecording = program.isSeriesRecording,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
fun Channel(
    channel: TvChannel,
    channelIndex: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(1f, 1f, .95f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            ),
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(2.dp)
                .fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = channel.number ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = .55f),
                    maxLines = 1,
                    modifier = Modifier.width(30.dp),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                ) {
                    var showImage by remember(channel.imageUrl) { mutableStateOf(channel.imageUrl != null) }
                    if (showImage) {
                        AsyncImage(
                            model = channel.imageUrl,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart,
                            modifier =
                                Modifier
                                    .height(22.dp)
                                    .fillMaxWidth(),
                            onError = {
                                showImage = false
                            },
                        )
                    }
                    (channel.shortName ?: channel.name)?.let {
                        Text(
                            text = it,
                            fontSize = if (showImage) 11.sp else 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = if (showImage) 13.sp else 17.sp,
                            // Without a logo the name has room for two lines
                            maxLines = if (showImage) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (channel.favorite) {
                Text(
                    color = colorResource(android.R.color.holo_red_light),
                    text = stringResource(R.string.fa_heart),
                    fontSize = 16.sp,
                    fontFamily = FontAwesome,
                    modifier =
                        Modifier
                            .padding(2.dp)
                            .align(Alignment.TopEnd),
                )
            }
        }
    }
}
