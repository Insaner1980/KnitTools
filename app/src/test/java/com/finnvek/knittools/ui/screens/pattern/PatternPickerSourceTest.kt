package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternPickerSourceTest {
    @Test
    fun `pattern picker orders saved patterns import images photo and continue actions`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val strings = ProjectSourceFiles.read(STRINGS)

        val savedIndex = picker.indexOf("R.string.pattern_picker_saved_patterns")
        val importIndex = picker.indexOf("R.string.pattern_picker_import_pdf")
        val imagesIndex = picker.indexOf("R.string.pattern_picker_choose_images")
        val photoIndex = picker.indexOf("R.string.pattern_picker_camera_scan")
        val continueIndex = picker.indexOf("R.string.pattern_picker_continue_without_pattern")

        assertTrue(savedIndex >= 0)
        assertTrue(importIndex > savedIndex)
        assertTrue(imagesIndex > importIndex)
        assertTrue(photoIndex > imagesIndex)
        assertTrue(continueIndex > photoIndex)
        assertTrue(strings.contains("""<string name="pattern_picker_import_pdf">"""))
        assertTrue(strings.contains("""<string name="pattern_picker_choose_images">"""))
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

        assertTrue(counterScreen.contains("if (pattern.isWebPatternCompatible)"))
        assertTrue(counterScreen.contains("viewModel.attachSavedPatternMetadata(pattern.id)"))
        assertTrue(counterScreen.contains("viewModel.attachSavedPattern(pattern)"))
        assertFalse(counterScreen.contains("pattern.localPdfUri?.let"))
        assertTrue(viewModel.contains("fun attachSavedPattern(pattern: SavedPattern)"))
        assertTrue(viewModel.contains("repository.attachSavedPattern("))
        assertTrue(repository.contains("suspend fun attachSavedPattern("))
    }

    @Test
    fun `pattern attachment UI waits for repository state instead of optimistic URI`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)

        assertFalse(viewModel.contains("updateAttachedPatternState("))
        assertFalse(viewModel.contains("updateSavedPatternAttachmentState("))
        assertTrue(viewModel.contains("attachment.copiedUri"))
        assertTrue(viewModel.contains("repository.isPatternDocumentAttached(projectId, attachment.internalUri)"))
        assertTrue(viewModel.contains("AppFileStorage.deleteIfAppOwned(context, failedUri)"))
    }

    @Test
    fun `gallery import uses Photo Picker without changing Room or storage permissions`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val database = ProjectSourceFiles.read(DATABASE)
        val manifest = ProjectSourceFiles.read(MANIFEST)

        assertTrue(picker.contains("ActivityResultContracts.PickMultipleVisualMedia"))
        assertTrue(picker.contains("ActivityResultContracts.PickVisualMedia.ImageOnly"))
        assertTrue(database.contains("version = 24"))
        assertFalse(manifest.contains("READ_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("MANAGE_EXTERNAL_STORAGE"))
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
        private const val DATABASE =
            "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        private const val MANIFEST = "app/src/main/AndroidManifest.xml"
    }
}
