package com.finnvek.knittools.ui.screens.ravelry

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PatternAvailability
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternAvailabilityPresentationTest {
    @Test
    fun `all availability states have distinct presentation labels`() {
        assertEquals(R.string.free, patternAvailabilityLabelRes(PatternAvailability.Free))
        assertEquals(R.string.paid, patternAvailabilityLabelRes(PatternAvailability.Paid))
        assertEquals(
            R.string.availability_unknown,
            patternAvailabilityLabelRes(PatternAvailability.Unknown),
        )
    }
}
