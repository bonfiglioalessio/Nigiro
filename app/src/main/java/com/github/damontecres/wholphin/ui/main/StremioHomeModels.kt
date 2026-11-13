package com.github.damontecres.wholphin.ui.main

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.RemotePlatformConfig
import java.util.Locale
import java.util.UUID

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
)

data class PlatformTile(
    val key: String,
    val name: String,
    val movieCollectionId: String?,
    val seriesCollectionId: String?,
    val isHome: Boolean,
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
        )
    }
}
