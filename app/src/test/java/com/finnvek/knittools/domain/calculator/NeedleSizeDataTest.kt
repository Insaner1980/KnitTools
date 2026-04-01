package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedleSizeDataTest {
    @Test
    fun `sizes list is not empty`() {
        assertTrue(NeedleSizeData.sizes.isNotEmpty())
    }

    @Test
    fun `sizes are sorted by metric mm`() {
        val sorted = NeedleSizeData.sizes.sortedBy { it.metricMm }
        assertEquals(sorted, NeedleSizeData.sizes)
    }

    @Test
    fun `covers range from 2mm to 25mm`() {
        assertEquals(2.0, NeedleSizeData.sizes.first().metricMm, 0.01)
        assertEquals(25.0, NeedleSizeData.sizes.last().metricMm, 0.01)
    }

    @Test
    fun `search by metric size`() {
        val results = NeedleSizeData.search("4.0")
        assertEquals(1, results.size)
        assertEquals("6", results[0].us)
    }

    @Test
    fun `search by US size`() {
        val results = NeedleSizeData.search("8")
        assertTrue(results.any { it.metricMm == 5.0 })
    }

    @Test
    fun `search by UK size`() {
        val results = NeedleSizeData.search("14")
        assertTrue(results.any { it.metricMm == 2.0 })
    }

    @Test
    fun `empty search returns all sizes`() {
        val results = NeedleSizeData.search("")
        assertEquals(NeedleSizeData.sizes.size, results.size)
    }

    @Test
    fun `no match returns empty list`() {
        val results = NeedleSizeData.search("999")
        assertTrue(results.isEmpty())
    }
}
