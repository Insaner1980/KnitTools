package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SavedPatternRouteSourceTest {
    @Test
    fun `saved pattern lists open one detail route for every saved pattern`() {
        val librarySavedPatterns = ProjectSourceFiles.read(SAVED_PATTERNS_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(librarySavedPatterns.contains("val onPatternClick: (Long) -> Unit"))
        assertTrue(librarySavedPatterns.contains("actions.onPatternClick(pattern.id)"))
        assertTrue(navGraph.contains("Screen.SavedPatternDetail(savedPatternId).route"))
        assertFalse(librarySavedPatterns.contains("pattern.routeTarget()"))
        assertFalse(Files.exists(ProjectSourceFiles.file(SAVED_PATTERN_ROUTE_TARGET)))
    }

    @Test
    fun `pattern picker does not limit project attachment to local pdf rows`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER_SHEET)

        assertTrue(picker.contains("savedPatterns = savedPatterns"))
        assertFalse(picker.contains("localPdfUri?.isLocalPatternUri() == true"))
        assertFalse(picker.contains("private fun String.isLocalPatternUri()"))
    }

    private companion object {
        private const val SAVED_PATTERN_ROUTE_TARGET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternRouteTarget.kt"
        private const val SAVED_PATTERNS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val PATTERN_PICKER_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
    }
}
