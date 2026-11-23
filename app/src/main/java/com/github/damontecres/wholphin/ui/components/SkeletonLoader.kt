package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.cards.ItemRowTitle
import com.github.damontecres.wholphin.ui.detail.LocalRowStyle
import com.github.damontecres.wholphin.ui.main.NigiroDimens
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.theme.colors.NigiroPalette
import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val SkeletonSurface = AioTokens.Gray900
private val SkeletonHighlight = NigiroPalette.Card
private val SkeletonSubtle = Color(0xFF151515)

private fun Modifier.skeletonBleedStart(start: Dp): Modifier =
    if (start <= 0.dp) {
        this
    } else {
        layout { measurable, constraints ->
            if (!constraints.hasBoundedWidth) {
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            } else {
                val startPx = start.roundToPx()
                val width = constraints.maxWidth + startPx
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(constraints.maxWidth, placeable.height) {
                    placeable.place(-startPx, 0)
                }
            }
        }
    }

/**
 * Static placeholder block used to compose Android TV skeleton screens without animations or render layers.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AioTokens.Radius),
    color: Color = SkeletonSurface,
    showBorder: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .background(color, shape)
                .then(
                    if (showBorder) {
                        Modifier.border(px(1), AioTokens.BorderFaint, shape)
                    } else {
                        Modifier
                    },
                ),
    )
}

/**
 * Focus-preserving full-page skeleton wrapper that delays drawing placeholders slightly to avoid flicker on fast loads.
 */
@Composable
fun SkeletonPageContainer(
    modifier: Modifier = Modifier,
    focusEnabled: Boolean = true,
    delay: Duration = 120.milliseconds,
    content: @Composable BoxScope.() -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    if (focusEnabled) {
        LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }
    }
    var visible by remember(delay) { mutableStateOf(delay <= Duration.ZERO) }
    if (!visible) {
        LaunchedEffect(delay) {
            delay(delay)
            visible = true
        }
    }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable(focusEnabled),
    ) {
        if (visible) {
            content()
        }
    }
}

@Composable
private fun SkeletonPosterCard(
    cardHeight: Dp = NigiroDimens.PosterHeight,
    aspectRatio: Float = AspectRatios.TALL,
    showTitle: Boolean = true,
    showSubtitle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val width = cardHeight * aspectRatio.coerceAtLeast(AspectRatios.MIN)
    Column(
        verticalArrangement = Arrangement.spacedBy(px(4)),
        modifier = modifier.width(width),
    ) {
        SkeletonBox(
            modifier = Modifier.size(width, cardHeight),
            shape = RoundedCornerShape(AioTokens.Radius),
            showBorder = true,
        )
        if (showTitle) {
            SkeletonBox(
                modifier =
                    Modifier
                        .padding(horizontal = px(2))
                        .width(width * 0.78f)
                        .height(px(14)),
                shape = RoundedCornerShape(px(4)),
                color = SkeletonHighlight,
            )
            if (showSubtitle) {
                SkeletonBox(
                    modifier =
                        Modifier
                            .padding(horizontal = px(2))
                            .width(width * 0.36f)
                            .height(px(12)),
                    shape = RoundedCornerShape(px(4)),
                    color = SkeletonSubtle,
                )
            }
        }
    }
}

@Composable
private fun SkeletonWideCard(
    cardHeight: Dp = NigiroDimens.ContinueCardHeight,
    aspectRatio: Float = AspectRatios.WIDE,
    showTitle: Boolean = true,
    showSubtitle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val width = cardHeight * aspectRatio.coerceAtLeast(AspectRatios.MIN)
    Column(
        verticalArrangement = Arrangement.spacedBy(px(6)),
        modifier = modifier.width(width),
    ) {
        SkeletonBox(
            modifier = Modifier.size(width, cardHeight),
            shape = RoundedCornerShape(AioTokens.RadiusXl),
            showBorder = true,
        )
        if (showTitle) {
            SkeletonBox(
                modifier =
                    Modifier
                        .padding(horizontal = px(2))
                        .width(width * 0.65f)
                        .height(px(15)),
                shape = RoundedCornerShape(px(4)),
                color = SkeletonHighlight,
            )
            if (showSubtitle) {
                SkeletonBox(
                    modifier =
                        Modifier
                            .padding(horizontal = px(2))
                            .width(width * 0.42f)
                            .height(px(13)),
                    shape = RoundedCornerShape(px(4)),
                    color = SkeletonSubtle,
                )
            }
        }
    }
}

