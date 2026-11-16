package com.github.damontecres.wholphin.ui.detail.movie

import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.components.versionDetails
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.tryRequestFocus
import org.jellyfin.sdk.model.api.ImageType
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.damontecres.wholphin.ui.main.episodeSubtitle
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.Locale
import kotlin.time.Duration

/**
 * The version picker shown when play is pressed and the item has more than one version. Same look as the
 * AIOStreams app's (packages/web/src/components/version-picker.tsx in a Modal): the title and subtitle over a
 * dimmed backdrop banner with a close button, the count with a Details pill, the Resume/From the start toggle
 * when there is a position to resume, then one bordered row per version.
 *
 * @param onDetails opens the item's page, null where the picker is already shown over it
 */
@Composable
fun VersionPickerDialog(
    item: BaseItem,
    picker: PickerState,
    onPick: (VersionOption, Duration) -> Unit,
    onDismissRequest: () -> Unit,
    onDetails: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Modal overlay: bg-black/80
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(AioTokens.OverlayAlpha)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .85f
        // The toggle starts on Resume; a version switched to carries on from the choice
        var fromStart by remember(item.id) { mutableStateOf(false) }
        val position = if (fromStart) Duration.ZERO else picker.position
        Column(
            modifier =
                Modifier
                    .width(px(896))
                    .heightIn(max = maxHeight)
                    .clip(RoundedCornerShape(AioTokens.Radius2xl))
                    .background(AioTokens.Paper)
                    .border(px(1), AioTokens.BorderFaint, RoundedCornerShape(AioTokens.Radius2xl)),
        ) {
            PickerHeader(
                item = item,
                count = (picker as? PickerState.Options)?.options?.size,
                resumeAt = picker.position,
                fromStart = fromStart,
                onFromStart = { fromStart = it },
                onDetails =
                    onDetails?.let { details ->
                        {
                            onDismissRequest()
                            details()
                        }
                    },
                onClose = onDismissRequest,
            )
            // versions-list: border-t border-white/5
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(px(1))
                        .background(AioTokens.BorderFaint),
            )
            when (picker) {
                is PickerState.Loading -> {
                    Text(
                        text = stringResource(R.string.searching_sources),
                        fontSize = pxSp(14),
                        color = AioTokens.Muted,
                        modifier = Modifier.padding(horizontal = px(20), vertical = px(28)),
                    )
                }

                is PickerState.Options -> {
                    VersionList(picker) { onPick(it, position) }
                }
            }
        }
    }
}

/** "40:28", or "1:02:03" past the hour, as AIOStreams' clock() */
internal fun clock(position: Duration): String {
    val total = position.inWholeSeconds
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%d:%02d", m, s)
    }
}

/** The Modal's title: the show for an episode, the movie's own otherwise */
private fun pickerTitle(item: BaseItem): String =
    if (item.type == BaseItemKind.EPISODE) {
        item.data.seriesName ?: item.name.orEmpty()
    } else {
        item.title ?: item.name.orEmpty()
    }

/** The Modal's description: "S02E01 · Name" for an episode, the year otherwise */
private fun pickerSubtitle(item: BaseItem): String? =
    if (item.type == BaseItemKind.EPISODE) {
        episodeSubtitle(item.data.parentIndexNumber, item.data.indexNumber, item.name)
    } else {
        item.data.productionYear?.toString()
    }

