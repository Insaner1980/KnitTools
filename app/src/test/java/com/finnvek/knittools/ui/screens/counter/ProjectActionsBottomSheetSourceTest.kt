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

    private companion object {
        private const val PROJECT_ACTIONS_BOTTOM_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt"
    }
}
