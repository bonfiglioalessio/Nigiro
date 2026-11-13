package com.github.damontecres.wholphin.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowConfig
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.data.model.QuickDetailsData
import com.github.damontecres.wholphin.preferences.BackdropStyle
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.cards.BannerCard
import com.github.damontecres.wholphin.ui.cards.BannerCardWithTitle
import com.github.damontecres.wholphin.ui.cards.GenreCard
import com.github.damontecres.wholphin.ui.cards.ItemRow
import com.github.damontecres.wholphin.ui.cards.ItemRowTitle
import com.github.damontecres.wholphin.ui.cards.StudioCard
import com.github.damontecres.wholphin.ui.cards.ViewMoreCard
import com.github.damontecres.wholphin.ui.components.BasicDialog
import com.github.damontecres.wholphin.ui.components.CircularProgress
import com.github.damontecres.wholphin.ui.components.ContextMenu
import com.github.damontecres.wholphin.ui.components.ContextMenuActions
import com.github.damontecres.wholphin.ui.components.ContextMenuDialog
import com.github.damontecres.wholphin.ui.components.EpisodeName
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.FocusableItemRow
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.components.QuickDetails
import com.github.damontecres.wholphin.ui.components.TitleOrLogo
import com.github.damontecres.wholphin.ui.components.rememberLogoUrl
import com.github.damontecres.wholphin.ui.data.AddPlaylistViewModel
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialog
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialogInfo
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.detail.PlaylistDialog
import com.github.damontecres.wholphin.ui.indexOfFirstOrNull
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.playback.isPlayKeyUp
import com.github.damontecres.wholphin.ui.playback.playable
import com.github.damontecres.wholphin.ui.playback.scale
import com.github.damontecres.wholphin.ui.rememberPosition
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.util.ScrollToTopBringIntoViewSpec
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import com.github.damontecres.wholphin.util.LoadingState
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration

