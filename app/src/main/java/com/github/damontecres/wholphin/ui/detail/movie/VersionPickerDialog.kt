package com.github.damontecres.wholphin.ui.detail.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.components.versionDetails
import com.github.damontecres.wholphin.ui.tryRequestFocus
import org.jellyfin.sdk.model.api.ImageType

/**
 * The version picker shown when play is pressed and the movie has more than one version: a header with the
 * movie's backdrop, title, year and number of versions, then one row per version (name, details, audio).
 */
@Composable
fun VersionPickerDialog(
    movie: BaseItem,
    picker: PickerState,
    onPick: (VersionOption) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier =
                Modifier
                    .width(760.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            PickerHeader(movie, (picker as? PickerState.Options)?.options?.size)
            when (picker) {
                is PickerState.Loading -> {
                    Text(
                        text = stringResource(R.string.searching_sources),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }

                is PickerState.Options -> {
                    VersionList(picker, onPick)
                }
            }
        }
    }
}

@Composable
private fun PickerHeader(
    movie: BaseItem,
    count: Int?,
) {
    val imageUrlService = LocalImageUrlService.current
    val backdrop =
        remember(movie.id) { imageUrlService.getItemImageUrl(movie, ImageType.BACKDROP, fillWidth = 760) }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(130.dp),
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = .85f),
                            .7f to Color.Black.copy(alpha = .45f),
                            1f to Color.Black.copy(alpha = .25f),
                        ),
                    ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = movie.title ?: movie.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub =
                listOfNotNull(
                    movie.data.productionYear?.toString(),
                    count?.let { stringResource(R.string.versions_count_label, it) },
                ).joinToString("  ·  ")
            if (sub.isNotEmpty()) {
                Text(text = sub, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .75f))
            }
        }
    }
}

@Composable
private fun VersionList(
    picker: PickerState.Options,
    onPick: (VersionOption) -> Unit,
) {
    val resources = LocalResources.current
    val initialIndex =
        remember(picker) {
            picker.options.indexOfFirst { it.source.id == picker.chosenSourceId }.coerceAtLeast(0)
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(picker) { focusRequester.tryRequestFocus() }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.heightIn(max = 420.dp),
    ) {
        itemsIndexed(picker.options, key = { _, option -> option.source.id.orEmpty() }) { index, option ->
            val details = remember(option) { versionDetails(resources, option.source, option.title) }
            ListItem(
                selected = option.source.id == picker.chosenSourceId,
                onClick = { onPick(option) },
                headlineContent = {
                    Text(text = option.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                supportingContent =
                    listOfNotNull(details.takeIf { it.isNotBlank() }, option.audio?.let { "🗣 $it" })
                        .takeIf { it.isNotEmpty() }
                        ?.let { lines ->
                            {
                                Column {
                                    lines.forEach { line ->
                                        Text(text = line, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        },
                leadingContent = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                modifier =
                    if (index == initialIndex) Modifier.focusRequester(focusRequester) else Modifier,
            )
        }
    }
}
