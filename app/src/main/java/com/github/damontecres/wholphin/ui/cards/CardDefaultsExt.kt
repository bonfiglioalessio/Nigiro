package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization

internal val DefaultCardShape: Shape = RoundedCornerShape(8.dp)

/** Disables TV Material3's Gaussian shadow glow shader on cards. */
@Composable
fun noCardGlow() =
    CardDefaults.glow(
        glow = Glow.None,
        focusedGlow = Glow.None,
        pressedGlow = Glow.None,
    )

/** Scales cards only when UI animations are enabled, avoiding per-frame layer invalidation on slow GPUs. */
@Composable
fun cardScale(focusedScale: Float = 1.05f) =
    CardDefaults.scale(
        scale = 1f,
        focusedScale = if (LocalInterfaceCustomization.current.enableAnimations) focusedScale else 1f,
        pressedScale = 1f,
    )

/** Crisp non-glowing focus outline used in place of expensive scale/shadow animations. */
@Composable
fun cardFocusBorder(
    shape: Shape = DefaultCardShape,
    focusedWidth: Dp = 2.5.dp,
) = CardDefaults.border(
    focusedBorder =
        Border(
            border =
                BorderStroke(
                    width = focusedWidth,
                    color = MaterialTheme.colorScheme.border,
                ),
            shape = shape,
        ),
)

/** Disables TV Material3's Gaussian shadow glow shader on buttons. */
@Composable
fun noButtonGlow() =
    ButtonDefaults.glow(
        glow = Glow.None,
        focusedGlow = Glow.None,
        pressedGlow = Glow.None,
    )

/** Scales buttons only when UI animations are enabled. */
@Composable
fun buttonScale(focusedScale: Float = 1.05f) =
    ButtonDefaults.scale(
        scale = 1f,
        focusedScale = if (LocalInterfaceCustomization.current.enableAnimations) focusedScale else 1f,
        pressedScale = 1f,
    )
