package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkSessionArchitectureSourceTest {
    @Test
    fun `active session stays Room owned without background execution infrastructure`() {
        val database = ProjectSourceFiles.read(DATABASE)
        val manifest = ProjectSourceFiles.read(MANIFEST)
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val dataStore = ProjectSourceFiles.read(PREFERENCES_MANAGER)
        val onCleared = viewModel.substring(viewModel.indexOf("override fun onCleared()"))

        assertTrue(database.contains("version = 22"))
        assertTrue(database.contains("MIGRATION_20_21"))
        assertTrue(database.contains("ActiveSessionEntity::class"))
        assertFalse(manifest.contains("POST_NOTIFICATIONS"))
        assertFalse(manifest.contains("foregroundServiceType"))
        assertFalse(manifest.contains("WAKE_LOCK"))
        assertFalse(onCleared.contains("stopSession("))
        assertFalse(onCleared.contains("insertSession("))
        assertFalse(viewModel.contains("WorkManager"))
        assertFalse(viewModel.contains("AlarmManager"))
        assertFalse(dataStore.contains("active_session"))
    }

    @Test
    fun `all supported locales define the complete work session vocabulary without middle dots`() {
        val files = ProjectSourceFiles.localizedStringFiles()
        val defaultFile = files.single { it.parent.fileName.toString() == "values" }
        val defaultKeys = workSessionKeys(ProjectSourceFiles.read(defaultFile))

        assertEquals(11, files.size)
        assertTrue(defaultKeys.contains("work_session_edit_duration"))
        assertTrue(defaultKeys.contains("work_session_active_description"))
        files.forEach { file ->
            val source = ProjectSourceFiles.read(file)
            assertEquals(file.toString(), defaultKeys, workSessionKeys(source))
            source.lineSequence().filter { "work_session_" in it }.forEach { line ->
                assertFalse(file.toString(), line.contains('\u00b7'))
            }
        }
    }

    @Test
    fun `completion and deletion navigate only after repository success`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val confirmationActions =
            screen.substring(
                screen.indexOf("onCompleteConfirm = {"),
                screen.indexOf("onRenameTextChange =", screen.indexOf("onCompleteConfirm = {")),
            )

        assertFalse(confirmationActions.contains("dependencies.onBack()"))
        assertTrue(screen.contains("viewModel.projectClosedEvents.collect"))
        assertTrue(viewModel.contains("_projectClosedEvents.tryEmit(Unit)"))
    }

    private fun workSessionKeys(source: String): Set<String> =
        Regex("name=\"(work_session_[^\"]+)\"")
            .findAll(source)
            .map { it.groupValues[1] }
            .toSet()

    private companion object {
        const val DATABASE =
            "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        const val MANIFEST = "app/src/main/AndroidManifest.xml"
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        const val PREFERENCES_MANAGER =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt"
    }
}
