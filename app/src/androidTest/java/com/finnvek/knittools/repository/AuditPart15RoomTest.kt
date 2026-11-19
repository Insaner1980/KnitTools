package com.finnvek.knittools.repository

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.time.AndroidSessionTimeSource
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.ui.screens.insights.InsightsViewModel
import com.finnvek.knittools.ui.screens.pattern.PatternImageImportPhase
import com.finnvek.knittools.ui.screens.pattern.PatternImageImportViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Part 15 device boundaries: real Room, real repositories, synthetic entitlement inputs. */
class AuditPart15RoomTest {
    private val app = ApplicationProvider.getApplicationContext<App>()
    private val pro get() = app.proManager.get()
    private lateinit var db: KnitToolsDatabase
    private lateinit var fixture: RoomCounterTestFixture
    private val models = ViewModelStore()
    private val applicationScope = CoroutineScope(Dispatchers.IO)
    private val onboardingStateRepository = OnboardingStateRepository(MemoryOnboardingPreferences())

    @Before fun setup() =
        runBlocking {
            check(app.packageName == "com.finnvek.knittools.activepolicytest" && ProPolicyTestControl.isActive)
            db = Room.inMemoryDatabaseBuilder(app, KnitToolsDatabase::class.java).build()
            fixture = RoomCounterTestFixture(db, app)
            status(ProStatus.TRIAL_NOT_STARTED)
        }

    @After fun close() =
        runBlocking {
            applicationScope.cancel()
            withContext(Dispatchers.Main) { models.clear() }
            db.close()
        }

    @Test fun committedCreationAttemptReusesProjectButNewAttemptWithSameNameSucceeds() =
        runBlocking {
            val repository = fixture.counterRepository()
            val first =
                repository.createProject("Blanket", canCreateAdditionalProjects = false, attemptId = "first")
                    as ProjectCreationResult.Created
            assertTrue(first.wasFirstProject)
            assertEquals(1, repository.getProjectCount())

            assertEquals(
                first,
                repository.createProject("Blanket", canCreateAdditionalProjects = false, attemptId = "first"),
            )
            assertEquals(1, repository.getProjectCount())

            status(ProStatus.PRO_PURCHASED)
            val second =
                repository.createProject("Blanket", canCreateAdditionalProjects = true, attemptId = "second")
                    as ProjectCreationResult.Created
            assertEquals(2, repository.getProjectCount())
            assertEquals("Blanket (2)", db.counterProjectDao().getProject(second.projectId)?.name)
        }

    @Test fun allSevenGatesUseRealPolicyAndCounterYarnWritesAcrossFourStates() =
        runBlocking {
            val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Gate matrix"))
            val counters = counters()
            val yarn = yarn()
            for (state in ProStatus.entries) {
                status(state)
                val allowed = state == ProStatus.TRIAL_ACTIVE || state == ProStatus.PRO_PURCHASED
                for (feature in features) {
                    assertEquals("$state $feature", allowed, pro.hasFeature(feature))
                    assertTrue(ProState(state).hasFeature(feature, debugUnlockAllFeatures = true))
                    assertEquals(allowed, ProState(state).isPro)
                }
                for (draft in drafts(id)) {
                    val result = counters.addCounter(draft)
                    assertEquals("$state ${draft.counterType}", allowed, result is ProjectCounterMutationResult.Success)
                    if (!allowed) assertEquals(ProjectCounterMutationResult.FeatureUnavailable, result)
                }
                assertEquals(allowed, yarn.saveCard(YarnCard(yarnName = "Gate $state")) != null)
            }
            assertEquals(6, counters.getCountersForProject(id).first().size)
            assertEquals(2, yarn.getAllCards().first().size)
        }

