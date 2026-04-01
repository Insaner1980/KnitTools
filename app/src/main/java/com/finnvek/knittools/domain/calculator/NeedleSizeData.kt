package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.NeedleSize

object NeedleSizeData {

    val sizes: List<NeedleSize> = listOf(
        NeedleSize(2.0, "0", "14", "0"),
        NeedleSize(2.25, "1", "13", "—"),
        NeedleSize(2.5, "1.5", "12", "1"),
        NeedleSize(2.75, "2", "12", "2"),
        NeedleSize(3.0, "2.5", "11", "3"),
        NeedleSize(3.25, "3", "10", "4"),
        NeedleSize(3.5, "4", "—", "5"),
        NeedleSize(3.75, "5", "9", "—"),
        NeedleSize(4.0, "6", "8", "6"),
        NeedleSize(4.5, "7", "7", "7"),
        NeedleSize(5.0, "8", "6", "8"),
        NeedleSize(5.5, "9", "5", "9"),
        NeedleSize(6.0, "10", "4", "10"),
        NeedleSize(6.5, "10.5", "3", "—"),
        NeedleSize(7.0, "—", "2", "—"),
        NeedleSize(7.5, "—", "1", "—"),
        NeedleSize(8.0, "11", "0", "—"),
        NeedleSize(9.0, "13", "00", "—"),
        NeedleSize(10.0, "15", "000", "—"),
        NeedleSize(12.0, "17", "—", "—"),
        NeedleSize(15.0, "19", "—", "—"),
        NeedleSize(19.0, "35", "—", "—"),
        NeedleSize(25.0, "50", "—", "—"),
    )

    fun search(query: String): List<NeedleSize> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return sizes

        return sizes.filter { needle ->
            needle.metricMm.toString().startsWith(trimmed) ||
                needle.us.lowercase() == trimmed ||
                needle.ukCanadian.lowercase() == trimmed ||
                needle.japanese.lowercase() == trimmed
        }
    }
}
