package com.github.damontecres.wholphin.ui.detail.movie

import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.cards.noButtonGlow
import com.github.damontecres.wholphin.ui.theme.AioTokens
import com.github.damontecres.wholphin.ui.FontAwesome
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.PersonKind
import java.util.Locale
import kotlin.time.Duration

/*
 * The movie page of the AIOStreams app (packages/web/src/pages/item.tsx, components/people.tsx,
 * components/overview.tsx, ui button/badge variants), written for Compose on a 960x540 dp TV screen.
 */

/**
 * item-backdrop (lg): top 85vh, image right-aligned within 2.6x its height and faded in over its first 12rem,
 * side shade from-background via-background/60 at 40%, bottom 3/4 from-background via-background/70
 */
@Composable
fun AioDetailBackdrop(
    movie: BaseItem,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val imageUrlService = LocalImageUrlService.current
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val height = screenHeight * .85f
    val request =
        remember(movie.id) {
            imageUrlService.getItemImageUrl(movie, ImageType.BACKDROP, fillWidth = 1280)?.let {
                ImageRequest
                    .Builder(context)
                    .data(it)
                    .size(1280, 720)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build()
            }
        } ?: return
    // mask-image linear-gradient(to right, transparent, black 12rem) on the frame
    val leftFade =
        remember(background) {
            Brush.horizontalGradient(0f to background, 0.15f to background.copy(alpha = 0f))
        }
    val sideShade =
        remember(background) {
            Brush.horizontalGradient(
                0f to background,
                0.4f to background.copy(alpha = 0.6f),
                1f to background.copy(alpha = 0f),
            )
        }
    // bottom: h-3/4 from-background via-background/70 to-transparent
    val bottomShade =
        remember(background) {
            Brush.verticalGradient(
                0.25f to background.copy(alpha = 0f),
                0.625f to background.copy(alpha = 0.7f),
                1f to background,
            )
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .widthIn(max = height * 2.6f)
                    .fillMaxWidth(),
        ) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(leftFade),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(sideShade),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(bottomShade),
        )
    }
}

