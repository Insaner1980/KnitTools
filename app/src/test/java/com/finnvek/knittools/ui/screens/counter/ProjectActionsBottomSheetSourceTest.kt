package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectActionsBottomSheetSourceTest {
    @Test
    fun `manage sheet separates project actions from counter tools`() {
        val source = ProjectSourceFiles.read(PROJECT_ACTIONS_BOTTOM_SHEET)

        val thisProjectSection = source.indexOf("R.string.project_actions_section_this_project")
        val remindersAction = source.indexOf("R.string.reminders")
        val countersAction = source.indexOf("R.string.counters")
        val counterToolsSection = source.indexOf("R.string.project_actions_section_counter_tools")
        val addCounterAction = source.indexOf("R.string.add_counter")
        val stitchesAction = source.indexOf("R.string.stitches_per_row")
        val trackStitchesAction = source.indexOf("R.string.track_stitches")
        val projectActionsSection = source.indexOf("R.string.project_actions_section_project_actions")
        val historyAction = source.indexOf("R.string.session_history_title")

        assertTrue(thisProjectSection >= 0)
        assertTrue(thisProjectSection < remindersAction)
        assertTrue(remindersAction < countersAction)
        assertTrue(countersAction < counterToolsSection)
        assertTrue(counterToolsSection < addCounterAction)
        assertTrue(addCounterAction < stitchesAction)
        assertTrue(stitchesAction < trackStitchesAction)
        assertTrue(trackStitchesAction < projectActionsSection)
        assertTrue(projectActionsSection < historyAction)
        assertFalse(source.contains("R.string.counter_undo_last_change"))
        assertFalse(source.contains("Icons.AutoMirrored.Outlined.Undo"))
        assertFalse(source.contains("onUndo: () -> Unit"))
    }

    @Test
    fun `manage sheet section labels are localized`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val text = ProjectSourceFiles.read(file)

            assertTrue(
                "$file is missing project_actions_section_counter_tools",
                text.contains("""name="project_actions_section_counter_tools""""),
            )
        }
    }

    @Test
    fun `track stitches switch stays reachable so missing count can open setup dialog`() {
        val source = ProjectSourceFiles.read(PROJECT_ACTIONS_BOTTOM_SHEET)
        val trackStitchesSwitch =
            source
                .substringAfter("label = stringResource(R.string.track_stitches)")
                .substringBefore("SectionDivider()")

        assertTrue(trackStitchesSwitch.contains("onCheckedChange = callbacks.onToggleStitchTracking"))
        assertFalse(trackStitchesSwitch.contains("enabled = (state.stitchCount ?: 0) > 0"))
    }

    @Test
    fun `completed project replaces active mutations with reactivation`() {
        val source = ProjectSourceFiles.read(PROJECT_ACTIONS_BOTTOM_SHEET)

        assertTrue(source.contains("val isCompleted: Boolean"))
        assertTrue(source.contains("if (!state.isCompleted)"))
        assertTrue(source.contains("if (state.isCompleted)"))
        assertTrue(source.contains("R.string.reactivate_project"))
        assertTrue(source.contains("onClick = callbacks.onReactivateProject"))
    }

    @Test
    fun `reactivation action is localized`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val text = ProjectSourceFiles.read(file)

            assertTrue(
                "$file is missing reactivate_project",
                text.contains("""name="reactivate_project"""),
            )
        }
    }

    @Test
    fun `project action overlays retain and validate their invoking project`() {
        val source = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertTrue(source.contains("var projectActionTargetId by rememberSaveable"))
        assertTrue(source.contains("projectActionTargetId = projectId"))
        assertTrue(source.contains("projectId = projectActionTargetId"))
        assertTrue(source.contains("projectActionTargetId == state.projectId"))
        assertTrue(source.contains("dependencies.projectId == viewModel.uiState.value.projectId"))
    }

    private companion object {
        private const val PROJECT_ACTIONS_BOTTOM_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt"
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
    }
}
