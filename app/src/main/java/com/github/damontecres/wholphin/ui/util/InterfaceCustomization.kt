package com.github.damontecres.wholphin.ui.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.DisplayToggle
import java.util.EnumSet

/**
 * Represents various UI preferences made available via [LocalInterfaceCustomization]
 */
data class InterfaceCustomization(
    val enabledDisplayToggles: EnumSet<DisplayToggle>,
    val enableAnimations: Boolean = false,
) {
    constructor(prefs: AppPreferences) : this(
        enabledDisplayToggles =
            prefs.interfacePreferences.displayTogglesList.let {
                if (it.isEmpty()) {
                    EnumSet.noneOf(DisplayToggle::class.java)
                } else {
                    EnumSet.copyOf(it)
                }
            },
        enableAnimations = prefs.interfacePreferences.enableAnimations,
    )
}

val LocalInterfaceCustomization =
    staticCompositionLocalOf<InterfaceCustomization> {
        InterfaceCustomization(
            enabledDisplayToggles = EnumSet.allOf(DisplayToggle::class.java),
            enableAnimations = false,
        )
    }