@Composable
private fun PickerHeader(
    item: BaseItem,
    count: Int?,
    resumeAt: Duration,
    fromStart: Boolean,
    onFromStart: (Boolean) -> Unit,
    onDetails: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val imageUrlService = LocalImageUrlService.current
    val backdrop =
        remember(item.id) { imageUrlService.getItemImageUrl(item, ImageType.BACKDROP, fillWidth = 680) }
    val fade =
        remember {
            Brush.verticalGradient(
                0f to AioTokens.Paper.copy(alpha = .3f),
                .5f to AioTokens.Paper.copy(alpha = .7f),
                1f to AioTokens.Paper,
            )
        }
    Box(modifier = Modifier.fillMaxWidth()) {
        // versions-banner: the art at opacity-25 under a fade into the paper, behind the whole header
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .matchParentSize()
                        .alpha(AioTokens.BannerArtAlpha),
            )
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(fade),
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            // Modal header: px-5 pb-3 pr-14 pt-5, title text-xl font-semibold, description text-sm muted
            Column(
                verticalArrangement = Arrangement.spacedBy(px(6)),
                modifier = Modifier.padding(start = px(20), end = px(56), top = px(20), bottom = px(12)),
            ) {
                Text(
                    text = pickerTitle(item),
                    fontSize = pxSp(20),
                    lineHeight = pxSp(26),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                pickerSubtitle(item)?.let {
                    Text(text = it, fontSize = pxSp(14), color = AioTokens.Muted, maxLines = 1)
                }
            }
            // versions-header: space-y-3 px-5 pb-3
            Column(
                verticalArrangement = Arrangement.spacedBy(px(12)),
                modifier = Modifier.padding(start = px(20), end = px(20), bottom = px(12)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(px(8)),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = px(32)),
                ) {
                    Text(
                        text = count?.let { stringResource(R.string.versions_count_label, it) }.orEmpty(),
                        fontSize = pxSp(14),
                        color = AioTokens.Muted,
                        modifier = Modifier.weight(1f),
                    )
                    onDetails?.let { details ->
                        HeaderPill(
                            text = stringResource(R.string.details_title),
                            icon = Icons.Outlined.Info,
                            onClick = details,
                        )
                    }
                }
                if (resumeAt > Duration.ZERO) {
                    ResumeToggle(resumeAt = resumeAt, fromStart = fromStart, onFromStart = onFromStart)
                }
            }
        }
        // Modal close: absolute right-4 top-4
        CloseButton(
            onClick = onClose,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = px(16), end = px(16)),
        )
    }
}

/** Button size sm, intent gray-subtle, rounded-full: h-8 px-3 text-sm on white/10 */
@Composable
private fun HeaderPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = .1f),
                focusedContainerColor = Color.White.copy(alpha = .2f),
                pressedContainerColor = Color.White.copy(alpha = .2f),
                contentColor = Gray100,
                focusedContentColor = Color.White,
                pressedContentColor = Color.White,
            ),
        border = focusRing(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(px(8)),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(px(32)).padding(horizontal = px(12)),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(px(16)))
            Text(text = text, fontSize = pxSp(14), fontWeight = FontWeight.Medium)
        }
    }
}

/** versions-start: grid-cols-2 gap-1 rounded-full bg-black/40 p-1, the chosen half white */
@Composable
private fun ResumeToggle(
    resumeAt: Duration,
    fromStart: Boolean,
    onFromStart: (Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(px(4)),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = .4f))
                .padding(px(4)),
    ) {
        ToggleHalf(
            text = stringResource(R.string.resume_from, clock(resumeAt)),
            chosen = !fromStart,
            onClick = { onFromStart(false) },
            modifier = Modifier.weight(1f),
        )
        ToggleHalf(
            text = stringResource(R.string.from_start),
            chosen = fromStart,
            onClick = { onFromStart(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Button size sm rounded-full: intent white when chosen, gray-basic otherwise */
@Composable
private fun ToggleHalf(
    text: String,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (chosen) Color.White else Color.Transparent,
                focusedContainerColor = if (chosen) Color.White else Color.White.copy(alpha = .1f),
                pressedContainerColor = if (chosen) Gray200 else Color.White.copy(alpha = .2f),
                contentColor = if (chosen) Color.Black else Gray200,
                focusedContentColor = if (chosen) Color.Black else Color.White,
                pressedContentColor = if (chosen) Color.Black else Color.White,
            ),
        border = focusRing(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(px(32))) {
            Text(text = text, fontSize = pxSp(14), fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

/** The Modal's CloseButton: a round gray-basic icon button with an ✕ */
@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = .1f),
                pressedContainerColor = Color.White.copy(alpha = .2f),
                contentColor = Gray200,
                focusedContentColor = Color.White,
                pressedContentColor = Color.White,
            ),
        border = focusRing(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(px(32))) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                modifier = Modifier.size(px(18)),
            )
        }
    }
}

