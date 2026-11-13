package com.github.damontecres.wholphin.data.model

import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
data class RemoteWholphinConfig(
    val schemaVersion: Int = 1,
    val configRevision: String = "default",
    val ui: RemoteUiConfig = RemoteUiConfig(),
    val screensaver: RemoteScreensaverConfig = RemoteScreensaverConfig(),
    val playback: RemotePlaybackConfig = RemotePlaybackConfig(),
    val search: RemoteSearchConfig = RemoteSearchConfig(),
    val home: RemoteHomeConfig = RemoteHomeConfig(),
) {
    companion object {
        val DEFAULT = RemoteWholphinConfig()
    }
}

@Serializable
data class RemoteUiConfig(
    val flatMode: Boolean = true,
    val animationsEnabled: Boolean = false,
    val backdropStyle: String = "NONE",
    val showClock: Boolean = true,
    val showLogos: Boolean = true,
    val hideTvSettings: Boolean = true,
    val navDrawerSwitchOnFocus: Boolean = false,
    val modules: RemoteModulesConfig = RemoteModulesConfig(),
)

@Serializable
data class RemoteModulesConfig(
    val discover: Boolean = true,
    val screensaver: Boolean = true,
    val liveTv: Boolean = false,
    val music: Boolean = false,
    val photos: Boolean = false,
)

@Serializable
data class RemoteScreensaverConfig(
    val enabled: Boolean = true,
    val animate: Boolean = false,
    val startDelaySeconds: Long = 300L,
    val durationSeconds: Long = 15L,
    val dimEnabled: Boolean = true,
    val dimPercent: Int = 35,
    val showClock: Boolean = true,
)

@Serializable
data class RemotePlaybackConfig(
    val playerBackend: String = "EXO_PLAYER",
    val maxBitrate: Long = 100_000_000L,
    val refreshRateSwitching: Boolean = true,
    val resolutionSwitching: Boolean = false,
    val skipIntros: String = "ASK",
    val skipOutros: String = "ASK",
    val autoPlayNext: Boolean = true,
    val autoPlayNextDelaySeconds: Long = 10L,
    val preferredAudioLanguage: String = "ita",
    val preferredSubtitleLanguage: String = "ita",
)

@Serializable
data class RemoteSearchConfig(
    val combinedResults: Boolean = true,
    val showVoiceSearchButton: Boolean = true,
)

@Serializable
data class RemoteHomeConfig(
    val defaultFilter: String = "all",
    val maxItemsPerRow: Int = 24,
    val enableRewatchingNextUp: Boolean = false,
    val includeNativeResumeNextUp: Boolean = true,
    val hero: RemoteHeroConfig = RemoteHeroConfig(),
    val platforms: List<RemotePlatformConfig> = DEFAULT_PLATFORMS,
) {
    companion object {
        val DEFAULT_PLATFORMS =
            listOf(
                RemotePlatformConfig(
                    key = "popular",
                    name = "Top 10 Italia",
                    movieCollectionId = "f9ef9d46e44c0a46b4318dcdaba3631e",
                    seriesCollectionId = "cedaa4db8850f9269de7f6834cf4a71b",
                    isHome = true,
                ),
                RemotePlatformConfig(
                    key = "netflix",
                    name = "Netflix",
                    movieCollectionId = "a2eed3870d64d88845c9ca39f727f870",
                    seriesCollectionId = "1f79d5a9eaebccdf68388c73fc5f46f1",
                    isHome = false,
                ),
                RemotePlatformConfig(
                    key = "prime",
                    name = "Prime Video",
                    movieCollectionId = "8be199302adb2f8647fe1a7990884ae1",
                    seriesCollectionId = "1610e2cc6f603e50ce551ac57e136b02",
                    isHome = false,
                ),
                RemotePlatformConfig(
                    key = "disney",
                    name = "Disney+",
                    movieCollectionId = "ec81336d9e40609c5fc8051d8cb63836",
                    seriesCollectionId = "99146fe62674dcd84f95207679749ea7",
                    isHome = false,
                ),
                RemotePlatformConfig(
                    key = "apple",
                    name = "Apple TV+",
                    movieCollectionId = "d46f64a398f269b11464b3744a87083f",
                    seriesCollectionId = "8e8b018d5f74a92b930b81be96caebfc",
                    isHome = false,
                ),
                RemotePlatformConfig(
                    key = "sky",
                    name = "Sky / HBO",
                    movieCollectionId = "1f6f5f6661cc120bc5773beec9a4bddf",
                    seriesCollectionId = "bbe13af056233dbc759c0e0334ecf583",
                    isHome = false,
                ),
                RemotePlatformConfig(
                    key = "para",
                    name = "Paramount+",
                    movieCollectionId = "44f3a7f96facef58cfa61c5902a92e0a",
                    seriesCollectionId = "97f2174bc85231990b7967dd79640c3d",
                    isHome = false,
                ),
            )
    }
}

@Serializable
data class RemoteHeroConfig(
    val enabled: Boolean = true,
    val sourceKey: String = "popular",
    val count: Int = 6,
    val intervalMs: Long = 9_000L,
    val crossfadeMs: Int = 280,
)

@Serializable
data class RemotePlatformConfig(
    val key: String,
    val name: String,
    val movieCollectionId: String? = null,
    val seriesCollectionId: String? = null,
    val isHome: Boolean = false,
)

object RemoteConfigUrlResolver {
    fun candidateUrls(
        serverUrl: String?,
        overrideUrl: String? = null,
    ): List<String> {
        val urls = LinkedHashSet<String>()
        overrideUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { urls.add(it) }

        val normalizedInput = serverUrl?.trim().orEmpty()
        if (normalizedInput.isNotEmpty()) {
            val withScheme =
                if (normalizedInput.startsWith("http://", ignoreCase = true) ||
                    normalizedInput.startsWith("https://", ignoreCase = true)
                ) {
                    normalizedInput
                } else {
                    "http://$normalizedInput"
                }
            val parsed = withScheme.toHttpUrlOrNull()
            if (parsed != null) {
                fun addConfigPaths(base: okhttp3.HttpUrl) {
                    urls.add(
                        base
                            .newBuilder()
                            .encodedPath("/nigiro-config.json")
                            .query(null)
                            .fragment(null)
                            .build()
                            .toString(),
                    )
                    urls.add(
                        base
                            .newBuilder()
                            .encodedPath("/wholphin-config.json")
                            .query(null)
                            .fragment(null)
                            .build()
                            .toString(),
                    )
                }

                val rootNoPort =
                    parsed
                        .newBuilder()
                        .port(if (parsed.isHttps) 443 else 80)
                        .build()
                if (parsed.port != 80 && parsed.port != 443 && parsed.port != 8096 && parsed.port != 8920) {
                    addConfigPaths(parsed)
                }
                addConfigPaths(rootNoPort)
                if (parsed.port == 8096 || parsed.port == 8920) {
                    addConfigPaths(parsed)
                }
            }
        }
        return urls.toList()
    }
}

object RemoteConfigRuntime {
    @Volatile
    var current: RemoteWholphinConfig? = null
}
