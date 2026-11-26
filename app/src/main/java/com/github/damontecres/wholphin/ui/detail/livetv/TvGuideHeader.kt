package com.github.damontecres.wholphin.ui.detail.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.transitionFactory
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.CrossFadeFactory
import com.github.damontecres.wholphin.ui.components.EpisodeName
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.OverviewText
import com.github.damontecres.wholphin.ui.components.QuickDetailsText
import com.github.damontecres.wholphin.ui.components.StreamLabel
import com.github.damontecres.wholphin.ui.components.TitleOrLogo
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoilApi::class)
@Composable
fun TvGuideHeader(
    program: TvProgram?,
    channel: TvChannel?,
    modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val airing = program != null && isProgramAiring(program, now)
    // Placeholder cell of a channel without guide data: show the channel, not "No guide" as a title
    val noGuide = program?.isFake == true
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f),
        ) {
            // Which channel the focused program is on: "1002  Eurosport 1 Italy  [Sport ITA]"
            channel?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = HeaderUtils.startPadding),
                ) {
                    it.number?.let { number ->
                        Text(
                            text = number,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
                        )
                    }
                    Text(
                        text = it.shortName ?: it.name ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    it.group?.let { group -> StreamLabel(group) }
                    if (airing) {
                        Text(
                            text = stringResource(R.string.live_now),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier =
                                Modifier
                                    .background(LiveRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            TitleOrLogo(
                title =
                    if (noGuide) {
                        channel?.shortName ?: channel?.name ?: ""
                    } else {
                        program?.name ?: program?.id.toString()
                    },
                logoImageUrl = null,
                showLogo = false,
                modifier =
                    Modifier
                        .padding(start = HeaderUtils.startPadding)
                        .fillMaxWidth(.9f),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(.85f),
            ) {
                if (noGuide) {
                    Text(
                        text = stringResource(R.string.live_tv_no_guide_hint),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                        modifier = Modifier.padding(start = HeaderUtils.startPadding),
                    )
                    return@Column
                }
                program?.subtitle?.let {
                    EpisodeName(
                        episodeName = program.subtitle,
                        modifier = Modifier.padding(start = HeaderUtils.startPadding),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = HeaderUtils.startPadding),
                ) {
                    program?.quickDetails?.let {
                        QuickDetailsText(
                            it,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    if (program?.isRepeat == true) {
                        StreamLabel(stringResource(R.string.live_tv_repeat))
                    }
                }
                OverviewText(
                    overview = program?.overview ?: "",
                    maxLines = 2,
                    onClick = {},
                    enabled = false,
                )
            }
        }

        // Fixed 16:9 frame: program stills and square channel logos (Pluto) both fit without jumping around
        val image = program?.imageUrl ?: channel?.imageUrl
        if (image != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .padding(end = HeaderUtils.startPadding)
                        .size(width = 224.dp, height = 126.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            ) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(image)
                            .transitionFactory(CrossFadeFactory(300.milliseconds))
                            .useExistingImageAsPlaceholder(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
