package com.github.damontecres.wholphin.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Typography
import com.github.damontecres.wholphin.R

/**
 * Inter, the AIOStreams app's font (packages/ui/theme.css). Bundled from github.com/rsms/inter (v4.1, SIL Open
 * Font License, see assets/licenses/Inter-OFL.txt).
 */
val InterFontFamily =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
        // No heavier cut is bundled: ExtraBold/Black fall back to Bold
        Font(R.font.inter_bold, FontWeight.ExtraBold),
        Font(R.font.inter_bold, FontWeight.Black),
    )

private fun TextStyle.inter() = copy(fontFamily = InterFontFamily)

val AppTypography =
    Typography().let { t ->
        Typography(
            displayLarge = t.displayLarge.inter(),
            displayMedium = t.displayMedium.inter(),
            displaySmall = t.displaySmall.inter(),
            headlineLarge = t.headlineLarge.inter(),
            headlineMedium = t.headlineMedium.inter(),
            headlineSmall = t.headlineSmall.inter(),
            titleLarge = t.titleLarge.inter(),
            titleMedium = t.titleMedium.inter(),
            titleSmall = t.titleSmall.inter(),
            bodyLarge = t.bodyLarge.inter(),
            bodyMedium = t.bodyMedium.inter(),
            bodySmall = t.bodySmall.inter(),
            labelLarge = t.labelLarge.inter(),
            labelMedium = t.labelMedium.inter(),
            labelSmall = t.labelSmall.inter(),
        )
    }

val AppMaterialTypography =
    androidx.compose.material3.Typography().let { t ->
        androidx.compose.material3.Typography(
            displayLarge = t.displayLarge.inter(),
            displayMedium = t.displayMedium.inter(),
            displaySmall = t.displaySmall.inter(),
            headlineLarge = t.headlineLarge.inter(),
            headlineMedium = t.headlineMedium.inter(),
            headlineSmall = t.headlineSmall.inter(),
            titleLarge = t.titleLarge.inter(),
            titleMedium = t.titleMedium.inter(),
            titleSmall = t.titleSmall.inter(),
            bodyLarge = t.bodyLarge.inter(),
            bodyMedium = t.bodyMedium.inter(),
            bodySmall = t.bodySmall.inter(),
            labelLarge = t.labelLarge.inter(),
            labelMedium = t.labelMedium.inter(),
            labelSmall = t.labelSmall.inter(),
        )
    }
