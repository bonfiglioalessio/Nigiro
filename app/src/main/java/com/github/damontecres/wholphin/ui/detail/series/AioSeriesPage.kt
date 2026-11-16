package com.github.damontecres.wholphin.ui.detail.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.cards.ExpandingPosterCard
import com.github.damontecres.wholphin.ui.detail.movie.AioDetailBackdrop
import com.github.damontecres.wholphin.ui.detail.movie.AioGenres
import com.github.damontecres.wholphin.ui.detail.movie.AioIconButton
import com.github.damontecres.wholphin.ui.detail.movie.AioOverview
import com.github.damontecres.wholphin.ui.detail.movie.AioPeopleRow
import com.github.damontecres.wholphin.ui.detail.movie.AioPersonEntry
import com.github.damontecres.wholphin.ui.detail.movie.AioPillButton
import com.github.damontecres.wholphin.ui.detail.movie.AioPoster
import com.github.damontecres.wholphin.ui.detail.movie.AioSectionTitle
import com.github.damontecres.wholphin.ui.detail.movie.AioTagline
import com.github.damontecres.wholphin.ui.detail.movie.AioTitle
import com.github.damontecres.wholphin.ui.detail.movie.MovieFacts
import com.github.damontecres.wholphin.ui.detail.movie.PageToSidebarGap
import com.github.damontecres.wholphin.ui.detail.movie.bleedHorizontally
import com.github.damontecres.wholphin.ui.detail.movie.crewEntries
import com.github.damontecres.wholphin.ui.detail.movie.crewJobName
import com.github.damontecres.wholphin.ui.detail.movie.splitCastAndCrew
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import com.github.damontecres.wholphin.ui.main.episodeCode
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import com.github.damontecres.wholphin.util.ExceptionHandler
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The show page of the AIOStreams app (packages/web/src/pages/item.tsx for a Series): the movie page's header
 * with the show's meta, next airing and Play/Resume SxxEyy, then the Episodes section (season posters, the
 * selected season's episodes), cast, crew, details and similar shows. The backdrop scrolls away with the page.
 */
