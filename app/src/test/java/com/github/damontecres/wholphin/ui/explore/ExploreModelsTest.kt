package com.github.damontecres.wholphin.ui.explore

import com.github.damontecres.wholphin.ui.main.HomeMediaFilter
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ExploreModelsTest {
    private val azione = UUID.randomUUID()
    private val action = UUID.randomUUID()
    private val actionAdventure = UUID.randomUUID()
    private val commedia = UUID.randomUUID()

    private val idsByName =
        mapOf(
            "azione" to azione,
            "action" to action,
            "action & adventure" to actionAdventure,
            "commedia" to commedia,
        )

    private fun genre(key: String) = ExploreGenres.first { it.key == key }

    @Test
    fun `a folder gathers Italian, English and combined TV genres`() {
        assertEquals(listOf(azione, action, actionAdventure), genre("action").serverIds(idsByName))
    }

    @Test
    fun `a combined TV genre feeds both folders`() {
        assertEquals(listOf(actionAdventure), genre("adventure").serverIds(idsByName))
    }

    @Test
    fun `a folder with no genre on the server is empty`() {
        assertTrue(genre("western").serverIds(idsByName).isEmpty())
    }

    @Test
    fun `server names match whatever their case and spacing`() {
        assertEquals("action & adventure", " Action & Adventure ".genreKey())
    }

    @Test
    fun `folder keys are unique`() {
        assertEquals(ExploreGenres.size, ExploreGenres.map { it.key }.toSet().size)
    }

    @Test
    fun `the filter picks the item kinds`() {
        assertEquals(listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES), HomeMediaFilter.ALL.itemKinds())
        assertEquals(listOf(BaseItemKind.MOVIE), HomeMediaFilter.MOVIE.itemKinds())
        assertEquals(listOf(BaseItemKind.SERIES), HomeMediaFilter.SERIES.itemKinds())
    }
}
