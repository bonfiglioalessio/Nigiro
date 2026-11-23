package com.github.damontecres.wholphin.services

import androidx.media3.common.MimeTypes
import com.github.damontecres.wholphin.services.hilt.StandardOkHttpClient
import com.github.damontecres.wholphin.util.WholphinDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import timber.log.Timber
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val YOUTUBE_HOSTS =
    setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "youtu.be",
        "www.youtu.be",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com",
    )

private const val MAX_TV_HEIGHT = 1080
internal const val TRAILER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
private const val INNERTUBE_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
private const val IOS_CLIENT_VERSION = "20.10.4"
private const val IOS_USER_AGENT = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
private val YOUTUBE_VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
private val CODECS_REGEX = Regex("""codecs="([^"]+)"""")
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

fun isYouTubeUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return false
    val host = runCatching { URI(trimmed).host?.lowercase(Locale.ROOT) }.getOrNull() ?: return false
    return host in YOUTUBE_HOSTS
}

internal fun extractYouTubeVideoId(url: String): String? {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return null
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    val host = uri.host?.lowercase(Locale.ROOT) ?: return null
    if (host !in YOUTUBE_HOSTS) return null

    val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
    val candidate =
        when {
            host == "youtu.be" || host == "www.youtu.be" -> segments.firstOrNull()
            segments.firstOrNull() == "watch" -> {
                uri.rawQuery
                    .orEmpty()
                    .split('&')
                    .firstNotNullOfOrNull { param ->
                        val eqIdx = param.indexOf('=')
                        if (eqIdx <= 0) return@firstNotNullOfOrNull null
                        val key = param.substring(0, eqIdx)
                        if (key != "v") return@firstNotNullOfOrNull null
                        val rawValue = param.substring(eqIdx + 1)
                        runCatching {
                            URLDecoder.decode(rawValue, StandardCharsets.UTF_8.name())
                        }.getOrDefault(rawValue)
                    }
            }
            segments.firstOrNull() in setOf("embed", "shorts", "live", "v") -> segments.getOrNull(1)
            else -> null
        }
    return candidate?.takeIf { YOUTUBE_VIDEO_ID_REGEX.matches(it) }
}

sealed interface ResolvedTrailerStream {
    data class Single(
        val url: String,
        val mimeType: String? = null,
    ) : ResolvedTrailerStream

    data class Merged(
        val videoUrl: String,
        val audioUrl: String,
    ) : ResolvedTrailerStream
}

internal data class TrailerVideoCandidate(
    val url: String,
    val height: Int,
    val bitrate: Int,
    val fps: Int,
    val codec: String?,
    val formatName: String?,
)

internal data class TrailerAudioCandidate(
    val url: String,
    val averageBitrate: Int,
    val language: String?,
    val isOriginal: Boolean,
    val formatName: String?,
)

private fun videoCodecPriority(candidate: TrailerVideoCandidate): Int {
    val codec = candidate.codec.orEmpty().lowercase(Locale.ROOT)
    val format = candidate.formatName.orEmpty().lowercase(Locale.ROOT)
    return when {
        codec.contains("av01") || codec.contains("av1") || format == "av1" -> 3
        codec.contains("avc") || codec.contains("h264") -> 0
        codec.contains("vp9") || codec.contains("vp09") -> 1
        else -> 2
    }
}

private fun audioLanguagePriority(candidate: TrailerAudioCandidate): Int {
    val lang = candidate.language?.lowercase(Locale.ROOT)
    return when {
        lang == "it" || lang?.startsWith("it-") == true -> 0
        candidate.isOriginal -> 1
        lang.isNullOrBlank() -> 2
        else -> 3
    }
}

private fun audioFormatPriority(candidate: TrailerAudioCandidate): Int {
    val format = candidate.formatName.orEmpty().lowercase(Locale.ROOT)
    return when {
        format.contains("m4a") || format.contains("mp4") -> 0
        else -> 1
    }
}

