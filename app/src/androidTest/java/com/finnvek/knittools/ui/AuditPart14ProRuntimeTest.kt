package com.finnvek.knittools.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.storage.NotesDraftStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectNotesSaveResult
import com.finnvek.knittools.repository.ReminderMutationResult
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.notes.NotesEditorScreen
import com.finnvek.knittools.ui.screens.notes.NotesEditorViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.CounterWidgetActions
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.WidgetData
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real Room, production repositories and rendered editor; only entitlement inputs are synthetic. */
class AuditPart14ProRuntimeTest {
    // Replacement content must not wait for the production splash's first Compose frame.
    @get:Rule(order = 0)
    val splash =
        TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()
    private val app = ApplicationProvider.getApplicationContext<App>()
    private val db get() = app.database.get()
    private val manager get() = app.proManager.get()
    private val repository get() =
        EntryPointAccessors
            .fromApplication(
                app,
                WidgetEntryPoint::class.java,
            ).counterRepository()
    private val notesStore = ViewModelStore()
    private val reminders get() =
        ReminderRepository(
            db.rowReminderDao(),
            db.counterProjectDao(),
            manager,
            app.transactionRunner.get(),
        )
    private val photos get() =
        ProgressPhotoRepository(
            db.progressPhotoDao(),
            ProgressPhotoStorage(),
            app,
            Dispatchers.IO,
            app.transactionRunner.get(),
        )

    @Before fun prepare() {
        org.junit.Assume.assumeTrue(ProPolicyTestControl.isActive && app.packageName.endsWith(".activepolicytest"))
        ProPolicyTestControl.reset()
        rule.runOnUiThread { rule.activity.setContent { } }
        runBlocking(Dispatchers.IO) { db.clearAllTables() }
        status(ProStatus.TRIAL_NOT_STARTED)
    }

    @After fun close() {
        rule.runOnUiThread { notesStore.clear() }
        if (ProPolicyTestControl.isActive && app.packageName.endsWith(".activepolicytest")) {
            runBlocking(Dispatchers.IO) { db.clearAllTables() }
            ProPolicyTestControl.reset()
        }
    }

