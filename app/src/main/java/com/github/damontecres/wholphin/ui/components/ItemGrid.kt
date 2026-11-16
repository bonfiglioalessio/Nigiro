package com.github.damontecres.wholphin.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.damontecres.wholphin.ui.cards.noButtonGlow
import com.github.damontecres.wholphin.ui.FontAwesome
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Button
import androidx.tv.material3.Border
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random
import com.github.damontecres.wholphin.util.RequestPager
import com.github.damontecres.wholphin.ui.explore.itemSortOrder
import com.github.damontecres.wholphin.ui.explore.itemSortBy
import com.github.damontecres.wholphin.ui.explore.ExploreSort
import com.github.damontecres.wholphin.R
import androidx.tv.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.api.client.extensions.itemsApi
import com.github.damontecres.wholphin.ui.explore.itemKinds
import com.github.damontecres.wholphin.ui.SlimItemFields
import com.github.damontecres.wholphin.services.LocalTwinService
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Job
import com.github.damontecres.wholphin.ui.explore.request
import com.github.damontecres.wholphin.ui.main.HomeMediaFilterBar
import com.github.damontecres.wholphin.ui.main.HomeMediaFilter
import com.github.damontecres.wholphin.ui.ifElse
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.Alignment
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import com.github.damontecres.wholphin.ui.main.isOnDisk
import com.github.damontecres.wholphin.ui.cards.ExpandingPosterCard
import com.github.damontecres.wholphin.ui.AspectRatios
import androidx.tv.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.FavoriteWatchManager
import com.github.damontecres.wholphin.services.MediaManagementService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.ServerReportService
import com.github.damontecres.wholphin.services.deleteItem
import com.github.damontecres.wholphin.ui.cards.GridCard
import com.github.damontecres.wholphin.ui.data.AddPlaylistViewModel
import com.github.damontecres.wholphin.ui.detail.CardGrid
import com.github.damontecres.wholphin.ui.equalsNotNull
import com.github.damontecres.wholphin.ui.launchDefault
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.showToast
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.ApiRequestPager
import com.github.damontecres.wholphin.util.DataLoadingState
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.RequestHandler
import com.github.damontecres.wholphin.util.WholphinDispatchers
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import timber.log.Timber
import java.util.UUID

