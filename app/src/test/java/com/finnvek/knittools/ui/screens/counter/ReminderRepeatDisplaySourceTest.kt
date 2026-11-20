package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ReminderRepeatDisplaySourceTest {
    @Test
    fun `repeating reminders use the project unit and the next count`() {
        val source =
            ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/screens/counter/ReminderComponents.kt")
        assertTrue(source.contains("ReminderLogic.nextOccurrence(reminder, currentRow)"))
        assertTrue(source.contains("mainCounterCountText(target)"))
        listOf("rows", "rounds", "repeats").forEach { unit ->
            val pluralCall = Regex("""pluralStringResource\(\s*R\.plurals\.reminder_interval_$unit\s*,""")
            assertTrue("Missing reminder interval plural for $unit", pluralCall.containsMatchIn(source))
        }
    }

    @Test
    fun `new interval and next occurrence resources replace the internal occurrence index in all locales`() {
        ProjectSourceFiles.configuredResourceDirectories().forEach { directory ->
            val strings = ProjectSourceFiles.textResources(directory)
            assertFalse("repeat_count_format still exists in $directory", "repeat_count_format" in strings)
            assertFalse(
                "occurrence index is still available in $directory",
                "reminder_repeat_occurrence_format" in strings,
            )
            listOf("rows", "rounds", "repeats").forEach { unit ->
                assertEquals("plurals", strings.getValue("reminder_interval_$unit").tagName)
            }
            assertEquals("string", strings.getValue("reminder_next_format").tagName)
        }
    }

    @Test
    fun `the English interval describes every row or the selected number of rows`() {
        val rows =
            ProjectSourceFiles
                .textResources(
                    "values",
                ).getValue("reminder_interval_rows")
                .getElementsByTagName("item")
        assertEquals("then every row", rows.item(0).textContent)
        assertEquals("then every 8 rows", String.format(Locale.ROOT, rows.item(1).textContent, 8))
    }
}
