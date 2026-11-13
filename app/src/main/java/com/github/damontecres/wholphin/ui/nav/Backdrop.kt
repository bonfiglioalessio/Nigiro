@file:OptIn(ExperimentalCoilApi::class)

package com.github.damontecres.wholphin.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.transitionFactory
import com.github.damontecres.wholphin.preferences.BackdropStyle
import com.github.damontecres.wholphin.services.BackdropResult
import com.github.damontecres.wholphin.ui.CrossFadeFactory
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shows the current backdrop images provided by [com.github.damontecres.wholphin.services.BackdropService]
 */
@Composable
fun Backdrop(
    drawerIsOpen: Boolean,
    backdropStyle: BackdropStyle,
    viewModel: ApplicationContentViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    enableTopScrim: Boolean = true,
    useExistingImageAsPlaceholder: Boolean = false,
    crossfadeDuration: Duration = 800.milliseconds,
) {
    val backdrop by viewModel.backdropService.backdropFlow.collectAsStateWithLifecycle()
    Backdrop(
        backdrop = backdrop,
        drawerIsOpen = drawerIsOpen,
        backdropStyle = backdropStyle,
        modifier = modifier,
        enableTopScrim = enableTopScrim,
        useExistingImageAsPlaceholder = useExistingImageAsPlaceholder,
        crossfadeDuration = crossfadeDuration,
    )
}

/**
 * Shows the current backdrop images provided by the [BackdropResult]
 */
@Composable
fun Backdrop(
    backdrop: BackdropResult,
    drawerIsOpen: Boolean,
    backdropStyle: BackdropStyle,
    modifier: Modifier = Modifier,
    enableTopScrim: Boolean = true,
    useExistingImageAsPlaceholder: Boolean = false,
    crossfadeDuration: Duration = 800.milliseconds,
) {
    val enableAnimations = LocalInterfaceCustomization.current.enableAnimations
    val baseBackgroundColor = MaterialTheme.colorScheme.background

    // Flat Litefin background: skip multi-layer animated radial gradients and Offscreen/BlendMode passes
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(baseBackgroundColor),
    )

    if (shouldDrawBackdropImage(backdropStyle, backdrop.imageUrl, enableAnimations)) {
        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(backdrop.imageUrl)
                        .useExistingImageAsPlaceholder(useExistingImageAsPlaceholder)
                        .apply {
                            if (enableAnimations) {
                                transitionFactory(CrossFadeFactory(crossfadeDuration))
                            }
                        }.build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight(.7f)
                        .fillMaxWidth(.7f)
                        .drawWithContent {
                            drawContent()
                            if (drawerIsOpen) {
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.75f),
                                )
                            } else if (enableTopScrim) {
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    Color.Black.copy(alpha = TOP_SCRIM_ALPHA),
                                                    Color.Transparent,
                                                    baseBackgroundColor,
                                                ),
                                        ),
                                )
                            }
                        },
            )
        }
    }
}

internal fun shouldDrawBackdropImage(
    backdropStyle: BackdropStyle,
    imageUrl: String?,
    enableAnimations: Boolean,
): Boolean {
    if (imageUrl.isNullOrBlank()) return false
    return when (backdropStyle) {
        BackdropStyle.BACKDROP_NONE -> false
        BackdropStyle.BACKDROP_IMAGE_ONLY -> true
        BackdropStyle.BACKDROP_DYNAMIC_COLOR,
        BackdropStyle.UNRECOGNIZED,
        -> enableAnimations
    }
}
