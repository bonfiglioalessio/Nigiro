package com.github.damontecres.wholphin.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.damontecres.wholphin.ui.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

val LocalClock = compositionLocalOf<Clock> { Clock() }

/**
 * Represents the current time
 */
data class Clock(
    /**
     * The current [LocalDateTime]
     */
    val now: MutableState<LocalDateTime> = mutableStateOf(LocalDateTime.now()),
    /**
     * The current time formatted as a string; populated by [ProvideLocalClock] via [formatTime].
     */
    val timeString: MutableState<String> = mutableStateOf(""),
)

/**
 * Provides [LocalClock], updated once a minute right after the minute changes: the clock and the
 * "ends at" times only show minutes, so waking Compose every 2 seconds only recomposed their readers
 */
@Composable
fun ProvideLocalClock(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val clock =
        remember {
            val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            Clock(
                now = mutableStateOf(now),
                timeString = mutableStateOf(formatTime(context, now)),
            )
        }
    LaunchedEffect(context) {
        while (isActive) {
            val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            if (clock.now.value != now) clock.now.value = now
            val time = formatTime(context, now)
            if (clock.timeString.value != time) clock.timeString.value = time
            // A little past the next minute, so the new minute is already there
            delay(MINUTE_MS - System.currentTimeMillis() % MINUTE_MS + 50L)
        }
    }
    CompositionLocalProvider(LocalClock provides clock, content)
}

private const val MINUTE_MS = 60_000L
