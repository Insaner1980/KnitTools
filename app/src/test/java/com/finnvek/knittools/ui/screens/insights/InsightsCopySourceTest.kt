package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lukitsee tekstipäätökset, joita kääntäjä ei näe koodista: suomen yksikkö-
 * lyhenteet ja se, ettei päivämäärää interpoloida hero-lauseeseen.
 */
class InsightsCopySourceTest {
    @Test
    fun `finnish relative units are separated from the number and weeks are not years`() {
        val finnish = ProjectSourceFiles.read(FINNISH_STRINGS)
        val gluedUnit = Regex("%1\\${'$'}d\\p{L}")

        RELATIVE_TIME_NAMES.forEach { name ->
            val value = stringValue(finnish, name)

            assertFalse("$name glues the unit to the number: $value", gluedUnit.containsMatchIn(value))
        }

        val weeks = stringValue(finnish, "relative_time_weeks_ago")

        assertTrue("Finnish week abbreviation must be vk, was: $weeks", weeks.contains("vk"))
        assertFalse("Finnish weeks must not read as years: $weeks", Regex("\\bv\\b").containsMatchIn(weeks))
    }

    @Test
    fun `all time lead-in never interpolates a date`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val source = file.toFile().readText()
            val leadIn = stringValue(source, "insights_hero_lead_in_all_time")

            assertFalse("$file interpolates a date into the hero sentence: $leadIn", leadIn.contains("%1"))
        }
    }

    @Test
    fun `the removed since-format is gone from every locale`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val source = file.toFile().readText()

            assertFalse("$file still defines insights_range_since_format", source.contains("name=\"insights_range_since_format\""))
            assertTrue("$file is missing insights_range_open_format", source.contains("name=\"insights_range_open_format\""))
        }
    }

    private fun stringValue(
        source: String,
        name: String,
    ): String =
        Regex("<string name=\"$name\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            .find(source)
            ?.groupValues
            ?.get(1)
            ?: error("Missing string $name")

    private companion object {
        const val FINNISH_STRINGS = "app/src/main/res/values-fi/strings.xml"

        val RELATIVE_TIME_NAMES =
            listOf(
                "relative_time_days_ago",
                "relative_time_weeks_ago",
                "relative_time_months_ago",
            )
    }
}
