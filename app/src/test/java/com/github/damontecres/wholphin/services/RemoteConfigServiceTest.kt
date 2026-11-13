package com.github.damontecres.wholphin.services

import com.github.damontecres.wholphin.data.model.RemoteConfigUrlResolver
import com.github.damontecres.wholphin.data.model.RemoteWholphinConfig
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.BackdropStyle
import com.github.damontecres.wholphin.preferences.PlayerBackend
import com.github.damontecres.wholphin.preferences.SkipSegmentBehavior
import com.github.damontecres.wholphin.preferences.ThemeSongVolume
import com.github.damontecres.wholphin.preferences.updateInterfacePreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class RemoteConfigServiceTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun candidateUrls_strips8096PortAndSubpath() {
        val urls = RemoteConfigUrlResolver.candidateUrls("http://192.168.1.157:8096/jellyfin/")
        assertEquals(listOf("http://192.168.1.157/wholphin-config.json"), urls)

        val tailscale = RemoteConfigUrlResolver.candidateUrls("http://100.74.237.31:8096")
        assertEquals(listOf("http://100.74.237.31/wholphin-config.json"), tailscale)

        val customPort = RemoteConfigUrlResolver.candidateUrls("http://192.168.1.157:8080")
        assertEquals(
            listOf(
                "http://192.168.1.157:8080/wholphin-config.json",
                "http://192.168.1.157/wholphin-config.json",
            ),
            customPort,
        )
    }

    @Test
    fun refreshForServer_parsesConfigWritesCacheAndFallsBackOffline() =
        runTest {
            val cacheFile = tempDir.newFile("wholphin-config-cache.json")
            cacheFile.delete()

            val serverJson =
                """
                {
                  "schemaVersion": 1,
                  "configRevision": "2026-11-13-42",
                  "ui": {
                    "flatMode": true,
                    "animationsEnabled": false,
                    "backdropStyle": "NONE",
                    "hideTvSettings": true,
                    "modules": {
                      "discover": true,
                      "screensaver": true,
                      "liveTv": false,
                      "music": false,
                      "photos": false
                    }
                  },
                  "screensaver": {
                    "enabled": true,
                    "animate": false,
                    "startDelaySeconds": 240,
                    "durationSeconds": 12
                  },
                  "playback": {
                    "playerBackend": "MPV",
                    "maxBitrate": 50000000,
                    "skipIntros": "AUTO",
                    "preferredAudioLanguage": "ita",
                    "preferredSubtitleLanguage": "ita"
                  },
                  "home": {
                    "defaultFilter": "Series",
                    "maxItemsPerRow": 18
                  }
                }
                """.trimIndent()

            val service =
                RemoteConfigService(
                    cacheFile = cacheFile,
                    preferencesDataStore = null,
                    fetcher = { serverJson },
                )

            val loaded = service.refreshForServer("http://192.168.1.157:8096")
            assertEquals("2026-11-13-42", loaded.configRevision)
            assertEquals("MPV", loaded.playback.playerBackend)
            assertEquals("Series", loaded.home.defaultFilter)
            assertTrue(cacheFile.exists())

            val offlineService =
                RemoteConfigService(
                    cacheFile = cacheFile,
                    preferencesDataStore = null,
                    fetcher = { throw IOException("timeout") },
                )
            val fallback = offlineService.refreshForServer("http://192.168.1.157:8096")
            assertEquals("2026-11-13-42", fallback.configRevision)
            assertEquals(18, fallback.home.maxItemsPerRow)
        }

    @Test
    fun refreshForServer_invalidJsonFallsBackToDefaultsWhenNoCache() =
        runTest {
            val cacheFile = tempDir.newFile("missing-cache.json")
            cacheFile.delete()

            val service =
                RemoteConfigService(
                    cacheFile = cacheFile,
                    preferencesDataStore = null,
                    fetcher = { "{broken json" },
                )
            val result = service.refreshForServer("http://192.168.1.157:8096")
            assertEquals(RemoteWholphinConfig.DEFAULT, result)
        }

    @Test
    fun mapToAppPreferences_appliesServerSettingsOverExistingProtoDefaults() {
        val base =
            AppPreferences
                .getDefaultInstance()
                .updateInterfacePreferences {
                    backdropStyle = BackdropStyle.BACKDROP_DYNAMIC_COLOR
                    playThemeSongs = ThemeSongVolume.HIGH
                    enableAnimations = true
                    showTvSettings = true
                }
        val mapped = RemoteConfigService.mapToAppPreferences(base, RemoteWholphinConfig.DEFAULT)

        assertEquals(BackdropStyle.BACKDROP_NONE, mapped.interfacePreferences.backdropStyle)
        assertEquals(ThemeSongVolume.DISABLED, mapped.interfacePreferences.playThemeSongs)
        assertFalse(mapped.interfacePreferences.enableAnimations)
        assertFalse(mapped.interfacePreferences.showTvSettings)
        assertTrue(mapped.interfacePreferences.screensaverPreference.enabled)
        assertFalse(mapped.interfacePreferences.screensaverPreference.animate)
        assertEquals(300_000L, mapped.interfacePreferences.screensaverPreference.startDelay)
        assertEquals(15_000L, mapped.interfacePreferences.screensaverPreference.duration)
        assertEquals(PlayerBackend.EXO_PLAYER, mapped.playbackPreferences.playerBackend)
        assertEquals(SkipSegmentBehavior.ASK_TO_SKIP, mapped.playbackPreferences.skipIntros)
        assertTrue(mapped.interfacePreferences.searchPreferences.combinedSearchResults)
    }
}
