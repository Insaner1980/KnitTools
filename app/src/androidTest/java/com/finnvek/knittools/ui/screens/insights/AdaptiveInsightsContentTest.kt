package com.finnvek.knittools.ui.screens.insights

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.navigation.insightsHeading
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AdaptiveInsightsContentTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun fixedCalendarRangesKeepChartSelectionAcrossResizeAndRestoration() {
        var state by mutableStateOf(populated())
        var size by mutableStateOf(800 to 700)
        val restoration = restoration({ state }, { size })
        for (range in TimeRange.entries) {
            val axis = insightsChartAxis(range, today, LocalDate.of(2024, 1, 1), DayOfWeek.MONDAY)
            val buckets =
                axis.bucketStarts.mapIndexed { index, date ->
                    InsightsChartBucket(date, (index + 1) * 10, index.toLong())
                }
            rule.runOnIdle {
                state =
                    populated().copy(
                        timeRange = range,
                        rangeStart = axis.bucketStarts.first(),
                        chartInterval = axis.interval,
                        chartBuckets = buckets,
                    )
            }
            for (viewport in listOf(800 to 270, 800 to 700)) {
                rule.runOnIdle { size = viewport }
                list().performScrollToKey("chart-heading")
                selectBucket(1, state.chartBuckets.size)
                val selected = heading()
                restoration.emulateSavedInstanceStateRestore()
                assertEquals(selected, heading())
                selectBucket(2, state.chartBuckets.size)
                assertTrue(selected != heading())
            }
        }
    }

    @Test
    fun projectFabricHitTestingAndSelectedDaySurviveResizeAndRestoration() {
        val start = LocalDate.of(2026, 9, 28)
        val fabric =
            InsightsProjectFabricModel(
                start,
                today,
                DayOfWeek.MONDAY,
                (0L..23L).map { InsightsProjectFabricDay(start.plusDays(it), listOf(it % 25 + 1)) },
            )
        val data = populated().copy(projectFabric = fabric)
        var size by mutableStateOf(800 to 700)
        val restoration = restoration({ data }, { size })

        fun selectFirstDay() {
            list().performScrollToKey("fabric")
            val plotHeight =
                rule
                    .onNodeWithTag("insights-fabric")
                    .fetchSemanticsNode()
                    .layoutInfo.height
            rule.onNodeWithTag("insights-fabric").performTouchInput {
                val gap = with(rule.density) { InsightsDimens.ProjectFabricCellGap.toPx() }
                val cell =
                    projectFabricCellSize(
                        width.toFloat(),
                        gap,
                        projectFabricColumnCount(fabric),
                        with(rule.density) { InsightsDimens.ProjectFabricMaxCellSize.toPx() },
                    )
                val labelHeight = plotHeight - cell * 7 - gap * 6
                click(Offset(cell / 2, labelHeight + cell / 2))
            }
        }
        selectFirstDay()
        list().performScrollToKey("fabric-selection")
        val selection = hasText(data.projects.first().name) and hasAnyAncestor(hasTestTag("insights-fabric-selection"))
        rule.onNode(selection).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNode(selection).assertIsDisplayed()
        for (next in listOf(360 to 700, 800 to 270, 1280 to 670)) {
            rule.runOnIdle { size = next }
            selectFirstDay()
            list().performScrollToKey("fabric-selection")
            rule.onNode(selection).assertIsDisplayed()
        }
    }

    @Test
    fun selectionAndScrollRestoreThroughLoadingAndResizeAndInvalidSelectionClears() {
        val data = populated()
        var state by mutableStateOf(data)
        var size by mutableStateOf(800 to 700)
        val restoration = restoration({ state }, { size })
        list().performScrollToKey("chart-heading")
        selectBucket(1, data.chartBuckets.size)
        val selectedText = heading()
        list().performScrollToKey("project-15")
        rule.onNode(hasText("Project 15", substring = true)).assertIsDisplayed()
        rule.runOnIdle { state = data.copy(isLoading = true, chartBuckets = emptyList(), projectFabric = null) }
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("insights-list").assertDoesNotExist()
        rule.runOnIdle { state = data }
        rule.onNode(hasText("Project 15", substring = true)).assertIsDisplayed()
        rule.runOnIdle { size = 360 to 700 }
        rule.onNode(hasText("Project 15", substring = true)).assertIsDisplayed()
        list().performScrollToKey("chart-heading")
        assertEquals(selectedText, heading())
        selectBucket(2, data.chartBuckets.size)
        assertTrue(selectedText != heading())
        // A real data refresh can remove the selected identity without resetting the range.
        rule.runOnIdle { state = data.copy(chartBuckets = data.chartBuckets.drop(3)) }
        assertTrue(heading() != selectedText)
        rule.runOnIdle { state = data.copy(timeRange = TimeRange.THIS_WEEK) }
        assertTrue(heading() != selectedText)
    }

    @Test
    fun emptySparseAndFreeStatesKeepExistingVisibilityRulesInBothThemes() {
        var state by mutableStateOf(InsightsUiState(isLoading = false))
        var dark by mutableStateOf(false)
        var size by mutableStateOf(360 to 700)
        rule.setContent {
            KnitToolsTheme(isDarkTheme = dark) {
                Box(Modifier.requiredSize(size.first.dp, size.second.dp)) {
                    InsightsScreenContent(state, {}, {})
                }
            }
        }
        for (theme in listOf(false, true)) {
            for (next in listOf(360 to 700, 800 to 270, 800 to 1100, 1280 to 670)) {
                rule.runOnIdle {
                    dark = theme
                    size = next
                    state = InsightsUiState(isLoading = false)
                }
                rule.onNodeWithTag("insights-empty").assertIsDisplayed()
                rule.onNodeWithTag("insights-hero").assertDoesNotExist()
                val sparse =
                    populated().copy(
                        hasMeaningfulChartData = false,
                        projects = populated().projects.take(1),
                        timePerProject = populated().timePerProject.take(1),
                        chartBuckets = listOf(InsightsChartBucket(today, 40, 12)),
                        totalMinutes = 40,
                        totalDuration = DurationDisplayFormatter.fromMinutes(40),
                    )
                rule.runOnIdle { state = sparse }
                rule.onNodeWithTag("insights-hero").assertIsDisplayed()
                rule.onNodeWithTag("insights-chart").assertDoesNotExist()
                rule.runOnIdle { state = populated().copy(isPro = false, chartBuckets = emptyList()) }
                list().performScrollToKey("chart")
                rule.onNodeWithTag("insights-chart").assertIsDisplayed()
                rule.onNodeWithTag("insights-chart-plot", useUnmergedTree = true).assertDoesNotExist()
                rule.runOnIdle {
                    state =
                        populated().copy(totalMinutes = 0, hasMeaningfulChartData = false, timePerProject = emptyList())
                }
                rule.onNodeWithTag("insights-hero").assertDoesNotExist()
                list().performScrollToKey("range-empty")
                rule.onNodeWithTag("insights-range-empty").assertIsDisplayed()
            }
        }
    }

    @Test
    fun localizedAxisLabelsNeverOverlapOrLeaveThePlotAndKeepChronologicalOrderInRtl() {
        var width by mutableStateOf(328)
        var locale by mutableStateOf("en")
        var range by mutableStateOf(TimeRange.THIS_WEEK)
        var rtl by mutableStateOf(false)
        rule.setContent {
            val context = rule.activity.localizedTestContext(locale)
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                KnitToolsTheme {
                    Box(Modifier.width(width.dp).testTag("axis-bounds")) {
                        ChartAxisLabels(
                            buckets = axisBuckets(range),
                            interval =
                                if (range ==
                                    TimeRange.ALL_TIME
                                ) {
                                    PaceGroupingInterval.MONTH
                                } else {
                                    PaceGroupingInterval.DAY
                                },
                            timeRange = range,
                            today = today,
                        )
                    }
                }
            }
        }
        for (language in listOf("en", "fi", "de", "pt-BR")) {
            for (timeRange in TimeRange.entries) {
                for (nextWidth in listOf(328, 568, 1168)) {
                    rule.runOnIdle {
                        width = nextWidth
                        locale = language
                        range = timeRange
                        rtl = language == "fi"
                    }
                    rule.waitForIdle()
                    val measuredLabels =
                        rule
                            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                            .fetchSemanticsNodes()
                    // Layout measures candidates before omitting colliding ticks from placement.
                    val labels = measuredLabels.filter { it.layoutInfo.isPlaced && it.boundsInRoot.width > 0f }
                    println(
                        "INSIGHTS_AXIS $language $timeRange $nextWidth " +
                            "measured=${measuredLabels.size} placed=${labels.size}",
                    )
                    assertTrue("Axis must keep readable ticks", labels.size >= 2)
                    val plotBounds = rule.onNodeWithTag("axis-bounds").fetchSemanticsNode().boundsInRoot
                    labels.forEach { label ->
                        assertTrue(label.boundsInRoot.left >= plotBounds.left)
                        assertTrue(label.boundsInRoot.right <= plotBounds.right)
                        assertEquals(label.layoutInfo.width.toFloat(), label.boundsInRoot.width, 1f)
                    }
                    val sorted = labels.sortedBy { it.boundsInRoot.left }
                    sorted.zipWithNext().forEach { (left, right) ->
                        assertTrue(
                            "$language $timeRange $nextWidth: ${left.config[SemanticsProperties.Text]} " +
                                "${left.boundsInRoot} overlaps ${right.config[SemanticsProperties.Text]} " +
                                "${right.boundsInRoot}",
                            left.boundsInRoot.right < right.boundsInRoot.left,
                        )
                    }
                    assertTrue(
                        sorted.last().boundsInRoot.right - sorted.first().boundsInRoot.left <=
                            nextWidth * rule.density.density,
                    )
                    if (timeRange == TimeRange.ALL_TIME) {
                        assertTrue(
                            sorted
                                .first()
                                .config[SemanticsProperties.Text]
                                .joinToString()
                                .contains("2024"),
                        )
                        assertTrue(
                            sorted
                                .last()
                                .config[SemanticsProperties.Text]
                                .joinToString()
                                .contains("2026"),
                        )
                    }
                }
            }
        }
    }

    private fun restoration(
        state: () -> InsightsUiState,
        size: () -> Pair<Int, Int>,
    ) = StateRestorationTester(rule).apply {
        setContent {
            KnitToolsTheme {
                val viewport = size()
                Box(Modifier.requiredSize(viewport.first.dp, viewport.second.dp)) {
                    InsightsScreenContent(state(), {}, {})
                }
            }
        }
    }

    private fun list() = rule.onNodeWithTag("insights-list")

    private fun selectBucket(
        index: Int,
        count: Int,
    ) {
        rule.onNodeWithTag("insights-chart-plot", useUnmergedTree = true).performTouchInput {
            click(Offset(width * (index + 0.5f) / count, height / 2f))
        }
    }

    private fun heading() = rule.insightsHeading()

    private fun axisBuckets(range: TimeRange): List<InsightsChartBucket> =
        when (range) {
            TimeRange.THIS_WEEK -> (0L..6L).map { today.minusDays(6 - it) }
            TimeRange.THIS_MONTH -> (1..today.dayOfMonth).map { today.withDayOfMonth(it) }
            TimeRange.ALL_TIME -> (0L..33L).map { LocalDate.of(2024, 1, 1).plusMonths(it) }
        }.mapIndexed { index, date -> InsightsChartBucket(date, (index + 1) * 10, index.toLong()) }

    private fun populated(): InsightsUiState {
        val projects = (1L..25L).map { CounterProject(id = it, name = "Project $it — long synthetic project name") }
        return InsightsUiState(
            isLoading = false,
            isPro = true,
            hasAnySessionData = true,
            hasSessionData = true,
            hasMeaningfulChartData = true,
            totalMinutes = 280,
            totalDuration = DurationDisplayFormatter.fromMinutes(280),
            activeDays = 7,
            daysInRange = 7,
            rangeStart = today.minusDays(6),
            rangeEnd = today,
            chartBuckets = axisBuckets(TimeRange.THIS_WEEK),
            projects = projects,
            timePerProject = projects.map { ProjectTime(it.id, it.name, if (it.id == 1L) 40 else 10, 12, 0) },
        )
    }
}