/**
 * Static horizontal row skeleton used for loading rows without layout jumps or nested lazy containers.
 */
@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    cardHeight: Dp = NigiroDimens.PosterHeight,
    aspectRatio: Float = AspectRatios.TALL,
    cardCount: Int = if (aspectRatio > 1f) 4 else 7,
    cardSpacing: Dp = NigiroDimens.CardGap,
    contentStartPadding: Dp = NigiroDimens.ContentStart,
    titleStartPadding: Dp = NigiroDimens.ContentStart,
    titleStyle: TextStyle? = NigiroDimens.RowTitle,
    titleGap: Dp = NigiroDimens.TitleGap,
    showCardTitles: Boolean = true,
    showCardSubtitles: Boolean = true,
    focusable: Boolean = false,
) {
    val rowStyle = LocalRowStyle.current
    val resolvedCardStart = (if (rowStyle.alignContentToTitle) titleStartPadding else contentStartPadding) + rowStyle.startInset
    val focusModifier =
        if (focusable) {
            val interactionSource = remember { MutableInteractionSource() }
            val focused by interactionSource.collectIsFocusedAsState()
            Modifier
                .focusable(interactionSource = interactionSource)
                .background(
                    if (focused) MaterialTheme.colorScheme.border.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(8.dp),
                )
        } else {
            Modifier
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(titleGap),
        modifier = modifier.fillMaxWidth().then(focusModifier),
    ) {
        if (!title.isNullOrBlank()) {
            ItemRowTitle(
                title = title,
                startPadding = titleStartPadding,
                style = titleStyle,
            )
        } else {
            SkeletonBox(
                modifier =
                    Modifier
                        .padding(start = titleStartPadding + rowStyle.startInset)
                        .width(160.dp)
                        .height(20.dp),
                shape = RoundedCornerShape(4.dp),
                color = SkeletonHighlight,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(top = 8.dp, bottom = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                modifier =
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .padding(start = resolvedCardStart),
            ) {
                repeat(cardCount) {
                    if (aspectRatio > 1f) {
                        SkeletonWideCard(
                            cardHeight = cardHeight,
                            aspectRatio = aspectRatio,
                            showTitle = showCardTitles,
                            showSubtitle = showCardSubtitles,
                        )
                    } else {
                        SkeletonPosterCard(
                            cardHeight = cardHeight,
                            aspectRatio = aspectRatio,
                            showTitle = showCardTitles,
                            showSubtitle = showCardSubtitles,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Static skeleton matching the Home page hero banner and first peeking catalog row.
 */
@Composable
fun HomePageSkeleton(
    modifier: Modifier = Modifier,
    focusEnabled: Boolean = true,
) {
    SkeletonPageContainer(
        modifier = modifier,
        focusEnabled = focusEnabled,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NigiroDimens.SectionGap),
            modifier = Modifier.fillMaxSize().clipToBounds(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(NigiroDimens.AioHeroHeight),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(NigiroDimens.AioHeroHeight - 24.dp)
                            .background(Color(0xFF0C0C0C)),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(px(16), Alignment.Bottom),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = NigiroDimens.ContentStart,
                                end = NigiroDimens.ContentEnd,
                                bottom = px(56),
                            ),
                ) {
                    SkeletonBox(
                        modifier = Modifier.width(px(340)).height(px(44)),
                        shape = RoundedCornerShape(px(8)),
                        color = SkeletonHighlight,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(px(10))) {
                        SkeletonBox(
                            modifier = Modifier.width(px(52)).height(px(16)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(40)).height(px(16)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(64)).height(px(16)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(px(8))) {
                        SkeletonBox(
                            modifier = Modifier.width(px(480)).height(px(14)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(340)).height(px(14)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(px(16)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBox(
                            modifier = Modifier.width(px(140)).height(px(42)),
                            shape = RoundedCornerShape(50),
                            color = SkeletonHighlight,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(124)).height(px(42)),
                            shape = RoundedCornerShape(50),
                            color = SkeletonSurface,
                            showBorder = true,
                        )
                        SkeletonBox(
                            modifier = Modifier.size(px(42)),
                            shape = CircleShape,
                            color = SkeletonSurface,
                            showBorder = true,
                        )
                    }
                }
            }
            SkeletonRow(
                title = null,
                cardHeight = NigiroDimens.PosterHeight,
            )
        }
    }
}

/**
 * Static skeleton matching movie, series, and episode detail headers.
 */
@Composable
fun DetailPageSkeleton(
    modifier: Modifier = Modifier,
    bleedStart: Dp = 12.dp,
    focusEnabled: Boolean = true,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val headerTop = screenHeight * 0.27f
    SkeletonPageContainer(
        modifier = modifier,
        focusEnabled = focusEnabled,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(px(48)),
            modifier =
                Modifier
                    .fillMaxSize()
                    .skeletonBleedStart(bleedStart)
                    .clipToBounds(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(px(32)),
                verticalAlignment = Alignment.Bottom,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = px(40), top = headerTop),
            ) {
                SkeletonBox(
                    modifier =
                        Modifier
                            .width(px(224))
                            .aspectRatio(2f / 3f),
                    shape = RoundedCornerShape(AioTokens.RadiusXl),
                    showBorder = true,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(px(16)),
                    modifier = Modifier.widthIn(max = px(768)),
                ) {
                    SkeletonBox(
                        modifier = Modifier.width(px(360)).height(px(46)),
                        shape = RoundedCornerShape(px(8)),
                        color = SkeletonHighlight,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(px(10))) {
                        SkeletonBox(
                            modifier = Modifier.width(px(54)).height(px(16)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(42)).height(px(16)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(68)).height(px(16)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(px(8))) {
                        SkeletonBox(
                            modifier = Modifier.width(px(72)).height(px(24)),
                            shape = RoundedCornerShape(50),
                            color = SkeletonSurface,
                            showBorder = true,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(88)).height(px(24)),
                            shape = RoundedCornerShape(50),
                            color = SkeletonSurface,
                            showBorder = true,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(64)).height(px(24)),
                            shape = RoundedCornerShape(50),
                            color = SkeletonSurface,
                            showBorder = true,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(px(8))) {
                        SkeletonBox(
                            modifier = Modifier.width(px(520)).height(px(14)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(480)).height(px(14)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(px(320)).height(px(14)),
                            shape = RoundedCornerShape(px(4)),
                            color = SkeletonSubtle,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(px(8)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBox(
                            modifier = Modifier.width(px(148)).height(px(42)),
                            shape = RoundedCornerShape(50),
                            color = SkeletonHighlight,
                        )
                        repeat(3) {
                            SkeletonBox(
                                modifier = Modifier.size(px(42)),
                                shape = CircleShape,
                                color = SkeletonSurface,
                                showBorder = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Static skeleton for card grid pages.
 */
@Composable
fun GridPageSkeleton(
    modifier: Modifier = Modifier,
    title: String? = null,
    showTitle: Boolean = true,
    columns: Int = 6,
    aspectRatio: Float = AspectRatios.TALL,
    rows: Int = 3,
    spacing: Dp = NigiroDimens.CardGap,
    contentPadding: PaddingValues = PaddingValues(start = NigiroDimens.ContentStart, end = NigiroDimens.ContentEnd, top = 20.dp),
    showCardTitles: Boolean = true,
    focusEnabled: Boolean = true,
) {
    val safeCols = columns.coerceIn(1, 8)
    val safeRatio = aspectRatio.coerceAtLeast(AspectRatios.MIN)
    SkeletonPageContainer(
        modifier = modifier,
        focusEnabled = focusEnabled,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NigiroDimens.TitleGap),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .clipToBounds(),
        ) {
            if (showTitle) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = NigiroDimens.RowTitle.copy(fontSize = 26.sp, lineHeight = 30.sp),
                        color = NigiroPalette.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    SkeletonBox(
                        modifier = Modifier.width(200.dp).height(28.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = SkeletonHighlight,
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                repeat(rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(safeCols) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(px(4)),
                                modifier = Modifier.weight(1f),
                            ) {
                                SkeletonBox(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(safeRatio),
                                    shape = RoundedCornerShape(AioTokens.Radius),
                                    showBorder = true,
                                )
                                if (showCardTitles) {
                                    SkeletonBox(
                                        modifier = Modifier.fillMaxWidth(0.75f).height(px(14)),
                                        shape = RoundedCornerShape(px(4)),
                                        color = SkeletonHighlight,
                                    )
                                    SkeletonBox(
                                        modifier = Modifier.fillMaxWidth(0.38f).height(px(12)),
                                        shape = RoundedCornerShape(px(4)),
                                        color = SkeletonSubtle,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Static skeleton for multi-row recommendation pages.
 */
@Composable
fun RowsPageSkeleton(
    modifier: Modifier = Modifier,
    rowCount: Int = 3,
    cardHeight: Dp = Cards.height2x3,
    focusEnabled: Boolean = true,
) {
    SkeletonPageContainer(
        modifier = modifier,
        focusEnabled = focusEnabled,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NigiroDimens.SectionGap),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
                    .clipToBounds(),
        ) {
            repeat(rowCount) {
                SkeletonRow(
                    title = null,
                    cardHeight = cardHeight,
                )
            }
        }
    }
}

/**
 * Static skeleton matching the person profile header and first credits row.
 */
@Composable
fun PersonPageSkeleton(
    modifier: Modifier = Modifier,
    focusEnabled: Boolean = true,
) {
    SkeletonPageContainer(
        modifier = modifier,
        focusEnabled = focusEnabled,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .clipToBounds(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
            ) {
                SkeletonBox(
                    modifier =
                        Modifier
                            .weight(1f)
                            .aspectRatio(2f / 3f),
                    shape = RoundedCornerShape(10),
                    showBorder = true,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier =
                        Modifier
                            .weight(3f)
                            .padding(end = 32.dp)
                            .background(SkeletonSurface, RoundedCornerShape(10))
                            .border(px(1), AioTokens.BorderFaint, RoundedCornerShape(10))
                            .padding(16.dp),
                ) {
                    SkeletonBox(
                        modifier = Modifier.width(240.dp).height(28.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = SkeletonHighlight,
                    )
                    SkeletonBox(
                        modifier = Modifier.width(180.dp).height(14.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = SkeletonSubtle,
                    )
                    SkeletonBox(
                        modifier = Modifier.width(380.dp).height(14.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = SkeletonSubtle,
                    )
                    SkeletonBox(
                        modifier = Modifier.width(300.dp).height(14.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = SkeletonSubtle,
                    )
                }
            }
            SkeletonRow(
                title = null,
                cardHeight = Cards.height2x3,
                contentStartPadding = 16.dp,
                titleStartPadding = 8.dp,
                titleStyle = null,
            )
        }
    }
}

/**
 * Non-focusable inline skeleton for season episode cards matching [AioEpisodeCard] dimensions.
 */
@Composable
fun EpisodeRowSkeleton(
    contentStart: Dp,
    cardCount: Int = 4,
    modifier: Modifier = Modifier,
) {
    val cardWidth = px(336)
    val cardHeight = cardWidth * (9f / 16f)
    Box(
        modifier = modifier.fillMaxWidth().clipToBounds(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(px(16)),
            modifier =
                Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .padding(start = contentStart),
        ) {
            repeat(cardCount) {
                SkeletonWideCard(
                    cardHeight = cardHeight,
                    aspectRatio = 16f / 9f,
                )
            }
        }
    }
}

/**
 * Non-focusable inline skeleton for Explore genre tiles while genre counts load.
 */
@Composable
fun ExploreGenresSkeleton(
    columns: Int = 4,
    rows: Int = 2,
    tileWidth: Dp = 190.dp,
    tileHeight: Dp = 107.dp,
    modifier: Modifier = Modifier,
) {
    val tileShape = RoundedCornerShape(10.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(NigiroDimens.SectionGap),
        modifier =
            modifier.padding(
                start = NigiroDimens.ContentStart,
                end = NigiroDimens.ContentEnd,
                top = NigiroDimens.TitleGap,
            ),
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(NigiroDimens.CardGap)) {
                repeat(columns) {
                    Box(
                        contentAlignment = Alignment.BottomStart,
                        modifier =
                            Modifier
                                .size(tileWidth, tileHeight)
                                .background(SkeletonSurface, tileShape)
                                .border(1.dp, AioTokens.BorderFaint, tileShape),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            SkeletonBox(
                                modifier = Modifier.width(96.dp).height(16.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = SkeletonHighlight,
                            )
                            SkeletonBox(
                                modifier = Modifier.width(56.dp).height(11.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = SkeletonSubtle,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Non-focusable inline skeleton for combined search results while a query is in flight.
 */
@Composable
fun SearchResultsSkeleton(
    posterHeight: Dp = 160.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NigiroDimens.SectionGap),
        modifier = modifier.padding(top = 12.dp).clipToBounds(),
    ) {
        repeat(2) {
            SkeletonRow(
                title = null,
                cardHeight = posterHeight,
                cardCount = 6,
                contentStartPadding = 0.dp,
                titleStartPadding = 0.dp,
                titleGap = NigiroDimens.TitleGap - 4.dp,
                focusable = false,
            )
        }
    }
}
