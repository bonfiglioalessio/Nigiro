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

/**
 * The version picker shown when play is pressed and the movie has more than one version. Same look as the
 * AIOStreams app's (packages/web/src/components/version-picker.tsx): a dimmed backdrop banner with title and
 * count, then one bordered row per version with a round play icon and the version's name.
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
        // Modal overlay: bg-black/80
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(AioTokens.OverlayAlpha)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .85f
        Column(
            modifier =
                Modifier
                    .width(px(896))
                    .heightIn(max = maxHeight)
                    .clip(RoundedCornerShape(AioTokens.Radius2xl))
                    .background(AioTokens.Paper)
                    .border(px(1), AioTokens.BorderFaint, RoundedCornerShape(AioTokens.Radius2xl)),
        ) {
            PickerHeader(movie, (picker as? PickerState.Options)?.options?.size)
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
        remember(movie.id) { imageUrlService.getItemImageUrl(movie, ImageType.BACKDROP, fillWidth = 680) }
    val fade =
        remember {
            Brush.verticalGradient(
                0f to AioTokens.Paper.copy(alpha = .3f),
                .5f to AioTokens.Paper.copy(alpha = .7f),
                1f to AioTokens.Paper,
            )
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(px(104)),
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(AioTokens.BannerArtAlpha),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(fade),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(px(2)),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = px(20), vertical = px(12)),
        ) {
            // text-lg font-semibold, then text-sm muted
            Text(
                text = movie.title ?: movie.name.orEmpty(),
                fontSize = pxSp(18),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub =
                listOfNotNull(
                    movie.data.productionYear?.toString(),
                    count?.let { stringResource(R.string.versions_count_label, it) },
                ).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(text = sub, fontSize = pxSp(14), color = AioTokens.Muted)
            }
        }
    }
}

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
    // space-y-2 px-5 pb-3
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(px(8)),
        contentPadding = PaddingValues(start = px(20), end = px(20), bottom = px(12), top = px(4)),
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
