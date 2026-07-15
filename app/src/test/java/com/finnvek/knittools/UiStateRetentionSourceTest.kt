package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateRetentionSourceTest {
    @Test
    fun `project actions notes opens notes bottom sheet before full editor`() {
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertTrue(screen.contains("showNotesSheet = true"))
        assertTrue(screen.contains("onExpandNotes = { state.projectId?.let(onNotesEditor) }"))
    }

    @Test
    fun `project scoped overlays are cleared when owning project changes`() {
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertTrue(screen.contains("var previousOverlayProjectId by rememberSaveable"))
        assertTrue(screen.contains("hideProjectScopedOverlays()"))
        assertTrue(screen.contains("LaunchedEffect(state.projectId)"))
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
    fun `foreground resume starts a fresh active session segment`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val onResume =
            viewModel.substring(
                viewModel.indexOf("override fun onResume"),
                viewModel.indexOf("override fun onPause"),
            )

        assertTrue(onResume.contains("val state = _uiState.value"))
        assertTrue(onResume.contains("restartSessionSegment(projectId, state.counter.count)"))
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

        assertTrue(mainActivity.contains("var lastShownDownloadedUpdatePromptId by rememberSaveable"))
        assertTrue(mainActivity.contains("downloadedUpdatePromptId > lastShownDownloadedUpdatePromptId"))
        assertTrue(mainActivity.contains("lastShownDownloadedUpdatePromptId = downloadedUpdatePromptId"))
        assertTrue(
            mainActivity.indexOf("lastShownDownloadedUpdatePromptId = downloadedUpdatePromptId") <
                mainActivity.indexOf("snackbarHostState.showSnackbar("),
        )
    }

    private companion object {
        private const val MAIN_ACTIVITY =
            "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        private const val NOTES_EDITOR_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt"
        private const val TARGET_ROWS_DIALOG =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/TargetRowsDialog.kt"
        private const val PHOTO_GALLERY_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt"
        private const val ALL_PHOTOS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
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
