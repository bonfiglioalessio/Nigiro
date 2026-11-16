package com.github.damontecres.wholphin.ui.detail.series

import androidx.compose.ui.focus.FocusRequester
import com.github.damontecres.wholphin.ui.AioIcons
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import com.github.damontecres.wholphin.ui.cards.showNotReleasedToast
import com.github.damontecres.wholphin.ui.cards.AioUnreleasedChip
import androidx.compose.ui.unit.sp
import com.github.damontecres.wholphin.ui.cards.AioWatchedMark
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.cards.aioCardBorder
import com.github.damontecres.wholphin.ui.cards.cardScale
import com.github.damontecres.wholphin.ui.cards.noCardGlow
import com.github.damontecres.wholphin.ui.detail.movie.aioDuration
import com.github.damontecres.wholphin.ui.main.episodeCode
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import org.jellyfin.sdk.model.api.ImageType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/*
 * The episodes part of the AIOStreams show page (packages/web/src/pages/item.tsx Seasons and NextAiring,
 * components/episodes.tsx EpisodeCard), written for Compose. 1 css px = 0.75 dp on the TV.
 */

private val ItalianShortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)
private val ItalianDay = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ITALIAN)
private val ItalianReleaseDay = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)

/** An episode whose air date is still to come: it can't be played yet */
fun BaseItem.isUnreleased(): Boolean = data.premiereDate?.toLocalDate()?.isAfter(LocalDate.now()) == true

/** "13 dic": the air day, for "Esce il …" */
fun BaseItem.releaseDay(): String? = data.premiereDate?.let { ItalianReleaseDay.format(it) }

/** "2024–" while airing, "2019–2023" when ended, like the AIOStreams MetaRow for a show */
fun seriesYears(series: BaseItem): String? {
    val start = series.data.productionYear ?: return null
    val end = series.data.endDate?.year
    return when {
        series.data.status.equals("Continuing", ignoreCase = true) -> "$start–"
        end != null && end != start -> "$start–$end"
        else -> start.toString()
    }
}

/** The show's status in Italian */
fun seriesStatus(series: BaseItem): String? =
    when (series.data.status?.lowercase()) {
        "continuing" -> "In corso"
        "ended" -> "Terminata"
        null, "" -> null
        else -> series.data.status
    }

/** "tra 3 settimane", "domani", "oggi" */
fun airsIn(
    date: LocalDate,
    today: LocalDate = LocalDate.now(),
): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days <= 0L -> "oggi"
        days == 1L -> "domani"
        days < 7L -> "tra $days giorni"
        days < 14L -> "tra 1 settimana"
        days < 60L -> "tra ${days / 7} settimane"
        else -> "tra ${days / 30} mesi"
    }
}

/** NextAiring: calendar icon, bold "S03E01", "airs in 3 weeks", muted day — text-sm */
@Composable
fun AioNextAiring(episode: BaseItem) {
    val date: LocalDateTime = episode.data.premiereDate ?: return
    val code = episodeCode(episode.data.parentIndexNumber, episode.data.indexNumber) ?: return
    Row(
        horizontalArrangement = Arrangement.spacedBy(px(8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AioIcons.Calendar, contentDescription = null, tint = AioTokens.Muted, modifier = Modifier.size(px(18)))
        Text(text = code, fontSize = pxSp(14), fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(text = "esce ${airsIn(date.toLocalDate())}", fontSize = pxSp(14), color = AioTokens.Foreground)
        Text(text = ItalianDay.format(date), fontSize = pxSp(14), color = AioTokens.Muted)
    }
}

/** season-poster: 120 px, rounded-lg, dimmed unless current; ring-2 white when current; name below */
@Composable
fun AioSeasonPoster(
    season: BaseItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrlService = LocalImageUrlService.current
    val url = remember(season.id) { imageUrlService.getItemImageUrl(season, ImageType.PRIMARY, fillWidth = 240) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(AioTokens.Radius)
    Column(verticalArrangement = Arrangement.spacedBy(px(8)), modifier = modifier.width(px(120))) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            interactionSource = interaction,
            shape = CardDefaults.shape(shape),
            border = aioCardBorder(shape),
            scale = cardScale(focusedScale = 1f),
            glow = noCardGlow(),
            colors = CardDefaults.colors(containerColor = AioTokens.Gray900),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .alpha(if (selected || focused) 1f else .6f),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(
                            if (selected) px(2) else 1.dp,
                            if (selected) Color.White else Color.White.copy(alpha = .1f),
                            shape,
                        ),
                )
                if (season.data.userData?.played == true) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(px(6))
                                .size(px(20))
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = .6f)),
                    ) {
                        Icon(AioIcons.Check, null, tint = Color.White, modifier = Modifier.size(px(14)))
                    }
                }
            }
        }
        Text(
            text = season.name.orEmpty(),
            fontSize = pxSp(14),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected || focused) Color.White else AioTokens.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            // The current season is underlined in the accent
            Box(
                Modifier
                    .width(px(24))
                    .height(px(2))
                    .background(AioTokens.Brand),
            )
        }
    }
}

