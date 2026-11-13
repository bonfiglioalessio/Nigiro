package com.github.damontecres.wholphin.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Does not retry loads that failed with an HTTP 4xx status.
 *
 * A 4xx from a debrid/remote link (expired, removed, forbidden) will not recover by retrying,
 * so failing fast lets the player move on to another media source instead of spinning
 * through the default retries. Other errors (5xx, timeouts, network blips) keep the default
 * retry behavior so that a brief network hiccup during playback does not abort the stream.
 */
@OptIn(UnstableApi::class)
class FastFailLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
        if (isClientError(loadErrorInfo.exception)) {
            C.TIME_UNSET
        } else {
            super.getRetryDelayMsFor(loadErrorInfo)
        }

    companion object {
        fun isClientError(exception: Throwable?): Boolean {
            var current = exception
            while (current != null) {
                if (current is HttpDataSource.InvalidResponseCodeException) {
                    return current.responseCode in 400..499
                }
                current = current.cause
            }
            return false
        }
    }
}
