package com.finnvek.knittools.ui.screens.sizecharts

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeChartScreenSourceTest {
    @Test
    fun `size chart categories use the shared segmented toggle instead of a dropdown`() {
        val screen =
            ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/screens/sizecharts/SizeChartScreen.kt")

        assertTrue(screen.contains("SegmentedToggle("))
        assertTrue(screen.contains("selectedIndex = categories.indexOf(selectedCategory)"))
        assertFalse(screen.contains("ExposedDropdownMenuBox("))
    }
}
