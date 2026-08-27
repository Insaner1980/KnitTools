package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
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

        assertTrue(viewModel.contains("MinutesPerRowFormatter.fromSeconds"))
        assertTrue(sections.contains("R.string.insights_stat_min_per_row"))
    }

    @Test
    fun `project colour comes from the project id, not the list position`() {
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)

        assertTrue(sections.contains("yarnColorForId(project.projectId,"))
        assertTrue(
            "The palette must follow the theme; cream drops the light yarns under 3:1",
            sections.contains("MaterialTheme.knitToolsColors.yarnPalette"),
        )
        assertFalse(sections.contains("InsightChartColors"))
        assertFalse(sections.contains("index % "))
    }

    @Test
    fun `the chart is one accessibility element that can still be operated`() {
        val chart = ProjectSourceFiles.read(INSIGHTS_CHART)

        assertTrue(chart.contains("clearAndSetSemantics"))
        assertTrue("Selection must be reachable without touch", chart.contains("customActions"))
        assertFalse("Zero ticks were replaced by the continuous baseline", chart.contains("ChartZeroTickHeight"))

        // Lukema muutti osio-otsikon mittalukuriville, joten live region muutti sinne myös.
        // Live region kuuluu solmuun jolla on itsellään tekstiä: paljas säiliö ei
        // yhdistä jälkeläisiään, jolloin ruudunlukija ei saa mitään luettavaa.
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)
        assertEquals(
            "The readout live region must sit on a merging node",
            1,
            Regex("""semantics\(mergeDescendants = true\) \{ liveRegion""").findAll(sections).count(),
        )
        // Reunatapausten ja askelten käyttäytyminen todistetaan kutsumalla
        // moveChartSelection-funktiota InsightsChartSelectionTest:ssä.
    }

    @Test
    fun `the hero sentence never carries a date and the range has its own line`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)

        assertTrue("The range needs a kicker row of its own", screen.contains("InsightsRangeKicker"))
        assertTrue(
            "The range must be rendered as its own case-free line",
            screen.contains("R.string.insights_range_format"),
        )
        assertFalse("Hero must not format a month into the sentence", sections.contains("heroSinceMonthText"))
        assertFalse(
            "The case-bound since-format must not come back into the sentence",
            sections.contains("R.string.insights_range_since_format"),
        )
    }

    @Test
    fun `the chart carries no duplicate scale or standing instruction`() {
        val chart = ProjectSourceFiles.read(INSIGHTS_CHART)

        // Korkein pylväs on itse asteikko: erillinen maksimileima toisti valitun
        // lukeman aina kun korkein oli valittuna.
        assertFalse(chart.contains("insights_chart_max_format"))
        assertFalse("A standing hint is noise once the chart is understood", chart.contains("insights_chart_hint"))
        assertTrue("The axis must thin its labels, not drop them", chart.contains("axisLabelIndices"))
        assertTrue("Narrow buckets need drag selection", chart.contains("detectHorizontalDragGestures"))
    }

    @Test
    fun `the screen keeps one loud level and one shared section style`() {
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)

        // Osio-otsikko on kategoriamerkki kuten Libraryssa, ei neljäs otsikkokoko.
        assertTrue(sections.contains("MaterialTheme.typography.labelSmall"))
        assertTrue(sections.contains("knitToolsColors.brandWine"))
    }

    @Test
    fun `project share is one bar for the whole range, not one per row`() {
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)

        assertTrue("The mix bar answers the share question at a glance", sections.contains("InsightsProjectMixBar"))
        assertFalse("Per-row bars could not be compared with each other", sections.contains("ProjectShareBar"))
        assertFalse("The bar carries the share; the percent repeated it", sections.contains("ProjectShareFontSize"))
    }

    @Test
    fun `the range is the title, not a control of its own`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)

        // Otsikkorivi + segmenttivalitsin + projektipilleri söivät 188 dp ennen kuin
        // yhtään dataa näkyi. Aikaväli otsikkona hoitaa saman yhdellä rivillä.
        assertTrue("The range belongs in the app bar title", screen.contains("InsightsRangeTitle"))
        assertTrue("The title belongs in a TopAppBar", screen.contains("TopAppBar("))
        assertFalse(
            "A separate range control must not come back as its own row",
            screen.contains("SegmentedToggle(") || screen.contains("InsightsRangeChip"),
        )
        assertTrue(
            "The project filter must reserve the touch target even when drawn smaller",
            screen.contains("minimumInteractiveComponentSize()"),
        )
    }

    @Test
    fun `the range line is only shown where it is not already the title`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)

        // "This Month" + "Aug 1 – Aug 19" + akselin päivät oli sama tieto kolmesti,
        // ja se söi tilan projektisuodattimelta.
        assertTrue(screen.contains("uiState.timeRange == TimeRange.ALL_TIME"))
        assertFalse(
            "A weighted spacer splits the free space in half and truncates the text early",
            screen.contains("Spacer(modifier = Modifier.weight(1f))"),
        )
    }

    @Test
    fun `the filtered view links to the existing history instead of copying it`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)

        // SessionHistoryScreen näyttää jo istunnot; Insights ei piirrä omaa kopiotaan.
        assertTrue(screen.contains("onSessionHistory"))
        assertTrue("The link must use the shared list item", screen.contains("HubListItem("))
        assertFalse("Sessions belong to their own screen", screen.contains("SessionItem("))
        // Linkki tarvitaan nimenomaan silloin kun tällä aikavälillä ei ole istuntoja.
        assertTrue(
            "The link must not depend on there being time in the range",
            screen.contains("if (selectedProjectId != null)"),
        )
    }

    @Test
    fun `a single bar is a number, not a chart`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)
        val viewModel = ProjectSourceFiles.read(INSIGHTS_VIEW_MODEL)

        // Asteikko skaalautuu maksimiin, joten ainoa pylväs piirtyy aina täyskorkeana.
        assertTrue(screen.contains("if (!uiState.hasMeaningfulChartData) return"))
        assertTrue(viewModel.contains("MINIMUM_MEANINGFUL_CHART_BUCKETS = 2"))
    }

    @Test
    fun `the meaningful data guard hides empty upsells without exposing free chart points`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)
        val viewModel = ProjectSourceFiles.read(INSIGHTS_VIEW_MODEL)
        val sections = ProjectSourceFiles.read(INSIGHTS_SECTIONS)

        assertTrue(screen.contains("InsightsProChartPrompt("))
        assertTrue(viewModel.contains("if (featureGates.canUseCharts) fillChartBuckets"))
        assertTrue(viewModel.contains("else emptyList()"))
        assertFalse("The Insights prompt must stay cardless", sections.contains(".border("))
    }

    @Test
    fun `session durations use the shared formatter while Projects stays duration free`() {
        val sessionItem = ProjectSourceFiles.read(SESSION_ITEM)
        val projectList = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        // Oma apuri näytti suomeksi "36m" alle tunnin ja "1t 5min" yli tunnin istunnoille.
        assertTrue(sessionItem.contains("DurationDisplayFormatter.fromMinutes"))
        assertFalse(sessionItem.contains("time_spent_minutes_format"))
        assertFalse(sessionItem.contains("session_duration_format"))

        listOf(
            "DurationDisplayFormatter",
            "durationText(",
            "formatMinutes",
            "time_spent_minutes_format",
            "session_duration_format",
            "totalMinutes",
        ).forEach { forbidden ->
            assertFalse("Projects must not use $forbidden", projectList.contains(forbidden))
        }
    }

    @Test
    fun `the project menu is styled and shows which filter is on`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)

        // Valikko oli näytön ainoa vakio-Material-pinta, eikä siitä nähnyt mikä on valittu.
        // Molemmat pudotusvalikot samaan tyyliin: kaksi eri nakoista samalla naytolla
        // luki huolimattomuutena, ja aikavalivalikko jai ilman valintamerkintaa.
        assertEquals(
            "Both dropdowns must use the app surface",
            2,
            screen.split("containerColor = MaterialTheme.colorScheme.surface").size - 1,
        )
        assertTrue(screen.contains("InsightsMenuItem"))
        assertTrue("The menu must carry the same yarn dots as the pill", screen.contains("dotColor"))
    }

    @Test
    fun `the chart readout appears on demand instead of holding a row`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)
        val chart = ProjectSourceFiles.read(INSIGHTS_CHART)

        assertTrue("The readout shares the section meta row", screen.contains("chartHeaderMeta"))
        assertFalse("The chart must not keep a standing readout row", chart.contains("ChartReadout"))
    }

    @Test
    fun `chart selection is additive and never repaints a band`() {
        val chart = ProjectSourceFiles.read(INSIGHTS_CHART)
        val dimens = ProjectSourceFiles.read(INSIGHTS_DIMENS)

        assertFalse(
            "The full-height selection band read as a rendering glitch in sparse data",
            chart.contains("selectionBandColor") || dimens.contains("ChartSelectionBandAlpha"),
        )
        assertFalse(
            "Dimming cannot keep this palette above 3:1, so selection must be additive",
            dimens.contains("ChartUnselectedBarAlpha") || chart.contains("chartBarAlpha("),
        )
        assertTrue(
            "The baseline marker stays: a selected bucket can have no bar at all",
            chart.contains("ChartSelectionMarkerHeight"),
        )
    }

    @Test
    fun `project fabric follows the chart and precedes the project mix`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)
        val chartIndex = screen.indexOf("chartSection(")
        val fabricIndex = screen.indexOf("projectFabricSection(")
        val projectMixIndex = screen.indexOf("R.string.insights_section_where_time_went")

        assertTrue(chartIndex >= 0)
        assertTrue(fabricIndex > chartIndex)
        assertTrue(projectMixIndex > fabricIndex)
        assertTrue(screen.contains("uiState.projectFabric ?: return"))
    }

    @Test
    fun `project fabric keeps every project and has no intensity model`() {
        val fabric = ProjectSourceFiles.read(INSIGHTS_PROJECT_FABRIC)
        val model = ProjectSourceFiles.read(INSIGHTS_PROJECT_FABRIC_MODEL)
        val combined = fabric + model

        assertFalse(combined.contains("dominantProjectId"))
        assertFalse(combined.contains("activityRamp"))
        assertFalse(combined.contains("FabricStitch"))
        assertFalse(combined.contains("OtherSegment"))
        assertFalse(Regex("\\blevel\\b").containsMatchIn(combined))
        assertTrue(model.contains("val projectIds: List<Long>"))
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
        const val SESSION_ITEM =
            "app/src/main/java/com/finnvek/knittools/ui/components/SessionItem.kt"
        const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
        const val INSIGHTS_DIMENS =
            "app/src/main/java/com/finnvek/knittools/ui/theme/InsightsDimens.kt"
        const val INSIGHTS_PROJECT_FABRIC =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsProjectFabric.kt"
        const val INSIGHTS_PROJECT_FABRIC_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsProjectFabricModel.kt"
        const val STRINGS = "app/src/main/res/values/strings.xml"

        val INSIGHTS_FILES =
            listOf(INSIGHTS_SCREEN, INSIGHTS_SECTIONS, INSIGHTS_CHART, INSIGHTS_VIEW_MODEL)
    }
}