@Composable
fun AioSeriesDetailsContent(
    preferences: UserPreferences,
    series: BaseItem,
    state: SeriesState,
    onPlay: (fromStart: Boolean) -> Unit,
    onShuffle: () -> Unit,
    onTrailer: () -> Unit,
    onWatched: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    overviewOnClick: () -> Unit,
    onSelectSeason: (BaseItem) -> Unit,
    onLongClickSeason: (Int, BaseItem) -> Unit,
    onAllEpisodes: (BaseItem) -> Unit,
    onPlayEpisode: (BaseItem) -> Unit,
    onLongClickEpisode: (Int, BaseItem) -> Unit,
    onClickPerson: (Person) -> Unit,
    onLongClickPerson: (Int, Person) -> Unit,
    onClickSimilar: (Int, BaseItem) -> Unit,
    onLongClickSimilar: (Int, BaseItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val dto = series.data
    val background = MaterialTheme.colorScheme.background
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val headerTop = screenHeight * .27f
    val contentStart = 0.dp
    val bringIntoView = remember { BringIntoViewRequester() }
    val playFocus = remember { FocusRequester() }
    val castAndCrew = remember(state.people) { splitCastAndCrew(state.people) { it.type } }
    val cast = remember(castAndCrew) { castAndCrew.first.map { AioPersonEntry(it, it.role) } }
    val crew = remember(castAndCrew) { crewEntries(castAndCrew.second, ::crewJobName) }
    val selectedSeasonId = (state.episodes as? EpisodeList.Success)?.seasonId
    val episodes = (state.episodes as? EpisodeList.Success)?.episodes
    val listState = rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(series.id) {
        runCatching { playFocus.requestFocus() }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(background),
    ) {
        AioDetailBackdrop(
            movie = series,
            background = background,
            modifier =
                Modifier
                    .bleedHorizontally(NigiroDimens.HeroBleedStart, NigiroDimens.HeroBleedEnd)
                    .graphicsLayer {
                        translationY =
                            if (listState.firstVisibleItemIndex == 0) {
                                -listState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                -size.height
                            }
                    },
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(px(48)),
            contentPadding = PaddingValues(bottom = px(64)),
            modifier =
                Modifier
                    .fillMaxSize()
                    .bleedHorizontally(PageToSidebarGap, 0.dp),
        ) {
            item(key = "header") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(px(32)),
                    verticalAlignment = Alignment.Bottom,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoView)
                            .padding(start = contentStart, end = px(40), top = headerTop),
                ) {
                    AioPoster(series)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(px(16)),
                        modifier = Modifier.widthIn(max = px(768)),
                    ) {
                        AioTitle(series, preferences.appPreferences.interfacePreferences.showLogos)
                        dto.taglines?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { AioTagline(it) }
                        AioSeriesMeta(series)
                        state.nextAiring?.let { AioNextAiring(it) }
                        dto.genres?.takeIf { it.isNotEmpty() }?.let { AioGenres(it) }
                        dto.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                            AioOverview(overview = overview, onMore = overviewOnClick)
                        }
                        AioSeriesActions(
                            nextUp = state.nextUp,
                            watched = dto.userData?.played ?: false,
                            favorite = dto.userData?.isFavorite ?: false,
                            hasTrailer = state.trailers.isNotEmpty(),
                            onPlay = onPlay,
                            onShuffle = onShuffle,
                            onTrailer = onTrailer,
                            onWatched = onWatched,
                            onFavorite = onFavorite,
                            onMore = onMore,
                            playFocusRequester = playFocus,
                            onFocused = { scope.launch(ExceptionHandler()) { bringIntoView.bringIntoView() } },
                        )
                    }
                }
            }
            if (state.seasons.isNotEmpty()) {
                item(key = "episodes") {
                    AioEpisodesSection(
                        seasons = state.seasons,
                        selectedSeasonId = selectedSeasonId,
                        episodes = episodes,
                        contentStart = contentStart,
                        onSelectSeason = onSelectSeason,
                        onLongClickSeason = onLongClickSeason,
                        onAllEpisodes = onAllEpisodes,
                        onPlayEpisode = onPlayEpisode,
                        onLongClickEpisode = onLongClickEpisode,
                    )
                }
            }
            if (cast.isNotEmpty()) {
                item(key = "cast") {
                    AioPeopleRow(
                        title = stringResource(R.string.cast_title),
                        people = cast,
                        onClick = onClickPerson,
                        onLongClick = onLongClickPerson,
                        contentStart = contentStart,
                    )
                }
            }
            if (crew.isNotEmpty()) {
                item(key = "crew") {
                    AioPeopleRow(
                        title = stringResource(R.string.crew_title),
                        people = crew,
                        onClick = onClickPerson,
                        onLongClick = onLongClickPerson,
                        contentStart = contentStart,
                    )
                }
            }
            item(key = "details") {
                MovieFacts(movie = series, modifier = Modifier.padding(start = contentStart, end = px(40)))
            }
            if (state.similar.isNotEmpty()) {
                item(key = "similar") {
                    Column(verticalArrangement = Arrangement.spacedBy(px(16))) {
                        AioSectionTitle(stringResource(R.string.more_like_this), Modifier.padding(start = contentStart))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(NigiroDimens.CardGap),
                            contentPadding = PaddingValues(start = contentStart, end = px(32)),
                        ) {
                            itemsIndexed(state.similar, key = { _, item -> item.id }) { index, item ->
                                ExpandingPosterCard(
                                    item = item,
                                    onClick = { onClickSimilar(index, item) },
                                    onLongClick = { onLongClickSimilar(index, item) },
                                    cardHeight = NigiroDimens.PosterHeight,
                                    cornerText = item.ui.episodeUnplayedCornerText,
                                    expandable = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** item-meta for a show: years • [age rating] • ★ score • status */
@Composable
private fun AioSeriesMeta(series: BaseItem) {
    val data = series.data
    val parts =
        remember(series.id, data) {
            listOfNotNull(
                seriesYears(series)?.let { "y:$it" },
                data.officialRating?.takeIf { it.isNotBlank() }?.let { "r:$it" },
                data.communityRating?.takeIf { it > 0f }?.let { "s:" + String.format(Locale.ROOT, "%.1f", it) },
                seriesStatus(series)?.let { "t:$it" },
            )
        }
    if (parts.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(px(10)), verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { index, part ->
            if (index > 0) Text(text = "•", fontSize = pxSp(14), color = Color(0xFF5A5A5A))
            val value = part.substring(2)
            when (part[0]) {
                'r' -> {
                    Text(
                        text = value,
                        fontSize = pxSp(12),
                        color = AioTokens.Foreground,
                        modifier =
                            Modifier
                                .border(1.dp, Color.White.copy(alpha = .3f), RoundedCornerShape(px(4)))
                                .padding(horizontal = px(6), vertical = 1.dp),
                    )
                }

                's' -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFACC15), modifier = Modifier.size(px(16)))
                        Spacer(Modifier.width(px(4)))
                        Text(text = value, fontSize = pxSp(14), color = AioTokens.Foreground)
                    }
                }

                else -> {
                    Text(text = value, fontSize = pxSp(14), color = AioTokens.Foreground)
                }
            }
        }
    }
}

/** item-actions for a show: Play/Resume SxxEyy (+ restart), shuffle, trailer, watched, favourite, more */
@Composable
private fun AioSeriesActions(
    nextUp: BaseItem?,
    watched: Boolean,
    favorite: Boolean,
    hasTrailer: Boolean,
    onPlay: (fromStart: Boolean) -> Unit,
    onShuffle: () -> Unit,
    onTrailer: () -> Unit,
    onWatched: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    playFocusRequester: FocusRequester,
    onFocused: () -> Unit,
) {
    val focusMod = Modifier.onFocusChanged { if (it.isFocused) onFocused() }
    val resuming = (nextUp?.data?.userData?.playbackPositionTicks ?: 0L) > 0L
    val code = nextUp?.let { episodeCode(it.data.parentIndexNumber, it.data.indexNumber) }
    val label =
        stringResource(if (resuming) R.string.resume else R.string.play) + (code?.let { " $it" } ?: "")
    Row(horizontalArrangement = Arrangement.spacedBy(px(8)), verticalAlignment = Alignment.CenterVertically) {
        AioPillButton(white = true, onClick = { onPlay(false) }, modifier = focusMod.focusRequester(playFocusRequester)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(px(20)))
            Spacer(Modifier.width(px(8)))
            Text(text = label, fontSize = pxSp(14), fontWeight = FontWeight.Medium)
        }
        if (resuming) {
            AioIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.restart),
                onClick = { onPlay(true) },
                modifier = focusMod,
            )
        }
        AioIconButton(
            icon = null,
            glyph = stringResource(R.string.fa_shuffle),
            contentDescription = stringResource(R.string.shuffle),
            onClick = onShuffle,
            modifier = focusMod,
        )
        if (hasTrailer) {
            AioPillButton(white = false, onClick = onTrailer, modifier = focusMod) {
                Text(text = stringResource(R.string.fa_film), fontFamily = com.github.damontecres.wholphin.ui.FontAwesome, fontSize = pxSp(15))
                Spacer(Modifier.width(px(8)))
                Text(text = "Trailer", fontSize = pxSp(14), fontWeight = FontWeight.Medium)
            }
        }
        AioIconButton(
            icon = Icons.Default.Check,
            contentDescription = stringResource(if (watched) R.string.mark_unwatched else R.string.mark_watched),
            onClick = onWatched,
            activeTint = if (watched) Color(0xFFC7C2FF) else null,
            modifier = focusMod,
        )
        AioIconButton(
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (favorite) R.string.remove_favorite else R.string.add_favorite),
            onClick = onFavorite,
            activeTint = if (favorite) Color(0xFFFCA5A5) else null,
            modifier = focusMod,
        )
        AioIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.more),
            onClick = onMore,
            modifier = focusMod,
        )
    }
}
