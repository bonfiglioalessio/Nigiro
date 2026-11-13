package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.detail.LocalRowStyle
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.tryRequestFocus

@Composable
fun <T> ItemRow(
    title: String,
    items: List<T?>,
    onClickItem: (Int, T) -> Unit,
    onLongClickItem: (Int, T) -> Unit,
    cardContent: @Composable (
        index: Int,
        item: T?,
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    showViewMore: Boolean = false,
    viewMoreCardContent: @Composable (Modifier) -> Unit = {},
    itemKey: ((T) -> Any)? = null,
    contentStartPadding: Dp = horizontalPadding,
    titleStartPadding: Dp = 8.dp,
    titleStyle: TextStyle? = null,
    titleGap: Dp = 8.dp,
) {
    val state = rememberLazyListState()
    // Stable keys keep focus & card state on the same item when the list changes (eg a refresh reorders it).
    // Duplicates fall back to the index since keys must be unique.
    val keys =
        remember(items, itemKey) {
            itemKey?.let { keyOf ->
                val seen = HashSet<Any>()
                items.mapIndexed { index, item -> item?.let(keyOf)?.takeIf { seen.add(it) } ?: "index_$index" }
            }
        }
    val firstFocus = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    var position by rememberInt()

    val currentOnClickItem by rememberUpdatedState(onClickItem)
    val currentOnLongClickItem by rememberUpdatedState(onLongClickItem)

    Column(
        verticalArrangement = Arrangement.spacedBy(titleGap),
        modifier =
            modifier.focusProperties {
                onEnter = {
                    focusRequester.tryRequestFocus()
                }
            },
    ) {
        ItemRowTitle(title, startPadding = titleStartPadding, style = titleStyle)

        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
            contentPadding =
                PaddingValues(
                    start =
                        LocalRowStyle.current.let {
                            (if (it.alignContentToTitle) titleStartPadding else contentStartPadding) + it.startInset
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
            itemsIndexed(items, key = keys?.let { k -> { index, _ -> k[index] } }) { index, item ->
                val cardModifier =
                    remember(index, position) {
                        if (index == position) {
                            Modifier.focusRequester(firstFocus)
                        } else {
                            Modifier
                        }
                    }

                val onClick =
                    remember(index, item) {
                        {
                            position = index
                            if (item != null) currentOnClickItem(index, item)
                        }
                    }

                val onLongClick =
                    remember(index, item) {
                        {
                            position = index
                            if (item != null) currentOnLongClickItem(index, item)
                        }
                    }

                cardContent.invoke(
                    index,
                    item,
                    cardModifier,
                    onClick,
                    onLongClick,
                )
            }
            if (showViewMore) {
                item {
                    val cardModifier =
                        remember(items.size, position) {
                            if (position == items.size) {
                                Modifier.focusRequester(firstFocus)
                            } else {
                                Modifier
                            }
                        }
                    viewMoreCardContent.invoke(cardModifier)
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
fun ItemRowTitle(
    title: String,
    modifier: Modifier = Modifier,
    startPadding: Dp = 8.dp,
    style: TextStyle? = null,
) {
    val rowStyle = LocalRowStyle.current
    Text(
        text = title,
        style = style ?: rowStyle.titleStyle ?: MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(start = startPadding + rowStyle.startInset),
    )
}
