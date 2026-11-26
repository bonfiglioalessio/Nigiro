package com.github.damontecres.wholphin.ui.detail.livetv

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.damontecres.wholphin.ui.detail.movie.AioPillButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.LiveTvPreferences
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.components.CircularProgress
import com.github.damontecres.wholphin.ui.components.DialogItem
import com.github.damontecres.wholphin.ui.components.DialogParams
import com.github.damontecres.wholphin.ui.components.DialogPopup
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.ExpandableFaButton
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.rememberPosition
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.LoadingState
import com.github.damontecres.wholphin.util.WholphinDispatchers
import eu.wewox.programguide.ProgramGuide
import eu.wewox.programguide.ProgramGuideDimensions
import eu.wewox.programguide.ProgramGuideItem
import eu.wewox.programguide.rememberSaveableProgramGuideState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.abs

@Composable
fun TvGuideGrid(
    preferences: UserPreferences,
    onRowPosition: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveTvViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val preferences by viewModel.preferences.data.collectAsState(preferences.appPreferences)
    val tvPrefs = preferences.interfacePreferences.liveTvPreferences
    var showViewOptions by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf<TvChannel?>(null) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalDateTime.now()
        }
    }
    val playChannel: (UUID) -> Unit = { channelId ->
        viewModel.navigationManager.navigateTo(
            Destination.Playback(
                itemId = channelId,
                positionMs = 0L,
            ),
        )
    }

    when (val st = state.loading) {
        is LoadingState.Error -> {
            ErrorMessage(st, modifier)
        }

        LoadingState.Pending,
        -> {
            LoadingPage(modifier)
        }

        LoadingState.Loading,
        LoadingState.Success,
        -> {
            val context = LocalContext.current
            val programDialogState =
                viewModel.programDialogState
                    .collectAsState()
                    .value.loading

            var showItemDialog by remember { mutableStateOf<Int?>(null) }
            val focusRequester = remember { FocusRequester() }
            val buttonFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.tryRequestFocus()
            }
            var focusedPosition by rememberPosition(0, 0)
            val focusedProgram =
                remember(focusedPosition, state.channels, state.programs) {
                    focusedPosition.let {
                        val channelId = state.channels.getOrNull(it.row)?.id
                        state.programs.programsByChannel[channelId]?.getOrNull(it.column)
                    }
                }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier,
            ) {
                val switchingGroup = state.channels.isEmpty() && state.loading == LoadingState.Loading
                if (state.channels.isEmpty() && !switchingGroup) {
                    ErrorMessage("Live TV is enabled, but no channels were found.", null)
                } else {
                    AnimatedVisibility(
                        visible = state.viewMode == LiveTvViewMode.GRID && tvPrefs.showHeader && !switchingGroup,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        TvGuideHeader(
                            program = focusedProgram,
                            channel = state.channels.getOrNull(focusedPosition.row),
                            now = now,
                            modifier =
                                Modifier
                                    .padding(
                                        top = HeaderUtils.topPadding,
                                        bottom = 0.dp,
                                        start = HeaderUtils.startPadding,
                                    ).fillMaxHeight(.30f),
                        )
                    }
                    AnimatedVisibility(
                        focusedPosition.row < 1,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LiveClockBadge(now = now)
                                LiveTvModePills(
                                    selected = state.viewMode,
                                    onSelect = {
                                        viewModel.selectViewMode(it)
                                        focusedPosition = RowColumn(0, 0)
                                        onRowPosition(0)
                                    },
                                )
                                ExpandableFaButton(
                                    title = R.string.view_options,
                                    iconStringRes = R.string.fa_sliders,
                                    onClick = { showViewOptions = true },
                                    modifier = Modifier.focusRequester(buttonFocusRequester),
                                )
                            }
                            if (state.groups.isNotEmpty() || state.hasFavorites) {
                                ChannelGroupChips(
                                    groups = state.groups,
                                    hasFavorites = state.hasFavorites,
                                    selected = state.selectedGroup,
                                    onSelect = {
                                        viewModel.selectGroup(it)
                                        focusedPosition = RowColumn(0, 0)
                                        onRowPosition(0)
                                    },
                                )
                            }
                        }
                    }
                    if (switchingGroup) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgress(Modifier.size(48.dp))
                        }
                    } else {
                        when (state.viewMode) {
                            LiveTvViewMode.NOW,
                            LiveTvViewMode.TONIGHT,
                            -> {
                                key(state.selectedGroup, state.viewMode) {
                                    LiveTvScheduleList(
                                        mode = state.viewMode,
                                        now = now,
                                        refreshing = state.refreshing,
                                        channels = state.channels,
                                        programsByChannel = state.programs.programsByChannel,
                                        onPlayChannel = { channel -> playChannel(channel.id) },
                                        onLongClickChannel = { channel -> showChannelDialog = channel },
                                        onFocusRow = { row ->
                                            focusedPosition = RowColumn(row, 0)
                                            onRowPosition.invoke(row)
                                            viewModel.onFocusChannel(RowColumn(row, 0))
                                        },
                                        focusRequester = focusRequester,
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.background),
                                    )
                                }
                            }

                            LiveTvViewMode.GRID -> {
                                key(state.selectedGroup) {
                                    TvGuideGridContent(
                                        preferences = tvPrefs,
                                        now = now,
                                        refreshing = state.refreshing,
                                        channels = state.channels,
                                        programs = state.programs,
                                        channelProgramCount = state.channelProgramCount,
                                        guideTimes = state.guideTimes,
                                        onClickChannel = { _, channel ->
                                            playChannel(channel.id)
                                        },
                                        onLongClickChannel = { _, channel ->
                                            showChannelDialog = channel
                                        },
                                        onFocus = {
                                            focusedPosition = it
                                            onRowPosition.invoke(it.row)
                                            viewModel.onFocusChannel(it)
                                        },
                                        onClickProgram = { index, program ->
                                            val airingNow =
                                                isProgramAiring(program, now) ||
                                                    (program.isFake && !now.isBefore(program.start) && now.isBefore(program.end))
                                            if (airingNow) {
                                                playChannel(program.channelId)
                                            } else if (program.isFake) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "No guide data found!",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            } else {
                                                viewModel.fetchProgramForDialog(program.id)
                                                showItemDialog = index
                                            }
                                        },
                                        onLongClickProgram = { index, program ->
                                            if (!program.isFake) {
                                                viewModel.fetchProgramForDialog(program.id)
                                                showItemDialog = index
                                            }
                                        },
                                        focusRequester = focusRequester,
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.background),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (showItemDialog != null) {
                val onDismissRequest = { showItemDialog = null }
                ProgramDialog(
                    state = programDialogState,
                    canRecord = true,
                    onDismissRequest = onDismissRequest,
                    onWatch = {
                        onDismissRequest.invoke()
                        val channelId = it.data.channelId
                        if (channelId != null) {
                            viewModel.navigationManager.navigateTo(
                                Destination.Playback(
                                    itemId = channelId,
                                    positionMs = 0L,
                                ),
                            )
                        } else {
                            Toast
                                .makeText(context, "Program has no channel ID", Toast.LENGTH_LONG)
                                .show()
                        }
                    },
                    onRecord = { program, series ->
                        viewModel.record(
                            programId = program.id,
                            series = series,
                        )
                        onDismissRequest.invoke()
                    },
                    onCancelRecord = { program, series ->
                        viewModel.cancelRecording(
                            series = series,
                            timerId = if (series) program.data.seriesTimerId else program.data.timerId,
                        )
                        onDismissRequest.invoke()
                    },
                )
            }
        }
    }
    if (showViewOptions) {
        LiveTvViewOptionsDialog(
            preferences = preferences,
            onDismissRequest = { showViewOptions = false },
            onViewOptionsChange = { newPrefs ->
                scope.launchIO {
                    viewModel.preferences.updateData {
                        newPrefs
                    }
                }
            },
        )
    }
    showChannelDialog?.let { channel ->
        val watchLiveStr = stringResource(R.string.watch_live)
        val dialogItems =
            remember(channel) {
                listOf(
                    DialogItem(
                        watchLiveStr,
                        Icons.Default.PlayArrow,
                        dismissOnClick = true,
                    ) {
                        viewModel.navigationManager.navigateTo(
                            Destination.Playback(
                                itemId = channel.id,
                                positionMs = 0L,
                            ),
                        )
                    },
                    DialogItem(
                        text = if (channel.favorite) R.string.remove_favorite else R.string.add_favorite,
                        iconStringRes = R.string.fa_heart,
                        iconColor = if (channel.favorite) Color.Red else Color.Unspecified,
                        dismissOnClick = true,
                    ) {
                        viewModel.toggleFavorite(channel)
                    },
                )
            }
        DialogPopup(
            onDismissRequest = { showChannelDialog = null },
            params = DialogParams(false, channel.shortName ?: channel.name ?: "", dialogItems),
            elevation = 3.dp,
        )
    }
}

