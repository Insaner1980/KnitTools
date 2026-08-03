package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class MinutesPerRowFormatterTest {
    @Test
    fun `no rows means no division at all`() {
        assertEquals(
            MinutesPerRowDisplay.Unavailable,
            MinutesPerRowFormatter.fromTotals(totalMinutes = 120, totalRows = 0),
        )
    }

    @Test
    fun `result rounding below one reports the under one marker`() {
        assertEquals(
            MinutesPerRowDisplay.UnderOneMinute,
            MinutesPerRowFormatter.fromTotals(totalMinutes = 10, totalRows = 40),
        )
        assertEquals(
            MinutesPerRowDisplay.UnderOneMinute,
            MinutesPerRowFormatter.fromTotals(totalMinutes = 0, totalRows = 5),
        )
    }

    @Test
    fun `pace rounds to the nearest whole minute`() {
        assertEquals(
            MinutesPerRowDisplay.Minutes(9),
            MinutesPerRowFormatter.fromTotals(totalMinutes = 795, totalRows = 85),
        )
        assertEquals(
            MinutesPerRowDisplay.Minutes(2),
            MinutesPerRowFormatter.fromTotals(totalMinutes = 5, totalRows = 3),
        )
        assertEquals(
            MinutesPerRowDisplay.Minutes(1),
            MinutesPerRowFormatter.fromTotals(totalMinutes = 3, totalRows = 4),
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
