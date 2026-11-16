package com.github.damontecres.wholphin.ui.explore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.services.ImageUrlService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.RemoteConfigService
import com.github.damontecres.wholphin.ui.SlimItemFields
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.main.HomeMediaFilter
import com.github.damontecres.wholphin.ui.main.PlatformTile
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.util.StringStringProvider
import com.github.damontecres.wholphin.util.GetItemsRequestHandler
import com.github.damontecres.wholphin.util.PlatformItemsRequest
import com.github.damontecres.wholphin.util.PlatformItemsRequestHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetGenresRequest
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class ExploreState(
    val filter: HomeMediaFilter = HomeMediaFilter.ALL,
    val platforms: List<PlatformTile> = emptyList(),
    val genres: List<ExploreGenreTile> = emptyList(),
    val genresLoading: Boolean = true,
)

@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val api: ApiClient,
        private val serverRepository: ServerRepository,
        private val imageUrlService: ImageUrlService,
        private val remoteConfigService: RemoteConfigService,
        private val navigationManager: NavigationManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ExploreState())
        val state: StateFlow<ExploreState> = _state

        // Genre tiles per filter: switching back and forth does not reload them
        private val genreTiles = ConcurrentHashMap<HomeMediaFilter, List<ExploreGenreTile>>()
        private var genreIdsByName: Map<String, UUID>? = null
        private var genreJob: Job? = null

        init {
            _state.update { it.copy(platforms = buildPlatforms()) }
            loadGenres(HomeMediaFilter.ALL)
        }

        fun setFilter(filter: HomeMediaFilter) {
            if (_state.value.filter == filter) return
            _state.update { it.copy(filter = filter) }
            loadGenres(filter)
        }

        fun openPlatform(tile: PlatformTile) {
            val filter = _state.value.filter
            val movieId = tile.movieCollectionId?.toUUIDOrNull()?.takeIf { filter != HomeMediaFilter.SERIES }
            val seriesId = tile.seriesCollectionId?.toUUIDOrNull()?.takeIf { filter != HomeMediaFilter.MOVIE }
            if (movieId == null && seriesId == null) return
            navigationManager.navigateTo(
                Destination.ItemGrid(
                    title = StringStringProvider(titleWithFilter(tile.name, filter)),
                    request = PlatformItemsRequest(serverRepository.currentUser?.id, movieId, seriesId),
                    requestHandler = PlatformItemsRequestHandler,
                ),
            )
        }

        fun openGenre(tile: ExploreGenreTile) {
            val filter = _state.value.filter
            val name = "${tile.genre.emoji} ${context.getString(tile.genre.label)}"
            navigationManager.navigateTo(
                Destination.ItemGrid(
                    title = StringStringProvider(titleWithFilter(name, filter)),
                    request =
                        GetItemsRequest(
                            userId = serverRepository.currentUser?.id,
                            recursive = true,
                            includeItemTypes = filter.itemKinds(),
                            genreIds = tile.genreIds,
                            // Newest first, like the streaming apps
                            sortBy = listOf(ItemSortBy.PREMIERE_DATE, ItemSortBy.SORT_NAME),
                            sortOrder = listOf(SortOrder.DESCENDING, SortOrder.ASCENDING),
                            fields = SlimItemFields,
                        ),
                    requestHandler = GetItemsRequestHandler,
                ),
            )
        }

        private fun titleWithFilter(
            name: String,
            filter: HomeMediaFilter,
        ): String =
            when (filter) {
                HomeMediaFilter.ALL -> name
                HomeMediaFilter.MOVIE -> "$name · ${context.getString(R.string.home_filter_movies)}"
                HomeMediaFilter.SERIES -> "$name · ${context.getString(R.string.home_filter_series)}"
            }

        private fun buildPlatforms(): List<PlatformTile> =
            remoteConfigService.config.value.home.platforms
                .filter { !it.isHome }
                .map { platform ->
                    val coverId =
                        listOfNotNull(platform.movieCollectionId, platform.seriesCollectionId)
                            .firstNotNullOfOrNull { it.toUUIDOrNull() }
                    PlatformTile(
                        key = platform.key,
                        name = platform.name,
                        movieCollectionId = platform.movieCollectionId,
                        seriesCollectionId = platform.seriesCollectionId,
                        isHome = platform.isHome,
                        coverUrl =
                            coverId?.let {
                                imageUrlService.getItemImageUrl(
                                    itemId = it,
                                    imageType = ImageType.THUMB,
                                    fillWidth = TILE_IMAGE_WIDTH,
                                    fillHeight = TILE_IMAGE_HEIGHT,
                                )
                            },
                    )
                }

        private fun loadGenres(filter: HomeMediaFilter) {
            genreTiles[filter]?.let { tiles ->
                genreJob?.cancel()
                _state.update { it.copy(genres = tiles, genresLoading = false) }
                return
            }
            genreJob?.cancel()
            _state.update { it.copy(genresLoading = true) }
            genreJob =
                viewModelScope.launchIO {
                    try {
                        val userId = serverRepository.currentUser?.id
                        val idsByName = genreIdsByName ?: fetchGenreIds(userId).also { genreIdsByName = it }
                        val kinds = filter.itemKinds()
                        val semaphore = Semaphore(4)
                        val tiles =
                            coroutineScope {
                                ExploreGenres
                                    .map { genre ->
                                        async { semaphore.withPermit { loadGenreTile(genre, idsByName, kinds, userId) } }
                                    }.awaitAll()
                                    .filterNotNull()
                            }
                        genreTiles[filter] = tiles
                        if (_state.value.filter == filter) {
                            _state.update { it.copy(genres = tiles, genresLoading = false) }
                        }
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error loading Explore genres")
                        _state.update { it.copy(genresLoading = false) }
                    }
                }
        }

        private suspend fun fetchGenreIds(userId: UUID?): Map<String, UUID> =
            api.genresApi
                .getGenres(
                    GetGenresRequest(
                        userId = userId,
                        includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                    ),
                ).content.items
                .mapNotNull { dto -> dto.name?.let { it.genreKey() to dto.id } }
                .toMap()

        private suspend fun loadGenreTile(
            genre: ExploreGenre,
            idsByName: Map<String, UUID>,
            kinds: List<BaseItemKind>,
            userId: UUID?,
        ): ExploreGenreTile? {
            val ids = genre.serverIds(idsByName)
            if (ids.isEmpty()) return null
            val result =
                api.itemsApi
                    .getItems(
                        GetItemsRequest(
                            userId = userId,
                            recursive = true,
                            includeItemTypes = kinds,
                            genreIds = ids,
                            // A random title each time the hub opens, like Nuvio's dynamic backdrops
                            sortBy = listOf(ItemSortBy.RANDOM),
                            limit = 4,
                            enableTotalRecordCount = true,
                            enableUserData = false,
                        ),
                    ).content
            if (result.totalRecordCount <= 0) return null
            val withBackdrop = result.items.firstOrNull { !it.backdropImageTags.isNullOrEmpty() }
            val imageUrl =
                withBackdrop?.let { item ->
                    imageUrlService.getItemImageUrl(
                        itemId = item.id,
                        itemType = item.type,
                        seriesId = null,
                        useSeriesForPrimary = false,
                        imageType = ImageType.BACKDROP,
                        imageTags = item.imageTags.orEmpty(),
                        backdropTags = item.backdropImageTags.orEmpty(),
                        fillWidth = TILE_IMAGE_WIDTH,
                        fillHeight = TILE_IMAGE_HEIGHT,
                    )
                }
            return ExploreGenreTile(genre, ids, result.totalRecordCount, imageUrl)
        }

        private companion object {
            const val TILE_IMAGE_WIDTH = 480
            const val TILE_IMAGE_HEIGHT = 270
        }
    }
