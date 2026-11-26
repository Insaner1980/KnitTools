package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tallenna ohje -sheetin tallennuspolut eivät jätä tyhjää projektia eivätkä orpoja PDF-kopioita. */
class IncomingPdfSourceTest {
    @Test
    fun `new project and incoming pdf are created in one transaction`() {
        val viewModel = ProjectSourceFiles.read(VIEW_MODEL)
        val repository = ProjectSourceFiles.read(COUNTER_REPOSITORY)
        val createWithPdf =
            repository
                .substringAfter("suspend fun createProjectWithImportedPdf(")
                .substringBefore("private suspend fun insertProjectInCurrentTransaction(")

        assertTrue(viewModel.contains("counterRepository.createProjectWithImportedPdf("))
        assertFalse(viewModel.contains("counterRepository.createProject("))
        assertTrue(createWithPdf.contains("transactionRunner.run {"))
        assertTrue(createWithPdf.contains("insertProjectInCurrentTransaction("))
        assertTrue(createWithPdf.contains("addImportedPdfInCurrentTransaction("))
        assertTrue(createWithPdf.contains("check(added is ProjectDocumentMutationResult.Added)"))
    }

    @Test
    fun `labels fit the project document limit before attachment`() {
        val viewModel = ProjectSourceFiles.read(VIEW_MODEL)
        val sheet = ProjectSourceFiles.read(SHEET)

        assertTrue(viewModel.contains("PatternDisplayNames.documentLabel(fileName)"))
        assertTrue(viewModel.contains(".takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)"))
        assertTrue(sheet.contains("name = it.takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)"))
    }

    @Test
    fun `discarded copies are cleaned up outside the view model lifetime`() {
        val viewModel = ProjectSourceFiles.read(VIEW_MODEL)

        assertTrue(viewModel.contains("applicationScope.launch {"))
        assertFalse(viewModel.contains("viewModelScope.launch { deleteUnusedCopy("))
        assertTrue(viewModel.contains("savedPatternRepository.deleteLocalPatternFileIfUnused(pdf.localUri)"))
        assertFalse(viewModel.contains("AppFileStorage.deleteIfAppOwned("))
    }

    @Test
    fun `incoming pdf saved state excludes external intent defaults`() {
        val activity = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/MainActivity.kt")
        val incomingPdfCreation =
            activity
                .substringAfter("private val incomingPdfViewModel:")
                .substringBefore("private val updateResultLauncher")

        assertTrue(incomingPdfCreation.contains("extrasProducer = {"))
        assertTrue(incomingPdfCreation.contains("MutableCreationExtras(defaultViewModelCreationExtras)"))
        assertTrue(incomingPdfCreation.contains("this[DEFAULT_ARGS_KEY] = Bundle()"))
    }

    @Test
    fun `incoming provider receipt has one bounded deadline aware transfer`() {
        val viewModel = ProjectSourceFiles.read(VIEW_MODEL)
        val storage = ProjectSourceFiles.read(PATTERN_DOCUMENT_STORAGE)

        assertTrue(viewModel.contains("incomingReceiveActive.compareAndSet(false, true)"))
        assertFalse(viewModel.contains("displayNameOrNull(uri)"))
        assertTrue(storage.contains("PATTERN_PDF_IMPORT_MAX_BYTES = 100L * 1024L * 1024L"))
        assertTrue(storage.contains("coerceAtMost(PATTERN_PDF_IMPORT_MAX_BYTES)"))
        assertTrue(storage.contains("ContentResolverBackupProviderIo(context)"))
        assertTrue(storage.contains("pdfProviderIo(context).read("))
    }

    private companion object {
        const val VIEW_MODEL = "app/src/main/java/com/finnvek/knittools/ui/navigation/IncomingPdfViewModel.kt"
        const val COUNTER_REPOSITORY = "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        const val PATTERN_DOCUMENT_STORAGE =
            "app/src/main/java/com/finnvek/knittools/data/storage/PatternDocumentStorage.kt"
        const val SHEET = "app/src/main/java/com/finnvek/knittools/ui/screens/library/IncomingPdfSheet.kt"
    }
}
