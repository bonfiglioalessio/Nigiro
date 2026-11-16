package com.github.damontecres.wholphin.util

import android.media.MediaFormat
import com.github.damontecres.wholphin.util.profile.MediaCodecCapabilitiesTest
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import timber.log.Timber

/**
 * The video decoding capabilities of this device, as far as choosing a media source is concerned
 */
data class DecodeCapabilities(
    val hevc: Boolean = true,
    val av1: Boolean = true,
    val vp9: Boolean = true,
    val dolbyVision: Boolean = true,
    val maxHeight: Int = 0,
) {
    fun canDecode(traits: SourceTraits): Boolean {
        val codecOk =
            when (traits.videoCodec) {
                SourceVideoCodec.HEVC -> hevc
                SourceVideoCodec.AV1 -> av1
                SourceVideoCodec.VP9 -> vp9
                SourceVideoCodec.H264, null -> true
            }
        val heightOk = maxHeight <= 0 || traits.height == null || traits.height <= maxHeight
        return codecOk && heightOk
    }

    companion object {
        fun from(test: MediaCodecCapabilitiesTest): DecodeCapabilities =
            try {
                DecodeCapabilities(
                    hevc = test.supportsHevc(),
                    av1 = test.supportsAV1(),
                    vp9 = true,
                    dolbyVision = test.supportsHevcDolbyVision(),
                    maxHeight =
                        maxOf(
                            test.getMaxResolution(MediaFormat.MIMETYPE_VIDEO_HEVC).height,
                            test.getMaxResolution(MediaFormat.MIMETYPE_VIDEO_AVC).height,
                        ),
                )
            } catch (ex: Exception) {
                Timber.w(ex, "Could not compute decode capabilities")
                DecodeCapabilities()
            }
    }
}

/**
 * Whether the source streams from elsewhere rather than being a file on the server. Gelato's sources claim to
 * be local files (protocol File, not remote) with "/stub" as their path, so the path decides for them.
 */
fun MediaSourceInfo.isStreamed(): Boolean {
    val path = path
    return isRemote || protocol != MediaProtocol.FILE || path == null || path == GELATO_STUB_PATH ||
        path.startsWith("gelato://")
}

private const val GELATO_STUB_PATH = "/stub"

/**
 * Orders media sources from the most to the least likely to play well on this device.
 *
 * The sort is stable and only uses coarse adjustments, so the order provided by the server
 * (eg AIOStreams' own sorting, which already prioritizes Italian & cached streams) is kept
 * unless a source is clearly better (a local file) or clearly worse (can't be decoded,
 * bitrate too high for the network, CAM release).
 */
object SourceRanking {
    /**
     * Above this the stream risks buffering over the TV's Wi-Fi and fills its small memory
     */
    const val HIGH_BITRATE = 40_000_000L

    fun score(
        source: MediaSourceInfo,
        traits: SourceTraits,
        capabilities: DecodeCapabilities?,
        runtimeTicks: Long?,
        maxBitrate: Long?,
    ): Int {
        var score = 0
        // A local file (eg downloaded via Seerr) is probed and served on the LAN: most reliable
        if (!source.isStreamed()) score += 100
        if (capabilities != null && !capabilities.canDecode(traits)) score -= 100
        if (traits.quality == SourceQuality.CAM) score -= 80
        val bitrate = traits.estimatedBitrate(runtimeTicks ?: source.runTimeTicks)
        if (bitrate != null) {
            val limit = maxBitrate?.takeIf { it > 0 }?.coerceAtMost(HIGH_BITRATE) ?: HIGH_BITRATE
            if (bitrate > limit) score -= 50
        } else if (traits.quality == SourceQuality.REMUX) {
            // Remuxes are usually far above what the Wi-Fi can sustain
            score -= 50
        }
        if (traits.italian) score += 20 else if (traits.multiAudio) score += 10
        if (source.isStreamed() && traits.cached) score += 10
        // Not cached on the debrid service: it has to be downloaded first, so it won't start right away
        if (source.isStreamed() && traits.uncached) score -= 40
        return score
    }

    /**
     * The TV's Wi-Fi downloads at about 20 Mbps: a stream averaging more than three quarters of it risks
     * buffering, as video bitrate peaks well above its average
     */
    const val WIFI_BUDGET = 15_000_000L

    /** Below this a bitrate the server reports for a stream is a probing artifact, not the video's */
    private const val MIN_PLAUSIBLE_BITRATE = 200_000L

    /**
     * The source Play takes when nothing was chosen: a file on the server first, then the Italian streams
     * (any stream when none is Italian), and among those the highest resolution whose size fits the Wi-Fi:
     * 4K when it is light enough, else 1080p and so on. Within a resolution the [rank] order decides, which
     * puts cached debrid streams first. Undecodable, CAM and uncached streams are only taken if nothing else
     * is left; if nothing fits the Wi-Fi the lightest Italian stream is taken.
     */
    fun best(
        sources: List<MediaSourceInfo>,
        capabilities: DecodeCapabilities?,
        runtimeTicks: Long? = null,
        budget: Long = WIFI_BUDGET,
    ): MediaSourceInfo? {
        if (sources.isEmpty()) return null
        val ranked = rank(sources, capabilities, runtimeTicks, budget)
        ranked.firstOrNull { !it.isStreamed() }?.let { return it }
        val traits = ranked.associateWith { StreamSourceParser.parse(it) }
        fun traitsOf(source: MediaSourceInfo) = traits.getValue(source)
        val playable =
            ranked
                .filter { source ->
                    val t = traitsOf(source)
                    (capabilities?.canDecode(t) ?: true) && t.quality != SourceQuality.CAM && !t.uncached
                }.ifEmpty { ranked }
        val pool = playable.filter { traitsOf(it).italian }.ifEmpty { playable }
        // The size in the addon's name over the movie's length; the server's own figures for a stream come from
        // probing a few seconds of it (bitrates of 2 b/s, runtimes of 30 s), so they are a last resort
        fun bitrateOf(source: MediaSourceInfo): Long? {
            val t = traitsOf(source)
            val runtime = runtimeTicks ?: source.runTimeTicks
            val fromSize = t.copy(bitrate = null).estimatedBitrate(runtime)
            return fromSize ?: t.bitrate?.takeIf { it >= MIN_PLAUSIBLE_BITRATE }
        }
        val fitting =
            pool.filter { source ->
                val bitrate = bitrateOf(source)
                // Without a size a remux is still likely too heavy; anything else is given a chance
                if (bitrate != null) bitrate <= budget else traitsOf(source).quality != SourceQuality.REMUX
            }
        if (fitting.isNotEmpty()) {
            val top = fitting.maxOf { traitsOf(it).height ?: 0 }
            return fitting.first { (traitsOf(it).height ?: 0) == top }
        }
        return pool.minByOrNull { bitrateOf(it) ?: Long.MAX_VALUE }
    }

    fun rank(
        sources: List<MediaSourceInfo>,
        capabilities: DecodeCapabilities?,
        runtimeTicks: Long? = null,
        maxBitrate: Long? = null,
    ): List<MediaSourceInfo> =
        sources
            .map { it to score(it, StreamSourceParser.parse(it), capabilities, runtimeTicks, maxBitrate) }
            .sortedByDescending { it.second }
            .map { it.first }
}
