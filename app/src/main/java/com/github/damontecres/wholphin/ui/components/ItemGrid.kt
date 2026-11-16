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
            _state.update { it.copy(exploreFilter = destination.explore?.filter) }
            load(destination.request as Any, destination.requestHandler as RequestHandler<Any>)
        }

        /** Explore grids: show all titles, only movies or only series */
        fun setExploreFilter(filter: HomeMediaFilter) {
            val source = destination.explore ?: return
            if (_state.value.exploreFilter == filter) return
            _state.update { it.copy(exploreFilter = filter) }
            val (request, handler) = source.request(serverRepository.currentUser?.id, filter)
            @Suppress("UNCHECKED_CAST")
            load(request, handler as RequestHandler<Any>)
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
                (state.value.items as? DataLoadingState.Success)?.let {
                    (it.data as? ApiRequestPager<*>)?.refreshItem(position, itemId)
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
                (state.value.items as? DataLoadingState.Success)?.let {
                    (it.data as? ApiRequestPager<*>)?.refreshItem(position, itemId)
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
                        HomeMediaFilterBar(
                            selectedFilter = filter,
                            onSelectFilter = viewModel::setExploreFilter,
                            firstButtonFocusRequester = filterFocusRequester,
                        )
                    }
                }

                // A new grid for each filter: it starts from the top instead of the old position
                key(state.exploreFilter) {
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