private fun pickBestVideo(candidates: List<TrailerVideoCandidate>): TrailerVideoCandidate? {
    if (candidates.isEmpty()) return null
    // Avoid software AV1 decoding on 32-bit TVs when hardware-friendly codecs (H.264/VP9) exist
    val nonAv1 = candidates.filter { videoCodecPriority(it) < 3 }.ifEmpty { candidates }
    val capped = nonAv1.filter { it.height in 1..MAX_TV_HEIGHT }
    return if (capped.isNotEmpty()) {
        capped.maxWithOrNull(
            compareBy<TrailerVideoCandidate> { it.height }
                .thenByDescending { videoCodecPriority(it) }
                .thenBy { it.bitrate },
        )
    } else {
        nonAv1.minWithOrNull(
            compareBy<TrailerVideoCandidate> { if (it.height > 0) it.height else Int.MAX_VALUE }
                .thenBy { videoCodecPriority(it) }
                .thenByDescending { it.bitrate },
        )
    }
}

private fun pickBestAudio(candidates: List<TrailerAudioCandidate>): TrailerAudioCandidate? =
    candidates.minWithOrNull(
        compareBy<TrailerAudioCandidate> { audioLanguagePriority(it) }
            .thenBy { audioFormatPriority(it) }
            .thenByDescending { it.averageBitrate },
    )

/**
 * Selects the best TV-friendly trailer stream: adaptive 1080p H.264/VP9 + audio, then muxed progressive,
 * then HLS, then DASH.
 */
internal fun selectBestTrailerStream(
    videoOnly: List<TrailerVideoCandidate>,
    audio: List<TrailerAudioCandidate>,
    muxed: List<TrailerVideoCandidate>,
    hlsUrl: String?,
    dashUrl: String?,
): ResolvedTrailerStream? {
    val bestVideo = pickBestVideo(videoOnly)
    val bestAudio = pickBestAudio(audio)
    if (bestVideo != null && bestAudio != null) {
        return ResolvedTrailerStream.Merged(
            videoUrl = bestVideo.url,
            audioUrl = bestAudio.url,
        )
    }
    pickBestVideo(muxed)?.let { stream ->
        return ResolvedTrailerStream.Single(url = stream.url)
    }
    hlsUrl?.takeIf { it.isNotBlank() }?.let { url ->
        return ResolvedTrailerStream.Single(url = url, mimeType = MimeTypes.APPLICATION_M3U8)
    }
    dashUrl?.takeIf { it.isNotBlank() }?.let { url ->
        return ResolvedTrailerStream.Single(url = url, mimeType = MimeTypes.APPLICATION_MPD)
    }
    return null
}

private data class ParsedMimeInfo(
    val isVideo: Boolean,
    val isAudio: Boolean,
    val codec: String?,
    val formatName: String?,
)

private fun parseInnerTubeMime(mimeType: String): ParsedMimeInfo {
    val isVideo = mimeType.startsWith("video/", ignoreCase = true)
    val isAudio = mimeType.startsWith("audio/", ignoreCase = true)
    val codec = CODECS_REGEX.find(mimeType)?.groupValues?.getOrNull(1)
    val formatName =
        when {
            mimeType.startsWith("audio/mp4", ignoreCase = true) -> "M4A"
            mimeType.startsWith("audio/webm", ignoreCase = true) -> "WEBMA_OPUS"
            mimeType.startsWith("video/mp4", ignoreCase = true) -> "MPEG_4"
            mimeType.startsWith("video/webm", ignoreCase = true) -> "WEBM"
            else -> null
        }
    return ParsedMimeInfo(
        isVideo = isVideo,
        isAudio = isAudio,
        codec = codec,
        formatName = formatName,
    )
}

