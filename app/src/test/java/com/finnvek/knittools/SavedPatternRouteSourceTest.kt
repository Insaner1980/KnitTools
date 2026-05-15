package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedPatternRouteSourceTest {
    @Test
    fun `saved pattern routing prefers local pattern uri before ravelry detail`() {
        val routeTarget = ProjectSourceFiles.read(SAVED_PATTERN_ROUTE_TARGET)
        val librarySavedPatterns = ProjectSourceFiles.read(SAVED_PATTERNS_SCREEN)
        val ravelrySearch = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)

        assertTrue(routeTarget.contains("patternUrl.isLocalPatternUri() || ravelryId <= 0"))
        assertTrue(routeTarget.contains("SavedPatternRouteTarget.LocalPattern(id)"))
        assertTrue(routeTarget.contains("SavedPatternRouteTarget.RavelryPattern(ravelryId)"))
        assertTrue(librarySavedPatterns.contains("pattern.routeTarget()"))
        assertTrue(ravelrySearch.contains("pattern.routeTarget()"))
        assertFalse(librarySavedPatterns.contains("else if (pattern.ravelryId > 0)"))
        assertFalse(ravelrySearch.contains("else if (pattern.ravelryId > 0)"))
    }

    @Test
    fun `local pattern uri detection has one shared helper`() {
        val helper = ProjectSourceFiles.read(PATTERN_URI)
        val picker = ProjectSourceFiles.read(PATTERN_PICKER_SHEET)
        val routeTarget = ProjectSourceFiles.read(SAVED_PATTERN_ROUTE_TARGET)

        assertTrue(helper.contains("fun String.isLocalPatternUri()"))
        assertTrue(picker.contains("patternUrl.isLocalPatternUri()"))
        assertTrue(routeTarget.contains("import com.finnvek.knittools.ui.screens.pattern.isLocalPatternUri"))
        assertFalse(picker.contains("private fun String.isLocalPatternUri()"))
    }

    private companion object {
        private const val SAVED_PATTERN_ROUTE_TARGET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternRouteTarget.kt"
        private const val SAVED_PATTERNS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt"
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val PATTERN_URI =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternUri.kt"
        private const val PATTERN_PICKER_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
    }
}
