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
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.HomeItemFields
import com.github.damontecres.wholphin.ui.collectLatestIn
import com.github.damontecres.wholphin.ui.combinePair
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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
                            loadStremioHome(userDto, remoteConfig)
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

        private suspend fun loadStremioHome(
            userDto: ServerUserConfig,
            remoteConfig: RemoteWholphinConfig,
        ) {
            Timber.i("Starting loadStremioHome revision=%s", remoteConfig.configRevision)
            try {
                val preferences = userPreferencesService.getCurrent()
                val homePrefs = preferences.appPreferences.homePagePreferences
                val libraries = navDrawerService.getAllUserLibraries(userDto.id, userDto.tvAccess)

                val refresh =
                    state.value.loadingState == LoadingState.Success &&
                        state.value.userId == userDto.id
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
                                    PlatformTile(
                                        key = platform.key,
                                        name = platform.name,
                                        movieCollectionId = platform.movieCollectionId,
                                        seriesCollectionId = platform.seriesCollectionId,
                                        isHome = platform.isHome,
                                    )
                                },
                    )
                }

                val maxItems = remoteConfig.home.maxItemsPerRow.coerceAtLeast(1)

                val continueJob =
                    viewModelScope.async(WholphinDispatchers.IO) {
                        if (!remoteConfig.home.includeNativeResumeNextUp) {
                            null
                        } else {
                            try {
                                val rowConfig =
                                    HomeRowConfig.ContinueWatchingCombined(
                                        viewOptions =
                                            HomeRowViewOptions(
                                                showTitles = true,
                                                aspectRatio = AspectRatio.WIDE,
                                                episodeAspectRatio = AspectRatio.WIDE,
                                                heightDp = Cards.HEIGHT_EPISODE,
                                            ),
                                    )
                                val result =
                                    homeDataService.fetchDataForRow(
                                        row = rowConfig,
                                        scope = viewModelScope,
                                        prefs = homePrefs,
                                        userDto = userDto,
                                        libraries = libraries,
                                        limit = maxItems,
                                        isRefresh = refresh,
                                    )
                                result as? HomeRowLoadingState.Success
                            } catch (ex: Exception) {
                                Timber.w(ex, "Failed loading Continue Watching / Next Up")
                                null
                            }
                        }
                    }

                val heroSourceKey = remoteConfig.home.hero.sourceKey
                val (immediatePlatforms, deferredPlatforms) =
                    remoteConfig.home.platforms.partition { platform ->
                        platform.isHome || platform.key == heroSourceKey
                    }

                val immediateSemaphore = Semaphore(4)
                val immediateDeferred =
                    immediatePlatforms.map { platform ->
                        viewModelScope.async(WholphinDispatchers.IO) {
                            immediateSemaphore.withPermit {
                                platform.key to fetchPlatformCatalog(userDto.id, platform, maxItems)
                            }
                        }
                    }

                cachedContinueWatching = continueJob.await()
                cachedCatalogByKey = immediateDeferred.awaitAll().toMap()
                cachedConfig = remoteConfig

                val activeFilter = _state.value.selectedFilter
                val rows =
                    buildVisibleHomeRows(
                        continueRow = cachedContinueWatching,
                        catalogByKey = cachedCatalogByKey,
                        config = remoteConfig,
                        filter = activeFilter,
                    )
                val heroSlides = buildHeroSlidesFromConfig(remoteConfig, cachedCatalogByKey)

                _state.update {
                    it.copy(
                        loadingState = LoadingState.Success,
                        refreshState =
                            if (deferredPlatforms.isEmpty()) {
                                LoadingState.Success
                            } else {
                                LoadingState.Loading
                            },
                        homeRows = rows,
                        heroSlides = heroSlides,
                        heroConfig = remoteConfig.home.hero,
                    )
                }

                if (deferredPlatforms.isNotEmpty()) {
                    val backgroundSemaphore = Semaphore(2)
                    val deferredResults =
                        deferredPlatforms
                            .map { platform ->
                                viewModelScope.async(WholphinDispatchers.IO) {
                                    backgroundSemaphore.withPermit {
                                        platform.key to fetchPlatformCatalog(userDto.id, platform, maxItems)
                                    }
                                }
                            }.awaitAll()
                            .toMap()
                    cachedCatalogByKey = cachedCatalogByKey + deferredResults
                    val currentFilter = _state.value.selectedFilter
                    val updatedRows =
                        buildVisibleHomeRows(
                            continueRow = cachedContinueWatching,
                            catalogByKey = cachedCatalogByKey,
                            config = remoteConfig,
                            filter = currentFilter,
                        )
                    _state.update {
                        it.copy(
                            refreshState = LoadingState.Success,
                            homeRows = updatedRows,
                        )
                    }
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.e(ex, "Exception during Stremio home load")
                if (state.value.loadingState == LoadingState.Success) {
                    showToast(context, "Error refreshing home: ${ex.localizedMessage}")
                    _state.update { it.copy(refreshState = LoadingState.Error(ex)) }
                } else {
                    _state.update { it.copy(loadingState = LoadingState.Error(ex)) }
                }
            }
        }

        private suspend fun fetchPlatformCatalog(
            userId: UUID,
            platform: RemotePlatformConfig,
            limit: Int,
        ): CachedPlatformCatalog {
            val movies = fetchBoxSetItems(userId, platform.movieCollectionId, limit)
            val series = fetchBoxSetItems(userId, platform.seriesCollectionId, limit)
            return CachedPlatformCatalog(movies = movies, series = series)
        }

        private suspend fun fetchBoxSetItems(
            userId: UUID,
            collectionId: String?,
            limit: Int,
        ): List<BaseItem> {
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
                    )
                GetItemsRequestHandler
                    .execute(api, request)
                    .content.items
                    .map { BaseItem(it, false) }
            } catch (ex: InvalidStatusException) {
                Timber.w(ex, "Status %s fetching collection %s", ex.status, collectionId)
                emptyList()
            } catch (ex: Exception) {
                Timber.w(ex, "Error fetching collection %s", collectionId)
                emptyList()
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
                HomeRowViewOptions(
                    heightDp = Cards.HEIGHT_2X3_DP,
                    spacing = 16,
                    contentScale = PrefContentScale.FILL,
                    aspectRatio = AspectRatio.TALL,
                    showTitles = true,
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
            withContext(WholphinDispatchers.Main) {
                init()
            }
        }

        private fun reloadHome(
            userDto: ServerUserConfig,
            config: RemoteWholphinConfig,
        ) {
            dataLoadingJob?.cancel()
            dataLoadingJob =
                viewModelScope.launchIO {
                    loadStremioHome(userDto, config)
                }
        }

        fun setFavorite(
            itemId: UUID,
            favorite: Boolean,
        ) = viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
            favoriteWatchManager.setFavorite(itemId, favorite)
            _state.update { current ->
                current.copy(
                    heroSlides =
                        current.heroSlides.map { slide ->
                            if (slide.item.id == itemId) {
                                val updatedUserData =
                                    (
                                        slide.item.data.userData ?: UserItemDataDto(
                                            playbackPositionTicks = 0L,
                                            playCount = 0,
                                            isFavorite = favorite,
                                            lastPlayedDate = null,
                                            played = false,
                                            key = itemId.toString(),
                                            itemId = itemId,
                                        )
                                    ).copy(isFavorite = favorite)
                                slide.copy(
                                    item =
                                        slide.item.copy(
                                            data = slide.item.data.copy(userData = updatedUserData),
                                        ),
                                    isFavorite = favorite,
                                )
                            } else {
                                slide
                            }
                        },
                )
            }
            serverRepository.currentUserDto?.let { userDto ->
                withContext(WholphinDispatchers.Main) {
                    reloadHome(userDto, cachedConfig)
                }
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
                            withContext(WholphinDispatchers.Main) {
                                reloadHome(userDto, cachedConfig)
                            }
                        }
                    }
                }
            } else {
                Timber.w("Item is not an episode %s", item.id)
            }
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