@Composable
fun HomePage(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: AddPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LifecycleStartEffect(Unit) {
        viewModel.init()
        onStopOrDispose { }
    }
    val state by viewModel.state.collectAsState()
    val loading = state.loadingState
    val refreshing = state.refreshState
    val homeRows = state.homeRows

    when (val loadingStatus = loading) {
        is LoadingState.Error -> {
            ErrorMessage(loadingStatus, modifier)
        }

        LoadingState.Loading,
        LoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        LoadingState.Success -> {
            var showContextMenu by remember { mutableStateOf<ContextMenu?>(null) }
            var showPlaylistDialog by remember { mutableStateOf<UUID?>(null) }
            var overviewDialog by remember { mutableStateOf<ItemDetailsDialogInfo?>(null) }
            var platformChoice by remember { mutableStateOf<PlatformTile?>(null) }

            val playlistState by playlistViewModel.playlistState.collectAsState()
            var position by rememberPosition()

            val onFocusPosition = remember { { it: RowColumn -> position = it } }
            val currentHomePrefs by rememberUpdatedState(preferences.appPreferences.homePagePreferences)
            val openCollectionById =
                remember {
                    { collectionId: String ->
                        collectionId.toUUIDOrNull()?.let { uuid ->
                            viewModel.navigationManager.navigateTo(
                                Destination.MediaItem(
                                    itemId = uuid,
                                    type = BaseItemKind.BOX_SET,
                                    collectionType = CollectionType.BOXSETS,
                                ),
                            )
                        }
                    }
                }
            val onClickItem =
                remember {
                    { clickedPosition: RowColumn, item: BaseItem ->
                        position = clickedPosition
                        if (currentHomePrefs.clickToPlay &&
                            homeRows.getOrNull(clickedPosition.row)?.isContinueWatchingNextUp == true
                        ) {
                            viewModel.navigationManager.navigateTo(Destination.Playback(item))
                        } else {
                            viewModel.navigationManager.navigateTo(item.destination())
                        }
                    }
                }
            val onLongClickItem =
                remember {
                    { clickedPosition: RowColumn, item: BaseItem ->
                        position = clickedPosition
                        val row =
                            (homeRows.getOrNull(clickedPosition.row) as? HomeRowLoadingState.Success)
                        val canRemoveContinueWatching =
                            row?.rowType is HomeRowConfig.ContinueWatching || row?.rowType is HomeRowConfig.ContinueWatchingCombined
                        val canRemoveNextUp =
                            row?.rowType is HomeRowConfig.NextUp || row?.rowType is HomeRowConfig.ContinueWatchingCombined
                        showContextMenu =
                            ContextMenu.ForBaseItem(
                                fromLongClick = true,
                                item = item,
                                chosenStreams = null,
                                showGoTo = true,
                                showStreamChoices = false,
                                canDelete =
                                    viewModel.canDelete(
                                        item,
                                        preferences.appPreferences,
                                    ),
                                canRemoveContinueWatching = canRemoveContinueWatching,
                                canRemoveNextUp = canRemoveNextUp,
                                actions =
                                    ContextMenuActions(
                                        navigateTo = viewModel.navigationManager::navigateTo,
                                        onClickWatch = viewModel::setWatched,
                                        onClickFavorite = viewModel::setFavorite,
                                        onClickAddPlaylist = { itemId ->
                                            playlistViewModel.loadPlaylists()
                                            showPlaylistDialog = itemId
                                        },
                                        onSendMediaInfo = viewModel.serverReportService::sendMediaReportFor,
                                        onDeleteItem = {
                                            viewModel.deleteItem(position, it)
                                        },
                                        onChooseVersion = { _, _ ->
                                            // Not supported on this page
                                        },
                                        onChooseTracks = {
                                            // Not supported on this page
                                        },
                                        onShowOverview = {
                                            overviewDialog = ItemDetailsDialogInfo(it)
                                        },
                                        onClearChosenStreams = {},
                                        onClickRemoveFromNextUp = viewModel::removeFromNextUp,
                                    ),
                            )
                    }
                }
            val onClickPlay =
                remember {
                    { _: RowColumn, item: BaseItem ->
                        viewModel.navigationManager.navigateTo(Destination.Playback(item))
                    }
                }
            val onClickViewMore =
                remember {
                    { _: RowColumn, row: HomeRowLoadingState.Success ->
                        val rowConfig = row.rowType
                        if (rowConfig != null) {
                            viewModel.navigationManager.navigateTo(
                                Destination.MoreHomeRow(row.title, rowConfig, row.items.size),
                            )
                        }
                    }
                }

            val enableBackdropUpdate =
                preferences.appPreferences.interfacePreferences.backdropStyle != BackdropStyle.BACKDROP_NONE
            StremioHomeContent(
                heroSlides = state.heroSlides,
                heroConfig = state.heroConfig,
                platforms = state.platforms,
                selectedFilter = state.selectedFilter,
                homeRows = homeRows,
                position = position,
                onSelectFilter = viewModel::setFilter,
                onHeroWatchNow = { item ->
                    viewModel.navigationManager.navigateTo(item.destination())
                },
                onHeroToggleFavorite = { item, favorite ->
                    viewModel.setFavorite(item.id, favorite)
                },
                onClickPlatform = { tile ->
                    val targetId = resolvePlatformTargetCollectionId(tile, state.selectedFilter)
                    if (targetId != null) {
                        openCollectionById(targetId)
                    } else if (needsPlatformTypeChoice(tile, state.selectedFilter)) {
                        platformChoice = tile
                    }
                },
                onFocusPosition = onFocusPosition,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
                onClickPlay = onClickPlay,
                showClock = preferences.appPreferences.interfacePreferences.showClock,
                onUpdateBackdrop = { item ->
                    if (enableBackdropUpdate) {
                        viewModel.updateBackdrop(item)
                    }
                },
                loadingState = refreshing,
                onClickViewMore = onClickViewMore,
                modifier = modifier,
            )

            platformChoice?.let { tile ->
                PlatformCategoryChoiceDialog(
                    platform = tile,
                    onDismissRequest = { platformChoice = null },
                    onSelectCollection = { collectionId ->
                        platformChoice = null
                        openCollectionById(collectionId)
                    },
                )
            }
            overviewDialog?.let { info ->
                ItemDetailsDialog(
                    info = info,
                    showFilePath = false,
                    onDismissRequest = { overviewDialog = null },
                )
            }
            showContextMenu?.let { contextMenu ->
                ContextMenuDialog(
                    onDismissRequest = { showContextMenu = null },
                    getMediaSource = null,
                    contextMenu = contextMenu,
                    preferredSubtitleLanguage = null,
                )
            }
            showPlaylistDialog?.let { itemId ->
                PlaylistDialog(
                    title = stringResource(R.string.add_to_playlist),
                    state = playlistState,
                    onDismissRequest = { showPlaylistDialog = null },
                    onClick = {
                        playlistViewModel.addToPlaylist(it.id, itemId)
                        showPlaylistDialog = null
                    },
                    createEnabled = true,
                    onCreatePlaylist = {
                        playlistViewModel.createPlaylistAndAddItem(it, itemId)
                        showPlaylistDialog = null
                    },
                    onSearch = playlistViewModel::loadPlaylists,
                    elevation = 3.dp,
                )
            }
        }
    }
}

