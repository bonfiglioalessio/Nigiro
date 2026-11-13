package com.github.damontecres.wholphin.util

import android.media.MediaFormat
import com.github.damontecres.wholphin.util.profile.MediaCodecCapabilitiesTest
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
        if (!source.isRemote) score += 100
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
        if (source.isRemote && traits.cached) score += 10
        // Not cached on the debrid service: it has to be downloaded first, so it won't start right away
        if (source.isRemote && traits.uncached) score -= 40
        return score
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
