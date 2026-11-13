package com.github.damontecres.wholphin.util

import androidx.media3.common.PlaybackException

/**
 * Broad categories of playback failures, used to decide how to recover
 */
enum class PlaybackErrorKind {
    /**
     * The media could not be fetched (dead link, HTTP error, network failure). Another media source may work.
     */
    SOURCE_UNAVAILABLE,

    /**
     * The media was fetched but could not be decoded or parsed. Transcoding the same source may work.
     */
    DECODING,

    /**
     * Anything else
     */
    OTHER,
}

object PlaybackErrorClassifier {
    fun classify(errorCode: Int): PlaybackErrorKind =
        when (errorCode) {
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            -> PlaybackErrorKind.SOURCE_UNAVAILABLE

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            -> PlaybackErrorKind.DECODING

            else -> PlaybackErrorKind.OTHER
        }
}
