package com.github.damontecres.wholphin.ui.search

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.main.pickHomeLiveTvChannels
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class LiveTvSearchTest {
    private fun channel(
        id: UUID = UUID.randomUUID(),
        name: String,
        number: String? = null,
        favorite: Boolean = false,
    ): BaseItem =
        BaseItem(
            BaseItemDto(
                id = id,
                type = BaseItemKind.TV_CHANNEL,
                name = name,
                channelNumber = number,
                userData =
                    UserItemDataDto(
                        playbackPositionTicks = 0L,
                        playCount = 0,
                        isFavorite = favorite,
                        lastPlayedDate = null,
                        played = false,
                        key = id.toString(),
                        itemId = id,
                    ),
            ),
            useSeriesForPrimary = false,
        )

    @Test
    fun `filterLiveTvChannels matches name and channel number with favorites and prefix first`() {
        val ch1 = channel(name = "Sport ITA · Eurosport 1", number = "1002", favorite = false)
        val ch2 = channel(name = "Sky Cinema Uno", number = "301", favorite = true)
        val ch3 = channel(name = "Sky Sport F1", number = "207", favorite = false)

        val channels = listOf(ch1, ch2, ch3)

        assertEquals(emptyList<UUID>(), filterLiveTvChannels(channels, "s").map { it.id })
        assertEquals(listOf(ch2.id, ch3.id), filterLiveTvChannels(channels, "sky").map { it.id })
        assertEquals(listOf(ch1.id), filterLiveTvChannels(channels, "1002").map { it.id })
    }

    @Test
    fun `pickHomeLiveTvChannels prefers favorites and falls back to leading channels`() {
        val c1 = channel(name = "Rai 1", favorite = false)
        val c2 = channel(name = "Sky Sport Uno", favorite = true)
        val c3 = channel(name = "Canale 5", favorite = false)

        assertEquals(listOf(c2.id), pickHomeLiveTvChannels(listOf(c1, c2, c3), limit = 12).map { it.id })
        assertEquals(listOf(c1.id, c3.id), pickHomeLiveTvChannels(listOf(c1, c3), limit = 2).map { it.id })
    }
}
