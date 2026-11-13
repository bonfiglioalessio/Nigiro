package com.github.damontecres.wholphin.ui.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.components.DialogPopup
import com.github.damontecres.wholphin.ui.components.ErrorViewModel
import com.github.damontecres.wholphin.ui.components.TextButton
import com.github.damontecres.wholphin.ui.components.chooseVersionParams
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.LoadingState
import org.jellyfin.sdk.model.api.MediaSourceInfo

/**
 * Shown when playback cannot start, with actions to recover instead of a dead end
 */
@Composable
fun PlaybackErrorPage(
    error: LoadingState.Error,
    sources: List<MediaSourceInfo>,
    onRetry: () -> Unit,
    onChooseSource: (MediaSourceInfo) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ErrorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val retryFocusRequester = remember { FocusRequester() }
    var showVersions by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { retryFocusRequester.tryRequestFocus() }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.playback_error_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
        )
        error.message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        error.exception?.localizedMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(
                stringRes = R.string.retry,
                onClick = onRetry,
                modifier = Modifier.focusRequester(retryFocusRequester),
            )
            if (sources.size > 1) {
                TextButton(
                    stringRes = R.string.playback_try_another_version,
                    onClick = { showVersions = true },
                )
            }
            TextButton(
                stringRes = R.string.go_back,
                onClick = onBack,
            )
            TextButton(
                stringRes = R.string.send_app_logs,
                onClick = { viewModel.sendLogs() },
            )
        }
    }
    if (showVersions) {
        DialogPopup(
            params =
                chooseVersionParams(
                    resources = context.resources,
                    sources = sources,
                    chosenSourceId = null,
                    onClick = { index -> sources.getOrNull(index)?.let(onChooseSource) },
                ),
            onDismissRequest = { showVersions = false },
        )
    }
}
