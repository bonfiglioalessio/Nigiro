package com.github.damontecres.wholphin.ui.playback

import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.Density
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ItemPlaybackDao
import com.github.damontecres.wholphin.data.ItemPlaybackRepository
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Chapter
import com.github.damontecres.wholphin.data.model.ItemPlayback
import com.github.damontecres.wholphin.data.model.Playlist
import com.github.damontecres.wholphin.data.model.PlaylistItem
import com.github.damontecres.wholphin.data.model.SeriesTrackChoice
import com.github.damontecres.wholphin.data.model.SeriesTrackChoiceType
import com.github.damontecres.wholphin.data.model.TrackIndex
import com.github.damontecres.wholphin.mpv.MpvPlayer
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.PlayerBackend
import com.github.damontecres.wholphin.preferences.ShowNextUpWhen
import com.github.damontecres.wholphin.preferences.SkipSegmentBehavior
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.preferences.enabled
import com.github.damontecres.wholphin.services.DatePlayedService
import com.github.damontecres.wholphin.services.DeviceProfileService
import com.github.damontecres.wholphin.services.ImageUrlService
import com.github.damontecres.wholphin.services.LocalTwinService
import com.github.damontecres.wholphin.services.MusicService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.PlayerFactory
import com.github.damontecres.wholphin.services.PlaylistCreationResult
import com.github.damontecres.wholphin.services.PlaylistCreator
import com.github.damontecres.wholphin.services.RefreshRateService
import com.github.damontecres.wholphin.services.ScreensaverService
import com.github.damontecres.wholphin.services.StreamChoiceService
import com.github.damontecres.wholphin.services.UserPreferencesService
import com.github.damontecres.wholphin.services.getPreferredLanguage
import com.github.damontecres.wholphin.ui.formatBitrate
import com.github.damontecres.wholphin.ui.gt
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.launchDefault
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.onMain
import com.github.damontecres.wholphin.ui.preferences.subtitle.SubtitleSettings.applyToMpv
import com.github.damontecres.wholphin.ui.seekBack
import com.github.damontecres.wholphin.ui.seekForward
import com.github.damontecres.wholphin.ui.showToast
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.LoadingState
import com.github.damontecres.wholphin.util.PlaybackErrorClassifier
import com.github.damontecres.wholphin.util.PlaybackErrorKind
import com.github.damontecres.wholphin.util.PlaybackItemState
import com.github.damontecres.wholphin.util.StreamMimeTypes
import com.github.damontecres.wholphin.util.isGelatoStream
import com.github.damontecres.wholphin.util.isStreamed
import com.github.damontecres.wholphin.util.StreamSourceParser
import com.github.damontecres.wholphin.util.TrackActivityPlaybackListener
import com.github.damontecres.wholphin.util.WholphinDispatchers
import com.github.damontecres.wholphin.util.checkForSupport
import com.github.damontecres.wholphin.util.mpv.mpvDeviceProfile
import com.github.damontecres.wholphin.util.profile.Codec
import com.github.damontecres.wholphin.util.subtitleMimeTypes
import com.github.damontecres.wholphin.util.supportItemKinds
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.mediaSegmentsApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.trickplayApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaystateCommand
import org.jellyfin.sdk.model.api.PlaystateMessage
import org.jellyfin.sdk.model.api.PlaystateRequest
import org.jellyfin.sdk.model.api.TrickplayInfo
import org.jellyfin.sdk.model.api.VideoRange
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.sdk.model.extensions.inWholeTicks
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.Collections
import java.util.Date
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * This [ViewModel] is responsible for playing media including moving through playlists (including next up episodes)
 */
