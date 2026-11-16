@file:UseSerializers(UUIDSerializer::class)

package com.github.damontecres.wholphin.util

import com.github.damontecres.wholphin.ui.SlimItemFields
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import java.util.UUID

/**
 * The titles of one streaming platform: its movie and series collections shown as one list, alternating
 * a movie and a series so both stay near the top in their catalog order.
 */
@Serializable
data class PlatformItemsRequest(
    val userId: UUID?,
    val movieCollectionId: UUID?,
    val seriesCollectionId: UUID?,
    val startIndex: Int = 0,
    val limit: Int? = null,
    val fields: List<ItemFields> = SlimItemFields,
)

@Serializable
object PlatformItemsRequestHandler : RequestHandler<PlatformItemsRequest> {
    override fun prepare(
        request: PlatformItemsRequest,
        startIndex: Int,
        limit: Int,
        enableTotalRecordCount: Boolean,
    ): PlatformItemsRequest = request.copy(startIndex = startIndex, limit = limit)

    override suspend fun execute(
        api: ApiClient,
        request: PlatformItemsRequest,
    ): Response<BaseItemDtoQueryResult> =
        coroutineScope {
            suspend fun children(collectionId: UUID?): List<BaseItemDto> =
                collectionId
                    ?.let {
                        api.itemsApi
                            .getItems(
                                GetItemsRequest(
                                    userId = request.userId,
                                    parentId = it,
                                    fields = request.fields,
                                    enableTotalRecordCount = false,
                                ),
                            ).content.items
                    }.orEmpty()

            val movies = async { children(request.movieCollectionId) }
            val series = async { children(request.seriesCollectionId) }
            val all = interleaveDistinct(movies.await(), series.await())
            val page =
                all
                    .drop(request.startIndex)
                    .let { rest -> request.limit?.let { rest.take(it) } ?: rest }
            Response(
                content = BaseItemDtoQueryResult(items = page, totalRecordCount = all.size, startIndex = request.startIndex),
                status = 200,
                headers = emptyMap(),
            )
        }
}

/** Alternates the two lists (first, second, first, …), skipping titles already taken. */
internal fun interleaveDistinct(
    first: List<BaseItemDto>,
    second: List<BaseItemDto>,
): List<BaseItemDto> {
    val seen = HashSet<UUID>()
    val result = ArrayList<BaseItemDto>(first.size + second.size)
    for (i in 0 until maxOf(first.size, second.size)) {
        first.getOrNull(i)?.takeIf { seen.add(it.id) }?.let(result::add)
        second.getOrNull(i)?.takeIf { seen.add(it.id) }?.let(result::add)
    }
    return result
}
