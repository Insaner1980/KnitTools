package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelrySearchResultCardsSourceTest {
    @Test
    fun `pattern card exposes a minimum touch target action slot`() {
        val patternCard = ProjectSourceFiles.read(PATTERN_CARD)

        assertTrue(patternCard.contains("actionContent: (@Composable () -> Unit)? = null"))
        assertTrue(patternCard.contains("PatternCardActionSlot(actionContent = actionContent)"))
        assertTrue(patternCard.contains("defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)"))
    }

    @Test
    fun `search results show saved open action or save import action`() {
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val strings = ProjectSourceFiles.read(BASE_STRINGS)

        assertTrue(
            searchScreen.contains(
                "savedRavelryPatternIds = savedPatterns.mapNotNull { it.ravelryPatternId }.toSet()",
            ),
        )
        assertTrue(searchScreen.contains("internal enum class RavelrySearchResultCardAction"))
        assertTrue(searchScreen.contains("RavelrySearchResultCardAction.OpenSavedPattern"))
        assertTrue(searchScreen.contains("RavelrySearchResultCardAction.SavePattern"))
        assertTrue(searchScreen.contains("actionContent = {"))
        assertTrue(searchScreen.contains("R.string.pattern_saved"))
        assertTrue(searchScreen.contains("R.string.ravelry_open_saved_pattern"))
        assertTrue(searchScreen.contains("R.string.save_pattern"))
        assertTrue(searchScreen.contains("viewModel.showImportConfirmationForPattern(patternId)"))
        assertTrue(searchScreen.contains("onSave = { onImportPattern(pattern.id) }"))
        assertFalse(searchScreen.contains("viewModel.savePattern()"))
        assertTrue(
            strings.contains(
                "<string name=\"ravelry_open_saved_pattern\">Open</string>",
            ),
        )
    }

    @Test
    fun `ravelry cards render backend availability instead of boolean only price`() {
        val patternCard = ProjectSourceFiles.read(PATTERN_CARD)
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)
        val savedPatternsScreen = ProjectSourceFiles.read(SAVED_PATTERNS_SCREEN)
        val importSheet = ProjectSourceFiles.read(RAVELRY_IMPORT_SHEET)
        val strings = ProjectSourceFiles.read(BASE_STRINGS)

        assertTrue(patternCard.contains("val availability: PatternAvailability"))
        assertTrue(patternCard.contains("PatternAvailability.Unknown ->"))
        assertTrue(patternCard.contains("R.string.availability_unknown"))
        assertTrue(strings.contains("<string name=\"availability_unknown\">Availability unknown</string>"))
        assertTrue(searchScreen.contains("availability = pattern.availability"))
        assertTrue(
            detailScreen.contains(
                "PatternAvailabilityBadge(availability = pattern.availability)",
            ),
        )
        assertTrue(savedPatternsScreen.contains("availability = pattern.availability"))
        assertTrue(importSheet.contains("availability = pattern.availability"))
        assertFalse(searchScreen.contains("PatternAvailability.fromFree"))
        assertFalse(savedPatternsScreen.contains("PatternAvailability.fromFree"))
        assertFalse(searchScreen.contains("isFree = pattern.free"))
        assertFalse(importSheet.contains("isFree = pattern.free"))
    }

    private companion object {
        private const val PATTERN_CARD =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/PatternCard.kt"
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val RAVELRY_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt"
        private const val RAVELRY_IMPORT_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryImportConfirmationSheet.kt"
        private const val SAVED_PATTERNS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt"
        private const val BASE_STRINGS =
            "app/src/main/res/values/strings.xml"
    }
}
