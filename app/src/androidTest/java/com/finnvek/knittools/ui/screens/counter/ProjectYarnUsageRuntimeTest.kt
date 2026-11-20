package com.finnvek.knittools.ui.screens.counter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectYarnUsageRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProjectYarnUsageRuntimeTest {
    private lateinit var app: App
    private lateinit var database: KnitToolsDatabase
    private lateinit var counter: CounterRepository
    private lateinit var yarn: YarnCardRepository
    private lateinit var usage: ProjectYarnUsageRepository
    private var scenario: ActivityScenario<MainActivity>? = null
    private var projectId = 0L
    private var cardId = 0L
    private var noteId = 0L
    private val projectName = "Yarn usage runtime " + UUID.randomUUID().toString().take(8)
    private val noteName = "Runtime mohair"
    private var sentinel: File? = null
    private var imperialBefore = false

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        check(app.packageName.endsWith(".notestest") || app.packageName.endsWith(".activepolicytest")) {
            "Runtime fixtures require an isolated test package"
        }
        if (ProPolicyTestControl.isActive) ProPolicyTestControl.setPurchased(true)
        database = app.database.get()
        counter = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        val runner = RoomDatabaseTransactionRunner(database)
        yarn = YarnCardRepository(database.yarnCardDao(), database.counterProjectDao(), app, runner, Dispatchers.IO)
        usage =
            ProjectYarnUsageRepository(
                database.projectYarnUsageDao(),
                database.projectYarnNoteDao(),
                database.yarnCardDao(),
                runner,
                Dispatchers.IO,
            )
        runBlocking(Dispatchers.IO) {
            if (ProPolicyTestControl.isActive) {
                withTimeout(5_000) {
                    app.proManager
                        .get()
                        .proState
                        .first { it.isPro }
                }
            }
            imperialBefore = PreferencesManager(app).preferences.first().useImperial
            PreferencesManager(app).setUseImperial(false)
            projectId =
                (
                    counter.createProject(
                        projectName,
                        canCreateAdditionalProjects = true,
                    ) as ProjectCreationResult.Created
                ).projectId
            noteId = database.projectYarnNoteDao().upsert(ProjectYarnNoteEntity(projectId = projectId, name = noteName))
        }
        sentinel =
            File.createTempFile("yarn-usage-runtime", ".bin", app.cacheDir).apply {
                writeText("Unchanged document and photo bytes")
            }
        launchProject()
    }

    private fun launchProject() {
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        // Projektikortti avaa projektinäkymän; laskuriin vie jatka-kortin nappi.
        clickText(projectName)
        clickDescription(text(R.string.project_continue_content_description, projectName))
        openYarn()
    }

    @After
    fun tearDown() {
        scenario?.close()
        if (::counter.isInitialized) {
            runBlocking(Dispatchers.IO) {
                if (projectId != 0L) counter.deleteProject(projectId)
                if (cardId != 0L) yarn.deleteCard(cardId)
                PreferencesManager(app).setUseImperial(imperialBefore)
            }
        }
        sentinel?.delete()
        if (ProPolicyTestControl.isActive) ProPolicyTestControl.setPurchased(false)
    }

    @Test
    fun realCounterYarnFlowPreservesPairAcrossRecreationCompletionAndSourceDeletion() {
        val before = runBlocking { database.counterProjectDao().getProject(projectId) }
        clickDescription(text(R.string.yarn_usage_track_named, noteName))
        edit(YarnUsageField.ALLOCATED, "600")
        edit(YarnUsageField.USED, "350")
        scenario?.recreate()
        scrollToNode("restored remaining") {
            it.text?.toString() ==
                app.getString(R.string.yarn_usage_remaining_format, "250 m")
        }
        save()
        val initial = currentUsage()
        assertEquals(600.0, initial.amounts.allocatedMeters)
        assertEquals(350.0, initial.amounts.usedMeters)
        assertEquals(before, runBlocking { database.counterProjectDao().getProject(projectId) })
        assertEquals("Unchanged document and photo bytes", sentinel?.readText())

        clickText(text(R.string.save_to_my_yarn))
        waitUntil("saved card") {
            runBlocking {
                database.projectYarnNoteDao().getById(noteId)?.savedYarnCardId !=
                    null
            }
        }
        waitUntil("save confirmation in the yarn sheet") {
            findNode { it.text?.toString() == text(R.string.saved_to_my_yarn_explanation) } != null
        }
        cardId = runBlocking { requireNotNull(database.projectYarnNoteDao().getById(noteId)?.savedYarnCardId) }
        assertEquals(initial.id, currentUsage().id)
        assertEquals(initial.amounts, currentUsage().amounts)
        val stash = runBlocking { database.yarnCardDao().getCard(cardId)?.quantityInStash }

        scrollToNode("note saved to My Yarn") {
            it.text?.toString().equals(text(R.string.saved_to_my_yarn), ignoreCase = true)
        }
        clickDescription(text(R.string.yarn_usage_edit_named, noteName))
        edit(YarnUsageField.USED, "400")
        save()
        val paired = currentUsage()
        assertEquals(initial.id, paired.id)
        assertEquals(cardId, paired.source.yarnCardId)
        assertEquals(noteId, paired.source.projectYarnNoteId)
        assertEquals(initial.amounts.copy(usedMeters = 400.0), paired.amounts)

        scenario?.close()
        scenario = null
        runBlocking { counter.archiveProject(projectId, System.currentTimeMillis()) }
        assertEquals(paired.amounts, currentUsage().amounts)
        runBlocking { counter.reactivateProject(projectId, counter.getProject(projectId)?.completedAt) }
        assertEquals(paired.amounts, currentUsage().amounts)
        launchProject()
        clickDescription(text(R.string.unlink_yarn))
        waitUntil("card unlinked") { runBlocking { requireNotNull(yarn.getCard(cardId)).linkedProjectId == null } }
        assertEquals(paired.amounts, currentUsage().amounts)
        assertEquals(stash, runBlocking { database.yarnCardDao().getCard(cardId)?.quantityInStash })
        assertTrue(runBlocking { yarn.deleteCard(cardId) })
        scrollToNode("save note again") { it.text?.toString() == text(R.string.save_to_my_yarn) }
        assertEquals(noteId, currentUsage().source.projectYarnNoteId)
        assertEquals(paired.amounts, currentUsage().amounts)
        runBlocking { database.projectYarnNoteDao().delete(noteId) }
        scrollToNode("orphan usage") { it.text?.toString() == text(R.string.yarn_usage_unavailable) }
        assertEquals(initial.sourceNameSnapshot, currentUsage().sourceNameSnapshot)
        clickDescription(text(R.string.yarn_usage_edit_named, noteName))
        edit(YarnUsageField.USED, "700")
        save()
        assertEquals(700.0, currentUsage().amounts.usedMeters)
        runBlocking { counter.deleteProject(projectId) }
        assertNull(runBlocking { database.projectYarnUsageDao().getById(initial.id) })
    }

    @Test
    fun linkedCardEntryUsesTheSamePersistenceAndKeepsGlobalQuantity() {
        runBlocking {
            cardId =
                requireNotNull(
                    yarn.saveCard(
                        YarnCard(yarnName = "Runtime stash", linkedProjectId = projectId, quantityInStash = 11),
                    ),
                )
        }
        clickDescription(text(R.string.yarn_usage_track_named, "Runtime stash"))
        edit(YarnUsageField.USED, "0")
        save()
        assertEquals(0.0, currentUsage().amounts.usedMeters)
        assertEquals(11, runBlocking { database.yarnCardDao().getCard(cardId)?.quantityInStash })
        assertNotNull(runBlocking { database.projectYarnNoteDao().getById(noteId) })
        assertTrue(
            runBlocking {
                database
                    .sessionDao()
                    .getSessionsForProject(projectId)
                    .first()
                    .isEmpty()
            },
        )
    }

    private fun openYarn() {
        clickText(text(R.string.project_content_yarn))
        waitForManagement()
    }

    private fun waitForManagement() {
        waitUntil("Yarn management") {
            findNode { it.text?.toString()?.equals(text(R.string.linked_yarn_title), ignoreCase = true) == true } !=
                null
        }
    }

    private fun edit(
        field: YarnUsageField,
        value: String,
    ) {
        val label =
            text(
                if (field == YarnUsageField.ALLOCATED) R.string.yarn_usage_allocated else R.string.yarn_usage_used,
            )
        var node: AccessibilityNodeInfo? = scrollToNode(label) { it.contentDescription?.toString() == label }
        while (node != null && !node.isEditable) node = node.parent
        val arguments =
            Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
        check(requireNotNull(node).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments))
        waitUntil("edited $label") {
            findNode { it.isEditable && it.text?.toString() == value } != null
        }
    }

    private fun save() {
        clickText(text(R.string.save))
        waitForManagement()
    }

    private fun clickText(value: String) = click(value) { it.text?.toString() == value }

    private fun clickDescription(value: String) = click(value) { it.contentDescription?.toString() == value }

    private fun click(
        description: String,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ) {
        var target: AccessibilityNodeInfo? = scrollToNode(description, matches)
        while (target != null && !target.isClickable) target = target.parent
        check(requireNotNull(target).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        idle()
    }

    private fun scrollToNode(
        description: String,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo {
        idle()
        findNode(matches)?.let { return it }
        waitUntil("scrollable content") { findNode { it.isScrollable } != null || findNode(matches) != null }
        waitUntil("scroll start") {
            if (findNode(matches) != null) return@waitUntil true
            val scrollable = findNode { it.isScrollable } ?: return@waitUntil false
            if (scrollable.actionList.none { it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD }) {
                true
            } else {
                scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                idle()
                false
            }
        }
        var result: AccessibilityNodeInfo? = null
        waitUntil(description) {
            result = findNode(matches)
            if (result != null) {
                true
            } else {
                findNode { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                idle()
                false
            }
        }
        return requireNotNull(result)
    }

    private fun findNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            automation.clearCache()
        }

        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isVisibleToUser && matches(node)) return node
            repeat(node.childCount) { index ->
                node.getChild(index)?.let { visit(it) }?.let { return it }
            }
            return null
        }
        return automation.rootInActiveWindow?.let(::visit)
    }

    private fun idle() = InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(150, 10_000)

    private fun waitUntil(
        description: String,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for $description")
    }

    private fun currentUsage() =
        runBlocking {
            requireNotNull(usage.observeForProject(projectId).first()).mapNotNull { it.usage }.single()
        }

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = app.getString(id, *args)
}
