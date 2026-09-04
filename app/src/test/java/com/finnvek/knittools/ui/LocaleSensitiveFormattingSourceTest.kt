package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleSensitiveFormattingSourceTest {
    @Test
    fun `date displays derive locale patterns from skeletons`() {
        val helper = ProjectSourceFiles.read(LOCALE_DATE_FORMAT)

        assertTrue(helper.contains("DateFormat.getBestDateTimePattern"))
        assertTrue(helper.contains("DateFormat.is24HourFormat"))

        DATE_DISPLAY_FILES.forEach { path ->
            val source = ProjectSourceFiles.read(path)
            assertFalse("$path contains a fixed date pattern", FIXED_DATE_PATTERNS.any(source::contains))
        }
    }

    @Test
    fun `localized messages are not assembled with fixed punctuation`() {
        val castOn = ProjectSourceFiles.read(CAST_ON_SCREEN)
        val insightsSections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)
        val projectList = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertTrue(castOn.contains("R.string.edge_stitches_total_format"))
        assertFalse(castOn.contains("stringResource(R.string.edge_stitches_optional) +"))
        assertTrue(insightsSections.contains("R.string.insights_project_sub_format"))
        assertFalse(insightsSections.contains("\"\$rowsText, \$lastActiveText\""))
        assertFalse(projectList.contains("rowContext + \", \" +"))
        assertFalse(projectList.contains(".joinToString(\", \")"))
    }

    @Test
    fun `needle metric values use locale aware decimal formatting`() {
        val needleScreen = ProjectSourceFiles.read(NEEDLE_SIZE_SCREEN)

        assertTrue(needleScreen.contains("formatDecimalForDisplay(needle.metricMm, locale, 0, 2)"))
        assertFalse(needleScreen.contains("needle.metricMm.toString()"))
    }

    @Test
    fun `new message formats exist in every localized string file`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val source = file.toFile().readText()
            REQUIRED_STRING_NAMES.forEach { name ->
                assertTrue("$file is missing $name", source.contains("name=\"$name\""))
            }
        }
    }

    private companion object {
        private const val LOCALE_DATE_FORMAT =
            "app/src/main/java/com/finnvek/knittools/ui/components/LocaleDateFormat.kt"
        private const val CAST_ON_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/caston/CastOnScreen.kt"
        private const val INSIGHTS_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSections.kt"
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
        private const val NEEDLE_SIZE_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/needles/NeedleSizeScreen.kt"

        private val DATE_DISPLAY_FILES =
            listOf(
                "app/src/main/java/com/finnvek/knittools/ui/components/ProjectListItem.kt",
                "app/src/main/java/com/finnvek/knittools/ui/components/SessionItem.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoComponents.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsChart.kt",
            )
        private val FIXED_DATE_PATTERNS = listOf("\"MMM d\"", "\"d MMM\"", "\"d.M.\"")
        private val REQUIRED_STRING_NAMES =
            listOf(
                "insights_project_sub_format",
                "edge_stitches_total_format",
                "measurement_with_unit_format",
            )
    }
}
