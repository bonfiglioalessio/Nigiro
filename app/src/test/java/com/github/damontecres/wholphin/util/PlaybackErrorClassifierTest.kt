package com.github.damontecres.wholphin.util

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@UnstableApi
class PlaybackErrorClassifierTest {
    @Test
    fun httpAndNetworkErrorsMeanTheSourceIsUnavailable() {
        listOf(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        ).forEach {
            assertEquals(PlaybackErrorKind.SOURCE_UNAVAILABLE, PlaybackErrorClassifier.classify(it))
        }
    }

    @Test
    fun decoderAndParsingErrorsCanBeTranscoded() {
        listOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        ).forEach {
            assertEquals(PlaybackErrorKind.DECODING, PlaybackErrorClassifier.classify(it))
        }
    }

    @Test
    fun otherErrors() {
        assertEquals(
            PlaybackErrorKind.OTHER,
            PlaybackErrorClassifier.classify(PlaybackException.ERROR_CODE_UNSPECIFIED),
        )
        assertEquals(
            PlaybackErrorKind.OTHER,
            PlaybackErrorClassifier.classify(PlaybackException.ERROR_CODE_DRM_UNSPECIFIED),
        )
    }

    private fun httpError(code: Int) =
        HttpDataSource.InvalidResponseCodeException(
            code,
            null,
            null,
            emptyMap(),
            mockk<DataSpec>(relaxed = true),
            ByteArray(0),
        )

    @Test
    fun clientErrorsAreNotRetried() {
        assertTrue(FastFailLoadErrorHandlingPolicy.isClientError(httpError(404)))
        assertTrue(FastFailLoadErrorHandlingPolicy.isClientError(httpError(403)))
        assertTrue(FastFailLoadErrorHandlingPolicy.isClientError(IOException("wrapped", httpError(410))))
    }

    @Test
    fun serverAndNetworkErrorsAreRetried() {
        assertFalse(FastFailLoadErrorHandlingPolicy.isClientError(httpError(503)))
        assertFalse(FastFailLoadErrorHandlingPolicy.isClientError(IOException("timeout")))
        assertFalse(FastFailLoadErrorHandlingPolicy.isClientError(null))
    }
}
