package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterStringResourcesSourceTest {
    @Test
    fun `localized counter strings include target helper plurals`() {
        ProjectSourceFiles.localizedStringFiles().forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)

            listOf(
                "counter_add_row",
                "counter_decrease_row",
                "counter_undo_last_change",
                "counter_decrease_named",
                "counter_increase_named",
                "counter_actions",
                "counter_value_of_target_format",
                "counter_target_remaining_format",
                "counter_target_reached",
                "counter_target_past_format",
                "project_content_add_pattern",
                "stitch_tracker_label",
            ).forEach { key ->
                assertTrue("Missing $key in $stringsFile", hasStringResource(strings, key))
            }

            COUNTER_PLURAL_KEYS.forEach { key ->
                assertTrue("Missing plural $key in $stringsFile", strings.contains("<plurals name=\"$key\""))
            }

            if (stringsFile.parent.fileName.toString() in MANY_QUANTITY_LOCALES) {
                COUNTER_PLURAL_KEYS.forEach { key ->
                    assertTrue(
                        "Missing many quantity for $key in $stringsFile",
                        counterPlural(strings, key).contains("""quantity="many""""),
                    )
                }
            }
        }
    }

    @Test
    fun `localized counter strings avoid stale copy`() {
        ProjectSourceFiles.localizedStringFiles().forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)

            listOf(
                "counter_target_one_row_left",
                "counter_target_past_one",
                "counter_target_past_many",
                "counter_target_rows_left",
                "counter_target_rows_past",
                "project_content_pattern",
                "counter_undo",
                "current_row",
                "project_content_open_pattern",
                "project_content_attach_pattern",
            ).forEach { key ->
                assertFalse("Stale string $key still exists in $stringsFile", hasStringResource(strings, key))
            }

            assertFalse(
                "Old shaping key still exists in $stringsFile",
                strings.contains("<string name=\"next_shaping_format\""),
            )
            assertFalse(
                "Old shaping next-row key still exists in $stringsFile",
                strings.contains("<string name=\"next_shaping_counter_format\""),
            )
            assertFalse(
                "Repeat section copy still uses middle dot in $stringsFile",
                counterString(strings, "repeat_section_progress_format").contains("·"),
            )
            listOf(
                "counter_target_rows_left",
                "counter_target_rows_past",
            ).forEach { key ->
                assertFalse(
                    "Stale plural $key still exists in $stringsFile",
                    strings.contains("<plurals name=\"$key\""),
                )
            }
        }
    }

    @Test
    fun `repeat section progress format uses locked English and Finnish copy`() {
        assertEquals(
            "Repeat %1\$d/%2\$d, Row %3\$d/%4\$d",
            repeatSectionProgressFormat("app/src/main/res/values/strings.xml"),
        )
        assertEquals(
            "Toisto %1\$d/%2\$d, kerros %3\$d/%4\$d",
            repeatSectionProgressFormat("app/src/main/res/values-fi/strings.xml"),
        )
    }

    @Test
    fun `repeat section progress format keeps placeholders in order in every locale`() {
        val stringFiles = ProjectSourceFiles.localizedStringFiles()
        assertEquals("Expected repeat section progress copy in all 11 string files", 11, stringFiles.size)

        stringFiles.forEach { stringsFile ->
            val format = counterString(ProjectSourceFiles.read(stringsFile), "repeat_section_progress_format")

            assertEquals(
                "repeat_section_progress_format placeholders changed in $stringsFile",
                EXPECTED_REPEAT_SECTION_PLACEHOLDERS,
                FORMAT_PLACEHOLDER_REGEX.findAll(format).map { it.value }.toList(),
            )
        }
    }

    @Test
    fun `English repeat section progress format does not use old of structure`() {
        val format = repeatSectionProgressFormat("app/src/main/res/values/strings.xml")

        assertFalse("English repeat section progress format still uses old of structure", format.contains(" of "))
    }

    @Test
    fun `project card rows use plural resources in every locale`() {
        val displayTextSource = ProjectSourceFiles.read(MAIN_COUNTER_DISPLAY_TEXT)

        assertTrue(
            "ROWS project-card count must use the rows_format plural",
            displayTextSource.contains(
                "MainCounterLabelType.ROWS -> pluralStringResource(R.plurals.rows_format, slot.count, slot.count)",
            ),
        )

        ProjectSourceFiles.localizedStringFiles().forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)

            assertTrue("Missing rows_format plural in $stringsFile", strings.contains("<plurals name=\"rows_format\""))
            assertFalse("rows_format string still present in $stringsFile", hasStringResource(strings, "rows_format"))
        }
    }

    @Test
    fun `counter scoped source does not use middle dot separators or old shaping string`() {
        listOf(
            COUNTER_WORKSPACE_SECTIONS,
            COUNTER_PROJECT_CONTENT_CARDS,
            MULTI_COUNTER_COMPONENTS,
            YARN_MANAGEMENT_SHEET,
        ).forEach { sourceFile ->
            val source = ProjectSourceFiles.read(sourceFile)

            assertFalse("$sourceFile still uses middle dot separator", source.contains(" · "))
            assertFalse("$sourceFile still references old shaping string", source.contains("next_shaping_format"))
            assertFalse(
                "$sourceFile still references next-row shaping string",
                source.contains("next_shaping_counter_format"),
            )
        }
    }

    private fun counterString(
        strings: String,
        key: String,
    ): String = strings.substringAfter("<string name=\"$key\"").substringAfter(">").substringBefore("</string>")

    private fun hasStringResource(
        strings: String,
        key: String,
    ): Boolean = Regex("""<string\s+name="$key"(\s|>)""").containsMatchIn(strings)

    private fun counterPlural(
        strings: String,
        key: String,
    ): String = strings.substringAfter("<plurals name=\"$key\"").substringBefore("</plurals>")

    private fun repeatSectionProgressFormat(relativePath: String): String =
        counterString(ProjectSourceFiles.read(relativePath), "repeat_section_progress_format")

    private companion object {
        val MANY_QUANTITY_LOCALES = setOf("values-es", "values-fr", "values-it", "values-pt")
        val COUNTER_PLURAL_KEYS =
            listOf(
                "reminder_repeat_occurrence_format",
                "rows_format",
            )
        val EXPECTED_REPEAT_SECTION_PLACEHOLDERS = listOf("%1\$d", "%2\$d", "%3\$d", "%4\$d")
        val FORMAT_PLACEHOLDER_REGEX = Regex("%\\d\\\$d")

        const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        const val COUNTER_PROJECT_CONTENT_CARDS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt"
        const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        const val YARN_MANAGEMENT_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/YarnManagementSheet.kt"
        const val MAIN_COUNTER_DISPLAY_TEXT =
            "app/src/main/java/com/finnvek/knittools/ui/components/MainCounterDisplayText.kt"
    }
}