/** item-poster: lg:w-56 aspect-[2/3] rounded-xl ring-1 ring-white/10 */
@Composable
fun AioPoster(
    movie: BaseItem,
    modifier: Modifier = Modifier,
) {
    val imageUrlService = LocalImageUrlService.current
    val context = LocalContext.current
    val request =
        remember(movie.id) {
            imageUrlService.getItemImageUrl(movie, ImageType.PRIMARY, fillWidth = 384)?.let {
                ImageRequest
                    .Builder(context)
                    .data(it)
                    .size(384, 576)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build()
            }
        }
    val shape = RoundedCornerShape(AioTokens.RadiusXl)
    Box(
        modifier =
            modifier
                .width(px(224))
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(AioTokens.Gray900)
                .border(px(1), Color.White.copy(alpha = .1f), shape),
    ) {
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** item-logo (lg:max-h-32, max-w min(26rem, 85%)) or item-title (lg:text-5xl bold) */
@Composable
fun AioTitle(
    movie: BaseItem,
    showLogo: Boolean,
) {
    val imageUrlService = LocalImageUrlService.current
    val logoUrl =
        remember(movie.id, showLogo) {
            movie
                .takeIf { showLogo && it.data.imageTags?.containsKey(ImageType.LOGO) == true }
                ?.let { imageUrlService.getItemImageUrl(it, ImageType.LOGO, fillWidth = 832, fillHeight = 192) }
        }
    var logoFailed by remember(logoUrl) { mutableStateOf(false) }
    if (logoUrl != null && !logoFailed) {
        AsyncImage(
            model = logoUrl,
            contentDescription = movie.name,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomStart,
            onError = { logoFailed = true },
            modifier = Modifier.size(width = px(416), height = px(128)),
        )
    } else {
        Text(
            text = movie.title ?: movie.name.orEmpty(),
            fontSize = pxSp(48),
            lineHeight = pxSp(52),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** item-tagline: text-base italic text-gray-300 */
@Composable
fun AioTagline(text: String) {
    Text(text = text, fontSize = pxSp(16), fontStyle = FontStyle.Italic, color = AioTokens.Gray300)
}

/** "2h 35m" like the AIOStreams duration() */
fun aioDuration(runTimeTicks: Long?): String? {
    val minutes = (runTimeTicks ?: return null) / 600_000_000L
    if (minutes <= 0) return null
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** item-meta: year • [age rating box] • runtime • ★ score (• 🍅 critics), text-sm text-gray-200 */
@Composable
fun AioMetaRow(movie: BaseItem) {
    val data = movie.data
    val year = data.productionYear?.toString()
    val ageRating = data.officialRating?.takeIf { it.isNotBlank() }
    val runtime = remember(data.runTimeTicks) { aioDuration(data.runTimeTicks) }
    val score = data.communityRating?.takeIf { it > 0f }?.let { String.format(Locale.ROOT, "%.1f", it) }
    val critics = data.criticRating?.takeIf { it > 0f }?.let { "\uD83C\uDF45 ${it.toInt()}%" }
    val count = listOfNotNull(year, ageRating, runtime, score, critics).size
    if (count == 0) return
    var drawn = 0
    @Composable
    fun Separator() {
        if (drawn++ > 0) Text(text = "•", fontSize = pxSp(14), color = Color(0xFF5A5A5A))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(px(10)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        year?.let {
            Separator()
            MetaText(it)
        }
        ageRating?.let {
            Separator()
            Text(
                text = it,
                fontSize = pxSp(12),
                color = AioTokens.Foreground,
                modifier =
                    Modifier
                        .border(px(1), Color.White.copy(alpha = .3f), RoundedCornerShape(px(4)))
                        .padding(horizontal = px(6), vertical = px(1)),
            )
        }
        runtime?.let {
            Separator()
            MetaText(it)
        }
        score?.let {
            Separator()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = Color(0xFFFACC15), modifier = Modifier.size(px(16)))
                Spacer(Modifier.width(px(4)))
                MetaText(it)
            }
        }
        critics?.let {
            Separator()
            MetaText(it)
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text = text, fontSize = pxSp(14), color = AioTokens.Foreground, maxLines = 1)
}

/** item-genres: Badge intent white size md (h-6 px-2 text-xs semibold tracking-wide rounded-full, gray border) */
@Composable
fun AioGenres(genres: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(px(6))) {
        genres.take(4).forEach { genre ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .height(px(24))
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1C1C1C).copy(alpha = .1f))
                        .border(px(1), Color(0xFF5A5A5A).copy(alpha = .4f), RoundedCornerShape(50))
                        .padding(horizontal = px(8)),
            ) {
                Text(
                    text = genre,
                    fontSize = pxSp(12),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = .3.sp,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

/** overview: text-base leading-relaxed text-gray-300, at most 4 lines, "More" (muted, medium) to open it all */
@Composable
fun AioOverview(
    overview: String,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onMore,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(AioTokens.Radius)),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = .06f),
                contentColor = AioTokens.Gray300,
                focusedContentColor = Color.White,
            ),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(AioTokens.RingWidth, AioTokens.Ring), shape = RoundedCornerShape(AioTokens.Radius)),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier,
    ) {
        // "More" floats at the end of the last line, over a fade so the clipped text runs under it
        val background = androidx.tv.material3.MaterialTheme.colorScheme.background
        var cut by remember(overview) { mutableStateOf(false) }
        Box(modifier = Modifier.padding(horizontal = px(4), vertical = px(2))) {
            Text(
                text = overview,
                onTextLayout = { cut = it.hasVisualOverflow },
                fontSize = pxSp(16),
                lineHeight = pxSp(24),
                maxLines = 4,
                overflow = TextOverflow.Clip,
            )
            if (cut) Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Box(
                    Modifier
                        .width(px(48))
                        .height(px(24))
                        .background(Brush.horizontalGradient(listOf(background.copy(alpha = 0f), background))),
                )
                Text(
                    text = stringResource(R.string.more),
                    fontSize = pxSp(16),
                    lineHeight = pxSp(24),
                    fontWeight = FontWeight.Medium,
                    color = AioTokens.Muted,
                    modifier =
                        Modifier
                            .background(background)
                            .padding(start = px(4)),
                )
            }
        }
    }
}

/** Button size md (h-10 px-4 text-sm font-medium) rounded-full: "white" or "gray-outline"; focus adds the ring */
@Composable
fun AioPillButton(
    white: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val pill = RoundedCornerShape(50)
    val container = if (white) Color.White else Color.Transparent
    val focusedContainer = if (white) Color.White else Color(0xFF5A5A5A).copy(alpha = .15f)
    val contentColor = if (white) Color.Black else AioTokens.Gray300
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(pill),
        colors =
            ButtonDefaults.colors(
                containerColor = container,
                contentColor = contentColor,
                focusedContainerColor = focusedContainer,
                focusedContentColor = if (white) Color.Black else Color.White,
                pressedContainerColor = focusedContainer,
                pressedContentColor = contentColor,
            ),
        border =
            ButtonDefaults.border(
                border =
                    Border(
                        BorderStroke(px(1), if (white) Color.Transparent else Color(0xFF8F8F8F).copy(alpha = .2f)),
                        shape = pill,
                    ),
                focusedBorder = Border(BorderStroke(AioTokens.RingWidth, AioTokens.Ring), shape = pill),
            ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        glow = noButtonGlow(),
        contentPadding = PaddingValues(horizontal = px(16)),
        modifier = modifier.height(px(40)),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
            content = content,
        )
    }
}

