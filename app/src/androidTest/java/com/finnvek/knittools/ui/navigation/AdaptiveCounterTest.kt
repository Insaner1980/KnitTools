package com.finnvek.knittools.ui.navigation

import android.graphics.Bitmap
import android.view.inspector.WindowInspector
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.RowReminderEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.ui.theme.CounterDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/** Actual Projects routes and persisted data, in the opt-in isolated package only. */
class AdaptiveCounterTest : AdaptiveRouteTestFixture() {
    private val projectName = "Adaptive cardigan"
    private val vertical =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
            !hasTestTag("navigation-rail") and !hasAnyAncestor(hasTestTag("navigation-rail"))

    @Before
    fun seed() {
        prepareResize()
        runBlocking(Dispatchers.IO) {
            app.preferencesManager.get().apply {
                setAppLanguage(AppLanguage.ENGLISH)
                setThemeMode(ThemeMode.LIGHT)
            }
            db.clearAllTables()
            db.counterProjectDao().insert(
                CounterProjectEntity(
                    id = 1,
                    name = projectName,
                    count = 20,
                    stepSize = 5,
                    targetRows = 100,
                    secondaryCount = 3,
                    secondaryCounterUsed = true,
                    stitchCount = 40,
                    stitchTrackingEnabled = true,
                    notes = "Keep this project note through resizing.",
                    notesCreated = true,
                ),
            )
            for (index in 1..12) {
                db.projectCounterDao().insert(
                    ProjectCounterEntity(
                        id = index.toLong(),
                        projectId = 1,
                        name = "Extra $index",
                        count = index,
                        sortOrder = index,
                        linkedToMainCounter = index == 1,
                    ),
                )
                db.rowReminderDao().insert(
                    RowReminderEntity(projectId = 1, targetRow = 30 + index, message = "Reminder $index"),
                )
            }
            db.sessionDao().insert(
                SessionEntity(
                    projectId = 1,
                    startedAt = 1_700_000_000_000,
                    endedAt = 1_700_000_600_000,
                    startRow = 0,
                    endRow = 20,
                    durationMinutes = 10,
                ),
            )
        }
        resize(360, 800)
        rule.waitUntil(10_000) { rule.onAllNodesWithText(projectName).fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun restore() {
        if (initialized) {
            try {
                val imeVisible =
                    ViewCompat
                        .getRootWindowInsets(rule.activity.window.decorView)
                        ?.isVisible(WindowInsetsCompat.Type.ime())
                if (imeVisible == true) {
                    Espresso.closeSoftKeyboard()
                }
            } finally {
                windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
                shell("wm size ${originalSize ?: "reset"}")
                ProPolicyTestControl.setPurchased(false)
            }
        }
    }

    @Test
    fun overviewAndCounterKeepBoundedReachableContentAcrossWindowMatrix() {
        text(projectName).performClick()
        for ((width, height) in listOf(360 to 800, 800 to 400, 800 to 1280, 1280 to 800, 1600 to 900)) {
            resize(width, height)
            runBlocking {
                app.preferencesManager.get().setThemeMode(
                    if (width >=
                        1280
                    ) {
                        ThemeMode.DARK
                    } else {
                        ThemeMode.LIGHT
                    },
                )
            }
            assertColumn(840)
            list().performScrollToIndex(0)
            text(R.string.project_overview_add_photo).assertIsDisplayed()
            if (width >= 800) screenshot("overview-$width-$height")
            scrollTo(label(R.string.project_overview_total_time))
            scrollTo(label(R.string.project_continue_content_description, projectName)).performClick()
            assertColumn(600)
            list().performScrollToIndex(0)
            val step = rule.onNodeWithTag("main-counter-step")
            step.performScrollTo().assertIsDisplayed()
            val plus = text(label(R.string.counter_step_increase, 5, label(R.string.main_counter_rows)))
            // The whole primary action group stays centered and no wider than its existing token.
            val minus = text(label(R.string.counter_step_decrease, 5, label(R.string.main_counter_rows)))
            val plusBounds = plus.fetchSemanticsNode().boundsInWindow
            val minusBounds = minus.fetchSemanticsNode().boundsInWindow
            val viewport = rule.onNodeWithTag("adaptive-content").fetchSemanticsNode().boundsInWindow
            val density = rule.activity.resources.displayMetrics.density
            // Pinottu ryhmä on enintään 360 dp (tabletilla iso laskuri samassa suhteessa); vierekkäisessä
            // asettelussa Kumoa on nappien välissä, jolloin ryhmä on kahden napin ja Kumoa-sarakkeen levyinen.
            val heroScale = (plusBounds.height / density) / CounterDimens.CounterPrimaryTouchSize.value
            val controlsCap =
                if (height < 480) {
                    CounterDimens.CounterPrimaryTouchSize.value * 2 + CounterDimens.ShortHeroUndoSize.value +
                        CounterDimens.StepRingButtonGap.value * 2
                } else {
                    CounterDimens.CounterControlsMaxWidth.value * heroScale
                }
            assertTrue(
                "Compact controls: $minusBounds / $plusBounds, cap $controlsCap dp",
                plusBounds.right - minusBounds.left <= controlsCap * density + 1,
            )
            if (height >= 480) {
                assertTrue(abs((plusBounds.right + minusBounds.left) / 2 - viewport.center.x) <= density * 2)
            } else {
                list().performScrollToIndex(0)
                assertPrimaryControlsVisible()
            }
            scrollTo(label(R.string.counter_undo_last_change))
            if (width >= 800) screenshot("counter-$width-$height")
            for (resource in listOf(
                R.string.project_content_add_pattern,
                R.string.project_content_yarn,
                R.string.project_content_notes,
                R.string.project_content_photos,
                R.string.reminders,
            )) {
                scrollTo(label(resource))
            }
            text(R.string.reminders).performClick()
            scrollTo("Reminder 12")
            Espresso.pressBack()
            scrollTo("Extra 12")
            menu()
            scrollTo(label(R.string.counters)).performClick()
            scrollTo("Extra 12")
            Espresso.pressBack()
            menu()
            scrollTo(label(R.string.work_session_start))
            Espresso.pressBack()
            text(projectName).performClick()
            assertUntouched()
        }
    }

    @Test
    fun activeWorkSessionKeepsRepeatAndTargetInPlaceAtDefaultFontScale() {
        resize(411, 780)
        assertEquals(1f, rule.activity.resources.configuration.fontScale, 0.01f)
        openCounter()
        val remainingRows = rule.activity.resources.getQuantityString(R.plurals.rows_format, 80, 80)
        val labels =
            listOf(
                label(R.string.counter_repeat_label).uppercase(),
                label(R.string.main_counter_rows),
                label(R.string.counter_target_of_format, 100),
                label(R.string.counter_target_remaining_format, remainingRows),
            )

        fun positions() =
            labels.map { value ->
                val bounds =
                    rule
                        .onNode(hasText(value), useUnmergedTree = true)
                        .assertIsDisplayed()
                        .getUnclippedBoundsInRoot()
                val viewport = list().getUnclippedBoundsInRoot()
                assertTrue(
                    "$value $bounds is fully visible within $viewport",
                    viewport.top <= bounds.top && bounds.bottom <= viewport.bottom,
                )
                bounds.top
            }

        val before = positions()
        assertTrue(before[0] < before[1])
        menu()
        scrollTo(label(R.string.work_session_start)).performClick()
        rule.waitUntil(10_000) { activeSession() != null }
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText(label(R.string.work_session_stop_short)).fetchSemanticsNodes().isNotEmpty()
        }
        assertSessionControlsVisible()
        assertEquals(before, positions())

        val tracker = rule.onNodeWithTag("counter-stitch-tracker", useUnmergedTree = true)

        fun assertTrackerBounds(allowBelowViewport: Boolean) {
            val viewport = list().getUnclippedBoundsInRoot()
            val bounds = tracker.getUnclippedBoundsInRoot()
            val fits = viewport.top <= bounds.top && bounds.bottom <= viewport.bottom
            assertTrue(
                "Stitch tracker $bounds must fit within $viewport or start below it when allowed",
                fits || (allowBelowViewport && bounds.top >= viewport.bottom),
            )
        }

        if (rule
                .onAllNodes(
                    hasTestTag("counter-stitch-tracker"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        ) {
            assertTrackerBounds(allowBelowViewport = true)
        }
        list().performScrollToNode(hasTestTag("counter-stitch-tracker"))
        tracker.assertIsDisplayed()

        resize(411, 900)
        list().performScrollToIndex(0)
        assertTrackerBounds(allowBelowViewport = false)
    }

    @Test
    fun activeWorkSessionKeepsStopAndUndoInFirstViewport() {
        openCounter()
        menu()
        scrollTo(label(R.string.work_session_start)).performClick()
        rule.waitUntil(10_000) { activeSession() != null }

        resize(360, 640)
        assertSessionControlsVisible()
        resize(640, 360)
        assertSessionControlsVisible()

        val previousFontScale = shell("settings get system font_scale").trim()
        try {
            resize(360, 640)
            shell("settings put system font_scale 2.0")
            rule.waitUntil(15_000) { rule.activity.resources.configuration.fontScale == 2f }
            assertSessionControlsVisible()
            val number = rule.onNodeWithTag("main-counter-value", useUnmergedTree = true)
            val numberBounds = number.assertIsDisplayed().getUnclippedBoundsInRoot()
            val captionBounds = text(R.string.main_counter_rows).assertIsDisplayed().getUnclippedBoundsInRoot()
            val numberHeight = numberBounds.bottom - numberBounds.top
            assertTrue("Main number height $numberHeight", numberHeight >= CounterDimens.MainNumberMinHeight)
            assertTrue(
                "Main number remains about twice the caption height: $numberBounds vs $captionBounds",
                numberHeight >= (captionBounds.bottom - captionBounds.top) * 1.9f,
            )
            screenshot("active-work-session-font-200")
        } finally {
            if (previousFontScale == "null" || previousFontScale.isBlank()) {
                shell("settings delete system font_scale")
            } else {
                shell("settings put system font_scale $previousFontScale")
            }
        }
    }

    @Test
    fun reachedAndPassedTargetKeepCompletionStopAndUndoVisible() {
        resize(411, 900)
        runBlocking(Dispatchers.IO) {
            db.counterProjectDao().updateTargetRows(1L, 20, System.currentTimeMillis())
        }
        openCounter()
        menu()
        scrollTo(label(R.string.work_session_start)).performClick()
        rule.waitUntil(10_000) { activeSession() != null }
        text(R.string.counter_target_reached).assertIsDisplayed()
        text(R.string.counter_mark_complete).assertIsDisplayed()
        assertSessionControlsVisible()

        val originalFontScale = shell("settings get system font_scale").trim()
        try {
            shell("settings put system font_scale 2.0")
            rule.waitUntil(15_000) { rule.activity.resources.configuration.fontScale == 2f }
            text(label(R.string.counter_step_increase, 5, label(R.string.main_counter_rows))).performClick()
            assertSessionControlsVisible()
            text(R.string.counter_mark_complete).assertIsDisplayed().performClick()
            text(R.string.complete_project_title).assertIsDisplayed()
        } finally {
            if (originalFontScale == "null" || originalFontScale.isBlank()) {
                shell("settings delete system font_scale")
            } else {
                shell("settings put system font_scale $originalFontScale")
            }
        }
    }

    private fun assertSessionControlsVisible() {
        val viewport = list().getUnclippedBoundsInRoot()
        val stop = text(R.string.work_session_stop_short).assertIsDisplayed().getUnclippedBoundsInRoot()
        val undo = text(R.string.counter_undo_last_change).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "Stop $stop and Undo $undo fit in order within $viewport",
            viewport.top <= stop.top && stop.bottom <= undo.top && undo.bottom <= viewport.bottom,
        )
    }

    @Test
    fun shortCounterKeepsValueAndPrimaryActionsVisibleThroughLiveResize() {
        openCounter()
        menu()
        scrollTo(label(R.string.work_session_start)).performClick()
        rule.waitUntil(10_000) { activeSession() != null }
        val session = checkNotNull(activeSession())
        for ((width, height) in listOf(
            640 to 360,
            800 to 400,
            800 to 1280,
            1280 to 800,
            1600 to 900,
            360 to 640,
            640 to 360,
        )) {
            resize(width, height)
            list().performScrollToIndex(0)
            screenshot("primary-$width-$height")
            if (height < 480) {
                assertPrimaryControlsVisible()
                text(label(R.string.counter_step_increase, 5, label(R.string.main_counter_rows)))
                    .performTouchInput { click() }
                waitCount(25)
                text(label(R.string.counter_step_decrease, 5, label(R.string.main_counter_rows)))
                    .performTouchInput { click() }
                waitCount(20)
                assertPrimaryControlsVisible()
            }
            assertEquals(session.sessionToken, activeSession()?.sessionToken)
            assertEquals(3, project().secondaryCount)
            assertEquals(1L, runBlocking { db.sessionDao().countCompletedSessions() })
        }
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertPrimaryControlsVisible()
        assertEquals(20, project().count)
        assertEquals(session.sessionToken, activeSession()?.sessionToken)
        resize(800, 1280)
        // Tabletilla iso laskuri näyttää myös Kumoa-napin, ja ruudut ovat 3 + 2 koko sisältöleveydellä.
        list().performScrollToIndex(0)
        assertPrimaryControlsVisible()
        list().performScrollToKey("project-content")
        val tile = text(R.string.project_content_add_pattern).fetchSemanticsNode().boundsInWindow
        assertTrue("Tablet tiles stay bounded: $tile", tile.width <= 180 * rule.density.density)
        screenshot("bounded-project-tiles-800-1280")
    }

    private fun assertPrimaryControlsVisible() {
        val viewport = list().getUnclippedBoundsInRoot()
        val nodes =
            listOf(
                rule.onNodeWithTag("main-counter-value", useUnmergedTree = true),
                text(label(R.string.counter_step_increase, 5, label(R.string.main_counter_rows))),
                text(label(R.string.counter_step_decrease, 5, label(R.string.main_counter_rows))),
                rule.onNodeWithTag("main-counter-step"),
                // Kumoa on laskiessa päätoiminto: se ei saa jäädä taitteen alle tai puoliksi leikatuksi.
                text(label(R.string.counter_undo_last_change)),
            )
        nodes.forEach { node ->
            node.assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue(
                "Fully visible together: $bounds in $viewport",
                bounds.top >= viewport.top && bounds.bottom <= viewport.bottom,
            )
        }
        nodes.drop(1).take(2).forEach { node ->
            val bounds = node.getUnclippedBoundsInRoot()
            with(rule.density) {
                val minimumSize = 144.dp.roundToPx()
                assertTrue(
                    "Primary targets retain their size: $bounds",
                    (bounds.right - bounds.left).roundToPx() >= minimumSize &&
                        (bounds.bottom - bounds.top).roundToPx() >= minimumSize,
                )
            }
        }
    }

    @Test
    fun longStitchValueLeavesSpaceForIncrementTouchTarget() {
        val current = 1_999_999_999
        runBlocking(Dispatchers.IO) {
            db.counterProjectDao().update(project().copy(stitchCount = Int.MAX_VALUE, currentStitch = current))
        }
        openCounter()
        val increase = scrollTo(label(R.string.counter_increase_named, label(R.string.stitch_tracker_label)))
        val bounds = increase.getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left >= CounterDimens.ExtraCounterStepperTouchSize - 1.dp)
        increase.performTouchInput { click() }
        rule.waitUntil(10_000) { project().currentStitch == current + 1 }
    }

    @Test
    fun heightAndWidthTransitionsKeepPrimaryAndSupportingActionsReachable() {
        openCounter()
        resize(800, 620)
        val initialViewport = list().getUnclippedBoundsInRoot()
        val chromeHeight = 620 - (initialViewport.bottom - initialViewport.top).value.roundToInt()
        for (contentHeight in listOf(456, 504, 456)) {
            val height = contentHeight + chromeHeight
            resize(800, height)
            list().performScrollToIndex(0)
            rule.waitUntil(15_000) {
                val bounds = list().getUnclippedBoundsInRoot()
                (contentHeight < 480) == ((bounds.bottom - bounds.top).value < 480)
            }
            val viewport = list().getUnclippedBoundsInRoot()
            assertEquals(contentHeight < 480, (viewport.bottom - viewport.top).value < 480)
            assertPrimaryControlsVisible()
            screenshot("height-boundary-$height")
            println("ADAPTIVE_COUNTER_BOUNDARY content=$viewport window=${rule.activity.resources.configuration}")
            scrollTo(label(R.string.counter_undo_last_change))
            scrollTo(label(R.string.project_content_notes))
            scrollTo("Extra 12")
            assertUntouched()
        }
        for (width in listOf(599, 600, 599)) {
            resize(width, 400)
            list().performScrollToIndex(0)
            if (width == 600) assertPrimaryControlsVisible()
            scrollTo(label(R.string.counter_step_increase, 5, label(R.string.main_counter_rows)))
                .performTouchInput { click() }
            waitCount(25)
            scrollTo(label(R.string.counter_undo_last_change)).performTouchInput { click() }
            waitCount(20)
            scrollTo("Extra 12")
            assertUntouched()
        }
    }

    @Test
    fun populatedOverviewBoundsPhotoAndRetainsEditDraftWithKeyboard() {
        text(projectName).performClick()
        text(R.string.project_overview_add_photo).assertIsDisplayed()
        val photo = File(app.cacheDir, "adaptive-overview.png")
        val bitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(95, 120, 85))
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        runBlocking(Dispatchers.IO) {
            db.progressPhotoDao().insert(
                ProgressPhotoEntity(projectId = 1, photoUri = photo.toURI().toString(), rowNumber = 20),
            )
        }
        val hero =
            SemanticsMatcher("Overview photo action") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == label(R.string.project_overview_open_photos)
            }
        rule.waitUntil(10_000) { rule.onAllNodes(hero).fetchSemanticsNodes().isNotEmpty() }
        for ((width, height) in listOf(360 to 800, 800 to 1280, 1280 to 800)) {
            resize(width, height)
            if (width == 1280) runBlocking { app.preferencesManager.get().setThemeMode(ThemeMode.DARK) }
            list().performScrollToIndex(0)
            val bounds = rule.onNode(hero).fetchSemanticsNode().boundsInWindow
            val density = rule.activity.resources.displayMetrics.density
            assertEquals(210f, bounds.height / density, 1f)
            assertTrue(bounds.width <= (840 - 32) * density + 1)
            screenshot("overview-photo-$width-$height")
        }
        scrollTo(label(R.string.project_overview_total_time))
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        text(R.string.project_overview_total_time).assertIsDisplayed()
        menu()
        text(R.string.project_overview_edit_details).performClick()
        val field = rule.onNode(hasSetTextAction() and hasContentDescription(label(R.string.project_name_label)))
        field.performScrollTo().performTextReplacement("Retained project draft")
        field.performTouchInput { click() }
        keyboard(true)
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        field.performScrollTo().assertTextContains("Retained project draft")
        field.performTouchInput { click() }
        keyboard(true)
        assertActionAboveKeyboard(R.string.save)
        text(R.string.cancel).performClick()
        assertEquals(projectName, project().name)
        assertUntouched()
    }

    @Test
    fun resizingRecreationAndRoundTripPreserveSessionCounterStepAndUndo() {
        openCounter()
        menu()
        scrollTo(label(R.string.work_session_start)).performClick()
        rule.waitUntil(10_000) { activeSession() != null }
        val started = checkNotNull(activeSession())
        scrollTo(label(R.string.counter_step_increase, 5, label(R.string.main_counter_rows))).performClick()
        waitCount(25)
        assertEquals(6, runBlocking { db.projectCounterDao().getCounter(1) }?.count)
        val history = runBlocking { db.counterProjectDao().observeCounterHistory(1).first() }
        // Anchor at the start of a row, rather than merely bringing a bottom-edge row into view.
        list().performScrollToKey(2L)
        text("Extra 2").assertIsDisplayed()
        for ((width, height) in listOf(
            599 to 900,
            600 to 900,
            601 to 900,
            839 to 900,
            840 to 900,
            841 to 900,
            800 to 400,
            800 to 1280,
            1280 to 800,
            360 to 800,
        )) {
            resize(width, height)
            println(
                "ADAPTIVE_SCROLL window=${width}x$height value=" +
                    list().fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value(),
            )
            text("Extra 2").assertIsDisplayed()
            rule.activityRule.scenario.recreate()
            rule.waitForIdle()
            text("Extra 2").assertIsDisplayed()
            assertEquals(25, project().count)
            assertEquals(5, project().stepSize)
            assertEquals(history, runBlocking { db.counterProjectDao().observeCounterHistory(1).first() })
            val active = checkNotNull(activeSession())
            assertEquals(started.sessionToken, active.sessionToken)
            assertEquals(started.startedAtWallMillis, active.startedAtWallMillis)
            assertEquals(started.startZoneId, active.startZoneId)
            assertEquals(1L, active.projectId)
            assertEquals(5, active.trustedRowsWorked + active.pendingRowsWorked)
            assertEquals(1L, runBlocking { db.sessionDao().countCompletedSessions() })
        }
        text(projectName).performClick()
        // Leave the Projects graph with a live session, then restore every tab's back stack.
        tab(TopLevelDestination.Tools)
        text(R.string.tool_cast_on_calculator).performClick()
        rule.onAllNodes(hasSetTextAction())[0].performClick().performTextReplacement("27")
        Espresso.closeSoftKeyboard()
        tab(TopLevelDestination.Library)
        text(R.string.my_yarn_title).performClick()
        tab(TopLevelDestination.Insights)
        tab(TopLevelDestination.Settings)
        runBlocking { app.preferencesManager.get().setThemeMode(ThemeMode.DARK) }
        runBlocking { app.preferencesManager.get().setAppLanguage(AppLanguage.FINNISH) }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.locales[0]
                .language == "fi"
        }
        resize(800, 1280)
        rule.activityRule.scenario.recreate()
        tab(TopLevelDestination.Tools)
        rule.onAllNodes(hasSetTextAction())[0].assertTextContains("27")
        Espresso.pressBack()
        text(R.string.tool_gauge_converter).assertIsDisplayed()
        tab(TopLevelDestination.Library)
        text(R.string.add_yarn_to_my_yarn).assertIsDisplayed()
        Espresso.pressBack()
        text(R.string.saved_patterns_title).assertIsDisplayed()
        tab(TopLevelDestination.Projects)
        assertEquals(25, project().count)
        assertEquals(started.sessionToken, activeSession()?.sessionToken)
        assertEquals(history, runBlocking { db.counterProjectDao().observeCounterHistory(1).first() })
        assertEquals(1L, runBlocking { db.sessionDao().countCompletedSessions() })
        scrollTo(label(R.string.project_continue_content_description, projectName)).performClick()
        scrollTo(label(R.string.counter_undo_last_change)).performClick()
        waitCount(20)
        assertEquals(1, runBlocking { db.projectCounterDao().getCounter(1) }?.count)
        assertEquals(started.sessionToken, activeSession()?.sessionToken)
        resize(800, 400)
        scrollTo(label(R.string.work_session_stop_short)).performClick()
        scrollTo(label(R.string.work_session_save))
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertNotNull(activeSession())
        assertEquals(1L, runBlocking { db.sessionDao().countCompletedSessions() })
        scrollTo(label(R.string.work_session_keep_running)).performClick()
        assertNotNull(activeSession())
        scrollTo(label(R.string.work_session_stop_short)).performClick()
        scrollTo(label(R.string.work_session_save)).performClick()
        rule.waitUntil(10_000) { activeSession() == null }
        assertEquals(2L, runBlocking { db.sessionDao().countCompletedSessions() })
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertNull(activeSession())
        assertEquals(2L, runBlocking { db.sessionDao().countCompletedSessions() })
    }

    @Test
    fun stepAndReminderDraftsSurviveWithKeyboardAndSaveOnlyExplicitly() {
        openCounter()
        rule.onNodeWithTag("main-counter-step").performScrollTo().performClick()
        val input = rule.onNodeWithTag("main-step-input")
        input.performScrollTo().performClick().performTextReplacement("12")
        keyboard(true)
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        input.performScrollTo().assertTextContains("12")
        input.performClick()
        keyboard(true)
        assertActionAboveKeyboard(R.string.save)
        assertEquals(5, project().stepSize)
        assertEquals(20, project().count)
        text(R.string.save).performClick()
        rule.waitUntil(10_000) { project().stepSize == 12 }
        assertUntouched(expectedStep = 12)
        scrollTo(label(R.string.reminders)).performClick()
        text(R.string.project_overview_add).performClick()
        val message = rule.onAllNodes(hasSetTextAction()).onLast()
        message.performScrollTo().performTextReplacement("Retained reminder draft")
        message.performClick()
        keyboard(true)
        resize(800, 1280)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction() and hasText("Retained reminder draft")).performScrollTo().assertIsDisplayed()
        Espresso.closeSoftKeyboard()
        scrollTo(label(R.string.cancel)).performClick()
        assertEquals(
            12,
            runBlocking {
                db
                    .rowReminderDao()
                    .getRemindersForProject(1)
                    .first()
                    .size
            },
        )
        assertUntouched(expectedStep = 12)
    }

    @Test
    fun foldingFeaturesKeepCounterSheetAndDialogInOnePaneWithoutMutations() {
        resize(1280, 800)
        openCounter()
        val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
        for (orientation in listOf(FoldingFeature.Orientation.VERTICAL, FoldingFeature.Orientation.HORIZONTAL)) {
            val fold = showFold(bounds, orientation)
            rule.onNodeWithTag("main-counter-step").performScrollTo().assertIsDisplayed()
            val content = rule.onNodeWithTag("adaptive-content").fetchSemanticsNode().boundsInWindow
            assertTrue(
                "Content avoids $fold: $content",
                if (orientation ==
                    FoldingFeature.Orientation.VERTICAL
                ) {
                    content.right <= fold.bounds.left || content.left >= fold.bounds.right
                } else {
                    content.bottom <= fold.bounds.top || content.top >= fold.bounds.bottom
                },
            )
            menu()
            scrollTo(label(R.string.work_session_start))
            assertModalOutsideFold(fold)
            screenshot("fold-actions-$orientation")
            Espresso.pressBack()
            rule.onNodeWithTag("main-counter-step").performScrollTo().performClick()
            val input = rule.onNodeWithTag("main-step-input")
            input.performScrollTo().performTextReplacement("17")
            input.performClick()
            keyboard(true)
            assertActionAboveKeyboard(R.string.save)
            assertModalOutsideFold(fold)
            screenshot("fold-keyboard-$orientation")
            Espresso.closeSoftKeyboard()
            scrollTo(label(R.string.cancel))
            assertModalOutsideFold(fold)
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
            rule.waitForIdle()
            input.performScrollTo().assertTextContains("17")
            text(R.string.cancel).performScrollTo().performClick()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
            menu()
            scrollTo(label(R.string.add_counter)).performClick()
            // Jaetun kentän nimike on kentän contentDescription, ei sen teksti.
            val name = rule.onNode(hasSetTextAction() and hasContentDescription(label(R.string.counter_name)))
            name.performScrollTo().performTextReplacement("Extra draft")
            Espresso.closeSoftKeyboard()
            rule.activityRule.scenario.recreate()
            rule.waitForIdle()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
            name.performScrollTo().assertTextContains("Extra draft")
            scrollTo(label(R.string.cancel))
            assertModalOutsideFold(fold)
            screenshot("fold-dialog-$orientation")
            text(R.string.cancel).performClick()
            assertEquals(
                12,
                runBlocking {
                    db
                        .projectCounterDao()
                        .getCountersForProject(1)
                        .first()
                        .size
                },
            )
            assertUntouched()
            text(projectName).performClick()
            menu()
            text(R.string.project_overview_edit_details).assertIsDisplayed()
            assertModalOutsideFold(fold)
            Espresso.pressBack()
            scrollTo(label(R.string.project_continue_content_description, projectName)).performClick()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
        }
    }

    private fun assertModalOutsideFold(fold: FoldingFeature) {
        var measured = ""
        try {
            rule.awaitWindowOutsideFold(fold) { measured = it }
        } finally {
            println("ADAPTIVE_MODAL $measured")
            screenshot("modal-${fold.orientation}")
        }
    }

    private fun assertActionAboveKeyboard(resource: Int) {
        scrollTo(label(resource))
        rule.waitUntil(10_000) {
            val view = WindowInspector.getGlobalWindowViews().last { it.isAttachedToWindow && it.hasWindowFocus() }
            val insets = ViewCompat.getRootWindowInsets(view) ?: return@waitUntil false
            val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
            text(resource).fetchSemanticsNode().boundsInWindow.bottom <=
                bounds.height() - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom + 1
        }
        text(resource).assertIsDisplayed()
        text(R.string.cancel).assertIsDisplayed()
    }

    private fun keyboard(visible: Boolean) = rule.awaitKeyboard(visible)

    private fun assertColumn(cap: Int) {
        val bounds = list().fetchSemanticsNode().boundsInWindow
        val viewport = rule.onNodeWithTag("adaptive-content").fetchSemanticsNode().boundsInWindow
        val density = rule.activity.resources.displayMetrics.density
        assertTrue("Column $bounds <= $cap dp", bounds.width <= cap * density + 1)
        assertTrue("Centered $bounds / $viewport", abs(bounds.center.x - viewport.center.x) <= density * 2)
        println(
            "ADAPTIVE_COUNTER window=${rule.activity.resources.configuration.screenWidthDp}x${rule.activity.resources.configuration.screenHeightDp} cap=$cap bounds=$bounds density=$density",
        )
    }

    private fun assertUntouched(expectedStep: Int = 5) {
        assertEquals(20, project().count)
        assertEquals(expectedStep, project().stepSize)
        assertEquals(3, project().secondaryCount)
        assertTrue(
            runBlocking {
                db
                    .counterProjectDao()
                    .observeCounterHistory(1)
                    .first()
                    .isEmpty()
            },
        )
        assertNull(activeSession())
        assertEquals(1L, runBlocking { db.sessionDao().countCompletedSessions() })
    }

    private fun project() = checkNotNull(runBlocking(Dispatchers.IO) { db.counterProjectDao().getProject(1) })

    private fun activeSession() = runBlocking(Dispatchers.IO) { db.sessionDao().getActiveSession() }

    private fun waitCount(count: Int) = rule.waitUntil(10_000) { project().count == count }

    private fun openCounter() {
        text(label(R.string.project_continue_content_description, projectName)).performClick()
        rule.waitUntil(10_000) { rule.onAllNodes(hasTestTag("main-counter-step")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun menu() = text(R.string.project_actions_title).performClick()

    private fun tab(destination: TopLevelDestination) {
        rule
            .onNode(
                isSelectable() and hasAnyDescendant(hasText(label(destination.labelRes).uppercase())),
                useUnmergedTree = true,
            ).assertIsDisplayed()
            .performClick()
    }

    private fun label(
        resource: Int,
        vararg args: Any,
    ) = rule.activity.getString(resource, *args)

    private fun text(resource: Int) = text(label(resource))

    private fun list() = rule.onAllNodes(vertical).onLast()

    private fun scrollTo(value: String) = rule.scrollToText(value, ::list)

    private fun resize(
        width: Int,
        height: Int,
    ) = rule.resizeWindow(width, height)

    private fun screenshot(name: String) {
        captureScreenshot(app, "adaptive-counter", name)
    }
}
