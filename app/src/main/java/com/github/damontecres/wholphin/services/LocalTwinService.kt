package com.github.damontecres.wholphin.services

import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.util.GetEpisodesRequestHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.request.GetEpisodesRequest
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the local copy of a streamed (Gelato) movie or episode.
 *
 * Something favorited in Gelato is requested via Seerr and downloaded by Radarr/Sonarr to the HDD, but Jellyfin
 * indexes the files as separate items in other libraries, so the Gelato item never shows them as versions. This
 * matches them by TMDB/TVDB/IMDb id (and season/episode number for episodes), so the local file can be played
 * instead of a debrid stream.
 */
@Singleton
class LocalTwinService
    @Inject
    constructor(
        private val api: ApiClient,
        private val serverRepository: ServerRepository,
    ) {
        private data class CacheEntry(
            val twinId: UUID?,
            val timeMs: Long,
        ) {
            val isFresh: Boolean get() = System.currentTimeMillis() - timeMs < CACHE_MS
        }

        // Streamed item id -> local item id
        private val itemCache = ConcurrentHashMap<UUID, CacheEntry>()

        // Streamed series id -> local series id
        private val seriesCache = ConcurrentHashMap<UUID, CacheEntry>()

        // Searches are rare: serializing them lets a concurrent lookup of the same item (eg a details page
        // initializing twice) wait for the first one and use its cached result instead of searching again
        private val searchMutex = Mutex()

        /**
         * Whether the item is a movie or episode with only streamed sources (eg a Gelato stub)
         */
        fun isStreamOnly(item: BaseItemDto): Boolean {
            if (item.type != BaseItemKind.MOVIE && item.type != BaseItemKind.EPISODE) return false
            if (item.path?.startsWith(GELATO_PREFIX) == true) return true
            val sources = item.mediaSources.orEmpty()
            return sources.isNotEmpty() && sources.all { it.isRemote || it.protocol == MediaProtocol.HTTP }
        }

        /**
         * Returns the full local item for a stream-only movie or episode, or null if there is none
         */
        suspend fun findLocalTwin(item: BaseItemDto): BaseItemDto? {
            if (!isStreamOnly(item)) return null
            val twinId =
                try {
                    searchMutex.withLock {
                        // A cached "no local copy" counts too, so it isn't searched again on every play
                        val cached = itemCache[item.id]?.takeIf { it.isFresh }
                        if (cached != null) {
                            cached.twinId
                        } else {
                            val found =
                                if (item.type == BaseItemKind.EPISODE) searchEpisodeTwin(item) else searchMovieTwin(item)
                            itemCache[item.id] = CacheEntry(found, System.currentTimeMillis())
                            found
                        }
                    }
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    Timber.w(ex, "Local copy search failed for %s", item.id)
                    null
                }
            return twinId?.let {
                try {
                    api.userLibraryApi.getItem(it).content
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    Timber.w(ex, "Could not fetch local copy %s", it)
                    null
                }
            }
        }

        private suspend fun searchMovieTwin(item: BaseItemDto): UUID? =
            searchByIds(item, BaseItemKind.MOVIE)?.also {
                Timber.i("Found local copy %s for streamed movie %s (%s)", it, item.id, item.name)
            }

        private suspend fun searchEpisodeTwin(episode: BaseItemDto): UUID? {
            val seriesId = episode.seriesId ?: return null
            val season = episode.parentIndexNumber ?: return null
            val number = episode.indexNumber ?: return null
            val cachedSeries = seriesCache[seriesId]?.takeIf { it.isFresh }
            val localSeriesId =
                if (cachedSeries != null) {
                    cachedSeries.twinId
                } else {
                    val series = api.userLibraryApi.getItem(seriesId).content
                    searchByIds(series, BaseItemKind.SERIES)
                        .also { seriesCache[seriesId] = CacheEntry(it, System.currentTimeMillis()) }
                } ?: return null
            val userId = serverRepository.currentUser?.id ?: return null
            val episodes =
                GetEpisodesRequestHandler
                    .execute(
                        api,
                        GetEpisodesRequest(
                            seriesId = localSeriesId,
                            userId = userId,
                            season = season,
                            isMissing = false,
                            fields = listOf(ItemFields.PATH),
                            enableImages = false,
                            enableUserData = false,
                        ),
                    ).content.items
            return episodes
                .firstOrNull { candidate ->
                    candidate.parentIndexNumber == season &&
                        (
                            candidate.indexNumber == number ||
                                (candidate.indexNumber != null && candidate.indexNumberEnd != null &&
                                    number in candidate.indexNumber!!..candidate.indexNumberEnd!!)
                        ) &&
                        candidate.isLocalFile()
                }?.id
                ?.also { Timber.i("Found local copy %s for streamed episode %s S%sE%s", it, episode.id, season, number) }
        }

        /**
         * Searches by title for a local item of [type] with the same provider ids as [item]
         */
        private suspend fun searchByIds(
            item: BaseItemDto,
            type: BaseItemKind,
        ): UUID? {
            val ids = item.providerIds.normalized()
            if (ids.isEmpty()) return null
            val userId = serverRepository.currentUser?.id ?: return null
            val names = listOfNotNull(item.name, item.originalTitle).filter { it.isNotBlank() }.distinct()
            for (name in names) {
                val candidates =
                    api.itemsApi
                        .getItems(
                            GetItemsRequest(
                                userId = userId,
                                searchTerm = name,
                                includeItemTypes = listOf(type),
                                recursive = true,
                                fields = listOf(ItemFields.PROVIDER_IDS, ItemFields.PATH),
                                enableImages = false,
                                enableUserData = false,
                                enableTotalRecordCount = false,
                                limit = 25,
                            ),
                        ).content.items
                candidates
                    .firstOrNull { candidate ->
                        val candidateIds = candidate.providerIds.normalized()
                        candidate.id != item.id &&
                            candidate.isLocalFile() &&
                            MATCHING_PROVIDERS.any { key -> ids[key] != null && ids[key] == candidateIds[key] }
                    }?.let { return it.id }
            }
            return null
        }

        private fun Map<String, String?>?.normalized(): Map<String, String> =
            orEmpty()
                .mapNotNull { (key, value) -> value?.takeIf { it.isNotBlank() }?.let { key.lowercase() to it.lowercase() } }
                .toMap()

        private fun BaseItemDto.isLocalFile(): Boolean {
            val path = path ?: return false
            return locationType != LocationType.VIRTUAL &&
                !path.startsWith(GELATO_PREFIX) &&
                !path.startsWith("http://") &&
                !path.startsWith("https://") &&
                !path.endsWith(".strm", ignoreCase = true)
        }

        companion object {
            private const val GELATO_PREFIX = "gelato://"
            private const val CACHE_MS = 10 * 60 * 1000L
            private val MATCHING_PROVIDERS = listOf("tmdb", "tvdb", "imdb")
        }
    }
