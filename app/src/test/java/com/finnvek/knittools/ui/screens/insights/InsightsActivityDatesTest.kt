package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class InsightsActivityDatesTest {
    @Test fun sparseDatesPreserveDuplicatesNegativeEpochsAndStreaksAcrossBlockEdges() {
        val days = listOf(-2L, -1L, 0L, 1L, 4094L, 4095L, 4096L, 4097L, 4098L, 10_000_000L)
        val dates = InsightsActivityDates()
        days.reversed().forEach { day ->
            dates.add(LocalDate.ofEpochDay(day))
            dates.add(LocalDate.ofEpochDay(day))
        }
        assertEquals(days.size, dates.size)
        assertEquals(days, dates.map { it.toEpochDay() })
        assertEquals(5, dates.bestStreak())
        assertEquals(5, currentStreak(dates, LocalDate.ofEpochDay(4098)))
        assertFalse(LocalDate.ofEpochDay(5000) in dates)
    }
}
