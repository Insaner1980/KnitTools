package com.finnvek.knittools.domain.model

import java.util.LinkedHashSet

// Tuhat käsin hallittavaa lankakorttilinkkiä ylittää realistisen projektikoon mutta pysyy mobiilissa rajattuna.
const val YARN_CARD_IDS_MAX_TOKENS = 1_000

// Jokaiselle tokenille varataan 20 merkin signeerattu Long-arvo ja erotin.
const val YARN_CARD_IDS_MAX_CHARACTERS = YARN_CARD_IDS_MAX_TOKENS * 21 - 1

fun parseYarnCardIds(value: String): List<Long> = parseYarnCardIdsWithinLimits(value).orEmpty()

internal fun parseYarnCardIdsWithinLimits(value: String): List<Long>? {
    if (value.length > YARN_CARD_IDS_MAX_CHARACTERS) return null

    val ids = LinkedHashSet<Long>()
    var tokenStart = 0
    var tokenCount = 0
    for (index in 0..value.length) {
        if (index == value.length || value[index] == ',') {
            tokenCount++
            if (tokenCount > YARN_CARD_IDS_MAX_TOKENS) return null
            value
                .substring(tokenStart, index)
                .trim()
                .toLongOrNull()
                ?.let(ids::add)
            tokenStart = index + 1
        }
    }
    return ids.toList()
}

fun formatYarnCardIds(ids: Iterable<Long>): String = ids.distinct().joinToString(",")