val HomeRowLoadingState?.isContinueWatchingNextUp: Boolean
    get() =
        (this as? HomeRowLoadingState.Success).let { row ->
            row?.rowType is HomeRowConfig.ContinueWatching ||
                row?.rowType is HomeRowConfig.NextUp ||
                row?.rowType is HomeRowConfig.ContinueWatchingCombined
        }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StremioHomeContent(
    heroSlides: List<HeroSlide>,
    heroConfig: com.github.damontecres.wholphin.data.model.RemoteHeroConfig,
    platforms: List<PlatformTile>,
    selectedFilter: HomeMediaFilter,
    homeRows: List<HomeRowLoadingState>,
    position: RowColumn,
    onSelectFilter: (HomeMediaFilter) -> Unit,
    onHeroWatchNow: (BaseItem) -> Unit,
    onHeroToggleFavorite: (BaseItem, Boolean) -> Unit,
    onClickPlatform: (PlatformTile) -> Unit,
    onFocusPosition: (RowColumn) -> Unit,
    onClickItem: (RowColumn, BaseItem) -> Unit,
    onLongClickItem: (RowColumn, BaseItem) -> Unit,
    onClickPlay: (RowColumn, BaseItem) -> Unit,
    showClock: Boolean,
    onUpdateBackdrop: (BaseItem) -> Unit,
    loadingState: LoadingState?,
    onClickViewMore: (RowColumn, HomeRowLoadingState.Success) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusedItem =
        remember(homeRows, position) {
            (homeRows.getOrNull(position.row) as? HomeRowLoadingState.Success)?.items?.getOrNull(
                position.column,
            )
        }
    val rowFocusRequesters = remember(homeRows.size) { List(homeRows.size) { FocusRequester() } }
    val heroFocusRequester = remember { FocusRequester() }
    val filterFocusRequester = remember { FocusRequester() }
    var firstFocused by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val currentPosition by rememberUpdatedState(position)
    val currentOnFocusPosition by rememberUpdatedState(onFocusPosition)
    val currentOnClickPlay by rememberUpdatedState(onClickPlay)

    LaunchedEffect(heroSlides.size, homeRows.size) {
        if (!firstFocused && (heroSlides.isNotEmpty() || homeRows.isNotEmpty())) {
            if (heroSlides.isNotEmpty()) {
                heroFocusRequester.tryRequestFocus()
                firstFocused = true
                delay(30)
                listState.scrollToItem(0)
            } else {
                val firstRowIdx =
                    homeRows.indexOfFirstOrNull { it is HomeRowLoadingState.Success && it.items.isNotEmpty() }
                if (firstRowIdx != null) {
                    rowFocusRequesters[firstRowIdx].tryRequestFocus()
                    firstFocused = true
                } else {
                    filterFocusRequester.tryRequestFocus()
                    firstFocused = true
                }
            }
        }
    }
    LaunchedEffect(onUpdateBackdrop, focusedItem) {
        focusedItem?.let { onUpdateBackdrop.invoke(it) }
    }

    val continueRowIndex =
        remember(homeRows) {
            if (homeRows.firstOrNull().isContinueWatchingNextUp) 0 else -1
        }
    val catalogStartIndex = if (continueRowIndex == 0) 1 else 0
    val catalogRows =
        remember(homeRows, catalogStartIndex) {
            if (catalogStartIndex < homeRows.size) homeRows.subList(catalogStartIndex, homeRows.size) else emptyList()
        }

    val density = LocalDensity.current
    val spaceAbovePx =
        remember(density) {
            with(density) { 44.dp.toPx() }
        }
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current

    Box(modifier = modifier) {
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides ScrollToTopBringIntoViewSpec(spaceAbovePx),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = Cards.height2x3),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .focusRestorer()
                        .focusProperties {
                            onEnter = {
                                val rowFocused =
                                    currentPosition.row >= 0 &&
                                        rowFocusRequesters.getOrNull(currentPosition.row)?.tryRequestFocus() == true
                                if (!rowFocused) {
                                    if (heroSlides.isNotEmpty()) {
                                        heroFocusRequester.tryRequestFocus()
                                    } else {
                                        filterFocusRequester.tryRequestFocus()
                                    }
                                }
                            }
                        },
            ) {
                if (heroSlides.isNotEmpty()) {
                    item(key = "stremio_hero") {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            HomeHeroBanner(
                                slides = heroSlides,
                                config = heroConfig,
                                watchFocusRequester = heroFocusRequester,
                                onWatchNow = onHeroWatchNow,
                                onToggleFavorite = onHeroToggleFavorite,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }

                if (continueRowIndex == 0) {
                    val continueRow = homeRows[0]
                    item(key = "stremio_continue_watching") {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            HomeRowItemContent(
                                rowIndex = 0,
                                row = continueRow,
                                rowFocusRequester = rowFocusRequesters[0],
                                showViewMore = true,
                                showEmptyRows = false,
                                currentPosition = currentPosition,
                                currentOnFocusPosition = currentOnFocusPosition,
                                currentOnClickPlay = currentOnClickPlay,
                                onClickItem = onClickItem,
                                onLongClickItem = onLongClickItem,
                                onClickViewMore = onClickViewMore,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }

                item(key = "stremio_filter_bar") {
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                    ) {
                        HomeMediaFilterBar(
                            selectedFilter = selectedFilter,
                            onSelectFilter = onSelectFilter,
                            firstButtonFocusRequester = filterFocusRequester,
                        )
                    }
                }

                if (platforms.isNotEmpty()) {
                    item(key = "stremio_platforms_row") {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            PlatformsRow(
                                platforms = platforms,
                                activeFilter = selectedFilter,
                                onClickPlatform = onClickPlatform,
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = catalogRows,
                    key = { idx, row ->
                        val title = (row as? HomeRowLoadingState.Success)?.title?.toString() ?: "row"
                        "catalog_${catalogStartIndex + idx}_$title"
                    },
                ) { catalogIdx, row ->
                    val actualRowIndex = catalogStartIndex + catalogIdx
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                    ) {
                        HomeRowItemContent(
                            rowIndex = actualRowIndex,
                            row = row,
                            rowFocusRequester = rowFocusRequesters[actualRowIndex],
                            showViewMore = true,
                            showEmptyRows = false,
                            currentPosition = currentPosition,
                            currentOnFocusPosition = currentOnFocusPosition,
                            currentOnClickPlay = currentOnClickPlay,
                            onClickItem = onClickItem,
                            onLongClickItem = onLongClickItem,
                            onClickViewMore = onClickViewMore,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
        }
        when (loadingState) {
            LoadingState.Pending,
            LoadingState.Loading,
            -> {
                Box(
                    modifier =
                        Modifier
                            .padding(if (showClock) 40.dp else 20.dp)
                            .size(40.dp)
                            .align(Alignment.TopEnd),
                ) {
                    CircularProgress(Modifier.fillMaxSize())
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun HomeMediaFilterBar(
    selectedFilter: HomeMediaFilter,
    onSelectFilter: (HomeMediaFilter) -> Unit,
    firstButtonFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val filters =
        listOf(
            HomeMediaFilter.ALL to stringResource(R.string.home_filter_all),
            HomeMediaFilter.MOVIE to stringResource(R.string.home_filter_movies),
            HomeMediaFilter.SERIES to stringResource(R.string.home_filter_series),
        )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .focusGroup()
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        filters.forEachIndexed { idx, (filter, label) ->
            val selected = filter == selectedFilter
            Button(
                onClick = { onSelectFilter(filter) },
                colors =
                    ButtonDefaults.colors(
                        containerColor =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            },
                        contentColor =
                            if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                modifier =
                    if (idx == 0) {
                        Modifier.focusRequester(firstButtonFocusRequester)
                    } else {
                        Modifier
                    },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PlatformsRow(
    platforms: List<PlatformTile>,
    activeFilter: HomeMediaFilter,
    onClickPlatform: (PlatformTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        ItemRowTitle(stringResource(R.string.home_platforms_title))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusGroup(),
        ) {
            items(platforms, key = { it.key }) { tile ->
                PlatformTileCard(
                    tile = tile,
                    activeFilter = activeFilter,
                    onClick = { onClickPlatform(tile) },
                )
            }
        }
    }
}

@Composable
private fun PlatformTileCard(
    tile: PlatformTile,
    activeFilter: HomeMediaFilter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent =
        when (tile.key.lowercase()) {
            "netflix" -> Color(0xFFB81D24)
            "prime" -> Color(0xFF0078A8)
            "disney" -> Color(0xFF113CCF)
            "apple" -> Color(0xFF2B2D36)
            "sky" -> Color(0xFF0B5CAD)
            "para" -> Color(0xFF0057D9)
            else -> Color(0xFF282B36)
        }
    val subLabel =
        when (activeFilter) {
            HomeMediaFilter.MOVIE -> stringResource(R.string.home_filter_movies)
            HomeMediaFilter.SERIES -> stringResource(R.string.home_filter_series)
            HomeMediaFilter.ALL -> "${stringResource(R.string.home_filter_movies)} · ${stringResource(R.string.home_filter_series)}"
        }
    val shape = RoundedCornerShape(10.dp)
    Card(
        onClick = onClick,
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = accent),
        border =
            CardDefaults.border(
                focusedBorder =
                    Border(
                        border = BorderStroke(2.dp, Color.White),
                        shape = shape,
                    ),
            ),
        modifier = modifier.size(width = 170.dp, height = 68.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = tile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlatformCategoryChoiceDialog(
    platform: PlatformTile,
    onDismissRequest: () -> Unit,
    onSelectCollection: (String) -> Unit,
) {
    BasicDialog(onDismissRequest = onDismissRequest) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.platform_choose_category_title, platform.name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.platform_choose_category_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                platform.movieCollectionId?.let { movieId ->
                    Button(onClick = { onSelectCollection(movieId) }) {
                        Text(stringResource(R.string.home_filter_movies))
                    }
                }
                platform.seriesCollectionId?.let { seriesId ->
                    Button(onClick = { onSelectCollection(seriesId) }) {
                        Text(stringResource(R.string.home_filter_series))
                    }
                }
                OutlinedButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun HomeRowItemContent(
    rowIndex: Int,
    row: HomeRowLoadingState,
    rowFocusRequester: FocusRequester,
    showViewMore: Boolean,
    showEmptyRows: Boolean,
    currentPosition: RowColumn,
    currentOnFocusPosition: (RowColumn) -> Unit,
    currentOnClickPlay: (RowColumn, BaseItem) -> Unit,
    onClickItem: (RowColumn, BaseItem) -> Unit,
    onLongClickItem: (RowColumn, BaseItem) -> Unit,
    onClickViewMore: (RowColumn, HomeRowLoadingState.Success) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val r = row) {
        is HomeRowLoadingState.Loading,
        is HomeRowLoadingState.Pending,
        -> {
            FocusableItemRow(
                title = r.title.getString(),
                subtitle = stringResource(R.string.loading),
                modifier = modifier,
            )
        }

        is HomeRowLoadingState.Error -> {
            FocusableItemRow(
                title = r.title.getString(),
                subtitle = r.localizedMessage,
                isError = true,
                modifier = modifier,
            )
        }

        is HomeRowLoadingState.Success -> {
            if (r.items.isNotEmpty()) {
                val viewOptions = r.viewOptions
                ItemRow(
                    title = r.title.getString(),
                    items = r.items,
                    onClickItem =
                        remember(rowIndex, onClickItem) {
                            { index, item ->
                                onClickItem.invoke(
                                    RowColumn(rowIndex, index),
                                    item,
                                )
                            }
                        },
                    onLongClickItem =
                        remember(rowIndex, onLongClickItem) {
                            { index, item ->
                                onLongClickItem.invoke(
                                    RowColumn(rowIndex, index),
                                    item,
                                )
                            }
                        },
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .focusGroup()
                            .focusRequester(rowFocusRequester),
                    horizontalPadding = viewOptions.spacing.dp,
                    cardContent = { index, item, cardModifier, onClick, onLongClick ->
                        val onFocus =
                            remember(rowIndex, index) {
                                { isFocused: Boolean ->
                                    if (isFocused) {
                                        currentOnFocusPosition(RowColumn(rowIndex, index))
                                    }
                                }
                            }
                        val onKey =
                            remember(item) {
                                { event: KeyEvent ->
                                    if (isPlayKeyUp(event) && item?.type?.playable == true) {
                                        Timber.v("Clicked play on ${item.id}")
                                        currentOnClickPlay(currentPosition, item)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        HomePageCardContent(
                            index = index,
                            item = item,
                            onClick = onClick,
                            onLongClick = onLongClick,
                            viewOptions = viewOptions,
                            modifier =
                                cardModifier
                                    .onFocusChanged { onFocus(it.isFocused) }
                                    .onKeyEvent { onKey(it) },
                        )
                    },
                    showViewMore = showViewMore && r.showViewMore,
                    viewMoreCardContent = { mod ->
                        HomePageViewMoreCard(
                            isEpisode = r.items.last()?.type == BaseItemKind.EPISODE,
                            onClick = {
                                onClickViewMore.invoke(
                                    RowColumn(rowIndex, r.items.size),
                                    r,
                                )
                            },
                            onLongClick = {},
                            viewOptions = viewOptions,
                            modifier =
                                mod.onFocusChanged {
                                    if (it.isFocused) {
                                        currentOnFocusPosition.invoke(
                                            RowColumn(rowIndex, r.items.size),
                                        )
                                    }
                                },
                        )
                    },
                )
            } else if (showEmptyRows) {
                FocusableItemRow(
                    title = r.title.getString(),
                    subtitle = stringResource(R.string.no_results),
                    modifier = modifier,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePageContent(
    homeRows: List<HomeRowLoadingState>,
    position: RowColumn,
    onFocusPosition: (RowColumn) -> Unit,
    onClickItem: (RowColumn, BaseItem) -> Unit,
    onLongClickItem: (RowColumn, BaseItem) -> Unit,
    onClickPlay: (RowColumn, BaseItem) -> Unit,
    showClock: Boolean,
    onUpdateBackdrop: (BaseItem) -> Unit,
    showLogo: Boolean,
    showViewMore: Boolean,
    modifier: Modifier = Modifier,
    loadingState: LoadingState? = null,
    listState: LazyListState = rememberLazyListState(),
    takeFocus: Boolean = true,
    showEmptyRows: Boolean = false,
    headerComposable: @Composable (focusedItem: BaseItem?) -> Unit = { focusedItem ->
        HomePageHeader(
            item = focusedItem,
            showLogo = showLogo,
            modifier = HeaderUtils.modifier,
        )
    },
    onClickViewMore: (RowColumn, HomeRowLoadingState.Success) -> Unit = { _, _ -> },
) {
    val focusedItem =
        remember(homeRows, position) {
            (homeRows.getOrNull(position.row) as? HomeRowLoadingState.Success)?.items?.getOrNull(
                position.column,
            )
        }

    val rowFocusRequesters = remember(homeRows.size) { List(homeRows.size) { FocusRequester() } }
    var firstFocused by remember { mutableStateOf(false) }

    val currentPosition by rememberUpdatedState(position)
    val currentOnFocusPosition by rememberUpdatedState(onFocusPosition)
    val currentOnClickPlay by rememberUpdatedState(onClickPlay)

    if (takeFocus) {
        LaunchedEffect(homeRows) {
            if (!firstFocused && homeRows.isNotEmpty()) {
                if (position.row >= 0) {
                    val index = position.row.coerceIn(0, rowFocusRequesters.lastIndex)
                    rowFocusRequesters.getOrNull(index)?.tryRequestFocus()
                    firstFocused = true
                } else {
                    // Waiting for the first home row to load, then focus on it
                    homeRows
                        .indexOfFirstOrNull { it is HomeRowLoadingState.Success && it.items.isNotEmpty() }
                        ?.let {
                            rowFocusRequesters[it].tryRequestFocus()
                            firstFocused = true
                            delay(50)
                            listState.scrollToItem(it)
                        }
                }
            }
        }
    }
    LaunchedEffect(onUpdateBackdrop, focusedItem) {
        focusedItem?.let { onUpdateBackdrop.invoke(it) }
    }
    Box(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .focusProperties {
                        onEnter = {
                            rowFocusRequesters.getOrNull(currentPosition.row)?.tryRequestFocus()
                        }
                    }.fillMaxSize(),
        ) {
            headerComposable.invoke(focusedItem)

            val density = LocalDensity.current
            val spaceAbovePx =
                remember(density) {
                    with(density) {
                        // The size of the row titles & spacing
                        50.dp.toPx()
                    }
                }
            val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
            CompositionLocalProvider(
                LocalBringIntoViewSpec provides ScrollToTopBringIntoViewSpec(spaceAbovePx),
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding =
                        PaddingValues(
                            bottom = Cards.height2x3,
                        ),
                    modifier =
                        Modifier
                            .focusRestorer(),
                ) {
                    itemsIndexed(homeRows) { rowIndex, row ->
                        val rowModifier =
                            Modifier
                                .animateItem(placementSpec = null)
                                .padding(bottom = 8.dp)
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            HomeRowItemContent(
                                rowIndex = rowIndex,
                                row = row,
                                rowFocusRequester = rowFocusRequesters[rowIndex],
                                showViewMore = showViewMore,
                                showEmptyRows = showEmptyRows,
                                currentPosition = currentPosition,
                                currentOnFocusPosition = currentOnFocusPosition,
                                currentOnClickPlay = currentOnClickPlay,
                                onClickItem = onClickItem,
                                onLongClickItem = onLongClickItem,
                                onClickViewMore = onClickViewMore,
                                modifier = rowModifier,
                            )
                        }
                    }
                }
            }
        }
        when (loadingState) {
            LoadingState.Pending,
            LoadingState.Loading,
            -> {
                Box(
                    modifier =
                        Modifier
                            .padding(if (showClock) 40.dp else 20.dp)
                            .size(40.dp)
                            .align(Alignment.TopEnd),
                ) {
                    CircularProgress(Modifier.fillMaxSize())
                }
            }

            else -> {}
        }
    }
}

@Composable
fun HomePageHeader(
    item: BaseItem?,
    showLogo: Boolean,
    modifier: Modifier = Modifier,
) {
    val isEpisode = item?.type == BaseItemKind.EPISODE
    val title =
        remember(item?.id) {
            when (item?.type) {
                BaseItemKind.EPISODE,
                BaseItemKind.SEASON,
                -> item.data.seriesName

                else -> item?.title
            }
        }
    val subtitle =
        remember(item?.id) {
            when (item?.type) {
                BaseItemKind.SEASON,
                BaseItemKind.EPISODE,
                -> item.name

                else -> null
            }
        }
    HomePageHeader(
        title = title,
        subtitle = subtitle,
        overview = item?.data?.overview,
        overviewTwoLines = isEpisode,
        quickDetails = item?.ui?.quickDetails,
        timeRemaining = item?.timeRemainingOrRuntime,
        endsAt = item?.data?.endDate,
        showLogo = showLogo,
        logoImageUrl = rememberLogoUrl(item),
        modifier = modifier,
    )
}

@Composable
fun HomePageHeader(
    title: String?,
    subtitle: String?,
    overview: String?,
    overviewTwoLines: Boolean,
    quickDetails: QuickDetailsData?,
    timeRemaining: Duration?,
    endsAt: DateTime?,
    showLogo: Boolean,
    logoImageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        TitleOrLogo(
            title = title,
            logoImageUrl = logoImageUrl,
            showLogo = showLogo,
            modifier = Modifier.fillMaxWidth(.75f),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .fillMaxWidth(.6f),
        ) {
            if (subtitle != null) {
                EpisodeName(subtitle)
            }
            QuickDetails(quickDetails, timeRemaining, endsAt = endsAt)
            val overviewModifier =
                Modifier
                    .padding(0.dp)
                    .height(48.dp + if (!overviewTwoLines) 12.dp else 0.dp)
                    .width(400.dp)
            if (overview.isNotNullOrBlank()) {
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (overviewTwoLines) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = overviewModifier,
                )
            } else {
                Spacer(overviewModifier)
            }
        }
    }
}

@Composable
fun HomePageCardContent(
    index: Int,
    item: BaseItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    viewOptions: HomeRowViewOptions,
    modifier: Modifier,
) {
    when (item?.type) {
        BaseItemKind.GENRE -> {
            GenreCard(
                genreId = item.id,
                name = item.name,
                imageUrl = item.imageUrlOverride,
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = modifier.height(viewOptions.heightDp.dp),
            )
        }

        BaseItemKind.STUDIO -> {
            StudioCard(
                studioId = item.id,
                name = item.name,
                imageUrl = item.imageUrlOverride,
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = modifier.height(viewOptions.heightDp.dp),
            )
        }

        else -> {
            val imageType =
                remember(item, viewOptions) {
                    if (item?.type == BaseItemKind.EPISODE) {
                        viewOptions.episodeImageType.imageType
                    } else {
                        viewOptions.imageType.imageType
                    }
                }
            val ratio =
                remember(item, viewOptions) {
                    if (item?.type == BaseItemKind.EPISODE) {
                        viewOptions.episodeAspectRatio.ratio
                    } else {
                        viewOptions.aspectRatio.ratio
                    }
                }
            val scale =
                remember(item, viewOptions) {
                    if (item?.type == BaseItemKind.EPISODE) {
                        viewOptions.episodeContentScale.scale
                    } else {
                        viewOptions.contentScale.scale
                    }
                }
            if (viewOptions.showTitles) {
                BannerCardWithTitle(
                    title = item?.title,
                    subtitle = item?.subtitle,
                    item = item,
                    aspectRatio = ratio,
                    imageType = imageType,
                    imageContentScale = scale,
                    cornerText = item?.ui?.episodeUnplayedCornerText,
                    played = item?.data?.userData?.played ?: false,
                    favorite = item?.favorite ?: false,
                    playPercent =
                        item?.data?.userData?.playedPercentage
                            ?: 0.0,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    modifier = modifier,
                    cardHeight = viewOptions.heightDp.dp,
                    useSeriesForPrimary = viewOptions.useSeries,
                )
            } else {
                BannerCard(
                    name = item?.data?.seriesName ?: item?.name,
                    item = item,
                    aspectRatio = ratio,
                    imageType = imageType,
                    imageContentScale = scale,
                    cornerText = item?.ui?.episodeUnplayedCornerText,
                    played = item?.data?.userData?.played ?: false,
                    favorite = item?.favorite ?: false,
                    playPercent =
                        item?.data?.userData?.playedPercentage
                            ?: 0.0,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    modifier = modifier,
                    interactionSource = null,
                    cardHeight = viewOptions.heightDp.dp,
                    useSeriesForPrimary = viewOptions.useSeries,
                )
            }
        }
    }
}

@Composable
fun HomePageViewMoreCard(
    isEpisode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    viewOptions: HomeRowViewOptions,
    modifier: Modifier,
) {
    val aspectRatio =
        remember(isEpisode, viewOptions) {
            if (isEpisode) {
                viewOptions.episodeAspectRatio
            } else {
                viewOptions.aspectRatio
            }
        }
    ViewMoreCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        aspectRatio = aspectRatio,
        size = DpSize(height = viewOptions.heightDp.dp, width = Dp.Unspecified),
        showTitle = viewOptions.showTitles,
    )
}
