package com.github.damontecres.wholphin.ui.cards

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.DiscoverItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.ui.detail.LocalRowStyle
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization

@Composable
fun PersonRow(
    people: List<Person>,
    onClick: (Person) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes title: Int = R.string.people_title,
    onLongClick: ((Int, Person) -> Unit)? = null,
) {
    val firstFocus = remember { FocusRequester() }
    var position by rememberInt()
    val enableAnimations = LocalInterfaceCustomization.current.enableAnimations
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        ItemRowTitle(stringResource(title))
        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 8.dp + LocalRowStyle.current.startInset, top = 8.dp, end = 8.dp, bottom = 8.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRestorer(firstFocus),
        ) {
            itemsIndexed(
                people,
                key = { index, person -> "${person.id}_${person.type}_$index" },
                contentType = { _, _ -> "person_card" },
            ) { index, person ->
                val currentPerson by rememberUpdatedState(person)
                val itemOnClick =
                    remember<() -> Unit>(index) {
                        {
                            position = index
                            currentOnClick.invoke(currentPerson)
                        }
                    }
                val itemOnLongClick =
                    remember<() -> Unit>(index) {
                        {
                            position = index
                            currentOnLongClick?.invoke(index, currentPerson)
                        }
                    }
                val cardModifier =
                    Modifier
                        .width(personRowCardWidth)
                        .ifElse(index == position, Modifier.focusRequester(firstFocus))
                PersonCard(
                    person = person,
                    onClick = itemOnClick,
                    onLongClick = itemOnLongClick,
                    modifier = if (enableAnimations) cardModifier.animateItem() else cardModifier,
                )
            }
        }
    }
}

@Composable
fun DiscoverPersonRow(
    people: List<DiscoverItem>,
    onClick: (DiscoverItem) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes title: Int = R.string.people_title,
    onLongClick: ((Int, DiscoverItem) -> Unit)? = null,
) {
    val firstFocus = remember { FocusRequester() }
    val enableAnimations = LocalInterfaceCustomization.current.enableAnimations
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(8.dp),
            modifier =
                Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth()
                    .focusRestorer(firstFocus),
        ) {
            itemsIndexed(
                people,
                key = { index, person -> "${person.type}_${person.id}_$index" },
                contentType = { _, _ -> "discover_person_card" },
            ) { index, person ->
                val currentPerson by rememberUpdatedState(person)
                val itemOnClick =
                    remember<() -> Unit>(index) {
                        { currentOnClick.invoke(currentPerson) }
                    }
                val itemOnLongClick =
                    remember<() -> Unit>(index) {
                        { currentOnLongClick?.invoke(index, currentPerson) }
                    }
                val cardModifier =
                    Modifier
                        .width(personRowCardWidth)
                        .ifElse(index == 0, Modifier.focusRequester(firstFocus))
                PersonCard(
                    name = person.title,
                    role = person.subtitle,
                    imageUrl = person.posterUrl,
                    favorite = false,
                    onClick = itemOnClick,
                    onLongClick = itemOnLongClick,
                    modifier = if (enableAnimations) cardModifier.animateItem() else cardModifier,
                )
            }
        }
    }
}

val personRowCardWidth = 108.dp
