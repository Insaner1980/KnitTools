package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.domain.model.RowReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CounterProjectInfoTest {
    @Test
    fun `first project note line ignores blank lines`() {
        assertEquals(
            "Sleeve shaping starts here",
            firstProjectNoteLineReflective("\n  \nSleeve shaping starts here\nSecond note"),
        )
        assertNull(firstProjectNoteLineReflective(" \n\t "))
    }

    @Test
    fun `nearest upcoming reminder picks current or next incomplete row`() {
        val reminders =
            listOf(
                reminder(id = 1, row = 4),
                reminder(id = 2, row = 14),
                reminder(id = 3, row = 9),
                reminder(id = 4, row = 12, isCompleted = true),
            )

        assertEquals(3L, nearestUpcomingReminderReflective(reminders, currentRow = 9)?.id)
        assertEquals(2L, nearestUpcomingReminderReflective(reminders, currentRow = 10)?.id)
        assertNull(nearestUpcomingReminderReflective(reminders, currentRow = 15))
    }

    @Test
    fun `empty project info collapses to add helper row`() {
        val projectInfoFile = ProjectSourceFiles.file(COUNTER_PROJECT_INFO)

        assertTrue("CounterProjectInfo.kt is missing", Files.exists(projectInfoFile))

        val projectInfo = ProjectSourceFiles.read(COUNTER_PROJECT_INFO)
        assertTrue(projectInfo.contains("data class CounterWorkspaceSummary"))
        assertTrue(projectInfo.contains("data class ProjectInfoRow"))
        assertTrue(projectInfo.contains("enum class ProjectInfoKind"))
        assertTrue(projectInfo.contains("ProjectInfoKind.EMPTY"))
        assertTrue(projectInfo.contains("if (rows.isEmpty())"))
    }

    private fun firstProjectNoteLineReflective(notes: String): String? =
        projectInfoKt()
            .getDeclaredMethod("firstProjectNoteLine", String::class.java)
            .invoke(null, notes) as String?

    @Suppress("UNCHECKED_CAST")
    private fun nearestUpcomingReminderReflective(
        reminders: List<RowReminder>,
        currentRow: Int,
    ): RowReminder? =
        projectInfoKt()
            .getDeclaredMethod("nearestUpcomingReminder", List::class.java, Int::class.javaPrimitiveType)
            .invoke(null, reminders, currentRow) as RowReminder?

    private fun projectInfoKt(): Class<*> =
        Class.forName("com.finnvek.knittools.ui.screens.counter.CounterProjectInfoKt")

    private fun reminder(
        id: Long,
        row: Int,
        isCompleted: Boolean = false,
    ) = RowReminder(
        id = id,
        projectId = 10,
        targetRow = row,
        message = "Reminder $row",
        isCompleted = isCompleted,
    )

    private companion object {
        private const val COUNTER_PROJECT_INFO =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectInfo.kt"
    }
}
