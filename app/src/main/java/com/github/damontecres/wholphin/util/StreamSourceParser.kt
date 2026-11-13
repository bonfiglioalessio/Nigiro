package com.github.damontecres.wholphin.util

import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType

enum class SourceVideoCodec { H264, HEVC, AV1, VP9 }

enum class SourceHdr { DOLBY_VISION, HDR10_PLUS, HDR10, HLG }

enum class SourceQuality { REMUX, BLURAY, WEB, HDTV, CAM }

/**
 * What can be inferred about a media source, either from server metadata (probed local files)
 * or from the stream name (remote Gelato/AIOStreams/StreamVix sources are rarely probed).
 */
data class SourceTraits(
    val height: Int? = null,
    val videoCodec: SourceVideoCodec? = null,
    val hdr: SourceHdr? = null,
    val sizeBytes: Long? = null,
    val bitrate: Long? = null,
    val cached: Boolean = false,
    val uncached: Boolean = false,
    val italian: Boolean = false,
    val multiAudio: Boolean = false,
    val quality: SourceQuality? = null,
    val isRemote: Boolean = false,
) {
    /**
     * Known bitrate, or else estimated from the size and runtime
     */
    fun estimatedBitrate(runtimeTicks: Long?): Long? =
        bitrate?.takeIf { it > 0 }
            ?: sizeBytes?.let { size ->
                runtimeTicks?.takeIf { it > 0 }?.let { ticks ->
                    val seconds = ticks / TICKS_PER_SECOND
                    if (seconds > 0) size * 8 / seconds else null
                }
            }

    companion object {
        private const val TICKS_PER_SECOND = 10_000_000L
    }
}

object StreamSourceParser {
    private const val OPTS = "(?i)"
    private const val B = "(?<![\\p{L}\\p{N}])"
    private const val E = "(?![\\p{L}\\p{N}])"

    private val resolutionPatterns =
        listOf(
            Regex("$OPTS${B}(2160p|4k|uhd)$E") to 2160,
            Regex("$OPTS${B}1440p$E") to 1440,
            Regex("$OPTS${B}(1080p|1080i|fhd)$E") to 1080,
            Regex("$OPTS${B}720p$E") to 720,
            Regex("$OPTS${B}(576p|480p|sd)$E") to 480,
        )

    private val codecPatterns =
        listOf(
            Regex("$OPTS$B(hevc|[hx]\\.?265)$E") to SourceVideoCodec.HEVC,
            Regex("$OPTS${B}av1$E") to SourceVideoCodec.AV1,
            Regex("$OPTS${B}vp9$E") to SourceVideoCodec.VP9,
            Regex("$OPTS$B(avc|[hx]\\.?264)$E") to SourceVideoCodec.H264,
        )

    private val hdrPatterns =
        listOf(
            Regex("$OPTS$B(dv|dovi|dolby[ .]?vision)$E") to SourceHdr.DOLBY_VISION,
            Regex("$OPTS${B}hdr10(\\+|plus)") to SourceHdr.HDR10_PLUS,
            Regex("$OPTS$B(hdr10|hdr)$E") to SourceHdr.HDR10,
            Regex("$OPTS${B}hlg$E") to SourceHdr.HLG,
        )

    private val qualityPatterns =
        listOf(
            Regex("$OPTS$B(hd-?cam|cam-?rip|cam|hd-?ts|telesync|telecine|hd-?tc)$E") to SourceQuality.CAM,
            Regex("$OPTS${B}remux$E") to SourceQuality.REMUX,
            Regex("$OPTS$B(blu-?ray|bd-?rip|br-?rip|bdremux)$E") to SourceQuality.BLURAY,
            Regex("$OPTS$B(web-?dl|web-?rip|web)$E") to SourceQuality.WEB,
            Regex("$OPTS${B}hdtv$E") to SourceQuality.HDTV,
        )

    private val sizePattern = Regex("$OPTS(\\d+(?:[.,]\\d+)?)\\s*(tib|tb|gib|gb|mib|mb)$E")

    // ⚡ is how AIOStreams marks a stream cached on the debrid service, "RD+" is the Torrentio style
    private val cachedPattern = Regex("$OPTS(⚡|\\[?(rd|ad|pm|tb|dl)\\+\\]?|${B}cached$E)")
    private val uncachedPattern = Regex("$OPTS(⏳|${B}uncached$E|\\[(rd|ad|pm|tb|dl) download\\])")

