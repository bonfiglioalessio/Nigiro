package com.github.damontecres.wholphin.ui.detail.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.ui.cards.ExpandingPosterCard
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import com.github.damontecres.wholphin.ui.theme.px
import org.jellyfin.sdk.model.api.PersonKind
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ExtrasItem
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.DiscoverItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.data.model.Trailer
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.TrailerService
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.RequestOrRestoreFocus
import com.github.damontecres.wholphin.ui.cards.ChapterRow
import com.github.damontecres.wholphin.ui.cards.ExtrasRow
import com.github.damontecres.wholphin.ui.cards.ItemRow
import com.github.damontecres.wholphin.ui.cards.PersonRow
import com.github.damontecres.wholphin.ui.cards.SeasonCard
import com.github.damontecres.wholphin.ui.components.ContextMenu
import com.github.damontecres.wholphin.ui.components.ContextMenuActions
import com.github.damontecres.wholphin.ui.components.ContextMenuDialog
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.ExpandablePlayButtons
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.components.Optional
import com.github.damontecres.wholphin.ui.components.PersonContextActions
import com.github.damontecres.wholphin.ui.data.AddPlaylistViewModel
import com.github.damontecres.wholphin.ui.data.ChooseVersionParams
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialog
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialogInfo
import com.github.damontecres.wholphin.ui.detail.DetailContentInset
import com.github.damontecres.wholphin.ui.detail.DetailPageFrame
import com.github.damontecres.wholphin.ui.detail.PlaylistDialog
import com.github.damontecres.wholphin.ui.detail.headerBackdropAlpha
import com.github.damontecres.wholphin.ui.discover.DiscoverRow
import com.github.damontecres.wholphin.ui.discover.DiscoverRowData
import com.github.damontecres.wholphin.ui.letNotEmpty
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.util.ResStringProvider
import com.github.damontecres.wholphin.util.DataLoadingState
import com.github.damontecres.wholphin.util.DiscoverRequestType
import com.github.damontecres.wholphin.util.ExceptionHandler
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.extensions.ticks
import java.util.UUID
import kotlin.time.Duration

