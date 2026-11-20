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
        assertEquals(listOf(typoItem), SearchRelevance.filterAndSort(listOf(unrelated, typoItem), "rispostat"))

        val fallback = SearchRelevance.filterAndSort(listOf(unrelated, typoItem), "rispotata")
        assertEquals(listOf(typoItem, unrelated), fallback)
    }

    @Test
    fun filterAndSortPutsTheMostPopularFirstWithinTheSameKindOfMatch() {
        fun rated(
            title: String,
            rating: Float,
        ): BaseItem =
            mockk(relaxed = true) {
                every { name } returns title
                every { type } returns BaseItemKind.MOVIE
                every { data.communityRating } returns rating
            }
        val obscure = rated("Dune Drifter", 4.1f)
        val popular = rated("Dune: Parte due", 8.2f)
        val wordMatch = rated("Children of Dune", 9.0f)
        // A few 9/10 votes and no metadata: below the well-known title despite the higher rating
        val fewVotes = rated("Dune Surfer", 9.0f)
        every { popular.data.overview } returns "Paul Atreides..."
        every { popular.data.genres } returns listOf("Fantascienza")
        every { obscure.data.overview } returns "..."
        every { obscure.data.genres } returns listOf("Azione")
        every { fewVotes.data.overview } returns null
        every { fewVotes.data.genres } returns null

        assertEquals(
            listOf(popular, obscure, fewVotes, wordMatch),
            SearchRelevance.filterAndSort(listOf(wordMatch, fewVotes, obscure, popular), "dune"),
        )
    }

    @Test
    fun defaultExcludedSearchTypesLeavesOnlyMoviesAndSeries() {
        val activeByDefault = allSearchableTypes.filterNot { it in DEFAULT_EXCLUDED_SEARCH_TYPES }

        assertEquals(listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES), activeByDefault)
    }

    @Test
    fun filterAndSortPushesMakingOfsAndDocumentariesToBottom() {
        fun item(
            title: String,
            originalTitle: String? = null,
            genres: List<String> = emptyList(),
            rating: Float = 7.0f,
            year: Int = 2012,
        ): BaseItem =
            mockk(relaxed = true) {
                every { name } returns title
                every { type } returns BaseItemKind.MOVIE
                every { data.originalTitle } returns originalTitle
                every { data.genres } returns genres
                every { data.communityRating } returns rating
                every { data.productionYear } returns year
            }

        val mainMovie = item("Hunger Games", originalTitle = "The Hunger Games", genres = listOf("Fantascienza"), rating = 7.2f, year = 2012)
        val sequel = item("Hunger Games: La ragazza di fuoco", originalTitle = "The Hunger Games: Catching Fire", genres = listOf("Azione"), rating = 7.5f, year = 2013)
        val makingOf = item("Pawns No More: The Making of The Hunger Games: Mockingjay Part 2", genres = listOf("Documentario"), rating = 9.0f, year = 2016)
        val parody = item("The Hunger Games (but better)", genres = emptyList(), rating = 9.5f, year = 2025)

        assertEquals(
            listOf(sequel, mainMovie, parody, makingOf),
            SearchRelevance.filterAndSort(listOf(parody, makingOf, sequel, mainMovie), "the hunger games"),
        )

        // Explicit making-of search keeps documentary order
        val explicit = SearchRelevance.filterAndSort(listOf(mainMovie, makingOf), "making of the hunger games")
        assertEquals(listOf(makingOf), explicit)
    }

    @Test
    fun pushSpecialsToEndMovesSpecialsToBottomPreservingOrder() {
        val items =
            listOf(
                "The World Is Watching: Making the Hunger Games",
                "Hunger Games",
                "Surviving the Game: Making The Hunger Games: Catching Fire",
                "Hunger Games: La ragazza di fuoco",
            )
        assertEquals(
            listOf(
                "Hunger Games",
                "Hunger Games: La ragazza di fuoco",
                "The World Is Watching: Making the Hunger Games",
                "Surviving the Game: Making The Hunger Games: Catching Fire",
            ),
            SearchRelevance.pushSpecialsToEnd(items, "hunger games") { it },
        )
        assertEquals(items, SearchRelevance.pushSpecialsToEnd(items, "making of") { it })
    }
}