private fun JsonObject.toInnerTubeVideoCandidate(mime: ParsedMimeInfo): TrailerVideoCandidate? {
    val url = this["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
    return TrailerVideoCandidate(
        url = url,
        height = this["height"]?.jsonPrimitive?.intOrNull ?: 0,
        bitrate = this["bitrate"]?.jsonPrimitive?.intOrNull ?: 0,
        fps = this["fps"]?.jsonPrimitive?.intOrNull ?: 0,
        codec = mime.codec,
        formatName = mime.formatName,
    )
}

private fun JsonObject.toInnerTubeAudioCandidate(mime: ParsedMimeInfo): TrailerAudioCandidate? {
    val url = this["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
    val audioTrack = this["audioTrack"] as? JsonObject
    val language = audioTrack?.get("id")?.jsonPrimitive?.contentOrNull?.substringBefore('.')
    val isOriginal =
        audioTrack?.get("displayName")?.jsonPrimitive?.contentOrNull?.contains("original", ignoreCase = true) == true ||
            audioTrack?.get("audioIsDefault")?.jsonPrimitive?.booleanOrNull == true
    val averageBitrate =
        this["averageBitrate"]?.jsonPrimitive?.intOrNull
            ?: this["bitrate"]?.jsonPrimitive?.intOrNull
            ?: 0
    return TrailerAudioCandidate(
        url = url,
        averageBitrate = averageBitrate,
        language = language,
        isOriginal = isOriginal,
        formatName = mime.formatName,
    )
}

internal fun parseInnerTubePlayerResponse(jsonBody: String): ResolvedTrailerStream? {
    val root = runCatching { Json.parseToJsonElement(jsonBody) as? JsonObject }.getOrNull() ?: return null
    val status = (root["playabilityStatus"] as? JsonObject)?.get("status")?.jsonPrimitive?.contentOrNull
    if (status != "OK") return null

    val streamingData = root["streamingData"] as? JsonObject ?: return null
    val videoOnly = mutableListOf<TrailerVideoCandidate>()
    val audio = mutableListOf<TrailerAudioCandidate>()
    val muxed = mutableListOf<TrailerVideoCandidate>()

    (streamingData["adaptiveFormats"] as? JsonArray)?.forEach { element ->
        val obj = element as? JsonObject ?: return@forEach
        val mimeType = obj["mimeType"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@forEach
        val mime = parseInnerTubeMime(mimeType)
        when {
            mime.isVideo -> obj.toInnerTubeVideoCandidate(mime)?.let(videoOnly::add)
            mime.isAudio -> obj.toInnerTubeAudioCandidate(mime)?.let(audio::add)
        }
    }

    (streamingData["formats"] as? JsonArray)?.forEach { element ->
        val obj = element as? JsonObject ?: return@forEach
        val mimeType = obj["mimeType"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@forEach
        val mime = parseInnerTubeMime(mimeType)
        if (mime.isVideo) {
            obj.toInnerTubeVideoCandidate(mime)?.let(muxed::add)
        }
    }

    val hlsUrl = streamingData["hlsManifestUrl"]?.jsonPrimitive?.contentOrNull
    val dashUrl = streamingData["dashManifestUrl"]?.jsonPrimitive?.contentOrNull
    return selectBestTrailerStream(
        videoOnly = videoOnly,
        audio = audio,
        muxed = muxed,
        hlsUrl = hlsUrl,
        dashUrl = dashUrl,
    )
}

internal class OkHttpNewPipeDownloader(
    baseClient: OkHttpClient,
) : Downloader() {
    private val client =
        baseClient
            .newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())
        var hasUserAgent = false
        for ((name, values) in request.headers()) {
            if (name.equals("User-Agent", ignoreCase = true)) {
                hasUserAgent = true
            }
            builder.removeHeader(name)
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }
        if (!hasUserAgent) {
            builder.header("User-Agent", TRAILER_USER_AGENT)
        }
        val method = request.httpMethod()
        val data = request.dataToSend()
        val body =
            when {
                data != null -> data.toRequestBody(null)
                method.equals("POST", ignoreCase = true) -> ByteArray(0).toRequestBody(null)
                else -> null
            }
        builder.method(method, body)

        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", request.url())
            }
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body.string(),
                response.request.url.toString(),
            )
        }
    }
}

