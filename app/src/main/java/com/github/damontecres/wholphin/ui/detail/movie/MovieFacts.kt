package com.github.damontecres.wholphin.ui.detail.movie

import com.github.damontecres.wholphin.ui.theme.px
import com.github.damontecres.wholphin.ui.theme.pxSp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.damontecres.wholphin.ui.theme.AioTokens
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "Details": studios, release date and country, side by side. Only what the server knows is shown.
 */
@Composable
fun MovieFacts(
    movie: BaseItem,
    modifier: Modifier = Modifier,
) {
    val studiosLabel = stringResource(R.string.studios)
    val releasedLabel = stringResource(R.string.released)
    val countryLabel = stringResource(R.string.country)
    val facts =
        remember(movie.data) {
            val dto = movie.data
            listOfNotNull(
                dto.studios
                    ?.mapNotNull { it.name?.takeIf(String::isNotBlank) }
                    ?.take(2)
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")
                    ?.let { studiosLabel to it },
                dto.premiereDate
                    ?.let { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()).format(it) }
                    ?.let { releasedLabel to it },
                dto.productionLocations
                    ?.firstOrNull { it.isNotBlank() }
                    ?.let { countryLabel to it },
            )
        }
    if (facts.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(px(12)),
        modifier = modifier.fillMaxWidth(),
    ) {
        AioSectionTitle(stringResource(R.string.details_title))
        Row(
            horizontalArrangement = Arrangement.spacedBy(px(32)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            facts.forEach { (label, value) ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(px(4)),
                    modifier = Modifier.weight(1f),
                ) {
                    // dt: text-xs uppercase tracking-wide muted; dd: mt-1 text-sm
                    Text(
                        text = label.uppercase(Locale.getDefault()),
                        fontSize = pxSp(12),
                        letterSpacing = .6.sp,
                        color = AioTokens.Muted,
                    )
                    Text(
                        text = value,
                        fontSize = pxSp(14),
                        color = AioTokens.Foreground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
