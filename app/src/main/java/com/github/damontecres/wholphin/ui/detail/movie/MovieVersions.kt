package com.github.damontecres.wholphin.ui.detail.movie

import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PersonKind
import java.util.Locale

/**
 * Fields for the first, fast load of a movie page. No [ItemFields.MEDIA_SOURCES]: on the server, asking for
 * the sources of a streamed (Gelato) item makes it look them up on the addons (1.5-5 s when cold), while
 * everything else comes back in ~0.1 s. The sources are loaded separately, in the background or on play.
 */
val LightDetailFields =
    listOf(
        ItemFields.OVERVIEW,
        ItemFields.GENRES,
        ItemFields.PEOPLE,
        ItemFields.STUDIOS,
        ItemFields.TAGLINES,
        ItemFields.PROVIDER_IDS,
        ItemFields.REMOTE_TRAILERS,
        ItemFields.EXTERNAL_URLS,
        ItemFields.PRODUCTION_LOCATIONS,
        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
        ItemFields.SORT_NAME,
        ItemFields.CAN_DELETE,
        ItemFields.MEDIA_SOURCE_COUNT,
        ItemFields.CHAPTERS,
    )

/**
 * One row of the version picker
 */
data class VersionOption(
    val source: MediaSourceInfo,
    val isLocal: Boolean,
    val title: String,
    /** The audio languages, if the server knows them */
    val audio: String?,
)

sealed interface PlayDecision {
    /** Nothing to choose: play [option] (null lets playback pick the item's default) */
    data class PlayNow(
        val option: VersionOption?,
    ) : PlayDecision

    data class Pick(
        val options: List<VersionOption>,
    ) : PlayDecision
}

/**
 * The versions to offer: the downloaded copy first (it needs no addon or debrid service), then the streamed ones
 */
fun versionOptions(
    streamed: List<MediaSourceInfo>,
    local: List<MediaSourceInfo>,
    localLabel: String,
): List<VersionOption> {
    val seen = mutableSetOf<String>()
    val options = mutableListOf<VersionOption>()
    fun add(
        source: MediaSourceInfo,
        isLocal: Boolean,
    ) {
        val key = source.id ?: return
        if (!seen.add(key)) return
        val title =
            if (isLocal) {
                localLabel
            } else {
                source.name?.takeIf { it.isNotBlank() }
                    ?: source.container?.uppercase()
                    ?: localLabel
            }
        options += VersionOption(source, isLocal, title, audioLanguages(source))
    }
    local.forEach { add(it, true) }
    streamed.forEach { add(it, false) }
    return options
}

fun decidePlay(options: List<VersionOption>): PlayDecision =
    if (options.size <= 1) PlayDecision.PlayNow(options.firstOrNull()) else PlayDecision.Pick(options)

private fun audioLanguages(source: MediaSourceInfo): String? =
    source.mediaStreams
        .orEmpty()
        .filter { it.type == MediaStreamType.AUDIO }
        .mapNotNull { stream -> stream.language?.takeIf { it.isNotBlank() }?.let(::languageName) }
        .distinct()
        .joinToString(", ")
        .takeIf { it.isNotEmpty() }

private fun languageName(code: String): String {
    val name = Locale(code).getDisplayLanguage(Locale.ITALIAN).ifBlank { code }
    return name.replaceFirstChar { it.titlecase(Locale.ITALIAN) }
}

private val CastKinds = setOf(PersonKind.ACTOR, PersonKind.GUEST_STAR)

/**
 * Actors first ("Cast"), everybody else ("Troupe"), keeping the server's order
 */
fun <T> splitCastAndCrew(
    people: List<T>,
    kindOf: (T) -> PersonKind,
): Pair<List<T>, List<T>> = people.partition { kindOf(it) in CastKinds }
