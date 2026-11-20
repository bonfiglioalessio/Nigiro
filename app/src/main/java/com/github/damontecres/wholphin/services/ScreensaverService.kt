package com.github.damontecres.wholphin.services

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import coil3.imageLoader
import coil3.request.ImageRequest
import com.github.damontecres.wholphin.preferences.ScreensaverPreference
import com.github.damontecres.wholphin.services.hilt.DefaultCoroutineScope
import com.github.damontecres.wholphin.ui.components.ScreensaverItem
import com.github.damontecres.wholphin.ui.formatDate
import com.github.damontecres.wholphin.ui.launchDefault
import com.github.damontecres.wholphin.util.ApiRequestPager
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.GetItemsRequestHandler
import com.github.damontecres.wholphin.util.WholphinDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles the queue of items to show on the screensaver, both in-app or OS
 */
@Singleton
class ScreensaverService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:DefaultCoroutineScope private val scope: CoroutineScope,
        private val api: ApiClient,
        private val userPreferencesService: UserPreferencesService,
        private val imageUrlService: ImageUrlService,
    ) {
        private val _state = MutableStateFlow(ScreensaverState(false, false, false, false))
        val state: StateFlow<ScreensaverState> = _state

        val keepScreenOn = MutableStateFlow(false)

        /** Started once and re-armed only when it fires, so D-pad presses don't create coroutines */
        private var timerJob: Job? = null

        @Volatile
        private var startDelayMs: Long = ScreensaverPreference.DEFAULT_START_DELAY

        @Volatile
        private var lastPulseElapsedMs: Long = SystemClock.elapsedRealtime()

        init {
            userPreferencesService.flow
                .onEach { prefs ->
                    val screensaverPrefs = prefs.appPreferences.interfacePreferences.screensaverPreference
                    startDelayMs = screensaverPrefs.startDelay
                    _state.update {
                        val enabled = screensaverPrefs.enabled
                        keepScreenOnInternal(enabled)
                        ScreensaverState(
                            enabled = enabled,
                            enabledTemp = false,
                            active = false,
                            paused = false,
                            dimEnabled = screensaverPrefs.dimEnabled,
                            dimActive = false,
                        )
                    }
                }.launchIn(scope)
        }

        /**
         * Reset the timer before showing the in-app screensaver.
         *
         * Called on every key press, so it only records the time: a single timer coroutine
         * sleeps until [startDelayMs] has passed since the last pulse.
         */
        @Synchronized
        fun pulse() {
            lastPulseElapsedMs = SystemClock.elapsedRealtime()
            val current = _state.value
            if (!current.enabled && !current.dimEnabled) return
            if (current.dimActive) {
                _state.update { it.copy(dimActive = false) }
            }
            if (!current.paused && timerJob?.isActive != true) {
                timerJob =
                    scope.launch(ExceptionHandler()) {
                        while (true) {
                            val remaining = startDelayMs - (SystemClock.elapsedRealtime() - lastPulseElapsedMs)
                            if (remaining <= 0L) break
                            delay(remaining)
                        }
                        Timber.v("Screensaver timer fired, state=%s", _state.value)
                        _state.update {
                            if (it.paused) {
                                it
                            } else {
                                it.copy(
                                    active = it.active || it.enabled,
                                    dimActive = it.dimEnabled,
                                )
                            }
                        }
                    }
            }
        }

        /**
         * Immediately start the in-app screensaver
         */
        fun start() {
            _state.update {
                it.copy(
                    enabledTemp = true,
                    active = true,
                    dimActive = true,
                )
            }
        }

        /**
         * Immediately stop the in-app screensaver
         */
        fun stop(cancelJob: Boolean) {
            _state.update {
                it.copy(
                    enabledTemp = false,
                    active = false,
                    dimActive = false,
                )
            }
            if (cancelJob) {
                timerJob?.cancel()
            }
        }

        /**
         * Signal to the OS for keeping the screen on such as during playback or when the in-app screensaver is active
         */
        fun keepScreenOn(keep: Boolean) {
            scope.launchDefault {
                val screensaverEnabled = state.value.enabled
                val dimEnabled = state.value.dimEnabled
                Timber.d("Keep screen on: %s, screensaverEnabled=%s", keep, screensaverEnabled)
                if (screensaverEnabled || dimEnabled) {
                    // Page is requesting to keep screen on, so we don't wait to show the screensaver
                    _state.update {
                        it.copy(
                            active = false,
                            dimActive = false,
                            paused = keep,
                        )
                    }
                    if (!keep) {
                        pulse()
                    }
                }
                if (!screensaverEnabled) {
                    // If in-app screensaver is not enabled, send keep screen on to the OS
                    keepScreenOnInternal(keep)
                }
            }
        }

        private fun keepScreenOnInternal(keep: Boolean) {
            keepScreenOn.update { keep }
        }

        /**
         * Create a flow of items to show on the screensaver
         */
        fun createItemFlow(scope: CoroutineScope): Flow<ScreensaverItem?> =
            flow {
                val pager =
                    try {
                        createPager()
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error creating pager for screensaver")
                        emit(ScreensaverItem.Error(ex))
                        return@flow
                    }
                Timber.v("Got %s items", pager.size)
                var index = 0
                if (pager.isEmpty()) {
                    emit(ScreensaverItem.Empty)
                } else {
                    val duration =
                        userPreferencesService
                            .getCurrent()
                            .appPreferences
                            .interfacePreferences.screensaverPreference.duration.milliseconds
                    while (true) {
                        try {
                            val item = pager.getBlocking(index)
                            Timber.v("Next index=%s, item=%s", index, item?.id)
                            if (item != null) {
                                val backdropUrl =
                                    if (item.type == BaseItemKind.PHOTO) {
                                        api.libraryApi.getDownloadUrl(item.id)
                                    } else {
                                        imageUrlService.getItemImageUrl(item, ImageType.BACKDROP)
                                    }
                                val title =
                                    if (item.type == BaseItemKind.PHOTO) {
                                        item.data.premiereDate?.let {
                                            formatDate(it.toLocalDate())
                                        }
                                    } else {
                                        item.title
                                    }
                                val logoUrl = imageUrlService.getItemImageUrl(item, ImageType.LOGO)
                                if (backdropUrl != null) {
                                    context.imageLoader
                                        .enqueue(
                                            ImageRequest
                                                .Builder(context)
                                                .data(backdropUrl)
                                                .build(),
                                        ).job
                                        .await()
                                    emit(
                                        ScreensaverItem.CurrentItem(
                                            item,
                                            backdropUrl,
                                            logoUrl,
                                            title ?: "",
                                        ),
                                    )
                                    delay(duration)
                                }
                            }
                        } catch (_: CancellationException) {
                            break
                        } catch (ex: Exception) {
                            Timber.e(ex, "Error fetching next item")
                            delay(duration)
                        }
                        index++
                        if (index > pager.lastIndex) index = 0
                    }
                }
            }.flowOn(WholphinDispatchers.Default).cancellable()

        private suspend fun createPager(): ApiRequestPager<GetItemsRequest> {
            val prefs =
                userPreferencesService.flow
                    .first()
                    .appPreferences
                    .interfacePreferences.screensaverPreference
            val maxAge = prefs.maxAgeFilter.takeIf { it >= 0 }
            val itemTypes = prefs.itemTypesList.map { BaseItemKind.fromName(it) }
            val request =
                GetItemsRequest(
                    recursive = true,
                    includeItemTypes = itemTypes,
                    imageTypes = if (BaseItemKind.PHOTO in itemTypes) null else listOf(ImageType.BACKDROP),
                    sortBy = listOf(ItemSortBy.RANDOM),
                    maxOfficialRating = maxAge?.toString(),
                    hasParentalRating = maxAge?.let { true },
                )
            return ApiRequestPager(api, request, GetItemsRequestHandler, scope).init()
        }

        companion object {
            val enterAnimation = fadeIn(animationSpec = tween(durationMillis = 1000))
            val exitAnimation = fadeOut(animationSpec = tween(durationMillis = 500))
        }
    }

data class ScreensaverState(
    val enabled: Boolean,
    val enabledTemp: Boolean,
    val active: Boolean,
    val paused: Boolean,
    val dimEnabled: Boolean = false,
    val dimActive: Boolean = false,
) {
    val show get() = (enabled || enabledTemp) && active && !paused

    val showDim get() = dimEnabled && dimActive && !paused
}
