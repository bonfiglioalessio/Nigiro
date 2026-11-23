package com.github.damontecres.wholphin.ui.main

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.RemotePlatformConfig
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.Locale
import java.util.UUID

@kotlinx.serialization.Serializable
enum class HomeMediaFilter(
    val wireValue: String,
) {
    ALL("all"),
    MOVIE("movie"),
    SERIES("series"),
}

data class HeroSlide(
    val item: BaseItem,
    val title: String,
    val badgeText: String,
    val yearText: String?,
    val ratingText: String?,
    val typeLabel: String?,
    val overview: String?,
    val backdropUrl: String?,
    val isFavorite: Boolean,
    val logoUrl: String? = null,
)

data class PlatformTile(
    val key: String,
    val name: String,
    val movieCollectionId: String?,
    val seriesCollectionId: String?,
    val isHome: Boolean,
    val coverUrl: String? = null,
)

data class StremioRowSpec(
    val key: String,
    val title: String,
    val movieCollectionId: String?,
    val seriesCollectionId: String?,
    val isHome: Boolean,
)

data class CachedPlatformCatalog(
    val movies: List<BaseItem> = emptyList(),
    val series: List<BaseItem> = emptyList(),
)

internal fun parseHomeMediaFilter(raw: String?): HomeMediaFilter =
    when (raw?.trim()?.lowercase(Locale.US)) {
        "movie", "movies", "film" -> HomeMediaFilter.MOVIE
        "series", "tv", "tvshows", "serie" -> HomeMediaFilter.SERIES
        else -> HomeMediaFilter.ALL
    }

internal fun resolvePlatformTargetCollectionId(
    platform: PlatformTile,
    filter: HomeMediaFilter,
): String? =
    when (filter) {
        HomeMediaFilter.MOVIE -> platform.movieCollectionId ?: platform.seriesCollectionId
        HomeMediaFilter.SERIES -> platform.seriesCollectionId ?: platform.movieCollectionId
        HomeMediaFilter.ALL -> {
            val hasMovie = !platform.movieCollectionId.isNullOrBlank()
            val hasSeries = !platform.seriesCollectionId.isNullOrBlank()
            when {
                hasMovie && !hasSeries -> platform.movieCollectionId
                !hasMovie && hasSeries -> platform.seriesCollectionId
                else -> null
            }
        }
    }

internal fun needsPlatformTypeChoice(
    platform: PlatformTile,
    filter: HomeMediaFilter,
): Boolean =
    filter == HomeMediaFilter.ALL &&
        !platform.movieCollectionId.isNullOrBlank() &&
        !platform.seriesCollectionId.isNullOrBlank()

internal fun interleavePlatformItems(
    movies: List<BaseItem>,
    series: List<BaseItem>,
    maxItems: Int,
): List<BaseItem> {
    if (maxItems <= 0) return emptyList()
    val result = ArrayList<BaseItem>(minOf(maxItems, movies.size + series.size))
    val seenIds = HashSet<UUID>()
    val limit = maxOf(movies.size, series.size)
    for (i in 0 until limit) {
        if (result.size >= maxItems) break
        movies.getOrNull(i)?.let { movie ->
            if (seenIds.add(movie.id)) {
                result.add(movie)
            }
        }
        if (result.size >= maxItems) break
        series.getOrNull(i)?.let { show ->
            if (seenIds.add(show.id)) {
                result.add(show)
            }
        }
    }
    return result
}

internal const val TOP_10_LIMIT = 10

internal fun maxItemsForRow(
    spec: StremioRowSpec,
    maxItemsPerRow: Int,
): Int = if (spec.isHome) minOf(maxItemsPerRow, TOP_10_LIMIT) else maxItemsPerRow

internal fun selectPlatformRowItems(
    cached: CachedPlatformCatalog?,
    filter: HomeMediaFilter,
    maxItems: Int,
): List<BaseItem> {
    if (cached == null) return emptyList()
    return when (filter) {
        HomeMediaFilter.ALL -> interleavePlatformItems(cached.movies, cached.series, maxItems)
        HomeMediaFilter.MOVIE -> cached.movies.take(maxItems)
        HomeMediaFilter.SERIES -> cached.series.take(maxItems)
    }
}

