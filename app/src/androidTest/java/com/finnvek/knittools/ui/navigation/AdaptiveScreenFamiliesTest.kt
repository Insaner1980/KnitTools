package com.finnvek.knittools.ui.navigation

import android.os.SystemClock
import android.view.inspector.WindowInspector
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextRange
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
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProjectFolderAssignmentEntity
import com.finnvek.knittools.data.local.ProjectFolderEntity
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.calculator.SizeChartData
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.pro.ProPolicyTestControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/** Real routes and Room fixtures. Display overrides are opt-in and restored after every test. */
class AdaptiveScreenFamiliesTest : AdaptiveRouteTestFixture() {
    private val vertical =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
            !hasTestTag("navigation-rail") and
            !hasAnyAncestor(hasTestTag("navigation-rail"))

    @Before
    fun seedIsolatedPackage() {
        prepareResize()
        runBlocking(Dispatchers.IO) {
            app.preferencesManager.get().apply {
                setAppLanguage(AppLanguage.ENGLISH)
                setThemeMode(ThemeMode.LIGHT)
                setProjectSortOrder(ProjectSortOrder.UPDATED)
                if (!preferences.first().showCompletedProjects) toggleShowCompletedProjects()
            }
            val db = app.database.get()
            db.clearAllTables()
            db.projectFolderDao().insert(
                ProjectFolderEntity(
                    id = 1,
                    name = "Adaptive folder",
                    normalizedName = "adaptive folder",
                    sortOrder = 0,
                ),
            )
            for (i in 1..24) {
                val suffix = i.toString().padStart(2, '0')
                db.counterProjectDao().insert(
                    CounterProjectEntity(
                        id = i.toLong(),
                        name = "Adaptive project $suffix",
                        count = i,
                        notes = "Original notes",
                        notesCreated = true,
                        updatedAt = 10_000L - i,
                    ),
                )
                db.projectFolderDao().insertOrReplaceAssignment(ProjectFolderAssignmentEntity(i.toLong(), 1))
                db.yarnCardDao().upsert(
                    YarnCardEntity(id = i.toLong(), yarnName = "Adaptive yarn $suffix", createdAt = 10_000L - i),
                )
                db.savedPatternDao().insert(
                    SavedPatternEntity(
                        id = i.toLong(),
                        source = "WEB_LINK",
                        name = "Adaptive pattern $suffix",
                        designerName = "Local fixture",
                        originalUrl = "https://example.com/pattern/$i",
                        canonicalUrl = "https://example.com/pattern/$i",
                        savedAt = 10_000L - i,
                    ),
                )
                db.sessionDao().insert(
                    SessionEntity(
                        projectId = 1,
                        startedAt = 1_700_000_000_000L + i * 3_600_000,
                        endedAt = 1_700_000_600_000L + i * 3_600_000,
                        startRow = i,
                        endRow = i + 2,
                        durationMinutes = 10,
                    ),
                )
                db.counterProjectDao().insertHistory(
                    CounterHistoryEntity(projectId = 1, action = "increment", previousValue = i, newValue = i + 1),
                )
            }
            db.counterProjectDao().insert(
                CounterProjectEntity(
                    id = 25,
                    name = "Adaptive finished",
                    isCompleted = true,
                    completedAt = 1_700_000_000_000L,
                ),
            )
            db.counterProjectDao().insertHistory(
                CounterHistoryEntity(projectId = 25, action = "increment", previousValue = 1, newValue = 2),
            )
        }
        resize(360, 800)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Adaptive project 01").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun restoreDisplay() {
        if (initialized) {
            Espresso.closeSoftKeyboard()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
            shell("wm size ${originalSize ?: "reset"}")
            ProPolicyTestControl.setPurchased(false)
        }
    }

