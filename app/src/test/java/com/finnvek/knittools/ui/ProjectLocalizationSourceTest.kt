package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectLocalizationSourceTest {
    @Test
    fun `project formats have their required placeholders in every supported locale`() {
        val files = ProjectSourceFiles.localizedStringFiles()

        assertEquals(EXPECTED_LOCALE_DIRECTORIES, files.map { it.parent.fileName.toString() }.toSet())
        files.forEach { file ->
            val source = ProjectSourceFiles.read(file)
            PROJECT_FORMATS.forEach { (name, placeholders) ->
                val value = stringValue(source, name)

                assertEquals("$file has incorrect placeholders for $name", placeholders, formatPlaceholders(value))
                assertFalse("$file has a middle dot in $name", value.contains(MIDDLE_DOT))
            }
        }
    }

    @Test
    fun `default project formats keep the exact English wording`() {
        val source = ProjectSourceFiles.read(DEFAULT_STRINGS)

        DEFAULT_PROJECT_FORMATS.forEach { (name, expected) ->
            assertEquals("Unexpected default value for $name", expected, stringValue(source, name))
        }
    }

    @Test
    fun `project timestamps stay localized relative time without a date cutoff`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)

        assertTrue(source.contains(".getRelativeTimeSpanString("))
        assertTrue(source.contains("if (now - timestamp < DateUtils.MINUTE_IN_MILLIS)"))
        assertFalse(source.contains("DateUtils.DAY_IN_MILLIS"))
        assertFalse(source.contains("DateUtils.WEEK_IN_MILLIS"))
        assertFalse(source.contains("formatDateRange("))
        assertFalse(source.contains("LocaleDateFormat"))
    }

    private fun stringValue(
        source: String,
        name: String,
    ): String =
        STRING_VALUE_REGEX
            .format(name)
            .toRegex()
            .find(source)
            ?.groupValues
            ?.get(1)
            ?: throw AssertionError("Missing string resource: $name")

    private fun formatPlaceholders(value: String): List<String> =
        FORMAT_PLACEHOLDER_REGEX.findAll(value).map { it.value }.toList()

    private companion object {
        private const val PROJECT_LIST_ITEM =
            "app/src/main/java/com/finnvek/knittools/ui/components/ProjectListItem.kt"
        private const val DEFAULT_STRINGS = "app/src/main/res/values/strings.xml"
        private const val STRING_VALUE_REGEX = "<string name=\\\"%s\\\">([^<]*)</string>"
        private const val MIDDLE_DOT = '\u00b7'

        private val FORMAT_PLACEHOLDER_REGEX = "%\\d+\\$[sd]".toRegex()
        private val EXPECTED_LOCALE_DIRECTORIES =
            setOf(
                "values",
                "values-da",
                "values-de",
                "values-es",
                "values-fi",
                "values-fr",
                "values-it",
                "values-nb",
                "values-nl",
                "values-pt",
                "values-sv",
            )
        private val PROJECT_FORMATS =
            mapOf(
                "project_updated_format" to listOf("%1\$s"),
                "project_completed_format" to listOf("%1\$s"),
                "project_section_count_format" to listOf("%1\$s", "%2\$d"),
                "project_continue_content_description" to listOf("%1\$s"),
            )
        private val DEFAULT_PROJECT_FORMATS =
            mapOf(
                "project_updated_format" to "Updated %1\$s",
                "project_completed_format" to "Completed %1\$s",
                "project_section_count_format" to "%1\$s %2\$d",
                "project_continue_content_description" to "Continue %1\$s",
            )
    }
}
