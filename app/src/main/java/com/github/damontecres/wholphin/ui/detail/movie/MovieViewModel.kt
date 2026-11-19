package com.github.damontecres.wholphin.ui.detail.movie

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.data.ChosenStreams
import com.github.damontecres.wholphin.data.ExtrasItem
import com.github.damontecres.wholphin.data.ItemPlaybackRepository
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Chapter
import com.github.damontecres.wholphin.data.model.DiscoverItem
import com.github.damontecres.wholphin.data.model.ItemPlayback
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.data.model.Trailer
import com.github.damontecres.wholphin.services.BackdropService
import com.github.damontecres.wholphin.services.ExtrasService
import com.github.damontecres.wholphin.services.FavoriteWatchManager
import com.github.damontecres.wholphin.services.LocalTwinService
import com.github.damontecres.wholphin.services.MediaManagementService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.PeopleFavorites
import com.github.damontecres.wholphin.services.SeerrService
import com.github.damontecres.wholphin.services.ServerReportService
import com.github.damontecres.wholphin.services.StreamChoiceService
import com.github.damontecres.wholphin.services.ThemeSongPlayer
import com.github.damontecres.wholphin.services.TrailerService
import com.github.damontecres.wholphin.services.UserPreferencesService
import com.github.damontecres.wholphin.services.deleteItem
import com.github.damontecres.wholphin.ui.ItemRowFields
import com.github.damontecres.wholphin.ui.launchDefault
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.letNotEmpty
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.showToast
import com.github.damontecres.wholphin.util.DataLoadingState
import com.github.damontecres.wholphin.util.SourceRanking
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.util.WholphinDispatchers
import com.github.damontecres.wholphin.R
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import kotlin.time.Duration
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID

