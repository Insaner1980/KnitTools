package com.finnvek.knittools.ui.screens.project

import android.accessibilityservice.AccessibilityServiceInfo
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.ActiveSessionEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.ProjectFolderAssignmentEntity
import com.finnvek.knittools.data.local.ProjectFolderEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.ui.descendants
import com.finnvek.knittools.widget.CounterWidgetReceiver
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.WidgetData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ProjectFoldersActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val lifecycle = ActivityLifecycleMonitorRegistry.getInstance()
    private val activity = AtomicReference<MainActivity>()
    private val activityObserver =
        ActivityLifecycleCallback { instance, stage ->
            if (instance is MainActivity && stage == Stage.CREATED) activity.set(instance)
        }

    private lateinit var database: KnitToolsDatabase
    private lateinit var transactionRunner: DatabaseTransactionRunner
    private lateinit var folderName: String
    private lateinit var destinationFolderName: String
    private lateinit var folderProjectName: String
    private lateinit var widgetProjectName: String
    private var originalAccessibilityFlags: Int? = null
    private var folderId = 0L
    private var destinationFolderId = 0L
    private var folderProjectId = 0L
    private var widgetProjectId = 0L
    private lateinit var activeSessionFixture: ActiveSessionEntity
    private lateinit var folderProjectBefore: CounterProjectEntity
    private lateinit var projectDocumentsBefore: List<ProjectDocumentEntity>

    @Before
    fun setUp() {
        val automation = instrumentation.uiAutomation
        val info = automation.serviceInfo
        originalAccessibilityFlags = info.flags
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        automation.serviceInfo = info
        val app = ApplicationProvider.getApplicationContext<App>()
        database = app.database.get()
        transactionRunner = app.transactionRunner.get()
        val suffix = UUID.randomUUID().toString().take(8)
        folderName = "Activity folder $suffix"
        destinationFolderName = "Activity destination $suffix"
        folderProjectName = "Activity folder project $suffix"
        widgetProjectName = "Activity widget project $suffix"

        runBlocking {
            if (ProPolicyTestControl.isActive) {
                setTestProStatus(ProStatus.TRIAL_NOT_STARTED)
                withTimeout(15_000) { app.proManager.get().initialControlsReady.first { it } }
            }
            transactionRunner.run {
                check(database.sessionDao().getActiveSession() == null) {
                    "Project folder activity fixture requires no active session"
                }
                folderId =
                    database.projectFolderDao().insert(
                        ProjectFolderEntity(
                            name = folderName,
                            normalizedName = folderName.lowercase(Locale.ROOT),
                            sortOrder = Int.MAX_VALUE,
                        ),
                    )
                destinationFolderId =
                    database.projectFolderDao().insert(
                        ProjectFolderEntity(
                            name = destinationFolderName,
                            normalizedName = destinationFolderName.lowercase(Locale.ROOT),
                            sortOrder = Int.MAX_VALUE,
                        ),
                    )
                folderProjectId =
                    database.counterProjectDao().insert(
                        CounterProjectEntity(name = folderProjectName),
                    )
                widgetProjectId =
                    database.counterProjectDao().insert(
                        CounterProjectEntity(name = widgetProjectName),
                    )
                database.projectFolderDao().insertOrReplaceAssignment(
                    ProjectFolderAssignmentEntity(folderProjectId, folderId),
                )
                folderProjectBefore = requireNotNull(database.counterProjectDao().getProject(folderProjectId))
                projectDocumentsBefore = database.projectDocumentDao().getForProject(folderProjectId)
                val now = System.currentTimeMillis()
                val bootCount =
                    runCatching {
                        Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT).toLong()
                    }.getOrNull()
                activeSessionFixture =
                    ActiveSessionEntity(
                        sessionToken = "activity-folder-$suffix",
                        projectId = folderProjectId,
                        startedAtWallMillis = now,
                        startZoneId = ZoneId.systemDefault().id,
                        startRow = 0,
                        lastObservedRow = 0,
                        trustedLastObservedRow = 0,
                        trustedRowsWorked = 0,
                        pendingRowsWorked = 0,
                        reviewedRowsWorked = 0,
                        reviewedLastObservedRow = 0,
                        unreviewedRowsWorked = 0,
                        checkpointedDurationSeconds = 0,
                        reviewedDurationBaselineSeconds = 0,
                        segmentStartedAtWallMillis = now,
                        segmentStartedElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
                        bootCount = bootCount,
                        recoveryReason = null,
                        recoveryIntervalToken = null,
                        recoverySuggestedDurationSeconds = null,
                        recoveryPromptShown = false,
                        updatedAtWallMillis = now,
                    )
                database.sessionDao().insertActiveSession(activeSessionFixture)
            }
        }
        // The trusted widget launch can replace and consume MainActivity's Intent.
        // Track the real Activity lifecycle without matching that mutable Intent.
        lifecycle.addLifecycleCallback(activityObserver)
        startActivity(
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
    }

    @After
    fun tearDown() {
        try {
            finishActivity()
        } finally {
            lifecycle.removeLifecycleCallback(activityObserver)
            if (ProPolicyTestControl.isActive) ProPolicyTestControl.reset()
            originalAccessibilityFlags?.let { flags ->
                val automation = instrumentation.uiAutomation
                val info = automation.serviceInfo
                info.flags = flags
                automation.serviceInfo = info
            }
            if (::transactionRunner.isInitialized) {
                runBlocking {
                    transactionRunner.run {
                        if (::activeSessionFixture.isInitialized) {
                            database.sessionDao().deleteActiveSession(activeSessionFixture.sessionToken)
                        }
                        if (folderProjectId != 0L) database.counterProjectDao().delete(folderProjectId)
                        if (widgetProjectId != 0L) database.counterProjectDao().delete(widgetProjectId)
                        if (folderId != 0L) database.projectFolderDao().delete(folderId)
                        if (destinationFolderId != 0L) database.projectFolderDao().delete(destinationFolderId)
                    }
                }
            }
        }
    }

    @Test
    fun selectedFolderSurvivesProjectReturnAndRecreation() {
        selectFixtureFolder()
        tapProjectCard(folderProjectName)

        waitForContentDescription(BACK_DESCRIPTION)
        waitForContentDescription(PROJECT_ACTIONS_DESCRIPTION)
        waitForText(folderProjectName)
        navigateBack()
        assertFixtureFolderVisible()

        recreateActivity()
        assertFixtureFolderVisible()
        assertFixtureData()
    }

    @Test
    fun actualWidgetOpensUnfiledProjectAndBackKeepsSameTaskFolder() {
        selectFixtureFolder()
        val task = currentTaskId()
        recreateActivity()
        assertFixtureFolderVisible()
        waitForReadyScreen()

        if (ProPolicyTestControl.isActive) {
            ProPolicyTestControl.setPurchased(true)
            val app = ApplicationProvider.getApplicationContext<App>()
            runBlocking { withTimeout(15_000) { app.proManager.get().proState.first { it.isPro } } }
        }
        launchFromRenderedWidget()
        assertWidgetProjectCounter()
        assertEquals(task, currentTaskId())
        navigateBack()
        assertFixtureFolderVisible()
        assertFixtureData()
    }

    @Test
    fun trustedWidgetEntryInNewTaskReturnsToAllProjects() {
        selectFixtureFolder()
        val previousTask = currentTaskId()
        finishActivity()

        val app = ApplicationProvider.getApplicationContext<App>()
        val intent =
            MainActivity
                .createCounterLaunchIntent(app, widgetProjectId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        assertWidgetProjectCounter()
        assertNotEquals(previousTask, currentTaskId())
        navigateBack()
        waitForContentDescription(ALL_PROJECTS_SELECTED_DESCRIPTION)
        waitForText(folderProjectName)
        waitForText(widgetProjectName)
        assertFixtureData()
    }

    private fun assertFixtureData() {
        runBlocking {
            assertEquals(folderId, database.projectFolderDao().getAssignment(folderProjectId)?.folderId)
            assertNull(database.projectFolderDao().getAssignment(widgetProjectId))
            assertEquals(activeSessionFixture, database.sessionDao().getActiveSession())
        }
    }

    private fun selectFixtureFolder() {
        waitForReadyScreen()
        waitForContentDescription(ALL_PROJECTS_SELECTED_DESCRIPTION)
        clickContentDescription(ALL_PROJECTS_SELECTED_DESCRIPTION)
        clickContentDescription(folderDescription(selected = false))
        assertFixtureFolderVisible()
    }

    private fun assertFixtureFolderVisible() {
        waitForNode("selected folder $folderName", timeoutMillis = STARTUP_TIMEOUT_MILLIS) {
            it.contentDescription?.toString() == folderDescription(selected = true)
        }
        waitForText(folderProjectName)
        assertNull(findNode { it.text?.toString() == widgetProjectName })
    }

    private fun currentTaskId(): Int {
        var taskId = -1
        onActivity { taskId = it.taskId }
        return taskId
    }

    private fun startActivity(intent: Intent) {
        val previous = activity.get()
        ApplicationProvider.getApplicationContext<App>().startActivity(intent)
        awaitActivityAfter(previous)
    }

    private fun recreateActivity() {
        val previous = checkNotNull(activity.get())
        instrumentation.runOnMainSync { previous.recreate() }
        awaitActivityAfter(previous)
    }

    private fun awaitActivityAfter(previous: MainActivity?) {
        waitUntil("a new MainActivity instance", timeoutMillis = STARTUP_TIMEOUT_MILLIS) {
            activity.get()?.let { it !== previous } == true
        }
    }

    private fun finishActivity() {
        val instance = activity.get() ?: return
        instrumentation.runOnMainSync {
            if (!instance.isDestroyed) instance.finishAndRemoveTask()
        }
        waitUntil("MainActivity destruction") { instance.isDestroyed }
    }

    private fun onActivity(action: (MainActivity) -> Unit) {
        instrumentation.runOnMainSync { action(checkNotNull(activity.get())) }
    }

    private fun assertWidgetProjectCounter() {
        val app = ApplicationProvider.getApplicationContext<App>()
        val addRow = app.getString(R.string.counter_add_row)
        waitForNode("counter controls after widget launch", timeoutMillis = STARTUP_TIMEOUT_MILLIS) {
            it.contentDescription?.toString() == addRow
        }
        waitForText(widgetProjectName)
    }

    private fun launchFromRenderedWidget() {
        val app = ApplicationProvider.getApplicationContext<App>()
        val manager = AppWidgetManager.getInstance(app)
        val host = AppWidgetHost(app, FOLDER_WIDGET_HOST_ID)
        val id = host.allocateAppWidgetId()
        val provider = ComponentName(app, CounterWidgetReceiver::class.java)
        val automation = instrumentation.uiAutomation
        var widget: View? = null
        try {
            val options =
                Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 300)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160)
                }
            automation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            try {
                assertTrue("Bind the isolated widget", manager.bindAppWidgetIdIfAllowed(id, provider, options))
            } finally {
                automation.dropShellPermissionIdentity()
            }
            host.startListening()
            onActivity { activity ->
                // RemoteViews needs the platform inflater, without AppCompat view substitution.
                val view = host.createView(app, id, manager.getAppWidgetInfo(id))
                widget = view
                val density = activity.resources.displayMetrics.density
                val layout = FrameLayout.LayoutParams((300 * density).toInt(), (160 * density).toInt())
                activity.addContentView(view, layout)
            }
            runBlocking {
                val data = WidgetData(projectName = widgetProjectName, projectId = widgetProjectId)
                CounterWidgetState.syncAll(app, data)
            }
            waitUntil("widget title for $widgetProjectName") {
                var visible = false
                onActivity {
                    visible = renderedWidgetTitle(checkNotNull(widget)) != null
                }
                visible
            }
            onActivity {
                var target: View = checkNotNull(renderedWidgetTitle(checkNotNull(widget)))
                while (!target.isClickable && target.parent is View) target = target.parent as View
                assertTrue("Click the widget's RemoteViews action", target.performClick())
            }
        } finally {
            try {
                instrumentation.runOnMainSync { widget?.let { (it.parent as? ViewGroup)?.removeView(it) } }
            } finally {
                host.stopListening()
                host.deleteAppWidgetId(id)
            }
        }
    }

    private fun renderedWidgetTitle(widget: View): TextView? =
        descendants(widget)
            .filterIsInstance<TextView>()
            .firstOrNull { it.isShown && it.text.toString() == widgetProjectName }

    @Test
    fun overviewMenuMovesActiveSessionProjectToFolderThenUnfiledWithoutChangingMetadata() {
        selectFixtureFolder()
        tapProjectCard(folderProjectName)
        waitForText(folderProjectName)

        // Projektikortti avaa projektinäkymän, jonka ylivuotovalikossa kansiosiirto on.
        moveOverviewProjectTo(destinationFolderName)
        waitForAssignment(destinationFolderId)
        moveOverviewProjectTo(UNFILED_LABEL)
        waitForAssignment(null)

        runBlocking {
            assertEquals(folderProjectBefore, database.counterProjectDao().getProject(folderProjectId))
            assertEquals(activeSessionFixture, database.sessionDao().getActiveSession())
            assertEquals(projectDocumentsBefore, database.projectDocumentDao().getForProject(folderProjectId))
        }
    }

    private fun navigateBack() {
        clickContentDescription(BACK_DESCRIPTION)
    }

    private fun waitForReadyScreen() {
        waitForNode("ready Activity content", timeoutMillis = STARTUP_TIMEOUT_MILLIS) {
            it.viewIdResourceName == "startup_ready"
        }
    }

    private fun moveOverviewProjectTo(destination: String) {
        clickContentDescription(PROJECT_ACTIONS_DESCRIPTION)
        clickText(MOVE_TO_FOLDER_LABEL)
        waitForText("Move $folderProjectName to folder")
        scrollUntilTextVisible(destination)
        clickText(destination)
        waitForContentDescription(PROJECT_ACTIONS_DESCRIPTION)
    }

    private fun waitForAssignment(expectedFolderId: Long?) {
        waitUntil("assignment $expectedFolderId") {
            runBlocking { database.projectFolderDao().getAssignment(folderProjectId)?.folderId == expectedFolderId }
        }
    }

    private fun folderDescription(selected: Boolean): String =
        "$folderName. Project organization folder" + if (selected) ". Selected." else "."

    private fun waitForContentDescription(description: String) {
        waitForNode("content description $description") { node ->
            node.contentDescription?.toString() == description
        }
    }

    private fun waitForText(text: String) {
        waitForNode("text $text") { node -> node.text?.toString() == text }
    }

    private fun scrollUntilTextVisible(text: String) {
        waitUntil("text $text") {
            if (findNode { node -> node.text?.toString() == text } != null) {
                true
            } else {
                findNode { node -> node.isScrollable }
                    ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                false
            }
        }
    }

    private fun clickContentDescription(description: String) {
        click(
            waitForNode("content description $description") { node ->
                node.contentDescription?.toString() == description
            },
        )
    }

    private fun clickText(text: String) {
        click(waitForNode("text $text") { node -> node.text?.toString() == text })
    }

    private fun tapProjectCard(name: String) {
        val bounds = Rect()
        waitForNode("project card $name") { it.text?.toString() == name }.getBoundsInScreen(bounds)
        val command = "input tap ${bounds.centerX()} ${bounds.centerY()}"
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    private fun click(node: AccessibilityNodeInfo) {
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) {
            val parent = target.parent
            target = parent
        }
        val clickable = checkNotNull(target) { "No clickable accessibility ancestor" }
        waitUntil("accessibility click") {
            clickable.refresh() &&
                clickable.isEnabled &&
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun waitForNode(
        description: String,
        timeoutMillis: Long = UI_TIMEOUT_MILLIS,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            findNode(matches)?.let { return it }
            Thread.sleep(UI_POLL_MILLIS)
        }
        val visible = mutableListOf<String>()
        findNode { node ->
            (node.text ?: node.contentDescription ?: node.viewIdResourceName)?.toString()?.let(visible::add)
            visible.size >= 20
        }
        val window = instrumentation.uiAutomation.rootInActiveWindow?.packageName
        throw AssertionError("Timed out waiting for $description (window=$window, visible=$visible)")
    }

    private fun waitUntil(
        description: String,
        timeoutMillis: Long = UI_TIMEOUT_MILLIS,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(UI_POLL_MILLIS)
        }
        throw AssertionError("Timed out waiting for $description")
    }

    private fun findNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val automation = instrumentation.uiAutomation
        // A widget or Activity transition can leave the previous Compose tree cached.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) automation.clearCache()
        val root = automation.rootInActiveWindow
        return root
            ?.takeIf { it.packageName == instrumentation.targetContext.packageName }
            ?.let { findNode(it, matches) }
    }

    private fun findNode(
        node: AccessibilityNodeInfo,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && matches(node)) return node
        repeat(node.childCount) { index ->
            val child = node.getChild(index) ?: return@repeat
            findNode(child, matches)?.let { match ->
                return match
            }
        }
        return null
    }

    private companion object {
        const val FOLDER_WIDGET_HOST_ID = 110014
        const val ALL_PROJECTS_SELECTED_DESCRIPTION = "All Projects. Virtual view of every project. Selected."
        const val BACK_DESCRIPTION = "Back"
        const val PROJECT_ACTIONS_DESCRIPTION = "Project actions"
        const val MOVE_TO_FOLDER_LABEL = "Move to folder"
        const val UNFILED_LABEL = "Unfiled"
        const val UI_TIMEOUT_MILLIS = 5_000L
        const val STARTUP_TIMEOUT_MILLIS = 30_000L
        const val UI_POLL_MILLIS = 50L
    }
}
