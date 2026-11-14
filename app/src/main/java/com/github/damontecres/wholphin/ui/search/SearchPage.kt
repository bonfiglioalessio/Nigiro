package com.github.damontecres.wholphin.ui.search

import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.DiscoverItem
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.preferences.PrefContentScale
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.AspectRatio
import com.github.damontecres.wholphin.ui.cards.DiscoverItemCard
import com.github.damontecres.wholphin.ui.cards.GridCard
import com.github.damontecres.wholphin.ui.cards.ItemRow
import com.github.damontecres.wholphin.ui.cards.ItemRowTitle
import com.github.damontecres.wholphin.ui.components.TabDetails
import com.github.damontecres.wholphin.ui.components.VoiceSearchButton
import com.github.damontecres.wholphin.ui.components.rememberContextMenu
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.detail.CardGrid
import com.github.damontecres.wholphin.ui.detail.CardGridItem
import com.github.damontecres.wholphin.ui.detail.GridItemDetails
import com.github.damontecres.wholphin.ui.main.HomePageCardContent
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import com.github.damontecres.wholphin.ui.main.OnDiskBadge
import com.github.damontecres.wholphin.ui.main.isOnDisk
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.onMain
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.titleStringRes
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization
import com.github.damontecres.wholphin.util.WholphinDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

internal fun shouldShowPendingPrompt(
    query: String,
    submittedQuery: String?,
): Boolean = query.isNotBlank() && query.trim() != submittedQuery?.trim()

@Composable
internal fun SearchSubmitButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = stringResource(R.string.search))
    }
}

