package com.github.damontecres.wholphin.ui.components

import android.content.res.Resources
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.TrackIndex
import com.github.damontecres.wholphin.preferences.lazyListWrapScrolling
import com.github.damontecres.wholphin.services.StreamChoiceResult
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.formatBitrate
import com.github.damontecres.wholphin.ui.formatBytes
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.playback.SimpleMediaStream
import com.github.damontecres.wholphin.ui.roundMinutes
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.SourceHdr
import com.github.damontecres.wholphin.util.SourceQuality
import com.github.damontecres.wholphin.util.SourceRanking
import com.github.damontecres.wholphin.util.StreamSourceParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.util.UUID

/**
 * Parameters for rendering a [DialogPopup]
 */
data class DialogParams(
    val fromLongClick: Boolean,
    val title: String,
    val items: List<DialogItemEntry>,
)

sealed interface DialogItemEntry

data object DialogItemDivider : DialogItemEntry

/**
 * An item within a [DialogPopup]
 */
data class DialogItem(
    val headlineContent: @Composable () -> Unit,
    val onClick: () -> Unit,
    val overlineContent: @Composable (() -> Unit)? = null,
    val supportingContent: @Composable (() -> Unit)? = null,
    val leadingContent: @Composable (BoxScope.() -> Unit)? = null,
    val trailingContent: @Composable (() -> Unit)? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val dismissOnClick: Boolean = false,
) : DialogItemEntry {
    constructor(
        @StringRes text: Int,
        @StringRes iconStringRes: Int,
        iconColor: Color = Color.Unspecified,
        dismissOnClick: Boolean = false,
        onClick: () -> Unit,
    ) : this(
        headlineContent = {
            Text(
                text = stringResource(text),
            )
        },
        leadingContent = {
            Text(
                text = stringResource(id = iconStringRes),
                fontFamily = FontAwesome,
                color = iconColor,
            )
        },
        onClick = onClick,
        dismissOnClick = dismissOnClick,
    )

    constructor(
        text: String,
        @StringRes iconStringRes: Int,
        dismissOnClick: Boolean = false,
        onClick: () -> Unit,
    ) : this(
        headlineContent = {
            Text(
                text = text,
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        leadingContent = {
            Text(
                text = stringResource(id = iconStringRes),
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = FontAwesome,
            )
        },
        onClick = onClick,
        dismissOnClick = dismissOnClick,
    )

    constructor(
        text: String,
        icon: ImageVector,
        iconColor: Color? = null,
        dismissOnClick: Boolean = false,
        onClick: () -> Unit,
    ) : this(
        headlineContent = {
            Text(
                text = text,
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = iconColor ?: LocalContentColor.current,
            )
        },
        onClick = onClick,
        dismissOnClick = dismissOnClick,
    )

    constructor(
        text: String,
        dismissOnClick: Boolean = false,
        onClick: () -> Unit,
    ) : this(
        headlineContent = {
            Text(
                text = text,
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        onClick = onClick,
        dismissOnClick = dismissOnClick,
    )

    companion object {
        fun divider(): DialogItemEntry = DialogItemDivider
    }
}

/**
 * Show a dialog with a list of entries.
 *
 * @param waitToLoad items start as disabled for about ~1s, which is useful if the dialog spawned from a long press
 */
@Composable
fun DialogPopup(
    showDialog: Boolean,
    title: String,
    dialogItems: List<DialogItemEntry>,
    onDismissRequest: () -> Unit,
    dismissOnClick: Boolean = true,
    waitToLoad: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    elevation: Dp = 1.dp,
) {
    var waiting by remember { mutableStateOf(waitToLoad) }
    if (showDialog) {
        if (waitToLoad) {
            LaunchedEffect(Unit) {
                // This is a hack because a long click will propagate here and click the first list item
                // So this disables the list items assuming the user will stop pressing when the dialog appears
                // This is also bypassed in the code below if the user releases the enter/d-pad center button
                waiting = true
                delay(1000)
                waiting = false
            }
        } else {
            waiting = false
        }
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            val elevatedContainerColor =
                MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
            DialogPopupContent(
                title = title,
                dialogItems = dialogItems,
                waiting = waiting,
                onDismissRequest = onDismissRequest,
                dismissOnClick = dismissOnClick,
                modifier =
                    Modifier
                        .onKeyEvent { event ->
                            val code = event.nativeKeyEvent.keyCode
                            if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                                code in
                                setOf(
                                    KeyEvent.KEYCODE_ENTER,
                                    KeyEvent.KEYCODE_DPAD_CENTER,
                                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                                )
                            ) {
                                waiting = false
                            }
                            false
                        }.shadow(elevation = elevation, shape = RoundedCornerShape(28.0.dp))
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(28.0.dp)
                        }.drawBehind { drawRect(color = elevatedContainerColor) }
                        .padding(PaddingValues(24.dp)),
            )
        }
    }
}

@Composable
fun DialogPopupContent(
    title: String,
    dialogItems: List<DialogItemEntry>,
    waiting: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnClick: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        val focusRequesters = remember { List(dialogItems.size) { FocusRequester() } }
        LaunchedEffect(Unit) {
            // Start on the current choice (eg the playing version or track) instead of the top of the list
            val selectedIndex = dialogItems.indexOfFirst { it is DialogItem && it.selected }
            if (selectedIndex > 0) {
                listState.scrollToItem(selectedIndex)
                focusRequesters[selectedIndex].tryRequestFocus()
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier,
        ) {
            itemsIndexed(dialogItems) { index, item ->
                when (item) {
                    is DialogItemDivider -> {
                        HorizontalDivider(Modifier.height(16.dp))
                    }

                    is DialogItem -> {
                        val interactionSource = remember { MutableInteractionSource() }
                        val focused by interactionSource.collectIsFocusedAsState()
                        ListItem(
                            selected = item.selected,
                            enabled = !waiting && item.enabled,
                            onClick = {
                                if (dismissOnClick || item.dismissOnClick) {
                                    onDismissRequest.invoke()
                                }
                                item.onClick.invoke()
                            },
                            headlineContent = item.headlineContent,
                            overlineContent = item.overlineContent,
                            supportingContent = item.supportingContent,
                            leadingContent = item.leadingContent,
                            trailingContent = item.trailingContent,
                            interactionSource = interactionSource,
                            modifier =
                                Modifier
                                    .focusRequester(focusRequesters[index])
                                    .lazyListWrapScrolling(
                                        listState,
                                        focused,
                                        index == 0,
                                        index == dialogItems.lastIndex,
                                        focusRequesters[0],
                                        focusRequesters[dialogItems.lastIndex],
                                    ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogPopup(
    params: DialogParams,
    onDismissRequest: () -> Unit,
    dismissOnClick: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    elevation: Dp = 1.dp,
) = DialogPopup(
    showDialog = true,
    waitToLoad = params.fromLongClick,
    title = params.title,
    dialogItems = params.items,
    onDismissRequest = onDismissRequest,
    dismissOnClick = dismissOnClick,
    properties = properties,
    elevation = elevation,
)

/**
 * A dialog that can be scrolled, typically for longer text content
 */
@Composable
fun ScrollableDialog(
    onDismissRequest: () -> Unit,
    width: Dp = 600.dp,
    maxHeight: Dp = 380.dp,
    itemSpacing: Dp = 8.dp,
    content: LazyListScope.() -> Unit,
) {
    val scrollAmount = 100f
    val columnState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun scroll(reverse: Boolean = false) {
        scope.launch(ExceptionHandler()) {
            columnState.scrollBy(if (reverse) -scrollAmount else scrollAmount)
        }
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
            ),
    ) {
        LazyColumn(
            state = columnState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
            modifier =
                Modifier
                    .width(width)
                    .heightIn(max = maxHeight)
                    .focusable()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(8.dp),
                    ).onKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            return@onKeyEvent false
                        }
                        if (it.key == Key.DirectionDown) {
                            scroll(false)
                            return@onKeyEvent true
                        }
                        if (it.key == Key.DirectionUp) {
                            scroll(true)
                            return@onKeyEvent true
                        }
                        return@onKeyEvent false
                    },
        )
    }
}

/**
 * Shows a basic dialog
 */
@Composable
fun BasicDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    elevation: Dp = 1.dp,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Box(
            modifier =
                Modifier
                    .shadow(elevation = elevation, shape = RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
                        shape = RoundedCornerShape(8.dp),
                    ),
        ) {
            content()
        }
    }
}

/**
 * Shows a confirmation dialog
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    elevation: Dp = 1.dp,
    bodyColor: Color = MaterialTheme.colorScheme.onSurface,
    @StringRes cancelStringRes: Int = R.string.cancel,
    @StringRes confirmStringRes: Int = R.string.confirm,
) = BasicDialog(
    onDismissRequest = onCancel,
    properties = properties,
    elevation = elevation,
    content = {
        ConfirmDialogContent(title, body, onCancel, onConfirm, Modifier, bodyColor, cancelStringRes, confirmStringRes)
    },
)

/**
 * Content for a confirmation dialog
 */
@Composable
fun ConfirmDialogContent(
    title: String,
    body: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    bodyColor: Color = MaterialTheme.colorScheme.onSurface,
    @StringRes cancelStringRes: Int = R.string.cancel,
    @StringRes confirmStringRes: Int = R.string.confirm,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier,
    ) {
        item {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        body?.let {
            item {
                Text(
                    text = body,
                    color = bodyColor,
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    stringRes = cancelStringRes,
                    onClick = onCancel,
                )
                TextButton(
                    stringRes = confirmStringRes,
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    itemTitle: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    elevation: Dp = 3.dp,
) {
    BasicDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        elevation = elevation,
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.wrapContentWidth(),
        ) {
            item {
                Text(
                    text = stringResource(R.string.delete_item),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            item {
                Text(
                    text = itemTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(bottom = 8.dp),
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier,
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.width(72.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = onConfirm,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = .8f),
                                modifier = Modifier,
                            )
                            Text(
                                text = stringResource(R.string.confirm),
                            )
                        }
                    }
                }
            }
        }
    }
}

fun chooseVersionParams(
    resources: Resources,
    sources: List<MediaSourceInfo>,
    chosenSourceId: UUID?,
    onClick: (Int) -> Unit,
): DialogParams {
    // Shown in ranked order, but callers expect the index within the given list
    val ranked =
        SourceRanking
            .rank(sources.filter { it.id.isNotNullOrBlank() }, capabilities = null)
            .map { source -> sources.indexOfFirst { it === source } to source }
    return DialogParams(
        fromLongClick = false,
        title = resources.getString(R.string.choose_stream, resources.getString(R.string.version)),
        items =
            ranked.map { (index, source) ->
                val selected = chosenSourceId != null && source.id?.toUUIDOrNull() == chosenSourceId
                val title = source.name ?: source.path?.substringAfterLast('/') ?: source.id.orEmpty()
                val details = versionDetails(resources, source, title)
                val runtime = source.runTimeTicks?.ticks?.roundMinutes
                DialogItem(
                    headlineContent = {
                        Text(
                            text = title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        SelectedLeadingContent(selected)
                    },
                    supportingContent =
                        details.takeIf { it.isNotBlank() }?.let { text ->
                            {
                                Text(
                                    text = text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                    trailingContent = runtime?.let { { Text(it.toString()) } },
                    selected = selected,
                    onClick = { onClick.invoke(index) },
                )
            },
    )
}

/**
 * A short summary of a media source, skipping what its name already says (eg AIOStreams names)
 */
internal fun versionDetails(
    resources: Resources,
    source: MediaSourceInfo,
    title: String,
): String {
    val traits = StreamSourceParser.parse(source)
    val videoStream = source.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
    fun String.ifNotInTitle() = takeIf { !title.contains(it, ignoreCase = true) }
    return buildList {
        // Gelato marks its HTTP streams as not remote: only a real file is local
        if (!source.isRemote && source.protocol == MediaProtocol.FILE) add(resources.getString(R.string.version_local))
        if (videoStream?.displayTitle != null) {
            add(videoStream.displayTitle!!)
        } else {
            traits.height?.let { if (it >= 2160) "4K" else "${it}p" }?.ifNotInTitle()?.let(::add)
            traits.videoCodec?.name?.ifNotInTitle()?.let(::add)
            traits.hdr?.let {
                when (it) {
                    SourceHdr.DOLBY_VISION -> "DV"
                    SourceHdr.HDR10_PLUS -> "HDR10+"
                    SourceHdr.HDR10 -> "HDR"
                    SourceHdr.HLG -> "HLG"
                }
            }?.ifNotInTitle()?.let(::add)
        }
        traits.quality?.let {
            when (it) {
                SourceQuality.REMUX -> "REMUX"
                SourceQuality.BLURAY -> "BluRay"
                SourceQuality.WEB -> "WEB"
                SourceQuality.HDTV -> "HDTV"
                SourceQuality.CAM -> "CAM"
            }
        }?.ifNotInTitle()?.let(::add)
        if (!title.contains("GB", ignoreCase = true) && !title.contains("MB", ignoreCase = true)) {
            traits.sizeBytes?.let { add(formatBytes(it)) }
        }
        traits.estimatedBitrate(source.runTimeTicks)?.let { add(formatBitrate(it.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())) }
        if (source.isRemote && traits.uncached) add(resources.getString(R.string.version_not_cached))
    }.joinToString(" · ")
}

@StringRes
fun resourceFor(type: MediaStreamType): Int =
    when (type) {
        MediaStreamType.AUDIO -> R.string.audio
        MediaStreamType.VIDEO -> R.string.video
        MediaStreamType.SUBTITLE -> R.string.subtitles
        MediaStreamType.EMBEDDED_IMAGE -> 0
        MediaStreamType.DATA -> 0
        MediaStreamType.LYRIC -> 0
    }

fun chooseStream(
    resources: Resources,
    streams: List<MediaStream>,
    streamChoice: StreamChoiceResult?,
    type: MediaStreamType,
    preferredSubtitleLanguage: String?,
    onClick: (Int) -> Unit,
): DialogParams {
    val filteredStreams = streams.filter { it.type == type }
    val currentIndex = streamChoice?.stream?.index
    return DialogParams(
        fromLongClick = false,
        title = resources.getString(R.string.choose_stream, resources.getString(resourceFor(type))),
        items =
            buildList {
                if (type == MediaStreamType.SUBTITLE) {
                    add(
                        DialogItem(
                            selected = currentIndex == null,
                            leadingContent = {
                                SelectedLeadingContent(currentIndex == null)
                            },
                            headlineContent = {
                                Text(text = stringResource(R.string.none))
                            },
                            supportingContent = {
                            },
                            trailingContent =
                                if (currentIndex == null && streamChoice != null) {
                                    {
                                        Text(
                                            text = streamChoice.reason.description.getString(),
                                        )
                                    }
                                } else {
                                    null
                                },
                            onClick = { onClick.invoke(TrackIndex.DISABLED) },
                        ),
                    )
                    add(
                        DialogItem(
                            leadingContent = {
                                SelectedLeadingContent(currentIndex == TrackIndex.ONLY_FORCED)
                            },
                            headlineContent = {
                                Text(text = stringResource(R.string.only_forced_subtitles))
                            },
                            supportingContent = {
                            },
                            onClick = { onClick.invoke(TrackIndex.ONLY_FORCED) },
                        ),
                    )
                    if (filteredStreams.isNotEmpty()) {
                        add(DialogItemDivider)
                    }
                }
                addAll(
                    filteredStreams
                        .let {
                            if (type == MediaStreamType.SUBTITLE && preferredSubtitleLanguage.isNotNullOrBlank()) {
                                it.sortedByDescending { it.language != null && it.language == preferredSubtitleLanguage }
                            } else {
                                it
                            }
                        }.mapIndexed { index, stream ->
                            val simpleStream = SimpleMediaStream.from(resources, stream, true)
                            DialogItem(
                                selected = currentIndex == stream.index,
                                leadingContent = {
                                    SelectedLeadingContent(currentIndex == stream.index)
                                },
                                headlineContent = {
                                    Text(
                                        text =
                                            simpleStream.streamTitle
                                                ?: simpleStream.displayTitle,
                                    )
                                },
                                supportingContent = {
                                    if (simpleStream.streamTitle != null) Text(text = simpleStream.displayTitle)
                                },
                                trailingContent =
                                    if (currentIndex == stream.index) {
                                        {
                                            Text(
                                                text = streamChoice.reason.description.getString(),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                onClick = { onClick.invoke(stream.index) },
                            )
                        },
                )
            },
    )
}
