package com.github.damontecres.wholphin.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.components.ItemGrid
import com.github.damontecres.wholphin.ui.explore.ExploreGridSource
import com.github.damontecres.wholphin.ui.explore.request
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.util.StringStringProvider
import com.github.damontecres.wholphin.util.RequestHandler

/**
 * Favourites as an Explore-style grid: AIOStreams poster cards with the all/movies/series filter. The grid reloads
 * on every return, so a title unfavourited on its page disappears.
 */
@Composable
fun FavoritesPage(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val destination =
        remember {
            val source = ExploreGridSource(favorites = true)
            // The grid's view model rebuilds the request with the current user
            val (request, handler) = source.request(userId = null, filter = source.filter)
            @Suppress("UNCHECKED_CAST")
            Destination.ItemGrid(
                title = StringStringProvider(context.getString(R.string.favorites)),
                request = request,
                requestHandler = handler as RequestHandler<Any>,
                explore = source,
            )
        }
    ItemGrid(preferences = preferences, destination = destination, modifier = modifier)
}
