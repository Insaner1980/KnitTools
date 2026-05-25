package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CounterWorkspaceSourceTest {
    @Test
    fun `counter content is implemented as one workspace lazy column`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspaceFile = ProjectSourceFiles.file(COUNTER_WORKSPACE_SECTIONS)

        assertTrue("CounterWorkspaceSections.kt is missing", Files.exists(workspaceFile))

        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        assertTrue(counterScreen.contains("CounterWorkspace("))
        assertFalse(counterScreen.contains("private fun CounterScreenContent("))
        assertTrue(workspace.contains("LazyColumn("))
        assertTrue(workspace.contains("contentPadding ="))
        assertTrue(workspace.contains("PaddingValues("))
        assertTrue(workspace.contains("scaffoldPadding.calculateTopPadding()"))
        assertTrue(workspace.contains("scaffoldPadding.calculateBottomPadding()"))
        assertTrue(workspace.contains("verticalArrangement = Arrangement.spacedBy"))
        assertTrue(workspace.contains("key = { counter -> counter.id }"))
    }

    @Test
    fun `top bar only keeps navigation and more while daily project actions move into quick actions`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val quickActionsFile = ProjectSourceFiles.file(COUNTER_QUICK_ACTIONS)

        assertTrue("CounterQuickActions.kt is missing", Files.exists(quickActionsFile))

        val quickActions = ProjectSourceFiles.read(COUNTER_QUICK_ACTIONS)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertFalse(counterScreen.contains("canUseProgressPhotos ="))
        assertFalse(counterScreen.contains("showPatternIcon ="))
        assertFalse(counterScreen.contains("Icons.Filled.CameraAlt"))
        assertFalse(counterScreen.contains("Icons.Filled.Description"))

        listOf(
            "quick_action_open_pattern",
            "quick_action_attach_pattern",
            "quick_action_notes",
            "quick_action_add_note",
            "quick_action_yarn",
            "quick_action_add_yarn",
            "quick_action_photos",
            "quick_action_add_photo",
        ).forEach { key ->
            assertTrue("Quick action string missing: $key", strings.contains("""<string name="$key">"""))
            assertTrue("Quick action source does not reference: $key", quickActions.contains("R.string.$key"))
        }
    }

    private companion object {
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val COUNTER_QUICK_ACTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterQuickActions.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
