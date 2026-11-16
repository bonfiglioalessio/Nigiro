package com.github.damontecres.wholphin.ui.detail.movie

import android.content.Context
import android.widget.Toast
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ItemPlaybackRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.services.LocalTwinService
import com.github.damontecres.wholphin.services.StreamChoiceService
import com.github.damontecres.wholphin.services.UserPreferencesService
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.showToast
import com.github.damontecres.wholphin.util.SourceRanking
import com.github.damontecres.wholphin.util.WholphinDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import kotlin.time.Duration

/** An open version picker: the item, what it shows, and the downloaded copy that may be one of the rows */
data class PickRequest(
    val item: BaseItem,
    val picker: PickerState,
    val localCopy: BaseItem? = null,
)

/**
 * Play and "Choose a version" for an item whose versions are not loaded yet, such as an episode from a list.
 * Play starts right away (the player takes the version chosen before, else the one last played, else the best
 * Italian one); choosing loads the versions and opens the picker, or plays straight away if there is one.
 */
class PlayVersionPicker(
    private val scope: CoroutineScope,
    private val context: Context,
    private val api: ApiClient,
    private val itemPlaybackRepository: ItemPlaybackRepository,
    private val userPreferencesService: UserPreferencesService,
    private val localTwinService: LocalTwinService,
    private val streamChoiceService: StreamChoiceService,
    private val navigate: (Destination) -> Unit,
) {
    private val _request = MutableStateFlow<PickRequest?>(null)
    val request: StateFlow<PickRequest?> = _request
    private var job: Job? = null

    fun play(
        item: BaseItem,
        position: Duration,
    ) {
        dismiss()
        navigate(Destination.Playback(item.id, position.inWholeMilliseconds))
    }

    fun choose(
        item: BaseItem,
        position: Duration,
    ) {
        job?.cancel()
        job =
            scope.launchIO {
                // Gelato looks the streams up when the item is fetched: show the picker loading only if it takes
                // long enough to notice, so a file on disk starts without a flash
                val load = async { loadOptions(item) }
                val showLoading =
                    launch {
                        delay(LOADING_AFTER_MS)
                        _request.value = PickRequest(item, PickerState.Loading(position))
                    }
                val loaded = load.await()
                showLoading.cancel()
                if (loaded == null) {
                    _request.value = null
                    showToast(context, context.getString(R.string.sources_unavailable), Toast.LENGTH_LONG)
                    return@launchIO
                }
                val (full, localCopy, options) = loaded
                when (val decision = decidePlay(options)) {
                    is PlayDecision.PlayNow -> {
                        _request.value = null
                        start(full, position)
                    }

                    is PlayDecision.Pick -> {
                        val prefs = userPreferencesService.getCurrent()
                        // The version picked before, else the one last played: never just the default
                        val chosenSource =
                            streamChoiceService.lastSourceId(
                                full.id,
                                itemPlaybackRepository.getSelectedTracks(full.id, full, prefs)?.itemPlayback,
                            )
                        val chosenId =
                            chosenSource?.let { id ->
                                decision.options.firstOrNull { it.source.id?.toUUIDOrNull() == id }?.source?.id
                            }
                        _request.value = PickRequest(full, PickerState.Options(decision.options, position, chosenId), localCopy)
                    }
                }
            }
    }

    private suspend fun loadOptions(item: BaseItem): Triple<BaseItem, BaseItem?, List<VersionOption>>? =
        try {
            val full = BaseItem(api.userLibraryApi.getItem(item.id).content)
            val localCopy =
                try {
                    localTwinService.findLocalTwin(full.data)?.let { BaseItem(it) }
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    Timber.w(ex, "Could not look for a local copy of %s", item.id)
                    null
                }
            val options =
                versionOptions(
                    // Same order as the version menu: preferred language, cached debrid, quality
                    streamed =
                        SourceRanking.rank(
                            full.data.mediaSources.orEmpty().filter { it.id.isNotNullOrBlank() },
                            capabilities = null,
                        ),
                    local = localCopy?.data?.mediaSources.orEmpty(),
                    localLabel = context.getString(R.string.on_disk),
                )
            Triple(full, localCopy, options)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to fetch sources for %s", item.id)
            null
        }

    fun pick(
        option: VersionOption,
        position: Duration,
    ) {
        val request = _request.value ?: return
        _request.value = null
        job?.cancel()
        job =
            scope.launchIO {
                val item = request.item
                if (option.isLocal && request.localCopy != null) {
                    // Forget a streamed version chosen before, so playback uses the file on disk again
                    val prefs = userPreferencesService.getCurrent()
                    itemPlaybackRepository
                        .getSelectedTracks(item.id, item, prefs)
                        ?.takeIf { it.itemPlayback?.sourceId != null }
                        ?.let { itemPlaybackRepository.deleteChosenStreams(it) }
                } else {
                    option.source.id
                        ?.toUUIDOrNull()
                        ?.let { itemPlaybackRepository.savePlayVersion(item.id, it) }
                }
                start(item, position)
            }
    }

    fun dismiss() {
        job?.cancel()
        _request.value = null
    }

    private suspend fun start(
        item: BaseItem,
        position: Duration,
    ) = withContext(WholphinDispatchers.Main) {
        navigate(Destination.Playback(item.id, position.inWholeMilliseconds))
    }

    private companion object {
        const val LOADING_AFTER_MS = 250L
    }
}
