package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lukitsee uudistuksen rakenteelliset päätökset: ei desimaalitunteja, ei
 * korttisäiliöitä ja projektien värit tulevat ID:stä eivätkä listan järjestyksestä.
 */
class InsightsRedesignSourceTest {
    @Test
    fun `insights never formats decimal hours`() {
        INSIGHTS_FILES.forEach { path ->
            val source = ProjectSourceFiles.read(path)

            assertFalse("$path still formats decimal hours", source.contains("time_format_hours"))
            assertFalse("$path still divides minutes into float hours", source.contains("/ 60f"))
            assertFalse("$path still uses rows per hour", source.contains("rowsPerHour"))
            assertFalse("$path still uses the rows per hour resource", source.contains("R.string.pace_format"))
        }

        val strings = ProjectSourceFiles.read(STRINGS)
        assertFalse(strings.contains("""<string name="time_format_hours">"""))
        assertFalse(strings.contains("""<string name="pace_format">"""))
    }

    @Test
    fun `insights sections use hairline rules instead of card containers`() {
        INSIGHTS_FILES.forEach { path ->
            val source = ProjectSourceFiles.read(path)

            assertFalse("$path reintroduces a Card container", Regex("""\bCard\s*\(""").containsMatchIn(source))
            assertFalse("$path reintroduces a Surface container", Regex("""\bSurface\s*\(""").containsMatchIn(source))
        }

        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)
        assertTrue(sections.contains("InsightsDimens.RuleStrongAlpha"))
        assertTrue(sections.contains("InsightsDimens.RuleHairlineAlpha"))
    }

    @Test
    fun `pace is reported as minutes per row`() {
        val viewModel = ProjectSourceFiles.read(INSIGHTS_VIEW_MODEL)
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)

        assertTrue(viewModel.contains("MinutesPerRowFormatter.fromTotals"))
        assertTrue(sections.contains("R.string.insights_stat_min_per_row"))
    }

    @Test
    fun `project colour comes from the project id, not the list position`() {
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)

        assertTrue(sections.contains("yarnColorForId(project.projectId)"))
        assertFalse(sections.contains("InsightChartColors"))
        assertFalse(sections.contains("index % "))
    }

    @Test
    fun `zero buckets render as baseline ticks and the chart is one accessibility element`() {
        val chart = ProjectSourceFiles.read(INSIGHTS_CHART)

        assertTrue(chart.contains("InsightsDimens.ChartZeroTickHeight"))
        assertTrue(chart.contains("clearAndSetSemantics"))
    }

    @Test
    fun `range chips announce their selection and meet the touch target minimum`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)

        assertTrue("Range chips must use selectable semantics", screen.contains("selectable("))
        assertTrue("Range chips must be grouped for screen readers", screen.contains("selectableGroup()"))
        assertTrue("Chips must reserve a 48dp touch target", screen.contains("heightIn(min = ChipMinTouchTarget)"))
        assertFalse("Range chips must not fall back to plain clickable", screen.contains("clickable(role = Role.Tab)"))
    }

    private companion object {
        const val INSIGHTS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt"
        const val INSIGHTS_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSections.kt"
        const val INSIGHTS_CHART =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsChart.kt"
        const val INSIGHTS_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsViewModel.kt"
        const val STRINGS = "app/src/main/res/values/strings.xml"

        val INSIGHTS_FILES =
            listOf(INSIGHTS_SCREEN, INSIGHTS_SECTIONS, INSIGHTS_CHART, INSIGHTS_VIEW_MODEL)
    }
}
