package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
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

    @Test
    fun `reminder validation cannot save malformed raw number fields`() {
        listOf("+2", "-2", "1e3", "12.5", "2147483648").forEach { text ->
            assertFalse(canSaveReminder(row = text, interval = "4"))
            assertFalse(canSaveReminder(row = "12", interval = text))
        }
        assertTrue(canSaveReminder(row = " 12 ", interval = "4"))
    }

    private fun canSaveReminder(
        row: String,
        interval: String,
    ): Boolean {
        val formClass = Class.forName("com.finnvek.knittools.ui.screens.counter.ReminderDialogForm")
        val constructor =
            formClass
                .getDeclaredConstructor(
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    String::class.java,
                    String::class.java,
                ).apply { isAccessible = true }
        val form = constructor.newInstance(row, 1, interval, "Decrease")
        val validation = formClass.getDeclaredMethod("getValidation").apply { isAccessible = true }.invoke(form)
        return validation.javaClass
            .getDeclaredMethod(
                "getCanSave",
            ).apply { isAccessible = true }
            .invoke(validation) as Boolean
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
