package com.github.damontecres.wholphin.util

import androidx.media3.common.MimeTypes
import com.github.damontecres.wholphin.util.profile.Codec

object StreamMimeTypes {
    // Remote web streams (eg StreamVix/VixSrc or a MediaFlow proxy) are often HLS without a .m3u8 file extension
    private val hlsUrlHints = listOf(".m3u8", "/hls/", "/playlist/", "format=m3u8", "type=m3u8")

    /**
     * The MIME type to give ExoPlayer for a stream, or null to let it infer from the URL.
     *
     * Without it, an HLS URL lacking the .m3u8 extension is treated as a progressive file,
     * which fails to parse and falls back to a slow server transcode.
     */
    fun infer(
        container: String?,
        isRemote: Boolean,
        url: String,
    ): String? =
        when (container?.lowercase()) {
            Codec.Container.HLS -> {
                MimeTypes.APPLICATION_M3U8
            }

            Codec.Container.DASH -> {
                MimeTypes.APPLICATION_MPD
            }

            else -> {
                val lowerUrl = url.lowercase()
                if (isRemote && hlsUrlHints.any { lowerUrl.contains(it) }) {
                    MimeTypes.APPLICATION_M3U8
                } else {
                    null
                }
            }
        }
}
