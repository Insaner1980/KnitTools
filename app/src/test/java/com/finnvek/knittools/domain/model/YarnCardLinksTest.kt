package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class YarnCardLinksTest {
    @Test
    fun `parse ignores blanks and duplicate ids`() {
        assertEquals(
            listOf(1L, 2L, 3L),
            parseYarnCardIds(" 1, ,2,1,invalid,9223372036854775808,3,2 "),
        )
    }

    @Test
    fun `parse preserves signed long boundaries and stable first seen order`() {
        assertEquals(
            listOf(Long.MIN_VALUE, Long.MAX_VALUE, 1L, -1L, 0L),
            parseYarnCardIds(
                " ${Long.MIN_VALUE},${Long.MAX_VALUE},${Long.MIN_VALUE}," +
                    "9223372036854775808,-9223372036854775809,+1,-1,0,invalid ",
            ),
        )
    }

    @Test
    fun `character budget accepts exact boundary and rejects boundary plus one`() {
        val exactBoundary = "x".repeat(YARN_CARD_IDS_MAX_CHARACTERS)

        assertEquals(emptyList<Long>(), parseYarnCardIdsWithinLimits(exactBoundary))
        assertNull(parseYarnCardIdsWithinLimits("${exactBoundary}x"))
        assertEquals(
            emptyList<Long>(),
            parseYarnCardIds("1,$exactBoundary"),
        )
    }

    @Test
    fun `token budget counts duplicate invalid and blank segments`() {
        val duplicateBoundary = List(YARN_CARD_IDS_MAX_TOKENS) { "1" }.joinToString(",")
        assertEquals(listOf(1L), parseYarnCardIdsWithinLimits(duplicateBoundary))
        assertNull(parseYarnCardIdsWithinLimits("$duplicateBoundary,1"))

        val blankBoundary = ",".repeat(YARN_CARD_IDS_MAX_TOKENS - 1)
        assertEquals(emptyList<Long>(), parseYarnCardIdsWithinLimits(blankBoundary))
        assertNull(parseYarnCardIdsWithinLimits("$blankBoundary,"))

        val invalidBoundary = List(YARN_CARD_IDS_MAX_TOKENS) { "invalid" }.joinToString(",")
        assertEquals(emptyList<Long>(), parseYarnCardIdsWithinLimits(invalidBoundary))
        assertNull(parseYarnCardIdsWithinLimits("$invalidBoundary,invalid"))
    }

    @Test
    fun `general parser rejects an over budget value instead of accepting its prefix`() {
        val overBudget = "1," + ",".repeat(YARN_CARD_IDS_MAX_TOKENS)

        assertFalse(parseYarnCardIds(overBudget).contains(1L))
        assertEquals(emptyList<Long>(), parseYarnCardIds(overBudget))
    }

    @Test
    fun `format writes each id once`() {
        assertEquals("1,2,3", formatYarnCardIds(listOf(1L, 2L, 1L, 3L, 2L)))
    }
}
