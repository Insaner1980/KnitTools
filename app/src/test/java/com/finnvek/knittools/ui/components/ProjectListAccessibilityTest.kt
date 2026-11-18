package com.finnvek.knittools.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectListAccessibilityTest {
    @Test
    fun `mildly enlarged font keeps the regular card layout`() {
        assertFalse(usesCompactProjectCardLayout(maxWidthDp = 360f, fontScale = 1f))
        assertFalse(usesCompactProjectCardLayout(maxWidthDp = 360f, fontScale = 1.15f))
        assertFalse(usesCompactProjectCardLayout(maxWidthDp = 360f, fontScale = 1.3f))
    }

    @Test
    fun `large font or narrow width uses the stacked card layout`() {
        assertTrue(usesCompactProjectCardLayout(maxWidthDp = 360f, fontScale = 1.5f))
        assertTrue(usesCompactProjectCardLayout(maxWidthDp = 300f, fontScale = 1f))
    }
}