@HiltViewModel(assistedFactory = ItemGridViewModel.Factory::class)
class ItemGridViewModel
    @AssistedInject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val api: ApiClient,
        private val navigationManager: NavigationManager,
        private val favoriteWatchManager: FavoriteWatchManager,
        private val mediaManagementService: MediaManagementService,
        val serverRepository: ServerRepository,
        val serverReportService: ServerReportService,
        private val localTwinService: LocalTwinService,
        @Assisted private val destination: Destination.ItemGrid<*>,
    ) : ViewModel(),
        ContextMenuProvider {
        @AssistedFactory
        interface Factory {
            fun create(destination: Destination.ItemGrid<*>): ItemGridViewModel
        }

        private val _state = MutableStateFlow(ItemGridState())
        val state: StateFlow<ItemGridState> = _state

        private var loadJob: Job? = null

        init {
            _state.update {
                it.copy(exploreFilter = destination.explore?.filter, exploreSort = destination.explore?.defaultSort())
            }
            if (destination.explore != null) {
                // Built here, with the current user: Favorites creates its destination without one
                reload()
            } else {
                load(destination.request as Any, destination.requestHandler as RequestHandler<Any>)
            }
        }

        /** Explore grids: show all titles, only movies or only series */
        fun setExploreFilter(filter: HomeMediaFilter) {
            val source = destination.explore ?: return
            if (_state.value.exploreFilter == filter) return
            _state.update { it.copy(exploreFilter = filter) }
            reload()
        }

        fun setExploreSort(sort: ExploreSort) {
            if (destination.explore == null || _state.value.exploreSort == sort) return
            _state.update { it.copy(exploreSort = sort) }
            reload()
        }

        /** Opens a random title of the grid (with the current filter) */
        fun openRandom() {
            viewModelScope.launchIO {
                val items = (state.value.items as? DataLoadingState.Success)?.data ?: return@launchIO
                if (items.isEmpty()) return@launchIO
                val index = Random.nextInt(items.size)
                val item = ((items as? RequestPager<*>)?.getBlocking(index) as? BaseItem) ?: items[index] ?: return@launchIO
                withContext(Dispatchers.Main) { navigateTo(item.destination(index)) }
            }
        }

        /** Loads the titles again, eg Favorites after one was removed elsewhere */
        fun reload() {
            val source = destination.explore ?: return
            val filter = _state.value.exploreFilter ?: source.filter
            val sort = _state.value.exploreSort ?: source.defaultSort()
            if (source.favorites) {
                loadFavorites(filter, sort)
                return
            }
            val (request, handler) = source.request(serverRepository.currentUser?.id, filter, sort)
            @Suppress("UNCHECKED_CAST")
            load(request, handler as RequestHandler<Any>)
        }

        /**
         * Favourites, all at once (they are few): a streamed (Gelato) favourite that was downloaded shows its copy
         * on disk instead, like the favourites row in Search
         */
        private fun loadFavorites(
            filter: HomeMediaFilter,
            sort: ExploreSort,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launchIO {
                    try {
                        val request =
                            GetItemsRequest(
                                userId = serverRepository.currentUser?.id,
                                recursive = true,
                                isFavorite = true,
                                includeItemTypes = filter.itemKinds(),
                                sortBy = sort.itemSortBy(),
                                sortOrder = sort.itemSortOrder(),
                                // Provider ids and sources let a streamed favourite be matched to its downloaded copy
                                fields = SlimItemFields + listOf(ItemFields.PROVIDER_IDS, ItemFields.MEDIA_SOURCES),
                            )
                        val items =
                            api.itemsApi
                                .getItems(request)
                                .content.items
                                .map { dto ->
                                    if (localTwinService.isStreamOnly(dto)) localTwinService.findLocalTwin(dto) ?: dto else dto
                                }.distinctBy { it.id }
                                .map { BaseItem.from(it, api, useSeriesForPrimary = true) }
                        _state.update { it.copy(items = DataLoadingState.Success(items)) }
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error fetching favorites")
                        _state.update { it.copy(items = DataLoadingState.Error(ex)) }
                    }
                }
        }

        // The current titles stay on screen until the new ones are loaded, so focus on the filter is not lost
        private fun load(
            request: Any,
            handler: RequestHandler<Any>,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launchIO {
                    try {
                        val pager =
                            ApiRequestPager(
                                api,
                                request,
                                handler,
                                viewModelScope,
                                useSeriesForPrimary = true,
                            ).init()
                        if (pager.isNotEmpty()) {
                            pager.getBlocking(0)
                        }
                        _state.update {
                            it.copy(items = DataLoadingState.Success(pager))
                        }
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error fetching items")
                        _state.update { it.copy(items = DataLoadingState.Error(ex)) }
                    }
                }
        }

        override fun navigateTo(destination: Destination) {
            navigationManager.navigateTo(destination)
        }

        override fun canDelete(
            item: BaseItem,
            appPreferences: AppPreferences,
        ): Boolean = mediaManagementService.canDelete(item, appPreferences)

        override fun deleteItem(
            index: Int,
            item: BaseItem,
        ) {
            deleteItem(context, mediaManagementService, item) {
                viewModelScope.launchDefault {
                    refreshAfterDelete(index, item)
                }
            }
        }

        private suspend fun refreshAfterDelete(
            position: Int,
            deletedItem: BaseItem,
        ) {
            try {
                val pager =
                    ((state.value.items as? DataLoadingState.Success)?.data as? ApiRequestPager<*>)
                position.let {
                    Timber.v("Item deleted: position=%s", it)
                    val item = pager?.get(it)
                    // Exact item deleted (eg a movie) or deleted item was within the series
                    if (item?.id == deletedItem.id ||
                        equalsNotNull(item?.data?.id, deletedItem.data.seriesId)
                    ) {
                        pager?.refreshPagesAfter(position)
                    }
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Error refreshing after deleted item %s", deletedItem.id)
                showToast(context, "Error refreshing after item deleted")
            }
        }

        override fun setWatched(
            position: Int,
            itemId: UUID,
            played: Boolean,
        ) {
            viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
                favoriteWatchManager.setWatched(itemId, played)
                if (destination.explore?.favorites == true) {
                    // Favourites are a plain list, not a pager
                    reload()
                } else {
                    (state.value.items as? DataLoadingState.Success)?.let {
                        (it.data as? ApiRequestPager<*>)?.refreshItem(position, itemId)
                    }
                }
            }
        }

        override fun setFavorite(
            position: Int,
            itemId: UUID,
            favorite: Boolean,
        ) {
            viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
                favoriteWatchManager.setFavorite(itemId, favorite)
                if (destination.explore?.favorites == true) {
                    // Favorites grid: a removed title leaves the grid
                    reload()
                } else {
                    (state.value.items as? DataLoadingState.Success)?.let {
                        (it.data as? ApiRequestPager<*>)?.refreshItem(position, itemId)
                    }
                }
            }
        }

        override fun isAdministrator(): Boolean = serverRepository.currentUserDto?.policy?.isAdministrator == true

        override fun sendReportFor(itemId: UUID) = serverReportService.sendMediaReportFor(itemId)
    }