@Composable
fun MovieDetails(
    preferences: UserPreferences,
    destination: Destination.MediaItem,
    modifier: Modifier = Modifier,
    viewModel: MovieViewModel =
        hiltViewModel<MovieViewModel, MovieViewModel.Factory>(
            creationCallback = { it.create(destination.itemId) },
        ),
    playlistViewModel: AddPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        // First resume loads the page, later ones (e.g. back from playback) only refresh the user data
        viewModel.onResume()
        onPauseOrDispose { }
    }
    val state by viewModel.state.collectAsState()

    var overviewDialog by remember { mutableStateOf<ItemDetailsDialogInfo?>(null) }
    var showContextMenu by remember { mutableStateOf<ContextMenu?>(null) }
    var showPlaylistDialog by remember { mutableStateOf<Optional<UUID>>(Optional.absent()) }
    val playlistState by playlistViewModel.playlistState.collectAsState()

    val userDto by viewModel.serverRepository.currentUserDtoFlow.collectAsState(null)
    val preferredSubtitleLanguage = userDto?.configuration?.subtitleLanguagePreference

    val contextActions =
        remember {
            ContextMenuActions(
                navigateTo = viewModel::navigateTo,
                onClickWatch = viewModel::setWatched,
                onClickFavorite = viewModel::setFavorite,
                onClickAddPlaylist = { itemId ->
                    playlistViewModel.loadPlaylists()
                    showPlaylistDialog.makePresent(itemId)
                },
                onSendMediaInfo = viewModel.serverReportService::sendMediaReportFor,
                onDeleteItem = viewModel::deleteItem,
                onChooseVersion = viewModel::chooseVersion,
                onChooseTracks = { result ->
                    viewModel.saveTrackSelection(
                        result.item,
                        result.itemPlayback,
                        result.trackIndex,
                        result.streamType,
                    )
                },
                onShowOverview = { overviewDialog = ItemDetailsDialogInfo(it) },
                onClearChosenStreams = { viewModel.clearChosenStreams(it) },
                onChooseVersionPicker = { item, position ->
                    if (item.id == viewModel.itemId) {
                        viewModel.openVersionPicker(position)
                    } else {
                        viewModel.versionPicker.choose(item, position)
                    }
                },
            )
        }

    when (val s = state.loading) {
        is DataLoadingState.Error -> {
            ErrorMessage(s, modifier)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        is DataLoadingState.Success -> {
            val unknownStr = stringResource(R.string.unknown)
            val movie by rememberUpdatedState(s.data)
            val chosenStreams by rememberUpdatedState(state.chosenStreams)
            LifecycleResumeEffect(destination.itemId) {
                viewModel.maybePlayThemeSong(destination.itemId)
                onPauseOrDispose {
                    viewModel.release()
                }
            }
            MovieDetailsContent(
                preferences = preferences,
                movie = movie,
                state = state,
                onClickItem = { index, item ->
                    viewModel.navigateTo(item.destination())
                },
                onClickPerson = {
                    viewModel.navigateTo(
                        Destination.MediaItem(
                            it.id,
                            BaseItemKind.PERSON,
                        ),
                    )
                },
                playOnClick = { viewModel.requestPlay(it) },
                overviewOnClick = {
                    overviewDialog =
                        ItemDetailsDialogInfo(movie)
                },
                moreOnClick = {
                    showContextMenu =
                        ContextMenu.ForBaseItem(
                            fromLongClick = false,
                            item = movie,
                            chosenStreams = chosenStreams,
                            showGoTo = false,
                            showStreamChoices = true,
                            canDelete = state.canDelete,
                            canRemoveContinueWatching = false,
                            canRemoveNextUp = false,
                            actions = contextActions,
                        )
                },
                watchOnClick = {
                    viewModel.setWatched(movie.id, !movie.played)
                },
                favoriteOnClick = {
                    viewModel.setFavorite(movie.id, !movie.favorite)
                },
                onLongClickPerson = { index, person ->
                    showContextMenu =
                        ContextMenu.ForPerson(
                            fromLongClick = true,
                            person = person,
                            actions =
                                PersonContextActions(
                                    navigateTo = viewModel::navigateTo,
                                    onClickFavorite = viewModel::setFavorite,
                                ),
                        )
                },
                onLongClickSimilar = { _, similar ->
                    showContextMenu =
                        ContextMenu.ForBaseItem(
                            fromLongClick = true,
                            item = similar,
                            chosenStreams = null,
                            showGoTo = true,
                            showStreamChoices = false,
                            canDelete = false,
                            canRemoveContinueWatching = false,
                            canRemoveNextUp = false,
                            actions = contextActions,
                        )
                },
                trailerOnClick = {
                    TrailerService.onClick(context, it, viewModel::navigateTo)
                },
                onClickExtra = { index, extra ->
                    viewModel.navigateTo(extra.destination)
                },
                onClickDiscover = { index, item ->
                    viewModel.navigateTo(item.destination)
                },
                canDelete = state.canDelete,
                onConfirmDelete = { state.movie?.let { viewModel.deleteItem(it) } },
                onChooseVersion = { contextActions.onChooseVersion.invoke(movie, it) },
                modifier = modifier,
            )
        }
    }
    state.picker?.let { picker ->
        state.movie?.let { movie ->
            VersionPickerDialog(
                item = movie,
                picker = picker,
                onPick = viewModel::pickVersion,
                onDismissRequest = viewModel::dismissPicker,
            )
        }
    }
    val otherPick by viewModel.versionPicker.request.collectAsState()
    otherPick?.let { request ->
        VersionPickerDialog(
            item = request.item,
            picker = request.picker,
            onPick = viewModel.versionPicker::pick,
            onDismissRequest = viewModel.versionPicker::dismiss,
            onDetails = { viewModel.navigateTo(request.item.destination()) },
        )
    }
    showContextMenu?.let { contextMenu ->
        ContextMenuDialog(
            onDismissRequest = { showContextMenu = null },
            getMediaSource = viewModel.streamChoiceService::chooseSource,
            contextMenu = contextMenu,
            preferredSubtitleLanguage = preferredSubtitleLanguage,
        )
    }
    overviewDialog?.let { info ->
        ItemDetailsDialog(
            info = info,
            showFilePath =
                userDto
                    ?.policy
                    ?.isAdministrator == true,
            onDismissRequest = { overviewDialog = null },
        )
    }
    showPlaylistDialog.compose { itemId ->
        PlaylistDialog(
            title = stringResource(R.string.add_to_playlist),
            state = playlistState,
            onDismissRequest = { showPlaylistDialog.makeAbsent() },
            onClick = {
                playlistViewModel.addToPlaylist(it.id, itemId)
                showPlaylistDialog.makeAbsent()
            },
            createEnabled = true,
            onCreatePlaylist = {
                playlistViewModel.createPlaylistAndAddItem(it, itemId)
                showPlaylistDialog.makeAbsent()
            },
            onSearch = playlistViewModel::loadPlaylists,
            elevation = 3.dp,
        )
    }
}

