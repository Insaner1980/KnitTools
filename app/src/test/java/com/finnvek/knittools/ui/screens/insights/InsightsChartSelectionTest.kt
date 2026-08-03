package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Ruudunlukijan valinnan siirto on tavallinen funktio, joten se testataan kutsumalla
 * sitä eikä lukemalla lähdekoodia. Jokainen tapaus väittää sekä paluuarvon että sen,
 * kutsuttiinko takaisinkutsua: reunassa alustan on saatava tieto ettei liike jatku,
 * eikä valinta saa silti hiljaa vaihtua. Askeleet tulevat samoista vakioista kuin
 * kaavion omat toiminnot, joten suunnan kääntyminen näkyisi testissä.
 */
class InsightsChartSelectionTest {
    @Test
    fun `empty chart cannot move and never selects anything`() {
        val result = move(emptyList(), selectedIndex = null, step = STEP_NEXT)

        assertFalse(result.moved)
        assertNull(result.selected)
    }

    @Test
    fun `without a selection both directions land on the first bucket`() {
        val forward = move(buckets(3), selectedIndex = null, step = STEP_NEXT)
        val backward = move(buckets(3), selectedIndex = null, step = STEP_PREVIOUS)

        assertTrue(forward.moved)
        assertEquals(0, forward.selected)
        assertTrue(backward.moved)
        assertEquals(0, backward.selected)
    }

    @Test
    fun `the first bucket reports failure instead of silently staying put`() {
        val result = move(buckets(3), selectedIndex = 0, step = STEP_PREVIOUS)

        assertFalse(result.moved)
        assertNull(result.selected)
    }

    @Test
    fun `the last bucket reports failure instead of silently staying put`() {
        val result = move(buckets(3), selectedIndex = 2, step = STEP_NEXT)

        assertFalse(result.moved)
        assertNull(result.selected)
    }

    @Test
    fun `a mid range move steps one bucket in both directions`() {
        val forward = move(buckets(5), selectedIndex = 2, step = STEP_NEXT)
        val backward = move(buckets(5), selectedIndex = 2, step = STEP_PREVIOUS)

        assertTrue(forward.moved)
        assertEquals(3, forward.selected)
        assertTrue(backward.moved)
        assertEquals(1, backward.selected)
    }

    /** Paluuarvo ja takaisinkutsun tulos yhdessä, jotta molemmat voi väittää. */
    private data class SelectionResult(
        val moved: Boolean,
        val selected: Int?,
    )

    private companion object {
        fun move(
            buckets: List<InsightsChartBucket>,
            selectedIndex: Int?,
            step: Int,
        ): SelectionResult {
            var selected: Int? = null
            val moved = moveChartSelection(buckets, selectedIndex, step) { selected = it }
            return SelectionResult(moved = moved, selected = selected)
        }

        fun buckets(count: Int): List<InsightsChartBucket> =
            (0 until count).map { index ->
                InsightsChartBucket(
                    bucketStart = LocalDate.of(2026, 7, 1).plusDays(index.toLong()),
                    totalMinutes = 10 * (index + 1),
                    totalRows = index + 1,
                )
            }
    }
}
