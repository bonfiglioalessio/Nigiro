package com.github.damontecres.wholphin.ui.main

import java.util.Locale

private const val TICKS_PER_MINUTE = 600_000_000L

/** "S02E01", or null without both numbers */
fun episodeCode(
    season: Int?,
    episode: Int?,
): String? =
    if (season != null && episode != null) String.format(Locale.ROOT, "S%02dE%02d", season, episode) else null

/** "S02E01 · Name" (either part alone if the other is missing) */
fun episodeSubtitle(
    season: Int?,
    episode: Int?,
    name: String?,
): String? =
    listOfNotNull(episodeCode(season, episode), name?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .takeIf { it.isNotEmpty() }

/** "8 min rimasti", "1 h 5 min rimasti": only for something started with at least a minute left */
fun remainingLabel(
    runTimeTicks: Long?,
    positionTicks: Long?,
): String? {
    if (runTimeTicks == null || positionTicks == null || positionTicks <= 0) return null
    val minutes = (runTimeTicks - positionTicks) / TICKS_PER_MINUTE
    if (minutes < 1) return null
    val hours = minutes / 60
    val rest = minutes % 60
    val time =
        when {
            hours == 0L -> "$rest min"
            rest == 0L -> "$hours h"
            else -> "$hours h $rest min"
        }
    return "$time rimasti"
}

/** "2021 · ★ 7.8 · 🍅 83% · 2 h 35 min · PG-13", skipping what is missing */
fun cardMeta(
    year: Int?,
    communityRating: Float?,
    criticRating: Float?,
    runTimeTicks: Long?,
    officialRating: String?,
): String {
    val minutes = (runTimeTicks ?: 0L) / TICKS_PER_MINUTE
    val runtime =
        when {
            minutes <= 0 -> null
            minutes >= 60 && minutes % 60 == 0L -> "${minutes / 60} h"
            minutes >= 60 -> "${minutes / 60} h ${minutes % 60} min"
            else -> "$minutes min"
        }
    return listOfNotNull(
        year?.toString(),
        communityRating?.takeIf { it > 0f }?.let { String.format(Locale.ITALIAN, "★ %.1f", it).replace(',', '.') },
        criticRating?.takeIf { it > 0f }?.let { "🍅 ${it.toInt()}%" },
        runtime,
        officialRating?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
}
