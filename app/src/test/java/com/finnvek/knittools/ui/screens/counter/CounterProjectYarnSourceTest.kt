package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterProjectYarnSourceTest {
    @Test
    fun `counter state observes project-only yarn notes and exposes save events`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val sheet = ProjectSourceFiles.read(YARN_MANAGEMENT_SHEET)

        assertTrue(viewModel.contains("ProjectYarnNoteRepository"))
        assertTrue(viewModel.contains("val projectYarnNotes: List<ProjectYarnNote> = emptyList()"))
        assertTrue(viewModel.contains("observeProjectYarnNotes(project.id)"))
        assertTrue(viewModel.contains("fun saveProjectYarnNote("))
        assertTrue(viewModel.contains("fun deleteProjectYarnNote("))
        assertTrue(viewModel.contains("fun saveProjectYarnNoteToMyYarn("))
        assertTrue(screen.contains("projectYarnNotes = state.projectYarnNotes"))
        assertTrue(screen.contains("onSaveProjectYarnNote = viewModel::saveProjectYarnNote"))
        assertTrue(screen.contains("onDeleteProjectYarnNote = viewModel::deleteProjectYarnNote"))
        assertTrue(screen.contains("onSaveProjectYarnNoteToMyYarn = viewModel::saveProjectYarnNoteToMyYarn"))
        assertTrue(sheet.contains("R.string.choose_from_my_yarn"))
        assertTrue(sheet.contains("R.string.add_yarn_to_project"))
    }

    @Test
    fun `project info and quick action include project-only yarn notes`() {
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)

        assertTrue(contentCards.contains("projectYarnNoteNames"))
        assertTrue(contentCards.contains("linkedYarnNames + projectYarnNoteNames"))
        assertTrue(contentCards.contains("state.linkedYarns.isNotEmpty() || state.projectYarnNotes.isNotEmpty()"))
    }

    @Test
    fun `yarn management uses large option cards with supporting copy`() {
        val sheet = ProjectSourceFiles.read(YARN_MANAGEMENT_SHEET)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertTrue(sheet.contains("YarnOptionCard("))
        assertTrue(sheet.contains("R.string.choose_from_my_yarn_body"))
        assertTrue(sheet.contains("R.string.add_yarn_to_project_body"))
        assertTrue(strings.contains("""<string name="choose_from_my_yarn_body">"""))
        assertTrue(strings.contains("""<string name="add_yarn_to_project_body">"""))
    }

    private companion object {
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val YARN_MANAGEMENT_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/YarnManagementSheet.kt"
        private const val COUNTER_PROJECT_CONTENT_CARDS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
