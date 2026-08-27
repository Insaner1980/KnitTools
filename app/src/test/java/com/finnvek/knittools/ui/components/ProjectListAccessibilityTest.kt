package com.finnvek.knittools.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectListAccessibilityTest {
    @Test
    fun `first enlarged font step uses the stacked project layout`() {
        assertFalse(usesCompactProjectListItemLayout(maxWidthDp = 360f, fontScale = 1f))
        assertTrue(usesCompactProjectListItemLayout(maxWidthDp = 360f, fontScale = 1.15f))
    }
}
