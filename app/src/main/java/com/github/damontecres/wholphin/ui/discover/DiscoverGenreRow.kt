package com.github.damontecres.wholphin.ui.discover

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.cards.GenreCard
import com.github.damontecres.wholphin.ui.cards.ItemRow
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.Genre
import com.github.damontecres.wholphin.ui.components.SkeletonRow
import com.github.damontecres.wholphin.ui.util.StringProvider
import com.github.damontecres.wholphin.util.DataLoadingState

@Composable
fun DiscoverGenreRow(
    title: StringProvider,
    items: DataLoadingState<List<Genre>>,
    onClickItem: (Int, Genre) -> Unit,
    onCardFocus: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onLongClickItem: (Int, Genre) -> Unit = { _, _ -> },
) {
    when (items) {
        is DataLoadingState.Error -> {
            ErrorMessage(items.message, items.exception, modifier)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            SkeletonRow(
                title = title.getString(),
                cardHeight = Cards.heightEpisode,
                aspectRatio = AspectRatios.WIDE,
                showCardTitles = false,
                contentStartPadding = 16.dp,
                titleStartPadding = 8.dp,
                titleStyle = null,
                titleGap = 8.dp,
                modifier = modifier,
            )
        }

        is DataLoadingState.Success<List<Genre>> -> {
            ItemRow(
                title = title.getString(),
                items = items.data,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
                cardContent = { index, item, mod, onClick, onLongClick ->
                    GenreCard(
                        genre = item,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        modifier =
                            mod
                                .height(Cards.heightEpisode)
                                .onFocusChanged {
                                    if (it.isFocused) onCardFocus.invoke(index)
                                },
                    )
                },
                modifier = modifier,
                horizontalPadding = 16.dp,
                showViewMore = false,
                viewMoreCardContent = {},
            )
        }
    }
}