data class ItemGridState(
    val items: DataLoadingState<List<BaseItem?>> = DataLoadingState.Pending,
    // Null when the grid was not opened from Explore and has no filter
    val exploreFilter: HomeMediaFilter? = null,
    val exploreSort: ExploreSort? = null,
)

/**
 * Display a grid of a list of arbitrary items [com.github.damontecres.wholphin.data.ExtrasItem]
 */
@Composable
fun ItemGrid(
    preferences: UserPreferences,
    destination: Destination.ItemGrid<*>,
    modifier: Modifier = Modifier,
    playlistViewModel: AddPlaylistViewModel = hiltViewModel(),
    viewModel: ItemGridViewModel =
        hiltViewModel<ItemGridViewModel, ItemGridViewModel.Factory>(
            creationCallback = { it.create(destination) },
        ),
) {
    val contextMenu = rememberContextMenu(preferences, viewModel)
    if (destination.explore?.favorites == true) {
        // Back from a title whose favourite was toggled on its page: reload, skipping the first resume
        var resumedOnce by remember { mutableStateOf(false) }
        LifecycleResumeEffect(Unit) {
            if (resumedOnce) viewModel.reload() else resumedOnce = true
            onPauseOrDispose { }
        }
    }

    val state by viewModel.state.collectAsState()
    when (val st = state.items) {
        is DataLoadingState.Error -> {
            ErrorMessage(st, modifier)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        is DataLoadingState.Success<List<BaseItem?>> -> {
            var position by rememberInt(destination.initialPosition)
            val focusRequester = remember { FocusRequester() }
            val filterFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.tryRequestFocus()
            }

            val density = LocalDensity.current
            Column(
                verticalArrangement = Arrangement.spacedBy(NigiroDimens.TitleGap),
                modifier = modifier.padding(start = NigiroDimens.ContentStart, end = NigiroDimens.ContentEnd, top = 20.dp),
            ) {
                // AIOStreams page title: left, like the Explore header, with the filter on the right
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = destination.title.getString(),
                        style = NigiroDimens.RowTitle.copy(fontSize = 26.sp, lineHeight = 30.sp),
                        color = NigiroPalette.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    state.exploreFilter?.let { filter ->
                        Spacer(Modifier.weight(1f))
                        // Filter, sort, random: all 36 dp high, evenly spaced
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HomeMediaFilterBar(
                                selectedFilter = filter,
                                onSelectFilter = viewModel::setExploreFilter,
                                firstButtonFocusRequester = filterFocusRequester,
                            )
                            destination.explore?.let { source ->
                                ExploreSortButton(
                                    options = source.sortOptions(),
                                    selected = state.exploreSort ?: source.defaultSort(),
                                    onSelect = viewModel::setExploreSort,
                                )
                            }
                            HeaderIconButton(
                                glyph = stringResource(R.string.fa_dice),
                                contentDescription = stringResource(R.string.explore_random),
                                onClick = viewModel::openRandom,
                            )
                        }
                    }
                }

                // A new grid for each filter or sort: it starts from the top instead of the old position
                key(state.exploreFilter, state.exploreSort) {
                CardGrid(
                    pager = st.data,
                    onClickItem = { index: Int, item: BaseItem ->
                        position = index
                        viewModel.navigateTo(item.destination(index))
                    },
                    onLongClickItem = { index: Int, item: BaseItem ->
                        position = index
                        contextMenu.showContextMenu(index, item)
                    },
                    onClickPlay = { _, item -> viewModel.navigateTo(Destination.Playback(item)) },
                    letterPosition = { c: Char -> 0 },
                    gridFocusRequester = focusRequester,
                    showJumpButtons = false,
                    showLetterButtons = false,
                    initialPosition = destination.initialPosition,
                    spacing = NigiroDimens.CardGap,
                    // The "1 / 183" counter covered the titles of the last row
                    showPositionFooter = false,
                    cardContent = @Composable { (item, index, onClick, onLongClick, widthPx, mod) ->
                        // The same poster card as the Home rows, sized to the grid cell
                        val cardHeight =
                            if (widthPx > 0) {
                                with(density) { widthPx.toDp() } / AspectRatios.TALL
                            } else {
                                NigiroDimens.PosterHeight
                            }
                        ExpandingPosterCard(
                            item = item,
                            onClick = onClick,
                            onLongClick = onLongClick,
                            cardHeight = cardHeight,
                            cornerText = item?.ui?.episodeUnplayedCornerText,
                            onDisk = item?.isOnDisk() == true,
                            expandable = false,
                            showFavorite = destination.explore?.favorites != true,
                            modifier = mod,
                        )
                    },
                    columns = destination.viewOptions.columns,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            // The filter sits at the right edge: Up from the first row reaches it
                            .ifElse(state.exploreFilter != null, Modifier.focusProperties { up = filterFocusRequester }),
                )
                }
            }
        }
    }
    contextMenu.Compose()
}

