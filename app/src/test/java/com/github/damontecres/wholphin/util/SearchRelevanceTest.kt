package com.github.damontecres.wholphin.util

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.search.DEFAULT_EXCLUDED_SEARCH_TYPES
import com.github.damontecres.wholphin.ui.search.allSearchableTypes
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRelevanceTest {
    private fun fakeItem(
        title: String?,
        kind: BaseItemKind = BaseItemKind.MOVIE,
    ): BaseItem =
        mockk(relaxed = true) {
            every { name } returns title
            every { type } returns kind
        }

    @Test
    fun risposataMatchesTitleWithCurlyApostropheAndOutranksUnrelated() {
        val targetScore = SearchRelevance.score("L’imperatrice risposata", BaseItemKind.SERIES, "risposata")
        val unrelatedScore = SearchRelevance.score("Unrelated Movie", BaseItemKind.MOVIE, "risposata")

        assertTrue("Expected title match score < 400, got $targetScore", targetScore < 400)
        assertTrue("Expected unrelated score >= 400, got $unrelatedScore", unrelatedScore >= 400)
        assertTrue(targetScore < unrelatedScore)
    }

    @Test
    fun limperatriceMatchesCurlyApostropheTitle() {
        assertEquals("limperatrice risposata", SearchRelevance.normalize("L’imperatrice risposata"))
        val score = SearchRelevance.score("L’imperatrice risposata", BaseItemKind.SERIES, "limperatrice")

        assertTrue("Expected apostrophe-normalized match < 400, got $score", score < 400)
    }

    @Test
    fun cittaMatchesDiacriticsNormalizedTitle() {
        assertEquals("la citta incantata", SearchRelevance.normalize("La Città Incantata"))
        val score = SearchRelevance.score("La Città Incantata", BaseItemKind.MOVIE, "citta")

        assertTrue("Expected diacritics-normalized match < 400, got $score", score < 400)
    }

    @Test
    fun filterAndSortDropsUnrelatedNoiseWhenTitleMatchesExistAndPreservesTypoFallback() {
        val seriesMatch = fakeItem("L’imperatrice risposata", BaseItemKind.SERIES)
        val exactMovie = fakeItem("Risposata", BaseItemKind.MOVIE)
        val unrelated = fakeItem("Unrelated Movie", BaseItemKind.MOVIE)

        val filtered =
            SearchRelevance.filterAndSort(
                listOf(unrelated, seriesMatch, exactMovie),
                "risposata",
            )

        assertEquals(listOf(exactMovie, seriesMatch), filtered)

        val typoItem = fakeItem("Rispostata", BaseItemKind.MOVIE)
        val fallback = SearchRelevance.filterAndSort(listOf(unrelated, typoItem), "rispostat")
        assertEquals(listOf(typoItem, unrelated), fallback)
    }

    @Test
    fun defaultExcludedSearchTypesLeavesOnlyMoviesAndSeries() {
        val activeByDefault = allSearchableTypes.filterNot { it in DEFAULT_EXCLUDED_SEARCH_TYPES }

        assertEquals(listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES), activeByDefault)
    }
}
