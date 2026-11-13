package com.github.damontecres.wholphin.util

import com.github.damontecres.wholphin.data.model.RemoteModulesConfig
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.BackdropStyle
import com.github.damontecres.wholphin.preferences.InterfacePreferences
import com.github.damontecres.wholphin.preferences.ThemeSongVolume
import com.github.damontecres.wholphin.services.isCollectionTypeAllowed
import com.github.damontecres.wholphin.services.isDiscoverAllowed
import com.github.damontecres.wholphin.services.shouldPlayThemeSong
import com.github.damontecres.wholphin.ui.nav.shouldDrawBackdropImage
import com.github.damontecres.wholphin.ui.util.InterfaceCustomization
import org.jellyfin.sdk.model.api.CollectionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteUiAndModulesTest {
    @Test
    fun `supportedCollectionTypes excludes live tv music and photos`() {
        assertTrue(CollectionType.MOVIES in supportedCollectionTypes)
        assertTrue(CollectionType.TVSHOWS in supportedCollectionTypes)
        assertTrue(CollectionType.BOXSETS in supportedCollectionTypes)
        assertFalse(CollectionType.LIVETV in supportedCollectionTypes)
        assertFalse(CollectionType.MUSIC in supportedCollectionTypes)
        assertFalse(CollectionType.MUSICVIDEOS in supportedCollectionTypes)
        assertFalse(CollectionType.PHOTOS in supportedCollectionTypes)
    }

    @Test
    fun `module flags allow movies tv and discover while pruning disabled modules`() {
        val liteModules = RemoteModulesConfig(liveTv = false, music = false, photos = false, discover = true)

        assertTrue(isCollectionTypeAllowed(CollectionType.MOVIES, liteModules))
        assertTrue(isCollectionTypeAllowed(CollectionType.TVSHOWS, liteModules))
        assertTrue(isCollectionTypeAllowed(CollectionType.BOXSETS, liteModules))
        assertFalse(isCollectionTypeAllowed(CollectionType.LIVETV, liteModules))
        assertFalse(isCollectionTypeAllowed(CollectionType.MUSIC, liteModules))
        assertFalse(isCollectionTypeAllowed(CollectionType.MUSICVIDEOS, liteModules))
        assertFalse(isCollectionTypeAllowed(CollectionType.PHOTOS, liteModules))
        assertTrue(isDiscoverAllowed(discoverActiveInServer = true, modules = liteModules))
        assertFalse(isDiscoverAllowed(discoverActiveInServer = false, modules = liteModules))
    }

    @Test
    fun `shouldPlayThemeSong respects music module and volume preference`() {
        assertFalse(shouldPlayThemeSong(modulesMusic = false, volume = ThemeSongVolume.HIGH))
        assertFalse(shouldPlayThemeSong(modulesMusic = true, volume = ThemeSongVolume.DISABLED))
        assertTrue(shouldPlayThemeSong(modulesMusic = true, volume = ThemeSongVolume.MEDIUM))
    }

    @Test
    fun `shouldDrawBackdropImage skips rendering when style is none or url blank`() {
        assertFalse(shouldDrawBackdropImage(BackdropStyle.BACKDROP_NONE, "https://img", enableAnimations = true))
        assertFalse(shouldDrawBackdropImage(BackdropStyle.BACKDROP_IMAGE_ONLY, "", enableAnimations = false))
        assertTrue(shouldDrawBackdropImage(BackdropStyle.BACKDROP_IMAGE_ONLY, "https://img", enableAnimations = false))
        assertFalse(shouldDrawBackdropImage(BackdropStyle.BACKDROP_DYNAMIC_COLOR, "https://img", enableAnimations = false))
        assertTrue(shouldDrawBackdropImage(BackdropStyle.BACKDROP_DYNAMIC_COLOR, "https://img", enableAnimations = true))
    }

    @Test
    fun `InterfaceCustomization defaults enableAnimations to false and mirrors proto`() {
        val prefs =
            AppPreferences
                .newBuilder()
                .setInterfacePreferences(
                    InterfacePreferences
                        .newBuilder()
                        .setEnableAnimations(false)
                        .build(),
                ).build()
        assertFalse(InterfaceCustomization(prefs).enableAnimations)

        val animated =
            prefs
                .toBuilder()
                .setInterfacePreferences(
                    prefs.interfacePreferences
                        .toBuilder()
                        .setEnableAnimations(true)
                        .build(),
                ).build()
        assertTrue(InterfaceCustomization(animated).enableAnimations)
    }
}
