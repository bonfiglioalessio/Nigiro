/*
 * This file contains code copied and modified from:
 * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-androidx-tv-material-release/tv/tv-material/src/main/java/androidx/tv/material3/
 *
 * Their license & required attribution is below.
 *
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.damontecres.wholphin.ui.nav

import androidx.compose.foundation.layout.requiredSize
import com.github.damontecres.wholphin.ui.theme.px
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Surface
import com.github.damontecres.wholphin.ui.theme.pxSp
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Glow
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.NavigationDrawerItemColors
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.theme.LocalTheme
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization

/**
 * This is a re-implementation of [androidx.tv.material3.ModalNavigationDrawer].
 *
 * It changes padding, sizing, and animations that couldn't be changed in the Androidx implementation.
 */
@Composable
fun ModalNavigationDrawer(
    drawerContent: @Composable NavigationDrawerScope.(DrawerValue) -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        // Content first: with the Nigiro theme the sidebar is transparent and drawn over the hero and backdrops
        content()

        // AIOStreams tv-rail: no panel behind the open sidebar, the page darkens from the left edge instead
        // (w-[36rem] bg-gradient-to-r from-black via-black/85 to-transparent, faded in over 200 ms)
        if (LocalTheme.current == AppThemeColors.NIGIRO) {
            val open = drawerState.currentValue == DrawerValue.Open
            val enableAnimations = LocalInterfaceCustomization.current.enableAnimations
            val scrimAlpha =
                if (enableAnimations) {
                    androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (open) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "drawer scrim",
                    ).value
                } else if (open) {
                    1f
                } else {
                    0f
                }
            if (scrimAlpha > 0f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(px(576))
                            .graphicsLayer { alpha = scrimAlpha }
                            .background(AioDrawerScrim),
                )
            }
        }

        DrawerSheet(
            modifier =
                Modifier
                    .align(Alignment.CenterStart),
            drawerState = drawerState,
            content = drawerContent,
        )
    }
}

@Composable
private fun DrawerSheet(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = remember { DrawerState() },
    content: @Composable NavigationDrawerScope.(DrawerValue) -> Unit,
) {
    // indicates that the drawer has been set to its initial state and has grabbed focus if
    // necessary. Controls whether focus is used to decide the state of the drawer going forward.
    var initializationComplete: Boolean by remember { mutableStateOf(false) }
    var focusState by remember { mutableStateOf<FocusState?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open && focusState?.hasFocus == false) {
            // used to grab focus if the drawer state is set to Open on start.
            focusRequester.requestFocus()
        }
        initializationComplete = true
    }

    val enableAnimations = LocalInterfaceCustomization.current.enableAnimations
    val sizeModifier =
        if (enableAnimations) {
            Modifier.animateContentSize(
                animationSpec =
                    spring(
                        stiffness = DrawerAnimationStiffness,
                        visibilityThreshold = IntSize.VisibilityThreshold,
                    ),
            )
        } else {
            Modifier
        }
    val flatContainerColor = MaterialTheme.colorScheme.surface
    // Nigiro theme: same full-height gradient as the page, so the sidebar has no visible edge
    val isNigiroTheme = LocalTheme.current == AppThemeColors.NIGIRO
    val hairlineColor = MaterialTheme.colorScheme.border.copy(alpha = 0.28f)
    val internalModifier =
        Modifier
            .focusRequester(focusRequester)
            .then(sizeModifier)
            .fillMaxHeight()
            .then(
                if (isNigiroTheme) {
                    // Always transparent: the page scrim behind it keeps the labels readable
                    Modifier
                } else {
                    Modifier.background(flatContainerColor)
                },
            )
            // adding passed-in modifier here to ensure animateContentSize is called before other
            // size based modifiers.
            .then(modifier)
            .onFocusChanged {
                focusState = it

                if (initializationComplete) {
                    drawerState.setValue(if (it.hasFocus) DrawerValue.Open else DrawerValue.Closed)
                }
            }.focusGroup()

    Box(modifier = internalModifier) {
        NavigationDrawerScopeImpl(drawerState.currentValue == DrawerValue.Open).apply {
            content(drawerState.currentValue)
        }
        if (!isNigiroTheme) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(hairlineColor),
            )
        }
    }
}

internal class NavigationDrawerScopeImpl(
    override val hasFocus: Boolean,
) : NavigationDrawerScope

