package com.github.damontecres.wholphin.ui.explore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.services.ImageUrlService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.RemoteConfigService
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.main.HomeMediaFilter
import com.github.damontecres.wholphin.ui.main.PlatformTile
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.util.StringStringProvider
import com.github.damontecres.wholphin.util.RequestHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import com.github.damontecres.wholphin.data.model.BaseItem
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

        // Genre tiles per filter: switching back and forth and re-entering Explore reuses the TTL cache
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

        /** A random title of the whole catalog, with the current filter */
        fun openRandom() {
            viewModelScope.launchIO {
                try {
                    val item =
                        api.itemsApi
                            .getItems(
                                GetItemsRequest(
                                    userId = serverRepository.currentUser?.id,
                                    recursive = true,
                                    includeItemTypes = _state.value.filter.itemKinds(),
                                    sortBy = listOf(ItemSortBy.RANDOM),
                                    limit = 1,
                                    enableTotalRecordCount = false,
                                ),
                            ).content.items
                            .firstOrNull() ?: return@launchIO
                    val destination = BaseItem.from(item, api).destination()
                    withContext(Dispatchers.Main) { navigationManager.navigateTo(destination) }
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    Timber.e(ex, "Error picking a random title")
                }
            }
        }

        fun openPlatform(tile: PlatformTile) {
            val source =
                ExploreGridSource(
                    movieCollectionId = tile.movieCollectionId?.toUUIDOrNull(),
                    seriesCollectionId = tile.seriesCollectionId?.toUUIDOrNull(),
                    filter = _state.value.filter,
                )
            if (source.movieCollectionId == null && source.seriesCollectionId == null) return
            openGrid(tile.name, source)
        }

        fun openGenre(tile: ExploreGenreTile) {
            val name = context.getString(tile.genre.label)
            openGrid(name, ExploreGridSource(genreIds = tile.genreIds, filter = _state.value.filter))
        }

        private fun openGrid(
            title: String,
            source: ExploreGridSource,
        ) {
            val (request, handler) = source.request(serverRepository.currentUser?.id, source.filter)
            @Suppress("UNCHECKED_CAST")
            navigationManager.navigateTo(
                Destination.ItemGrid(
                    title = StringStringProvider(title),
                    request = request,
                    requestHandler = handler as RequestHandler<Any>,
                    explore = source,
                ),
            )
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
            val userId = serverRepository.currentUser?.id
            val cacheKey = userId to filter
            genreTilesCache[cacheKey]
                ?.takeIf { System.currentTimeMillis() - it.loadedAtMs < GENRE_CACHE_TTL_MS }
                ?.let { cached ->
                    genreJob?.cancel()
                    _state.update { it.copy(genres = cached.tiles, genresLoading = false) }
                    return
                }
            genreJob?.cancel()
            _state.update { it.copy(genresLoading = true) }
            genreJob =
                viewModelScope.launchIO {
                    try {
                        val idsByName =
                            genreIdsCache[userId]
                                ?.takeIf { System.currentTimeMillis() - it.loadedAtMs < GENRE_CACHE_TTL_MS }
                                ?.idsByName
                                ?: fetchGenreIds(userId).also {
                                    genreIdsCache[userId] = CachedGenreIds(it, System.currentTimeMillis())
                                }
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
                        genreTilesCache[cacheKey] = CachedGenreTiles(tiles, System.currentTimeMillis())
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
                            limit = 3,
                            fields = emptyList(),
                            enableImageTypes = listOf(ImageType.BACKDROP),
                            imageTypeLimit = 1,
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

        private data class CachedGenreTiles(
            val tiles: List<ExploreGenreTile>,
            val loadedAtMs: Long,
        )

        private data class CachedGenreIds(
            val idsByName: Map<String, UUID>,
            val loadedAtMs: Long,
        )

        private companion object {
            const val TILE_IMAGE_WIDTH = 480
            const val TILE_IMAGE_HEIGHT = 270
            const val GENRE_CACHE_TTL_MS = 15 * 60 * 1000L

            private val genreTilesCache = ConcurrentHashMap<Pair<UUID?, HomeMediaFilter>, CachedGenreTiles>()
            private val genreIdsCache = ConcurrentHashMap<UUID?, CachedGenreIds>()
        }
    }
