package com.finnvek.knittools.ui.navigation

import android.graphics.Bitmap
import android.view.inspector.WindowInspector
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
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
import androidx.compose.ui.test.performTextReplacement
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
            Espresso.closeSoftKeyboard()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
            shell("wm size ${originalSize ?: "reset"}")
            ProPolicyTestControl.setPurchased(false)
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
            assertTrue(
                "Compact controls: $minusBounds / $plusBounds",
                plusBounds.right - minusBounds.left <= 360 * density + 1,
            )
            assertTrue(abs((plusBounds.right + minusBounds.left) / 2 - viewport.center.x) <= density * 2)
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
        val field = rule.onNode(hasSetTextAction() and hasText(label(R.string.project_name_label)))
        field.performScrollTo().performClick().performTextReplacement("Retained project draft")
        keyboard(true)
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        field.performScrollTo().assertTextContains("Retained project draft")
        field.performClick()
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
        list().performScrollToIndex(6)
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
        scrollTo(label(R.string.work_session_stop)).performClick()
        scrollTo(label(R.string.work_session_save))
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertNotNull(activeSession())
        assertEquals(1L, runBlocking { db.sessionDao().countCompletedSessions() })
        scrollTo(label(R.string.cancel)).performClick()
        assertNotNull(activeSession())
        scrollTo(label(R.string.work_session_stop)).performClick()
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
                    content.right <= fold.bounds.left
                } else {
                    content.bottom <=
                        fold.bounds.top
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
            val name = rule.onNode(hasSetTextAction() and hasText(label(R.string.counter_name)))
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
            rule.waitUntil(10_000) {
                focusedWindowOutside(fold) { view, position ->
                    measured = "origin=${position.toList()} size=${view.width}x${view.height} fold=${fold.bounds}"
                }
            }
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
