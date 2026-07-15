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
        val activityGrid = ProjectSourceFiles.read(ACTIVITY_GRID)
        val projectList = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertTrue(castOn.contains("R.string.edge_stitches_total_format"))
        assertFalse(castOn.contains("stringResource(R.string.edge_stitches_optional) +"))
        assertTrue(activityGrid.contains("R.string.activity_grid_tooltip_format"))
        assertFalse(activityGrid.contains("\"\$dayName \$dateStr — \$minutesText\""))
        assertFalse(projectList.contains("rowContext + \", \" +"))
        assertFalse(projectList.contains(".joinToString(\", \")"))
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
        private const val ACTIVITY_GRID =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/ActivityGrid.kt"
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"

        private val DATE_DISPLAY_FILES =
            listOf(
                "app/src/main/java/com/finnvek/knittools/ui/components/ProjectCard.kt",
                "app/src/main/java/com/finnvek/knittools/ui/components/SessionItem.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoComponents.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt",
            )
        private val FIXED_DATE_PATTERNS = listOf("\"MMM d\"", "\"d MMM\"", "\"d.M.\"")
        private val REQUIRED_STRING_NAMES =
            listOf(
                "activity_grid_tooltip_format",
                "edge_stitches_total_format",
                "measurement_with_unit_format",
            )
    }
}
