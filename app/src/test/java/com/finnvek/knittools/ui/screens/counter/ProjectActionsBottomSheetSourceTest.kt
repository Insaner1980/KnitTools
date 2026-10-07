package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectActionsBottomSheetSourceTest {
    @Test
    fun `sheet links to overview and keeps counter tools in order`() {
        val source = ProjectSourceFiles.read(PROJECT_ACTIONS_BOTTOM_SHEET)
        val tools = source.substringAfter("private fun CounterToolActions")
        val positions =
            listOf(
                "R.string.counters",
                "R.string.add_counter",
                "R.string.counter_history_title",
                "R.string.stitches_per_row",
                "R.string.track_stitches",
                "R.string.measurement_title",
                "R.string.reset_counter",
            ).map(tools::indexOf)
        assertTrue(positions.all { it >= 0 })
        assertTrue(positions.zipWithNext().all { (first, second) -> first < second })
        assertTrue(source.contains("onClick = callbacks.onOpenOverview"))
        assertTrue(source.contains("R.string.project_actions_section_work_session"))
        listOf(
            "project_documents_title",
            "reminders",
            "folder_move_to",
            "project_details",
            "rename_project",
            "complete_project",
            "delete_project",
            "session_history_title",
            "reactivate_project",
        ).forEach {
            assertFalse(it, source.contains("R.string.$it"))
        }
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
    fun `completed project management and reactivation live in overview`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectOverviewScreen.kt",
            )
        val completionAction =
            source
                .substringAfter(
                    "if (state.isCompleted) {",
                ).substringBefore("R.string.delete_project")
        assertTrue(completionAction.contains("R.string.reactivate_project to actions.onReactivate"))
        assertTrue(completionAction.contains("} else {"))
        assertTrue(completionAction.contains("R.string.complete_project to actions.onComplete"))
        // Valmistuneella projektilla jatka-kortissa ei ole laskuriin vievää toimintoa.
        assertTrue(source.contains("onOpenCounter = onOpenCounter.takeUnless { state.isCompleted }"))
        assertTrue(source.contains("R.string.complete_project to actions.onComplete"))
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
