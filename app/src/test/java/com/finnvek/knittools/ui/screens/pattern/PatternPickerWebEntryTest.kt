package com.finnvek.knittools.ui.screens.pattern

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternPickerWebEntryTest {
    @Test
    fun `web pattern creation is available only from the initial project picker`() {
        assertTrue(shouldShowWebPatternEntry(PatternPickerMode.INITIAL_PROJECT_PATTERN))
        assertFalse(shouldShowWebPatternEntry(PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT))
    }
}
