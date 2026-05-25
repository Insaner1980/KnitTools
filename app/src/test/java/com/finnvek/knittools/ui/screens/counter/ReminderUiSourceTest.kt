package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUiSourceTest {
    @Test
    fun `counter screen exposes row reminder alert list add edit and delete paths`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val reminderComponents = ProjectSourceFiles.read(REMINDER_COMPONENTS)
        val projectActions = ProjectSourceFiles.read(PROJECT_ACTIONS_BOTTOM_SHEET)

        assertTrue(workspace.contains("ReminderAlertCard("))
        assertTrue(counterScreen.contains("RemindersSheet("))
        assertTrue(counterScreen.contains("viewModel.updateReminder("))
        assertTrue(counterScreen.contains("viewModel.deleteReminder("))
        assertTrue(projectActions.contains("onOpenReminders"))
        assertTrue(projectActions.contains("R.string.reminders"))
        assertTrue(reminderComponents.contains("onEdit"))
        assertTrue(reminderComponents.contains("key = { it.id }"))
    }

    private companion object {
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val REMINDER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ReminderComponents.kt"
        private const val PROJECT_ACTIONS_BOTTOM_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt"
    }
}
