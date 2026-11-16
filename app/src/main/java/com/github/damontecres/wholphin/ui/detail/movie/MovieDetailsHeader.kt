package com.github.damontecres.wholphin.ui.detail.movie

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.cards.ItemCardImage
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.OverviewText
import com.github.damontecres.wholphin.ui.components.QuickDetails
import com.github.damontecres.wholphin.ui.components.TitleOrLogo
import com.github.damontecres.wholphin.ui.letNotEmpty
import com.github.damontecres.wholphin.util.ExceptionHandler
import kotlinx.coroutines.launch

val MoviePosterWidth = 200.dp

/**
 * The movie's poster, shown left of the header
 */
@Composable
fun MoviePoster(
    movie: BaseItem,
    modifier: Modifier = Modifier,
) {
    ItemCardImage(
        item = movie,
        name = movie.name,
        showOverlay = false,
        favorite = false,
        watched = false,
        unwatchedCount = 0,
        watchedPercent = null,
        numberOfVersions = 0,
        contentScale = ContentScale.Crop,
        fillWidth = 400,
        modifier =
            modifier
                .width(MoviePosterWidth)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp)),
    )
}

/**
 * Title (or logo), tagline, quick details, genres and overview. Nothing here needs the media sources, so the
 * header is complete as soon as the page opens.
 */
@Composable
fun MovieDetailsHeader(
    preferences: UserPreferences,
    movie: BaseItem,
    bringIntoViewRequester: BringIntoViewRequester,
    overviewOnClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dto = movie.data
    val scope = rememberCoroutineScope()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        TitleOrLogo(
            item = movie,
            showLogo = preferences.appPreferences.interfacePreferences.showLogos,
            modifier =
                Modifier
                    .fillMaxWidth(.7f)
                    .padding(start = HeaderUtils.startPadding),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(.9f),
        ) {
            dto.taglines?.firstOrNull()?.let { tagline ->
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = HeaderUtils.startPadding),
                )
            }

            QuickDetails(
                movie.ui.quickDetails,
                movie.timeRemainingOrRuntime,
                Modifier.padding(start = HeaderUtils.startPadding),
            )

            dto.genres?.letNotEmpty { genres ->
                GenreChips(genres, Modifier.padding(start = HeaderUtils.startPadding))
            }

            dto.overview?.let { overview ->
                OverviewText(
                    overview = overview,
                    maxLines = 3,
                    onClick = overviewOnClick,
                    textBoxHeight = Dp.Unspecified,
                    modifier =
                        Modifier.onFocusChanged {
                            if (it.isFocused) {
                                scope.launch(ExceptionHandler()) {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun GenreChips(
    genres: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        genres.take(MAX_GENRE_CHIPS).forEach { genre ->
            Text(
                text = genre,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier =
                    Modifier
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .35f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

private const val MAX_GENRE_CHIPS = 4
