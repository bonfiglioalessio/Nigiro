package com.github.damontecres.wholphin.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowConfig
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.data.model.RemoteHeroConfig
import com.github.damontecres.wholphin.data.model.RemotePlatformConfig
import com.github.damontecres.wholphin.data.model.RemoteWholphinConfig
import com.github.damontecres.wholphin.data.model.ServerUserConfig
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.PrefContentScale
import com.github.damontecres.wholphin.services.BackdropService
import com.github.damontecres.wholphin.services.DatePlayedService
import com.github.damontecres.wholphin.services.FavoriteWatchManager
import com.github.damontecres.wholphin.services.HomeDataService
import com.github.damontecres.wholphin.services.ImageUrlService
import com.github.damontecres.wholphin.services.LatestNextUpService
import com.github.damontecres.wholphin.services.MediaManagementService
import com.github.damontecres.wholphin.services.NavDrawerService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.RemoteConfigService
import com.github.damontecres.wholphin.services.ServerReportService
import com.github.damontecres.wholphin.services.UserPreferencesService
import com.github.damontecres.wholphin.services.deleteItem
import com.github.damontecres.wholphin.ui.AspectRatio
import com.github.damontecres.wholphin.ui.HomeItemFields
import com.github.damontecres.wholphin.ui.collectLatestIn
import com.github.damontecres.wholphin.ui.combinePair
import com.github.damontecres.wholphin.ui.components.ViewOptionImageType
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.launchDefault
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.showToast
import com.github.damontecres.wholphin.ui.util.StringStringProvider
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.GetItemsRequestHandler
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import com.github.damontecres.wholphin.util.LoadingState
import com.github.damontecres.wholphin.util.WholphinDispatchers
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        val navigationManager: NavigationManager,
        val serverRepository: ServerRepository,
        val serverReportService: ServerReportService,
        private val api: ApiClient,
        private val navDrawerService: NavDrawerService,
        private val homeDataService: HomeDataService,
        private val remoteConfigService: RemoteConfigService,
        private val imageUrlService: ImageUrlService,
        private val favoriteWatchManager: FavoriteWatchManager,
        private val datePlayedService: DatePlayedService,
        private val backdropService: BackdropService,
        private val userPreferencesService: UserPreferencesService,
        private val mediaManagementService: MediaManagementService,
        private val latestNextUpService: LatestNextUpService,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HomeState.EMPTY)
        val state: StateFlow<HomeState> = _state

        private var dataLoadingJob: Job? = null
        private var cachedContinueWatching: HomeRowLoadingState.Success? = null
        private var cachedCatalogByKey: Map<String, CachedPlatformCatalog> = emptyMap()
        private var cachedConfig: RemoteWholphinConfig = RemoteWholphinConfig.DEFAULT
        private var hasAppliedInitialFilter = false

        // When the platform catalogs were last fully loaded, see CATALOG_TTL_MS
        private var catalogsLoadedAtMs: Long = 0L
        private val catalogMutex = Mutex()

        init {
            datePlayedService.invalidateAll()
            serverRepository.currentUserDtoFlow
                .combinePair(remoteConfigService.config)
                .collectLatestIn(viewModelScope) { (userDto, remoteConfig) ->
                    dataLoadingJob?.cancel()
                    if (userDto == null) {
                        _state.update { HomeState.EMPTY }
                        return@collectLatestIn
                    }
                    val userChanged = state.value.userId != null && state.value.userId != userDto.id
                    if (userChanged) {
                        hasAppliedInitialFilter = false
                        cachedContinueWatching = null
                        cachedCatalogByKey = emptyMap()
                        _state.update { HomeState.EMPTY }
                    }
                    val configChanged = remoteConfig != cachedConfig
                    cachedConfig = remoteConfig
                    if (!hasAppliedInitialFilter) {
                        hasAppliedInitialFilter = true
                        _state.update {
                            it.copy(
                                selectedFilter = parseHomeMediaFilter(remoteConfig.home.defaultFilter),
                            )
                        }
                    }
                    dataLoadingJob =
                        viewModelScope.launchIO {
                            loadStremioHome(userDto, remoteConfig, forceCatalogs = configChanged || userChanged)
                        }
                }
        }

        fun init() {
            viewModelScope.launchIO {
                Timber.d("init HomeViewModel")
                serverRepository.currentUserDto?.let { userDto ->
                    if (dataLoadingJob == null || dataLoadingJob?.isActive == false) {
                        val remoteConfig = remoteConfigService.config.value
                        dataLoadingJob =
                            viewModelScope.launchIO {
                                loadStremioHome(userDto, remoteConfig)
                            }
                    }
                }
            }
        }

        /**
         * Reload everything, eg from the error page
         */
        fun retry() {
            serverRepository.currentUserDto?.let { userDto ->
                _state.update { it.copy(loadingState = LoadingState.Loading) }
                reloadHome(userDto, cachedConfig, forceCatalogs = true)
            }
        }

        fun setFilter(filter: HomeMediaFilter) {
            if (_state.value.selectedFilter == filter) return
            val rows =
                buildVisibleHomeRows(
                    continueRow = cachedContinueWatching,
                    catalogByKey = cachedCatalogByKey,
                    config = cachedConfig,
                    filter = filter,
                )
            _state.update {
                it.copy(
                    selectedFilter = filter,
                    homeRows = rows,
                )
            }
        }

        private fun catalogsAreFresh(): Boolean =
            cachedCatalogByKey.isNotEmpty() &&
                System.currentTimeMillis() - catalogsLoadedAtMs < CATALOG_TTL_MS

        /**
         * Loads the home page. The platform catalogs change rarely (Gelato/Top 10 collections), so while they are
         * fresh only the Continue Watching / Next Up row is refreshed, eg when coming back from playback.
         */
        private suspend fun loadStremioHome(
            userDto: ServerUserConfig,
            remoteConfig: RemoteWholphinConfig,
            forceCatalogs: Boolean = false,
        ) {
            Timber.i("Starting loadStremioHome revision=%s, forceCatalogs=%s", remoteConfig.configRevision, forceCatalogs)
            try {
                val refresh =
                    state.value.loadingState == LoadingState.Success &&
                        state.value.userId == userDto.id
                if (refresh && !forceCatalogs && catalogsAreFresh()) {
                    refreshContinueWatching(userDto, remoteConfig)
                    return
                }
                _state.update {
                    it.copy(
                        userId = userDto.id,
                        loadingState = if (refresh) LoadingState.Success else LoadingState.Loading,
                        refreshState = LoadingState.Loading,
                        heroConfig = remoteConfig.home.hero,
                        platforms =
                            remoteConfig.home.platforms
                                .filter { platform -> !platform.isHome }
                                .map { platform ->
                                    val coverUuid =
                                        listOfNotNull(
                                            platform.movieCollectionId,
                                            platform.seriesCollectionId,
                                        ).firstNotNullOfOrNull { it.toUUIDOrNull() }
                                    val coverUrl =
                                        coverUuid?.let { id ->
                                            imageUrlService.getItemImageUrl(
                                                itemId = id,
                                                imageType = ImageType.THUMB,
                                                fillWidth = 480,
                                                fillHeight = 270,
                                            )
                                        }
                                    PlatformTile(
                                        key = platform.key,
                                        name = platform.name,
                                        movieCollectionId = platform.movieCollectionId,
                                        seriesCollectionId = platform.seriesCollectionId,
                                        isHome = platform.isHome,
                                        coverUrl = coverUrl,
                                    )
                                },
                    )
                }

                val maxItems = remoteConfig.home.maxItemsPerRow.coerceAtLeast(1)
                val heroSourceKey = remoteConfig.home.hero.sourceKey
                val (immediatePlatforms, deferredPlatforms) =
                    remoteConfig.home.platforms.partition { platform ->
                        platform.isHome || platform.key == heroSourceKey
                    }

                coroutineScope {
                    val continueJob = async { fetchContinueWatching(userDto, remoteConfig, refresh) }
                    val immediate =
                        immediatePlatforms.map { platform ->
                            async {
                                platform.key to fetchPlatformCatalog(userDto.id, platform, maxItems, cachedCatalogByKey[platform.key])
                            }
                        }
                    continueJob.await().onSuccess { cachedContinueWatching = it }
                    val immediateCatalogs = immediate.awaitAll().toMap()
                    catalogMutex.withLock {
                        // Merge, so platforms not reloaded yet keep their rows instead of disappearing
                        cachedCatalogByKey = cachedCatalogByKey + immediateCatalogs
                        publishRowsLocked(remoteConfig)
                        _state.update { it.copy(heroSlides = buildHeroSlidesFromConfig(remoteConfig, cachedCatalogByKey)) }
                    }

                    // Each platform row is shown as soon as it's loaded instead of waiting for the slowest
                    val semaphore = Semaphore(2)
                    deferredPlatforms
                        .map { platform ->
                            async {
                                semaphore.withPermit {
                                    val catalog =
                                        fetchPlatformCatalog(userDto.id, platform, maxItems, cachedCatalogByKey[platform.key])
                                    catalogMutex.withLock {
                                        cachedCatalogByKey = cachedCatalogByKey + (platform.key to catalog)
                                        publishRowsLocked(remoteConfig)
                                    }
                                }
                            }
                        }.awaitAll()
                }
                catalogsLoadedAtMs = System.currentTimeMillis()
                _state.update { it.copy(refreshState = LoadingState.Success) }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.e(ex, "Exception during Stremio home load")
                if (state.value.loadingState == LoadingState.Success) {
                    showToast(context, context.getString(R.string.home_refresh_error))
                    _state.update { it.copy(refreshState = LoadingState.Error(ex)) }
                } else {
                    _state.update { it.copy(loadingState = LoadingState.Error(ex)) }
                }
            }
        }

        /**
         * Rebuilds the visible rows from the cached data. Must hold [catalogMutex].
         */
        private fun publishRowsLocked(config: RemoteWholphinConfig) {
            val rows =
                buildVisibleHomeRows(
                    continueRow = cachedContinueWatching,
                    catalogByKey = cachedCatalogByKey,
                    config = config,
                    filter = _state.value.selectedFilter,
                )
            _state.update { it.copy(loadingState = LoadingState.Success, homeRows = rows) }
        }

        private suspend fun refreshContinueWatching(
            userDto: ServerUserConfig,
            config: RemoteWholphinConfig,
        ) {
            _state.update { it.copy(refreshState = LoadingState.Loading) }
            fetchContinueWatching(userDto, config, isRefresh = true).onSuccess { cachedContinueWatching = it }
            catalogMutex.withLock { publishRowsLocked(config) }
            _state.update { it.copy(refreshState = LoadingState.Success) }
        }

        /**
         * @return the row (null if disabled or empty), or a failure to keep showing the previous row
         */
        private suspend fun fetchContinueWatching(
            userDto: ServerUserConfig,
            config: RemoteWholphinConfig,
            isRefresh: Boolean,
        ): Result<HomeRowLoadingState.Success?> {
            if (!config.home.includeNativeResumeNextUp) return Result.success(null)
            return try {
                val preferences = userPreferencesService.getCurrent()
                val libraries = navDrawerService.getAllUserLibraries(userDto.id, userDto.tvAccess)
                val rowConfig =
                    HomeRowConfig.ContinueWatchingCombined(
                        viewOptions =
                            // Real 16:9 artwork (thumb, else backdrop) instead of a poster stretched to 16:9
                            HomeRowViewOptions(
                                showTitles = true,
                                aspectRatio = AspectRatio.WIDE,
                                episodeAspectRatio = AspectRatio.WIDE,
                                heightDp = NigiroDimens.WideCardHeight.value.toInt(),
                                spacing = NigiroDimens.CardGap.value.toInt(),
                                imageType = ViewOptionImageType.THUMB,
                                episodeImageType = ViewOptionImageType.THUMB,
                                contentScale = PrefContentScale.CROP,
                                episodeContentScale = PrefContentScale.CROP,
                            ),
                    )
                val result =
                    homeDataService.fetchDataForRow(
                        row = rowConfig,
                        scope = viewModelScope,
                        prefs = preferences.appPreferences.homePagePreferences,
                        userDto = userDto,
                        libraries = libraries,
                        limit = config.home.maxItemsPerRow.coerceAtLeast(1),
                        isRefresh = isRefresh,
                    )
                when (result) {
                    is HomeRowLoadingState.Success -> Result.success(result)
                    is HomeRowLoadingState.Error -> Result.failure(result.exception ?: Exception(result.message))
                    else -> Result.success(null)
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.w(ex, "Failed loading Continue Watching / Next Up")
                Result.failure(ex)
            }
        }

        private suspend fun fetchPlatformCatalog(
            userId: UUID,
            platform: RemotePlatformConfig,
            limit: Int,
            previous: CachedPlatformCatalog?,
        ): CachedPlatformCatalog =
            coroutineScope {
                val movies = async { fetchBoxSetItems(userId, platform.movieCollectionId, limit) }
                val series = async { fetchBoxSetItems(userId, platform.seriesCollectionId, limit) }
                // On error keep what was shown before rather than removing the row
                CachedPlatformCatalog(
                    movies = movies.await() ?: previous?.movies.orEmpty(),
                    series = series.await() ?: previous?.series.orEmpty(),
                )
            }

        /**
         * @return the items, or null if they could not be fetched
         */
        private suspend fun fetchBoxSetItems(
            userId: UUID,
            collectionId: String?,
            limit: Int,
        ): List<BaseItem>? {
            val uuid =
                collectionId
                    ?.takeIf { it.isNotBlank() }
                    ?.toUUIDOrNull() ?: return emptyList()
            return try {
                val request =
                    GetItemsRequest(
                        userId = userId,
                        parentId = uuid,
                        limit = limit,
                        fields = HomeItemFields,
                        enableTotalRecordCount = false,
                    )
                GetItemsRequestHandler
                    .execute(api, request)
                    .content.items
                    .map { BaseItem(it, false) }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: InvalidStatusException) {
                Timber.w(ex, "Status %s fetching collection %s", ex.status, collectionId)
                null
            } catch (ex: Exception) {
                Timber.w(ex, "Error fetching collection %s", collectionId)
                null
            }
        }

        private fun buildVisibleHomeRows(
            continueRow: HomeRowLoadingState.Success?,
            catalogByKey: Map<String, CachedPlatformCatalog>,
            config: RemoteWholphinConfig,
            filter: HomeMediaFilter,
        ): List<HomeRowLoadingState> {
            val rows = mutableListOf<HomeRowLoadingState>()
            if (continueRow != null && continueRow.items.isNotEmpty()) {
                rows.add(continueRow)
            }
            val posterOptions =
                // Poster only: title & details show in the header for the focused item
                HomeRowViewOptions(
                    heightDp = NigiroDimens.PosterHeight.value.toInt(),
                    spacing = NigiroDimens.CardGap.value.toInt(),
                    contentScale = PrefContentScale.CROP,
                    aspectRatio = AspectRatio.TALL,
                    showTitles = false,
                    useSeries = false,
                )
            val maxItems = config.home.maxItemsPerRow.coerceAtLeast(1)
            val specs = buildStremioHomeRowSpecs(config.home.platforms, filter)
            for (spec in specs) {
                val items = selectPlatformRowItems(catalogByKey[spec.key], filter, maxItems)
                if (items.isNotEmpty()) {
                    val targetCollectionId =
                        when (filter) {
                            HomeMediaFilter.MOVIE -> spec.movieCollectionId
                            HomeMediaFilter.SERIES -> spec.seriesCollectionId
                            HomeMediaFilter.ALL ->
                                if (spec.movieCollectionId != null && spec.seriesCollectionId == null) {
                                    spec.movieCollectionId
                                } else if (spec.seriesCollectionId != null && spec.movieCollectionId == null) {
                                    spec.seriesCollectionId
                                } else {
                                    null
                                }
                        }
                    val rowConfig =
                        targetCollectionId?.toUUIDOrNull()?.let { uuid ->
                            HomeRowConfig.ByParent(
                                parentId = uuid,
                                recursive = false,
                                viewOptions = posterOptions,
                            )
                        }
                    rows.add(
                        HomeRowLoadingState.Success(
                            title = StringStringProvider(spec.title),
                            items = items,
                            viewOptions = posterOptions,
                            rowType = rowConfig,
                            showViewMore = rowConfig != null && items.size >= maxItems,
                        ),
                    )
                }
            }
            return rows
        }

        private fun buildHeroSlidesFromConfig(
            config: RemoteWholphinConfig,
            catalogByKey: Map<String, CachedPlatformCatalog>,
        ): List<HeroSlide> {
            if (!config.home.hero.enabled) return emptyList()
            val heroPlatform =
                config.home.platforms.firstOrNull { it.key == config.home.hero.sourceKey }
                    ?: config.home.platforms.firstOrNull { it.isHome }
                    ?: return emptyList()
            val catalog = catalogByKey[heroPlatform.key] ?: return emptyList()
            val pool =
                interleavePlatformItems(
                    movies = catalog.movies,
                    series = catalog.series,
                    maxItems = config.home.hero.count.coerceAtLeast(1),
                )
            val badgeLabel = context.getString(R.string.hero_top10_badge)
            val movieLabel = context.getString(R.string.home_filter_movies)
            val seriesLabel = context.getString(R.string.home_filter_series)
            return buildHeroSlides(
                items = pool,
                count = config.home.hero.count,
                badgeLabel = badgeLabel,
                backdropUrlFor = { item ->
                    imageUrlService.getItemImageUrl(
                        item = item,
                        imageType = ImageType.BACKDROP,
                        fillWidth = 1280,
                        fillHeight = null,
                        useSeriesForPrimary = false,
                    ) ?: imageUrlService.getItemImageUrl(
                        item = item,
                        imageType = ImageType.PRIMARY,
                        fillWidth = 960,
                        fillHeight = null,
                        useSeriesForPrimary = false,
                    )
                },
                typeLabelFor = { item ->
                    when (item.type) {
                        BaseItemKind.SERIES -> seriesLabel
                        BaseItemKind.MOVIE -> movieLabel
                        else -> null
                    }
                },
            )
        }

        fun setWatched(
            itemId: UUID,
            played: Boolean,
        ) = viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
            favoriteWatchManager.setWatched(itemId, played)
            updateItemInPlace(itemId) { it.copy(played = played) }
            // Watched status changes what is in Continue Watching / Next Up
            serverRepository.currentUserDto?.let { refreshContinueWatching(it, cachedConfig) }
        }

        private fun reloadHome(
            userDto: ServerUserConfig,
            config: RemoteWholphinConfig,
            forceCatalogs: Boolean = false,
        ) {
            dataLoadingJob?.cancel()
            dataLoadingJob =
                viewModelScope.launchIO {
                    loadStremioHome(userDto, config, forceCatalogs)
                }
        }

        fun setFavorite(
            itemId: UUID,
            favorite: Boolean,
        ) = viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
            favoriteWatchManager.setFavorite(itemId, favorite)
            updateItemInPlace(itemId) { it.copy(isFavorite = favorite) }
        }

        private fun BaseItem.withUserData(block: (UserItemDataDto) -> UserItemDataDto): BaseItem {
            val current =
                data.userData ?: UserItemDataDto(
                    playbackPositionTicks = 0L,
                    playCount = 0,
                    isFavorite = false,
                    lastPlayedDate = null,
                    played = false,
                    key = id.toString(),
                    itemId = id,
                )
            return copy(data = data.copy(userData = block(current)))
        }

        /**
         * Updates an item everywhere it is shown without reloading the whole page
         */
        private suspend fun updateItemInPlace(
            itemId: UUID,
            block: (UserItemDataDto) -> UserItemDataDto,
        ) {
            catalogMutex.withLock {
                fun List<BaseItem>.updated() = map { if (it.id == itemId) it.withUserData(block) else it }
                cachedCatalogByKey =
                    cachedCatalogByKey.mapValues { (_, catalog) ->
                        catalog.copy(movies = catalog.movies.updated(), series = catalog.series.updated())
                    }
                cachedContinueWatching =
                    cachedContinueWatching?.let { row ->
                        row.copy(items = row.items.map { if (it?.id == itemId) it.withUserData(block) else it })
                    }
                _state.update { current ->
                    current.copy(
                        heroSlides =
                            current.heroSlides.map { slide ->
                                if (slide.item.id == itemId) {
                                    val item = slide.item.withUserData(block)
                                    slide.copy(item = item, isFavorite = item.data.userData?.isFavorite == true)
                                } else {
                                    slide
                                }
                            },
                    )
                }
                publishRowsLocked(cachedConfig)
            }
        }

        fun updateBackdrop(item: BaseItem) {
            viewModelScope.launchIO {
                backdropService.submit(item)
            }
        }

        fun deleteItem(
            position: RowColumn,
            item: BaseItem,
        ) {
            deleteItem(context, mediaManagementService, item) {
                viewModelScope.launchDefault {
                    val row = state.value.homeRows.getOrNull(position.row)
                    if (row is HomeRowLoadingState.Success) {
                        _state.update {
                            val newRow =
                                row.items.toMutableList().apply {
                                    removeAt(position.column)
                                }
                            it.copy(
                                homeRows =
                                    it.homeRows.toMutableList().apply {
                                        set(position.row, row.copy(items = newRow))
                                    },
                            )
                        }
                    }
                }
            }
        }

        fun canDelete(
            item: BaseItem,
            appPreferences: AppPreferences,
        ): Boolean = mediaManagementService.canDelete(item, appPreferences)

        fun removeFromNextUp(item: BaseItem) {
            if (item.type == BaseItemKind.EPISODE) {
                viewModelScope.launchDefault {
                    serverRepository.currentUser?.id?.let { userId ->
                        latestNextUpService.removeFromNextUp(userId, item)
                        serverRepository.currentUserDto?.let { userDto ->
                            refreshContinueWatching(userDto, cachedConfig)
                        }
                    }
                }
            } else {
                Timber.w("Item is not an episode %s", item.id)
            }
        }

        companion object {
            /**
             * How long the platform catalogs are reused before reloading them
             */
            const val CATALOG_TTL_MS = 20 * 60 * 1000L
        }
    }

data class HomeState(
    val userId: UUID?,
    val loadingState: LoadingState,
    val refreshState: LoadingState,
    val homeRows: List<HomeRowLoadingState>,
    val heroSlides: List<HeroSlide>,
    val heroConfig: RemoteHeroConfig,
    val platforms: List<PlatformTile>,
    val selectedFilter: HomeMediaFilter,
) {
    companion object {
        val EMPTY =
            HomeState(
                userId = null,
                loadingState = LoadingState.Pending,
                refreshState = LoadingState.Pending,
                homeRows = emptyList(),
                heroSlides = emptyList(),
                heroConfig = RemoteHeroConfig(),
                platforms = emptyList(),
                selectedFilter = HomeMediaFilter.ALL,
            )
    }
}
