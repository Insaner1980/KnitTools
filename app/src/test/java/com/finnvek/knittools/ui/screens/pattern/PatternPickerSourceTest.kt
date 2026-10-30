package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternPickerSourceTest {
    @Test
    fun `same counter entry project transition preserves separate image drafts`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val counter = ProjectSourceFiles.read(COUNTER_SCREEN)

        // Hiding the sheet does not clear its route's ViewModelStore. B must not render A's model.
        assertTrue(counter.contains("hideProjectScopedOverlays()"))
        assertTrue(counter.contains("showPatternPicker = false"))
        assertTrue(picker.contains("hiltViewModel(key = patternImageImportViewModelKey(projectId))"))
        assertTrue(picker.contains("key(projectId)"))
    }

    @Test
    fun `gallery registration captures the request and resumes its stable key without adopting a later token`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val registration =
            picker
                .substringAfter("DisposableEffect(registry, imageImportViewModel, requestId)")
                .substringBefore("val openDocumentLauncher")

        assertTrue(picker.contains("val requestId = imageImportState.pickerRequestId"))
        assertTrue(
            registration.contains("registry.register(\"pattern-images-\$requestId\", PatternImagePickerContract())"),
        )
        assertTrue(registration.contains("imageImportViewModel.onGalleryPickerResult(requestId, uris)"))
        assertTrue(
            registration.indexOf("markGalleryPickerLaunched(requestId)") < registration.indexOf("launcher.launch("),
        )
        assertTrue(registration.contains("onDispose { launcher?.unregister() }"))
        assertFalse(picker.contains("pendingGalleryRequestId"))
        assertTrue(picker.contains("imageImportViewModel.consumeCloseRequest(sessionId)"))
        val closeEffect = picker.substringAfter("LaunchedEffect(sessionId,").substringBefore("pendingProAction?.let")
        assertTrue(
            closeEffect.replace(Regex("\\s+"), " ").contains(
                "imageImportState.closeReady && imageImportViewModel.consumeCloseRequest(sessionId) && " +
                    "sessionId != completedBeforeOpening ) { onDismiss()",
            ),
        )
        assertTrue(picker.contains("imageImportViewModel.createPatternPdf(hasExistingPattern, sessionId)"))
        assertTrue(
            picker.contains("imageImportViewModel.acceptCameraCapture(acceptedOwner, imageUri, imageFile, session)"),
        )
    }

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
        // Tuo Ravelrysta avaa Ravelryn sovelluksen sisällä, jotta Download PDF tallentuu suoraan.
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.RavelryBrowser.route)"))
        assertTrue(strings.contains("""<string name="pattern_picker_import_from_ravelry">"""))
    }

    @Test
    fun `saved pattern selection attaches by saved pattern id instead of pdf uri`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val repository = ProjectSourceFiles.read(COUNTER_REPOSITORY)

        assertTrue(counterScreen.contains("} else if (pattern.isWebPatternCompatible && requestProjectId != null) {"))
        assertTrue(
            counterScreen.contains("viewModel.attachSavedPatternMetadata(pattern.id, projectId = requestProjectId)"),
        )
        // Myöhäinen korvausvaatimus ei avaa vahvistusta projektin vaihduttua.
        assertTrue(counterScreen.contains("if (requestProjectId == currentProjectId) {"))
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
        assertTrue(viewModel.contains("savedPatternRepository.deleteLocalPatternFileIfUnused(failedUri)"))
    }

    @Test
    fun `project import feedback opens the existing localized document sheet with reselection`() {
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val resultHandler =
            screen
                .substringAfter(
                    "onPatternImportResult = { error ->",
                ).substringBefore("onImportFromRavelry =")
        val addAction = screen.substringAfter("onAdd = {").substringBefore("onClearError =")

        assertTrue(
            screen.contains("attachProjectPatternWithFeedback(viewModel, uri, name, owner, onPatternImportResult)"),
        )
        assertTrue(resultHandler.contains("projectDocumentError = error"))
        assertTrue(resultHandler.contains("if (error != null)"))
        assertTrue(resultHandler.contains("showPatternPicker = false"))
        assertTrue(resultHandler.contains("showDocumentsSheet = true"))
        assertTrue(screen.contains("error = projectDocumentError"))
        assertTrue(addAction.contains("projectDocumentError = null"))
        assertTrue(addAction.contains("showPatternPicker = true"))
        assertTrue(viewer.contains("text = stringResource(error.messageRes())"))
        assertTrue(viewer.contains("ProjectDocumentError.MUTATION_FAILURE -> R.string.project_documents_error_generic"))
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            assertTrue(
                "$file must localize document failures",
                ProjectSourceFiles.read(file).contains("name=\"project_documents_error_generic\""),
            )
        }
    }

    @Test
    fun `cancelled PDF selection clears pending owner and cannot deliver or dismiss an import`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val callback =
            picker
                .substringAfter("rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())")
                .substringBefore("LaunchedEffect(pendingDocumentUriString")
        val delivery =
            picker
                .substringAfter(
                    "LaunchedEffect(pendingDocumentUriString",
                ).substringBefore("val cameraLauncher")

        assertTrue(callback.contains("pendingDocumentUriString = uri?.toString()"))
        assertTrue(callback.contains("if (uri == null) pendingDocumentProjectId = null"))
        assertFalse(callback.contains("onDocumentSelected("))
        assertFalse(callback.contains("onDismiss()"))
        assertTrue(delivery.contains("val uri = pendingDocumentUriString ?: return@LaunchedEffect"))
        assertTrue(delivery.indexOf("onDocumentSelected(uri, \"\", owner)") < delivery.indexOf("onDismiss()"))
    }

    @Test
    fun `gallery import uses shared picker contract without changing Room or storage permissions`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val pickerContract = ProjectSourceFiles.read(PATTERN_IMAGE_PICKER_CONTRACT)
        val database = ProjectSourceFiles.read(DATABASE)
        val manifest = ProjectSourceFiles.read(MANIFEST)

        assertTrue(picker.contains("PatternImagePickerContract()"))
        assertFalse(picker.contains("ActivityResultContracts.PickMultipleVisualMedia"))
        assertFalse(picker.contains("ActivityResultContracts.OpenMultipleDocuments"))
        assertTrue(picker.contains("ActivityResultContracts.PickVisualMedia.ImageOnly"))
        assertTrue(picker.contains("imageImportViewModel.onGalleryPickerResult(requestId, uris)"))
        assertTrue(pickerContract.contains("ActivityResultContracts.PickMultipleVisualMedia"))
        assertTrue(pickerContract.contains(".OpenMultipleDocuments()"))
        assertTrue(database.contains("version = KNITTOOLS_DATABASE_VERSION"))
        assertFalse(manifest.contains("READ_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("MANAGE_EXTERNAL_STORAGE"))
    }

    private companion object {
        private const val PATTERN_PICKER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
        private const val PATTERN_IMAGE_PICKER_CONTRACT =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternImagePickerContract.kt"
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val PATTERN_VIEWER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"
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