    @Test
    fun screenFamiliesUseTheirWidthsAndScrollInBothThemes() {
        for ((width, height) in listOf(360 to 800, 700 to 900, 800 to 1280, 1280 to 800, 800 to 400, 1600 to 900)) {
            resize(width, height)
            if (width == 1600) runBlocking { app.preferencesManager.get().setThemeMode(ThemeMode.DARK) }
            tab(TopLevelDestination.Projects)
            assertColumn("projects", 840)
            text(R.string.new_project).assertIsDisplayed()
            if (width >= 1280) screenshot("projects-$width")

            tab(TopLevelDestination.Library)
            assertColumn("library", 840)
            click(R.string.my_yarn_title)
            assertColumn("yarn-list", 840)
            scrollTo("Adaptive yarn 24")
            scrollTo("Adaptive yarn 01").performClick()
            assertColumn("yarn-detail", 840)
            if (width >= 1280) screenshot("yarn-$width")
            back()
            back()
            click(R.string.saved_patterns_title)
            assertColumn("pattern-list", 840)
            scrollTo("Adaptive pattern 24")
            scrollTo("Adaptive pattern 01").performClick()
            assertColumn("pattern-detail", 840)
            text(R.string.web_pattern_open_website).performScrollTo().assertIsDisplayed()
            back()
            back()

            for ((resource, cap) in listOf(
                R.string.tool_needle_sizes to 840,
                R.string.size_charts_title to 1200,
                R.string.abbreviations_title to 840,
                R.string.chart_symbols_title to 840,
            )) {
                click(resource)
                assertColumn("reference-$resource", cap, table = resource == R.string.size_charts_title)
                if (resource == R.string.size_charts_title && width >= 1280) screenshot("table-$width")
                if (resource == R.string.size_charts_title) {
                    val lastSize =
                        SizeChartData
                            .entries(SizeChartData.Category.WOMEN)
                            .last()
                            .sizeLabel
                            .resolve(app)
                    rule.onAllNodes(vertical)[0].performScrollToIndex(1)
                    repeat(3) {
                        if (!text(lastSize).isDisplayed()) rule.onAllNodes(vertical)[0].performTouchInput { swipeUp() }
                    }
                    text(lastSize).assertIsDisplayed()
                }
                back()
            }
            tab(TopLevelDestination.Tools)
            assertColumn("tools", 600)
            for (resource in listOf(
                R.string.tool_gauge_converter,
                R.string.tool_increase_decrease,
                R.string.tool_cast_on_calculator,
                R.string.tool_yarn_estimator,
            )) {
                click(resource)
                assertColumn("calculator-$resource", 600)
                back()
            }
            tab(TopLevelDestination.Settings)
            assertColumn("settings", 600)
            if (width >= 1280) screenshot("settings-$width")
            click(R.string.backup_title)
            assertColumn("backup", 600)
            text(R.string.backup_restore).performScrollTo().assertIsDisplayed()
            back()
            click(R.string.knittools_pro)
            assertColumn("pro", 600)
            back()
        }
    }

    @Test
    fun projectYarnAndWebPatternFormsAvoidSeparatingHinges() {
        resize(1280, 800)
        for (orientation in listOf(FoldingFeature.Orientation.VERTICAL, FoldingFeature.Orientation.HORIZONTAL)) {
            val fold = FoldingFeature(rule.activity, size = 24, orientation = orientation)
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
            tab(TopLevelDestination.Projects)
            click(R.string.new_project)
            assertFocusedWindowOutside(fold)
            text(R.string.cancel).performScrollTo().performClick()
            tab(TopLevelDestination.Library)
            click(R.string.my_yarn_title)
            click(R.string.add_yarn_to_my_yarn)
            assertFocusedWindowOutside(fold)
            text(R.string.cancel).performScrollTo().performClick()
            back()
            click(R.string.saved_patterns_title)
            click(R.string.web_pattern_add)
            assertFocusedWindowOutside(fold)
            text(R.string.cancel).performScrollTo().performClick()
            back()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
        }
    }

    private fun assertFocusedWindowOutside(fold: FoldingFeature) {
        rule.waitUntil(10_000) { focusedWindowOutside(fold) }
    }

    @Test
    fun listSelectionFolderAndVisibleItemsSurviveResizeAndRecreation() {
        tab(TopLevelDestination.Projects)
        rule
            .onNodeWithContentDescription(
                app.getString(R.string.folder_selected_description, label(R.string.folder_all_projects_description)),
            ).performClick()
        text("Adaptive folder").performClick()
        scrollTo("Adaptive project 18").performTouchInput { longClick() }
        resize(1280, 800)
        rule.activityRule.scenario.recreate()
        text("Adaptive project 18").assertIsDisplayed()
        rule.onNode(hasText("Adaptive project 18") and isSelectable()).assertIsSelected()
        Espresso.pressBack()
        rule
            .onNodeWithContentDescription(
                app.getString(
                    R.string.folder_selected_description,
                    app.getString(R.string.folder_user_description, "Adaptive folder"),
                ),
            ).assertIsDisplayed()

        tab(TopLevelDestination.Library)
        for ((resource, item) in listOf(
            R.string.my_yarn_title to "Adaptive yarn 18",
            R.string.saved_patterns_title to "Adaptive pattern 18",
        )) {
            click(resource)
            rule.onAllNodes(vertical)[0].performScrollToIndex(18)
            scrollTo(item).performTouchInput { longClick() }
            resize(700, 900)
            rule.activityRule.scenario.recreate()
            text(item).assertIsDisplayed()
            rule.onNode(hasText(item) and isSelectable()).assertIsSelected()
            click(R.string.delete)
            text(R.string.cancel).assertIsDisplayed().performClick()
            resize(360, 800)
            text(item).assertIsDisplayed()
            Espresso.pressBack()
            back()
        }
    }

