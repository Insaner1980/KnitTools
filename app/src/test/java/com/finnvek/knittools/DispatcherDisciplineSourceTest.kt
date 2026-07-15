package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatcherDisciplineSourceTest {
    @Test
    fun `blocking storage work is dispatched away from main`() {
        val app = ProjectSourceFiles.read(APP)
        val activity = ProjectSourceFiles.read(MAIN_ACTIVITY)
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val repository = ProjectSourceFiles.read(SAVED_PATTERN_REPOSITORY)

        assertTrue(app.contains("applicationScope.launch(ioDispatcher)"))
        assertTrue(activity.contains("withContext(ioDispatcher)"))
        assertTrue(
            picker.contains(
                "withContext(AppDispatchers.IO) {\n" +
                    "                        patternStorage.createCaptureImageFile(context, pendingProjectId)\n" +
                    "                    }",
            ),
        )
        assertTrue(repository.contains("private suspend fun String.isAppOwnedMissingFile(): Boolean"))
        assertTrue(repository.contains("return withContext(ioDispatcher)"))
    }

    @Test
    fun `viewmodel cleanup uses the owned application scope`() {
        val dispatchers = ProjectSourceFiles.read(DISPATCHERS_MODULE)
        val counterViewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val notesViewModel = ProjectSourceFiles.read(NOTES_VIEW_MODEL)

        assertTrue(dispatchers.contains("annotation class ApplicationScope"))
        assertTrue(dispatchers.contains("fun provideApplicationScope(): CoroutineScope"))
        listOf(counterViewModel, notesViewModel).forEach { source ->
            assertTrue(source.contains("@param:ApplicationScope private val applicationScope: CoroutineScope"))
            assertTrue(source.contains("applicationScope.launch {"))
            assertFalse(source.contains("applicationScope.launch(ioDispatcher)"))
            assertFalse(source.contains("CoroutineScope(ioDispatcher + NonCancellable)"))
        }
    }

    private companion object {
        const val APP = "app/src/main/java/com/finnvek/knittools/App.kt"
        const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        const val PATTERN_PICKER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
        const val SAVED_PATTERN_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/SavedPatternRepository.kt"
        const val DISPATCHERS_MODULE = "app/src/main/java/com/finnvek/knittools/di/DispatchersModule.kt"
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        const val NOTES_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorViewModel.kt"
    }
}
