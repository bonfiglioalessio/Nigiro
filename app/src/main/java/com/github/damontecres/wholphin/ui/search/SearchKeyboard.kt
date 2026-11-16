package com.github.damontecres.wholphin.ui.search

import androidx.compose.foundation.border
import com.github.damontecres.wholphin.ui.FaGlyph
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.damontecres.wholphin.ui.FontAwesome
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette

private val KeySize = 40.dp
private val KeyGap = 6.dp
private const val COLUMNS = 6

val SearchKeyboardWidth: Dp = KeySize * COLUMNS + KeyGap * (COLUMNS - 1)

private val CharRows =
    listOf(
        "abcdef",
        "ghijkl",
        "mnopqr",
        "stuvwx",
        "yz1234",
        "567890",
    )

private val FieldHeight = 52.dp
private val FieldShape = RoundedCornerShape(12.dp)

/**
 * The query being typed, shown above the keyboard (not focusable: typing happens on the keyboard), with an optional
 * trailing control such as the microphone, like the Google TV search bar
 */
@Composable
fun SearchQueryDisplay(
    query: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KeyGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.width(SearchKeyboardWidth),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .weight(1f)
                    .height(FieldHeight)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), FieldShape)
                    .padding(horizontal = 14.dp),
        ) {
            FaGlyph(
                text = stringResource(R.string.fa_magnifying_glass),
                fontFamily = FontAwesome,
                fontSize = 15.sp,
                color = NigiroPalette.TextMuted,
            )
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    fontSize = 17.sp,
                    color = NigiroPalette.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = "$query|",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Voice search next to the field: a square key as tall as the field */
@Composable
fun VoiceFieldButton(onClick: () -> Unit) {
    val label = stringResource(R.string.voice_search)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(FieldShape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.08f),
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = NigiroPalette.Background,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        modifier = Modifier.size(FieldHeight).semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(FieldHeight)) {
            FaGlyph(text = stringResource(R.string.fa_microphone), fontFamily = FontAwesome, fontSize = 18.sp)
        }
    }
}

/**
 * On-screen keyboard for the remote: always visible, no system IME. Searching happens only with the Search key.
 */
@Composable
fun SearchKeyboard(
    onChar: (Char) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    firstKeyFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    voiceKey: (@Composable () -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(KeyGap),
        modifier = modifier.width(SearchKeyboardWidth),
    ) {
        CharRows.forEachIndexed { rowIndex, chars ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                chars.forEachIndexed { index, char ->
                    KeyboardKey(
                        onClick = { onChar(char) },
                        modifier =
                            if (rowIndex == 0 && index == 0) {
                                Modifier.focusRequester(firstKeyFocusRequester)
                            } else {
                                Modifier
                            },
                    ) {
                        Text(text = char.uppercase(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        // Apostrophe for Italian titles (L'imperatrice, C'era una volta)
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            KeyboardKey(onClick = onSpace, span = 2) {
                Text(text = stringResource(R.string.keyboard_space), fontSize = 14.sp)
            }
            KeyboardKey(onClick = { onChar('\'') }) {
                Text(text = "'", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            KeyboardKey(onClick = onBackspace) {
                Text(text = "⌫", fontSize = 18.sp)
            }
            // Written out: a ✕ here read as the letter X right above it
            KeyboardKey(onClick = onClear, span = 2) {
                Text(text = stringResource(R.string.keyboard_clear), fontSize = 14.sp)
            }
        }
        // Search and voice are keys too: same height, gaps and focus as the rest of the keyboard
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            KeyboardKey(
                onClick = onSubmit,
                span = if (voiceKey != null) 4 else COLUMNS,
                containerColor = NigiroPalette.Pink,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.search), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    // Goes to the results
                    Text(text = "→", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            voiceKey?.invoke()
        }
    }
}

@Composable
private fun KeyboardKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    span: Int = 1,
    containerColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = containerColor,
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = NigiroPalette.Background,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        modifier = modifier.size(width = KeySize * span + KeyGap * (span - 1), height = KeySize),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(KeySize)) {
            content()
        }
    }
}
