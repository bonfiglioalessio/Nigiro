package com.github.damontecres.wholphin.ui.components

import android.graphics.Rect
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.theme.InterFontFamily
import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay

/**
 * Where an item menu opens: the middle of what had focus when it was asked for, in window pixels. A remote's
 * held Select has no point of its own, so the AIOStreams app opens its menu beside the focused card the same way.
 */
@Composable
fun rememberFocusAnchor(): IntOffset? {
    val view = LocalView.current
    return remember {
        val rect = Rect()
        view.getFocusedRect(rect)
        if (rect.isEmpty) null else IntOffset(rect.centerX(), rect.centerY())
    }
}

/**
 * The AIOStreams item menu (packages/web/src/components/item-menu.tsx over packages/ui/src/context-menu):
 * a small panel opened beside the item, the item's name as a muted label, then one row per action with
 * separators between the groups. It is not modal there, so nothing behind it is dimmed.
 *
 * @param dismissOnClick every row closes the menu, not only the rows that ask to
 * @param fromLongClick the press that opened it is still down: its repeats and release are dropped, or they
 * would choose the first row as soon as it gets focus
 */
@Composable
fun AioItemMenu(
    title: String,
    entries: List<DialogItemEntry>,
    anchor: IntOffset?,
    fromLongClick: Boolean,
    onDismissRequest: () -> Unit,
    dismissOnClick: Boolean = false,
) {
    var held by remember { mutableStateOf(fromLongClick) }
    if (held) {
        LaunchedEffect(Unit) {
            // The release normally ends it; this only covers a release the menu never saw
            delay(1500)
            held = false
        }
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        (LocalView.current.parent as? DialogWindowProvider)?.window?.apply {
            setDimAmount(0f)
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        val margin = with(LocalDensity.current) { px(8).roundToPx() }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        val native = event.nativeKeyEvent
                        if (held && native.keyCode in SELECT_KEYS) {
                            // A fresh press means the hold already ended before the menu could see it go
                            val freshPress = native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0
                            if (freshPress || native.action == KeyEvent.ACTION_UP) held = false
                            !freshPress
                        } else {
                            false
                        }
                    },
        ) {
            AnchoredMenu(anchor = anchor, margin = margin) {
                MenuPanel(
                    title = title,
                    entries = entries,
                    dismissOnClick = dismissOnClick,
                    onDismissRequest = onDismissRequest,
                )
            }
        }
    }
}

private val SELECT_KEYS =
    setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)

/**
 * Places the menu with its corner on [anchor], as Radix does with a context menu: it goes left or up from the
 * point when there is no room right or below, and always stays [margin] inside the screen.
 */
@Composable
private fun AnchoredMenu(
    anchor: IntOffset?,
    margin: Int,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val placeable =
            measurables
                .first()
                .measure(Constraints(maxWidth = width - 2 * margin, maxHeight = height - 2 * margin))
        val point = anchor ?: IntOffset((width - placeable.width) / 2, (height - placeable.height) / 2)
        val x =
            when {
                anchor == null -> point.x
                point.x + placeable.width + margin <= width -> point.x
                else -> point.x - placeable.width
            }.coerceIn(margin, (width - placeable.width - margin).coerceAtLeast(margin))
        val y =
            when {
                anchor == null -> point.y
                point.y + placeable.height + margin <= height -> point.y
                else -> point.y - placeable.height
            }.coerceIn(margin, (height - placeable.height - margin).coerceAtLeast(margin))
        layout(width, height) { placeable.place(x, y) }
    }
}

@Composable
private fun MenuPanel(
    title: String,
    entries: List<DialogItemEntry>,
    dismissOnClick: Boolean,
    onDismissRequest: () -> Unit,
) {
    val shape = RoundedCornerShape(AioTokens.RadiusXl)
    val first = remember { FocusRequester() }
    val firstItem = entries.indexOfFirst { it is DialogItem }
    LaunchedEffect(Unit) { first.tryRequestFocus() }
    // min-w-[14rem] rounded-xl border bg-[--background] p-1 shadow-lg, as wide as its longest row
    Column(
        modifier =
            Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = px(224), max = px(360))
                .shadow(px(10), shape)
                .clip(shape)
                .background(AioTokens.Background)
                .border(px(1), AioTokens.Border, shape)
                .verticalScroll(rememberScrollState())
                .padding(px(4)),
    ) {
        // ContextMenuLabel: px-2 py-1.5 text-xs font-semibold text-[--muted], one line
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontSize = pxSp(12),
            fontWeight = FontWeight.SemiBold,
            color = AioTokens.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = px(8), vertical = px(6)),
        )
        entries.forEachIndexed { index, entry ->
            when (entry) {
                // ContextMenuSeparator: -mx-1 my-1 h-px bg-[--border]
                is DialogItemDivider -> {
                    Box(
                        modifier =
                            Modifier
                                .padding(vertical = px(4))
                                .fillMaxWidth()
                                .height(px(1))
                                .background(AioTokens.Border),
                    )
                }

                is DialogItem -> {
                    MenuRow(
                        item = entry,
                        onClick = {
                            if (dismissOnClick || entry.dismissOnClick) onDismissRequest()
                            entry.onClick()
                        },
                        modifier = if (index == firstItem) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
        }
    }
}

/** ContextMenuItem: rounded px-2 py-2 text-sm, an 18px icon and mr-2; focused rows get --subtle and the ring */
@Composable
private fun MenuRow(
    item: DialogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AioTokens.Radius)
    Surface(
        onClick = onClick,
        enabled = item.enabled,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = SubtleBackground,
                pressedContainerColor = SubtleBackground,
                contentColor = AioTokens.Foreground,
                focusedContentColor = Color.White,
                pressedContentColor = Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = AioTokens.Foreground.copy(alpha = .5f),
            ),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(AioTokens.RingWidth, AioTokens.Ring), shape = shape),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(px(8)),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = px(8), vertical = px(8)),
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides
                    TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = pxSp(14),
                        lineHeight = pxSp(20),
                    ),
            ) {
                item.leadingContent?.let { icon ->
                    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = pxSp(15))) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(px(18)), content = icon)
                    }
                }
                Box(modifier = Modifier.weight(1f, fill = false)) { item.headlineContent() }
                item.trailingContent?.let { it() }
            }
        }
    }
}

/** --subtle: rgba(255, 255, 255, 0.06) */
private val SubtleBackground = Color.White.copy(alpha = .06f)