internal fun buildStremioHomeRowSpecs(
    platforms: List<RemotePlatformConfig>,
    filter: HomeMediaFilter,
): List<StremioRowSpec> {
    val ordered =
        platforms.sortedByDescending { it.isHome }
    return ordered.mapNotNull { platform ->
        val movieId = platform.movieCollectionId?.takeIf { it.isNotBlank() }
        val seriesId = platform.seriesCollectionId?.takeIf { it.isNotBlank() }
        val hasRelevantCollection =
            when (filter) {
                HomeMediaFilter.ALL -> movieId != null || seriesId != null
                HomeMediaFilter.MOVIE -> movieId != null
                HomeMediaFilter.SERIES -> seriesId != null
            }
        if (!hasRelevantCollection) {
            null
        } else {
            StremioRowSpec(
                key = platform.key,
                title = platform.name,
                movieCollectionId = movieId,
                seriesCollectionId = seriesId,
                isHome = platform.isHome,
            )
        }
    }
}

internal fun buildHeroSlides(
    items: List<BaseItem>,
    count: Int,
    badgeLabel: String,
    backdropUrlFor: (BaseItem) -> String?,
    typeLabelFor: (BaseItem) -> String?,
    logoUrlFor: (BaseItem) -> String? = { null },
): List<HeroSlide> {
    val limit = count.coerceAtLeast(1)
    return items.take(limit).mapIndexed { index, item ->
        val rating =
            item.data.communityRating
                ?.takeIf { it > 0f }
                ?.let { String.format(Locale.US, "%.1f", it) }
        HeroSlide(
            item = item,
            title = item.title ?: item.name ?: "",
            badgeText = "#${index + 1} $badgeLabel",
            yearText = item.data.productionYear?.toString(),
            ratingText = rating,
            typeLabel = typeLabelFor(item),
            overview = item.data.overview,
            backdropUrl = backdropUrlFor(item),
            isFavorite = item.favorite,
            logoUrl = logoUrlFor(item),
        )
    }
}

private const val HERO_MIN_PREFERRED_RATING = 5.5f

private fun BaseItem.heroTitleKey(): String? =
    (title ?: name)?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }

private fun heroCandidateTier(item: BaseItem): Int {
    val hasBackdrop = !item.data.backdropImageTags.isNullOrEmpty()
    val hasOverview = !item.data.overview.isNullOrBlank()
    val unwatched = item.data.userData?.played != true
    val rating = item.data.communityRating?.takeIf { it > 0f }
    val goodRating = rating == null || rating >= HERO_MIN_PREFERRED_RATING
    return when {
        hasBackdrop && unwatched && hasOverview && goodRating -> 0
        hasBackdrop && unwatched -> 1
        hasBackdrop -> 2
        else -> 3
    }
}

/**
 * Picks up to [count] recent movies and series for the hero banner, excluding titles already in [excludedItems]
 * (eg Top 10 Italia) and prioritizing unwatched items with backdrop, overview and solid ratings.
 */
internal fun selectRecentHeroItems(
    recentMovies: List<BaseItem>,
    recentSeries: List<BaseItem>,
    excludedItems: List<BaseItem>,
    filter: HomeMediaFilter,
    count: Int,
): List<BaseItem> {
    if (count <= 0) return emptyList()
    val excludedIds = excludedItems.mapTo(HashSet()) { it.id }
    val excludedKeys =
        excludedItems.mapNotNullTo(HashSet()) { item ->
            item.heroTitleKey()?.let { item.type to it }
        }

    fun prepare(items: List<BaseItem>): List<BaseItem> {
        val seenKeys = HashSet<Any>()
        return items
            .asSequence()
            .filter { item ->
                item.id !in excludedIds &&
                    item.heroTitleKey()?.let { (item.type to it) in excludedKeys } != true
            }.sortedBy { heroCandidateTier(it) }
            .filter { item ->
                val dedupeKey: Any = item.heroTitleKey()?.let { item.type to it } ?: item.id
                seenKeys.add(dedupeKey)
            }.toList()
    }

    val preparedMovies = if (filter == HomeMediaFilter.SERIES) emptyList() else prepare(recentMovies)
    val preparedSeries = if (filter == HomeMediaFilter.MOVIE) emptyList() else prepare(recentSeries)
    return when (filter) {
        HomeMediaFilter.ALL -> interleavePlatformItems(preparedMovies, preparedSeries, count)
        HomeMediaFilter.MOVIE -> preparedMovies.take(count)
        HomeMediaFilter.SERIES -> preparedSeries.take(count)
    }
}
