package com.github.damontecres.wholphin.ui.explore

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.cards.ItemRowTitle
import com.github.damontecres.wholphin.ui.cards.cardScale
import com.github.damontecres.wholphin.ui.cards.noCardGlow
import com.github.damontecres.wholphin.ui.components.CircularProgress
import com.github.damontecres.wholphin.ui.components.HeaderIconButton
import com.github.damontecres.wholphin.ui.main.HomeMediaFilterBar
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import com.github.damontecres.wholphin.ui.main.PlatformTileCard
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.tryRequestFocus

private val TileWidth = 190.dp
private val TileHeight = 107.dp
private const val GENRE_COLUMNS = 4
private val TileShape = RoundedCornerShape(10.dp)

/**
 * Explore hub in the style of Nuvio's collections: a row of streaming platforms and a grid of genres, each tile
 * opening its titles. The filter on top limits everything to movies or series.
 */
@Composable
fun ExplorePage(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filterFocusRequester = remember { FocusRequester() }
    val tileFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun tileFocus(key: String) = tileFocusRequesters.getOrPut(key) { FocusRequester() }

    // Coming back from a tile's titles, focus returns to that tile; the first visit starts on the first platform
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val firstKey = state.platforms.firstOrNull()?.let { platformKey(it.key) } ?: state.genres.firstOrNull()?.let { genreKey(it) }
    LaunchedEffect(firstKey != null) {
        val target = lastFocusedKey ?: firstKey ?: return@LaunchedEffect
        // The tile may need a frame or two to be laid out again
        repeat(10) {
            if (tileFocus(target).tryRequestFocus()) return@LaunchedEffect
            delay(50)
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(NigiroDimens.SectionGap),
        contentPadding = PaddingValues(top = 20.dp, bottom = 48.dp),
        modifier = modifier.fillMaxSize().focusRestorer(),
    ) {
        item(key = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = NigiroDimens.ContentStart, end = NigiroDimens.ContentEnd),
            ) {
                Text(
                    text = stringResource(R.string.explore),
                    style = NigiroDimens.RowTitle.copy(fontSize = 26.sp, lineHeight = 30.sp),
                    color = NigiroPalette.Text,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeMediaFilterBar(
                        selectedFilter = state.filter,
                        onSelectFilter = viewModel::setFilter,
                        firstButtonFocusRequester = filterFocusRequester,
                    )
                    HeaderIconButton(
                        glyph = stringResource(R.string.fa_dice),
                        contentDescription = stringResource(R.string.explore_random),
                        onClick = viewModel::openRandom,
                    )
                }
            }
        }

        if (state.platforms.isNotEmpty()) {
            item(key = "platforms") {
                Column(verticalArrangement = Arrangement.spacedBy(NigiroDimens.TitleGap)) {
                    ItemRowTitle(
                        stringResource(R.string.explore_platforms),
                        startPadding = NigiroDimens.ContentStart,
                        style = NigiroDimens.RowTitle,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(NigiroDimens.CardGap),
                        contentPadding =
                            PaddingValues(
                                start = NigiroDimens.ContentStart,
                                end = NigiroDimens.ContentEnd,
                                top = 6.dp,
                                bottom = 6.dp,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRestorer()
                                // The filter sits at the right edge: without this, Up from the first tiles opens the sidebar
                                .focusProperties { up = filterFocusRequester },
                    ) {
                        items(state.platforms, key = { tile -> tile.key }) { tile ->
                            val key = platformKey(tile.key)
                            PlatformTileCard(
                                tile = tile,
                                activeFilter = state.filter,
                                onClick = { viewModel.openPlatform(tile) },
                                width = TileWidth,
                                height = TileHeight,
                                modifier =
                                    Modifier
                                        .focusRequester(tileFocus(key))
                                        .onFocusChanged { if (it.isFocused) lastFocusedKey = key },
                            )
                        }
                    }
                }
            }
        }

        if (state.genres.isEmpty()) {
            item(key = "genres_status") {
                GenresTitle()
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier
                            .padding(start = NigiroDimens.ContentStart, top = NigiroDimens.TitleGap)
                            .size(TileWidth, TileHeight),
                ) {
                    if (state.genresLoading) {
                        CircularProgress(Modifier.size(32.dp))
                    } else {
                        Text(stringResource(R.string.explore_empty), color = NigiroPalette.TextMuted)
                    }
                }
            }
        }
        itemsIndexed(
            items = state.genres.chunked(GENRE_COLUMNS),
            key = { _, row -> "genres_" + row.first().genre.key },
        ) { index, row ->
            // The title rides with the first row, so it sits as close to its tiles as "Piattaforme" does
            if (index == 0) {
                GenresTitle(Modifier.padding(bottom = NigiroDimens.TitleGap))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(NigiroDimens.CardGap),
                modifier = Modifier.padding(start = NigiroDimens.ContentStart, end = NigiroDimens.ContentEnd),
            ) {
                row.forEach { tile ->
                    val key = genreKey(tile)
                    GenreTileCard(
                        tile = tile,
                        onClick = { viewModel.openGenre(tile) },
                        modifier =
                            Modifier
                                .focusRequester(tileFocus(key))
                                .onFocusChanged { if (it.isFocused) lastFocusedKey = key },
                    )
                }
            }
        }
    }
}

@Composable
private fun GenresTitle(modifier: Modifier = Modifier) {
    ItemRowTitle(
        stringResource(R.string.explore_genres),
        startPadding = NigiroDimens.ContentStart,
        style = NigiroDimens.RowTitle,
        modifier = modifier,
    )
}

private fun platformKey(key: String) = "platform_$key"

private fun genreKey(tile: ExploreGenreTile) = "genre_${tile.genre.key}"

@Composable
private fun GenreTileCard(
    tile: ExploreGenreTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val request =
        remember(context, tile.imageUrl, density) {
            tile.imageUrl?.let { url ->
                val (w, h) = with(density) { TileWidth.roundToPx() to TileHeight.roundToPx() }
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .size(w, h)
                    .build()
            }
        }
    Card(
        onClick = onClick,
        shape = CardDefaults.shape(TileShape),
        colors = CardDefaults.colors(containerColor = NigiroPalette.Card),
        border =
            CardDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), shape = TileShape),
                focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = TileShape),
            ),
        scale = cardScale(),
        glow = noCardGlow(),
        modifier = modifier.size(TileWidth, TileHeight),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Dark from the bottom left, where the label sits, so it reads on any backdrop
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                0f to Color.Black.copy(alpha = 0.15f),
                                1f to Color.Black.copy(alpha = 0.85f),
                                start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                                end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
                            ),
                        ),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(tile.genre.label),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.explore_titles_count, tile.count, tile.count),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                )
            }
        }
    }
}