    @Test
    fun formDraftsAndActionsSurviveShortWindowKeyboardAndRecreation() {
        click(R.string.new_project)
        checkFormDraft("Project draft", R.string.create_project)
        text(R.string.cancel).performClick()
        tab(TopLevelDestination.Library)
        click(R.string.my_yarn_title)
        click(R.string.add_yarn_to_my_yarn)
        checkFormDraft("Yarn draft", R.string.save)
        text(R.string.cancel).performClick()
        back()
        click(R.string.saved_patterns_title)
        click(R.string.web_pattern_add)
        checkFormDraft("Pattern draft", R.string.save)
        text(R.string.cancel).performClick()
    }

    @Test
    fun notesHistoryAndReferenceStateSurviveRecreation() {
        rule
            .onNodeWithContentDescription(
                app.getString(R.string.project_continue_content_description, "Adaptive project 01"),
            ).performClick()
        rule.onNodeWithContentDescription(label(R.string.counter_add_row)).assertExists()
        back()
        text("Adaptive project 01").performClick()
        scrollTo("Original notes").performClick()
        val draft = (1..60).joinToString("\n") { "Draft line $it" }
        rule.onNode(hasSetTextAction()).performTextReplacement(draft)
        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.SetSelection) { it(20, 30, false) }
        rule.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(20, 30)),
        )
        resize(1280, 800)
        rule.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(20, 30)),
        )
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 1 }
        rule
            .onNode(hasSetTextAction())
            .assertTextContains(draft)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(20, 30)))
        Espresso.closeSoftKeyboard()
        assertColumn("notes", 600, content = hasSetTextAction())
        back()
        click(R.string.session_history_title)
        assertColumn("session-history", 840)
        val visibleSession = app.getString(R.string.session_row_range, 12, 14)
        rule.onAllNodes(vertical)[0].performScrollToIndex(13)
        scrollTo(visibleSession)
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        assertColumn("session-history-short", 840)
        text(visibleSession).assertIsDisplayed()
        back()
        back()
        scrollTo("Adaptive finished").performClick()
        click(R.string.counter_history_title)
        assertColumn("counter-history-short", 840)
        resize(1280, 800)
        rule.activityRule.scenario.recreate()
        assertColumn("counter-history", 840)
        back()
        back()
        tab(TopLevelDestination.Library)
        click(R.string.tool_needle_sizes)
        rule.onNode(hasSetTextAction()).performTextReplacement("4.5")
        Espresso.closeSoftKeyboard()
        rule.onNode(hasText("4.5") and isSelectable()).performClick()
        resize(700, 900)
        rule.activityRule.scenario.recreate()
        rule.onNode(hasSetTextAction()).assertTextContains("4.5")
        rule.onNode(hasText("4.5") and isSelectable()).assertIsSelected()
        back()
        click(R.string.size_charts_title)
        text(R.string.category_men).performClick()
        resize(360, 800)
        rule.activityRule.scenario.recreate()
        rule.onNode(hasText(label(R.string.category_men)) and isSelectable()).assertIsSelected()
    }

    @Test
    fun emptyCollectionsKeepCreationReachableInShortWindows() {
        runBlocking(Dispatchers.IO) { app.database.get().clearAllTables() }
        resize(800, 400)
        tab(TopLevelDestination.Library)
        click(R.string.my_yarn_title)
        text(R.string.add_yarn_to_my_yarn).performScrollTo().assertIsDisplayed()
        back()
        click(R.string.saved_patterns_title)
        text(R.string.web_pattern_add).performScrollTo().assertIsDisplayed()
    }

    private fun checkFormDraft(
        draft: String,
        confirm: Int,
    ) {
        val initialField = rule.onAllNodes(hasSetTextAction())[0]
        initialField.performScrollTo().performTextReplacement(draft)
        initialField.performTouchInput { click() }
        waitForKeyboard(visible = true)
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        Espresso.closeSoftKeyboard()
        waitForKeyboard(visible = false)
        val field = rule.onAllNodes(hasSetTextAction())[0]
        field.assertTextContains(draft).performScrollTo().performTouchInput { click() }
        try {
            waitForKeyboard(visible = true)
            field.performScrollTo().assertIsDisplayed()
            val bounds = field.fetchSemanticsNode().boundsInWindow
            val window = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
            val imeBottom = checkNotNull(visibleImeInsets()).getInsets(WindowInsetsCompat.Type.ime()).bottom
            assertTrue(
                "Editable field remains readable",
                bounds.height >= 48 * rule.activity.resources.displayMetrics.density,
            )
            assertTrue("Editable field above keyboard", bounds.bottom <= window.height() - imeBottom + 1)
        } finally {
            screenshot("field-${draft.substringBefore(' ')}")
        }
        text(confirm).performScrollTo()
        try {
            rule.waitUntil(10_000) {
                val insets = visibleImeInsets()
                val window = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
                insets != null &&
                    text(confirm).isDisplayed() &&
                    text(confirm).fetchSemanticsNode().boundsInWindow.bottom <=
                    window.height() - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom + 1
            }
        } finally {
            screenshot("form-${draft.substringBefore(' ')}")
        }
        text(confirm).assertIsDisplayed()
        text(R.string.cancel).assertIsDisplayed()
        val window = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
        val ime = checkNotNull(visibleImeInsets())
        val keyboardTop = window.height() - ime.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val button = text(confirm).fetchSemanticsNode().boundsInWindow
        assertTrue("Action above keyboard: $button / $keyboardTop", button.bottom <= keyboardTop + 1)
        Espresso.closeSoftKeyboard()
        resize(360, 800)
    }

    private fun assertColumn(
        name: String,
        cap: Int,
        table: Boolean = false,
        content: SemanticsMatcher = vertical,
    ) {
        rule.waitForIdle()
        val bounds =
            rule
                .onAllNodes(content)
                .fetchSemanticsNodes()
                .maxBy { it.boundsInWindow.width }
                .boundsInWindow
        val density = rule.activity.resources.displayMetrics.density
        val viewport = rule.onNodeWithTag("adaptive-content").fetchSemanticsNode().boundsInWindow
        val maximum = minOf(cap * density, viewport.width)
        if (!table) {
            assertTrue("$name width $bounds <= $maximum", bounds.width <= maximum + 1)
            assertTrue("$name centered in $viewport: $bounds", abs(bounds.center.x - viewport.center.x) <= density * 2)
            assertTrue("$name uses its policy: $bounds / $maximum", bounds.width >= maximum - 64 * density)
        } else {
            val horizontal =
                rule
                    .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
                    .fetchSemanticsNode()
                    .boundsInWindow
            assertTrue("Table viewport $horizontal / $maximum", horizontal.width <= maximum + 1)
            assertTrue("Table can use wide space", horizontal.width >= maximum - 64 * density)
        }
        assertTrue("$name has usable height: $bounds", bounds.height > 0)
        println(
            "ADAPTIVE_SCREEN $name window=${rule.activity.resources.configuration.screenWidthDp}x${rule.activity.resources.configuration.screenHeightDp} bounds=$bounds density=$density",
        )
    }

    private fun resize(
        width: Int,
        height: Int,
    ) = rule.resizeWindow(width, height)

    private fun tab(destination: TopLevelDestination) {
        rule
            .onNode(
                isSelectable() and hasAnyDescendant(hasText(label(destination.labelRes).uppercase())),
                useUnmergedTree = true,
            ).performClick()
    }

    private fun scrollTo(value: String) =
        text(value).also {
            if (rule.onAllNodes(hasText(value) or hasContentDescription(value)).fetchSemanticsNodes().isEmpty()) {
                rule.onAllNodes(vertical)[0].performScrollToNode(hasText(value))
            } else {
                runCatching { it.performScrollTo() }
            }
            it.assertIsDisplayed()
        }

    private fun click(resource: Int) = scrollTo(label(resource)).performClick()

    private fun back() {
        Espresso.closeSoftKeyboard()
        rule.onNodeWithContentDescription(label(R.string.back)).performClick()
    }

    private fun label(resource: Int) = rule.activity.getString(resource)

    private fun text(resource: Int) = text(label(resource))

    private fun waitForKeyboard(visible: Boolean) {
        var imeBottom = -1
        var lastInsetChange = SystemClock.uptimeMillis()
        // Platform IME animation is not a Compose idling resource.
        rule.waitUntil(10_000) {
            val bottom = visibleImeInsets()?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            if (bottom != imeBottom) {
                imeBottom = bottom
                lastInsetChange = SystemClock.uptimeMillis()
            }
            (bottom > 0) == visible && SystemClock.uptimeMillis() - lastInsetChange >= 350
        }
    }

    private fun visibleImeInsets() =
        WindowInspector
            .getGlobalWindowViews()
            .filter { it.isAttachedToWindow && it.hasWindowFocus() }
            .mapNotNull { ViewCompat.getRootWindowInsets(it) }
            .firstOrNull { it.isVisible(WindowInsetsCompat.Type.ime()) }

    private fun screenshot(name: String) {
        captureScreenshot(app, "adaptive-screens", name)
    }
}
