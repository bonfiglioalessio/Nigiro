package com.github.damontecres.wholphin.services

import android.content.Context
import androidx.datastore.core.DataStore
import com.github.damontecres.wholphin.data.model.RemoteConfigRuntime
import com.github.damontecres.wholphin.data.model.RemoteConfigUrlResolver
import com.github.damontecres.wholphin.data.model.RemoteWholphinConfig
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.BackdropStyle
import com.github.damontecres.wholphin.preferences.PlayerBackend
import com.github.damontecres.wholphin.preferences.SkipSegmentBehavior
import com.github.damontecres.wholphin.preferences.ThemeSongVolume
import com.github.damontecres.wholphin.preferences.updateHomePagePreferences
import com.github.damontecres.wholphin.preferences.updateInterfacePreferences
import com.github.damontecres.wholphin.preferences.updatePlaybackPreferences
import com.github.damontecres.wholphin.preferences.updateScreensaverPreferences
import com.github.damontecres.wholphin.preferences.updateSearchPreferences
import com.github.damontecres.wholphin.services.hilt.StandardOkHttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigService private constructor(
    private val cacheFile: File,
    private val preferencesDataStore: DataStore<AppPreferences>?,
    private val fetcher: suspend (String) -> String?,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @StandardOkHttpClient okHttpClient: OkHttpClient,
        preferencesDataStore: DataStore<AppPreferences>,
    ) : this(
        cacheFile = File(context.filesDir, CACHE_FILE_NAME),
        preferencesDataStore = preferencesDataStore,
        fetcher = defaultFetcher(okHttpClient),
    )

    internal constructor(
        cacheFile: File,
        preferencesDataStore: DataStore<AppPreferences>? = null,
        fetcher: suspend (String) -> String?,
        testInit: Boolean = true,
    ) : this(cacheFile, preferencesDataStore, fetcher) {
        if (testInit) {
            RemoteConfigRuntime.current = _config.value
        }
    }

    private val mutex = Mutex()
    private val _config = MutableStateFlow(loadInitialConfig(cacheFile))
    val config: StateFlow<RemoteWholphinConfig> = _config

    init {
        RemoteConfigRuntime.current = _config.value
    }

    suspend fun refreshForServer(
        serverUrl: String?,
        overrideUrl: String? = null,
    ): RemoteWholphinConfig =
        mutex.withLock {
            val candidates = RemoteConfigUrlResolver.candidateUrls(serverUrl, overrideUrl)
            var fetched: Pair<RemoteWholphinConfig, String>? = null
            for (url in candidates) {
                val raw =
                    try {
                        fetcher(url)
                    } catch (ex: Exception) {
                        Timber.w(ex, "Failed fetching remote config from %s", url)
                        null
                    }
                if (raw != null) {
                    val parsed = parseOrNull(raw)
                    if (parsed != null) {
                        fetched = parsed to raw
                        break
                    }
                }
            }

            val resolved =
                if (fetched != null) {
                    writeCache(cacheFile, fetched.second)
                    fetched.first
                } else {
                    loadInitialConfig(cacheFile)
                }

            _config.value = resolved
            RemoteConfigRuntime.current = resolved
            applyPreferences(resolved)
            resolved
        }

    private suspend fun applyPreferences(remote: RemoteWholphinConfig) {
        val store = preferencesDataStore ?: return
        try {
            store.updateData { current -> mapToAppPreferences(current, remote) }
        } catch (ex: Exception) {
            Timber.w(ex, "Failed applying remote config to AppPreferences")
        }
    }

    companion object {
        const val CACHE_FILE_NAME = "wholphin-config-cache.json"
        private const val NETWORK_TIMEOUT_MS = 2_000L

        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
            }

        private fun defaultFetcher(okHttpClient: OkHttpClient): suspend (String) -> String? {
            val shortClient =
                okHttpClient
                    .newBuilder()
                    .connectTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .readTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .callTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .build()
            return { url ->
                withContext(Dispatchers.IO) {
                    val request =
                        Request
                            .Builder()
                            .url(url)
                            .header("Cache-Control", "no-cache")
                            .build()
                    shortClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body.string()
                        } else {
                            null
                        }
                    }
                }
            }
        }

        fun parseOrNull(raw: String?): RemoteWholphinConfig? {
            if (raw.isNullOrBlank()) return null
            return try {
                val parsed = json.decodeFromString<RemoteWholphinConfig>(raw)
                if (parsed.schemaVersion <= 0) null else parsed
            } catch (ex: Exception) {
                Timber.w(ex, "Invalid remote config JSON")
                null
            }
        }

        private fun loadInitialConfig(cacheFile: File): RemoteWholphinConfig =
            try {
                if (cacheFile.exists()) {
                    parseOrNull(cacheFile.readText()) ?: RemoteWholphinConfig.DEFAULT
                } else {
                    RemoteWholphinConfig.DEFAULT
                }
            } catch (ex: Exception) {
                Timber.w(ex, "Failed reading cached remote config")
                RemoteWholphinConfig.DEFAULT
            }

        private fun writeCache(
            cacheFile: File,
            raw: String,
        ) {
            try {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(raw)
            } catch (ex: Exception) {
                Timber.w(ex, "Failed writing remote config cache")
            }
        }

        fun mapToAppPreferences(
            current: AppPreferences,
            remote: RemoteWholphinConfig,
        ): AppPreferences {
            val targetBackdrop =
                when (remote.ui.backdropStyle.uppercase()) {
                    "IMAGE_ONLY", "BACKDROP_IMAGE_ONLY" -> BackdropStyle.BACKDROP_IMAGE_ONLY
                    "DYNAMIC", "BACKDROP_DYNAMIC_COLOR" -> BackdropStyle.BACKDROP_DYNAMIC_COLOR
                    else -> BackdropStyle.BACKDROP_NONE
                }
            val targetBackend =
                when (remote.playback.playerBackend.uppercase()) {
                    "MPV" -> PlayerBackend.MPV
                    "PREFER_MPV" -> PlayerBackend.PREFER_MPV
                    "EXTERNAL_PLAYER" -> PlayerBackend.EXTERNAL_PLAYER
                    else -> PlayerBackend.EXO_PLAYER
                }
            val targetSkipIntro = parseSkipBehavior(remote.playback.skipIntros)
            val targetSkipOutro = parseSkipBehavior(remote.playback.skipOutros)

            return current
                .updateInterfacePreferences {
                    backdropStyle = targetBackdrop
                    enableAnimations = remote.ui.animationsEnabled
                    showClock = remote.ui.showClock
                    showLogos = remote.ui.showLogos
                    showTvSettings = !remote.ui.hideTvSettings
                    navDrawerSwitchOnFocus = remote.ui.navDrawerSwitchOnFocus
                    playThemeSongs =
                        if (remote.ui.modules.music) {
                            current.interfacePreferences.playThemeSongs
                        } else {
                            ThemeSongVolume.DISABLED
                        }
                }.updateScreensaverPreferences {
                    enabled = remote.ui.modules.screensaver && remote.screensaver.enabled
                    animate = remote.screensaver.animate
                    startDelay = (remote.screensaver.startDelaySeconds.coerceAtLeast(10L)) * 1_000L
                    duration = (remote.screensaver.durationSeconds.coerceAtLeast(3L)) * 1_000L
                    dimEnabled = remote.screensaver.dimEnabled
                    dimPercent = remote.screensaver.dimPercent.coerceIn(0, 95)
                    showClock = remote.screensaver.showClock
                }.updatePlaybackPreferences {
                    playerBackend = targetBackend
                    maxBitrate = remote.playback.maxBitrate.coerceAtLeast(1_000_000L)
                    refreshRateSwitching = remote.playback.refreshRateSwitching
                    resolutionSwitching = remote.playback.resolutionSwitching
                    skipIntros = targetSkipIntro
                    skipOutros = targetSkipOutro
                    autoPlayNext = remote.playback.autoPlayNext
                    autoPlayNextDelaySeconds = remote.playback.autoPlayNextDelaySeconds.coerceAtLeast(0L)
                }.updateSearchPreferences {
                    combinedSearchResults = remote.search.combinedResults
                    separateSearchResults = !remote.search.combinedResults
                    showVoiceSearchButton = remote.search.showVoiceSearchButton
                }.updateHomePagePreferences {
                    maxItemsPerRow = remote.home.maxItemsPerRow.coerceIn(6, 50)
                    enableRewatchingNextUp = remote.home.enableRewatchingNextUp
                    clickToPlay = remote.home.clickToPlay
                }
        }

        private fun parseSkipBehavior(value: String): SkipSegmentBehavior =
            when (value.uppercase()) {
                "AUTO", "AUTO_SKIP" -> SkipSegmentBehavior.AUTO_SKIP
                "IGNORE", "NONE" -> SkipSegmentBehavior.IGNORE
                else -> SkipSegmentBehavior.ASK_TO_SKIP
            }
    }
}
