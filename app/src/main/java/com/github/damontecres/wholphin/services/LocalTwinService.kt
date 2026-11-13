package com.github.damontecres.wholphin.services

import com.github.damontecres.wholphin.data.ServerRepository
import kotlinx.coroutines.CancellationException
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the local copy of a streamed (Gelato) movie.
 *
 * A movie favorited in Gelato is requested via Seerr and downloaded by Radarr to the HDD, but Jellyfin indexes the
 * file as a separate item in another library, so the Gelato item never shows it as a version. This matches them by
 * TMDB/IMDb id, like Jellyfin does for metadata, so the local file can be played instead of a debrid stream.
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
        )

        private val cache = ConcurrentHashMap<UUID, CacheEntry>()

        /**
         * Whether the item only has streamed sources (eg a Gelato stub)
         */
        fun isStreamOnly(item: BaseItemDto): Boolean {
            if (item.type != BaseItemKind.MOVIE) return false
            if (item.path?.startsWith(GELATO_PREFIX) == true) return true
            val sources = item.mediaSources.orEmpty()
            return sources.isNotEmpty() && sources.all { it.isRemote || it.protocol == MediaProtocol.HTTP }
        }

        /**
         * Returns the full local item for a stream-only movie, or null if there is none
         */
        suspend fun findLocalTwin(item: BaseItemDto): BaseItemDto? {
            if (!isStreamOnly(item)) return null
            val cached = cache[item.id]
            val twinId =
                if (cached != null && System.currentTimeMillis() - cached.timeMs < CACHE_MS) {
                    cached.twinId
                } else {
                    searchTwinId(item).also { cache[item.id] = CacheEntry(it, System.currentTimeMillis()) }
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

        private suspend fun searchTwinId(item: BaseItemDto): UUID? {
            val ids = item.providerIds.orEmpty().mapKeys { it.key.lowercase() }
            val tmdb = ids["tmdb"]?.takeIf { it.isNotBlank() }
            val imdb = ids["imdb"]?.takeIf { it.isNotBlank() }
            if (tmdb == null && imdb == null) return null
            val userId = serverRepository.currentUser?.id ?: return null
            val names = listOfNotNull(item.name, item.originalTitle).filter { it.isNotBlank() }.distinct()
            for (name in names) {
                val candidates =
                    try {
                        api.itemsApi
                            .getItems(
                                GetItemsRequest(
                                    userId = userId,
                                    searchTerm = name,
                                    includeItemTypes = listOf(BaseItemKind.MOVIE),
                                    recursive = true,
                                    fields = listOf(ItemFields.PROVIDER_IDS, ItemFields.PATH),
                                    enableImages = false,
                                    enableUserData = false,
                                    enableTotalRecordCount = false,
                                    limit = 25,
                                ),
                            ).content.items
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.w(ex, "Local copy search failed for %s", item.id)
                        return null
                    }
                val twin =
                    candidates.firstOrNull { candidate ->
                        val candidateIds = candidate.providerIds.orEmpty().mapKeys { it.key.lowercase() }
                        candidate.id != item.id &&
                            candidate.isLocalFile() &&
                            (
                                (tmdb != null && candidateIds["tmdb"] == tmdb) ||
                                    (imdb != null && candidateIds["imdb"].equals(imdb, ignoreCase = true))
                            )
                    }
                if (twin != null) {
                    Timber.i("Found local copy %s for streamed item %s (%s)", twin.id, item.id, item.name)
                    return twin.id
                }
            }
            return null
        }

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
        }
    }
