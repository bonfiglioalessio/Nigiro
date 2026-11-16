@file:UseSerializers(UUIDSerializer::class)

package com.github.damontecres.wholphin.ui.explore

import androidx.annotation.StringRes
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.SlimItemFields
import com.github.damontecres.wholphin.ui.main.HomeMediaFilter
import com.github.damontecres.wholphin.util.GetItemsRequestHandler
import com.github.damontecres.wholphin.util.PlatformItemsRequest
import com.github.damontecres.wholphin.util.PlatformItemsRequestHandler
import com.github.damontecres.wholphin.util.RequestHandler
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.Locale
import java.util.UUID

/**
 * A curated genre folder of the Explore hub. It groups the server's genre names that mean the same thing: Italian
 * TMDB names, English leftovers and the combined TV genres (eg "Action & Adventure").
 */
data class ExploreGenre(
    val key: String,
    @param:StringRes val label: Int,
    val serverNames: List<String>,
)

val ExploreGenres =
    listOf(
        ExploreGenre("action", R.string.genre_action, listOf("Azione", "Action", "Action & Adventure")),
        ExploreGenre("comedy", R.string.genre_comedy, listOf("Commedia", "Comedy")),
        ExploreGenre("drama", R.string.genre_drama, listOf("Dramma", "Drama")),
        ExploreGenre("thriller", R.string.genre_thriller, listOf("Thriller")),
        ExploreGenre("crime", R.string.genre_crime, listOf("Crime")),
        ExploreGenre("horror", R.string.genre_horror, listOf("Horror")),
        ExploreGenre("scifi", R.string.genre_scifi, listOf("Fantascienza", "Science Fiction", "Sci-Fi & Fantasy")),
        ExploreGenre("fantasy", R.string.genre_fantasy, listOf("Fantasy", "Sci-Fi & Fantasy")),
        ExploreGenre("adventure", R.string.genre_adventure, listOf("Avventura", "Adventure", "Action & Adventure")),
        ExploreGenre("mystery", R.string.genre_mystery, listOf("Mistero", "Mystery")),
        ExploreGenre("romance", R.string.genre_romance, listOf("Romance")),
        ExploreGenre("family", R.string.genre_family, listOf("Famiglia", "Family", "Kids")),
        ExploreGenre("animation", R.string.genre_animation, listOf("Animazione", "Animation")),
        ExploreGenre("documentary", R.string.genre_documentary, listOf("Documentario", "Documentary")),
        ExploreGenre("history", R.string.genre_history, listOf("Storia", "History")),
        ExploreGenre("war", R.string.genre_war, listOf("Guerra", "War", "War & Politics")),
        ExploreGenre("reality", R.string.genre_reality, listOf("Reality", "Talk")),
        ExploreGenre("music", R.string.genre_music, listOf("Musica", "Music")),
        ExploreGenre("western", R.string.genre_western, listOf("Western")),
    )

/** Genre ids on the server for the folder, matching names case-insensitively; empty when none exists. */
internal fun ExploreGenre.serverIds(idsByName: Map<String, UUID>): List<UUID> =
    serverNames.mapNotNull { idsByName[it.genreKey()] }.distinct()

internal fun String.genreKey(): String = trim().lowercase(Locale.ROOT)

internal fun HomeMediaFilter.itemKinds(): List<BaseItemKind> =
    when (this) {
        HomeMediaFilter.ALL -> listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
        HomeMediaFilter.MOVIE -> listOf(BaseItemKind.MOVIE)
        HomeMediaFilter.SERIES -> listOf(BaseItemKind.SERIES)
    }

/** One genre tile: how many titles it has for the current filter and a backdrop from one of them. */
data class ExploreGenreTile(
    val genre: ExploreGenre,
    val genreIds: List<UUID>,
    val count: Int,
    val imageUrl: String?,
)

/**
 * What an Explore tile opens: a platform (its movie and series collections) or a genre folder. Kept in the title
 * grid's destination so the grid can switch between all, movies and series itself.
 */
@Serializable
data class ExploreGridSource(
    val movieCollectionId: UUID? = null,
    val seriesCollectionId: UUID? = null,
    val genreIds: List<UUID> = emptyList(),
    val filter: HomeMediaFilter = HomeMediaFilter.ALL,
    // The user's favourites instead of a platform or genre
    val favorites: Boolean = false,
) {
    val isPlatform: Boolean get() = !favorites && genreIds.isEmpty()

    /** Catalog order only makes sense for a platform, whose collections keep the ranking */
    fun sortOptions(): List<ExploreSort> = if (isPlatform) ExploreSort.entries else ExploreSort.entries - ExploreSort.CATALOG

    fun defaultSort(): ExploreSort =
        when {
            favorites -> ExploreSort.DATE_ADDED
            isPlatform -> ExploreSort.CATALOG
            else -> ExploreSort.RELEASE
        }
}

/** Sort choices of the Explore and Favorites grids (AIOStreams: added, name, release, rating) */
@Serializable
enum class ExploreSort(
    @param:StringRes val label: Int,
) {
    CATALOG(R.string.explore_sort_catalog),
    DATE_ADDED(R.string.explore_sort_date_added),
    NAME(R.string.explore_sort_name),
    RELEASE(R.string.explore_sort_release),
    RATING(R.string.explore_sort_rating),
}

internal fun ExploreSort.itemSortBy(): List<ItemSortBy> =
    when (this) {
        ExploreSort.CATALOG, ExploreSort.RELEASE -> listOf(ItemSortBy.PREMIERE_DATE, ItemSortBy.SORT_NAME)
        ExploreSort.DATE_ADDED -> listOf(ItemSortBy.DATE_CREATED, ItemSortBy.SORT_NAME)
        ExploreSort.NAME -> listOf(ItemSortBy.SORT_NAME)
        ExploreSort.RATING -> listOf(ItemSortBy.COMMUNITY_RATING, ItemSortBy.SORT_NAME)
    }

// Newest, highest first; names A to Z
internal fun ExploreSort.itemSortOrder(): List<SortOrder> =
    if (this == ExploreSort.NAME) listOf(SortOrder.ASCENDING) else listOf(SortOrder.DESCENDING, SortOrder.ASCENDING)

/** The grid request for [filter]: a platform keeps only the matching collection, a genre the matching kinds. */
internal fun ExploreGridSource.request(
    userId: UUID?,
    filter: HomeMediaFilter,
    sort: ExploreSort = defaultSort(),
): Pair<Any, RequestHandler<*>> =
    if (favorites) {
        GetItemsRequest(
            userId = userId,
            recursive = true,
            isFavorite = true,
            includeItemTypes = filter.itemKinds(),
            sortBy = sort.itemSortBy(),
            sortOrder = sort.itemSortOrder(),
            fields = SlimItemFields,
        ) to GetItemsRequestHandler
    } else if (genreIds.isEmpty()) {
        PlatformItemsRequest(
            userId = userId,
            movieCollectionId = movieCollectionId.takeIf { filter != HomeMediaFilter.SERIES },
            seriesCollectionId = seriesCollectionId.takeIf { filter != HomeMediaFilter.MOVIE },
            sort = sort,
        ) to PlatformItemsRequestHandler
    } else {
        GetItemsRequest(
            userId = userId,
            recursive = true,
            includeItemTypes = filter.itemKinds(),
            genreIds = genreIds,
            sortBy = sort.itemSortBy(),
            sortOrder = sort.itemSortOrder(),
            fields = SlimItemFields,
        ) to GetItemsRequestHandler
    }