private const val HEADER_ROW = 0
private const val PEOPLE_ROW = HEADER_ROW + 1
private const val CREW_ROW = PEOPLE_ROW + 1
private const val TRAILER_ROW = CREW_ROW + 1
private const val CHAPTER_ROW = TRAILER_ROW + 1
private const val EXTRAS_ROW = CHAPTER_ROW + 1
private const val SIMILAR_ROW = EXTRAS_ROW + 1
private const val DISCOVER_ROW = SIMILAR_ROW + 1

@Composable
fun MovieDetailsContent(
    preferences: UserPreferences,
    movie: BaseItem,
    state: MovieState,
    playOnClick: (Duration) -> Unit,
    trailerOnClick: (Trailer) -> Unit,
    overviewOnClick: () -> Unit,
    watchOnClick: () -> Unit,
    favoriteOnClick: () -> Unit,
    moreOnClick: () -> Unit,
    onClickItem: (Int, BaseItem) -> Unit,
    onClickPerson: (Person) -> Unit,
    onLongClickPerson: (Int, Person) -> Unit,
    onLongClickSimilar: (Int, BaseItem) -> Unit,
    onClickExtra: (Int, ExtrasItem) -> Unit,
    onClickDiscover: (Int, DiscoverItem) -> Unit,
    canDelete: Boolean,
    onConfirmDelete: () -> Unit,
    onChooseVersion: (MediaSourceInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var position by rememberInt(0)
    val focusRequesters = remember { List(DISCOVER_ROW + 1) { FocusRequester() } }
    val dto = movie.data
    val resumePosition = dto.userData?.playbackPositionTicks?.ticks ?: Duration.ZERO
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    RequestOrRestoreFocus(focusRequesters.getOrNull(position))

    val castAndCrew = remember(state.people) { splitCastAndCrew(state.people) { it.type } }
    val cast = remember(castAndCrew) { castAndCrew.first.map { AioPersonEntry(it, it.role) } }
    val crew = remember(castAndCrew) { crewEntries(castAndCrew.second, ::crewJobName) }
    val background = MaterialTheme.colorScheme.background
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    // item-body (lg): pt-[26vh], pl-0 (right after the sidebar), pr-10, space-y-12
    val headerTop = screenHeight * .27f
    val contentStart = 0.dp

    val listState = rememberLazyListState()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(background),
    ) {
    // item-backdrop is absolute: it scrolls with the page but takes no room, so the sections start right below
    // the header and run over its faded bottom. Full screen width, under the sidebar like AIOStreams.
    AioDetailBackdrop(
        movie = movie,
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
                // lg:pl-0: content right after the 60 dp sidebar, where Nigiro pages start 12 dp further
                .bleedHorizontally(PageToSidebarGap, 0.dp),
    ) {
        item(key = "header") {
            Box(modifier = Modifier.fillMaxWidth()) {
                // item-header: flex-row items-end gap-8
                Row(
                    horizontalArrangement = Arrangement.spacedBy(px(32)),
                    verticalAlignment = Alignment.Bottom,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .padding(start = contentStart, end = px(40), top = headerTop),
                ) {
                    AioPoster(movie)
                    // max-w-3xl space-y-4
                    Column(
                        verticalArrangement = Arrangement.spacedBy(px(16)),
                        modifier = Modifier.widthIn(max = px(768)),
                    ) {
                        AioTitle(movie, preferences.appPreferences.interfacePreferences.showLogos)
                        dto.taglines?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { AioTagline(it) }
                        AioMetaRow(movie)
                        dto.genres?.takeIf { it.isNotEmpty() }?.let { AioGenres(it) }
                        dto.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                            AioOverview(
                                overview = overview,
                                onMore = overviewOnClick,
                                modifier =
                                    Modifier.onFocusChanged {
                                        if (it.isFocused) {
                                            scope.launch(ExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
                                        }
                                    },
                            )
                        }
                        AioActions(
                            resumePosition = resumePosition,
                            watched = dto.userData?.played ?: false,
                            favorite = dto.userData?.isFavorite ?: false,
                            hasTrailer = state.trailers.isNotEmpty(),
                            onPlay = {
                                position = HEADER_ROW
                                playOnClick.invoke(it)
                            },
                            onTrailer = {
                                position = HEADER_ROW
                                state.trailers.firstOrNull()?.let(trailerOnClick)
                            },
                            onWatched = watchOnClick,
                            onFavorite = favoriteOnClick,
                            onMore = moreOnClick,
                            playFocusRequester = focusRequesters[HEADER_ROW],
                            onButtonFocused = {
                                position = HEADER_ROW
                                scope.launch(ExceptionHandler()) { bringIntoViewRequester.bringIntoView() }
                            },
                        )
                    }
                }
            }
        }
        if (cast.isNotEmpty()) {
            item(key = "cast") {
                AioPeopleRow(
                    title = stringResource(R.string.cast_title),
                    people = cast,
                    onClick = {
                        position = PEOPLE_ROW
                        onClickPerson.invoke(it)
                    },
                    onLongClick = { index, person ->
                        position = PEOPLE_ROW
                        onLongClickPerson.invoke(index, person)
                    },
                    contentStart = contentStart,
                    modifier = Modifier.focusRequester(focusRequesters[PEOPLE_ROW]),
                )
            }
        }
        if (crew.isNotEmpty()) {
            item(key = "crew") {
                AioPeopleRow(
                    title = stringResource(R.string.crew_title),
                    people = crew,
                    onClick = {
                        position = CREW_ROW
                        onClickPerson.invoke(it)
                    },
                    onLongClick = { index, person ->
                        position = CREW_ROW
                        onLongClickPerson.invoke(index, person)
                    },
                    contentStart = contentStart,
                    modifier = Modifier.focusRequester(focusRequesters[CREW_ROW]),
                )
            }
        }
        item(key = "details") {
            MovieFacts(movie = movie, modifier = Modifier.padding(start = contentStart, end = px(40)))
        }
        state.similar.letNotEmpty { similar ->
            item(key = "similar") {
                Column(verticalArrangement = Arrangement.spacedBy(px(16))) {
                    AioSectionTitle(stringResource(R.string.more_like_this), Modifier.padding(start = contentStart))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(NigiroDimens.CardGap),
                        contentPadding = PaddingValues(start = contentStart, end = px(32)),
                        modifier = Modifier.focusRequester(focusRequesters[SIMILAR_ROW]),
                    ) {
                        itemsIndexed(similar, key = { _, item -> item.id }) { index, item ->
                            ExpandingPosterCard(
                                item = item,
                                onClick = {
                                    position = SIMILAR_ROW
                                    onClickItem.invoke(index, item)
                                },
                                onLongClick = {
                                    position = SIMILAR_ROW
                                    onLongClickSimilar.invoke(index, item)
                                },
                                cardHeight = NigiroDimens.PosterHeight,
                                cornerText = item.ui.episodeUnplayedCornerText,
                                expandable = false,
                            )
                        }
                    }
                }
            }
        }
        state.chapters.letNotEmpty { chapters ->
            item(key = "chapters") {
                ChapterRow(
                    chapters = chapters,
                    onClick = {
                        position = CHAPTER_ROW
                        playOnClick.invoke(it.position)
                    },
                    onLongClick = {},
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = DetailContentInset)
                            .focusRequester(focusRequesters[CHAPTER_ROW]),
                )
            }
        }
        state.extras.letNotEmpty { extras ->
            item(key = "extras") {
                ExtrasRow(
                    extras = extras,
                    onClickItem = { index, item ->
                        position = EXTRAS_ROW
                        onClickExtra.invoke(index, item)
                    },
                    onLongClickItem = { _, _ -> },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = DetailContentInset)
                            .focusRequester(focusRequesters[EXTRAS_ROW]),
                )
            }
        }
        state.discovered.letNotEmpty { discovered ->
            item(key = "discover") {
                DiscoverRow(
                    row =
                        DiscoverRowData(
                            ResStringProvider(R.string.discover),
                            DataLoadingState.Success(discovered),
                            type = DiscoverRequestType.UNKNOWN,
                        ),
                    onClickItem = { index: Int, item: DiscoverItem ->
                        position = DISCOVER_ROW
                        onClickDiscover.invoke(index, item)
                    },
                    onLongClickItem = { _, _ -> },
                    onCardFocus = {},
                    focusRequester = focusRequesters[DISCOVER_ROW],
                )
            }
        }
    }
    }
}

/** Nigiro pages start 12 dp right of the 60 dp sidebar; AIOStreams content starts right after it */
internal val PageToSidebarGap = 12.dp

/** Lets content extend past the page's horizontal padding (like the home hero) */
internal fun Modifier.bleedHorizontally(
    start: androidx.compose.ui.unit.Dp,
    end: androidx.compose.ui.unit.Dp,
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

/** Crew jobs in Italian, as the AIOStreams app shows them ("Regista Scrittore") */
internal fun crewJobName(kind: PersonKind): String =
    when (kind) {
        PersonKind.DIRECTOR -> "Regista"
        PersonKind.WRITER -> "Scrittore"
        PersonKind.PRODUCER -> "Produttore"
        PersonKind.COMPOSER -> "Compositore"
        PersonKind.EDITOR -> "Montaggio"
        else -> kind.serialName
    }
