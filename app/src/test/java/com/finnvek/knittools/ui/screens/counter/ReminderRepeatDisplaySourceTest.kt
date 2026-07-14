package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Varmistaa, että toistuvan muistutuksen rivi (CounterValueDisplay.ReminderRepeat)
 * ei enää näytä murtolukumuotoa "Repeat 11/3", vaan käyttää selkeää
 * monikkoresurssia "Repeat #11 every 3 rows".
 */
class ReminderRepeatDisplaySourceTest {
    @Test
    fun `reminder repeat resolves through plural resource and not the old fraction string`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertFalse(
            "ReminderRepeat must not use the old repeat_count_format string",
            source.contains("R.string.repeat_count_format"),
        )
        assertTrue(
            "ReminderRepeat must resolve via the reminder_repeat_occurrence_format plural",
            source.contains("R.plurals.reminder_repeat_occurrence_format"),
        )
        assertTrue(
            "ReminderRepeat must use pluralStringResource",
            source.contains("pluralStringResource("),
        )
    }

    @Test
    fun `old repeat_count_format resource is removed from every locale`() {
        ProjectSourceFiles.localizedStringFiles().forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)
            assertFalse(
                "repeat_count_format still present in $stringsFile",
                strings.contains("name=\"repeat_count_format\""),
            )
            assertTrue(
                "reminder_repeat_occurrence_format plural missing in $stringsFile",
                strings.contains("<plurals name=\"reminder_repeat_occurrence_format\""),
            )
        }
    }

    @Test
    fun `occurrence 11 every 3 rows renders the new copy and never the fraction`() {
        val strings = ProjectSourceFiles.read(DEFAULT_STRINGS)
        val other = pluralItem(strings, "reminder_repeat_occurrence_format", "other")
        val one = pluralItem(strings, "reminder_repeat_occurrence_format", "one")

        val rendered = String.format(other, 11, 3)

        assertEquals("Repeat #11 every 3 rows", rendered)
        assertNotEquals("Repeat 11/3", rendered)
        assertFalse("New copy must not contain a slash", rendered.contains("/"))
        assertFalse("New copy must not contain a middle dot", rendered.contains(" · "))
        assertEquals("Repeat #11 every row", String.format(one, 11))
    }

    private fun pluralItem(
        strings: String,
        name: String,
        quantity: String,
    ): String =
        strings
            .substringAfter("<plurals name=\"$name\"")
            .substringBefore("</plurals>")
            .substringAfter("quantity=\"$quantity\">")
            .substringBefore("</item>")

    private companion object {
        const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        const val DEFAULT_STRINGS = "app/src/main/res/values/strings.xml"
    }
}