/** The app's one focus mark: a 2px ring in --ring */
@Composable
private fun focusRing(shape: Shape) =
    ClickableSurfaceDefaults.border(
        focusedBorder = Border(BorderStroke(AioTokens.RingWidth, AioTokens.Ring), shape = shape),
    )

private val Gray100 = Color(0xFFE6E6E6)
private val Gray200 = Color(0xFFDADADA)

@Composable
private fun VersionList(
    picker: PickerState.Options,
    onPick: (VersionOption) -> Unit,
) {
    val initialIndex =
        remember(picker) {
            picker.options.indexOfFirst { it.source.id == picker.chosenSourceId }.coerceAtLeast(0)
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(picker) { focusRequester.tryRequestFocus() }
    // space-y-2 px-5 pb-5 pt-3
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(px(8)),
        contentPadding = PaddingValues(start = px(20), end = px(20), bottom = px(20), top = px(12)),
    ) {
        itemsIndexed(picker.options, key = { _, option -> option.source.id.orEmpty() }) { index, option ->
            VersionRow(
                option = option,
                chosen = option.source.id == picker.chosenSourceId,
                onClick = { onPick(option) },
                modifier = if (index == initialIndex) Modifier.focusRequester(focusRequester) else Modifier,
            )
        }
    }
}

@Composable
private fun VersionRow(
    option: VersionOption,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    // A downloaded copy has no addon name: describe the file instead. Streamed names already carry the
    // quality, size and audio (eg "⚡ RD · 🇮🇹 ITA · 1080p\n🗣 Italian, English").
    val description =
        remember(option) {
            if (option.isLocal) {
                listOfNotNull(
                    versionDetails(resources, option.source, option.title).takeIf { it.isNotBlank() },
                    option.audio?.let { "🗣 $it" },
                ).joinToString("\n").takeIf { it.isNotEmpty() }
            } else if (option.audio != null && !option.title.contains('\n') && !option.title.contains("🗣")) {
                "🗣 ${option.audio}"
            } else {
                null
            }
        }
    val shape = RoundedCornerShape(AioTokens.RadiusXl)
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = AioTokens.RowBackground,
                focusedContainerColor = AioTokens.RowBackgroundFocused,
                pressedContainerColor = AioTokens.RowBackgroundFocused,
                contentColor = Color.White,
                focusedContentColor = Color.White,
                pressedContentColor = Color.White,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(px(1), AioTokens.BorderFaint), shape = shape),
                focusedBorder = Border(BorderStroke(AioTokens.RingWidth, AioTokens.Ring), shape = shape),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(px(12)),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(px(12)),
        ) {
            // size-9 rounded-full bg-white/10, white with a black icon when focused
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(px(36))
                        .clip(CircleShape)
                        .background(if (focused) Color.White else Color.White.copy(alpha = .1f)),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (focused) Color.Black else Color.White,
                    modifier = Modifier.size(px(22)),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(px(4)), modifier = Modifier.weight(1f)) {
                if (chosen) {
                    Text(
                        text = stringResource(R.string.last_choice),
                        fontSize = pxSp(12),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .padding(horizontal = px(8), vertical = px(2)),
                    )
                }
                // text-base font-medium, line breaks kept
                Text(
                    text = option.title,
                    fontSize = pxSp(16),
                    lineHeight = pxSp(22),
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                // text-sm text-gray-300
                description?.let {
                    Text(
                        text = it,
                        fontSize = pxSp(14),
                        lineHeight = pxSp(20),
                        color = AioTokens.Gray300,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
