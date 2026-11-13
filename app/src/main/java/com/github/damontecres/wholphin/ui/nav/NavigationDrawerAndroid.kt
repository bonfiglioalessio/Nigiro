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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
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
                    // Transparent when collapsed; a dark scrim when open so the labels stay readable over images
                    if (drawerState.currentValue == DrawerValue.Open) {
                        Modifier.background(NigiroOpenDrawerBrush)
                    } else {
                        Modifier
                    }
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
internal val ExpandedDrawerItemWidth = 220.dp
internal val DrawerIconSize = 24.dp
internal val DrawerIconPadding = (ListItemDefaults.IconSize - DrawerIconSize) / 2
internal val DrawerAnimationStiffness = Spring.StiffnessMedium
internal val FlatDrawerItemShape = RoundedCornerShape(10.dp)

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
            CollapsedDrawerItemWidth
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
            NavigationDrawerItemDefaults.ContainerHeightOneLine
        } else {
            NavigationDrawerItemDefaults.ContainerHeightTwoLine
        }

    ListItem(
        selected = selected,
        onClick = onClick,
        headlineContent = content,
        leadingContent = {
            Box(
                Modifier
                    .padding(horizontal = DrawerIconPadding)
                    .size(DrawerIconSize),
            ) { leadingContent() }
        },
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        modifier =
            modifier
                .layout { measurable, constraints ->
                    val width = animatedWidth.roundToPx()
                    val height = navDrawerItemHeight.roundToPx()
                    val placeable =
                        measurable.measure(
                            constraints.copy(
                                minWidth = width,
                                maxWidth = width,
                                minHeight = height,
                                maxHeight = height,
                            ),
                        )
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
        enabled = enabled,
        onLongClick = onLongClick,
        tonalElevation = tonalElevation,
        shape = ListItemDefaults.shape(shape = FlatDrawerItemShape),
        colors = colors.toToggleableListItemColors(hasFocus),
        scale = ListItemDefaults.scale(1f, 1f),
        interactionSource = interactionSource,
    )
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

private val NigiroOpenDrawerBrush =
    Brush.horizontalGradient(
        0f to NigiroPalette.Background.copy(alpha = 0.97f),
        1f to NigiroPalette.Background.copy(alpha = 0.88f),
    )