val tvGuideDimensions =
    ProgramGuideDimensions(
        timelineHourWidth = 240.dp,
        timelineHeight = 32.dp,
        channelWidth = 176.dp,
        channelHeight = 60.dp,
        currentTimeWidth = 3.dp,
    )

@Composable
fun TvGuideGridContent(
    preferences: LiveTvPreferences,
    now: LocalDateTime,
    refreshing: Boolean,
    channels: List<TvChannel>,
    programs: FetchedPrograms,
    channelProgramCount: Map<UUID, Int>,
    guideTimes: List<LocalDateTime>,
    onClickChannel: (Int, TvChannel) -> Unit,
    onLongClickChannel: (Int, TvChannel) -> Unit,
    onClickProgram: (Int, TvProgram) -> Unit,
    onLongClickProgram: (Int, TvProgram) -> Unit,
    onFocus: (RowColumn) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state = rememberSaveableProgramGuideState()
    val scope = rememberCoroutineScope()
    val guideStart = remember(guideTimes) { guideTimes.first() }

    var focusedChannelIndex by rememberInt(-1)
    var focusedItem by rememberPosition(RowColumn(0, 0))
    var focusedProgramIndex by rememberInt(0)

    LaunchedEffect(onFocus, focusedProgramIndex) {
        withContext(WholphinDispatchers.Default) {
            val program = programs.programs.getOrNull(focusedProgramIndex)
            if (program != null) {
                val channelIndex = channels.indexOfFirst { it.id == program.channelId }
                val programsBefore =
                    (0..<channelIndex)
                        .mapNotNull {
                            val channel = channels[it]
                            channelProgramCount[channel.id]
                        }.sum()
                val programIndex = focusedProgramIndex - programsBefore
                focusedItem = RowColumn(channelIndex, programIndex)
                onFocus.invoke(focusedItem)
            }
        }
    }
    val programFocusRequester = remember { FocusRequester() }

    BackHandler(focusedItem.row > 0) {
        scope.launch {
            focusedProgramIndex = 0
            state.animateToProgram(0, Alignment.Center)
            programFocusRequester.tryRequestFocus()
        }
    }

    BackHandler(focusedItem.column > 0) {
        scope.launch {
            val programIndex =
                (0..<focusedItem.row)
                    .mapNotNull {
                        val channel = channels[it]
                        channelProgramCount[channel.id]
                    }.sum()
            focusedProgramIndex = programIndex
            state.animateToProgram(programIndex, Alignment.Center)
            programFocusRequester.tryRequestFocus()
        }
    }

    Box(
        modifier = modifier,
    ) {
        ProgramGuide(
            state = state,
            dimensions = tvGuideDimensions,
            modifier = Modifier.fillMaxSize(),
        ) {
            guideStartHour = 0f
            currentTime(
                layoutInfo = {
                    ProgramGuideItem.CurrentTime(
                        hoursBetween(guideStart, now),
                    )
                },
            ) {
                Surface(
                    colors = SurfaceDefaults.colors(LiveRed),
                    modifier = Modifier,
                ) {
                    // Empty
                }
            }
            timeline(
                count = guideTimes.size,
                layoutInfo = { index ->
                    val start = guideTimes[index]
                    val end =
                        if (index < guideTimes.lastIndex) {
                            guideTimes[index + 1]
                        } else {
                            start.plusHours(1)
                        }
                    ProgramGuideItem.Timeline(
                        startHour = hoursBetween(guideStart, start),
                        endHour = hoursBetween(guideStart, end),
                    )
                },
            ) { index ->
                Box(
                    modifier =
                        Modifier
                            // Intentionally set background twice
                            // The second is padded so there are gaps between times
                            // The first covers those gaps
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 2.dp)
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                            ),
                ) {
                    val guideTime = guideTimes[index]
                    val differentDay = guideTime.toLocalDate() != guideStart.toLocalDate()
                    val time =
                        DateUtils.formatDateTime(
                            context,
                            guideTime
                                .toInstant(OffsetDateTime.now().offset)
                                .epochSecond * 1000,
                            DateUtils.FORMAT_SHOW_TIME or if (differentDay) DateUtils.FORMAT_SHOW_WEEKDAY else 0,
                        )
                    Text(
                        text = time.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }
            programs(
                count = programs.programs.size,
                layoutInfo = { programIndex ->
                    val program = programs.programs[programIndex]
                    val channelIndex = channels.indexOfFirst { it.id == program.channelId }
                    // Using the duration for endHour accounts for daylight savings switch
                    // Eg a 1:30am-1:00am show
                    val duration = abs(program.endHours - program.startHours)
                    ProgramGuideItem.Program(
                        channelIndex,
                        program.startHours,
                        program.startHours + duration,
                    )
                },
            ) { programIndex ->
                val program = programs.programs[programIndex]
                Program(
                    guideStart = guideStart,
                    now = now,
                    program = program,
                    colorCode = preferences.colorCodePrograms,
                    onClick = {
                        onClickProgram.invoke(programIndex, program)
                    },
                    onLongClick = {
                        onLongClickProgram.invoke(programIndex, program)
                    },
                    modifier =
                        Modifier
                            .ifElse(
                                programIndex == focusedProgramIndex,
                                Modifier.focusRequester(programFocusRequester),
                            ).ifElse(
                                focusedChannelIndex < 0 && programIndex == focusedProgramIndex,
                                Modifier.focusRequester(focusRequester),
                            ).onFocusChanged {
                                if (it.isFocused) {
                                    focusedChannelIndex = -1
                                    focusedProgramIndex = programIndex
                                    scope.launch {
                                        try {
                                            state.animateToProgram(
                                                programIndex,
                                                Alignment.Center,
                                            )
                                        } catch (ex: Exception) {
                                            Timber.e(ex, "Couldn't scroll to $programIndex")
                                        }
                                    }
                                }
                            },
                )
            }

            channels(
                count = channels.size,
                layoutInfo = { channelIndex ->
                    ProgramGuideItem.Channel(channelIndex)
                },
            ) { channelIndex ->
                val channel = channels[channelIndex]
                Channel(
                    channel = channel,
                    channelIndex = channelIndex,
                    onClick = { onClickChannel.invoke(channelIndex, channel) },
                    onLongClick = { onLongClickChannel.invoke(channelIndex, channel) },
                    modifier =
                        Modifier
                            .ifElse(
                                focusedChannelIndex == channelIndex,
                                Modifier.focusRequester(focusRequester),
                            ).onFocusChanged {
                                if (it.isFocused) {
                                    focusedChannelIndex = channelIndex
                                    scope.launch {
                                        try {
                                            state.animateToChannel(
                                                channelIndex,
                                                Alignment.CenterVertically,
                                            )
                                        } catch (ex: Exception) {
                                            Timber.e(ex, "Couldn't scroll to $channelIndex")
                                        }
                                    }
                                    focusedItem = RowColumn(channelIndex, 0)
                                    onFocus.invoke(focusedItem)
                                }
                            },
                )
            }

            topCorner {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
        if (refreshing) {
            CircularProgress(
                Modifier
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = .5f),
                        shape = CircleShape,
                    ).size(64.dp)
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
            )
        }
    }
}

/**
 * One pill per channel group ("Sport ITA", "Film"...) plus "All": filters the guide to that group.
 * Same pill style as the rest of the Nigiro UI; the selected group is the frosted one.
 */
@Composable
fun ChannelGroupChips(
    groups: List<String>,
    hasFavorites: Boolean,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        remember(groups, hasFavorites) {
            buildList<String?> {
                add(null)
                if (hasFavorites) add(FAVORITES_GROUP_KEY)
                addAll(groups)
            }
        }
    val allLabel = stringResource(R.string.all_channels)
    val favLabel = stringResource(R.string.live_tv_favorites_chip)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        modifier = modifier,
    ) {
        items(options) { group ->
            val label =
                when (group) {
                    null -> allLabel
                    FAVORITES_GROUP_KEY -> favLabel
                    else -> group
                }
            AioPillButton(
                white = group == selected,
                onClick = { if (group != selected) onSelect(group) },
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (group == selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
