package com.finnvek.knittools.ui.screens.counter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ActiveProjectPolicyRuntimeTest {
    private lateinit var app: App
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: CounterRepository
    private var scenario: ActivityScenario<MainActivity>? = null

    @get:Rule
    val testName = TestName()

    private val diagnosticRun = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
    private var expectedProjectName: String? = null
    private var actionSearch = 0
    private val clickTimeline = JSONArray()

    private fun clickObservation(phase: String) {
        clickTimeline.put(
            JSONObject()
                .put("phase", phase)
                .put("elapsedRealtime", SystemClock.elapsedRealtime())
                .put("expectedProjectName", expectedProjectName ?: JSONObject.NULL),
        )
    }

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        assumeTrue(
            "Requires the offline isolated policy installation",
            app.packageName == "com.finnvek.knittools.activepolicytest",
        )
        database = app.database.get()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking(Dispatchers.IO) {
            database.clearAllTables()
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            if (!app.preferencesManager
                    .get()
                    .preferences
                    .first()
                    .showCompletedProjects
            ) {
                app.preferencesManager.get().toggleShowCompletedProjects()
            }
        }
        purchased(false)
        trial(TrialState())
        trialResult(TrialStartResult.Started)
        waitFor {
            app.proManager
                .get()
                .proState.value.status == ProStatus.TRIAL_NOT_STARTED
        }
        assertFalse(app.proManager.get().hasFeature(ProFeature.UNLIMITED_PROJECTS))
        diagnostic("setup-complete")
    }

    @After
    fun close() {
        diagnostic("before-close")
        scenario?.close()
    }

    @Test
    fun ravelryPendingCreationUsesFreedSlotAfterDismissedRequestStaysCancelled() {
        val active = seed("Active project")
        launch()
        click(R.string.tab_tools)
        scrollTo(R.string.tool_ravelry)
        click(R.string.tool_ravelry)
        waitFor { findNode { it.isEditable && it.isEnabled } != null }
        typeName("cardigan")
        val editor = requireNotNull(findNode { it.isEditable })
        assertTrue(editor.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id))
        open("Synthetic long winter cardigan pattern with cables and a matching hat 1")
        scrollTo(R.string.start_project)
        click(R.string.start_project)
        waitText(text(R.string.pro_prompt_projects_title))
        click(R.string.pro_prompt_not_now)
        runBlocking { repository.archiveProject(active, 200L) }
        idle()
        assertEquals(1, runBlocking { repository.getProjectCount() })
        val next = seed("Another active")
        click(R.string.start_project)
        waitText(text(R.string.pro_prompt_projects_title))
        runBlocking { repository.deleteProject(next) }
        waitFor { runBlocking { repository.getActiveProjectCount() == 1 } }
        assertEquals(2, runBlocking { repository.getProjectCount() })
        val created = runBlocking { repository.getActiveProjects().first().single() }
        assertEquals("Synthetic long winter cardigan pattern with cables and a matching hat", created.name)
        assertTrue(created.linkedPatternId != null)
    }

    private fun scrollTo(label: Int) {
        repeat(8) {
            if (findText(text(label))?.isVisibleToUser == true) return
            findNode { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            idle()
        }
        waitText(text(label))
    }

    @Test
    fun pendingCreationOpensWhenLastActiveProjectIsCompleted() {
        val active = seed("Active project")
        launch()
        clickDescription(R.string.new_project)
        waitText(text(R.string.pro_prompt_projects_title))
        runBlocking { repository.archiveProject(active, 200L) }
        waitText(text(R.string.new_project_details_title))
        typeName("Free slot creation")
        click(R.string.create_project)
        waitFor { runBlocking { repository.getActiveProjectCount() == 1 } }
        assertEquals(2, runBlocking { repository.getProjectCount() })
    }

    @Test
    fun dismissedCreationDoesNotOpenWhenLastActiveProjectIsDeleted() {
        val active = seed("Active project")
        launch()
        clickDescription(R.string.new_project)
        waitText(text(R.string.pro_prompt_projects_title))
        click(R.string.pro_prompt_not_now)
        runBlocking { repository.deleteProject(active) }
        idle()
        assertFalse(findText(text(R.string.new_project_details_title))?.isVisibleToUser == true)
        assertEquals(0, runBlocking { repository.getProjectCount() })
    }

    @Test
    fun freeCreateCompleteRetainCreateAgainAndDismissLimitPrompt() {
        launch()
        createViaUi("First free project")
        val first = projectId("First free project")
        action(R.string.complete_project)
        click(R.string.complete_project)
        waitFor { runBlocking { repository.getProject(first)?.isCompleted == true } }
        waitText("First free project")
        assertTrue(findText("First free project")?.isVisibleToUser == true)
        createViaUi("Next free project")
        pressBack()
        clickDescription(R.string.new_project)
        waitText(text(R.string.pro_prompt_projects_title))
        screenshot("en-create")
        click(R.string.pro_prompt_not_now)
        assertEquals(1, runBlocking { repository.getActiveProjectCount() })
        assertEquals(2, runBlocking { repository.getProjectCount() })
    }

    @Test
    fun reactivationPromptDismissalAndZeroActiveRetryPreserveTheTarget() {
        val completed = seed("Finished target", true)
        val active = seed("Active project")
        launch()
        open("Finished target")
        action(R.string.reactivate_project)
        waitText(text(R.string.pro_prompt_reactivation_title))
        screenshot("en-reactivation")
        click(R.string.pro_prompt_not_now)
        assertTrue(runBlocking { requireNotNull(repository.getProject(completed)).isCompleted })
        action(R.string.reactivate_project)
        waitText(text(R.string.pro_prompt_reactivation_title))
        runBlocking { repository.archiveProject(active, 200L) }
        waitFor { runBlocking { repository.getProject(completed)?.isCompleted == false } }
        assertEquals(1, runBlocking { repository.getActiveProjectCount() })
        assertFalse(app.proManager.get().isPro())
        assertEquals(2, runBlocking { repository.getProjectCount() })
    }

    @Test
    fun zeroActiveReactivationIsFreeAndLegacyActiveProjectsRemainMutable() {
        val completed = seed("Finished target", true)
        launch()
        open("Finished target")
        action(R.string.reactivate_project)
        waitFor { runBlocking { repository.getProject(completed)?.isCompleted == false } }
        clickDescription(R.string.counter_add_row)
        waitFor { runBlocking { repository.getProject(completed)?.count == 1 } }
        val legacy = seed("Legacy active")
        pressBack()
        open("Legacy active")
        clickDescription(R.string.counter_add_row)
        waitFor { runBlocking { repository.getProject(legacy)?.count == 1 } }
        assertEquals(1, runBlocking { repository.getProject(completed)?.count })
        assertEquals(2, runBlocking { repository.getActiveProjectCount() })
        screenshot("legacy-active")
    }

    @Test
    fun trialResumesExactCreationDraftOnce() {
        launch()
        clickDescription(R.string.new_project)
        waitText(text(R.string.new_project_details_title))
        typeName("Original draft")
        seed("Concurrent active")
        click(R.string.create_project)
        waitText(text(R.string.pro_prompt_projects_title))
        click(R.string.pro_start_free_trial)
        waitFor { runBlocking { repository.getAllProjects().first().count { it.name == "Original draft" } == 1 } }
        assertEquals(2, runBlocking { repository.getActiveProjectCount() })
        assertEquals(
            ProStatus.TRIAL_ACTIVE,
            app.proManager
                .get()
                .proState.value.status,
        )
        idle()
        assertEquals(2, runBlocking { repository.getProjectCount() })
    }

    @Test
    fun alreadyActiveTrialResumesExactReactivationOnce() {
        val completed = seed("Finished target", true)
        seed("Active project")
        trialResult(TrialStartResult.AlreadyActive)
        launch()
        open("Finished target")
        action(R.string.reactivate_project)
        waitText(text(R.string.pro_prompt_reactivation_title))
        click(R.string.pro_start_free_trial)
        waitFor { runBlocking { repository.getProject(completed)?.isCompleted == false } }
        val after = runBlocking { repository.getProject(completed) }
        idle()
        assertEquals(after, runBlocking { repository.getProject(completed) })
        assertEquals(2, runBlocking { repository.getProjectCount() })
    }

    @Test
    fun rejectedTrialResultsDoNotReactivateAndPurchaseReturnUsesOriginalProject() {
        val completed = seed("Finished target", true)
        seed("Active project")
        launch()
        open("Finished target")
        action(R.string.reactivate_project)
        waitText(text(R.string.pro_prompt_reactivation_title))
        for (result in listOf(
            TrialStartResult.AlreadyExpired,
            TrialStartResult.AlreadyTampered,
            TrialStartResult.Failed,
        )) {
            trialResult(result)
            click(R.string.pro_start_free_trial)
            idle()
            assertTrue(runBlocking { requireNotNull(repository.getProject(completed)).isCompleted })
        }
        trial(TrialState(hasStarted = true))
        waitFor {
            app.proManager
                .get()
                .proState.value.status == ProStatus.TRIAL_EXPIRED
        }
        click(R.string.pro_prompt_see_pro)
        assertTrue(runBlocking { requireNotNull(repository.getProject(completed)).isCompleted })
        purchased(true)
        waitFor { app.proManager.get().isPro() }
        pressBack()
        waitFor { runBlocking { repository.getProject(completed)?.isCompleted == false } }
        assertEquals(2, runBlocking { repository.getActiveProjectCount() })
    }

    @Test
    fun finnishCreateAndReactivationPromptsRenderDistinctCopy() {
        seed("Valmis projekti", true)
        seed("Aktiivinen projekti")
        runBlocking { app.preferencesManager.get().setAppLanguage(AppLanguage.FINNISH) }
        launch()
        clickDescription(R.string.new_project)
        waitText("Luo toinen aktiivinen projekti Prolla")
        screenshot("fi-create")
        click(R.string.pro_prompt_not_now)
        open("Valmis projekti")
        action(R.string.reactivate_project)
        waitText("Aktivoi projekti uudelleen Prolla")
        screenshot("fi-reactivation")
        click(R.string.pro_prompt_not_now)
        assertEquals(1, runBlocking { repository.getActiveProjectCount() })
    }

    private fun launch() {
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        idle()
    }

    private fun createViaUi(name: String) {
        expectedProjectName = name
        clickDescription(R.string.new_project)
        waitText(text(R.string.new_project_details_title))
        typeName(name)
        click(R.string.create_project)
        waitFor { runBlocking { repository.getAllProjects().first().any { it.name == name } } }
        idle()
    }

    private fun open(name: String) {
        expectedProjectName = name
        clickObservation("open-start")
        waitText(name)
        clickObservation("open-name-visible")
        clickNode(name) { it.text?.toString()?.equals(name, ignoreCase = true) == true }
        clickObservation("open-click-returned")
        idle()
        clickObservation("open-idle-finished")
    }

    private fun action(label: Int) {
        actionSearch++
        diagnostic("before-open-sheet", requested = text(label))
        clickDescription(R.string.project_actions_title)
        repeat(8) { attempt ->
            val node = findText(text(label))
            val visible = node?.isVisibleToUser == true
            diagnostic("lookup", attempt + 1, text(label), node, visible)
            if (visible) {
                diagnostic("scroll-not-attempted-action-visible", attempt + 1, text(label))
                val value = text(label)
                clickNode(value) { it.text?.toString()?.equals(value, ignoreCase = true) == true }
                idle()
                diagnostic("action-clicked", attempt + 1, text(label))
                return
            }
            val scrollNode = findNode { it.isScrollable }
            diagnostic("scroll-selected", attempt + 1, text(label), selected = scrollNode)
            try {
                val result = scrollNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                diagnostic(
                    "scroll-result",
                    attempt + 1,
                    text(label),
                    operation = "ACTION_SCROLL_FORWARD result=$result",
                )
            } catch (failure: Throwable) {
                diagnostic("scroll-exception", attempt + 1, text(label), operation = failure.stackTraceToString())
                throw failure
            }
            idle()
            diagnostic("after-scroll-idle", attempt + 1, text(label))
        }
        diagnostic("final-action-failure", 8, text(label))
        error("Action not visible: ${text(label)}")
    }

    private fun diagnostic(
        phase: String,
        attempt: Int = 0,
        requested: String? = null,
        matched: AccessibilityNodeInfo? = null,
        visibleDecision: Boolean? = null,
        selected: AccessibilityNodeInfo? = null,
        operation: String? = null,
        clickEvidence: JSONObject? = null,
    ) {
        if (!::app.isInitialized || app.packageName != "com.finnvek.knittools.activepolicytest") return
        val started = SystemClock.elapsedRealtime()
        val record =
            JSONObject()
                .put("timestamp", System.currentTimeMillis())
                .put("elapsedRealtime", started)
                .put("run", diagnosticRun)
                .put("test", testName.methodName)
                .put("phase", phase)
                .put("search", actionSearch)
                .put("attempt", attempt)
                .put("requested", requested ?: JSONObject.NULL)
                .put("visibleDecision", visibleDecision ?: JSONObject.NULL)
                .put("matched", matched?.let(::diagnosticNode) ?: JSONObject.NULL)
                .put("selectedScrollNode", selected?.let(::diagnosticNode) ?: JSONObject.NULL)
                .put("operation", operation ?: JSONObject.NULL)
                .put("clickEvidence", clickEvidence ?: JSONObject.NULL)
                .put("clickTimeline", JSONArray(clickTimeline.toString()))
        try {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val nodes = JSONArray()

            fun visit(
                node: AccessibilityNodeInfo,
                path: String,
            ) {
                nodes.put(diagnosticNode(node).put("path", path).put("selected", node == selected))
                for (index in 0 until node.childCount) {
                    node.getChild(index)?.let { visit(it, "$path/$index") }
                }
            }
            automation.rootInActiveWindow?.let { visit(it, "active") }
            record.put("accessibilityTree", nodes)
            record.put(
                "scrollableCount",
                (0 until nodes.length()).count { nodes.getJSONObject(it).getBoolean("scrollable") },
            )
            val windows = JSONArray()
            automation.windows.forEach { window ->
                windows.put(
                    JSONObject()
                        .put("id", window.id)
                        .put("type", window.type)
                        .put("title", window.title)
                        .put("active", window.isActive)
                        .put("focused", window.isFocused),
                )
            }
            record.put("windows", windows)
            record.put("accessibilityScope", "UiAutomation active-window tree; not the unmerged Compose semantics tree")
            val rows = runBlocking(Dispatchers.IO) { database.counterProjectDao().getAllProjectsOnce() }
            record.put(
                "projects",
                JSONArray().apply {
                    rows.sortedBy { it.id }.forEach {
                        put(
                            JSONObject()
                                .put("id", it.id)
                                .put("name", it.name)
                                .put("isCompleted", it.isCompleted)
                                .put("count", it.count)
                                .put("completedAt", it.completedAt ?: JSONObject.NULL)
                                .put("updatedAt", it.updatedAt),
                        )
                    }
                },
            )
            record.put("totalCount", rows.size).put("activeCount", rows.count { !it.isCompleted })
            record.put("expectedProjectName", expectedProjectName ?: JSONObject.NULL)
            record.put("expectedProjectIds", JSONArray(rows.filter { it.name == expectedProjectName }.map { it.id }))
            record.put("defaultLocale", Locale.getDefault().toLanguageTag())
            record.put(
                "appLocales",
                app.resources.configuration.locales
                    .toLanguageTags(),
            )
            val counters = JSONArray()
            scenario?.onActivity { activity ->
                record.put(
                    "activityLocales",
                    activity.resources.configuration.locales
                        .toLanguageTags(),
                )

                fun inspectStore(store: ViewModelStore) {
                    val field =
                        ViewModelStore::class.java.declaredFields.single {
                            Map::class.java.isAssignableFrom(it.type)
                        }
                    field.isAccessible = true
                    (field.get(store) as Map<*, *>).values.forEach { model ->
                        if (model is CounterViewModel) {
                            val state = model.uiState.value
                            counters.put(
                                JSONObject()
                                    .put("projectId", state.projectId ?: JSONObject.NULL)
                                    .put("projectName", state.projectName)
                                    .put("isCompleted", state.isCompleted),
                            )
                            val details = counters.getJSONObject(counters.length() - 1)
                            details.put("count", state.counter.count)
                            val jobs = JSONObject()
                            model.javaClass.declaredFields
                                .filter {
                                    Job::class.java.isAssignableFrom(it.type)
                                }.forEach { field ->
                                    field.isAccessible = true
                                    val job = field.get(model) as? Job
                                    jobs.put(
                                        field.name,
                                        job?.let {
                                            JSONObject()
                                                .put("active", it.isActive)
                                                .put("completed", it.isCompleted)
                                                .put("cancelled", it.isCancelled)
                                        } ?: JSONObject.NULL,
                                    )
                                }
                            details.put("jobs", jobs)
                            val savedField =
                                model.javaClass.getDeclaredField("savedStateHandle").apply {
                                    isAccessible = true
                                }
                            val saved = savedField.get(model) as SavedStateHandle
                            val savedState = JSONObject()
                            val keys = saved.keys().filter { it.contains("project") || it.contains("reactivation") }
                            keys.forEach { savedState.put(it, saved.get<Any?>(it) ?: JSONObject.NULL) }
                            details.put("savedState", savedState)
                        } else if (model != null &&
                            model.javaClass.name == "androidx.navigation.NavControllerViewModel"
                        ) {
                            model.javaClass.declaredFields
                                .filter {
                                    Map::class.java.isAssignableFrom(it.type)
                                }.forEach { nested ->
                                    nested.isAccessible = true
                                    val nestedStores = nested.get(model) as Map<*, *>
                                    nestedStores.values.filterIsInstance<ViewModelStore>().forEach(::inspectStore)
                                }
                        }
                    }
                }
                inspectStore(activity.viewModelStore)
            }
            record.put("counterViewModels", counters)
            record.put(
                "proState",
                app.proManager
                    .get()
                    .proState.value
                    .toString(),
            )
            record.put("unlimitedProjects", app.proManager.get().hasFeature(ProFeature.UNLIMITED_PROJECTS))
            record.put(
                "purchased",
                app.billingManager
                    .get()
                    .isProPurchased.value,
            )
            record.put(
                "purchaseStateReady",
                app.billingManager
                    .get()
                    .purchaseStateReady.value,
            )
            val manager = trialManager()
            val state =
                manager.javaClass
                    .getMethod(
                        "getTrialState",
                    ).invoke(manager) as kotlinx.coroutines.flow.StateFlow<*>
            record.put("controlledTrialState", state.value.toString())
            val result = manager.javaClass.getDeclaredField("auditResult").apply { isAccessible = true }
            record.put("controlledTrialStartResult", result.get(manager).toString())
        } catch (failure: Throwable) {
            record.put("diagnosticError", failure.stackTraceToString())
        }
        record.put("observationDurationMs", SystemClock.elapsedRealtime() - started)
        val folder = File(app.getExternalFilesDir(null), "active-policy/runtime-diagnostics").apply { mkdirs() }
        File(folder, "$diagnosticRun-${testName.methodName}.jsonl").appendText("$record\n")
    }

    private fun diagnosticNode(node: AccessibilityNodeInfo): JSONObject {
        val bounds = Rect().also(node::getBoundsInScreen)
        return JSONObject()
            .put("identity", node.hashCode())
            .put("windowId", node.windowId)
            .put("class", node.className)
            .put("package", node.packageName)
            .put("viewId", node.viewIdResourceName)
            .put("text", node.text)
            .put("description", node.contentDescription)
            .put("paneTitle", node.paneTitle)
            .put("stateDescription", node.stateDescription)
            .put("visible", node.isVisibleToUser)
            .put("clickable", node.isClickable)
            .put("enabled", node.isEnabled)
            .put("scrollable", node.isScrollable)
            .put("focused", node.isFocused)
            .put("bounds", bounds.toShortString())
            .put("actions", JSONArray(node.actionList.map { "${it.id}:${it.label}" }))
            .put("nativeDescription", node.toString())
    }

    private fun click(label: Int) {
        waitText(text(label))
        val value = text(label)
        clickNode(value) { it.text?.toString()?.equals(value, ignoreCase = true) == true }
        idle()
    }

    private fun clickDescription(label: Int) {
        val description = text(label)
        if (label == R.string.counter_add_row) {
            clickObservation("description-wait-start")
        }
        val counterProject =
            expectedProjectName.takeIf {
                label == R.string.counter_add_row || label == R.string.project_actions_title
            }
        clickNode(description, counterProject) { it.contentDescription?.toString() == description }
        idle()
    }

    private fun typeName(value: String) {
        val editor = requireNotNull(findNode { it.isEditable })
        assertTrue(
            editor.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
                },
            ),
        )
        idle()
    }

    private fun clickNode(
        requested: String,
        counterProject: String? = null,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ) {
        val attempts = JSONArray()
        waitFor(failureMessage = {
            diagnostic(
                "live-click-failure",
                requested = requested,
                clickEvidence = JSONObject().put("attempts", attempts),
            )
            "No unique live click target: $requested; expected counter project: $counterProject; attempts: $attempts"
        }) {
            val nodes = currentNodes()
            val evidence = JSONObject().put("lookupStarted", SystemClock.elapsedRealtime())
            attempts.put(evidence)
            val heading = counterProject?.uppercase(Locale.getDefault())
            val projectNode = nodes.singleOrNull { it.text?.toString() == heading && it.isVisibleToUser }
            val projectReady = heading == null || projectNode?.refresh() == true && projectNode.isVisibleToUser
            evidence.put("expectedHeading", heading).put("projectReady", projectReady)
            if (!projectReady) return@waitFor false
            val candidates = JSONArray()
            evidence.put("candidates", candidates)
            val targets =
                nodes
                    .filter(predicate)
                    .mapNotNull { node ->
                        val candidate = JSONObject().put("matchedBefore", diagnosticNode(node))
                        candidates.put(candidate)
                        liveClickTarget(node, candidate)?.takeIf { predicate(node) }
                    }.distinct()
            evidence.put("liveTargetCount", targets.size)
            val target = targets.singleOrNull() ?: return@waitFor false
            val refreshed = target.refresh()
            evidence.put("targetRefresh", refreshed).put("targetBefore", diagnosticNode(target))
            if (!refreshed || !target.isVisibleToUser || !target.isEnabled || !target.isClickable) return@waitFor false
            evidence.put("actionStarted", SystemClock.elapsedRealtime())
            val result = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            evidence.put("actionReturned", SystemClock.elapsedRealtime()).put("result", result)
            if (result && requested == text(R.string.counter_add_row)) {
                diagnostic(
                    "description-click-result",
                    requested = requested,
                    clickEvidence = JSONObject().put("attempts", attempts),
                )
            }
            result
        }
    }

    private fun liveClickTarget(
        node: AccessibilityNodeInfo,
        evidence: JSONObject,
    ): AccessibilityNodeInfo? {
        val ancestors = JSONArray()
        evidence.put("ancestors", ancestors)
        var target: AccessibilityNodeInfo? = node
        while (target != null) {
            val refreshed = target.refresh()
            ancestors.put(diagnosticNode(target).put("refresh", refreshed))
            if (!refreshed || !target.isVisibleToUser || !target.isEnabled) return null
            if (target.isClickable) return target
            target = target.parent
        }
        return null
    }

    private fun currentNodes(): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.clearCache()
        val nodes = mutableListOf<AccessibilityNodeInfo>()

        fun visit(node: AccessibilityNodeInfo) {
            nodes.add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        automation.rootInActiveWindow?.let(::visit)
        return nodes
    }

    private fun findText(value: String): AccessibilityNodeInfo? =
        findNode {
            it.text?.toString()?.equals(value, ignoreCase = true) == true
        }

    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { child -> visit(child)?.let { return it } }
            }
            return null
        }
        return InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation.rootInActiveWindow
            ?.let(::visit)
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        idle()
    }

    private fun idle() = SystemClock.sleep(250L)

    private fun text(id: Int): String {
        var value = app.getString(id)
        scenario?.onActivity { value = it.getString(id) }
        return value
    }

    private fun waitText(value: String) = waitFor { findText(value)?.isVisibleToUser == true }

    private fun waitFor(
        failureMessage: () -> String = { "Timed out waiting for policy UI state" },
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + 15_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100L)
        }
        screenshot("failure-${SystemClock.elapsedRealtime()}")
        error(failureMessage())
    }

    private fun projectId(name: String): Long =
        runBlocking {
            repository
                .getAllProjects()
                .first()
                .single {
                    it.name ==
                        name
                }.id
        }

    private fun seed(
        name: String,
        completed: Boolean = false,
    ): Long =
        runBlocking {
            database.counterProjectDao().insert(
                CounterProjectEntity(name = name, isCompleted = completed, completedAt = if (completed) 100L else null),
            )
        }

    private fun trialManager(): Any =
        app.proManager.get().javaClass.getDeclaredField("trialManager").let {
            it.isAccessible = true
            requireNotNull(it.get(app.proManager.get()))
        }

    private fun trial(state: TrialState) {
        val manager = trialManager()
        manager.javaClass.getMethod("auditState", TrialState::class.java).invoke(manager, state)
    }

    private fun trialResult(result: TrialStartResult) {
        val manager = trialManager()
        manager.javaClass.getMethod("auditStartResult", TrialStartResult::class.java).invoke(manager, result)
    }

    private fun purchased(value: Boolean) {
        val manager = app.billingManager.get()
        manager.javaClass
            .getMethod(
                "auditPurchased",
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).invoke(manager, value, true)
    }

    private fun screenshot(name: String) {
        val folder = File(app.getExternalFilesDir(null), "active-policy").apply { mkdirs() }
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
