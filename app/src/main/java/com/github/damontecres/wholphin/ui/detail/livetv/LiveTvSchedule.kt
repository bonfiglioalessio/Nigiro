package com.github.damontecres.wholphin.ui.detail.livetv

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

enum class LiveTvViewMode {
    NOW,
    TONIGHT,
    GRID,
}

const val FAVORITES_GROUP_KEY = "__FAVORITES__"

data class ChannelScheduleRow(
    val channel: TvChannel,
    val current: TvProgram?,
    val upcoming: List<TvProgram>,
    val tonight: List<TvProgram>,
)

private val TIME_HH_MM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

fun isProgramAiring(
    program: TvProgram,
    now: LocalDateTime,
): Boolean = !program.isFake && !now.isBefore(program.start) && now.isBefore(program.end)

fun isProgramPast(
    program: TvProgram,
    now: LocalDateTime,
): Boolean = !program.isFake && !now.isBefore(program.end)

fun programProgress(
    program: TvProgram,
    now: LocalDateTime,
): Float {
    val totalSeconds = Duration.between(program.start, program.end).seconds.coerceAtLeast(1L)
    val elapsedSeconds = Duration.between(program.start, now).seconds.coerceIn(0L, totalSeconds)
    return (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

fun programRemainingMinutes(
    program: TvProgram,
    now: LocalDateTime,
): Int {
    val remainingSeconds = Duration.between(now, program.end).seconds
    if (remainingSeconds <= 0L) return 0
    return ceil(remainingSeconds / 60.0).toInt().coerceAtLeast(1)
}

fun formatTimeHHmm(dt: LocalDateTime): String = dt.format(TIME_HH_MM_FORMATTER)

fun formatTimeRange(
    start: LocalDateTime,
    end: LocalDateTime,
): String = "${formatTimeHHmm(start)}–${formatTimeHHmm(end)}"

fun tonightWindow(now: LocalDateTime): Pair<LocalDateTime, LocalDateTime> {
    val todayEnd = now.toLocalDate().atTime(23, 30)
    val targetDate = if (now.isBefore(todayEnd)) now.toLocalDate() else now.toLocalDate().plusDays(1)
    return targetDate.atTime(20, 0) to targetDate.atTime(23, 30)
}

fun buildChannelScheduleRow(
    channel: TvChannel,
    programs: List<TvProgram>,
    now: LocalDateTime,
    maxUpcoming: Int = 3,
    maxTonight: Int = 3,
): ChannelScheduleRow {
    val realPrograms = programs.filter { !it.isFake }.sortedBy { it.start }
    val current = realPrograms.firstOrNull { isProgramAiring(it, now) }
    val upcoming =
        realPrograms
            .filter { it.id != current?.id && !it.start.isBefore(now) && it.end.isAfter(now) }
            .take(maxUpcoming)
    val (winStart, winEnd) = tonightWindow(now)
    val tonight =
        realPrograms
            .filter { it.end.isAfter(winStart) && it.start.isBefore(winEnd) }
            .take(maxTonight)
    return ChannelScheduleRow(
        channel = channel,
        current = current,
        upcoming = upcoming,
        tonight = tonight,
    )
}

fun filterChannelsByGroup(
    channels: List<TvChannel>,
    selectedGroup: String?,
): List<TvChannel> =
    when (selectedGroup) {
        null -> channels
        FAVORITES_GROUP_KEY -> channels.filter { it.favorite }
        else -> channels.filter { it.group == selectedGroup }
    }