/** "7 nov 2025 · 8m rimanenti · ★ 7.2" (or the runtime when not started) */
fun episodeMeta(episode: BaseItem): String {
    val data = episode.data
    val date =
        data.premiereDate?.let {
            if (episode.isUnreleased()) "Esce il ${ItalianReleaseDay.format(it)}" else ItalianShortDate.format(it)
        }
    val position = data.userData?.playbackPositionTicks ?: 0L
    val runtime = data.runTimeTicks
    val time =
        if (position > 0 && runtime != null && runtime > position) {
            aioDuration(runtime - position)?.let { "$it rimanenti" }
        } else {
            aioDuration(runtime)
        }
    val score = data.communityRating?.takeIf { it > 0f }?.let { "★ " + String.format(Locale.ROOT, "%.1f", it) }
    return listOfNotNull(date, time, score).joinToString(" · ")
}

/** EpisodeCard (lg row: 22rem): 16:9 still, rounded-xl, play veil on focus, watched mark, progress; title & meta */
@Composable
fun AioEpisodeCard(
    episode: BaseItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrlService = LocalImageUrlService.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val width = px(336)
    val height = width * (9f / 16f)
    val request =
        remember(episode.id) {
            val w = with(density) { width.toPx() }.roundToInt()
            val h = with(density) { height.toPx() }.roundToInt()
            val url =
                if (episode.data.imageTags?.containsKey(ImageType.PRIMARY) == true) {
                    imageUrlService.getItemImageUrl(episode, ImageType.PRIMARY, fillWidth = w, useSeriesForPrimary = false)
                } else {
                    imageUrlService.getItemImageUrl(episode, ImageType.BACKDROP, fillWidth = w)
                }
            url?.let {
                ImageRequest
                    .Builder(context)
                    .data(it)
                    .size(w, h)
                    .build()
            }
        }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val data = episode.data
    val shape = RoundedCornerShape(AioTokens.RadiusXl)
    val played = data.userData?.played == true
    val percent = data.userData?.playedPercentage ?: 0.0
    val unreleased = remember(episode.id, data.premiereDate) { episode.isUnreleased() }
    Column(verticalArrangement = Arrangement.spacedBy(px(8)), modifier = modifier.width(width)) {
        Card(
            onClick = {
                if (unreleased) {
                    // Not out yet: nothing to play, say when it comes out instead
                    showNotReleasedToast(context, episode.releaseDay())
                } else {
                    currentOnClick()
                }
            },
            onLongClick = { currentOnLongClick() },
            interactionSource = interaction,
            shape = CardDefaults.shape(shape),
            border = aioCardBorder(shape),
            scale = cardScale(focusedScale = 1f),
            glow = noCardGlow(),
            colors = CardDefaults.colors(containerColor = AioTokens.Gray900),
            modifier = Modifier.size(width, height),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (request != null) {
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        // Dimmed until it airs
                        alpha = if (unreleased) .45f else 1f,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (unreleased) {
                    AioUnreleasedChip()
                }
                if (focused && !unreleased) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = .3f)),
                    ) {
                        Icon(AioIcons.Play, null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size(px(48)))
                    }
                }
                if (played && (percent <= 0 || percent >= 100)) {
                    // Same quiet watched mark as the posters
                    AioWatchedMark()
                }
                if (percent > 0 && percent < 100) {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(px(4))
                            .background(Color.Black.copy(alpha = .6f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((percent.coerceIn(2.0, 100.0) / 100).toFloat())
                                .height(px(4))
                                .background(AioTokens.Brand),
                        )
                    }
                }
            }
        }
        // episode-title: "1. Name", semibold; meta text-sm muted
        Text(
            text = listOfNotNull(data.indexNumber?.let { "$it." }, data.name).joinToString(" "),
            fontSize = pxSp(16),
            fontWeight = FontWeight.SemiBold,
            color = if (focused) Color.White else AioTokens.Foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = px(2)),
        )
        val meta = remember(episode.id, data.userData) { episodeMeta(episode) }
        if (meta.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = px(2))) {
                // The star is drawn in yellow like the AIOStreams score
                val parts = meta.split("★ ")
                Text(text = parts[0], fontSize = pxSp(14), color = AioTokens.Muted, maxLines = 1)
                if (parts.size > 1) {
                    Icon(AioIcons.Star, null, tint = Color(0xFFFACC15), modifier = Modifier.size(px(14)))
                    Spacer(Modifier.width(px(4)))
                    Text(text = parts[1], fontSize = pxSp(14), color = AioTokens.Muted, maxLines = 1)
                }
            }
        }
    }
}

