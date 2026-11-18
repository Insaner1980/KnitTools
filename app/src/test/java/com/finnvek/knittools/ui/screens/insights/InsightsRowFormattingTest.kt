package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsRowFormattingTest {
    @Test
    fun pluralQuantityPreservesSmallCountsAndLargeCountEndings() {
        val cases =
            listOf(
                0L to 0,
                1L to 1,
                2L to 2,
                2_147_483_647L to Int.MAX_VALUE,
                2_147_483_648L to 1_483_648,
                3_000_000_000L to 1_000_000,
                3_000_000_001L to 1_000_001,
                3_000_000_002L to 1_000_002,
            )

        cases.forEach { (rows, expected) ->
            assertEquals("Plural quantity for $rows", expected, insightsRowPluralQuantity(rows))
        }
    }
}
