package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RavelryImportConfirmationSourceTest {
    @Test
    fun `view model owns import confirmation states for id and url previews`() {
        val viewModel = ProjectSourceFiles.read(RAVELRY_VIEW_MODEL)
        val repository = ProjectSourceFiles.read(RAVELRY_REPOSITORY)
        val api = ProjectSourceFiles.read(RAVELRY_API_SERVICE)

        assertTrue(viewModel.contains("val importConfirmationState"))
        assertTrue(viewModel.contains("enum class RavelryImportStatus"))
        IMPORT_STATUSES.forEach { status ->
            assertTrue(viewModel.contains(status))
        }
        assertTrue(viewModel.contains("fun showImportConfirmationForPattern(patternId: Int)"))
        assertTrue(viewModel.contains("fun showImportConfirmationForUrl(url: String)"))
        assertTrue(viewModel.contains("fun saveImportPattern()"))
        assertTrue(viewModel.contains("fun dismissImportConfirmation()"))
        assertTrue(viewModel.contains("repository.findDuplicateFor(detail)"))
        assertTrue(viewModel.contains("RavelryHttpException ->"))
        assertTrue(viewModel.contains("statusCode == 412 -> RavelryImportStatus.NeedsSignIn"))

        assertTrue(repository.contains("suspend fun importPatternByUrl(url: String): PatternDetail"))
        assertTrue(repository.contains("suspend fun findDuplicateFor(detail: PatternDetail): SavedPattern?"))
        assertTrue(repository.contains("private fun PatternDetail.toSavedPattern()"))
        assertTrue(api.contains("suspend fun importPatternByUrl(url: String): PatternDetail"))
    }

    @Test
    fun `search screen shows one modal import confirmation sheet for search result and url imports`() {
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val sheetPath = ProjectSourceFiles.file(RAVELRY_IMPORT_SHEET)
        val strings = ProjectSourceFiles.read(BASE_STRINGS)

        assertTrue(searchScreen.contains("importUrl: String? = null"))
        assertTrue(searchScreen.contains("onSavedPatternDetail: (Long) -> Unit = {}"))
        assertTrue(searchScreen.contains("viewModel.importConfirmationState.collectAsStateWithLifecycle"))
        assertTrue(searchScreen.contains("importUrl?.let(viewModel::showImportConfirmationForUrl)"))
        assertTrue(searchScreen.contains("viewModel.showImportConfirmationForPattern(patternId)"))
        assertTrue(searchScreen.contains("RavelryImportConfirmationSheet("))
        assertTrue(searchScreen.contains("onSave = viewModel::saveImportPattern"))
        assertTrue(searchScreen.contains("onOpenSavedPattern = onSavedPatternDetail"))

        assertTrue(Files.exists(sheetPath))
        val sheet = ProjectSourceFiles.read(sheetPath)
        assertTrue(sheet.contains("ModalBottomSheet("))
        assertTrue(sheet.contains("RavelryImportStatus.Loading"))
        assertTrue(sheet.contains("RavelryImportStatus.Ready"))
        assertTrue(sheet.contains("RavelryImportStatus.AlreadySaved"))
        assertTrue(sheet.contains("RavelryImportStatus.NeedsSignIn"))
        assertTrue(sheet.contains("RavelryImportStatus.CouldNotImport"))
        assertTrue(sheet.contains("RavelryImportStatus.BackendUnavailable"))
        assertTrue(sheet.contains("R.string.save_pattern"))
        assertTrue(sheet.contains("onOpenSavedPattern(state.savedPatternId)"))

        IMPORT_STRINGS.forEach { stringName ->
            assertTrue(strings.contains("name=\"$stringName\""))
        }
    }

    private companion object {
        private val IMPORT_STATUSES =
            listOf(
                "Loading",
                "Ready",
                "AlreadySaved",
                "NeedsSignIn",
                "CouldNotImport",
                "BackendUnavailable",
            )
        private val IMPORT_STRINGS =
            listOf(
                "ravelry_import_loading",
                "ravelry_import_title",
                "ravelry_import_already_saved",
                "ravelry_import_needs_sign_in",
                "ravelry_import_could_not_import",
                "ravelry_import_backend_unavailable",
            )
        private const val RAVELRY_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryViewModel.kt"
        private const val RAVELRY_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/RavelryRepository.kt"
        private const val RAVELRY_API_SERVICE =
            "app/src/main/java/com/finnvek/knittools/data/remote/RavelryApiService.kt"
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val RAVELRY_IMPORT_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryImportConfirmationSheet.kt"
        private const val BASE_STRINGS =
            "app/src/main/res/values/strings.xml"
    }
}
