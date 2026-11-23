package com.github.damontecres.wholphin.ui.discover

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.data.model.DiscoverItem
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.cards.DiscoverItemCard
import com.github.damontecres.wholphin.ui.cards.ItemRowTitle
import com.github.damontecres.wholphin.ui.cards.ViewMoreCard
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.SkeletonRow
import com.github.damontecres.wholphin.ui.detail.LocalRowStyle
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.DataLoadingState

@Composable
fun DiscoverRow(
    row: DiscoverRowData,
    onClickItem: (Int, DiscoverItem) -> Unit,
    onLongClickItem: (Int, DiscoverItem) -> Unit,
    onCardFocus: (Int) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    enableViewMore: Boolean = false,
    onClickViewMore: () -> Unit = {},
) {
    when (val state = row.items) {
        is DataLoadingState.Error -> {
            ErrorMessage(state.message, state.exception, modifier)
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            SkeletonRow(
                title = row.title.getString(),
                cardHeight = Cards.height2x3,
                contentStartPadding = 16.dp,
                titleStartPadding = 8.dp,
                titleStyle = null,
                titleGap = 8.dp,
                modifier = modifier,
            )
        }

        is DataLoadingState.Success<List<DiscoverItem>> -> {
            DiscoverItemRow(
                title = row.title.getString(),
                items = state.data,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
                onCardFocus = onCardFocus,
                enableViewMore = enableViewMore,
                onClickViewMore = onClickViewMore,
                modifier = modifier.focusRequester(focusRequester),
            )
        }
    }
}

@Composable
fun DiscoverItemRow(
    title: String,
    items: List<DiscoverItem?>,
    onClickItem: (Int, DiscoverItem) -> Unit,
    onLongClickItem: (Int, DiscoverItem) -> Unit,
    onCardFocus: (Int) -> Unit,
    enableViewMore: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    onClickViewMore: () -> Unit = {},
) {
    val state = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    var position by rememberInt()

    val currentOnClickItem by rememberUpdatedState(onClickItem)
    val currentOnLongClickItem by rememberUpdatedState(onLongClickItem)
    val currentOnCardFocus by rememberUpdatedState(onCardFocus)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier.focusProperties {
                onEnter = {
                    focusRequester.tryRequestFocus()
                }
            },
    ) {
        ItemRowTitle(title)

        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
            contentPadding =
                PaddingValues(
                    start =
                        LocalRowStyle.current.let {
                            (if (it.alignContentToTitle) 8.dp else horizontalPadding) + it.startInset
                        },
                    end = horizontalPadding,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusGroup()
                    .focusRestorer(firstFocus)
                    .focusRequester(focusRequester),
        ) {
            itemsIndexed(
                items,
                key = { index, item ->
                    if (item != null) "${item.type}_${item.id}_$index" else "placeholder_$index"
                },
                contentType = { _, item -> if (item != null) "discover_card" else "discover_placeholder" },
            ) { index, item ->
                val isFirstFocusTarget = index == position
                val cardModifier =
                    remember(index, isFirstFocusTarget) {
                        if (isFirstFocusTarget) {
                            Modifier.focusRequester(firstFocus)
                        } else {
                            Modifier
                        }.onFocusChanged {
                            if (it.isFocused) {
                                currentOnCardFocus.invoke(index)
                            }
                        }
                    }

                val currentItem by rememberUpdatedState(item)
                val onClick =
                    remember<() -> Unit>(index) {
                        {
                            position = index
                            currentItem?.let { currentOnClickItem(index, it) }
                        }
                    }

                val onLongClick =
                    remember<() -> Unit>(index) {
                        {
                            position = index
                            currentItem?.let { currentOnLongClickItem(index, it) }
                        }
                    }

                DiscoverItemCard(
                    item = item,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    showOverlay = true,
                    modifier = cardModifier,
                )
            }
            if (enableViewMore) {
                item {
                    ViewMoreCard(
                        onClick =
                            remember {
                                {
                                    position = items.size
                                    onClickViewMore.invoke()
                                }
                            },
                        onLongClick = {},
                        modifier =
                            Modifier
                                .ifElse(items.size == position, Modifier.focusRequester(firstFocus))
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        currentOnCardFocus.invoke(items.size)
                                    }
                                },
                    )
                }
            }
        }
    }
}
