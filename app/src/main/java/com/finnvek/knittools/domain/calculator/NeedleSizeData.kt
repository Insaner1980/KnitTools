package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NeedleSize

/**
 * Lähde: Craft Yarn Council hooks & needles -standardit:
 * https://www.craftyarncouncil.com/standards/hooks-and-needles
 *
 * CYC muistuttaa, että numerojärjestelmät vaihtelevat valmistajittain; millimetrikoko on
 * tarkin vertailuarvo. UK/Canadian values retain their existing regional convention.
 * Japanese numbered sizes are exact Clover diameters (0–15), not nearest equivalents:
 * https://clover.co.jp/recipe/takumikikakus.pdf
 */
object NeedleSizeData {
    val sizes: List<NeedleSize> =
        listOf(
            NeedleSize(1.5, "000", "—", "—"),
            NeedleSize(1.75, "00", "—", "—"),
            NeedleSize(2.0, "0", "14", "—"),
            NeedleSize(2.1, "—", "—", "0"),
            NeedleSize(2.25, "1", "13", "—"),
            NeedleSize(2.4, "—", "—", "1"),
            NeedleSize(2.5, "1.5", "—", "—"),
            NeedleSize(2.7, "—", "—", "2"),
            NeedleSize(2.75, "2", "12", "—"),
            NeedleSize(3.0, "2.5", "11", "3"),
            NeedleSize(3.25, "3", "10", "—"),
            NeedleSize(3.3, "—", "—", "4"),
            NeedleSize(3.5, "4", "—", "—"),
            NeedleSize(3.6, "—", "—", "5"),
            NeedleSize(3.75, "5", "9", "—"),
            NeedleSize(3.9, "—", "—", "6"),
            NeedleSize(4.0, "6", "8", "—"),
            NeedleSize(4.2, "—", "—", "7"),
            NeedleSize(4.5, "7", "7", "8"),
            NeedleSize(4.8, "—", "—", "9"),
            NeedleSize(5.0, "8", "6", "—"),
            NeedleSize(5.1, "—", "—", "10"),
            NeedleSize(5.4, "—", "—", "11"),
            NeedleSize(5.5, "9", "5", "—"),
            NeedleSize(5.7, "—", "—", "12"),
            NeedleSize(6.0, "10", "4", "13"),
            NeedleSize(6.3, "—", "—", "14"),
            NeedleSize(6.5, "10.5", "3", "—"),
            NeedleSize(6.6, "—", "—", "15"),
            NeedleSize(7.0, "—", "2", "—"),
            NeedleSize(7.5, "—", "1", "—"),
            NeedleSize(8.0, "11", "0", "—"),
            NeedleSize(9.0, "13", "00", "—"),
            NeedleSize(10.0, "15", "000", "—"),
            NeedleSize(12.5, "17", "—", "—"),
            NeedleSize(15.0, "19", "—", "—"),
            NeedleSize(19.0, "35", "—", "—"),
            NeedleSize(25.0, "50", "—", "—"),
        )

    fun search(query: String): List<NeedleSize> {
        val trimmed = query.trim().lowercase().replace(',', '.')
        if (trimmed.isEmpty()) return sizes

        return sizes.filter { needle ->
            needle.metricMm.toString().startsWith(trimmed) ||
                needle.us.lowercase() == trimmed ||
                needle.ukCanadian.lowercase() == trimmed ||
                needle.japanese.lowercase() == trimmed
        }
    }
}
