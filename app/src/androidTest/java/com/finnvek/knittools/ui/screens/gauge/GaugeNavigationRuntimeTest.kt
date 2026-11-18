package com.finnvek.knittools.ui.screens.gauge

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KNITTOOLS_DATABASE_VERSION
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class GaugeNavigationRuntimeTest {
    private lateinit var app: App
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: CounterRepository
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var fixtureProjectId = 0L
    private var fixtureProjectName = ""
    private var copiedText: String? = null

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        check(app.packageName.endsWith(".notestest") || app.packageName.endsWith(".activepolicytest")) {
            "Runtime fixtures require an isolated test package"
        }
        database = app.database.get()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
    }

    @After
    fun tearDown() {
        try {
            if (copiedText != null && ::scenario.isInitialized) {
                scenario.onActivity { activity ->
                    val clipboard = activity.getSystemService(ClipboardManager::class.java)
                    val clip = clipboard.primaryClip
                    if (clip?.description?.label == text(R.string.measurement_title) &&
                        clip.getItemAt(0).text?.toString() == copiedText
                    ) {
                        clipboard.clearPrimaryClip()
                    }
                }
            }
        } finally {
            try {
                if (::scenario.isInitialized) scenario.close()
            } finally {
                if (::repository.isInitialized && fixtureProjectId > 0L) {
                    runBlocking(Dispatchers.IO) { repository.deleteProject(fixtureProjectId) }
                }
            }
        }
    }

    @Test
    fun legacyToolsEntryDefaultsToAdjustmentAndReturnsWithoutDatabaseWrites() {
        runBlocking { app.onboardingStateRepository.get().dismissWelcome() }
        launchActivity()
        clickText(text(R.string.tab_tools))
        assertTabSelected(R.string.tab_tools)
        clickText(text(R.string.tool_gauge_converter))
        waitForText(text(R.string.measurement_task))
        assertAdjustmentSelected()
        assertTabSelected(R.string.tab_tools)
        assertProjectContextAbsent()
        assertTextAbsent(text(R.string.measurement_project_unavailable))
        val before = databaseSnapshot()

        enterStitchAdjustment()
        assertAdjustedStitches()
        pressToolbarBack()
        waitForText(text(R.string.tool_increase_decrease))
        waitForText(text(R.string.tool_gauge_converter))
        assertTextAbsent(text(R.string.measurement_task))
        assertTabSelected(R.string.tab_tools)

        clickText(text(R.string.tool_gauge_converter))
        waitForText(text(R.string.measurement_task))
        assertAdjustmentSelected()
        selectChoice(R.string.gauge_stated_per, R.string.measurement_per_10cm)
        assertField(gaugeLabel(R.string.measurement_pattern_stitches), "")
        assertField(text(R.string.stitches_in_pattern), "")
        assertTextAbsent(text(R.string.measurement_nearest_count))
        assertEquals(before, databaseSnapshot())
    }

    // CPD-OFF: Navigaatiotestin perakkaiset tilatarkistukset pidetaan skenaarion yhteydessa.
    @Test
    fun projectEntryCopiesAndRestoresRealActivityStateWithoutMutatingProject() {
        createProjectFixture()
        launchActivity()
        openFixtureProject()
        val before = databaseSnapshot()
        assertNotNull(before.project)
        assertEquals(1, before.project?.count)
        assertEquals(FIXTURE_NOTES, before.project?.notes)

        openProjectCalculator()
        assertTabSelected(R.string.tab_projects)
        assertAdjustmentSelected()
        enterStitchAdjustment()
        assertAdjustedStitches()
        copyAndCheckOwnResult()
        assertEquals(before, databaseSnapshot())

        clickText(text(R.string.tab_tools))
        assertTabSelected(R.string.tab_tools)
        waitForText(text(R.string.tool_gauge_converter))
        clickText(text(R.string.tab_projects))
        assertTabSelected(R.string.tab_projects)
        waitForNode("calculator toolbar") { it.contentDescription?.toString() == text(R.string.back) }
        assertAdjustmentSelected()
        scrollToText(text(R.string.measurement_project_context, fixtureProjectName))
        assertField(gaugeLabel(R.string.measurement_actual_stitches), "22")
        assertField(gaugeLabel(R.string.measurement_pattern_stitches), "20")
        assertField(text(R.string.stitches_in_pattern), "100")

        scenario.recreate()
        assertAdjustmentSelected()
        assertTextAbsent(text(R.string.measurement_copied))
        scrollToText(text(R.string.measurement_project_context, fixtureProjectName))
        assertField(gaugeLabel(R.string.measurement_actual_stitches), "22")
        assertField(gaugeLabel(R.string.measurement_pattern_stitches), "20")
        assertField(text(R.string.stitches_in_pattern), "100")
        assertAdjustedStitches()
        assertEquals(before, databaseSnapshot())

        pressToolbarBack()
        waitForProjectCounter()
        assertTabSelected(R.string.tab_projects)
        assertTextAbsent(text(R.string.measurement_task))
        assertEquals(before, databaseSnapshot())
    }

    // CPD-ON

    @Test
    fun deletedProjectBecomesGenericAndKeepsTemporaryInputsAfterRecreation() {
        createProjectFixture()
        launchActivity()
        openFixtureProject()
        openProjectCalculator()
        enterStitchAdjustment()
        assertAdjustedStitches()

        runBlocking(Dispatchers.IO) { repository.deleteProject(fixtureProjectId) }
        scrollToText(text(R.string.measurement_project_unavailable))
        assertProjectContextAbsent()
        assertAdjustedStitches()
        val afterDeletion = databaseSnapshot()
        assertNull(afterDeletion.project)

        scenario.recreate()
        scrollToText(text(R.string.measurement_project_unavailable))
        assertProjectContextAbsent()
        assertField(gaugeLabel(R.string.measurement_actual_stitches), "22")
        assertField(text(R.string.stitches_in_pattern), "100")
        assertAdjustedStitches()
        assertEquals(afterDeletion, databaseSnapshot())
    }

    private fun launchActivity() {
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        assertTabSelected(R.string.tab_projects)
    }

    private fun createProjectFixture() {
        fixtureProjectName = "Measurement test " + UUID.randomUUID().toString().take(8)
        runBlocking(Dispatchers.IO) {
            // Calculator navigation needs existing content independently of the current creation entitlement.
            fixtureProjectId = database.counterProjectDao().insert(CounterProjectEntity(name = fixtureProjectName))
            // Seed existing content independently of the current notes-creation entitlement.
            database.counterProjectDao().updateNotes(fixtureProjectId, FIXTURE_NOTES, System.currentTimeMillis())
            check(repository.applyMainCounterChange(fixtureProjectId, MainCounterChange.Increment))
        }
    }

    private fun openFixtureProject() {
        assertTabSelected(R.string.tab_projects)
        // Projektikortti avaa projektinäkymän; laskuriin vie jatka-kortin nappi.
        clickText(fixtureProjectName)
        waitForNode("project overview toolbar") { it.contentDescription?.toString() == text(R.string.back) }
        clickContentDescription(text(R.string.project_continue_content_description, fixtureProjectName))
        waitForProjectCounter()
    }

    private fun waitForProjectCounter() {
        waitForText(fixtureProjectName)
        // Projektinäkymässäkin on toimintovalikko, joten laskurin tunnistaa rivinlisäyksestä.
        waitForNode("project counter") { it.contentDescription?.toString() == text(R.string.counter_add_row) }
    }

    private fun openProjectCalculator() {
        clickContentDescription(text(R.string.project_actions_title))
        waitForText(text(R.string.project_actions_section_counter_tools))
        scrollToText(text(R.string.measurement_title))
        clickText(text(R.string.measurement_title))
        waitForText(text(R.string.measurement_task))
        assertAdjustmentSelected()
        scrollToText(text(R.string.measurement_project_context, fixtureProjectName))
    }

    private fun enterStitchAdjustment() {
        selectChoice(R.string.gauge_stated_per, R.string.measurement_per_10cm)
        selectChoice(R.string.your_gauge_section, R.string.enter_directly)
        setField(gaugeLabel(R.string.measurement_actual_stitches), "22")
        setField(gaugeLabel(R.string.measurement_pattern_stitches), "20")
        setField(text(R.string.stitches_in_pattern), "100")
    }

    private fun assertAdjustedStitches() {
        scrollToText(app.resources.getQuantityString(R.plurals.measurement_stitches, 110, 110))
        scrollToText(text(R.string.measurement_original_size))
        scrollToText(text(R.string.measurement_value_unit_format, "50", text(R.string.unit_cm)))
        scrollToText(text(R.string.measurement_rounded_size))
        scrollToText(text(R.string.measurement_value_unit_format, "50", text(R.string.unit_cm)))
        // Osio-otsikko on SectionLabel, joka näytetään versaalina.
        assertTextAbsent(text(R.string.measurement_rows_height).uppercase())
    }

    private fun copyAndCheckOwnResult() {
        scrollToText(text(R.string.measurement_copy))
        clickText(text(R.string.measurement_copy))
        waitForText(text(R.string.measurement_copied))
        scenario.onActivity { activity ->
            val clip = checkNotNull(activity.getSystemService(ClipboardManager::class.java).primaryClip)
            check(clip.description.label == text(R.string.measurement_title)) {
                "Calculator clipboard label is missing"
            }
            val value = clip.getItemAt(0).text.toString()
            check(value.contains(fixtureProjectName)) { "Calculator clipboard context is missing" }
            copiedText = value
            val count = app.resources.getQuantityString(R.plurals.measurement_stitches, 110, 110)
            assertTrue(
                "Clipboard is missing the adjusted stitch count",
                value.contains(text(R.string.measurement_result_line, text(R.string.measurement_nearest_count), count)),
            )
            assertTrue("Clipboard is missing measurement units", value.contains(text(R.string.unit_cm)))
            assertTrue(
                "Clipboard is missing the estimate warning",
                value.contains(text(R.string.measurement_adjust_warning)),
            )
        }
    }

    private fun selectChoice(
        selectorResource: Int,
        optionResource: Int,
    ) {
        // Valinnat ovat segmenttivalitsimia: nimike ja vaihtoehdot näkyvät suoraan ilman ponnahdusikkunaa.
        // Vaihtoehto vieritetään näkyviin itse: nimikkeen alla se voi jäädä ruudun alareunan alle.
        scrollToText(text(selectorResource))
        val option = scrollToText(text(optionResource))
        // Valitulla RadioButton-vaihtoehdolla ei ole klikkaustoimintoa, joten valmiiksi valittua ei napauteta.
        if (generateSequence(option) { it.parent }.none { it.isChecked }) clickText(text(optionResource))
    }

    private fun assertAdjustmentSelected() {
        scrollToText(text(R.string.measurement_task))
        assertTabSelected(R.string.measurement_adjust)
    }

    private fun assertTabSelected(labelResource: Int) {
        val label = text(labelResource)
        waitForNode("selected tab " + label) { candidate ->
            if (!hasText(candidate, label)) {
                false
            } else {
                // Alanavigaation välilehti on selected; segmenttivalitsimen RadioButton-vaihtoehto on checked.
                var node: AccessibilityNodeInfo? = candidate
                while (node != null && !node.isSelected && !node.isChecked && !node.isClickable) node = node.parent
                node?.isSelected == true || node?.isChecked == true
            }
        }
    }

    private fun assertProjectContextAbsent() {
        val prefix = text(R.string.measurement_project_context, "")
        assertNull(
            "Generic calculator must not keep a project context",
            findNode(visibleOnly = false) { it.text?.toString()?.startsWith(prefix) == true },
        )
    }

    private fun assertTextAbsent(expected: String) {
        assertNull("Unexpected text: " + expected, findNode(visibleOnly = false) { hasText(it, expected) })
    }

    private fun setField(
        description: String,
        value: String,
    ) {
        val field =
            editableAncestor(
                scrollToNode("input " + description) { it.contentDescription?.toString() == description },
            )
        val arguments =
            Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
        check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) { "Could not edit " + description }
        assertField(description, value)
    }

    private fun assertField(
        description: String,
        expected: String,
    ) {
        scrollToNode("input " + description) { it.contentDescription?.toString() == description }
        waitUntil("input value for " + description) {
            val field = findNode { it.contentDescription?.toString() == description }?.let(::editableAncestor)
            field != null && field.text?.toString().orEmpty() == expected
        }
    }

    private fun gaugeLabel(labelResource: Int): String =
        text(R.string.measurement_field_with_unit, text(labelResource), text(R.string.measurement_per_10cm))

    private fun pressToolbarBack() {
        clickContentDescription(text(R.string.back))
    }

    private fun clickContentDescription(description: String) {
        click("content description " + description) { it.contentDescription?.toString() == description }
    }

    private fun clickText(expected: String) {
        click("text " + expected) { hasText(it, expected) }
    }

    private fun click(
        description: String,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ) {
        waitForUiIdle()
        waitUntil("click " + description) {
            var target = findNode(matches = matches)
            while (target != null && !target.isClickable) target = target.parent
            target?.let { it.refresh() && it.isEnabled && it.performAction(AccessibilityNodeInfo.ACTION_CLICK) } == true
        }
        waitForUiIdle()
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) target = target.parent
        return checkNotNull(target) { "No clickable accessibility ancestor" }
    }

    private fun editableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isEditable) target = target.parent
        return checkNotNull(target) { "No editable accessibility ancestor" }
    }

    private fun waitForText(expected: String): AccessibilityNodeInfo =
        waitForNode("text " + expected) { hasText(it, expected) }

    private fun scrollToText(expected: String): AccessibilityNodeInfo =
        scrollToNode("text " + expected) { hasText(it, expected) }

    private fun scrollToNode(
        description: String,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo {
        waitForUiIdle()
        findNode(matches = matches)?.let { return it }
        findNode(visibleOnly = false, matches = matches)?.let { target ->
            if (target.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)) {
                waitForUiIdle()
                findNode(matches = matches)?.let { return it }
            }
        }
        waitForNode("scrollable content") { it.isScrollable }
        waitUntil("start of scrollable content") {
            val scrollable = findNode { it.isScrollable } ?: return@waitUntil false
            if (scrollable.actionList.none { it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD }) {
                true
            } else {
                scroll(scrollable, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                false
            }
        }
        // Large fonts make this form several viewports long. Bound the traversal by
        // scroll steps, not the short timeout used for a single asynchronous UI change.
        repeat(40) {
            findNode(matches = matches)?.let { return it }
            val scrollable =
                findNode { node ->
                    node.isScrollable && node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD }
                } ?: throw AssertionError("Reached end of scrollable content before " + description)
            scrollForwardPartially(scrollable)
        }
        return findNode(matches = matches) ?: throw AssertionError("Scroll step limit reached for " + description)
    }

    private fun scroll(
        node: AccessibilityNodeInfo,
        action: Int,
    ) {
        if (node.refresh() && node.performAction(action)) waitForUiIdle()
    }

    private fun scrollForwardPartially(node: AccessibilityNodeInfo) {
        val bounds = Rect().also(node::getBoundsInScreen)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val start = SystemClock.uptimeMillis()
        repeat(13) { step ->
            val action =
                when (step) {
                    0 -> MotionEvent.ACTION_DOWN
                    12 -> MotionEvent.ACTION_UP
                    else -> MotionEvent.ACTION_MOVE
                }
            val event =
                MotionEvent.obtain(
                    start,
                    start + step * 25L,
                    action,
                    bounds.exactCenterX(),
                    bounds.top + bounds.height() * (0.7f - 0.4f * step / 12f),
                    0,
                )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        waitForUiIdle()
    }

    // CPD-OFF: UIAutomator-odotusapujen toistuva rakenne pidetaan runtime-testin yhteydessa.
    private fun waitForUiIdle() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(UI_IDLE_MILLIS, UI_TIMEOUT_MILLIS)
    }

    private fun waitForNode(
        description: String,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo {
        var result: AccessibilityNodeInfo? = null
        waitUntil(description) {
            result = findNode(matches = matches)
            result != null
        }
        return checkNotNull(result)
    }

    private fun waitUntil(
        description: String,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(UI_POLL_MILLIS)
        }
        throw AssertionError("Timed out waiting for " + description)
    }
    // CPD-ON

    private fun hasText(
        node: AccessibilityNodeInfo,
        expected: String,
    ): Boolean =
        node.text
            ?.toString()
            ?.lineSequence()
            ?.any { it.equals(expected, ignoreCase = true) } == true

    private fun findNode(
        visibleOnly: Boolean = true,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) automation.clearCache()
        val root = automation.rootInActiveWindow ?: return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !root.refresh()) return null
        return findNode(root, visibleOnly, matches)
    }

    private fun findNode(
        node: AccessibilityNodeInfo,
        visibleOnly: Boolean = true,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if ((!visibleOnly || node.isVisibleToUser) && matches(node)) return node
        repeat(node.childCount) { index ->
            val child = node.getChild(index) ?: return@repeat
            findNode(child, visibleOnly, matches)?.let { return it }
        }
        return null
    }

    private fun text(
        resource: Int,
        vararg arguments: Any,
    ): String = app.getString(resource, *arguments)

    private fun databaseSnapshot(): DatabaseSnapshot =
        runBlocking(Dispatchers.IO) {
            val sqlite = database.openHelper.readableDatabase
            val counts =
                DATABASE_TABLES.associateWith { table ->
                    sqlite.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
                        check(cursor.moveToFirst())
                        cursor.getLong(0)
                    }
                }
            assertEquals("Calculator must not change the Room schema", KNITTOOLS_DATABASE_VERSION, sqlite.version)
            DatabaseSnapshot(database.counterProjectDao().getProject(fixtureProjectId), counts)
        }

    private data class DatabaseSnapshot(
        val project: CounterProjectEntity?,
        val rowCounts: Map<String, Long>,
    )

    private companion object {
        const val UI_TIMEOUT_MILLIS = 10_000L
        const val UI_POLL_MILLIS = 50L
        const val UI_IDLE_MILLIS = 200L
        const val FIXTURE_NOTES = "Keep this measurement navigation fixture note unchanged."
        val DATABASE_TABLES =
            listOf(
                "counter_projects",
                "counter_history",
                "yarn_cards",
                "sessions",
                "active_sessions",
                "row_reminders",
                "progress_photos",
                "project_counters",
                "project_yarn_notes",
                "saved_patterns",
                "pattern_annotation_layers",
                "pattern_annotations",
                "pattern_bookmarks",
                "project_documents",
                "project_folders",
                "project_folder_assignments",
                "project_yarn_usage",
                "project_completions",
            )
    }
}