    @Test fun fourStatesReachRealRepositoryAndViewModelBoundaries() {
        lateinit var counter: CounterViewModel
        rule.runOnUiThread { counter = ViewModelProvider(rule.activity)["audit14", CounterViewModel::class.java] }
        for (state in listOf(
            ProStatus.TRIAL_NOT_STARTED,
            ProStatus.TRIAL_ACTIVE,
            ProStatus.TRIAL_EXPIRED,
            ProStatus.PRO_PURCHASED,
        )) {
            status(state)
            val allowed = state == ProStatus.TRIAL_ACTIVE || state == ProStatus.PRO_PURCHASED
            val id =
                runBlocking(
                    Dispatchers.IO,
                ) { db.counterProjectDao().insert(CounterProjectEntity(name = "Audit $state")) }
            rule.runOnUiThread { counter.selectProjectById(id) }
            rule.waitUntil(10_000) { counter.uiState.value.projectId == id && counter.uiState.value.proStatus == state }
            assertEquals(allowed, counter.uiState.value.canCreateNotes)
            assertEquals(allowed, counter.uiState.value.canCreateSecondaryCounter)
            assertEquals(allowed, counter.uiState.value.canUseRowReminders)
            assertEquals(allowed, counter.uiState.value.canUseProgressPhotos)
            runBlocking(Dispatchers.IO) {
                assertEquals(allowed, manager.hasFeatureAfterInitialLoad(ProFeature.WIDGET))
                assertEquals(
                    allowed,
                    repository.createProject(
                        "Additional",
                        canCreateAdditionalProjects = true,
                    ) is ProjectCreationResult.Created,
                )
                assertEquals(allowed, repository.saveProjectNotes(id, "", "New note") is ProjectNotesSaveResult.Saved)
                assertEquals(allowed, repository.updateProjectSecondaryCount(id, 1))
                assertEquals(
                    allowed,
                    reminders.insert(
                        RowReminder(projectId = id, targetRow = 4, message = "Audit"),
                    ) is ReminderMutationResult.Success,
                )
            }
            runBlocking(Dispatchers.IO) { CounterWidgetState.save(app, WidgetData(projectId = id)) }
            val broadcastFinished = CountDownLatch(1)
            app.sendOrderedBroadcast(
                CounterWidgetActions.incrementIntent(app, id),
                null,
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        broadcastFinished.countDown()
                    }
                },
                null,
                0,
                null,
                null,
            )
            assertTrue(broadcastFinished.await(10, TimeUnit.SECONDS))
            assertEquals(
                if (allowed) 1 else 0,
                runBlocking(Dispatchers.IO) { db.counterProjectDao().getProject(id)?.count },
            )
            val source = image()
            try {
                rule.runOnUiThread { assertEquals(allowed, counter.savePhoto(Uri.fromFile(source), id)) }
                if (allowed) {
                    rule.waitUntil(10_000) { runBlocking(Dispatchers.IO) { photos.getPhotoCount(id).first() == 1 } }
                } else {
                    assertEquals(0, runBlocking(Dispatchers.IO) { photos.getPhotoCount(id).first() })
                }
            } finally {
                source.delete()
            }
        }
    }

    @Test fun delayedTrialUnlocksRestoredFirstNotesInLightTheme() = notesUnlock(dark = false)

    @Test fun delayedTrialUnlocksRestoredFirstNotesInDarkTheme() = notesUnlock(dark = true)

    @Test fun admittedPhotoFinishesAfterExpiryButCannotAuthorizeAnotherPhoto() {
        status(ProStatus.TRIAL_ACTIVE)
        val id =
            runBlocking(Dispatchers.IO) { db.counterProjectDao().insert(CounterProjectEntity(name = "Pending photo")) }
        lateinit var counter: CounterViewModel
        rule.runOnUiThread {
            counter = ViewModelProvider(rule.activity)["audit14photo", CounterViewModel::class.java]
            counter.selectProjectById(id)
        }
        rule.waitUntil(10_000) { counter.uiState.value.projectId == id && counter.uiState.value.canUseProgressPhotos }
        rule.runOnUiThread { counter.authorizeProgressPhotoCreation() }
        status(ProStatus.TRIAL_EXPIRED)
        val source = image()
        try {
            rule.runOnUiThread { assertTrue(counter.savePhoto(Uri.fromFile(source), id)) }
            rule.waitUntil(10_000) { runBlocking(Dispatchers.IO) { photos.getPhotoCount(id).first() == 1 } }
            rule.waitForIdle()
            rule.runOnUiThread { assertFalse(counter.savePhoto(Uri.fromFile(source), id)) }
            assertEquals(1, runBlocking(Dispatchers.IO) { photos.getPhotoCount(id).first() })
        } finally {
            source.delete()
        }
    }

    private fun notesUnlock(dark: Boolean) {
        val id =
            runBlocking(
                Dispatchers.IO,
            ) { db.counterProjectDao().insert(CounterProjectEntity(name = "Late entitlement")) }
        val handle =
            SavedStateHandle(
                mapOf(
                    "projectId" to id,
                    "notesDraft" to "Restored device draft",
                    "notesDraftBase" to "",
                ),
            )
        lateinit var model: NotesEditorViewModel
        rule.runOnUiThread {
            model =
                NotesEditorViewModel(
                    repository,
                    manager,
                    app.applicationScope,
                    handle,
                    NotesDraftStorage(app, Dispatchers.IO),
                )
            notesStore.put("notes", model)
            rule.activity.setContent {
                KnitToolsTheme(isDarkTheme = dark) {
                    NotesEditorScreen(onBack = {}, onSeePro = {}, viewModelProvider = { model })
                }
            }
        }
        rule.waitUntil(10_000) { model.uiState.value.isLoaded }
        assertFalse(model.uiState.value.canEditNotes)
        status(ProStatus.TRIAL_ACTIVE)
        rule.waitUntil(10_000) { model.uiState.value.canEditNotes }
        rule.onNode(hasSetTextAction()).assertIsDisplayed().assertTextContains("Restored device draft")
        rule.waitUntil(10_000) {
            runBlocking(Dispatchers.IO) {
                db.counterProjectDao().getProject(id)?.notesCreated ==
                    true
            }
        }
        status(ProStatus.TRIAL_EXPIRED)
        rule.onNode(hasSetTextAction()).assertIsDisplayed().assertTextContains("Restored device draft")
        assertEquals(
            "Restored device draft",
            runBlocking(Dispatchers.IO) { db.counterProjectDao().getProject(id)?.notes },
        )
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(app.getExternalFilesDir(null), "audit14-notes-$dark.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun restorePreservesExistingContentWithoutRestoringEntitlement() {
        status(ProStatus.PRO_PURCHASED)
        val archive = File(app.cacheDir, "audit14-${UUID.randomUUID()}.zip")
        val source = image()
        try {
            val id =
                runBlocking(Dispatchers.IO) {
                    val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Preserved content"))
                    assertTrue(repository.saveProjectNotes(id, "", "Saved notes") is ProjectNotesSaveResult.Saved)
                    assertTrue(repository.updateProjectSecondaryCount(id, 3))
                    assertTrue(
                        reminders.insert(
                            RowReminder(projectId = id, targetRow = 5, message = "Saved reminder"),
                        ) is ReminderMutationResult.Success,
                    )
                    assertTrue(photos.savePhoto(id, Uri.fromFile(source), 0) > 0)
                    app.backupRepository.get().export(Uri.fromFile(archive))
                    id
                }
            status(ProStatus.TRIAL_EXPIRED)
            runBlocking(Dispatchers.IO) {
                val selection = UUID.randomUUID()
                app.backupRepository.get().prepare(Uri.fromFile(archive), selection)
                app.backupRepository.get().restore(selection)
                val restored = db.counterProjectDao().getAllProjectsOnce().single { it.name == "Preserved content" }
                val restoredId = restored.id
                assertTrue(restoredId != id)
                assertTrue(restored.notesCreated && restored.secondaryCounterUsed)
                assertEquals("Saved notes", restored.notes)
                assertEquals(3, restored.secondaryCount)
                assertEquals(1, photos.getPhotosForProject(restoredId).first().size)
                val reminder = reminders.getRemindersForProject(restoredId).first().single()
                assertTrue(
                    reminders.update(
                        restoredId,
                        reminder.copy(message = "Edited after expiry"),
                    ) is ReminderMutationResult.Success,
                )
                assertTrue(
                    repository.saveProjectNotes(
                        restoredId,
                        "Saved notes",
                        "Edited notes",
                    ) is ProjectNotesSaveResult.Saved,
                )
                assertTrue(repository.updateProjectSecondaryCount(restoredId, 0))
                assertTrue(checkNotNull(db.counterProjectDao().getProject(restoredId)).secondaryCounterUsed)
                assertEquals(ReminderMutationResult.FeatureUnavailable, reminders.insert(reminder.copy(id = 0)))
                assertEquals(
                    ProjectCreationResult.LimitReached,
                    repository.createProject("Denied", canCreateAdditionalProjects = true),
                )
                assertFalse(manager.hasFeatureAfterInitialLoad(ProFeature.WIDGET))
                assertFalse(manager.hasFeature(ProFeature.PROGRESS_PHOTOS))
                val fresh = db.counterProjectDao().insert(CounterProjectEntity(name = "Unused restored boundary"))
                assertEquals(
                    ProjectNotesSaveResult.FeatureUnavailable,
                    repository.saveProjectNotes(fresh, "", "Denied"),
                )
                assertFalse(repository.updateProjectSecondaryCount(fresh, 1))
            }
            assertEquals(ProStatus.TRIAL_EXPIRED, manager.proState.value.status)
        } finally {
            archive.delete()
            source.delete()
        }
    }

    private fun status(state: ProStatus) {
        setTestProStatus(state)
        rule.waitUntil(10_000) { manager.proState.value.status == state && manager.initialStateReady.value }
    }

    private fun image(): File {
        val file = File(app.cacheDir, "audit14-${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        return file
    }
}
