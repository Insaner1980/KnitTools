package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsDatePatternTest {
    @Test
    fun `day-first localized patterns allow the compact same-month range`() {
        assertTrue(datePatternPlacesDayBeforeMonth("d. MMM"))
        assertTrue(datePatternPlacesDayBeforeMonth("d 'de' MMM"))
    }

    @Test
    fun `month-first localized patterns keep the month on both endpoints`() {
        assertFalse(datePatternPlacesDayBeforeMonth("MMM d"))
        assertFalse(datePatternPlacesDayBeforeMonth("LLL d"))
    }
}
