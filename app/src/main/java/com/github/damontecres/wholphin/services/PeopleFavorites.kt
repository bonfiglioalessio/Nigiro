package com.github.damontecres.wholphin.services

import android.content.Context
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.ui.letNotEmpty
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gets people in media, specifically to check if they are favorited or not
 */
@Singleton
class PeopleFavorites
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val api: ApiClient,
    ) {
        suspend fun getPeopleFor(item: BaseItem): List<Person> =
            item.data.people.orEmpty().map { it.id }.chunked(50).let { chunks ->
                val favorites =
                    chunks
                        .map {
                            api.itemsApi
                                .getItems(ids = it)
                                .content.items
                        }.flatten()
                        .associateBy({ it.id }, { it.userData?.isFavorite ?: false })
                val people =
                    item.data.people
                        ?.letNotEmpty { people ->
                            people.map {
                                Person.fromDto(
                                    context,
                                    it,
                                    favorites[it.id] ?: false,
                                    api,
                                )
                            }
                        }.orEmpty()
                mergeRoles(people)
            }
    }

/**
 * One card per person: eg a director who also wrote the screenplay shows once with both roles
 */
fun mergeRoles(people: List<Person>): List<Person> =
    people
        .groupBy { it.id }
        .values
        .map { same ->
            if (same.size == 1) {
                same.first()
            } else {
                same.first().copy(
                    role = same.mapNotNull { it.role?.takeIf { r -> r.isNotBlank() } }.distinct().joinToString(", "),
                    favorite = same.any { it.favorite },
                )
            }
        }
