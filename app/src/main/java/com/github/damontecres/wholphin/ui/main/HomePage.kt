package com.github.damontecres.wholphin.ui.main

import com.github.damontecres.wholphin.ui.detail.movie.VersionPickerDialog
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowConfig
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.data.model.QuickDetailsData
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.cards.ContinueWatchingCard
import com.github.damontecres.wholphin.ui.cards.ExpandingPosterCard
import com.github.damontecres.wholphin.ui.cards.BannerCard
import com.github.damontecres.wholphin.ui.cards.BannerCardWithTitle
import com.github.damontecres.wholphin.ui.cards.GenreCard
import com.github.damontecres.wholphin.ui.cards.ItemRow
import com.github.damontecres.wholphin.ui.cards.ItemRowTitle
import com.github.damontecres.wholphin.ui.cards.StudioCard
import com.github.damontecres.wholphin.ui.cards.ViewMoreCard
import com.github.damontecres.wholphin.ui.cards.buttonScale
import com.github.damontecres.wholphin.ui.cards.cardScale
import com.github.damontecres.wholphin.ui.cards.noButtonGlow
import com.github.damontecres.wholphin.ui.cards.noCardGlow
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
import com.github.damontecres.wholphin.ui.components.TextButton
import com.github.damontecres.wholphin.ui.components.TitleOrLogo
import com.github.damontecres.wholphin.ui.components.rememberLogoUrl
import com.github.damontecres.wholphin.ui.data.AddPlaylistViewModel
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialog
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialogInfo
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.detail.PlaylistDialog
import com.github.damontecres.wholphin.ui.detail.series.isUnreleased
import com.github.damontecres.wholphin.ui.indexOfFirstOrNull
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.playback.isPlayKeyUp
import com.github.damontecres.wholphin.ui.playback.playable
import com.github.damontecres.wholphin.ui.playback.scale
import com.github.damontecres.wholphin.ui.rememberPosition
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.util.ScrollToTopBringIntoViewSpec
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import com.github.damontecres.wholphin.util.LoadingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
            Column(modifier = modifier) {
                TextButton(
                    stringRes = R.string.retry,
                    onClick = viewModel::retry,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                )
                ErrorMessage(loadingStatus)
            }
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
            // Read through a lambda so that moving focus doesn't recompose the whole page
            val positionProvider = remember { { position } }
            val currentHomePrefs by rememberUpdatedState(preferences.appPreferences.homePagePreferences)
            val currentHomeRows by rememberUpdatedState(homeRows)
            val currentPreferences by rememberUpdatedState(preferences)
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
                            currentHomeRows.getOrNull(clickedPosition.row)?.isContinueWatchingNextUp == true &&
                            item.playable &&
                            !item.isUnreleased()
                        ) {
                            viewModel.versionPicker.play(item, item.playbackPosition)
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
                            (currentHomeRows.getOrNull(clickedPosition.row) as? HomeRowLoadingState.Success)
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
                                        currentPreferences.appPreferences,
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
                                        onPlay = viewModel.versionPicker::play,
                                        onChooseVersionPicker = viewModel.versionPicker::choose,
                                    ),
                            )
                    }
                }
            val onClickPlay =
                remember {
                    { _: RowColumn, item: BaseItem ->
                        viewModel.versionPicker.play(item, item.playbackPosition)
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

            val currentSelectedFilter by rememberUpdatedState(state.selectedFilter)
            val onHeroWatchNow =
                remember {
                    { item: BaseItem ->
                        // Come back to the hero instead of the last focused row
                        position = RowColumn(-1, -1)
                        viewModel.navigationManager.navigateTo(item.destination())
                    }
                }
            val onHeroToggleFavorite =
                remember<(BaseItem, Boolean) -> Unit> {
                    { item, favorite ->
                        viewModel.setFavorite(item.id, favorite)
                    }
                }
            val onHeroPlay =
                remember {
                    { item: BaseItem ->
                        // Come back to the hero instead of the last focused row
                        position = RowColumn(-1, -1)
                        viewModel.navigationManager.navigateTo(Destination.Playback(item))
                    }
                }
            val onClickPlatform =
                remember<(PlatformTile) -> Unit> {
                    { tile ->
                        val targetId = resolvePlatformTargetCollectionId(tile, currentSelectedFilter)
                        if (targetId != null) {
                            openCollectionById(targetId)
                        } else if (needsPlatformTypeChoice(tile, currentSelectedFilter)) {
                            platformChoice = tile
                        }
                    }
                }
            // StremioHomeContent renders FocusedItemBackdrop directly; skip duplicate BackdropService Palette work
            val onUpdateBackdrop = remember { { _: BaseItem -> } }
            StremioHomeContent(
                heroSlides = state.heroSlides,
                heroConfig = state.heroConfig,
                platforms = state.platforms,
                selectedFilter = state.selectedFilter,
                homeRows = homeRows,
                positionProvider = positionProvider,
                onSelectFilter = viewModel::setFilter,
                onHeroWatchNow = onHeroWatchNow,
                onHeroToggleFavorite = onHeroToggleFavorite,
                platformFilter = state.platformFilter,
                onFocusPlatform = viewModel::setPlatformFilter,
                onHeroPlay = onHeroPlay,
                onClickPlatform = onClickPlatform,
                onFocusPosition = onFocusPosition,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
                onClickPlay = onClickPlay,
                showClock = preferences.appPreferences.interfacePreferences.showClock,
                onUpdateBackdrop = onUpdateBackdrop,
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
            val pickRequest by viewModel.versionPicker.request.collectAsState()
            pickRequest?.let { request ->
                VersionPickerDialog(
                    item = request.item,
                    picker = request.picker,
                    onPick = viewModel.versionPicker::pick,
                    onDismissRequest = viewModel.versionPicker::dismiss,
                    onDetails = { viewModel.navigationManager.navigateTo(request.item.destination()) },
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
    positionProvider: () -> RowColumn,
    onSelectFilter: (HomeMediaFilter) -> Unit,
    onHeroWatchNow: (BaseItem) -> Unit,
    onHeroToggleFavorite: (BaseItem, Boolean) -> Unit,
    onHeroPlay: (BaseItem) -> Unit,
    platformFilter: String? = null,
    onFocusPlatform: (String?) -> Unit = {},
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
    val rowFocusRequesters = remember { mutableListOf<FocusRequester>() }
    while (rowFocusRequesters.size < homeRows.size) {
        rowFocusRequesters.add(FocusRequester())
    }
    while (rowFocusRequesters.size > homeRows.size) {
        rowFocusRequesters.removeAt(rowFocusRequesters.lastIndex)
    }
    val heroFocusRequester = remember { FocusRequester() }
    val filterFocusRequester = remember { FocusRequester() }
    // Saveable: when coming back to Home (eg from details or playback) restore focus instead of jumping to the hero
    var firstFocused by rememberSaveable { mutableStateOf(false) }
    var focusRestored by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val currentOnFocusPosition by rememberUpdatedState(onFocusPosition)
    val currentOnClickPlay by rememberUpdatedState(onClickPlay)

    LaunchedEffect(heroSlides.size, homeRows.size) {
        if (firstFocused && !focusRestored && (heroSlides.isNotEmpty() || homeRows.isNotEmpty())) {
            focusRestored = true
            val row = positionProvider().row
            val restored = row >= 0 && rowFocusRequesters.getOrNull(row)?.tryRequestFocus() == true
            if (!restored && heroSlides.isNotEmpty()) {
                heroFocusRequester.tryRequestFocus()
            }
        } else if (!firstFocused && (heroSlides.isNotEmpty() || homeRows.isNotEmpty())) {
            focusRestored = true
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

    val continueRowIndex =
        remember(homeRows) {
            if (homeRows.firstOrNull().isContinueWatchingNextUp) 0 else -1
        }
    val rankedStartIndex = if (continueRowIndex == 0) 1 else 0
    val rankedRows =
        remember(homeRows, rankedStartIndex) {
            homeRows
                .drop(rankedStartIndex)
                .takeWhile { (it as? HomeRowLoadingState.Success)?.ranked == true }
        }
    val catalogStartIndex = rankedStartIndex + rankedRows.size
    val catalogRows =
        remember(homeRows, catalogStartIndex) {
            if (catalogStartIndex < homeRows.size) homeRows.subList(catalogStartIndex, homeRows.size) else emptyList()
        }

    val density = LocalDensity.current
    // Focused row kept at a fixed height: its title and the bottom of the row above stay visible (AIOStreams-style)
    val spaceAbovePx =
        remember(density) {
            with(density) { NigiroDimens.FocusedRowTop.toPx() }
        }
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
    var isHeroFocused by remember { mutableStateOf(false) }
    var isFilterFocused by remember { mutableStateOf(false) }
    val stremioBringIntoViewSpec =
        remember(spaceAbovePx) {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float,
                ): Float =
                    when {
                        // Hero scrolls to index 0 via LaunchedEffect so bottom-aligned buttons never clip the banner top
                        isHeroFocused -> 0f
                        isFilterFocused && offset >= spaceAbovePx && offset + size <= containerSize - spaceAbovePx -> 0f
                        else -> offset - spaceAbovePx
                    }
            }
        }

    val currentContinueRowIndex by rememberUpdatedState(continueRowIndex)
    val currentHasPlatforms by rememberUpdatedState(platforms.isNotEmpty())
    val currentCatalogStartIndex by rememberUpdatedState(catalogStartIndex)

    LaunchedEffect(listState) {
        snapshotFlow { isHeroFocused }.collectLatest { focused ->
            if (focused) {
                listState.scrollToItem(0)
            }
        }
    }

    // Room below the last row, so it too can scroll up to the fixed focused-row height
    val bottomRoom = LocalConfiguration.current.screenHeightDp.dp

    Box(modifier = modifier) {
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides stremioBringIntoViewSpec,
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(NigiroDimens.SectionGap),
                contentPadding = PaddingValues(bottom = bottomRoom),
                modifier =
                    Modifier
                        .fillMaxSize()
                        // Span full screen width so LazyColumn's vertical scroll clip never cuts the hero at the sidebar
                        .bleedHorizontally(NigiroDimens.HeroBleedStart, NigiroDimens.HeroBleedEnd)
                        .focusRestorer()
                        .focusProperties {
                            onEnter = {
                                val currentRow = positionProvider().row
                                val rowFocused =
                                    currentRow >= 0 &&
                                        rowFocusRequesters.getOrNull(currentRow)?.tryRequestFocus() == true
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
                    item(key = "stremio_hero", contentType = "hero") {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            HomeHeroBanner(
                                slides = heroSlides,
                                config = heroConfig,
                                watchFocusRequester = heroFocusRequester,
                                onPlay = onHeroPlay,
                                onDetails = onHeroWatchNow,
                                onToggleFavorite = onHeroToggleFavorite,
                                modifier = Modifier.onFocusChanged { isHeroFocused = it.hasFocus },
                            )
                        }
                    }
                } else {
                    item(key = "stremio_filter_bar") {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            HomeMediaFilterBar(
                                selectedFilter = selectedFilter,
                                onSelectFilter = onSelectFilter,
                                firstButtonFocusRequester = filterFocusRequester,
                                modifier =
                                    Modifier
                                        .padding(
                                            start = NigiroDimens.HeroBleedStart + NigiroDimens.ContentStart,
                                            top = 20.dp,
                                            end = NigiroDimens.HeroBleedEnd,
                                        ).onFocusChanged { isFilterFocused = it.hasFocus },
                            )
                        }
                    }
                }

                if (continueRowIndex == 0) {
                    val continueRow = homeRows[0]
                    item(key = "stremio_continue_watching", contentType = "continue_watching") {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            HomeRowItemContent(
                                rowIndex = 0,
                                row = continueRow,
                                rowFocusRequester = rowFocusRequesters[0],
                                showViewMore = true,
                                showEmptyRows = false,
                                currentOnFocusPosition = currentOnFocusPosition,
                                currentOnClickPlay = currentOnClickPlay,
                                onClickItem = onClickItem,
                                onLongClickItem = onLongClickItem,
                                onClickViewMore = onClickViewMore,
                                aioCards = true,
                                modifier =
                                    Modifier
                                        .padding(start = NigiroDimens.HeroBleedStart, end = NigiroDimens.HeroBleedEnd),
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = rankedRows,
                    key = { idx, row -> "ranked_${(row as? HomeRowLoadingState.Success)?.title ?: idx}" },
                    contentType = { _, _ -> "ranked_catalog_row" },
                ) { rankedIdx, row ->
                    val actualRowIndex = rankedStartIndex + rankedIdx
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                    ) {
                        HomeRowItemContent(
                            rowIndex = actualRowIndex,
                            row = row,
                            rowFocusRequester = rowFocusRequesters[actualRowIndex],
                            showViewMore = true,
                            showEmptyRows = false,
                            currentOnFocusPosition = currentOnFocusPosition,
                            currentOnClickPlay = currentOnClickPlay,
                            onClickItem = onClickItem,
                            onLongClickItem = onLongClickItem,
                            onClickViewMore = onClickViewMore,
                            aioCards = true,
                            modifier =
                                Modifier
                                    .padding(start = NigiroDimens.HeroBleedStart, end = NigiroDimens.HeroBleedEnd),
                        )
                    }
                }

                if (platforms.isNotEmpty()) {
                    item(key = "stremio_platforms_row", contentType = "platforms") {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides defaultBringIntoViewSpec,
                        ) {
                            PlatformsRow(
                                platforms = platforms,
                                activeFilter = selectedFilter,
                                activePlatform = platformFilter,
                                onClickPlatform = onClickPlatform,
                                onFocusPlatform = onFocusPlatform,
                                modifier = Modifier.padding(start = NigiroDimens.HeroBleedStart, end = NigiroDimens.HeroBleedEnd),
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = catalogRows,
                    // Independent of whether the Continue Watching row is shown, so rows keep their state & focus
                    key = { idx, row -> "catalog_${(row as? HomeRowLoadingState.Success)?.title ?: idx}" },
                    contentType = { _, _ -> "catalog_row" },
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
                            currentOnFocusPosition = currentOnFocusPosition,
                            currentOnClickPlay = currentOnClickPlay,
                            onClickItem = onClickItem,
                            onLongClickItem = onLongClickItem,
                            onClickViewMore = onClickViewMore,
                            aioCards = true,
                            modifier =
                                Modifier
                                    .padding(start = NigiroDimens.HeroBleedStart, end = NigiroDimens.HeroBleedEnd),
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
internal fun HomeMediaFilterBar(
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
    // Segmented control: the three choices in one 36 dp pill, so they read as a single choice
    val trackShape = RoundedCornerShape(50)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .height(36.dp)
                .background(Color.White.copy(alpha = 0.06f), trackShape)
                .border(1.dp, Color.White.copy(alpha = 0.10f), trackShape)
                .padding(3.dp)
                .focusGroup(),
    ) {
        filters.forEachIndexed { idx, (filter, label) ->
            val selected = filter == selectedFilter
            val shape = RoundedCornerShape(50)
            Button(
                onClick = { onSelectFilter(filter) },
                shape = ButtonDefaults.shape(shape),
                colors =
                    ButtonDefaults.colors(
                        containerColor = if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                        contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = NigiroPalette.Background,
                    ),
                scale = buttonScale(),
                glow = noButtonGlow(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier =
                    Modifier
                        .height(30.dp)
                        .then(if (idx == 0) Modifier.focusRequester(firstButtonFocusRequester) else Modifier),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight()) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformsRow(
    platforms: List<PlatformTile>,
    activeFilter: HomeMediaFilter,
    activePlatform: String?,
    onClickPlatform: (PlatformTile) -> Unit,
    onFocusPlatform: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resting on a tile filters the rows below to that platform; the short delay avoids reloading rows
    // while moving across tiles
    var focusedKey by remember { mutableStateOf<String?>(activePlatform) }
    var hasFocus by remember { mutableStateOf(false) }
    val currentOnFocusPlatform by rememberUpdatedState(onFocusPlatform)
    LaunchedEffect(focusedKey, hasFocus) {
        if (hasFocus) {
            delay(250)
            currentOnFocusPlatform(focusedKey)
        }
    }
    Column(
        // The row's vertical content padding (room for the focus scale) completes the title gap
        verticalArrangement = Arrangement.spacedBy(NigiroDimens.TitleGap),
        modifier = modifier.fillMaxWidth(),
    ) {
        ItemRowTitle(
            stringResource(R.string.home_platforms_title),
            startPadding = NigiroDimens.ContentStart,
            style = NigiroDimens.RowTitle,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(NigiroDimens.CardGap),
            contentPadding =
                PaddingValues(start = NigiroDimens.ContentStart, end = NigiroDimens.ContentEnd, top = 6.dp, bottom = 6.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { hasFocus = it.hasFocus }
                    .focusGroup(),
        ) {
            item(key = "all_platforms", contentType = "platform_all") {
                AllPlatformsTile(
                    active = activePlatform == null,
                    modifier = Modifier.onFocusChanged { if (it.isFocused) focusedKey = null },
                )
            }
            items(platforms, key = { it.key }, contentType = { "platform_tile" }) { tile ->
                val currentOnClickPlatform by rememberUpdatedState(onClickPlatform)
                val currentTile by rememberUpdatedState(tile)
                val onTileClick = remember(tile.key) { { currentOnClickPlatform(currentTile) } }
                PlatformTileCard(
                    tile = tile,
                    activeFilter = activeFilter,
                    active = activePlatform == tile.key,
                    onClick = onTileClick,
                    modifier = Modifier.onFocusChanged { if (it.isFocused) focusedKey = tile.key },
                )
            }
        }
    }
}

private val PlatformTileShape = RoundedCornerShape(10.dp)

@Composable
internal fun PlatformTileCard(
    tile: PlatformTile,
    activeFilter: HomeMediaFilter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    width: Dp = NigiroDimens.PlatformTileWidth,
    height: Dp = NigiroDimens.PlatformTileHeight,
) {
    // Neutral tile in the theme's gray (the cover image, when present, carries the platform's look)
    val accent = NigiroPalette.SurfaceRaised
    val subLabel =
        when (activeFilter) {
            HomeMediaFilter.MOVIE -> stringResource(R.string.home_filter_movies)
            HomeMediaFilter.SERIES -> stringResource(R.string.home_filter_series)
            HomeMediaFilter.ALL -> "${stringResource(R.string.home_filter_movies)} · ${stringResource(R.string.home_filter_series)}"
        }
    val shape = PlatformTileShape
    val context = LocalContext.current
    val density = LocalDensity.current
    val (tileWidthPx, tileHeightPx) =
        remember(density, width, height) {
            with(density) { width.roundToPx() to height.roundToPx() }
        }
    var imageFailed by remember(tile.coverUrl) { mutableStateOf(false) }
    val coverRequest =
        remember(context, tile.coverUrl, tileWidthPx, tileHeightPx) {
            tile.coverUrl?.takeIf { it.isNotBlank() }?.let { url ->
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .size(tileWidthPx, tileHeightPx)
                    .build()
            }
        }
    val showCoverImage = coverRequest != null && !imageFailed
    Card(
        onClick = onClick,
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = accent),
        border =
            CardDefaults.border(
                // Pink outline on the platform the rows below are filtered to
                border =
                    Border(
                        BorderStroke(if (active) 2.dp else 1.dp, if (active) NigiroPalette.Pink else Color.White.copy(alpha = 0.10f)),
                        shape = shape,
                    ),
                focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape),
            ),
        scale = cardScale(),
        glow = noCardGlow(),
        modifier = modifier.size(width = width, height = height),
    ) {
        if (showCoverImage) {
            AsyncImage(
                model = coverRequest,
                contentDescription = tile.name,
                contentScale = ContentScale.Crop,
                onError = remember(tile.coverUrl) { { imageFailed = true } },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
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
    currentOnFocusPosition: (RowColumn) -> Unit,
    currentOnClickPlay: (RowColumn, BaseItem) -> Unit,
    onClickItem: (RowColumn, BaseItem) -> Unit,
    onLongClickItem: (RowColumn, BaseItem) -> Unit,
    onClickViewMore: (RowColumn, HomeRowLoadingState.Success) -> Unit,
    modifier: Modifier = Modifier,
    // AIOStreams-style cards (expanding posters, landscape continue watching): Stremio home only
    aioCards: Boolean = false,
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
                val isContinueRow = aioCards && r.isContinueWatchingNextUp
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
                        val currentCardItem by rememberUpdatedState(item)
                        val onKey =
                            remember(rowIndex, index) {
                                { event: KeyEvent ->
                                    val focused = currentCardItem
                                    if (isPlayKeyUp(event) && focused?.type?.playable == true && !focused.isUnreleased()) {
                                        Timber.v("Clicked play on ${focused.id}")
                                        currentOnClickPlay(RowColumn(rowIndex, index), focused)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        val cardMod =
                            cardModifier
                                .onFocusChanged { onFocus(it.isFocused) }
                                .onKeyEvent { onKey(it) }
                        when {
                            isContinueRow -> {
                                ContinueWatchingCard(
                                    item = item,
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                    cardHeight = NigiroDimens.ContinueCardHeight,
                                    modifier = cardMod,
                                )
                            }

                            aioCards && (item?.type == BaseItemKind.MOVIE || item?.type == BaseItemKind.SERIES) -> {
                                ExpandingPosterCard(
                                    item = item,
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                    cardHeight = NigiroDimens.PosterHeight,
                                    onDisk = item.isOnDisk(),
                                    rank = if (r.ranked && index < TOP_10_LIMIT) index + 1 else null,
                                    modifier = cardMod,
                                )
                            }

                            else -> {
                                HomePageCardContent(
                                    index = index,
                                    item = item,
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                    viewOptions = viewOptions,
                                    modifier = cardMod,
                                )
                            }
                        }
                    },
                    showViewMore = showViewMore && r.showViewMore,
                    itemKey = { it.id.toString() },
                    contentStartPadding = NigiroDimens.ContentStart,
                    titleStartPadding = NigiroDimens.ContentStart,
                    titleStyle = NigiroDimens.RowTitle,
                    // Plus the row's 8 dp top padding: room for the focused card's scale-up below the title
                    titleGap = NigiroDimens.TitleGap,
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

    val rowFocusRequesters = remember { mutableListOf<FocusRequester>() }
    while (rowFocusRequesters.size < homeRows.size) {
        rowFocusRequesters.add(FocusRequester())
    }
    while (rowFocusRequesters.size > homeRows.size) {
        rowFocusRequesters.removeAt(rowFocusRequesters.lastIndex)
    }
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
                    itemsIndexed(
                        homeRows,
                        // Lazy keys must be Bundle-able: a StringProvider title is stored by its string form.
                        key = { index, row -> (row as? HomeRowLoadingState.Success)?.title?.toString() ?: index },
                        contentType = { _, _ -> "home_row" },
                    ) { rowIndex, row ->
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
    showFavorite: Boolean = true,
) {
    // The rating is shown in the focused item header, and the hero/rows show the on-disk mark
    val onDisk = item?.isOnDisk() == true
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
            val isEpisode = item?.type == BaseItemKind.EPISODE
            val imageType =
                if (isEpisode) viewOptions.episodeImageType.imageType else viewOptions.imageType.imageType
            val ratio =
                if (isEpisode) viewOptions.episodeAspectRatio.ratio else viewOptions.aspectRatio.ratio
            val scale =
                if (isEpisode) viewOptions.episodeContentScale.scale else viewOptions.contentScale.scale
            if (viewOptions.showTitles) {
                BannerCardWithTitle(
                    title = item?.title,
                    subtitle = item?.subtitle,
                    item = item,
                    aspectRatio = ratio,
                    imageType = imageType,
                    imageContentScale = scale,
                    played = item?.data?.userData?.played ?: false,
                    favorite = showFavorite && item?.favorite == true,
                    playPercent =
                        item?.data?.userData?.playedPercentage
                            ?: 0.0,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    modifier = modifier,
                    cardHeight = viewOptions.heightDp.dp,
                    useSeriesForPrimary = viewOptions.useSeries,
                    showRating = false,
                    onDisk = onDisk,
                )
            } else {
                BannerCard(
                    name = item?.data?.seriesName ?: item?.name,
                    item = item,
                    aspectRatio = ratio,
                    imageType = imageType,
                    imageContentScale = scale,
                    played = item?.data?.userData?.played ?: false,
                    favorite = showFavorite && item?.favorite == true,
                    playPercent =
                        item?.data?.userData?.playedPercentage
                            ?: 0.0,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    modifier = modifier,
                    interactionSource = null,
                    cardHeight = viewOptions.heightDp.dp,
                    useSeriesForPrimary = viewOptions.useSeries,
                    showRating = false,
                    onDisk = onDisk,
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

/**
 * Lets a full-width element (the hero) extend past the page's horizontal padding to the screen edges
 */
private fun Modifier.bleedHorizontally(
    start: Dp,
    end: Dp,
): Modifier =
    layout { measurable, constraints ->
        val startPx = start.roundToPx()
        val endPx = end.roundToPx()
        val width = constraints.maxWidth + startPx + endPx
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) {
            placeable.place(-startPx, 0)
        }
    }

/**
 * First platform tile: shows the rows of every platform again
 */
@Composable
private fun AllPlatformsTile(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Card(
        onClick = {},
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = NigiroPalette.SurfaceRaised),
        border =
            CardDefaults.border(
                border =
                    Border(
                        BorderStroke(if (active) 2.dp else 1.dp, if (active) NigiroPalette.Pink else Color.White.copy(alpha = 0.10f)),
                        shape = shape,
                    ),
                focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape),
            ),
        scale = cardScale(),
        glow = noCardGlow(),
        modifier = modifier.size(width = NigiroDimens.PlatformTileWidth, height = NigiroDimens.PlatformTileHeight),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.home_platforms_all),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}
