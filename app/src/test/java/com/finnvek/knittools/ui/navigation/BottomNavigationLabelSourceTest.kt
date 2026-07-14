package com.finnvek.knittools.ui.navigation

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationLabelSourceTest {
    @Test
    fun `shared label measurement uses the widest rendered font weight`() {
        val source = ProjectSourceFiles.read(BOTTOM_BAR)

        assertTrue(source.contains("fontWeight = FontWeight.SemiBold"))
        assertTrue(source.contains("style.copy(fontSize = candidate, fontWeight = FontWeight.SemiBold)"))
    }

    private companion object {
        private const val BOTTOM_BAR =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsBottomBar.kt"
    }
}