/** IconButton size md (40x40, icon text-2xl) rounded-full, "gray-subtle" or an active intent */
@Composable
fun AioIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeTint: Color? = null,
) {
    val circle = CircleShape
    val tint = activeTint ?: Color(0xFFE1E1E1)
    val container = activeTint?.copy(alpha = .12f) ?: Color.White.copy(alpha = .1f)
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(circle),
        colors =
            ButtonDefaults.colors(
                containerColor = container,
                contentColor = tint,
                focusedContainerColor = activeTint?.copy(alpha = .2f) ?: Color.White.copy(alpha = .2f),
                focusedContentColor = activeTint ?: Color.White,
                pressedContainerColor = container,
                pressedContentColor = tint,
            ),
        border =
            ButtonDefaults.border(
                focusedBorder = Border(BorderStroke(AioTokens.RingWidth, AioTokens.Ring), shape = circle),
            ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        glow = noButtonGlow(),
        contentPadding = PaddingValues(px(0)),
        modifier = modifier.size(px(40)),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(px(22)))
        }
    }
}

/** item-actions: Play/Resume (+ restart), Trailer, watched, favourite, more */
@Composable
fun AioActions(
    resumePosition: Duration,
    watched: Boolean,
    favorite: Boolean,
    hasTrailer: Boolean,
    onPlay: (Duration) -> Unit,
    onTrailer: () -> Unit,
    onWatched: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    playFocusRequester: FocusRequester,
    onButtonFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusMod = Modifier.onFocusChanged { if (it.isFocused) onButtonFocused() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(px(8)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        val resuming = resumePosition > Duration.ZERO
        AioPillButton(
            white = true,
            onClick = { onPlay(resumePosition) },
            modifier = focusMod.focusRequester(playFocusRequester),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(px(20)))
            Spacer(Modifier.width(px(8)))
            Text(
                text = stringResource(if (resuming) R.string.resume else R.string.play),
                fontSize = pxSp(14),
                fontWeight = FontWeight.Medium,
            )
        }
        if (resuming) {
            AioIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.restart),
                onClick = { onPlay(Duration.ZERO) },
                modifier = focusMod,
            )
        }
        if (hasTrailer) {
            AioPillButton(white = false, onClick = onTrailer, modifier = focusMod) {
                // BiMoviePlay: the FontAwesome film glyph bundled with the app
                Text(text = stringResource(R.string.fa_film), fontFamily = FontAwesome, fontSize = pxSp(15))
                Spacer(Modifier.width(px(8)))
                Text(text = "Trailer", fontSize = pxSp(14), fontWeight = FontWeight.Medium)
            }
        }
        AioIconButton(
            icon = Icons.Default.Check,
            contentDescription = stringResource(if (watched) R.string.mark_unwatched else R.string.mark_watched),
            onClick = onWatched,
            // primary-subtle when watched
            activeTint = if (watched) Color(0xFFC7C2FF) else null,
            modifier = focusMod,
        )
        AioIconButton(
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (favorite) R.string.remove_favorite else R.string.add_favorite),
            onClick = onFavorite,
            // alert-subtle when a favourite
            activeTint = if (favorite) Color(0xFFFCA5A5) else null,
            modifier = focusMod,
        )
        AioIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.more),
            onClick = onMore,
            modifier = focusMod,
        )
    }
}

