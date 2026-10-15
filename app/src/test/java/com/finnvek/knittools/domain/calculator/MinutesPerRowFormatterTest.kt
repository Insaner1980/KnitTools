package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class MinutesPerRowFormatterTest {
    @Test
    fun `pace uses row totals above the int limit without clamping`() {
        assertEquals(
            MinutesPerRowDisplay.Minutes(2),
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 360_000_000_000L, totalRows = 3_000_000_000L),
        )
    }

    @Test
    fun `no rows means no division at all`() {
        assertEquals(
            MinutesPerRowDisplay.Unavailable,
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 120L * 60L, totalRows = 0),
        )
    }

    @Test
    fun `result rounding below one reports the under one marker`() {
        assertEquals(
            MinutesPerRowDisplay.UnderOneMinute,
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 10L * 60L, totalRows = 40),
        )
        assertEquals(
            MinutesPerRowDisplay.UnderOneMinute,
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 0L * 60L, totalRows = 5),
        )
    }

    @Test
    fun `pace rounds to the nearest whole minute`() {
        assertEquals(
            MinutesPerRowDisplay.Minutes(9),
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 795L * 60L, totalRows = 85),
        )
        assertEquals(
            MinutesPerRowDisplay.Minutes(2),
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 5L * 60L, totalRows = 3),
        )
        assertEquals(
            MinutesPerRowDisplay.Minutes(1),
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 3L * 60L, totalRows = 4),
        )
    }

    @Test
    fun `exact seconds are rounded only after pace is calculated`() {
        assertEquals(
            MinutesPerRowDisplay.Minutes(1),
            MinutesPerRowFormatter.fromSeconds(totalSeconds = 61L, totalRows = 1),
        )
    }
}