@HiltViewModel(assistedFactory = PlaybackViewModel.Factory::class)
@OptIn(markerClass = [UnstableApi::class])
class PlaybackViewModel
    @AssistedInject
    constructor(
        @param:ApplicationContext internal val context: Context,
        internal val api: ApiClient,
        val navigationManager: NavigationManager,
        private val playlistCreator: PlaylistCreator,
        private val itemPlaybackDao: ItemPlaybackDao,
        internal val serverRepository: ServerRepository,
        internal val itemPlaybackRepository: ItemPlaybackRepository,
        private val playerFactory: PlayerFactory,
        private val datePlayedService: DatePlayedService,
        private val deviceInfo: DeviceInfo,
        private val deviceProfileService: DeviceProfileService,
        private val refreshRateService: RefreshRateService,
        val streamChoiceService: StreamChoiceService,
        private val userPreferencesService: UserPreferencesService,
        private val imageUrlService: ImageUrlService,
        private val screensaverService: ScreensaverService,
        private val musicService: MusicService,
        private val localTwinService: LocalTwinService,
        @Assisted private val destination: Destination,
    ) : ViewModel(),
        Player.Listener,
        AnalyticsListener {
        @AssistedFactory
        interface Factory {
            fun create(destination: Destination): PlaybackViewModel
        }

        val currentPlayer = MutableStateFlow<PlayerInstance?>(null)

        internal lateinit var player: Player

        private var mediaSession: MediaSession? = null

        // Mutex & Job for changing playlist index
        private val playlistMutex = Mutex()
        private var playlistJob: Job? = null

        val controllerViewState =
            ControllerViewState(
                AppPreference.ControllerTimeout.defaultValue,
                true,
            )

        private val _state = MutableStateFlow(PlaybackState())
        val state: StateFlow<PlaybackState> = _state

        internal lateinit var preferences: UserPreferences
        internal lateinit var itemId: UUID
        internal lateinit var currentItem: PlaylistItem
        internal var forceTranscoding: Boolean = false

        // Media sources of the current item that failed to play, reset when a new item starts
        private val failedSourceIds: MutableSet<String?> = Collections.synchronizedSet(mutableSetOf())
        /** Why the sources ran out, shown on the error page instead of the generic message */
        private var sourceErrorMessage: String? = null

        // Media sources of the current item where direct play failed so transcoding is used instead
        private val transcodeOnlySourceIds: MutableSet<String?> = Collections.synchronizedSet(mutableSetOf())

        // Last known playback position, used to resume after an error
        @Volatile
        private var lastPositionMs: Long = 0L

        private var activityListener: TrackActivityPlaybackListener? = null
        private var trackChangeListener: TracksChangedListener? = null
        private val jobs = mutableListOf<Job>()
        private var subscribeJob: Job? = null

        private val isPlaylist = destination is Destination.PlaybackList

        val subtitleSearchState = MutableStateFlow(SubtitleSearchState())

        val currentUserDto = serverRepository.currentUserDtoFlow

        @VisibleForTesting
        val initJob: Job

        init {
            initJob =
                viewModelScope.launchIO {
                    addCloseable {
                        screensaverService.keepScreenOn(false)
                        disconnectPlayer()
                    }
                    runCatchingPlayback { init() }
                }
        }

        private fun disconnectPlayer() {
            if (this@PlaybackViewModel::player.isInitialized) {
                player.removeListener(this@PlaybackViewModel)
                (player as? ExoPlayer)?.removeAnalyticsListener(this@PlaybackViewModel)

                this@PlaybackViewModel.activityListener?.let {
                    it.release()
                    player.removeListener(it)
                }
                this@PlaybackViewModel.trackChangeListener?.let {
                    player.removeListener(it)
                }
                player.release()
                mediaSession?.release()
            }
            jobs.forEach { it.cancel() }
        }

        private suspend fun createPlayer(
            isHdr: Boolean,
            is4k: Boolean,
        ) {
            val softwareDecoding =
                !preferences.appPreferences.playbackPreferences.mpvOptions.enableHardwareDecoding
            val requestBackend =
                (destination as? Destination.Playback)?.backend
                    ?: preferences.appPreferences.playbackPreferences.playerBackend
            val playerBackend =
                when (requestBackend) {
                    PlayerBackend.UNRECOGNIZED,
                    PlayerBackend.EXO_PLAYER,
                    -> PlayerBackend.EXO_PLAYER

                    PlayerBackend.MPV -> PlayerBackend.MPV

                    PlayerBackend.PREFER_MPV -> if (isHdr || (is4k && softwareDecoding)) PlayerBackend.EXO_PLAYER else PlayerBackend.MPV

                    PlayerBackend.EXTERNAL_PLAYER -> throw IllegalStateException("Cannot use this for external playback")
                }

            Timber.d("Selected backend: %s", playerBackend)
            if (currentPlayer.value?.backend != playerBackend) {
                Timber.i("Switching player backend to %s", playerBackend)
                withContext(WholphinDispatchers.Main) {
                    disconnectPlayer()
                }

                val playerCreation =
                    playerFactory.createVideoPlayer(
                        playerBackend,
                        preferences.appPreferences,
                    )
                this.player = playerCreation.player
                currentPlayer.update {
                    PlayerInstance(playerCreation.player, playerBackend, playerCreation.assHandler)
                }
                configurePlayer()
            }
        }

        private fun configurePlayer() {
            player.addListener(this)
            (player as? ExoPlayer)?.addAnalyticsListener(this)
            subscribeToWebSocket()
            jobs.add(listenForTranscodeReason())
            val sessionPlayer =
                MediaSessionPlayer(
                    player,
                    preferences.appPreferences.playbackPreferences,
                )
            mediaSession = playerFactory.createMediaSession(sessionPlayer)
        }

        /**
         * Initialize from the UI to start playback
         */
        private suspend fun init() {
            musicService.stop()
            this.preferences = userPreferencesService.getCurrent()
            val maxBitrate =
                preferences.appPreferences.playbackPreferences.maxBitrate
                    .takeIf { it > 0 } ?: AppPreference.DEFAULT_BITRATE
            _state.update {
                it.copy(
                    nextUp = null,
                    maxBitrate = maxBitrate,
                )
            }
            if (preferences.appPreferences.playbackPreferences.refreshRateSwitching) {
                addCloseable { refreshRateService.resetRefreshRate() }
            }
            controllerViewState.hideMilliseconds =
                preferences.appPreferences.playbackPreferences.controllerTimeoutMs
            this.forceTranscoding =
                (destination as? Destination.Playback)?.forceTranscoding ?: false
            val positionMs: Long
            val forceTranscoding: Boolean

            val itemId =
                when (val d = destination) {
                    is Destination.Playback -> {
                        positionMs = d.positionMs
                        forceTranscoding = d.forceTranscoding
                        d.itemId
                    }

                    is Destination.PlaybackList -> {
                        positionMs = 0
                        forceTranscoding = false
                        d.itemId
                    }

                    else -> {
                        throw IllegalArgumentException("Destination not supported: $destination")
                    }
                }
            this.itemId = itemId
            val queriedItem = api.userLibraryApi.getItem(itemId).content
            val playlistItem =
                if (queriedItem.type.playable) {
                    PlaylistItem.Media(BaseItem(queriedItem, false))
                } else {
                    val playlistResult =
                        if (destination is Destination.PlaybackList) {
                            playlistCreator.createFrom(
                                item = queriedItem,
                                startIndex = destination.startIndex ?: 0,
                                sortAndDirection = destination.sortAndDirection,
                                shuffled = destination.shuffle,
                                recursive = destination.recursive,
                                filter = destination.filter,
                            )
                        } else {
                            val shuffled =
                                if (destination is Destination.Playback) {
                                    destination.shuffle
                                } else {
                                    false
                                }
                            // Try to create a playlist
                            playlistCreator.createFrom(
                                item = queriedItem,
                                recursive = true,
                                shuffled = shuffled,
                            )
                        }
                    when (val r = playlistResult) {
                        is PlaylistCreationResult.Error -> {
                            _state.update { it.copy(loading = LoadingState.Error(r.message, r.ex)) }
                            return
                        }

                        is PlaylistCreationResult.Success -> {
                            if (r.playlist.items.isEmpty()) {
                                showToast(context, "Playlist is empty", Toast.LENGTH_SHORT)
                                navigationManager.goBack()
                                return
                            }
                            _state.update {
                                it.copy(playlist = r.playlist)
                            }
                            r.playlist.items.first()
                        }
                    }
                }

            viewModelScope.launch(ExceptionHandler()) { controllerViewState.observe() }

            val intros =
                // If not resuming playback & cinema mode is enabled, get potential intros
                if (positionMs == 0L && preferences.appPreferences.playbackPreferences.cinemaMode) {
                    api.userLibraryApi
                        .getIntros(
                            itemId = playlistItem.id,
                            userId = serverRepository.currentUser?.id,
                        ).content.items
                        .map {
                            PlaylistItem.Intro(BaseItem(it))
                        }
                } else {
                    emptyList()
                }
            val firstItem =
                if (intros.isNotEmpty()) {
                    Timber.v("Got %s intros", intros.size)
                    _state.update {
                        it.copy(playlist = Playlist(intros + it.playlist.items))
                    }
                    intros.first()
                } else {
                    playlistItem
                }

            val played =
                play(
                    firstItem,
                    positionMs,
                    forceTranscoding,
                )
            if (!played) {
                playNextUp()
            }

            if (!isPlaylist) {
                val result = playlistCreator.createFrom(queriedItem)
                if (result is PlaylistCreationResult.Success && result.playlist.items.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            playlist = Playlist(it.playlist.items + result.playlist.items),
                        )
                    }
                }
            }
        }

        /**
         * A streamed (Gelato) movie that was also downloaded to the server plays from the local file,
         * unless the user explicitly chose one of the streamed versions
         */
        private suspend fun preferLocalCopy(playlistItem: PlaylistItem): PlaylistItem {
            if (playlistItem !is PlaylistItem.Media) return playlistItem
            val dto = preferLocalCopy(playlistItem.item.data)
            return if (dto.id != playlistItem.item.id) {
                PlaylistItem.Media(BaseItem(dto, playlistItem.item.useSeriesForPrimary))
            } else {
                playlistItem
            }
        }

        private suspend fun preferLocalCopy(item: BaseItemDto): BaseItemDto {
            if (!localTwinService.isStreamOnly(item)) return item
            return try {
                val chosenSourceId =
                    serverRepository.currentUser
                        ?.let { itemPlaybackDao.getItem(it, item.id) }
                        ?.sourceId
                val choseStream =
                    chosenSourceId != null &&
                        item.mediaSources.orEmpty().any { it.id?.toUUIDOrNull() == chosenSourceId }
                if (choseStream) {
                    item
                } else {
                    localTwinService.findLocalTwin(item)?.also {
                        Timber.i("Playing local copy %s instead of streamed item %s", it.id, item.id)
                    } ?: item
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.w(ex, "Could not check for a local copy of %s", item.id)
                item
            }
        }

        internal fun updateCurrentPlayback(block: (CurrentPlayback?) -> CurrentPlayback?) {
            _state.update {
                it.copy(currentPlayback = block.invoke(it.currentPlayback))
            }
        }

        /**
         * Play an item
         *
         * @param currentItem the item to play
         * @param positionMs the starting playback position in milliseconds
         * @param itemPlayback the parameters for playback such chosen subtitle or audio streams
         * @param forceTranscoding whether the user has requested to force playback via transcoding
         */
        private suspend fun play(
            playlistItem: PlaylistItem,
            positionMs: Long,
            forceTranscoding: Boolean = this.forceTranscoding,
        ): Boolean =
            withContext(WholphinDispatchers.IO) {
                // A streamed (Gelato) movie/episode that was also downloaded plays from the local file, while the
                // playlist stays the streamed one so next up keeps working for episodes not downloaded yet
                val toPlay = preferLocalCopy(playlistItem)
                val item =
                    when (toPlay) {
                        is PlaylistItem.Intro -> toPlay.item
                        is PlaylistItem.Media -> toPlay.item
                    }

                Timber.i("Playing ${item.id}")

                // New item, so we can clear the media segment tracker, subtitle cues & failed sources
                resetSegmentState()
                failedSourceIds.clear()
                transcodeOnlySourceIds.clear()
                sourceErrorMessage = null
                lastPositionMs = positionMs.coerceAtLeast(0L)
                _state.update { it.copy(subtitleCues = emptyList()) }

                viewModelScope.launchIO {
                    // Starting playback, so want to invalidate the last played timestamp for this item
                    datePlayedService.invalidate(item)
                }

                if (item.type !in supportItemKinds) {
                    showToast(
                        context,
                        "Unsupported type '${item.type}', skipping...",
                        Toast.LENGTH_SHORT,
                    )
                    return@withContext false
                }
                this@PlaybackViewModel.currentItem = toPlay
                this@PlaybackViewModel.itemId = item.id

                val isLiveTv = item.type == BaseItemKind.TV_CHANNEL
                val base = item.data

                // Use the provided playback parameters or else check if the database has some
                val itemPlayback =
                    serverRepository.currentUser?.let { user ->
                        itemPlaybackDao.getItem(user, base.id)?.let {
                            Timber.v("Fetched itemPlayback from DB: %s", it)
                            if (it.sourceId != null) {
                                it
                            } else {
                                null
                            }
                        }
                    }
                val mediaSource =
                    if (!isLiveTv) {
                        streamChoiceService.chooseSource(base, itemPlayback)
                    } else {
                        null
                    }

                val audioStc =
                    streamChoiceService.getSeriesTrackChoices(
                        base,
                        SeriesTrackChoiceType.AUDIO,
                    )
                val subtitleStc =
                    streamChoiceService.getSeriesTrackChoices(
                        base,
                        SeriesTrackChoiceType.SUBTITLE,
                    )

                // Resuming later takes the same source, unless a version gets chosen in the picker
                if (itemPlayback == null && mediaSource != null) {
                    streamChoiceService.rememberPlayedSource(base.id, mediaSource.id)
                }
                // Nothing in Italian to stream: it plays, but says so before the original language starts. Only
                // AIOStreams streams are sure to name their language, so others never raise a false alarm
                if (mediaSource != null && mediaSource.isGelatoStream() &&
                    base.mediaSources.orEmpty().none { StreamSourceParser.parse(it).italian }
                ) {
                    showToast(context, context.getString(R.string.playback_no_italian_source), Toast.LENGTH_LONG)
                }

                if (mediaSource == null && !isLiveTv) {
                    showToast(
                        context,
                        context.getString(R.string.playback_no_media_sources),
                        Toast.LENGTH_SHORT,
                    )
                    return@withContext false
                }

                startSource(
                    item = item,
                    mediaSource = mediaSource,
                    itemPlayback = itemPlayback,
                    audioStc = audioStc,
                    subtitleStc = subtitleStc,
                    positionMs = positionMs,
                    forceTranscoding = forceTranscoding,
                )
                return@withContext true
            }

        /**
         * Starts playing [item] from [mediaSource], choosing the audio & subtitle streams
         */
        private suspend fun startSource(
            item: BaseItem,
            mediaSource: MediaSourceInfo?,
            itemPlayback: ItemPlayback?,
            audioStc: List<SeriesTrackChoice>?,
            subtitleStc: List<SeriesTrackChoice>?,
            positionMs: Long,
            forceTranscoding: Boolean,
        ): Unit =
            withContext(WholphinDispatchers.IO) {
                val base = item.data
                val videoStream =
                    mediaSource
                        ?.mediaStreams
                        ?.firstOrNull { it.type == MediaStreamType.VIDEO }
                        ?.let {
                            val isHdr =
                                it.videoRange == VideoRange.HDR ||
                                    (it.videoRangeType != VideoRangeType.SDR && it.videoRangeType != VideoRangeType.UNKNOWN)
                            // Often times 4k movies have a wider aspect ratio so the height is lower even though the width is still 3840
                            val is4k = (it.width ?: 0) > 2560 || (it.height ?: 0) > 1440
                            SimpleVideoStream(it.index, isHdr, is4k)
                        }

                // Create the correct player for the media
                createPlayer(videoStream?.hdr == true, videoStream?.is4k == true)

                val subtitleStreams = mediaSource?.let { getSubtitleStreams(mediaSource) }.orEmpty()
                val audioStreams = mediaSource?.let { getAudioStreams(mediaSource) }.orEmpty()
                val audioResult =
                    mediaSource?.let {
                        streamChoiceService
                            .chooseAudioStream(
                                source = mediaSource,
                                item = base,
                                itemPlayback = itemPlayback,
                                stc = audioStc,
                                prefs = preferences,
                            )
                    }
                val audioIndex = audioResult?.stream?.index

                val subtitleIndex =
                    mediaSource?.let {
                        streamChoiceService
                            .chooseSubtitleStream(
                                item = base,
                                source = mediaSource,
                                audioStream = audioResult?.stream,
                                itemPlayback = itemPlayback,
                                stc = subtitleStc,
                                prefs = preferences,
                            )?.stream
                            ?.index
                    }
                Timber.d(
                    "Selected mediaSource=%s, audioIndex=%s, subtitleIndex=%s",
                    mediaSource?.id,
                    audioIndex,
                    subtitleIndex,
                )

                val trickPlayInfo =
                    mediaSource?.id?.let { sourceId ->
                        item.data.trickplay
                            ?.get(sourceId)
                            ?.values
                            ?.firstOrNull()
                    }

                trickPlayInfo?.let { trickplayInfo ->
                    mediaSource.runTimeTicks?.ticks?.let { duration ->
                        viewModelScope.launchIO {
                            prefetchTrickplay(
                                duration,
                                trickplayInfo,
                                mediaSource.id?.toUUIDOrNull(),
                            )
                        }
                    }
                }

                val chapters = Chapter.fromDto(base)
                _state.update {
                    it.copy(
                        currentItemPlayback = itemPlayback,
                        currentPlayback = null,
                    )
                }
                updateCurrentMedia {
                    CurrentMediaInfo(
                        sourceId = mediaSource?.id,
                        videoStream = videoStream,
                        audioStreams = audioStreams,
                        subtitleStreams = subtitleStreams,
                        chapters = chapters,
                        trickPlayInfo = trickPlayInfo,
                    )
                }
                val directAllowed = !forceTranscoding && mediaSource?.id !in transcodeOnlySourceIds
                val preferredAudioLanguage =
                    getPreferredLanguage(
                        MediaStreamType.AUDIO,
                        preferences,
                        serverRepository.currentUserDto?.configuration,
                    )
                withContext(WholphinDispatchers.Main) {
                    // When the server doesn't know the audio streams (eg unprobed remote sources),
                    // let ExoPlayer pick the track in the preferred language instead of the file's default
                    val exoPlayer = player as? ExoPlayer
                    if (exoPlayer != null && preferredAudioLanguage.isNotNullOrBlank()) {
                        exoPlayer.trackSelectionParameters =
                            exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setPreferredAudioLanguages(preferredAudioLanguage)
                                .build()
                    }
                    val started =
                        changeStreams(
                            item = item,
                            audioIndex = audioIndex,
                            subtitleIndex = subtitleIndex,
                            positionMs = if (positionMs > 0) positionMs else C.TIME_UNSET,
                            sourceId = mediaSource?.id,
                            enableDirectPlay = directAllowed,
                            enableDirectStream = directAllowed,
                            allowSourceFailover = true,
                        )
                    // If not started, another source was tried instead or an error is shown
                    if (started) {
                        player.prepare()
                        player.play()
                    }
                }
                listenForSegments(item.id)
            }

        /**
         * Whether direct play/stream may be attempted for the given media source of the current item
         */
        private fun directPlayAllowed(sourceId: String?): Boolean = !forceTranscoding && sourceId !in transcodeOnlySourceIds

        /** A Gelato HLS stream the player can read itself, unless it already failed that way */
        private fun isDirectHlsCandidate(source: MediaSourceInfo): Boolean =
            source.isGelatoStream() &&
                source.container.equals(Codec.Container.HLS, ignoreCase = true) &&
                source.id !in transcodeOnlySourceIds

        /**
         * Marks [failedSourceId] as failed and starts the next best media source of the current item, if any remain
         *
         * @return true if another media source is being started
         */
        private suspend fun tryNextSource(
            failedSourceId: String?,
            positionMs: Long,
        ): Boolean {
            if (!this::currentItem.isInitialized) return false
            failedSourceId?.let { failedSourceIds.add(it) }
            if (failedSourceIds.size >= MAX_SOURCE_ATTEMPTS) {
                Timber.w("Giving up after %s failed media sources", failedSourceIds.size)
                return false
            }
            val item = currentItem.item
            val sources = item.data.mediaSources.orEmpty()
            val remaining = sources.filter { it.id.isNotNullOrBlank() && it.id !in failedSourceIds }
            // Italian only: when the item has Italian sources and none of them is left, playback stops and says
            // so rather than carrying on in another language
            val hasItalian = sources.any { StreamSourceParser.parse(it).italian }
            val candidates = if (hasItalian) remaining.filter { StreamSourceParser.parse(it).italian } else remaining
            if (hasItalian && candidates.isEmpty()) {
                Timber.w("No Italian source left for %s after %s failed", item.id, failedSourceId)
                sourceErrorMessage = context.getString(R.string.playback_no_italian_source_left)
                return false
            }
            val next = streamChoiceService.chooseSource(candidates, item.data.runTimeTicks) ?: return false
            Timber.w("Media source %s failed, trying %s", failedSourceId, next.id)
            // The failed one would come back on resume: remember the one that is tried now
            streamChoiceService.rememberPlayedSource(item.id, next.id)
            showToast(
                context,
                context.getString(R.string.playback_source_unavailable_trying_next),
                Toast.LENGTH_SHORT,
            )
            startSource(
                item = item,
                mediaSource = next,
                itemPlayback = null,
                audioStc = null,
                subtitleStc = null,
                positionMs = positionMs,
                forceTranscoding = forceTranscoding,
            )
            return true
        }

        /**
         * A media source could not be started: try another one or else show an error
         */
        private suspend fun handleSourceFailure(
            sourceId: String?,
            positionMs: Long,
            exception: Throwable?,
            message: String? = null,
        ) {
            val position = positionMs.coerceAtLeast(0L)
            lastPositionMs = position
            if (!tryNextSource(sourceId, position)) {
                setPlaybackError(exception, message)
            }
        }

        private suspend fun setPlaybackError(
            exception: Throwable?,
            message: String? = null,
        ) {
            onMain {
                if (this@PlaybackViewModel::player.isInitialized) {
                    lastPositionMs = player.currentPosition.coerceAtLeast(lastPositionMs)
                    player.stop()
                }
            }
            _state.update {
                it.copy(
                    loading =
                        LoadingState.Error(
                            message ?: sourceErrorMessage ?: context.getString(R.string.playback_error_generic),
                            exception,
                        ),
                )
            }
        }

        /**
         * Runs [block], showing the playback error page if it throws
         */
        private suspend fun runCatchingPlayback(block: suspend () -> Unit) {
            try {
                block()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.e(ex, "Error during playback")
                setPlaybackError(ex)
            }
        }

        /**
         * The media sources of the current item which the user can choose from
         */
        fun availableSources(): List<MediaSourceInfo> =
            if (this::currentItem.isInitialized) {
                currentItem.item.data.mediaSources
                    .orEmpty()
                    .filter { it.id.isNotNullOrBlank() }
            } else {
                emptyList()
            }

        /**
         * Retry playing the current item, eg after an error
         */
        fun retry() {
            viewModelScope.launchIO {
                _state.update { it.copy(loading = LoadingState.Loading) }
                runCatchingPlayback {
                    if (this@PlaybackViewModel::currentItem.isInitialized) {
                        if (!play(currentItem, lastPositionMs)) {
                            setPlaybackError(null)
                        }
                    } else {
                        init()
                    }
                }
            }
        }

        /**
         * Switch to another version of the current item, keeping the playback position
         */
        fun switchVersion(sourceId: String) {
            viewModelScope.launchIO {
                if (this@PlaybackViewModel::player.isInitialized) {
                    lastPositionMs = onMain { player.currentPosition }.coerceAtLeast(0L)
                }
                playSource(sourceId)
            }
        }

        /**
         * Play the current item using the media source chosen by the user
         */
        fun playSource(sourceId: String) {
            viewModelScope.launchIO {
                if (!this@PlaybackViewModel::currentItem.isInitialized) return@launchIO
                val source =
                    currentItem.item.data.mediaSources
                        ?.firstOrNull { it.id == sourceId } ?: return@launchIO
                _state.update { it.copy(loading = LoadingState.Loading) }
                failedSourceIds.clear()
                runCatchingPlayback {
                    startSource(
                        item = currentItem.item,
                        mediaSource = source,
                        itemPlayback = null,
                        audioStc = null,
                        subtitleStc = null,
                        positionMs = lastPositionMs,
                        forceTranscoding = forceTranscoding,
                    )
                }
            }
        }

        /**
         * Change which streams (ie audio or subtitle) are active
         *
         * @param allowSourceFailover if the media source cannot be started, try another source of the item instead of failing
         * @return true if the player has new media to prepare (or the tracks were switched in place)
         */
        @OptIn(UnstableApi::class)
        internal suspend fun changeStreams(
            item: BaseItem,
            sourceId: String?,
            audioIndex: Int?,
            subtitleIndex: Int?,
            positionMs: Long = 0,
            enableDirectPlay: Boolean = !this.forceTranscoding,
            enableDirectStream: Boolean = !this.forceTranscoding,
            maxBitrate: Long = this.state.value.maxBitrate,
            allowSourceFailover: Boolean = false,
        ): Boolean =
            withContext(WholphinDispatchers.IO) {
                val itemId = item.id

                trackChangeListener?.let { onMain { player.removeListener(it) } }
                trackChangeListener = null

                state.value.currentPlayback?.let { currentPlayback ->
                    if (currentPlayback.item.id == item.id &&
                        currentPlayback.mediaSourceInfo.id == sourceId &&
                        currentPlayback.playMethod == PlayMethod.DIRECT_PLAY &&
                        enableDirectPlay &&
                        state.value.maxBitrate == maxBitrate
                    ) {
                        val wasSuccessful =
                            changeStreamsDirectPlay(
                                currentPlayback = currentPlayback,
                                audioIndex = audioIndex,
                                subtitleIndex = subtitleIndex,
                            )
                        if (wasSuccessful) return@withContext true
                    }
                }

                Timber.i(
                    "changeStreams (%s): audioIndex=%s, subtitleIndex=%s, enableDirectPlay=%s, enableDirectStream=%s, positionMs=%s, maxBitrate=%s",
                    itemId,
                    audioIndex,
                    subtitleIndex,
                    enableDirectPlay,
                    enableDirectStream,
                    positionMs,
                    maxBitrate,
                )
                _state.update { it.copy(maxBitrate = maxBitrate) }

                val response =
                    try {
                        api.mediaInfoApi
                            .getPostedPlaybackInfo(
                                itemId,
                                PlaybackInfoDto(
                                    startTimeTicks = null,
                                    deviceProfile =
                                        if (currentPlayer.value!!.backend == PlayerBackend.EXO_PLAYER) {
                                            deviceProfileService.getOrCreateDeviceProfile(
                                                preferences.appPreferences,
                                                serverRepository.currentServer?.serverVersion,
                                            )
                                        } else {
                                            mpvDeviceProfile
                                        },
                                    maxAudioChannels = null,
                                    audioStreamIndex = audioIndex,
                                    subtitleStreamIndex = subtitleIndex,
                                    mediaSourceId = sourceId,
                                    alwaysBurnInSubtitleWhenTranscoding = false,
                                    maxStreamingBitrate = maxBitrate.toInt(),
                                    enableDirectPlay = enableDirectPlay,
                                    enableDirectStream = enableDirectStream,
                                    allowVideoStreamCopy = enableDirectStream,
                                    allowAudioStreamCopy = enableDirectStream,
                                    enableTranscoding = true,
                                    autoOpenLiveStream = true,
                                ),
                            ).content
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error in PostedPlaybackInfo for source %s", sourceId)
                        if (allowSourceFailover) {
                            handleSourceFailure(sourceId, positionMs, ex)
                        } else {
                            // Eg changing tracks, the current stream keeps playing
                            showToast(
                                context,
                                context.getString(R.string.playback_error_change_streams),
                                Toast.LENGTH_SHORT,
                            )
                        }
                        return@withContext false
                    }
                if (response.errorCode != null) {
                    Timber.e("Error in PostedPlaybackInfo: %s", response.errorCode)
                    if (allowSourceFailover) {
                        handleSourceFailure(sourceId, positionMs, null, response.errorCode?.serialName)
                    } else {
                        setPlaybackError(null, response.errorCode?.serialName)
                    }
                    return@withContext false
                }
                val source = response.mediaSources.firstOrNull()
                if (source == null) {
                    Timber.e("PostedPlaybackInfo returned no media sources for %s", sourceId)
                    if (allowSourceFailover) {
                        handleSourceFailure(sourceId, positionMs, null)
                    } else {
                        setPlaybackError(null)
                    }
                    return@withContext false
                }
                source.let { source ->
                    val mediaInfo = state.value.currentMediaInfo
                    if (mediaInfo.sourceId == sourceId &&
                        mediaInfo.audioStreams.isEmpty() &&
                        mediaInfo.subtitleStreams.isEmpty() &&
                        !source.mediaStreams.isNullOrEmpty()
                    ) {
                        // Remote sources are often only probed when playback is requested
                        updateCurrentMedia {
                            it.copy(
                                audioStreams = getAudioStreams(source),
                                subtitleStreams = getSubtitleStreams(source),
                            )
                        }
                    }
                    // Gelato's web streams (StreamVix/VixSrc) are HLS playlists of public CDN segments: Jellyfin never
                    // offers them for direct play and remuxes them with ffmpeg on the server instead, which costs
                    // seconds at every start. The static stream is that playlist, which the player reads itself.
                    val directHls = enableDirectPlay && !source.supportsDirectPlay && isDirectHlsCandidate(source)
                    if (directHls) Timber.i("Playing HLS source %s directly instead of a server remux", source.id)
                    val mediaUrl =
                        if (source.supportsDirectPlay || directHls) {
                            if (source.isRemote && source.path.isNotNullOrBlank()) {
                                Timber.i("Playback is remote for source: %s", source.id)
                                source.path
                            } else {
                                api.videosApi.getVideoStreamUrl(
                                    itemId = itemId,
                                    mediaSourceId = source.id,
                                    liveStreamId = source.liveStreamId,
                                    static = true,
                                    tag = source.eTag,
                                    playSessionId = response.playSessionId,
                                )
                            }
                        } else if (source.supportsDirectStream) {
                            source.transcodingUrl?.let(api::createUrl)
                        } else {
                            source.transcodingUrl?.let(api::createUrl)
                        }
                    if (mediaUrl.isNullOrBlank()) {
                        val message = context.getString(R.string.playback_error_no_media_url)
                        if (allowSourceFailover) {
                            handleSourceFailure(sourceId, positionMs, null, message)
                        } else {
                            setPlaybackError(null, message)
                        }
                        return@withContext false
                    }
                    val transcodeType =
                        when {
//                        playerBackend == PlayerBackend.MPV -> PlayMethod.DIRECT_PLAY
                            source.supportsDirectPlay || directHls -> PlayMethod.DIRECT_PLAY

                            source.supportsDirectStream -> PlayMethod.DIRECT_STREAM

                            source.supportsTranscoding -> PlayMethod.TRANSCODE

                            else -> throw Exception("No supported playback method")
                        }
                    Timber.i("Playback decision for $itemId: $transcodeType")

                    val externalSubtitleCount = source.externalSubtitlesCount

                    val externalSubtitle =
                        source.findExternalSubtitle(subtitleIndex)?.let {
                            Timber.v("Using externally delivered subtitle tracks %s", subtitleIndex)
                            it.deliveryUrl?.let { deliveryUrl ->
                                var flags = 0
                                if (it.isForced) flags = flags.or(C.SELECTION_FLAG_FORCED)
                                if (it.isDefault) flags = flags.or(C.SELECTION_FLAG_DEFAULT)
                                MediaItem.SubtitleConfiguration
                                    .Builder(
                                        api.createUrl(deliveryUrl).toUri(),
                                    ).setId("e:${it.index}")
                                    .setMimeType(subtitleMimeTypes[it.codec])
                                    .setLanguage(it.language)
                                    .setLabel(it.title)
                                    .setSelectionFlags(flags)
                                    .build()
                            }
                        }

                    Timber.v(
                        "subtitleIndex=$subtitleIndex, externalSubtitleCount=$externalSubtitleCount, externalSubtitle=$externalSubtitle",
                    )

                    val mediaItem =
                        MediaItem
                            .Builder()
                            .setMediaId(itemId.toString())
                            .setMediaMetadata(
                                item.toMediaMetadata(
                                    imageUrlService.getItemImageUrl(
                                        item,
                                        ImageType.PRIMARY,
                                        useSeriesForPrimary = true,
                                    ),
                                ),
                            ).setUri(mediaUrl.toUri())
                            .setSubtitleConfigurations(listOfNotNull(externalSubtitle))
                            .apply {
                                StreamMimeTypes
                                    .infer(
                                        container = source.container,
                                        isRemote = source.isRemote && transcodeType == PlayMethod.DIRECT_PLAY,
                                        url = mediaUrl,
                                    )?.let(::setMimeType)
                            }.build()

                    val playback =
                        CurrentPlayback(
                            item = item,
                            tracks = listOf(),
                            backend = currentPlayer.value!!.backend,
                            playMethod = transcodeType,
                            playSessionId = response.playSessionId,
                            liveStreamId = source.liveStreamId,
                            mediaSourceInfo = source,
                            audioIndex = audioIndex ?: TrackIndex.DISABLED,
                            subtitleIndex = subtitleIndex ?: TrackIndex.DISABLED,
                        )

                    preferences.appPreferences.playbackPreferences.let { prefs ->
                        source.mediaStreams
                            ?.firstOrNull { it.type == MediaStreamType.VIDEO }
                            ?.let { stream ->
                                refreshRateService.changeRefreshRate(
                                    stream = stream,
                                    switchRefreshRate = prefs.refreshRateSwitching,
                                    switchResolution = prefs.resolutionSwitching,
                                )
                            }
                    }
                    withContext(WholphinDispatchers.Main) {
                        // TODO, don't need to release & recreate when switching streams
                        this@PlaybackViewModel.activityListener?.let {
                            it.release()
                            player.removeListener(it)
                        }

                        val playbackItemState = PlaybackItemState(playback)
                        val activityListener =
                            TrackActivityPlaybackListener(
                                api = api,
                                player = player,
                                getState = { playbackItemState },
                            )
                        player.addListener(activityListener)
                        this@PlaybackViewModel.activityListener = activityListener

                        _state.update {
                            it.copy(
                                loading = LoadingState.Success,
                                currentPlayback = playback,
                            )
                        }
                        player.setMediaItem(
                            mediaItem,
                            positionMs,
                        )
                        val onFailure: () -> Unit = {
                            if (player is MpvPlayer && externalSubtitle != null) {
                                // MpvPlayer may change tracks more than once to add external subtitles
                                trackChangeListener?.let { player.addListener(it) }
                            } else if (source.isStreamed()) {
                                // The server's stream indexes often don't match a remote (debrid/web) file, nor the
                                // renditions of an HLS playlist; Gelato's streams count too, though not "remote".
                                // Keep direct play with the player's own choice (preferred language) rather than
                                // making the server remux the remote URL
                                Timber.w("Could not map tracks of remote source %s, keeping direct play", source.id)
                            } else {
                                viewModelScope.launchIO {
                                    changeStreams(
                                        item = item,
                                        sourceId = sourceId,
                                        audioIndex = audioIndex,
                                        subtitleIndex = subtitleIndex,
                                        positionMs = positionMs,
                                        enableDirectPlay = false,
                                        enableDirectStream = true,
                                    )
                                }
                            }
                        }
                        val onTracksChangedListener =
                            if (transcodeType == PlayMethod.DIRECT_PLAY && (audioIndex != null || subtitleIndex != null)) {
                                TracksChangedListener(
                                    player = player,
                                    audioIndex = audioIndex,
                                    subtitleIndex = subtitleIndex,
                                    source = source,
                                    onFailure = onFailure,
                                )
                            } else if (externalSubtitle != null) {
                                TracksChangedListener(
                                    player = player,
                                    audioIndex = null, // Do not manually activate audio
                                    subtitleIndex = subtitleIndex,
                                    source = source,
                                    onFailure = onFailure,
                                )
                            } else {
                                null
                            }
                        onTracksChangedListener?.let {
                            player.addListener(it)
                            this@PlaybackViewModel.trackChangeListener = it
                        }

                        if (subtitleIndex != null) {
                            viewModelScope.launchIO { loadSubtitleDelay() }
                        }
                    }
                    true
                }
            }

        /**
         * If direct playing, can try to switch tracks without playback restarting
         * Except for external subtitles
         */
        @OptIn(UnstableApi::class)
        private suspend fun changeStreamsDirectPlay(
            currentPlayback: CurrentPlayback,
            audioIndex: Int?,
            subtitleIndex: Int?,
        ): Boolean =
            withContext(WholphinDispatchers.IO) {
                Timber.v("changeStreams direct play")

                // TODO Better way to handle unsupported types in general is needed
                // This is a workaround for switching to a non AC3 track when the user wants audio transcoded to AC3
                if (preferences.appPreferences.experimentalPreferences.enabled { preferAc3Surround } && audioIndex != null) {
                    currentPlayback.mediaSourceInfo.mediaStreams
                        .orEmpty()
                        .firstOrNull { it.index == audioIndex }
                        ?.let {
                            if (it.channels.gt(2) && it.codec != Codec.Audio.AC3) {
                                // User wants to transcode audio into AC3
                                return@withContext false
                            }
                        }
                }

                val source = currentPlayback.mediaSourceInfo
                val externalSubtitle = source.findExternalSubtitle(subtitleIndex)

                if (externalSubtitle == null) {
                    val result =
                        withContext(WholphinDispatchers.Main) {
                            TrackSelectionUtils.createTrackSelections(
                                onMain { player.trackSelectionParameters },
                                onMain { player.currentTracks },
                                audioIndex,
                                subtitleIndex,
                                source,
                            )
                        }
                    if (result.bothSelected) {
                        onMain { player.trackSelectionParameters = result.trackSelectionParameters }
                        Timber.d("Changes tracks audio=$audioIndex, subtitle=$subtitleIndex")
                        _state.update {
                            it.copy(
                                currentPlayback =
                                    (it.currentPlayback ?: currentPlayback).copy(
                                        tracks = checkForSupport(onMain { player.currentTracks }),
                                    ),
                            )
                        }
                        loadSubtitleDelay()
                        return@withContext true
                    }
                } else {
                    Timber.v("changeStreams direct play, external subtitle was requested")
                }
                return@withContext false
            }

        fun changeAudioStream(index: Int) {
            viewModelScope.launchIO {
                val currentPlayback = state.value.currentPlayback
                if (currentPlayback != null) {
                    Timber.d("Changing audio track to %s", index)
                    saveTrackSelection(index, MediaStreamType.AUDIO)
                    _state.update {
                        it.copy(
                            currentPlayback = it.currentPlayback?.copy(audioIndex = index),
                        )
                    }

                    // Resolve ONLY_FORCED to actual track based on new audio language
                    val resolvedSubtitleIndex =
                        streamChoiceService.resolveSubtitleIndex(
                            source = currentPlayback.mediaSourceInfo,
                            audioStreamIndex = index,
                            item = currentItem.item.data,
                            subtitleIndex = currentPlayback.subtitleIndex,
                            prefs = preferences,
                        )

                    changeStreams(
                        item = currentItem.item,
                        sourceId = currentPlayback.mediaSourceInfo.id,
                        audioIndex = index,
                        subtitleIndex = resolvedSubtitleIndex,
                        positionMs = onMain { player.currentPosition },
                        enableDirectPlay = directPlayAllowed(currentPlayback.mediaSourceInfo.id),
                    )
                } else {
                    Timber.w("Trying to change audio, but currentPlayback is null")
                }
            }
        }

        fun changeSubtitleStream(index: Int): Job =
            viewModelScope.launchIO {
                val currentPlayback = state.value.currentPlayback
                if (currentPlayback != null) {
                    Timber.d("Changing subtitle track to %s", index)
                    saveTrackSelection(index, MediaStreamType.SUBTITLE)
                    _state.update {
                        it.copy(
                            currentPlayback = it.currentPlayback?.copy(subtitleIndex = index),
                        )
                    }

                    // Resolve ONLY_FORCED to actual track index for playback
                    val resolvedIndex =
                        streamChoiceService.resolveSubtitleIndex(
                            source = currentPlayback.mediaSourceInfo,
                            audioStreamIndex = currentPlayback.audioIndex,
                            item = currentItem.item.data,
                            subtitleIndex = index,
                            prefs = preferences,
                        )

                    changeStreams(
                        item = currentItem.item,
                        sourceId = currentPlayback.mediaSourceInfo.id,
                        audioIndex = currentPlayback.audioIndex,
                        subtitleIndex = resolvedIndex,
                        positionMs = onMain { player.currentPosition },
                        enableDirectPlay = directPlayAllowed(currentPlayback.mediaSourceInfo.id),
                    )
                } else {
                    Timber.w("Trying to change subtitle, but currentPlayback is null")
                }
            }

        fun changeMaxBitrate(newMaxBitrate: Long) {
            viewModelScope.launchIO {
                val currentPlayback = state.value.currentPlayback
                if (currentPlayback != null) {
                    Timber.d("Changing bitrate to %s", newMaxBitrate)
                    changeStreams(
                        item = currentItem.item,
                        sourceId = currentPlayback.mediaSourceInfo.id,
                        audioIndex = currentPlayback.audioIndex,
                        subtitleIndex = currentPlayback.subtitleIndex,
                        positionMs = onMain { player.currentPosition },
                        enableDirectPlay = directPlayAllowed(currentPlayback.mediaSourceInfo.id),
                        maxBitrate = newMaxBitrate,
                    )
                } else {
                    Timber.w("Trying to change bitrate, but currentPlayback is null")
                }
            }
        }

        internal suspend fun saveTrackSelection(
            trackIndex: Int,
            type: MediaStreamType,
        ) {
            val itemPlayback =
                itemPlaybackRepository.saveTrackSelection(
                    item = currentItem.item,
                    itemPlayback = state.value.currentItemPlayback,
                    trackIndex = trackIndex,
                    type = type,
                )
            _state.update {
                it.copy(
                    currentItemPlayback = itemPlayback,
                )
            }
        }

        private suspend fun prefetchTrickplay(
            duration: Duration,
            trickplayInfo: TrickplayInfo,
            mediaSourceId: UUID?,
        ) {
            val tilesPerImage = trickplayInfo.tileWidth * trickplayInfo.tileHeight
            val totalCount =
                (duration.inWholeMilliseconds / trickplayInfo.interval).toInt() / tilesPerImage + 1
            (0..<totalCount).forEach {
                val url = getTrickplayUrl(it, trickplayInfo, mediaSourceId)
                context.imageLoader.enqueue(
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        .size(Size.ORIGINAL)
                        .build(),
                )
            }
        }

        fun getTrickplayUrl(
            index: Int,
            trickPlayInfo: TrickplayInfo? = state.value.currentMediaInfo.trickPlayInfo,
            mediaSourceId: UUID? =
                state.value.currentPlayback
                    ?.mediaSourceInfo
                    ?.id
                    ?.toUUIDOrNull(),
        ): String? =
            trickPlayInfo?.let {
                val itemId = currentItem.id
                return api.trickplayApi.getTrickplayTileImageUrl(
                    itemId,
                    trickPlayInfo.width,
                    index,
                    mediaSourceId,
                )
            }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                Timber.v("Playback state is STATE_ENDED")
                viewModelScope.launchDefault {
                    when (val nextItem = state.value.nextItem()) {
                        is PlaylistItem.Intro -> {
                            Timber.v("Next item is intro, so playing immediately")
                            playNextUp()
                        }

                        is PlaylistItem.Media -> {
                            val prefs = preferences.appPreferences.playbackPreferences
                            when {
                                currentItem is PlaylistItem.Intro -> {
                                    Timber.v("Current item is intro, so playing next up immediately")
                                    playNextUp()
                                }

                                prefs.showNextUpWhen == ShowNextUpWhen.NEXT_UP_NEVER && !prefs.autoPlayNext -> {
                                    Timber.v("Never show or auto play next up, returning")
                                    navigationManager.goBack()
                                }

                                prefs.showNextUpWhen != ShowNextUpWhen.NEXT_UP_NEVER -> {
                                    Timber.v("Setting next up to ${nextItem.id}")
                                    _state.update { it.copy(nextUp = nextItem.item) }
                                }

                                else -> {
                                    controllerViewState.showControls()
                                }
                            }
                        }

                        null -> {
                            Timber.v("No next up")
                            navigationManager.goBack()
                        }
                    }
                }
            }
        }

        // Variables for tracking segment state
        @VisibleForTesting
        internal var segmentJob: Job? = null
        private val autoSkippedSegments = mutableSetOf<UUID>()
        private val outroShownSegments = mutableSetOf<UUID>()

        /**
         * Cancels listening for segments and clears current segment state
         */
        private fun resetSegmentState() {
            segmentJob?.cancel()
            autoSkippedSegments.clear()
            outroShownSegments.clear()
            _state.update { it.copy(currentSegment = null) }
        }

        /**
         * This sets up a coroutine to periodically check whether the current playback progress is within a media segment (intro, outro, etc)
         */
        private fun listenForSegments(itemId: UUID) {
            segmentJob?.cancel()
            segmentJob =
                viewModelScope.launchIO {
                    val prefs = preferences.appPreferences.playbackPreferences
                    val segments =
                        try {
                            api.mediaSegmentsApi.getItemSegments(itemId).content
                        } catch (ex: CancellationException) {
                            throw ex
                        } catch (ex: Exception) {
                            Timber.w(ex, "Could not get media segments for %s", itemId)
                            null
                        }
                    if (prefs.showNextUpWhen == ShowNextUpWhen.DURING_CREDITS &&
                        segments?.items.orEmpty().none { it.type == MediaSegmentType.OUTRO }
                    ) {
                        // No credits segment (eg remote sources aren't analyzed), so show next up shortly before the end
                        launch { showNextUpNearEnd(itemId) }
                    }
                    if (segments != null && segments.items.isNotEmpty()) {
                        while (isActive) {
                            delay(500L)
                            val currentTicks =
                                onMain { player.currentPosition.milliseconds.inWholeTicks }
                            val currentSegment =
                                segments.items
                                    .firstOrNull {
                                        it.type != MediaSegmentType.UNKNOWN && currentTicks >= it.startTicks && currentTicks < it.endTicks
                                    }
                            if (currentSegment != null &&
                                currentSegment.itemId == this@PlaybackViewModel.itemId
                            ) {
                                if (currentSegment.id !=
                                    state.value.currentSegment
                                        ?.segment
                                        ?.id
                                ) {
                                    Timber.d(
                                        "Found media segment for %s: %s, %s",
                                        currentSegment.itemId,
                                        currentSegment.id,
                                        currentSegment.type,
                                    )
                                }
                                val state = state.value

                                if (currentSegment.type == MediaSegmentType.OUTRO &&
                                    prefs.showNextUpWhen == ShowNextUpWhen.DURING_CREDITS &&
                                    state.hasNext &&
                                    outroShownSegments.add(currentSegment.id)
                                ) {
                                    val nextItem = state.nextItem()
                                    if (nextItem is PlaylistItem.Media) {
                                        Timber.v("Setting next up during outro to ${nextItem?.id}")
                                        _state.update { it.copy(nextUp = nextItem.item) }
                                    }
                                } else {
                                    val behavior =
                                        when (currentSegment.type) {
                                            MediaSegmentType.COMMERCIAL -> prefs.skipCommercials
                                            MediaSegmentType.PREVIEW -> prefs.skipPreviews
                                            MediaSegmentType.RECAP -> prefs.skipRecaps
                                            MediaSegmentType.OUTRO -> prefs.skipOutros
                                            MediaSegmentType.INTRO -> prefs.skipIntros
                                            MediaSegmentType.UNKNOWN -> SkipSegmentBehavior.IGNORE
                                        }
                                    withContext(WholphinDispatchers.Main) {
                                        val newSegment =
                                            when (behavior) {
                                                SkipSegmentBehavior.AUTO_SKIP -> {
                                                    if (autoSkippedSegments.add(currentSegment.id)) {
                                                        onMain { player.seekTo(currentSegment.endTicks.ticks.inWholeMilliseconds + 1) }
                                                    }
                                                    MediaSegmentState(currentSegment, true)
                                                }

                                                SkipSegmentBehavior.ASK_TO_SKIP -> {
                                                    MediaSegmentState(
                                                        currentSegment,
                                                        autoSkippedSegments.contains(currentSegment.id),
                                                    )
                                                }

                                                else -> {
                                                    null
                                                }
                                            }
                                        _state.update { it.copy(currentSegment = newSegment) }
                                    }
                                }
                            } else if (currentSegment == null) {
                                _state.update { it.copy(currentSegment = null) }
                            }
                        }
                    }
                }
        }

        private suspend fun showNextUpNearEnd(itemId: UUID) {
            while (currentCoroutineContext().isActive) {
                delay(1.seconds)
                val (position, duration) = onMain { player.currentPosition to player.duration }
                if (duration <= 0 || duration == C.TIME_UNSET || itemId != this.itemId) continue
                if (duration - position in 1..NEXT_UP_WITHOUT_CREDITS_MS) {
                    val state = state.value
                    val nextItem = state.nextItem()
                    if (state.nextUp == null && nextItem is PlaylistItem.Media) {
                        Timber.v("Setting next up near the end to %s", nextItem.id)
                        _state.update { it.copy(nextUp = nextItem.item) }
                    }
                    return
                }
            }
        }

        fun updateSegment(
            segmentId: UUID?,
            dismissed: Boolean,
        ) {
            viewModelScope.launchDefault {
                val segment = state.value.currentSegment?.segment
                if (segment != null && segment.id == segmentId) {
                    autoSkippedSegments.add(segment.id)
                    if (dismissed) {
                        _state.update { it.copy(currentSegment = it.currentSegment?.copy(interacted = true)) }
                    } else {
                        _state.update { it.copy(currentSegment = null) }
                        onMain { player.seekTo(segment.endTicks.ticks.inWholeMilliseconds + 1) }
                    }
                }
            }
        }

        private fun listenForTranscodeReason(): Job =
            viewModelScope.launchIO {
                state.map { it.currentPlayback }.collectLatest {
                    if (it != null && it.playMethod == PlayMethod.TRANSCODE && it.transcodeInfo == null) {
                        try {
                            var transcodeInfo = it.transcodeInfo
                            while (isActive && transcodeInfo == null) {
                                delay(2.seconds)
                                transcodeInfo =
                                    api.sessionApi
                                        .getSessions(deviceId = deviceInfo.id)
                                        .content
                                        .firstOrNull()
                                        ?.transcodingInfo
                                if (transcodeInfo == null) delay(3.seconds)
                            }
                            Timber.v("transcodeInfo=$transcodeInfo")
                            updateCurrentPlayback { current ->
                                current?.copy(transcodeInfo = transcodeInfo)
                            }
                        } catch (ex: Exception) {
                            if (ex !is CancellationException) {
                                Timber.w(ex, "Exception trying to get session info")
                                updateCurrentPlayback { current ->
                                    current?.copy(transcodeInfo = null)
                                }
                            }
                        }
                    }
                }
            }

        private var lastInteractionDate: Date = Date()

        /**
         * Tracks interactions with the UI for passout protection
         */
        fun reportInteraction() {
//            Timber.v("reportInteraction")
            lastInteractionDate = Date()
        }

        fun shouldAutoPlayNextUp(): Boolean =
            preferences.appPreferences.playbackPreferences.let {
                it.autoPlayNext &&
                    if (it.passOutProtectionMs > 0) {
                        (Date().time - lastInteractionDate.time) < it.passOutProtectionMs
                    } else {
                        true
                    }
            }

        fun playNextUp() {
            viewModelScope.launchDefault {
                playlistMutex.withLock {
                    val state = state.value
                    if (state.hasNext) {
                        cancelUpNextEpisode()
                        val nextIndex = state.playlistIndex + 1
                        val item = state.playlist.items[nextIndex]
                        _state.update { it.copy(playlistIndex = nextIndex) }
                        playlistJob?.cancel()
                        playlistJob =
                            viewModelScope.launchDefault {
                                runCatchingPlayback {
                                    val played = play(item, 0)
                                    if (!played) {
                                        playNextUp()
                                    }
                                }
                            }
                    } else {
                        Timber.w("Attempting to play next, but there are no more items")
                        return@launchDefault
                    }
                }
            }
        }

        fun playPrevious() {
            viewModelScope.launchDefault {
                playlistMutex.withLock {
                    val state = state.value
                    if (state.hasPrevious) {
                        cancelUpNextEpisode()
                        val previousIndex = state.playlistIndex - 1
                        val item = state.playlist.items[previousIndex]
                        _state.update { it.copy(playlistIndex = previousIndex) }
                        playlistJob?.cancel()
                        playlistJob =
                            viewModelScope.launchDefault {
                                runCatchingPlayback {
                                    val played = play(item, 0)
                                    if (!played) {
                                        playPrevious()
                                    }
                                }
                            }
                    } else {
                        Timber.w("Attempting to play previous, but there is none")
                        return@launchDefault
                    }
                }
            }
        }

        suspend fun cancelUpNextEpisode() {
            _state.update { it.copy(nextUp = null) }
        }

        fun playItemInPlaylist(item: BaseItem) {
            viewModelScope.launchDefault {
                playlistMutex.withLock {
                    val state = state.value
                    val index = state.playlist.items.indexOfFirst { it.id == item.id }
                    if (index in state.playlist.items.indices) {
                        val toPlay = state.playlist.items[index]
                        _state.update { it.copy(playlistIndex = index) }
                        playlistJob?.cancel()
                        playlistJob =
                            viewModelScope.launchDefault {
                                runCatchingPlayback {
                                    val played = play(toPlay, 0)
                                    if (!played) {
                                        playNextUp()
                                    }
                                }
                            }
                    } else {
                        Timber.w("Item not found in playlist %s", item.id)
                        return@launchDefault
                    }
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateCurrentPlayback {
                it?.copy(
                    tracks = checkForSupport(tracks),
                )
            }
        }

        override fun onCues(cueGroup: CueGroup) {
            _state.update { it.copy(subtitleCues = cueGroup.cues) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error, "Playback error")
            viewModelScope.launch(WholphinDispatchers.Main + ExceptionHandler()) {
                runCatchingPlayback {
                    val currentPlayback = state.value.currentPlayback
                    if (currentPlayback == null) {
                        Timber.w("Playback error, currentPlayback is null")
                        setPlaybackError(error)
                        return@runCatchingPlayback
                    }
                    val positionMs = player.currentPosition
                    lastPositionMs = positionMs
                    val source = currentPlayback.mediaSourceInfo
                    val kind = PlaybackErrorClassifier.classify(error.errorCode)
                    val canTranscode = currentPlayback.playMethod != PlayMethod.TRANSCODE
                    // A dead link won't work when transcoded either, and for remote (debrid/web) sources
                    // another version is a better bet than a CPU-only transcode on the server
                    // An HLS web stream played directly gets the server remux of the same (Italian) source first
                    val directHlsFailed =
                        currentPlayback.playMethod == PlayMethod.DIRECT_PLAY && isDirectHlsCandidate(source)
                    val preferAnotherSource =
                        !directHlsFailed &&
                            (kind == PlaybackErrorKind.SOURCE_UNAVAILABLE || source.isStreamed() || !canTranscode)
                    Timber.i(
                        "Playback error kind=%s, playMethod=%s, remote=%s",
                        kind,
                        currentPlayback.playMethod,
                        source.isRemote,
                    )
                    if (preferAnotherSource && tryNextSource(source.id, positionMs)) {
                        return@runCatchingPlayback
                    }
                    if (canTranscode) {
                        Timber.w("Playback error during ${currentPlayback.playMethod}, falling back to transcoding")
                        source.id?.let { transcodeOnlySourceIds.add(it) }
                        val started =
                            changeStreams(
                                item = currentItem.item,
                                sourceId = source.id,
                                audioIndex = currentPlayback.audioIndex,
                                subtitleIndex = currentPlayback.subtitleIndex,
                                positionMs = positionMs,
                                enableDirectPlay = false,
                                enableDirectStream = false,
                                allowSourceFailover = true,
                            )
                        if (started) {
                            withContext(WholphinDispatchers.Main) {
                                player.prepare()
                                player.play()
                            }
                        }
                    } else {
                        setPlaybackError(error)
                    }
                }
            }
        }

        fun release() {
            Timber.v("release")
            disconnectPlayer()
            activityListener = null
        }

        fun subscribeToWebSocket() {
            subscribeJob?.cancel()
            try {
                subscribeJob =
                    viewModelScope.launch {
                        coroutineScope {
                            api.webSocket
                                .subscribe<PlaystateMessage>()
                                .onEach { message ->
                                    message.data?.let { request ->
                                        Timber.v("Received playstate request: %s", request)
                                        withContext(WholphinDispatchers.Main) {
                                            handlePlaystateRequest(request)
                                        }
                                    }
                                }.catch { ex ->
                                    Timber.e(ex, "Error in websocket subscription")
                                }.launchIn(this)
                        }
                    }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.e(ex, "Error in playback websocket subscription")
                if (viewModelScope.isActive) {
                    subscribeToWebSocket()
                }
            }
        }

        private suspend fun handlePlaystateRequest(request: PlaystateRequest) =
            withContext(WholphinDispatchers.Main) {
                when (request.command) {
                    PlaystateCommand.STOP -> {
                        release()
                        navigationManager.goBack()
                    }

                    PlaystateCommand.PAUSE -> {
                        player.pause()
                    }

                    PlaystateCommand.UNPAUSE -> {
                        player.play()
                    }

                    PlaystateCommand.NEXT_TRACK -> {
                        playNextUp()
                    }

                    PlaystateCommand.PREVIOUS_TRACK -> {
                        playPrevious()
                    }

                    PlaystateCommand.SEEK -> {
                        request.seekPositionTicks?.ticks?.let {
                            player.seekTo(it.inWholeMilliseconds)
                        }
                    }

                    PlaystateCommand.REWIND -> {
                        player.seekBack(
                            preferences.appPreferences.playbackPreferences.skipBackMs.milliseconds,
                        )
                    }

                    PlaystateCommand.FAST_FORWARD -> {
                        player.seekForward(
                            preferences.appPreferences.playbackPreferences.skipForwardMs.milliseconds,
                        )
                    }

                    PlaystateCommand.PLAY_PAUSE -> {
                        Util.handlePlayPauseButtonAction(player)
                    }
                }
            }

        /**
         * Atomically update [currentMediaInfo]
         */
        internal suspend fun updateCurrentMedia(block: (CurrentMediaInfo) -> CurrentMediaInfo) =
            withContext(WholphinDispatchers.Default) {
                _state.update {
                    it.copy(currentMediaInfo = block.invoke(it.currentMediaInfo))
                }
            }

        private fun updateDecoder(
            decoderName: String,
            type: MediaType,
        ) {
            viewModelScope.launchDefault {
                val codecInfo =
                    MediaCodecList(MediaCodecList.ALL_CODECS)
                        .codecInfos
                        .firstOrNull { !it.isEncoder && it.name == decoderName }
                val decoderString =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (codecInfo?.isHardwareAccelerated == true) {
                            "$decoderName (HW)"
                        } else {
                            decoderName
                        }
                    } else {
                        decoderName
                    }
                updateCurrentPlayback {
                    when (type) {
                        MediaType.VIDEO -> it?.copy(videoDecoder = decoderString)
                        MediaType.AUDIO -> it?.copy(audioDecoder = decoderString)
                        else -> throw IllegalArgumentException("Unsupported type: $type")
                    }
                }
            }
        }

        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long,
        ) {
            Timber.v("onVideoDecoderInitialized: decoder=$decoderName")
            updateDecoder(decoderName, MediaType.VIDEO)
        }

        override fun onVideoDisabled(
            eventTime: AnalyticsListener.EventTime,
            decoderCounters: DecoderCounters,
        ) {
            Timber.d("onVideoDisabled")
            updateCurrentPlayback { it?.copy(videoDecoder = null) }
        }

        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?,
        ) {
            decoderReuseEvaluation?.let { decoder ->
                if (decoder.result != DecoderReuseEvaluation.REUSE_RESULT_NO) {
                    Timber.d("onVideoInputFormatChanged: decoder=${decoder.decoderName}")
                    updateDecoder(decoder.decoderName, MediaType.VIDEO)
                }
            }
        }

        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long,
        ) {
            Timber.d("decoder: onAudioDecoderInitialized: decoder=$decoderName")
            updateDecoder(decoderName, MediaType.AUDIO)
        }

        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?,
        ) {
            decoderReuseEvaluation?.let { decoder ->
                if (decoder.result != DecoderReuseEvaluation.REUSE_RESULT_NO) {
                    Timber.d("decoder: onAudioInputFormatChanged: decoder=${decoder.decoderName}")
                    updateDecoder(decoder.decoderName, MediaType.AUDIO)
                }
            }
        }

        override fun onAudioDisabled(
            eventTime: AnalyticsListener.EventTime,
            decoderCounters: DecoderCounters,
        ) {
            Timber.d("decoder: onAudioDisabled")
            updateCurrentPlayback { it?.copy(audioDecoder = null) }
        }

        private var subtitleDelaySaveJob: Job? = null

        fun updateSubtitleDelay(delta: Duration) {
            subtitleDelaySaveJob?.cancel()
            updateCurrentPlayback {
                it?.let {
                    val newDelay = it.subtitleDelay + delta
                    val result = it.copy(subtitleDelay = it.subtitleDelay + delta)
                    subtitleDelaySaveJob =
                        viewModelScope.launchIO {
                            // Debounce & save
                            state.value.currentItemPlayback?.let { item ->
                                delay(1500)
                                itemPlaybackRepository.saveTrackModifications(
                                    item.itemId,
                                    item.subtitleIndex,
                                    newDelay,
                                )
                            }
                        }
                    result
                }
            }
        }

        suspend fun loadSubtitleDelay() {
            state.value.currentPlayback?.let {
                if (it.subtitleIndexEnabled) {
                    val result =
                        itemPlaybackRepository.getTrackModifications(it.item.id, it.subtitleIndex)
                    if (result != null) {
                        Timber.v(
                            "Loading subtitle delay %s for track=%s, itemId=%s",
                            result.delayMs,
                            it.subtitleIndex,
                            it.item.id,
                        )
                        updateCurrentPlayback { it?.copy(subtitleDelay = result.delayMs.milliseconds) }
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            screensaverService.keepScreenOn(isPlaying)
        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            val player = this@PlaybackViewModel.player
            if (availableCommands.contains(Player.COMMAND_PREPARE) && player is MpvPlayer) {
                // MpvPlayer is initialized, so configure subtitles
                Timber.i("Applying subtitle config to MPV")
                viewModelScope.launchDefault {
                    val configuration = context.resources.configuration
                    val density = Density(context.resources.displayMetrics.density)
                    preferences.appPreferences.interfacePreferences.subtitlesPreferences.applyToMpv(
                        configuration,
                        density,
                    )
                }
            }
        }

        fun updateDensity(density: Density) {
            Timber.d("Density changed")
            viewModelScope.launchDefault {
                val availableCommands = onMain { player.availableCommands }
                if (availableCommands.contains(Player.COMMAND_PREPARE) && player is MpvPlayer) {
                    Timber.i("Applying density change for subtitle config to MPV")
                    val configuration = context.resources.configuration
                    preferences.appPreferences.interfacePreferences.subtitlesPreferences.applyToMpv(
                        configuration,
                        density,
                    )
                }
            }
        }

        override fun onBandwidthEstimate(
            eventTime: AnalyticsListener.EventTime,
            totalLoadTimeMs: Int,
            totalBytesLoaded: Long,
            bitrateEstimate: Long,
        ) {
            Timber.v(
                "onBandwidthEstimate: totalLoadTimeMs=%s, totalBytesLoaded=%s, bitrateEstimate=%s",
                totalLoadTimeMs,
                totalBytesLoaded,
                bitrateEstimate,
            )
            if (totalLoadTimeMs > 0 && totalBytesLoaded > 0) {
                _state.update {
                    it.copy(
                        analyticsState =
                            it.analyticsState.copy(
                                bitrate = formatBitrate((totalBytesLoaded.toDouble() / (totalLoadTimeMs / 1000.0) * 8).roundToInt()),
                                bitrateEstimate = formatBitrate(bitrateEstimate.toInt()),
                            ),
                    )
                }
            }
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long,
        ) {
            _state.update {
                it.copy(
                    analyticsState =
                        it.analyticsState.copy(
                            droppedFrames = it.analyticsState.droppedFrames + droppedFrames,
                        ),
                )
            }
        }

        companion object {
            /**
             * Maximum number of media sources to try for an item before giving up
             */
            const val MAX_SOURCE_ATTEMPTS = 3

            /**
             * Without a credits segment, show next up this long before the end
             */
            const val NEXT_UP_WITHOUT_CREDITS_MS = 30_000L
        }
    }