/** "Tutti gli episodi ›": Button size sm, rounded-full, gray-outline */
@Composable
fun AioSmallPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(pill),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = .1f),
                contentColor = AioTokens.Gray300,
                focusedContentColor = Color.White,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = androidx.tv.material3.Border(BorderStroke(1.dp, Color(0xFF8F8F8F).copy(alpha = .2f)), shape = pill),
                focusedBorder = androidx.tv.material3.Border(BorderStroke(AioTokens.RingWidth, AioTokens.Ring), shape = pill),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier.height(px(32)),
    ) {
        // Wraps its label: filling the row would leave the "2025 · 6 episodi" text no width at all
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight().padding(horizontal = px(12))) {
            Text(text = "$text  ›", fontSize = pxSp(14), fontWeight = FontWeight.Medium)
        }
    }
}

/** The Episodes section: season posters, "2025 · 6 episodi" + "All episodes", then the episodes row */
@Composable
fun AioEpisodesSection(
    seasons: List<BaseItem?>,
    selectedSeasonId: java.util.UUID?,
    episodes: List<BaseItem?>?,
    contentStart: androidx.compose.ui.unit.Dp,
    onSelectSeason: (BaseItem) -> Unit,
    onLongClickSeason: (Int, BaseItem) -> Unit,
    /** A page with every episode of the season; null shows no link, the season's episodes being listed here */
    onAllEpisodes: ((BaseItem) -> Unit)? = null,
    onPlayEpisode: (BaseItem) -> Unit,
    onLongClickEpisode: (Int, BaseItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedSeason = seasons.firstOrNull { it?.id == selectedSeasonId }
    Column(verticalArrangement = Arrangement.spacedBy(px(16)), modifier = modifier) {
        com.github.damontecres.wholphin.ui.detail.movie.AioSectionTitle(
            stringResource(R.string.episodes),
            Modifier.padding(start = contentStart),
        )
        // Coming down from the buttons lands on the season shown, not on whichever poster is under them
        val selectedSeasonFocus = remember { FocusRequester() }
        // Resting on a season shows its episodes (cached, so instant); the short delay skips the posters only passed
        var focusedSeason by remember { mutableStateOf<BaseItem?>(null) }
        val currentOnSelectSeason by rememberUpdatedState(onSelectSeason)
        LaunchedEffect(focusedSeason?.id) {
            val season = focusedSeason ?: return@LaunchedEffect
            if (season.id == selectedSeasonId) return@LaunchedEffect
            delay(SEASON_FOCUS_DELAY_MS)
            currentOnSelectSeason(season)
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(px(16)),
            contentPadding = PaddingValues(start = contentStart, end = px(32)),
            modifier = Modifier.focusRestorer(selectedSeasonFocus),
        ) {
            itemsIndexed(seasons, key = { i, s -> s?.id ?: i }) { index, season ->
                if (season != null) {
                    val selected = season.id == selectedSeasonId
                    AioSeasonPoster(
                        season = season,
                        selected = selected,
                        onClick = { onSelectSeason(season) },
                        onLongClick = { onLongClickSeason(index, season) },
                        modifier =
                            Modifier
                                .onFocusChanged { if (it.isFocused) focusedSeason = season }
                                .then(if (selected) Modifier.focusRequester(selectedSeasonFocus) else Modifier),
                    )
                }
            }
        }
        if (selectedSeason != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = contentStart, end = px(32)),
            ) {
                val year = selectedSeason.data.productionYear ?: selectedSeason.data.premiereDate?.year
                val count = episodes?.size ?: selectedSeason.data.childCount
                Text(
                    text = listOfNotNull(year?.toString(), count?.let { "$it episodi" }).joinToString(" · "),
                    fontSize = pxSp(14),
                    color = AioTokens.Muted,
                    modifier = Modifier.weight(1f),
                )
                onAllEpisodes?.let { allEpisodes ->
                    AioSmallPill(stringResource(R.string.all_episodes), onClick = { allEpisodes(selectedSeason) })
                }
            }
        }
        if (episodes != null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(px(16)),
                contentPadding = PaddingValues(start = contentStart, end = px(32)),
            ) {
                items(episodes.size) { index ->
                    val episode = episodes[index]
                    if (episode != null) {
                        AioEpisodeCard(
                            episode = episode,
                            onClick = { onPlayEpisode(episode) },
                            onLongClick = { onLongClickEpisode(index, episode) },
                        )
                    } else {
                        Box(
                            Modifier
                                .width(px(336))
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(AioTokens.RadiusXl))
                                .background(AioTokens.Gray900),
                        )
                    }
                }
            }
        }
    }
}

private const val SEASON_FOCUS_DELAY_MS = 300L
