package com.finnvek.knittools.ui.screens.project

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.data.local.ActiveSessionEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.ProjectFolderAssignmentEntity
import com.finnvek.knittools.data.local.ProjectFolderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProjectFoldersActivityTest {
    private lateinit var database: KnitToolsDatabase
    private lateinit var transactionRunner: DatabaseTransactionRunner
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var folderName: String
    private lateinit var destinationFolderName: String
    private lateinit var folderProjectName: String
    private lateinit var widgetProjectName: String
    private var folderId = 0L
    private var destinationFolderId = 0L
    private var folderProjectId = 0L
    private var widgetProjectId = 0L
    private lateinit var activeSessionFixture: ActiveSessionEntity
    private lateinit var folderProjectBefore: CounterProjectEntity
    private lateinit var projectDocumentsBefore: List<ProjectDocumentEntity>

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<App>()
        database = app.database.get()
        transactionRunner = app.transactionRunner.get()
        val suffix = UUID.randomUUID().toString().take(8)
        folderName = "Activity folder $suffix"
        destinationFolderName = "Activity destination $suffix"
        folderProjectName = "Activity folder project $suffix"
        widgetProjectName = "Activity widget project $suffix"

        runBlocking {
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
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
        if (!::transactionRunner.isInitialized) return
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

    @Test
    fun selectedFolderSurvivesProjectReturnRecreationAndTrustedWidgetLaunch() {
        waitForContentDescription(ALL_PROJECTS_SELECTED_DESCRIPTION)
        clickContentDescription(ALL_PROJECTS_SELECTED_DESCRIPTION)
        waitForContentDescription(folderDescription(selected = false))
        clickContentDescription(folderDescription(selected = false))
        waitForContentDescription(folderDescription(selected = true))
        clickText(folderProjectName)

        waitForContentDescription(BACK_DESCRIPTION)
        waitForText(folderProjectName.uppercase(Locale.ROOT))
        navigateBack()
        waitForContentDescription(folderDescription(selected = true))
        waitForText(folderProjectName)

        scenario.recreate()
        waitForContentDescription(folderDescription(selected = true))
        waitForText(folderProjectName)

        scenario.onActivity { activity ->
            activity.startActivity(MainActivity.createCounterLaunchIntent(activity, widgetProjectId))
        }
        waitForContentDescription(BACK_DESCRIPTION)
        waitForText(widgetProjectName.uppercase(Locale.ROOT))

        navigateBack()
        waitForContentDescription(folderDescription(selected = true))
        waitForText(folderProjectName)

        runBlocking {
            assertEquals(folderId, database.projectFolderDao().getAssignment(folderProjectId)?.folderId)
            assertNull(database.projectFolderDao().getAssignment(widgetProjectId))
            assertEquals(activeSessionFixture, database.sessionDao().getActiveSession())
        }
    }

    @Test
    fun counterActionsMoveActiveSessionProjectToFolderThenUnfiledWithoutChangingMetadata() {
        waitForContentDescription(ALL_PROJECTS_SELECTED_DESCRIPTION)
        clickContentDescription(ALL_PROJECTS_SELECTED_DESCRIPTION)
        waitForContentDescription(folderDescription(selected = false))
        clickContentDescription(folderDescription(selected = false))
        waitForText(folderProjectName)
        clickText(folderProjectName)
        waitForText(folderProjectName.uppercase(Locale.ROOT))

        moveCounterProjectTo(destinationFolderName)
        waitForAssignment(destinationFolderId)
        moveCounterProjectTo(UNFILED_LABEL)
        waitForAssignment(null)

        runBlocking {
            assertEquals(folderProjectBefore, database.counterProjectDao().getProject(folderProjectId))
            assertEquals(activeSessionFixture, database.sessionDao().getActiveSession())
            assertEquals(projectDocumentsBefore, database.projectDocumentDao().getForProject(folderProjectId))
        }
    }

    private fun navigateBack() {
        scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun moveCounterProjectTo(destination: String) {
        clickContentDescription(PROJECT_ACTIONS_DESCRIPTION)
        waitForText(MOVE_TO_FOLDER_LABEL)
        waitForText(STOP_WORK_SESSION_LABEL)
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
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            findNode(matches)?.let { return it }
            Thread.sleep(UI_POLL_MILLIS)
        }
        throw AssertionError("Timed out waiting for $description")
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
        throw AssertionError("Timed out waiting for $description")
    }

    private fun findNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? =
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation.rootInActiveWindow
            ?.let { root -> findNode(root, matches) }

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
        const val ALL_PROJECTS_SELECTED_DESCRIPTION = "All Projects. Virtual view of every project. Selected."
        const val BACK_DESCRIPTION = "Back"
        const val PROJECT_ACTIONS_DESCRIPTION = "Project actions"
        const val MOVE_TO_FOLDER_LABEL = "Move to folder"
        const val STOP_WORK_SESSION_LABEL = "Stop work session"
        const val UNFILED_LABEL = "Unfiled"
        const val UI_TIMEOUT_MILLIS = 5_000L
        const val UI_POLL_MILLIS = 50L
    }
}