@Singleton
class YouTubeTrailerExtractor
    @Inject
    constructor(
        @param:StandardOkHttpClient private val okHttpClient: OkHttpClient,
    ) {
        @Volatile
        private var initialized = false

        private val innerTubeClient: OkHttpClient by lazy {
            okHttpClient
                .newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }

        private fun ensureInitialized() {
            if (initialized) return
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(
                        OkHttpNewPipeDownloader(okHttpClient),
                        Localization("it", "IT"),
                        ContentCountry("IT"),
                    )
                    initialized = true
                }
            }
        }

        suspend fun extract(url: String): ResolvedTrailerStream? =
            withContext(WholphinDispatchers.IO) {
                extractWithNewPipe(url) ?: extractWithIosInnerTube(url)
            }

        private fun extractWithNewPipe(url: String): ResolvedTrailerStream? =
            try {
                ensureInitialized()
                val info = StreamInfo.getInfo(ServiceList.YouTube, url)
                val videoOnly =
                    info.videoOnlyStreams
                        .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.content.isNotBlank() }
                        .map {
                            TrailerVideoCandidate(
                                url = it.content,
                                height = it.height,
                                bitrate = it.bitrate,
                                fps = it.fps,
                                codec = it.codec,
                                formatName = it.format?.name,
                            )
                        }
                val audio =
                    info.audioStreams
                        .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.content.isNotBlank() }
                        .map {
                            TrailerAudioCandidate(
                                url = it.content,
                                averageBitrate = it.averageBitrate,
                                language = it.audioLocale?.language,
                                isOriginal = it.audioTrackType == AudioTrackType.ORIGINAL,
                                formatName = it.format?.name,
                            )
                        }
                val muxed =
                    info.videoStreams
                        .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.content.isNotBlank() }
                        .map {
                            TrailerVideoCandidate(
                                url = it.content,
                                height = it.height,
                                bitrate = it.bitrate,
                                fps = it.fps,
                                codec = it.codec,
                                formatName = it.format?.name,
                            )
                        }
                val resolved =
                    selectBestTrailerStream(
                        videoOnly = videoOnly,
                        audio = audio,
                        muxed = muxed,
                        hlsUrl = info.hlsUrl,
                        dashUrl = info.dashMpdUrl,
                    )
                Timber.i(
                    "Extracted YouTube trailer (%s): videoOnly=%d audio=%d muxed=%d resolved=%s",
                    info.name,
                    videoOnly.size,
                    audio.size,
                    muxed.size,
                    resolved?.javaClass?.simpleName,
                )
                resolved
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.w(ex, "Failed extracting YouTube trailer stream via NewPipe for %s", url)
                null
            }

        private fun extractWithIosInnerTube(url: String): ResolvedTrailerStream? {
            val videoId = extractYouTubeVideoId(url) ?: return null
            return try {
                val payload =
                    buildJsonObject {
                        putJsonObject("context") {
                            putJsonObject("client") {
                                put("clientName", "IOS")
                                put("clientVersion", IOS_CLIENT_VERSION)
                                put("deviceMake", "Apple")
                                put("deviceModel", "iPhone16,2")
                                put("hl", "it")
                                put("gl", "IT")
                            }
                        }
                        put("videoId", videoId)
                    }.toString()
                val request =
                    okhttp3.Request
                        .Builder()
                        .url(INNERTUBE_PLAYER_URL)
                        .header("User-Agent", IOS_USER_AGENT)
                        .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                innerTubeClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.w("iOS InnerTube fallback HTTP %d for %s", response.code, videoId)
                        return null
                    }
                    val body = response.body.string()
                    val resolved = parseInnerTubePlayerResponse(body)
                    Timber.i(
                        "Extracted YouTube trailer via iOS InnerTube fallback (%s): resolved=%s",
                        videoId,
                        resolved?.javaClass?.simpleName,
                    )
                    resolved
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.w(ex, "Failed extracting YouTube trailer stream via iOS InnerTube for %s", url)
                null
            }
        }
    }