    @Test fun admittedCameraImageCreatesRealPdfAfterExpiryAndExistingPdfRemainsReadable() =
        runBlocking {
            status(ProStatus.TRIAL_ACTIVE)
            val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Pending camera"))
            val model =
                withContext(Dispatchers.Main) {
                    PatternImageImportViewModel(
                        PatternDocumentStorage(Dispatchers.IO),
                        fixture.counterRepository(),
                        app,
                        Dispatchers.IO,
                        SavedStateHandle(),
                        pro,
                        onboardingStateRepository,
                        applicationScope,
                    ).also {
                        models.put("camera", it)
                    }
                }
            assertTrue(pro.hasFeature(ProFeature.PATTERN_CAMERA_SCAN))
            withContext(Dispatchers.Main) { model.authorizeCameraCapture(id) }
            val target = checkNotNull(model.createCameraCaptureTarget(id))
            Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).let { bitmap ->
                target.first.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                bitmap.recycle()
            }
            status(ProStatus.TRIAL_EXPIRED)
            assertFalse(pro.hasFeature(ProFeature.PATTERN_CAMERA_SCAN))
            withContext(Dispatchers.Main) { model.acceptCameraCapture(id, target.second, target.first) }
            withTimeout(10_000) { model.uiState.first { it.phase == PatternImageImportPhase.READY } }
            withContext(Dispatchers.Main) { model.createPatternPdf() }
            withTimeout(20_000) { model.uiState.first { it.closeReady } }
            val documents =
                db.openHelper.readableDatabase.query(
                    "SELECT localPdfUri FROM project_documents WHERE projectId = ?",
                    arrayOf(id.toString()),
                )
            val uri =
                documents.use {
                    assertTrue(it.moveToFirst())
                    it.getString(0)
                }
            val descriptor = checkNotNull(app.contentResolver.openFileDescriptor(Uri.parse(uri), "r"))
            android.graphics.pdf
                .PdfRenderer(descriptor)
                .use { renderer -> assertEquals(1, renderer.pageCount) }
            fixture.counterRepository().deleteProject(id)
        }

    @Test fun expiryAtTransactionAdmissionRejectsAllCounterTypesAndYarn() =
        runBlocking {
            withTimeout(30_000) {
                val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Late admission"))
                for (draft in drafts(id)) {
                    status(ProStatus.TRIAL_ACTIVE)
                    val entered = CompletableDeferred<Unit>()
                    val release = CompletableDeferred<Unit>()
                    val job =
                        async {
                            counters(
                                fixture.transactionRunner.pauseBeforeTransaction(entered, release),
                            ).addCounter(draft)
                        }
                    entered.await()
                    status(ProStatus.TRIAL_EXPIRED)
                    release.complete(Unit)
                    assertEquals(ProjectCounterMutationResult.FeatureUnavailable, job.await())
                }
                status(ProStatus.TRIAL_ACTIVE)
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val job =
                    async {
                        yarn(
                            fixture.transactionRunner.pauseBeforeTransaction(entered, release),
                        ).saveCard(YarnCard(yarnName = "Late"))
                    }
                entered.await()
                status(ProStatus.TRIAL_EXPIRED)
                release.complete(Unit)
                assertNull(job.await())
                assertTrue(counters().getCountersForProject(id).first().isEmpty())
                assertTrue(yarn().getAllCards().first().isEmpty())
            }
        }

    @Test fun expiryPreservesEditingResetAndDeletionOfAllExistingCounterTypesAndYarn() =
        runBlocking {
            status(ProStatus.PRO_PURCHASED)
            val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Existing"))
            val counters = counters()
            for (draft in drafts(id)) assertTrue(counters.addCounter(draft) is ProjectCounterMutationResult.Success)
            val yarn = yarn()
            val cardId = checkNotNull(yarn.saveCard(YarnCard(yarnName = "Saved", quantityInStash = 3)))
            status(ProStatus.TRIAL_EXPIRED)
            for (counter in counters.getCountersForProject(id).first()) {
                assertTrue(counters.renameCounter(id, counter.id, "Edited") is ProjectCounterMutationResult.Success)
                assertTrue(counters.incrementCounter(counter) is ProjectCounterMutationResult.Success)
                assertTrue(counters.resetCounter(id, counter.id) is ProjectCounterMutationResult.Success)
                assertEquals(0, db.projectCounterDao().getCounter(counter.id)?.count)
                assertTrue(counters.deleteCounter(id, counter.id) is ProjectCounterMutationResult.Success)
            }
            assertNotNull(
                yarn.saveCard(checkNotNull(yarn.getCard(cardId)).copy(yarnName = "Edited", quantityInStash = 2)),
            )
            assertEquals("Edited", yarn.getCard(cardId)?.yarnName)
            assertEquals(3, yarn.getCard(cardId)?.quantityInStash)
            assertTrue(yarn.updateQuantity(cardId, 2))
            assertEquals(2, yarn.getCard(cardId)?.quantityInStash)
            assertTrue(yarn.deleteCard(cardId))
            assertNull(yarn.getCard(cardId))
        }

    @Test fun backupRestorePreservesCountersYarnAndHistoryWithoutRestoringPro() =
        runBlocking(Dispatchers.IO) {
            val live = app.database.get()
            live.clearAllTables()
            val transactions =
                com.finnvek.knittools.data.local
                    .RoomDatabaseTransactionRunner(live)
            val counters =
                ProjectCounterRepository(live.projectCounterDao(), live.counterProjectDao(), pro, transactions)
            val yarn =
                YarnCardRepository(
                    live.yarnCardDao(),
                    live.counterProjectDao(),
                    app,
                    transactions,
                    Dispatchers.IO,
                    proManager = pro,
                )
            val archive = java.io.File(app.cacheDir, "audit15-backup.zip")
            try {
                status(ProStatus.PRO_PURCHASED)
                val id = live.counterProjectDao().insert(CounterProjectEntity(name = "Restore gates"))
                drafts(id).forEach { assertTrue(counters.addCounter(it) is ProjectCounterMutationResult.Success) }
                assertNotNull(yarn.saveCard(YarnCard(yarnName = "Restore yarn")))
                val now = System.currentTimeMillis()
                live.sessionDao().insert(
                    SessionEntity(
                        projectId = id,
                        startedAt = now - 60_000,
                        endedAt = now,
                        startRow = 0,
                        endRow = 3,
                        durationMinutes = 1,
                    ),
                )
                app.backupRepository.get().export(Uri.fromFile(archive))
                status(ProStatus.TRIAL_EXPIRED)
                val selection = java.util.UUID.randomUUID()
                app.backupRepository.get().prepare(Uri.fromFile(archive), selection)
                app.backupRepository.get().restore(selection)
                val restored = live.counterProjectDao().getAllProjectsOnce().single()
                assertEquals(3, counters.getCountersForProject(restored.id).first().size)
                assertEquals(1, yarn.getAllCards().first().size)
                assertEquals(
                    1,
                    live
                        .sessionDao()
                        .getAllSessions(null)
                        .first()
                        .size,
                )
                assertEquals(ProStatus.TRIAL_EXPIRED, pro.proState.value.status)
                assertEquals(
                    ProjectCounterMutationResult.FeatureUnavailable,
                    counters.addCounter(drafts(restored.id).first()),
                )
                assertNull(yarn.saveCard(YarnCard(yarnName = "Rejected")))
                val saved = yarn.getAllCards().first().single()
                assertNotNull(yarn.saveCard(saved.copy(yarnName = "Still editable")))
            } finally {
                archive.delete()
                live.clearAllTables()
            }
        }

    @Test fun insightsChartsAndStreakDisappearOnExpiryWhileBasicRoomHistoryRemains() =
        runBlocking {
            val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Insights"))
            val now = System.currentTimeMillis()
            db.sessionDao().insert(
                SessionEntity(
                    projectId = id,
                    startedAt = now - 60_000,
                    endedAt = now,
                    startRow = 0,
                    endRow = 3,
                    durationMinutes = 1,
                ),
            )
            val model =
                withContext(Dispatchers.Main) {
                    InsightsViewModel(
                        fixture.counterRepository(),
                        pro,
                        Dispatchers.IO,
                        SavedStateHandle(),
                        onboardingStateRepository,
                        applicationScope,
                    ).also { models.put("insights", it) }
                }
            val collection = launch { model.uiState.collect() }
            try {
                for (state in listOf(
                    ProStatus.TRIAL_NOT_STARTED,
                    ProStatus.TRIAL_ACTIVE,
                    ProStatus.TRIAL_EXPIRED,
                    ProStatus.PRO_PURCHASED,
                )) {
                    status(state)
                    val allowed = state == ProStatus.TRIAL_ACTIVE || state == ProStatus.PRO_PURCHASED
                    val ui =
                        withTimeout(10_000) {
                            model.uiState.first {
                                !it.isLoading &&
                                    it.isPro == allowed &&
                                    it.canUseStreak == allowed &&
                                    it.totalRows == 3L
                            }
                        }
                    assertEquals(1, ui.totalMinutes)
                    assertEquals(allowed, ui.chartBuckets.isNotEmpty())
                    assertEquals(if (allowed) 1 else 0, ui.currentStreak)
                    assertEquals(
                        1,
                        db
                            .sessionDao()
                            .getAllSessions(null)
                            .first()
                            .size,
                    )
                }
            } finally {
                collection.cancel()
            }
        }

    @Test fun widgetBeforeQueuedUndoUsesLatestRoomHistoryAndPreservesCustomStep() =
        runBlocking {
            withTimeout(15_000) {
                val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Undo", count = 12, stepSize = 7))
                val repository = fixture.counterRepository()
                assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val waiting =
                    fixture.counterRepository(
                        transactions = fixture.transactionRunner.pauseBeforeTransaction(entered, release),
                    )
                val undo = async { waiting.applyMainCounterChange(id, MainCounterChange.Undo) }
                entered.await()
                assertTrue(repository.applyWidgetCountChange(id, increment = true))
                assertEquals(26, db.counterProjectDao().getProject(id)?.count)
                release.complete(Unit)
                assertTrue(undo.await())
                assertEquals(19, db.counterProjectDao().getProject(id)?.count)
                assertEquals(7, db.counterProjectDao().getProject(id)?.stepSize)
                assertEquals(1, repository.observeCounterHistory(id).first().size)
            }
        }

    @Test fun clampedWidgetDecrementResetsStitchesAndCheckpointsSessionOnce() =
        runBlocking {
            val id =
                db.counterProjectDao().insert(
                    CounterProjectEntity(
                        name = "Clamp",
                        count = 2,
                        stepSize = 5,
                        stitchCount = 30,
                        currentStitch = 9,
                        stitchTrackingEnabled = true,
                    ),
                )
            val linked =
                db.projectCounterDao().insert(
                    ProjectCounterEntity(projectId = id, name = "Linked", count = 4, linkedToMainCounter = true),
                )
            val repository = fixture.counterRepository(sessionTimeSource = AndroidSessionTimeSource(app))
            repository.startSession(id)
            assertNotNull(db.sessionDao().getActiveSession())
            assertTrue(repository.applyWidgetCountChange(id, increment = false))
            val row = checkNotNull(db.counterProjectDao().getProject(id))
            assertEquals(0, row.count)
            assertEquals(0, row.currentStitch)
            assertEquals(2, db.projectCounterDao().getCounter(linked)?.count)
            val history = repository.observeCounterHistory(id).first().single()
            assertEquals(2, history.previousValue)
            assertEquals(0, history.newValue)
            val session = checkNotNull(db.sessionDao().getActiveSession())
            assertEquals(0, session.lastObservedRow)
            assertEquals(2, session.startRow)
            assertEquals(0, session.trustedRowsWorked)
            assertFalse(repository.applyWidgetCountChange(id, increment = false))
            assertEquals(1, repository.observeCounterHistory(id).first().size)
        }

    @Test fun customStepChecksOnlyFinalReminderAndResetDoesNotWrapLinkedRepeatingCounter() =
        runBlocking {
            val id = db.counterProjectDao().insert(CounterProjectEntity(name = "Reminder", count = 2, stepSize = 7))
            val repository = fixture.counterRepository()
            assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
            val row = checkNotNull(db.counterProjectDao().getProject(id))
            assertEquals(9, row.count)
            val reminders =
                listOf(
                    RowReminder(projectId = id, targetRow = 4, message = "Four"),
                    RowReminder(projectId = id, targetRow = 6, message = "Six"),
                    RowReminder(projectId = id, targetRow = 3, repeatInterval = 3, message = "Repeat"),
                )
            assertEquals(listOf("Repeat"), ReminderLogic.activeReminders(reminders, row.count).map { it.message })
            val linked =
                db.projectCounterDao().insert(
                    ProjectCounterEntity(
                        projectId = id,
                        name = "Repeating",
                        count = 3,
                        counterType = ProjectCounterType.REPEATING.persistedValue,
                        repeatAt = 4,
                        linkedToMainCounter = true,
                    ),
                )
            assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Reset))
            assertEquals(0, db.counterProjectDao().getProject(id)?.count)
            assertEquals(0, db.projectCounterDao().getCounter(linked)?.count)
            assertEquals(7, db.counterProjectDao().getProject(id)?.stepSize)
        }

    private fun counters(transactions: DatabaseTransactionRunner = fixture.transactionRunner) =
        ProjectCounterRepository(db.projectCounterDao(), db.counterProjectDao(), pro, transactions)

    private fun yarn(transactions: DatabaseTransactionRunner = fixture.transactionRunner) =
        YarnCardRepository(
            db.yarnCardDao(),
            db.counterProjectDao(),
            app,
            transactions,
            Dispatchers.IO,
            proManager = pro,
        )

    private suspend fun status(state: ProStatus) {
        setTestProStatus(state)
        withTimeout(10_000) {
            pro.initialStateReady.first { it }
            pro.proState.first { it.status == state }
        }
    }

    private fun drafts(id: Long) =
        listOf(
            ProjectCounter(projectId = id, name = "Extra"),
            ProjectCounter(
                projectId = id,
                name = "Shape",
                counterType = ProjectCounterType.SHAPING,
                startingStitches = 20,
                stitchChange = -1,
                shapeEveryN = 2,
            ),
            ProjectCounter(
                projectId = id,
                name = "Section",
                counterType = ProjectCounterType.REPEAT_SECTION,
                repeatStartRow = 1,
                repeatEndRow = 4,
                totalRepeats = 3,
            ),
        )

    private val features =
        listOf(
            ProFeature.MULTIPLE_COUNTERS,
            ProFeature.SHAPING_COUNTER,
            ProFeature.REPEAT_SECTION,
            ProFeature.PATTERN_CAMERA_SCAN,
            ProFeature.INSIGHTS_CHARTS,
            ProFeature.STREAK,
            ProFeature.UNLIMITED_YARN,
        )
}
