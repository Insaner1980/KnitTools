package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternPickerSourceTest {
    @Test
    fun `pattern picker orders saved patterns import photo and continue actions`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val strings = ProjectSourceFiles.read(STRINGS)

        val savedIndex = picker.indexOf("R.string.pattern_picker_saved_patterns")
        val importIndex = picker.indexOf("R.string.pattern_picker_import_pdf")
        val photoIndex = picker.indexOf("R.string.pattern_picker_camera_scan")
        val continueIndex = picker.indexOf("R.string.pattern_picker_continue_without_pattern")

        assertTrue(savedIndex >= 0)
        assertTrue(importIndex > savedIndex)
        assertTrue(photoIndex > importIndex)
        assertTrue(continueIndex > photoIndex)
        assertTrue(strings.contains("""<string name="pattern_picker_import_pdf">"""))
        assertTrue(strings.contains("""<string name="pattern_picker_continue_without_pattern">"""))
    }

    @Test
    fun `pattern picker lists all saved patterns and opens ravelry import`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertTrue(picker.contains("savedPatterns = savedPatterns"))
        assertFalse(picker.contains("filter { it.localPdfUri"))
        assertFalse(picker.contains("isLocalPatternUri()"))
        assertTrue(picker.contains("onImportFromRavelry: () -> Unit"))
        assertTrue(picker.contains("R.string.pattern_picker_import_from_ravelry"))
        assertTrue(counterScreen.contains("onImportFromRavelry = actions.onImportFromRavelry"))
        assertTrue(navGraph.contains("navController.navigateToTopLevel(TopLevelDestination.Tools)"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.Ravelry.route)"))
        assertTrue(strings.contains("""<string name="pattern_picker_import_from_ravelry">"""))
    }

    @Test
    fun `saved pattern selection attaches by saved pattern id instead of pdf uri`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val repository = ProjectSourceFiles.read(COUNTER_REPOSITORY)

        assertTrue(counterScreen.contains("onSavedPatternSelected = viewModel::attachSavedPattern"))
        assertFalse(counterScreen.contains("pattern.localPdfUri?.let"))
        assertTrue(viewModel.contains("fun attachSavedPattern(pattern: SavedPattern)"))
        assertTrue(viewModel.contains("repository.attachSavedPattern("))
        assertTrue(repository.contains("suspend fun attachSavedPattern("))
    }

    private companion object {
        private const val PATTERN_PICKER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
