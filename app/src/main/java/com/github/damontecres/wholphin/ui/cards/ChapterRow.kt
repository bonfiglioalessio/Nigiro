package com.github.damontecres.wholphin.ui.cards

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.Chapter
import com.github.damontecres.wholphin.ui.detail.LocalRowStyle

@Composable
fun ChapterRow(
    chapters: List<Chapter>,
    onClick: (Chapter) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((Chapter) -> Unit)? = null,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        ItemRowTitle(stringResource(R.string.chapters))
        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding =
                PaddingValues(
                    start = LocalRowStyle.current.let { (if (it.alignContentToTitle) 8.dp else 16.dp) + it.startInset },
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRestorer(),
        ) {
            itemsIndexed(
                chapters,
                key = { index, item -> "${item.itemId}_${item.index}_$index" },
                contentType = { _, _ -> "chapter_card" },
            ) { index, item ->
                val currentItem by rememberUpdatedState(item)
                val itemOnClick = remember<() -> Unit>(index) { { currentOnClick(currentItem) } }
                val hasLongClick = onLongClick != null
                val itemOnLongClick =
                    remember<(() -> Unit)?>(index, hasLongClick) {
                        if (hasLongClick) {
                            { currentOnLongClick?.invoke(currentItem) }
                        } else {
                            null
                        }
                    }
                ChapterCard(
                    chapter = item,
                    onClick = itemOnClick,
                    modifier = Modifier,
                    onLongClick = itemOnLongClick,
                )
            }
        }
    }
}
