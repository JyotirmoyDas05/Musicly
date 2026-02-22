package com.jyotirmoy.musicly.data.network.lyrics

import kotlin.math.max
import kotlin.math.min

object LyricsCleaner {
    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit|From|Movie).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit|From|Movie).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(From .*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[From .*?\]""", RegexOption.IGNORE_CASE),
    )

    // YT Music channel suffixes (pear-desktop inspired)
    private val artistSuffixPatterns = listOf(
        Regex("""\s*-\s*topic$""", RegexOption.IGNORE_CASE),   // "Adele - Topic" → "Adele"
        Regex("""\s*vevo$""", RegexOption.IGNORE_CASE),        // "AdeleVEVO" → "Adele"
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        // Strip YT-specific suffixes first
        for (pattern in artistSuffixPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        // Then split on separators, keep primary artist
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(Regex(separator, RegexOption.IGNORE_CASE), limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    /**
     * Split artist string into individual names (for fuzzy matching).
     */
    fun splitArtists(artist: String): List<String> {
        var cleaned = artist.trim()
        for (pattern in artistSuffixPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        val separatorRegex = Regex("""\s*[&,]\s*|\s+(?:and|feat\.?|ft\.?|featuring|with|x)\s+""", RegexOption.IGNORE_CASE)
        return cleaned.split(separatorRegex).map { it.trim() }.filter { it.isNotBlank() }
    }

    /**
     * Jaro-Winkler similarity (0.0 to 1.0). Matches pear-desktop's approach.
     */
    fun jaroWinklerSimilarity(s1: String, s2: String): Double {
        val a = s1.lowercase()
        val b = s2.lowercase()
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val matchDistance = max(a.length, b.length) / 2 - 1
        val aMatches = BooleanArray(a.length)
        val bMatches = BooleanArray(b.length)

        var matches = 0
        var transpositions = 0

        for (i in a.indices) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, b.length)
            for (j in start until end) {
                if (bMatches[j] || a[i] != b[j]) continue
                aMatches[i] = true
                bMatches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var k = 0
        for (i in a.indices) {
            if (!aMatches[i]) continue
            while (!bMatches[k]) k++
            if (a[i] != b[k]) transpositions++
            k++
        }

        val jaro = (matches.toDouble() / a.length +
                matches.toDouble() / b.length +
                (matches - transpositions / 2.0) / matches) / 3.0

        // Winkler bonus for common prefix (up to 4 chars)
        var prefix = 0
        for (i in 0 until min(4, min(a.length, b.length))) {
            if (a[i] == b[i]) prefix++ else break
        }

        return jaro + prefix * 0.1 * (1 - jaro)
    }

    /**
     * Check if query artist fuzzy-matches a result artist (pear-desktop style).
     * Splits both on separators and checks all permutations.
     */
    fun artistFuzzyMatch(queryArtist: String, resultArtist: String, threshold: Double = 0.85): Boolean {
        val queryParts = splitArtists(queryArtist)
        val resultParts = splitArtists(resultArtist)
        if (queryParts.isEmpty() || resultParts.isEmpty()) return false

        var bestScore = 0.0
        for (qp in queryParts) {
            for (rp in resultParts) {
                bestScore = max(bestScore, jaroWinklerSimilarity(qp, rp))
            }
        }
        return bestScore >= threshold
    }
}
