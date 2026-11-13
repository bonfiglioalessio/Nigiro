package com.github.damontecres.wholphin.util

import com.github.damontecres.wholphin.data.model.BaseItem
import org.jellyfin.sdk.model.api.BaseItemKind
import java.text.Normalizer
import java.util.Locale

object SearchRelevance {
    private val REGEX_PATTERN = Regex("^/(.+)/(i)?$")
    private val COMBINING_MARKS_REGEX = Regex("\\p{Mn}+")
    private val APOSTROPHE_REGEX = Regex("['’`ʼʻ]")
    private val NON_ALNUM_REGEX = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE_REGEX = Regex("\\s+")

    /**
     * Normalizes titles and queries for TV search by removing diacritics, stripping apostrophes
     * (e.g. "L’imperatrice" -> "limperatrice"), and collapsing punctuation to single spaces.
     */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return COMBINING_MARKS_REGEX
            .replace(decomposed, "")
            .lowercase(Locale.ROOT)
            .replace(APOSTROPHE_REGEX, "")
            .replace(NON_ALNUM_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    fun isTitleMatch(
        name: String?,
        query: String,
    ): Boolean {
        val rawName = name?.lowercase(Locale.getDefault()) ?: return false
        REGEX_PATTERN.find(query)?.let { match ->
            val pattern = match.groupValues[1]
            val caseInsensitive = match.groupValues[2] == "i"
            return regexScore(rawName, pattern, caseInsensitive) < 400
        }
        val normQ = normalize(query)
        if (normQ.isEmpty()) {
            return rawName.contains(query.lowercase(Locale.getDefault()))
        }
        return normalize(name).contains(normQ)
    }

    /**
     * Prioritizes title matches and drops unrelated fuzzy noise whenever at least one item
     * matches the query in its normalized title. Within the same kind of match (exact or prefix,
     * word or substring, fuzzy) the most popular titles come first: highest rating, then newest.
     */
    fun filterAndSort(
        items: List<BaseItem>,
        query: String,
    ): List<BaseItem> {
        if (query.isBlank() || items.isEmpty()) return items
        // The original title counts too, eg searching the English title of an Italian-titled item
        val titleMatches =
            items.filter { isTitleMatch(it.name, query) || isTitleMatch(it.data.originalTitle, query) }
        val candidates = if (titleMatches.isNotEmpty()) titleMatches else items
        // Score each item once rather than on every comparison
        return candidates
            .map { item ->
                val originalScore = item.data.originalTitle?.let { score(it, item.type, query) } ?: Int.MAX_VALUE
                item to minOf(score(item, query), originalScore)
            }.sortedWith(
                compareBy<Pair<BaseItem, Int>> { matchBand(it.second) }
                    .thenByDescending { it.first.data.communityRating ?: -1f }
                    .thenByDescending { it.first.data.productionYear ?: 0 }
                    .thenBy { it.second },
            ).map { it.first }
    }

    // 0: exact or prefix, 1: word start, substring or regex, 2: fuzzy
    private fun matchBand(score: Int): Int =
        when {
            score < 200 -> 0
            score < 350 -> 1
            else -> 2
        }

    fun score(
        item: BaseItem,
        query: String,
    ): Int = score(item.name, item.type, query)

    fun score(
        name: String?,
        type: BaseItemKind,
        query: String,
    ): Int {
        val rawName = name?.lowercase(Locale.getDefault()) ?: return Int.MAX_VALUE
        val rawQ = query.lowercase(Locale.getDefault())

        REGEX_PATTERN.find(query)?.let { match ->
            val pattern = match.groupValues[1]
            val caseInsensitive = match.groupValues[2] == "i"
            return regexScore(rawName, pattern, caseInsensitive) + typeBonus(type)
        }

        val normName = normalize(name)
        val normQ = normalize(query)
        val targetName = if (normQ.isNotEmpty()) normName else rawName
        val targetQuery = if (normQ.isNotEmpty()) normQ else rawQ

        return when {
            targetName == targetQuery -> 0
            targetName.startsWith(targetQuery) -> 100
            targetName.contains(" $targetQuery") -> 200
            targetName.contains(targetQuery) -> 300
            else -> fuzzyScore(targetName, targetQuery)
        } + typeBonus(type)
    }

    private fun regexScore(
        name: String,
        pattern: String,
        caseInsensitive: Boolean,
    ): Int =
        try {
            val options = if (caseInsensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
            if (Regex(pattern, options).containsMatchIn(name)) 250 else 400
        } catch (e: Exception) {
            400
        }

    private fun fuzzyScore(
        name: String,
        query: String,
    ): Int {
        val distance = levenshteinDistance(name, query)
        val maxLen = maxOf(name.length, query.length)
        val similarity = 1.0 - (distance.toDouble() / maxLen)

        return when {
            similarity >= 0.8 -> 350
            similarity >= 0.6 -> 375
            else -> 400
        }
    }

    private fun levenshteinDistance(
        s1: String,
        s2: String,
    ): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] =
                    if (s1[i - 1] == s2[j - 1]) {
                        dp[i - 1][j - 1]
                    } else {
                        1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                    }
            }
        }
        return dp[m][n]
    }

    private fun typeBonus(type: BaseItemKind): Int =
        when (type) {
            BaseItemKind.SERIES -> 0
            BaseItemKind.MOVIE -> 1
            BaseItemKind.BOX_SET -> 2
            BaseItemKind.EPISODE -> 10
            else -> 5
        }
}