internal val CollapsedDrawerItemWidth = 60.dp
// AIOStreams tv-rail open width: w-56
internal val ExpandedDrawerItemWidth = px(224)

// AIOStreams sidebar (ui vertical-menu, size lg on the TV; 1 css px = 0.75 dp): 48 px items, 24 px icons,
// collapsed entries a 56 px wide pill
internal val DrawerIconSize = px(24)
internal val DrawerIconPadding = 0.dp
// 64 px: the list item's own paddings leave the icon its full size (at 56 px it gets squeezed)
internal val CollapsedDrawerPillWidth = px(64)
internal val DrawerItemHeight = px(48)
internal val DrawerAnimationStiffness = Spring.StiffnessMedium
internal val FlatDrawerItemShape = RoundedCornerShape(50)

@Composable
internal fun NavigationDrawerScope.NavigationDrawerItem(
    selected: Boolean,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = 0.dp,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val enableAnimations = LocalInterfaceCustomization.current.enableAnimations
    val targetWidth =
        if (hasFocus) {
            ExpandedDrawerItemWidth
        } else {
            CollapsedDrawerPillWidth
        }
    val animatedWidth =
        if (enableAnimations) {
            animateDpAsState(
                targetValue = targetWidth,
                label = "NavigationDrawerItem width open/closed state of the drawer item",
            ).value
        } else {
            targetWidth
        }
    val navDrawerItemHeight =
        if (supportingContent == null) {
            DrawerItemHeight
        } else {
            px(64)
        }

    // AIOStreams vertical-menu item: a pill of fixed height with the icon centred in the collapsed width and the
    // name beside it when the drawer is open. Own layout rather than a ListItem, whose fixed inner paddings clip
    // the name at the 48 px item height.
    val shape = FlatDrawerItemShape
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (selected) colors.selectedContainerColor else colors.containerColor,
                contentColor =
                    when {
                        selected -> colors.selectedContentColor
                        hasFocus -> colors.contentColor
                        else -> colors.inactiveContentColor
                    },
                focusedContainerColor = if (selected) colors.focusedSelectedContainerColor else colors.focusedContainerColor,
                focusedContentColor = colors.focusedContentColor,
                pressedContainerColor = colors.pressedContainerColor,
                pressedContentColor = colors.pressedContentColor,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None, pressedGlow = Glow.None),
        tonalElevation = tonalElevation,
        modifier =
            modifier
                .layout { measurable, constraints ->
                    val width = animatedWidth.roundToPx()
                    val height = navDrawerItemHeight.roundToPx()
                    val placeable =
                        measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height),
                        )
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxSize()
                    // px-3.5 when open (icon lines up with the collapsed one), centred in the collapsed pill
                    .padding(horizontal = if (hasFocus) px(14) else (CollapsedDrawerPillWidth - DrawerIconSize) / 2),
        ) {
            Box(Modifier.requiredSize(DrawerIconSize), contentAlignment = Alignment.Center) { leadingContent() }
            if (hasFocus) {
                Spacer(Modifier.width(px(12)))
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f),
                ) {
                    // text-base font-medium; the supporting line text-xs muted
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyLarge.copy(fontSize = pxSp(16), lineHeight = pxSp(20), fontWeight = FontWeight.Medium),
                    ) { content() }
                    supportingContent?.let {
                        ProvideTextStyle(
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = pxSp(12),
                                lineHeight = pxSp(16),
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = .4f),
                            ),
                        ) { it() }
                    }
                }
                trailingContent?.invoke()
            }
        }
    }
}

@Composable
private fun NavigationDrawerItemColors.toToggleableListItemColors(doesNavigationDrawerHaveFocus: Boolean) =
    ListItemDefaults.colors(
        containerColor = containerColor,
        contentColor = if (doesNavigationDrawerHaveFocus) contentColor else inactiveContentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor =
            if (doesNavigationDrawerHaveFocus) {
                disabledContentColor
            } else {
                disabledInactiveContentColor
            },
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor,
    )

private val AioDrawerScrim =
    Brush.horizontalGradient(
        0f to androidx.compose.ui.graphics.Color.Black,
        0.5f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f),
        1f to androidx.compose.ui.graphics.Color.Transparent,
    )

private val NigiroOpenDrawerBrush =
    Brush.horizontalGradient(
        0f to NigiroPalette.Background.copy(alpha = 0.97f),
        1f to NigiroPalette.Background.copy(alpha = 0.88f),
    )
