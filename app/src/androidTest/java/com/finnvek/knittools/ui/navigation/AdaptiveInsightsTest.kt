package com.finnvek.knittools.ui.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProjectCompletionEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.ui.screens.insights.currentInsightsFirstDayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Runs only against an isolated local database; window overrides are restored even on failure. */
class AdaptiveInsightsTest : AdaptiveRouteTestFixture() {
    private val matrix = listOf(360 to 800, 800 to 400, 800 to 1280, 1280 to 800)
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)
    private val sessionDates = listOf(0L, 1L, 14L, 90L, 700L).map(today::minusDays)

    @Before
    fun seed() {
        prepareResize()
        runBlocking(Dispatchers.IO) {
            setAdaptivePreferences()
            db.clearAllTables()
            for (id in 1L..25L) {
                db.counterProjectDao().insert(CounterProjectEntity(id = id, name = name(id)))
                for (date in sessionDates) {
                    val start =
                        date
                            .atTime(10, 0)
                            .atZone(zone)
                            .toInstant()
                            .toEpochMilli()
                    db.sessionDao().insert(
                        SessionEntity(
                            projectId = id,
                            startedAt = start,
                            endedAt = start + 600_000L,
                            startRow = 0,
                            endRow = 12,
                            durationMinutes = 10,
                            zoneId = zone.id,
                        ),
                    )
                }
            }
            for (id in 1L..5L) {
                db.counterProjectDao().insertCompletion(
                    ProjectCompletionEntity(
                        id = id,
                        projectId = id,
                        completedAt =
                            today
                                .minusDays(id)
                                .atStartOfDay(zone)
                                .toInstant()
                                .toEpochMilli(),
                        zoneId = zone.id,
                    ),
                )
            }
        }
    }

    @After
    fun cleanup() {
        if (!initialized) return
        shell("wm size ${originalSize ?: "reset"}")
        ProPolicyTestControl.setPurchased(false)
    }

    @Test
    fun rangesFiltersSelectionAndScrollSurviveWindowsAndRecreationWithoutWrites() {
        val before = storedData()
        resize(1280, 800)
        openInsights()
        for (range in listOf(R.string.insights_this_week, R.string.insights_this_month, R.string.insights_all_time)) {
            chooseRange(range)
            val start =
                when (range) {
                    R.string.insights_this_week -> {
                        today.with(TemporalAdjusters.previousOrSame(currentInsightsFirstDayOfWeek()))
                    }

                    R.string.insights_this_month -> {
                        today.withDayOfMonth(1)
                    }

                    else -> {
                        sessionDates.last()
                    }
                }
            val expectedDays = sessionDates.count { !it.isBefore(start) }
            for ((width, height) in matrix) {
                resize(width, height)
                assertRangeTotal(expectedDays * 250)
                val readout =
                    if (expectedDays >= 2) {
                        list().performScrollToKey("chart-heading")
                        selectChart()
                        heading()
                    } else {
                        assertSparseChartAbsent()
                        null
                    }
                rule.activityRule.scenario.recreate()
                waitLoaded()
                if (readout != null) {
                    assertEquals(readout, heading())
                    assertTrue(
                        "Chart must fit current content",
                        plot().fetchSemanticsNode().boundsInRoot.width <= width * rule.density.density,
                    )
                } else {
                    assertRangeTotal(expectedDays * 250)
                    assertSparseChartAbsent()
                }
                screenshot("range-$range-$width-$height")
            }
        }
        resize(1280, 800)
        list().performScrollToKey("hero")
        assertTrue(
            rule
                .onNodeWithTag("insights-hero")
                .fetchSemanticsNode()
                .boundsInRoot.width <= 600 * rule.density.density,
        )
        list().performScrollToKey("chart")
        assertTrue(
            "Chart uses the wider policy",
            plot().fetchSemanticsNode().boundsInRoot.width > 840 * rule.density.density,
        )
        list().performScrollToKey("project-15")
        rule.onAllNodesWithText(name(15)).onLast().assertIsDisplayed()
        for ((width, height) in matrix) {
            resize(width, height)
            assertProjectVisibleAfterLayout(15)
            rule.activityRule.scenario.recreate()
            waitLoaded()
            assertProjectVisibleAfterLayout(15)
        }
        list().performScrollToKey("context")
        rule
            .onNode(hasText(app.getString(R.string.all_projects)) and hasAnyAncestor(hasTestTag("insights-context")))
            .performClick()
        rule
            .onNode(hasText(name(1)) and hasAnyAncestor(isPopup()))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        waitLoaded()
        list().performScrollToKey("context")
        screenshot("selected-before-resize")
        rule.waitUntil(15_000) {
            rule
                .onAllNodes(hasText(name(1)) and hasAnyAncestor(hasTestTag("insights-context")))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        chooseRange(R.string.insights_this_month)
        for ((width, height) in matrix) {
            resize(width, height)
            rule.activityRule.scenario.recreate()
            waitLoaded()
            list().performScrollToKey("context")
            screenshot("filtered-$width-$height")
            rule.onNode(hasText(name(1)) and hasAnyAncestor(hasTestTag("insights-context"))).assertIsDisplayed()
            rule.onAllNodesWithText(app.getString(R.string.insights_this_month)).onLast().assertIsDisplayed()
            list().performScrollToKey("session-history")
            rule.onAllNodesWithText(app.getString(R.string.session_history_title)).onLast().assertIsDisplayed()
        }
        assertEquals(before, storedData())
    }

    @Test
    fun longLocalizedTextBothThemesAndFoldedMenusAndCompletionSheetStayReachable() {
        resize(1280, 800)
        openInsights()
        runBlocking { app.preferencesManager.get().setAppLanguage(AppLanguage.GERMAN) }
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            runBlocking { app.preferencesManager.get().setThemeMode(theme) }
            for ((width, height) in matrix) {
                resize(width, height)
                waitLoaded()
                list().performScrollToKey("context")
                screenshot("german-$theme-$width-$height")
                list().performScrollToKey("project-25")
                rule.onAllNodesWithText(name(25)).onLast().assertIsDisplayed()
            }
        }
        resize(1280, 800)
        for (orientation in listOf(FoldingFeature.Orientation.VERTICAL, FoldingFeature.Orientation.HORIZONTAL)) {
            val fold =
                FoldingFeature(
                    rule.activity,
                    orientation = orientation,
                    state = FoldingFeature.State.HALF_OPENED,
                    size = 24,
                )
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
            list().performScrollToKey("context")
            rule.onAllNodesWithText(app.getString(R.string.insights_all_time)).onLast().performClick()
            rule.onNode(isPopup()).assertIsDisplayed()
            assertFocusedWindowOutside(fold)
            back()
            rule.onAllNodesWithText(app.getString(R.string.all_projects)).onLast().performClick()
            assertFocusedWindowOutside(fold)
            back()
            list().performScrollToKey("completion_events")
            rule
                .onAllNodesWithText(app.getString(R.string.show_more))
                .onLast()
                .performScrollTo()
                .performClick()
            assertFocusedWindowOutside(fold)
            rule.onNodeWithTag("completion_history").performScrollToNode(hasText(name(5)))
            rule.onNode(hasText(name(5)) and hasAnyAncestor(hasTestTag("completion_history"))).assertIsDisplayed()
            back()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
        }
    }

    private fun name(id: Long) = "Project $id \u2014 Besonders lange lokale Projektbezeichnung"

    private fun list() = rule.onNodeWithTag("insights-list")

    private fun plot() = rule.onNodeWithTag("insights-chart-plot", useUnmergedTree = true)

    private fun openInsights() {
        rule
            .onNode(
                isSelectable() and
                    hasAnyDescendant(hasText(app.getString(TopLevelDestination.Insights.labelRes).uppercase())),
                useUnmergedTree = true,
            ).performClick()
        waitLoaded()
    }

    private fun waitLoaded() =
        rule.waitUntil(15_000) {
            rule.onAllNodes(hasTestTag("insights-list")).fetchSemanticsNodes().isNotEmpty()
        }

    private fun chooseRange(resource: Int) {
        list().performScrollToKey("context")
        rule
            .onNode(
                SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.Expand) and
                    !hasAnyAncestor(hasTestTag("insights-context")),
            ).performClick()
        rule.onAllNodesWithText(app.getString(resource)).onLast().performClick()
        waitLoaded()
    }

    private fun selectChart() {
        plot().performTouchInput { click(Offset(width * 0.75f, height / 2f)) }
        val rightReadout = heading()
        plot().performTouchInput { click(Offset(width * 0.25f, height / 2f)) }
        assertTrue("Resized touch coordinates select distinct buckets", rightReadout != heading())
    }

    private fun heading() = rule.insightsHeading()

    private fun assertProjectVisibleAfterLayout(id: Long) {
        val row = rule.onAllNodesWithText(name(id)).onLast()
        // Wait for resized or recreated content without moving the list.
        rule.waitUntil(15_000) { row.isDisplayed() }
        row.assertIsDisplayed()
    }

    private fun assertRangeTotal(minutes: Int) {
        list().performScrollToKey("hero")
        val expected = listOf(minutes / 60, minutes % 60).filter { it != 0 }.map(Int::toString)
        rule.waitUntil(15_000) {
            rule
                .onAllNodes(hasAnyAncestor(hasTestTag("insights-hero")), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
                .map { it.text }
                .filter { it.toIntOrNull() != null } == expected
        }
    }

    private fun assertSparseChartAbsent() {
        // Check the lazy key as well as semantics; an offscreen chart is not an absent chart.
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                list().performScrollToKey("chart-heading")
            }
        assertTrue(failure.message.orEmpty().contains("couldn't find the key"))
        rule.onNodeWithTag("insights-chart-heading").assertDoesNotExist()
        plot().assertDoesNotExist()
    }

    private fun resize(
        width: Int,
        height: Int,
    ) {
        val density = app.resources.displayMetrics.density
        rule.resizeWindow(width, height, density)
        println(
            "ADAPTIVE_INSIGHTS window=${width}x$height density=$density " +
                "fontScale=${app.resources.configuration.fontScale}",
        )
    }

    private fun assertFocusedWindowOutside(fold: FoldingFeature) {
        rule.waitUntil(10_000) { focusedWindowOutside(fold) }
        screenshot("fold-${fold.orientation}")
    }

    private fun storedData(): List<String> =
        runBlocking(Dispatchers.IO) {
            listOf("counter_projects", "sessions", "project_completions", "active_sessions").flatMap { table ->
                db.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY 1").use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                (0 until cursor.columnCount).joinToString { cursor.getString(it).orEmpty() },
                            )
                        }
                    }
                }
            }
        }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        captureScreenshot(app, "adaptive-insights", name)
    }

    private fun back() {
        shell("input keyevent 4")
        rule.waitForIdle()
    }
}