/** AIOStreams sort select: a pill with the current order, opening the item menu with the choices */
@Composable
private fun ExploreSortButton(
    options: List<ExploreSort>,
    selected: ExploreSort,
    onSelect: (ExploreSort) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Button(
        onClick = { showMenu = true },
        shape = ButtonDefaults.shape(HeaderPill),
        colors = headerButtonColors(),
        border = headerButtonBorder(HeaderPill),
        scale = ButtonDefaults.scale(focusedScale = 1.04f),
        glow = noButtonGlow(),
        contentPadding = PaddingValues(start = 14.dp, end = 8.dp),
        modifier = Modifier.height(HeaderControlHeight),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
            Text(
                text = stringResource(R.string.explore_sort_prefix),
                fontSize = 14.sp,
                color = LocalContentColor.current.copy(alpha = 0.6f),
            )
            Text(text = stringResource(selected.label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
    if (showMenu) {
        // Read while the pill still has focus, so the menu opens next to it
        val anchor = rememberFocusAnchor()
        val entries =
            options.map { option ->
                DialogItem(
                    headlineContent = { Text(stringResource(option.label)) },
                    trailingContent =
                        if (option == selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                    onClick = {
                        showMenu = false
                        onSelect(option)
                    },
                )
            }
        AioItemMenu(
            title = stringResource(R.string.explore_sort_title),
            entries = entries,
            anchor = anchor,
            fromLongClick = false,
            onDismissRequest = { showMenu = false },
            dismissOnClick = true,
        )
    }
}

private val HeaderControlHeight = 36.dp
private val HeaderPill = RoundedCornerShape(50)

// Outlined and transparent; focused white with black text, like the hero buttons
@Composable
private fun headerButtonColors() =
    ButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = NigiroPalette.TextSecondary,
        focusedContainerColor = Color.White,
        focusedContentColor = Color.Black,
        pressedContainerColor = Color.White,
        pressedContentColor = Color.Black,
    )

@Composable
private fun headerButtonBorder(shape: androidx.compose.ui.graphics.Shape) =
    ButtonDefaults.border(
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), shape = shape),
        focusedBorder = Border(BorderStroke(1.dp, Color.Transparent), shape = shape),
    )

@Composable
internal fun HeaderIconButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(CircleShape),
        colors = headerButtonColors(),
        border = headerButtonBorder(CircleShape),
        scale = ButtonDefaults.scale(focusedScale = 1.04f),
        glow = noButtonGlow(),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(HeaderControlHeight).semantics { this.contentDescription = contentDescription },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = glyph, fontFamily = FontAwesome, fontSize = 15.sp, lineHeight = 15.sp)
        }
    }
}
