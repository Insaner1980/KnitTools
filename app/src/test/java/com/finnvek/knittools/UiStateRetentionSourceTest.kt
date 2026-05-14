package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateRetentionSourceTest {
    @Test
    fun `journal entry sheet dismiss resets transient entry state`() {
        val editor = ProjectSourceFiles.read(NOTES_EDITOR_SCREEN)
        val sheet = ProjectSourceFiles.read(JOURNAL_ENTRY_BOTTOM_SHEET)
        val viewModel = ProjectSourceFiles.read(JOURNAL_ENTRY_VIEW_MODEL)

        assertTrue(editor.contains("var showJournalSheet by rememberSaveable"))
        assertTrue(editor.contains("val journalEntryViewModel: JournalEntryViewModel = hiltViewModel()"))
        assertTrue(editor.contains("LaunchedEffect(journalEntryState.pendingEntry)"))
        assertTrue(editor.contains("journalEntryViewModel.consumePendingEntry()"))
        assertTrue(viewModel.contains("val pendingEntry: JournalEvent.EntryReady? = null"))
        assertTrue(viewModel.contains("fun consumePendingEntry()"))
        assertTrue(viewModel.contains("fun dismissEntry()"))
        assertTrue(viewModel.contains("processingJob?.cancel()"))
        assertTrue(viewModel.contains("JournalEntryUiState(speechAvailable = it.speechAvailable)"))
        assertTrue(sheet.contains("viewModel.dismissEntry()"))
        assertFalse(sheet.contains("viewModel.events.collect"))
    }

    @Test
    fun `counter summary and project overlays are cleared when owning project changes`() {
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)

        assertTrue(screen.contains("var previousOverlayProjectId by rememberSaveable"))
        assertTrue(screen.contains("hideProjectScopedOverlays()"))
        assertTrue(screen.contains("LaunchedEffect(state.projectId)"))
        assertTrue(viewModel.contains("private var summaryJob: Job? = null"))
        assertTrue(viewModel.contains("val requestProjectId = state.projectId ?: return"))
        assertTrue(viewModel.contains("if (_uiState.value.projectId != requestProjectId) return@launch"))
        assertTrue(viewModel.contains("summaryJob?.cancel()"))
        assertTrue(viewModel.contains("isSummaryLoading = false"))
    }

    @Test
    fun `project counter item dialog state is keyed by counter identity`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val counterComponents = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)
        val counterDao = ProjectSourceFiles.read(PROJECT_COUNTER_DAO)

        assertTrue(counterScreen.contains("key(counter.id)"))
        assertTrue(counterComponents.contains("rememberSaveable(counter.id)"))
        assertTrue(counterDao.contains("ORDER BY sortOrder ASC, id ASC"))
    }

    @Test
    fun `voice counter commands require unique counter name matches`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)

        assertTrue(viewModel.contains("findUniqueProjectCounterByName(name)"))
        assertFalse(viewModel.contains("firstOrNull { it.name.equals(name, ignoreCase = true) }"))
    }

    @Test
    fun `target row dialog preserves in-progress numeric input`() {
        val dialog = ProjectSourceFiles.read(TARGET_ROWS_DIALOG)

        assertTrue(dialog.contains("import androidx.compose.runtime.saveable.rememberSaveable"))
        assertTrue(dialog.contains("var text by rememberSaveable"))
    }

    @Test
    fun `photo gallery stores selected photo ids and rename draft as saveable state`() {
        val screen = ProjectSourceFiles.read(PHOTO_GALLERY_SCREEN)

        assertTrue(screen.contains("var renamingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }"))
        assertTrue(screen.contains("var viewingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }"))
        assertTrue(screen.contains("val renamingPhoto = remember(renamingPhotoId, photos)"))
        assertTrue(screen.contains("val viewingPhoto = remember(viewingPhotoId, photos)"))
        assertTrue(screen.contains("var text by rememberSaveable(currentNote)"))
        assertFalse(screen.contains("var renamingPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }"))
        assertFalse(screen.contains("var viewingPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }"))
    }

    @Test
    fun `all photos viewer stores selected photo id instead of object state`() {
        val screen = ProjectSourceFiles.read(ALL_PHOTOS_SCREEN)

        assertTrue(screen.contains("var viewingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }"))
        assertTrue(screen.contains("val viewingPhoto = remember(viewingPhotoId, state.photos)"))
        assertFalse(screen.contains("var viewingPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }"))
    }

    @Test
    fun `library multi select state is cleared when navigating away from routes`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(navGraph.contains("route = Screen.SavedPatterns.route"))
        assertTrue(navGraph.contains("clearSelection = libraryViewModel::exitPatternSelectMode"))
        assertTrue(navGraph.contains("route = Screen.MyYarn.route"))
        assertTrue(navGraph.contains("clearSelection = libraryViewModel::exitYarnSelectMode"))
        assertTrue(navGraph.contains("route = Screen.AllPhotos.route"))
        assertTrue(navGraph.contains("clearSelection = libraryViewModel::exitPhotoSelectMode"))
        assertTrue(navGraph.contains("NavController.OnDestinationChangedListener"))
    }

    @Test
    fun `snackbar triggers are consumed before suspending display calls`() {
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)
        val yarnEstimator = ProjectSourceFiles.read(YARN_ESTIMATOR_SCREEN)

        assertTrue(mainActivity.contains("var lastShownDownloadedUpdatePromptId by rememberSaveable"))
        assertTrue(mainActivity.contains("downloadedUpdatePromptId > lastShownDownloadedUpdatePromptId"))
        assertTrue(
            mainActivity.indexOf("lastShownDownloadedUpdatePromptId = downloadedUpdatePromptId") <
                mainActivity.indexOf("snackbarHostState.showSnackbar("),
        )

        assertFalse(yarnEstimator.contains("LaunchedEffect(scanError)"))
        assertTrue(yarnEstimator.contains(".map { it.scanError }"))
        assertTrue(
            yarnEstimator.indexOf("copy(scanError = null)") <
                yarnEstimator.indexOf("showSnackbar(scanError"),
        )
    }

    private companion object {
        private const val MAIN_ACTIVITY =
            "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        private const val NOTES_EDITOR_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt"
        private const val JOURNAL_ENTRY_BOTTOM_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/notes/JournalEntryBottomSheet.kt"
        private const val JOURNAL_ENTRY_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/notes/JournalEntryViewModel.kt"
        private const val TARGET_ROWS_DIALOG =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/TargetRowsDialog.kt"
        private const val PHOTO_GALLERY_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt"
        private const val ALL_PHOTOS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val YARN_ESTIMATOR_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/yarn/YarnEstimatorScreen.kt"
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        private const val PROJECT_COUNTER_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/ProjectCounterDao.kt"
    }
}
