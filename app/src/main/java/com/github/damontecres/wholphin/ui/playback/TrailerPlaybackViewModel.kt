package com.github.damontecres.wholphin.ui.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.ResolvedTrailerStream
import com.github.damontecres.wholphin.services.ScreensaverService
import com.github.damontecres.wholphin.services.TRAILER_USER_AGENT
import com.github.damontecres.wholphin.services.YouTubeTrailerExtractor
import com.github.damontecres.wholphin.services.hilt.StandardOkHttpClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber

sealed interface TrailerPlaybackState {
    data object Loading : TrailerPlaybackState

    data object Playing : TrailerPlaybackState

    data class FallbackToExternal(
        val reason: String?,
    ) : TrailerPlaybackState
}

@OptIn(UnstableApi::class)
@HiltViewModel(assistedFactory = TrailerPlaybackViewModel.Factory::class)
class TrailerPlaybackViewModel
    @AssistedInject
    constructor(
        @param:ApplicationContext context: Context,
        @StandardOkHttpClient okHttpClient: OkHttpClient,
        private val trailerExtractor: YouTubeTrailerExtractor,
        private val screensaverService: ScreensaverService,
        val navigationManager: NavigationManager,
        @Assisted("title") val title: String,
        @Assisted("url") val url: String,
    ) : ViewModel(),
        Player.Listener {
        @AssistedFactory
        interface Factory {
            fun create(
                @Assisted("title") title: String,
                @Assisted("url") url: String,
            ): TrailerPlaybackViewModel
        }

        private val dataSourceFactory =
            OkHttpDataSource
                .Factory(okHttpClient)
                .setUserAgent(TRAILER_USER_AGENT)

        private val trackSelector =
            DefaultTrackSelector(context).apply {
                parameters =
                    buildUponParameters()
                        .setMaxVideoSize(1920, 1080)
                        .setPreferredAudioLanguage("it")
                        .build()
            }

        val player: ExoPlayer =
            ExoPlayer
                .Builder(context)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setRenderersFactory(
                    DefaultRenderersFactory(context).setEnableDecoderFallback(true),
                ).setLoadControl(
                    DefaultLoadControl
                        .Builder()
                        .setBufferDurationsMs(8_000, 20_000, 1_500, 2_500)
                        .setTargetBufferBytes(24 * 1024 * 1024)
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build(),
                ).build()
                .apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        true,
                    )
                }

        private val _state = MutableStateFlow<TrailerPlaybackState>(TrailerPlaybackState.Loading)
        val state: StateFlow<TrailerPlaybackState> = _state

        private var loadJob: Job? = null

        init {
            player.addListener(this)
            load()
        }

        fun retry() {
            load()
        }

        private fun load() {
            loadJob?.cancel()
            _state.value = TrailerPlaybackState.Loading
            loadJob =
                viewModelScope.launch {
                    val stream = trailerExtractor.extract(url)
                    if (stream == null) {
                        _state.value = TrailerPlaybackState.FallbackToExternal(null)
                        return@launch
                    }
                    when (stream) {
                        is ResolvedTrailerStream.Single -> {
                            val mediaItem =
                                MediaItem
                                    .Builder()
                                    .setUri(stream.url)
                                    .apply { stream.mimeType?.let(::setMimeType) }
                                    .build()
                            player.setMediaItem(mediaItem)
                        }

                        is ResolvedTrailerStream.Merged -> {
                            val progressiveFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
                            val videoSource = progressiveFactory.createMediaSource(MediaItem.fromUri(stream.videoUrl))
                            val audioSource = progressiveFactory.createMediaSource(MediaItem.fromUri(stream.audioUrl))
                            player.setMediaSource(MergingMediaSource(true, videoSource, audioSource))
                        }
                    }
                    player.prepare()
                    player.playWhenReady = true
                }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            screensaverService.keepScreenOn(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    Timber.d(
                        "Trailer buffering at pos=%dms buf=%dms",
                        player.currentPosition,
                        player.bufferedPosition,
                    )
                }

                Player.STATE_READY -> {
                    _state.value = TrailerPlaybackState.Playing
                    Timber.i("Trailer ready: %dx%d", player.videoSize.width, player.videoSize.height)
                }

                Player.STATE_ENDED -> {
                    screensaverService.keepScreenOn(false)
                    navigationManager.goBack()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.w(error, "Internal trailer playback failed for %s", url)
            screensaverService.keepScreenOn(false)
            _state.value = TrailerPlaybackState.FallbackToExternal(error.localizedMessage)
        }

        override fun onCleared() {
            loadJob?.cancel()
            screensaverService.keepScreenOn(false)
            player.removeListener(this)
            player.release()
            super.onCleared()
        }
    }