    private val italianPattern = Regex("$OPTS(🇮🇹|${B}(ita|italian|italiano)$E)")
    private val multiPattern = Regex("$OPTS${B}(multi|multi-?audio|dual-?audio)$E")

    private val italianLanguageCodes = setOf("ita", "it", "italian", "italiano")

    /**
     * Parses traits from free text such as a stream name or file name
     */
    fun parse(text: String): SourceTraits {
        val uncached = uncachedPattern.containsMatchIn(text)
        val cached = !uncached && cachedPattern.containsMatchIn(text)
        return SourceTraits(
            height = resolutionPatterns.firstOrNull { it.first.containsMatchIn(text) }?.second,
            videoCodec = codecPatterns.firstOrNull { it.first.containsMatchIn(text) }?.second,
            hdr = hdrPatterns.firstOrNull { it.first.containsMatchIn(text) }?.second,
            sizeBytes = parseSize(text),
            cached = cached,
            uncached = uncached,
            italian = italianPattern.containsMatchIn(text),
            multiAudio = multiPattern.containsMatchIn(text),
            quality = qualityPatterns.firstOrNull { it.first.containsMatchIn(text) }?.second,
        )
    }

    /**
     * Combines the server metadata of a [MediaSourceInfo] with what can be parsed from its name/path,
     * preferring the server metadata when it is known
     */
    fun parse(source: MediaSourceInfo): SourceTraits {
        val text =
            listOfNotNull(
                source.name,
                source.path?.takeIf { source.isRemote }?.substringAfterLast('/')?.substringBefore('?'),
            ).joinToString(" ")
        val fromText = parse(text)
        val video = source.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
        val audioLanguages =
            source.mediaStreams
                ?.filter { it.type == MediaStreamType.AUDIO }
                ?.mapNotNull { it.language?.lowercase() }
                .orEmpty()
        return fromText.copy(
            height = video?.height?.takeIf { it > 0 }?.let(::normalizeHeight) ?: fromText.height,
            videoCodec = video?.codec?.let(::codecFromServer) ?: fromText.videoCodec,
            hdr = video?.videoRangeType?.let(::hdrFromServer) ?: fromText.hdr,
            sizeBytes = source.size?.takeIf { it > 0 } ?: fromText.sizeBytes,
            bitrate = source.bitrate?.takeIf { it > 0 }?.toLong(),
            italian = fromText.italian || audioLanguages.any { it in italianLanguageCodes },
            multiAudio = fromText.multiAudio || audioLanguages.distinct().size > 1,
            isRemote = source.isRemote,
        )
    }

    private fun parseSize(text: String): Long? {
        val match = sizePattern.find(text) ?: return null
        val number = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val multiplier =
            when (match.groupValues[2].lowercase()) {
                "tb", "tib" -> 1024.0 * 1024 * 1024 * 1024
                "gb", "gib" -> 1024.0 * 1024 * 1024
                else -> 1024.0 * 1024
            }
        return (number * multiplier).toLong()
    }

    // Wide movies are often 3840x1600, so classify on the closest standard height
    private fun normalizeHeight(height: Int): Int =
        when {
            height > 1440 -> 2160
            height > 1080 -> 1440
            height > 720 -> 1080
            height > 480 -> 720
            else -> 480
        }

    private fun codecFromServer(codec: String): SourceVideoCodec? =
        when (codec.lowercase()) {
            "hevc", "h265" -> SourceVideoCodec.HEVC
            "h264", "avc" -> SourceVideoCodec.H264
            "av1" -> SourceVideoCodec.AV1
            "vp9" -> SourceVideoCodec.VP9
            else -> null
        }

    private fun hdrFromServer(type: VideoRangeType): SourceHdr? =
        when (type) {
            VideoRangeType.DOVI,
            VideoRangeType.DOVI_WITH_HDR10,
            VideoRangeType.DOVI_WITH_HLG,
            VideoRangeType.DOVI_WITH_SDR,
            -> SourceHdr.DOLBY_VISION

            VideoRangeType.HDR10_PLUS -> SourceHdr.HDR10_PLUS
            VideoRangeType.HDR10 -> SourceHdr.HDR10
            VideoRangeType.HLG -> SourceHdr.HLG
            VideoRangeType.SDR, VideoRangeType.UNKNOWN -> null
        }
}