/** section-title: text-xl font-semibold */
@Composable
fun AioSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = pxSp(20),
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = modifier,
    )
}

/** A crew member with every job they held, like the AIOStreams CastAndCrew */
data class AioPersonEntry(
    val person: Person,
    val detail: String?,
)

fun crewEntries(
    crew: List<Person>,
    jobName: (PersonKind) -> String,
): List<AioPersonEntry> =
    crew
        .groupBy { it.id }
        .map { (_, roles) ->
            AioPersonEntry(roles.first(), roles.map { jobName(it.type) }.distinct().joinToString(" "))
        }

/** MediaRow of person-cards: 120 dp wide, size-24 round photo with ring, name and role text-xs centered */
@Composable
fun AioPeopleRow(
    title: String,
    people: List<AioPersonEntry>,
    onClick: (Person) -> Unit,
    onLongClick: (Int, Person) -> Unit,
    contentStart: Dp,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(px(16)), modifier = modifier) {
        AioSectionTitle(title, Modifier.padding(start = contentStart))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(px(0)),
            contentPadding = PaddingValues(start = contentStart, end = px(32)),
        ) {
            itemsIndexed(people.take(40), key = { i, e -> "${e.person.id}_$i" }) { index, entry ->
                AioPersonCard(entry, onClick = { onClick(entry.person) }, onLongClick = { onLongClick(index, entry.person) })
            }
        }
    }
}

@Composable
private fun AioPersonCard(
    entry: AioPersonEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(AioTokens.RadiusXl)),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                contentColor = Color.White,
                focusedContentColor = Color.White,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier.width(px(120)),
    ) {
        val focused by interaction.collectIsFocusedAsState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = px(6), vertical = px(4)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(px(96))
                        .clip(CircleShape)
                        .background(AioTokens.Gray900)
                        .border(
                            if (focused) px(3) else px(1),
                            if (focused) AioTokens.Ring else Color.White.copy(alpha = .1f),
                            CircleShape,
                        ),
            ) {
                if (entry.person.imageUrl != null) {
                    AsyncImage(
                        model = entry.person.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                    )
                } else {
                    Text(
                        text = initials(entry.person.name.orEmpty()),
                        fontSize = pxSp(18),
                        fontWeight = FontWeight.SemiBold,
                        color = AioTokens.Muted,
                    )
                }
            }
            Spacer(Modifier.height(px(8)))
            Text(
                text = entry.person.name.orEmpty(),
                fontSize = pxSp(12),
                lineHeight = pxSp(16),
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            entry.detail?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = pxSp(12),
                    lineHeight = pxSp(16),
                    color = AioTokens.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun initials(name: String): String =
    name
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