@Composable
internal fun SearchPendingBanner(
    query: String,
    submittedQuery: String?,
    modifier: Modifier = Modifier,
) {
    if (shouldShowPendingPrompt(query, submittedQuery)) {
        Text(
            text = stringResource(R.string.search_pending_prompt, query.trim()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

internal const val SEARCH_ROW = 0
internal const val TAB_ROW = SEARCH_ROW + 1
internal const val SEERR_ROW = TAB_ROW + 1
internal const val COMBINED_ROW = SEERR_ROW
internal const val RESULTS_START = SEERR_ROW + 1

@Composable
fun SearchPage(
    initialQuery: String,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsState()

    // Start with current preferences, but collect updates when view options change
    val prefs =
        viewModel.userPreferencesService.flow
            .collectAsState(userPreferences)
            .value.appPreferences.interfacePreferences.searchPreferences
    val combinedMode = !prefs.separateSearchResults
    val voiceSearchButtonVisible = prefs.showVoiceSearchButton

//    val query = rememberTextFieldState()
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val focusRequesters =
        remember(state.includedSearchableTypes.size) { List(RESULTS_START + state.includedSearchableTypes.size) { FocusRequester() } }

    val seerrActive by viewModel.seerrActive.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    var selectedTab by rememberSaveable(seerrActive, state.discoverEnabled) { mutableIntStateOf(0) }
    var showViewOptions by rememberSaveable { mutableStateOf(false) }
    var showFilterTypeDialog by rememberSaveable { mutableStateOf(false) }
    var searchClicked by rememberSaveable(query) { mutableStateOf(false) }
    var submittedQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }

    val position by viewModel.position.collectAsState()

    fun setPosition(pos: RowColumn) {
        Timber.v("pos=%s", pos)
        viewModel.position.value = pos
    }

    val contextMenu = rememberContextMenu(userPreferences, viewModel)

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            viewModel.voiceInputManager.stopListening()
        }
    }

    fun triggerImmediateSearch(
        searchQuery: String,
        force: Boolean = true,
    ) {
        val trimmed = searchQuery.trim()
        submittedQuery = trimmed.takeIf { it.isNotBlank() }
        isSearchActive = false
        keyboardController?.hide()
        searchClicked = trimmed.isNotBlank()
        viewModel.search(trimmed, combinedMode, force = force)
    }

    // Search while typing, shortly after the last key; the Search key still jumps straight to the results
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            if (submittedQuery != null) {
                submittedQuery = null
                viewModel.search(null, combinedMode, force = true)
            }
            return@LaunchedEffect
        }
        if (trimmed.length < 2 || trimmed == submittedQuery) return@LaunchedEffect
        delay(500)
        submittedQuery = trimmed
        // Not saved as a recent search: it may be a partial word
        viewModel.search(trimmed, combinedMode, force = false, saveAsRecent = false)
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && submittedQuery == null) {
            triggerImmediateSearch(initialQuery, force = false)
        }
    }

    LaunchedEffect(combinedMode) {
        submittedQuery?.takeIf { it.isNotBlank() }?.let { activeQuery ->
            viewModel.search(activeQuery, combinedMode, force = false)
        }
    }
    LaunchedEffect(Unit) {
        focusRequesters.getOrNull(position.row)?.tryRequestFocus()
    }
    val onClickItem = { _: Int, item: BaseItem ->
        Timber.v("Clicked %s, type=%s", item.id, item.type)
        // Opening a result is when a typed search becomes a recent one
        submittedQuery?.let(viewModel::rememberSearch)
        viewModel.navigationManager.navigateTo(item.destination())
    }
    val onLongClickItem = { rowIndex: Int, index: Int, item: BaseItem ->
        setPosition(RowColumn(rowIndex, index))
        contextMenu.showContextMenu(index, item)
    }
    val onPlayItem = { _: Int, item: BaseItem ->
        viewModel.navigationManager.navigateTo(Destination.Playback(item))
    }

    val onClickDiscover = { _: Int, item: DiscoverItem ->
        val dest =
            if (item.jellyfinItemId != null && item.type.baseItemKind != null) {
                Destination.MediaItem(
                    itemId = item.jellyfinItemId,
                    type = item.type.baseItemKind,
                )
            } else {
                Destination.DiscoveredItem(item)
            }
        viewModel.navigationManager.navigateTo(dest)
    }

    var showHeader by rememberSaveable { mutableStateOf(true) }
    val positionCallback = { columns: Int, index: Int ->
        showHeader = index < columns
    }
    // Based on the submitted query, so the tabs don't flicker while typing
    // Seerr isn't searched here (Gelato already makes the library complete), so there is only one tab
    val showTabs = false
    val animationsEnabled = LocalInterfaceCustomization.current.enableAnimations
    val isLibraryTab = selectedTab == 0

    LaunchedEffect(seerrActive, query) {
        if (!seerrActive || query.isBlank()) {
            selectedTab = 0
        }
    }

    LaunchedEffect(
        searchClicked,
        state,
        combinedMode,
        selectedTab,
        seerrActive,
    ) {
        if (!searchClicked || position.row > TAB_ROW) return@LaunchedEffect

        withContext(WholphinDispatchers.IO) {
            // Want to focus on the first successful row after all the ones before it are finished searching
            val results =
                if (isLibraryTab) {
                    if (combinedMode) {
                        listOf(state.combinedResults)
                    } else {
                        state.includedSearchableTypes.map { state.results[it] }
                    }
                } else {
                    listOf(state.seerrResults)
                }
            val firstSuccess =
                results.indexOfFirst { it is SearchResult.Success || it is SearchResult.SuccessSeerr }
            if (firstSuccess >= 0) {
                val anyBeforeSearching =
                    results.subList(0, firstSuccess).any { it is SearchResult.Searching }
                if (!anyBeforeSearching) {
                    val targetRow =
                        if (isLibraryTab) {
                            if (combinedMode) {
                                COMBINED_ROW
                            } else {
                                firstSuccess
                            }
                        } else {
                            SEERR_ROW
                        }
//                    setPosition(RowColumn(targetRow, 0))
                    onMain { focusRequesters.getOrNull(targetRow)?.tryRequestFocus() }
                }
            }
        }
    }

    val tabs =
        remember(seerrActive, state.discoverEnabled) {
            buildList {
                add(TabDetails(R.string.library))
                if (seerrActive && state.discoverEnabled) {
                    add(TabDetails(R.string.discover))
                }
            }
        }

    // Keyboard on the left, results on the right: typing never needs the system keyboard
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        modifier =
            modifier
                .fillMaxSize()
                .padding(start = NigiroDimens.ContentStart, top = 24.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.focusGroup(),
        ) {
            SearchQueryDisplay(query = query)
            SearchKeyboard(
                onChar = { query += it },
                onSpace = { if (query.isNotEmpty() && !query.endsWith(" ")) query += " " },
                onBackspace = { query = query.dropLast(1) },
                onClear = { query = "" },
                onSubmit = { triggerImmediateSearch(query) },
                firstKeyFocusRequester = focusRequesters[SEARCH_ROW],
                voiceKey =
                    if (voiceSearchButtonVisible) {
                        {
                            VoiceSearchButton(
                                onSpeechResult = { spokenText ->
                                    query = spokenText
                                    triggerImmediateSearch(spokenText)
                                },
                                voiceInputManager = viewModel.voiceInputManager,
                            )
                        }
                    } else {
                        null
                    },
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        ) {
//            SideEffect {
//                Timber.v("isLibraryTab=%s, combinedMode=%s", isLibraryTab, combinedMode)
//            }
            when {
                combinedMode && state.combinedResults is SearchResult.NoQuery -> {
                    SearchIdleContent(
                        recentSearches = recentSearches,
                        favorites = favorites,
                        onClickRecent = { recent ->
                            query = recent
                            triggerImmediateSearch(recent)
                        },
                        onClickItem = { index, item -> onClickItem(index, item) },
                        onLongClickItem = { index, item -> onLongClickItem(COMBINED_ROW, index, item) },
                        firstFocusRequester = focusRequesters[COMBINED_ROW],
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                isLibraryTab && combinedMode -> {
                    SearchCombinedResults(
                        result = state.combinedResults,
                        focusRequester = focusRequesters[COMBINED_ROW],
                        onClickItem = onClickItem,
                        onLongClickItem = { index, item ->
                            onLongClickItem(COMBINED_ROW, index, item)
                        },
                        onPlayItem = onPlayItem,
                        onClickPosition = { setPosition(it) },
                        onClickDiscover = onClickDiscover,
                        positionCallback = positionCallback,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                !isLibraryTab && combinedMode -> {
                    SearchCombinedResults(
                        result = state.seerrResults,
                        focusRequester = focusRequesters[SEERR_ROW],
                        onClickItem = onClickItem,
                        onLongClickItem = { _, _ -> },
                        onPlayItem = onPlayItem,
                        onClickPosition = { setPosition(it) },
                        onClickDiscover = onClickDiscover,
                        positionCallback = positionCallback,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                isLibraryTab -> {
                    LazyColumn(
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 44.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.focusGroup(),
                    ) {
                        itemsIndexed(state.includedSearchableTypes) { index, type ->
                            val rowIndex = RESULTS_START + index
                            val result = state.results.getOrDefault(type, SearchResult.Searching)
                            val focusRequester =
                                remember(state.includedSearchableTypes.size) {
                                    focusRequesters.getOrNull(rowIndex) ?: FocusRequester()
                                }
                            SearchRowResult(
                                title = type.titleStringRes,
                                result = result,
                                rowIndex = rowIndex,
                                position = position,
                                focusRequester = focusRequester,
                                onClickItem = onClickItem,
                                onLongClickItem = { index, item ->
                                    onLongClickItem(rowIndex, index, item)
                                },
                                onClickPosition = { setPosition(it) },
                                modifier = Modifier.fillMaxWidth(),
                                cardContent = { index, item, mod, onClick, onLongClick ->
                                    SearchPageCard(
                                        item = item,
                                        type = type,
                                        onClick = {
                                            setPosition(RowColumn(rowIndex, index))
                                            onClick.invoke()
                                        },
                                        onLongClick = onLongClick,
                                        modifier = mod,
                                    )
                                },
                            )
                        }

                        if (seerrActive && state.discoverEnabled) {
                            item {
                                SearchRowResult(
                                    title = R.string.discover,
                                    result = state.seerrResults,
                                    rowIndex = SEERR_ROW,
                                    position = position,
                                    focusRequester = focusRequesters[SEERR_ROW],
                                    onClickItem = onClickItem,
                                    onLongClickItem = { _, _ -> },
                                    onClickDiscover = onClickDiscover,
                                    onClickPosition = { setPosition(it) },
                                    cardContent = { _, _, _, _, _ -> },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
        contextMenu.Compose()
    }

    if (showViewOptions) {
        SearchViewOptionsDialog(
            combinedResults = combinedMode,
            onCombinedResultsChange = viewModel::setCombinedResults,
            voiceSearchButtonVisible = voiceSearchButtonVisible,
            onVoiceSearchButtonVisibleChange = viewModel::setVoiceSearchButtonVisible,
            onClickFilterTypes = { showFilterTypeDialog = true },
            onDismissRequest = { showViewOptions = false },
        )
    }

    if (showFilterTypeDialog) {
        SearchTypeOptionsDialog(
            onDismissRequest = { showFilterTypeDialog = false },
            searchableTypes = state.possibleSearchableTypes,
            excludedSearchableTypes = state.excludedSearchableTypes,
            discoverAvailable = seerrActive,
            discoverEnabled = state.discoverEnabled,
            onClick = viewModel::onClickExcludeSearchableType,
            onClickDiscover = viewModel::onClickExcludeDiscover,
        )
    }
}

@Composable
fun SearchCombinedResults(
    result: SearchResult,
    focusRequester: FocusRequester,
    onClickItem: (Int, BaseItem) -> Unit,
    onLongClickItem: (Int, BaseItem) -> Unit,
    onPlayItem: (Int, BaseItem) -> Unit,
    onClickPosition: (RowColumn) -> Unit,
    onClickDiscover: (Int, DiscoverItem) -> Unit,
    positionCallback: (columns: Int, position: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (result) {
        SearchResult.NoQuery -> {}

        SearchResult.Searching -> {
            SearchResultPlaceholder(
                title = stringResource(R.string.results),
                message = stringResource(R.string.searching),
                modifier = modifier.padding(16.dp),
            )
        }

        is SearchResult.Error -> {
            SearchResultPlaceholder(
                title = stringResource(R.string.results),
                message = result.ex.localizedMessage ?: "Error occurred during search",
                messageColor = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(16.dp),
            )
        }

        is SearchResult.Success -> {
            if (result.items.isEmpty()) {
                SearchResultPlaceholder(
                    title = stringResource(R.string.results),
                    message = stringResource(R.string.no_results),
                    modifier = modifier.padding(16.dp),
                )
            } else {
                SearchGrid(
                    items = result.items,
                    focusRequester = focusRequester,
                    onClickItem = onClickItem,
                    onLongClickItem = onLongClickItem,
                    onPlayItem = onPlayItem,
                    onClickPosition = onClickPosition,
                    positionCallback = positionCallback,
                    cardContent = { details ->
                        GridCard(
                            item = details.item,
                            onClick = details.onClick,
                            onLongClick = details.onLongClick,
                            modifier = details.mod,
                            fillWidth = details.widthPx,
                            showTitle = false,
                            imageContentScale = ContentScale.Crop,
                        )
                    },
                    focusedItemHeader = true,
                    modifier = modifier,
                )
            }
        }

        is SearchResult.SuccessSeerr -> {
            if (result.items.isEmpty()) {
                SearchResultPlaceholder(
                    title = stringResource(R.string.results),
                    message = stringResource(R.string.no_results),
                    modifier = modifier.padding(16.dp),
                )
            } else {
                SearchGrid(
                    items = result.items,
                    focusRequester = focusRequester,
                    onClickItem = onClickDiscover,
                    onLongClickItem = { _, _ -> },
                    onPlayItem = { _, _ -> },
                    onClickPosition = onClickPosition,
                    positionCallback = positionCallback,
                    cardContent = { details ->
                        DiscoverItemCard(
                            item = details.item,
                            onClick = details.onClick,
                            onLongClick = details.onLongClick,
                            modifier = details.mod,
                        )
                    },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun <T : CardGridItem> SearchGrid(
    items: List<T?>,
    focusRequester: FocusRequester,
    onClickItem: (Int, T) -> Unit,
    onLongClickItem: (Int, T) -> Unit,
    onPlayItem: (Int, T) -> Unit,
    onClickPosition: (RowColumn) -> Unit,
    cardContent: @Composable (GridItemDetails<T>) -> Unit,
    positionCallback: (columns: Int, position: Int) -> Unit,
    modifier: Modifier = Modifier,
    focusedItemHeader: Boolean = false,
) {
    // Read only by the header, through a provider, so moving focus doesn't recompose the grid
    var focusedIndex by remember(items) { mutableIntStateOf(0) }
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier,
    ) {
        if (focusedItemHeader) {
            SearchFocusedHeader(itemProvider = { items.getOrNull(focusedIndex) as? BaseItem })
        } else {
            ItemRowTitle(stringResource(R.string.results))
        }

        CardGrid(
            pager = items,
            onClickItem = { index, item ->
                onClickPosition.invoke(RowColumn(COMBINED_ROW, index))
                onClickItem.invoke(index, item)
            },
            onLongClickItem = onLongClickItem,
            onClickPlay = { index, item ->
                onClickPosition.invoke(RowColumn(COMBINED_ROW, index))
                onPlayItem.invoke(index, item)
            },
            letterPosition = { -1 },
            // Narrower area next to the keyboard
            columns = 4,
            // The "1 / 24" counter would cover the posters of the last visible row
            showPositionFooter = false,
            gridFocusRequester = focusRequester,
            showJumpButtons = false,
            showLetterButtons = false,
            positionCallback = { columns, index ->
                focusedIndex = index
                positionCallback(columns, index)
            },
            // Aligned with the search bar and the header above
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 0.dp, end = NigiroDimens.ContentEnd),
            cardContent = cardContent,
        )
    }
}

@Composable
fun SearchRowResult(
    @StringRes title: Int,
    result: SearchResult,
    rowIndex: Int,
    position: RowColumn,
    focusRequester: FocusRequester,
    onClickItem: (Int, BaseItem) -> Unit,
    onLongClickItem: (Int, BaseItem) -> Unit,
    onClickPosition: (RowColumn) -> Unit,
    modifier: Modifier = Modifier,
    onClickDiscover: ((Int, DiscoverItem) -> Unit)? = null,
    cardContent: @Composable (
        index: Int,
        item: BaseItem?,
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) -> Unit,
) {
    when (val r = result) {
        is SearchResult.Error -> {
            SearchResultPlaceholder(
                title = stringResource(title),
                message = r.ex.localizedMessage ?: "Error occurred during search",
                messageColor = MaterialTheme.colorScheme.error,
                modifier = Modifier,
            )
        }

        SearchResult.NoQuery -> {
            // no-op
        }

        SearchResult.Searching -> {
            SearchResultPlaceholder(
                title = stringResource(title),
                message = stringResource(R.string.searching),
                modifier = modifier,
            )
        }

        is SearchResult.Success -> {
            if (r.items.isNotEmpty()) {
                ItemRow(
                    title = stringResource(title),
                    items = r.items,
                    onClickItem = onClickItem,
                    onLongClickItem = onLongClickItem,
                    modifier = modifier.focusRequester(focusRequester),
                    cardContent = cardContent,
                )
            }
        }

        is SearchResult.SuccessSeerr -> {
            if (r.items.isNotEmpty()) {
                ItemRow(
                    title = stringResource(title),
                    items = r.items,
                    onClickItem = { index, item ->
                        onClickPosition.invoke(RowColumn(rowIndex, index))
                        onClickDiscover?.invoke(index, item)
                    },
                    onLongClickItem = { _, _ -> },
                    modifier = modifier.focusRequester(focusRequester),
                    cardContent = { index: Int, item: DiscoverItem?, mod: Modifier, onClick: () -> Unit, onLongClick: () -> Unit ->
                        DiscoverItemCard(
                            item = item,
                            onClick = onClick,
                            onLongClick = onLongClick,
                            showOverlay = true,
                            modifier = mod,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun SearchResultPlaceholder(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    messageColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(bottom = 32.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = messageColor,
        )
    }
}

/**
 * One line with title, details and the on-disk badge of the focused result
 */
@Composable
private fun SearchFocusedHeader(itemProvider: () -> BaseItem?) {
    val item = itemProvider()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SearchHeaderHeight)
                .padding(end = NigiroDimens.ContentEnd, bottom = 10.dp),
    ) {
        if (item == null) return@Column
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            // Keep long titles clear of the clock
            modifier = Modifier.padding(end = 96.dp),
        ) {
            Text(
                text = item.name.orEmpty(),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (item.isOnDisk()) {
                OnDiskBadge()
            }
        }
        val meta = remember(item.id) { searchHeaderMeta(item) }
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = NigiroDimens.Meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item.data.overview?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = NigiroDimens.Body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val SearchHeaderHeight = 108.dp

/**
 * Year · rating · up to three genres · runtime
 */
private fun searchHeaderMeta(item: BaseItem): String {
    val data = item.data
    val minutes = (data.runTimeTicks ?: 0L) / 600_000_000L
    val runtime =
        when {
            minutes <= 0 -> null
            minutes >= 60 -> "${minutes / 60} h ${minutes % 60} min"
            else -> "$minutes min"
        }
    return listOfNotNull(
        data.productionYear?.toString(),
        data.communityRating?.let { String.format(Locale.ITALIAN, "★ %.1f", it) },
        data.genres?.take(3)?.takeIf { it.isNotEmpty() }?.joinToString(", "),
        runtime,
    ).joinToString(" · ")
}

/**
 * Before searching: recent searches and the favorites (usually the downloaded titles) instead of a blank page
 */
@Composable
private fun SearchIdleContent(
    recentSearches: List<String>,
    favorites: List<BaseItem>,
    onClickRecent: (String) -> Unit,
    onClickItem: (Int, BaseItem) -> Unit,
    onLongClickItem: (Int, BaseItem) -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val posterOptions =
        remember {
            HomeRowViewOptions(
                heightDp = NigiroDimens.PosterHeight.value.toInt(),
                spacing = NigiroDimens.CardGap.value.toInt(),
                contentScale = PrefContentScale.CROP,
                aspectRatio = AspectRatio.TALL,
                showTitles = false,
                useSeries = false,
            )
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(NigiroDimens.SectionGap),
        modifier = modifier.padding(top = 12.dp),
    ) {
        if (recentSearches.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(NigiroDimens.TitleGap)) {
                ItemRowTitle(
                    stringResource(R.string.recent_searches),
                    startPadding = 0.dp,
                    style = NigiroDimens.RowTitle,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 0.dp, end = NigiroDimens.ContentEnd),
                    modifier = Modifier.focusGroup(),
                ) {
                    itemsIndexed(recentSearches, key = { _, it -> it }) { index, recent ->
                        Button(
                            onClick = { onClickRecent(recent) },
                            shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                            colors =
                                ButtonDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.10f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = NigiroPalette.Background,
                                ),
                            scale = ButtonDefaults.scale(focusedScale = 1.05f),
                            modifier = if (index == 0) Modifier.focusRequester(firstFocusRequester) else Modifier,
                        ) {
                            Text(text = recent, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            ItemRow(
                title = stringResource(R.string.favorites_and_on_disk),
                items = favorites,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
                cardContent = { index, item, cardModifier, onClick, onLongClick ->
                    // Every favorite is marked: the heart would only repeat the row title
                    HomePageCardContent(
                        index = index,
                        item = item,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        viewOptions = posterOptions,
                        modifier = cardModifier,
                        showFavorite = false,
                    )
                },
                horizontalPadding = NigiroDimens.CardGap,
                itemKey = { it.id.toString() },
                contentStartPadding = 0.dp,
                titleStartPadding = 0.dp,
                titleStyle = NigiroDimens.RowTitle,
                titleGap = NigiroDimens.TitleGap - 8.dp,
                modifier =
                    if (recentSearches.isEmpty()) Modifier.focusRequester(firstFocusRequester) else Modifier,
            )
        }
    }
}