@HiltViewModel(assistedFactory = MovieViewModel.Factory::class)
class MovieViewModel
    @AssistedInject
    constructor(
        private val api: ApiClient,
        private val seerrService: SeerrService,
        @param:ApplicationContext private val context: Context,
        private val navigationManager: NavigationManager,
        val serverRepository: ServerRepository,
        val itemPlaybackRepository: ItemPlaybackRepository,
        val streamChoiceService: StreamChoiceService,
        val serverReportService: ServerReportService,
        private val themeSongPlayer: ThemeSongPlayer,
        private val favoriteWatchManager: FavoriteWatchManager,
        private val peopleFavorites: PeopleFavorites,
        private val trailerService: TrailerService,
        private val extrasService: ExtrasService,
        private val userPreferencesService: UserPreferencesService,
        private val backdropService: BackdropService,
        private val mediaManagementService: MediaManagementService,
        private val localTwinService: LocalTwinService,
        @Assisted val itemId: UUID,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(itemId: UUID): MovieViewModel
        }

        private val _state = MutableStateFlow(MovieState())
        val state: StateFlow<MovieState> = _state

        /** "Choose a version" for the other titles on the page (similar movies), whose versions aren't loaded */
        val versionPicker =
            PlayVersionPicker(
                scope = viewModelScope,
                context = context,
                api = api,
                itemPlaybackRepository = itemPlaybackRepository,
                userPreferencesService = userPreferencesService,
                localTwinService = localTwinService,
                streamChoiceService = streamChoiceService,
                navigate = ::navigateTo,
            )

        // Declared before the init block: initializers below it would run after init() and reset them
        private var started = false
        private var resumedOnce = false
        private var sourcesJob: Deferred<BaseItem?>? = null
        private var playJob: Job? = null

        init {
            init()
            viewModelScope.launchDefault {
                mediaManagementService.collectCanDelete(state.map { it.movie }) { canDelete ->
                    _state.update {
                        it.copy(canDelete = canDelete)
                    }
                }
            }
        }

        /** The movie with its media sources: for a streamed (Gelato) item this makes the server look them up */
        private suspend fun getMovie(): BaseItem =
            api.userLibraryApi.getItem(itemId).content.let {
                BaseItem(it)
            }

        /** Everything the page shows, without media sources: ~0.1 s even for a streamed item never opened */
        private suspend fun getMovieLight(): BaseItem =
            api.itemsApi
                .getItems(
                    GetItemsRequest(
                        userId = serverRepository.currentUser?.id,
                        ids = listOf(itemId),
                        fields = LightDetailFields,
                        enableTotalRecordCount = false,
                    ),
                ).content.items
                .firstOrNull()
                ?.let { BaseItem(it) }
                ?: getMovie()

        /** Keeps the media sources already loaded when only the user data (watched, favorite, position) changed */
        private fun withLoadedSources(fresh: BaseItem): BaseItem {
            val loaded = (state.value.sources as? DataLoadingState.Success<BaseItem>)?.data ?: return fresh
            return loaded.copy(data = loaded.data.copy(userData = fresh.data.userData))
        }

        /**
         * The media sources (and the downloaded copy, if any), loaded once and shared by the background
         * prefetch and the play button. A failed load is retried on the next call.
         */
        private fun sources(): Deferred<BaseItem?> {
            sourcesJob?.let { job ->
                if (!job.isCompleted || job.getCompleted() != null) return job
            }
            return viewModelScope
                .async(WholphinDispatchers.IO) {
                    _state.update { it.copy(sources = DataLoadingState.Loading) }
                    try {
                        val full = getMovie()
                        onSourcesLoaded(full)
                        full
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.e(ex, "Failed to fetch sources for movie %s", itemId)
                        _state.update { it.copy(sources = DataLoadingState.Error(ex)) }
                        null
                    }
                }.also { sourcesJob = it }
        }

        private suspend fun onSourcesLoaded(full: BaseItem) {
            // A streamed (Gelato) movie downloaded via Seerr is a separate item: offer & play its local file
            val localCopy =
                try {
                    localTwinService.findLocalTwin(full.data)?.let { BaseItem(it) }
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    Timber.w(ex, "Could not look for a local copy of %s", itemId)
                    null
                }
            val chosenStreams = chosenStreamsFor(full, localCopy)
            _state.update {
                it.copy(
                    loading = DataLoadingState.Success(full),
                    sources = DataLoadingState.Success(full),
                    chosenStreams = chosenStreams,
                    localCopy = localCopy,
                    chapters = Chapter.fromDto(full.data).ifEmpty { it.chapters },
                )
            }
        }

        /** The tracks playback will use: the local copy's, unless one of the streamed versions was chosen */
        private suspend fun chosenStreamsFor(
            full: BaseItem,
            localCopy: BaseItem?,
        ): ChosenStreams? {
            val prefs = userPreferencesService.getCurrent()
            val chosen = itemPlaybackRepository.getSelectedTracks(itemId, full, prefs)
            if (localCopy == null) return chosen
            val explicitStream =
                chosen?.itemPlayback?.sourceId?.let { sourceId ->
                    full.data.mediaSources.orEmpty().any { it.id?.toUUIDOrNull() == sourceId }
                } == true
            return if (explicitStream) chosen else itemPlaybackRepository.getSelectedTracks(localCopy.id, localCopy, prefs)
        }

        /**
         * Called on every resume: the first time the page loads, afterwards (e.g. back from playback)
         * only the user data is refreshed, without looking up the sources again
         */
        fun onResume() {
            if (!resumedOnce) {
                // The first resume is the page opening: init() already runs from the constructor
                resumedOnce = true
                if (!started) init()
                return
            }
            refreshUserData()
        }

        private fun refreshUserData() =
            viewModelScope.launchIO {
                try {
                    val fresh = withLoadedSources(getMovieLight())
                    val chosen =
                        if (state.value.sources is DataLoadingState.Success) {
                            chosenStreamsFor(fresh, state.value.localCopy)
                        } else {
                            state.value.chosenStreams
                        }
                    _state.update { it.copy(loading = DataLoadingState.Success(fresh), chosenStreams = chosen) }
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    Timber.w(ex, "Failed to refresh movie %s", itemId)
                }
            }

        fun init(): Job {
            started = true
            return viewModelScope.launchDefault {
                val movie =
                    try {
                        getMovieLight()
                    } catch (ex: Exception) {
                        Timber.e(ex, "Failed to fetch movie %s", itemId)
                        _state.update { it.copy(loading = DataLoadingState.Error(ex)) }
                        return@launchDefault
                    }
                val remoteTrailers = trailerService.getRemoteTrailers(movie)
                _state.update {
                    it.copy(
                        loading = DataLoadingState.Success(movie),
                        trailers = remoteTrailers,
                        chapters = Chapter.fromDto(movie.data),
                    )
                }
                // The detail page draws its own full-size backdrop: the app-wide one would only download
                // a second copy and extract its colours for a layer that stays covered
                backdropService.clearBackdrop()
                // Sources in the background, once the page is drawn: by the time play is pressed they are usually ready
                viewModelScope.launch {
                    delay(SOURCES_PREFETCH_DELAY_MS)
                    sources()
                }
                viewModelScope.launchIO {
                    trailerService.getLocalTrailers(movie).letNotEmpty { localTrailers ->
                        _state.update {
                            it.copy(
                                trailers = localTrailers + remoteTrailers,
                            )
                        }
                    }
                }
                viewModelScope.launchIO {
                    val people = peopleFavorites.getPeopleFor(movie)
                    _state.update {
                        it.copy(
                            people = people,
                        )
                    }
                }
                viewModelScope.launchIO {
                    val extras = extrasService.getExtras(itemId)
                    _state.update {
                        it.copy(
                            extras = extras,
                        )
                    }
                }
                viewModelScope.launchIO {
                    val results = seerrService.similar(movie).orEmpty()
                    _state.update {
                        it.copy(
                            discovered = results,
                        )
                    }
                }
                viewModelScope.launchIO {
                    if (state.value.similar.isEmpty()) {
                        val similar =
                            api.libraryApi
                                .getSimilarItems(
                                    GetSimilarItemsRequest(
                                        userId = serverRepository.currentUser?.id,
                                        itemId = itemId,
                                        fields = ItemRowFields,
                                        limit = 25,
                                    ),
                                ).content.items
                                .map { BaseItem(it) }

                        _state.update { it.copy(similar = similar) }
                    }
                }
            }
        }

        /**
         * Play pressed: playback starts right away with the version chosen before, else the one the movie
         * last played with, else the best Italian one the Wi-Fi carries (see StreamChoiceService.chooseSource)
         */
        fun requestPlay(position: Duration) {
            playJob?.cancel()
            _state.update { it.copy(picker = null) }
            playJob = viewModelScope.launchIO { startPlayback(position) }
        }

        /**
         * "Choose a version": with one version (e.g. a file on disk) playback starts right away, with several the
         * version picker opens. If the sources are still loading the picker shows a loading state meanwhile.
         */
        fun openVersionPicker(position: Duration) {
            playJob?.cancel()
            val pending = sources()
            if (!pending.isCompleted) {
                _state.update { it.copy(picker = PickerState.Loading(position)) }
            }
            playJob =
                viewModelScope.launchIO {
                    val full = pending.await()
                    if (full == null) {
                        _state.update { it.copy(picker = null) }
                        showToast(context, context.getString(R.string.sources_unavailable), Toast.LENGTH_LONG)
                        return@launchIO
                    }
                    val options =
                        versionOptions(
                            // Same order as the version menu: preferred language, cached debrid, quality
                            streamed = SourceRanking.rank(full.data.mediaSources.orEmpty().filter { it.id.isNotNullOrBlank() }, capabilities = null),
                            local = state.value.localCopy?.data?.mediaSources.orEmpty(),
                            localLabel = context.getString(R.string.on_disk),
                        )
                    when (val decision = decidePlay(options)) {
                        is PlayDecision.PlayNow -> {
                            _state.update { it.copy(picker = null) }
                            startPlayback(position)
                        }

                        is PlayDecision.Pick -> {
                            // "Last choice": the version picked before, else the one the movie last played with,
                            // never just the default
                            val lastId =
                                streamChoiceService.lastSourceId(itemId, state.value.chosenStreams?.itemPlayback)
                            val chosenId =
                                decision.options.firstOrNull { it.source.id?.toUUIDOrNull() == lastId }?.source?.id
                            _state.update { it.copy(picker = PickerState.Options(decision.options, position, chosenId)) }
                        }
                    }
                }
        }

        /** A row of the picker: [position] is where the Resume/From the start toggle says to begin */
        fun pickVersion(
            option: VersionOption,
            position: Duration,
        ) {
            _state.update { it.copy(picker = null) }
            playJob =
                viewModelScope.launchIO {
                    val movie = state.value.movie ?: return@launchIO
                    val localCopy = state.value.localCopy
                    if (option.isLocal && localCopy != null) {
                        chooseLocalCopyNow(movie, localCopy)
                    } else {
                        option.source.id?.toUUIDOrNull()?.let { savePlayVersionNow(movie, it) }
                    }
                    startPlayback(position)
                }
        }

        fun dismissPicker() {
            playJob?.cancel()
            _state.update { it.copy(picker = null) }
        }

        private suspend fun startPlayback(position: Duration) {
            withContext(WholphinDispatchers.Main) {
                navigateTo(Destination.Playback(itemId, position.inWholeMilliseconds))
            }
        }

        fun setWatched(
            itemId: UUID,
            played: Boolean,
        ) = viewModelScope.launchDefault {
            try {
                favoriteWatchManager.setWatched(itemId, played)
                withLoadedSources(getMovieLight()).let { movie ->
                    _state.update {
                        it.copy(loading = DataLoadingState.Success(movie))
                    }
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Error updating watch status for movie %s", itemId)
                showToast(context, "Something went wrong...")
            }
        }

        fun setFavorite(
            itemId: UUID,
            favorite: Boolean,
        ) = viewModelScope.launchDefault {
            try {
                favoriteWatchManager.setFavorite(itemId, favorite)
                val movie = withLoadedSources(getMovieLight())
                _state.update {
                    it.copy(loading = DataLoadingState.Success(movie))
                }
                if (itemId != movie.id) {
                    viewModelScope.launchIO {
                        val people = peopleFavorites.getPeopleFor(movie)
                        _state.update { it.copy(people = people) }
                    }
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Error updating favorite  %s", itemId)
                showToast(context, "Something went wrong...")
            }
        }

        /**
         * The user chose a version: either the local copy or one of the item's own (streamed) sources
         */
        fun chooseVersion(
            item: BaseItem,
            source: MediaSourceInfo,
        ) {
            val localCopy = state.value.localCopy
            if (localCopy != null && localCopy.data.mediaSources.orEmpty().any { it.id == source.id }) {
                chooseLocalCopy(item, localCopy)
            } else {
                val sourceId = source.id?.toUUIDOrNull()
                if (sourceId != null) {
                    savePlayVersion(item, sourceId)
                } else {
                    Timber.w("Cannot save version with non-UUID id %s", source.id)
                }
            }
        }

        private fun chooseLocalCopy(
            item: BaseItem,
            localCopy: BaseItem,
        ) {
            viewModelScope.launchIO { chooseLocalCopyNow(item, localCopy) }
        }

        private suspend fun chooseLocalCopyNow(
            item: BaseItem,
            localCopy: BaseItem,
        ) {
            run {
                val prefs = userPreferencesService.getCurrent()
                // Forget the streamed version chosen for this item, so playback uses the local file again
                itemPlaybackRepository
                    .getSelectedTracks(item.id, item, prefs)
                    ?.takeIf { it.itemPlayback?.sourceId != null }
                    ?.let { itemPlaybackRepository.deleteChosenStreams(it) }
                val chosen = itemPlaybackRepository.getSelectedTracks(localCopy.id, localCopy, prefs)
                _state.update { it.copy(chosenStreams = chosen) }
            }
        }

        fun savePlayVersion(
            item: BaseItem,
            sourceId: UUID,
        ) {
            viewModelScope.launchIO { savePlayVersionNow(item, sourceId) }
        }

        private suspend fun savePlayVersionNow(
            item: BaseItem,
            sourceId: UUID,
        ) {
            run {
                val prefs = userPreferencesService.getCurrent()
                val result = itemPlaybackRepository.savePlayVersion(item.id, sourceId)
                val chosen =
                    result?.let {
                        itemPlaybackRepository.getChosenItemFromPlayback(item, result, prefs)
                    }
                _state.update { it.copy(chosenStreams = chosen) }
            }
        }

        fun saveTrackSelection(
            item: BaseItem,
            itemPlayback: ItemPlayback?,
            trackIndex: Int,
            type: MediaStreamType,
        ) {
            viewModelScope.launchIO {
                val prefs = userPreferencesService.getCurrent()
                val result =
                    itemPlaybackRepository.saveTrackSelection(
                        item = item,
                        itemPlayback = itemPlayback,
                        trackIndex = trackIndex,
                        type = type,
                    )
                val chosen = itemPlaybackRepository.getChosenItemFromPlayback(item, result, prefs)
                _state.update { it.copy(chosenStreams = chosen) }
            }
        }

        fun maybePlayThemeSong(itemId: UUID) {
            viewModelScope.launchIO {
                themeSongPlayer.playThemeFor(itemId)
                addCloseable {
                    themeSongPlayer.stop()
                }
            }
        }

        fun release() {
            themeSongPlayer.stop()
        }

        fun navigateTo(destination: Destination) {
            release()
            navigationManager.navigateTo(destination)
        }

        fun clearChosenStreams(chosenStreams: ChosenStreams?) {
            viewModelScope.launchIO {
                itemPlaybackRepository.deleteChosenStreams(chosenStreams)
                state.value.movie?.let { item ->
                    val result =
                        itemPlaybackRepository.getSelectedTracks(
                            itemId,
                            item,
                            userPreferencesService.getCurrent(),
                        )
                    _state.update { it.copy(chosenStreams = result) }
                }
            }
        }

        fun deleteItem(item: BaseItem) {
            deleteItem(context, mediaManagementService, item) {
                navigationManager.goBack()
            }
        }
    }

data class MovieState(
    val loading: DataLoadingState<BaseItem> = DataLoadingState.Pending,
    val trailers: List<Trailer> = emptyList(),
    val people: List<Person> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val extras: List<ExtrasItem> = emptyList(),
    val similar: List<BaseItem> = emptyList(),
    val discovered: List<DiscoverItem> = emptyList(),
    val chosenStreams: ChosenStreams? = null,
    val canDelete: Boolean = false,
    /**
     * The downloaded copy of a streamed (Gelato) movie, if any
     */
    val localCopy: BaseItem? = null,
    /** The movie with its media sources, loaded after the page is shown */
    val sources: DataLoadingState<BaseItem> = DataLoadingState.Pending,
    /** The version picker, while open */
    val picker: PickerState? = null,
) {
    val movie: BaseItem? = (loading as? DataLoadingState.Success<BaseItem>)?.data
}

sealed interface PickerState {
    val position: Duration

    data class Loading(
        override val position: Duration,
    ) : PickerState

    data class Options(
        val options: List<VersionOption>,
        override val position: Duration,
        val chosenSourceId: String?,
    ) : PickerState
}

private const val SOURCES_PREFETCH_DELAY_MS = 800L
