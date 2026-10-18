package com.finnvek.knittools.repository

import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteException
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.counter.MainCounterStepSheet
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class MainCounterStepRoomTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private lateinit var database: KnitToolsDatabase
    private lateinit var fixture: RoomCounterTestFixture
    private val databaseName = "main-step-${UUID.randomUUID()}.db"
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val models = ViewModelStore()

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        check(context.packageName == "com.finnvek.knittools.notestest")
        database = Room.databaseBuilder(context, KnitToolsDatabase::class.java, databaseName).build()
        fixture = RoomCounterTestFixture(database, context)
    }

    @After fun teardown() =
        runBlocking {
            withContext(Dispatchers.Main) { models.clear() }
            applicationScope.cancel()
            if (::database.isInitialized) database.close()
            if (::context.isInitialized) context.deleteDatabase(databaseName)
        }

    @Test fun customStepSqlFailurePreservesDatabaseAndEditorThenRetryCommits() =
        runBlocking {
            withTimeout(60_000) {
                val dao = database.counterProjectDao()
                val id =
                    dao.insert(
                        CounterProjectEntity(
                            name = "Step failure",
                            count = 42,
                            stepSize = 1,
                            targetRows = 20,
                            currentStitch = 7,
                            updatedAt = 123L,
                        ),
                    )
                val otherId = dao.insert(CounterProjectEntity(name = "Other", count = 24, stepSize = 3))
                dao.insertHistory(
                    CounterHistoryEntity(projectId = id, action = "increment", previousValue = 41, newValue = 42),
                )
                database.projectCounterDao().insert(
                    ProjectCounterEntity(projectId = id, name = "Linked", count = 9, linkedToMainCounter = true),
                )
                database.sessionDao().insert(
                    SessionEntity(
                        projectId = id,
                        startedAt = 1000L,
                        endedAt = 61000L,
                        startRow = 41,
                        endRow = 42,
                        durationMinutes = 1,
                    ),
                )
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val sqlFailure = CompletableDeferred<Throwable>()
                val holdWrite = AtomicBoolean(false)
                val transactions =
                    object : DatabaseTransactionRunner {
                        override suspend fun <T> run(block: suspend () -> T): T =
                            fixture.transactionRunner.run {
                                if (holdWrite.get()) {
                                    entered.complete(Unit)
                                    release.await()
                                }
                                try {
                                    block()
                                } catch (failure: SQLiteException) {
                                    sqlFailure.complete(failure)
                                    throw failure
                                }
                            }
                    }
                val model = createModel(transactions)
                model.awaitState { it.projectId != null }
                val project = requireNotNull(fixture.counterRepository().getProject(id))
                withContext(Dispatchers.Main) { model.selectProject(project) }
                model.awaitState { it.projectId == id && it.counter.count == 42 }
                val before = readCommittedProject(id)
                val otherBefore = readCommittedProject(otherId)
                val beforeTables = databaseSnapshot()
                withContext(Dispatchers.IO) {
                    database.openHelper.writableDatabase.execSQL(
                        """
                        CREATE TRIGGER fail_main_step AFTER UPDATE OF stepSize ON counter_projects
                        WHEN NEW.id = $id
                        BEGIN SELECT RAISE(ABORT, 'task764_step_write_failure'); END
                        """.trimIndent(),
                    )
                }
                val arguments = InstrumentationRegistry.getArguments()
                val language = arguments.getString("stepLanguage", "fi")
                val width = arguments.getString("stepWidth", "320").toFloat()
                val fontScale = arguments.getString("stepFontScale", "2").toFloat()
                val dark = arguments.getString("stepDark", "true").toBoolean()
                val localized =
                    context.createConfigurationContext(
                        Configuration(context.resources.configuration).apply {
                            setLocale(Locale.forLanguageTag(language))
                        },
                    )
                var visible by mutableStateOf(true)
                val results = mutableStateListOf<Boolean>()
                compose.setContent {
                    CompositionLocalProvider(
                        LocalContext provides localized,
                        LocalResources provides localized.resources,
                        LocalDensity provides Density(context.resources.displayMetrics.widthPixels / width, fontScale),
                    ) {
                        KnitToolsTheme(isDarkTheme = dark) {
                            if (visible) {
                                MainCounterStepSheet(
                                    stepSize = 1,
                                    labelType = MainCounterLabelType.ROWS,
                                    customLabel = null,
                                    onSave = { size, onResult ->
                                        model.setStepSize(size, id) {
                                            results += it
                                            onResult(it)
                                        }
                                    },
                                    onDismiss = { visible = false },
                                )
                            }
                        }
                    }
                }
                val input = compose.onNodeWithTag("main-step-input")
                val save = compose.onNodeWithText(localized.getString(R.string.save))
                compose.onNodeWithTag("main-step-preset_option_2").performScrollTo().performClick()
                input.performScrollTo().performTextReplacement("12")
                compose.onNodeWithText(localized.getString(R.string.cancel)).performScrollTo().performClick()
                compose.runOnIdle { assertFalse(visible) }
                assertEquals(beforeTables, databaseSnapshot())
                compose.runOnIdle { visible = true }
                input.performScrollTo().performTextReplacement("12")
                for (invalid in listOf("", "0", "-1", "1.5", "100")) {
                    input.performScrollTo().performTextReplacement(invalid)
                    save.performScrollTo().assertIsNotEnabled()
                }
                input.performScrollTo().performTextReplacement("12")
                assertEquals(beforeTables, databaseSnapshot())
                holdWrite.set(true)
                save.performScrollTo().performClick()
                entered.await()
                compose.onNodeWithText(localized.getString(R.string.cancel)).assertIsNotEnabled()
                compose.runOnIdle { assertTrue(results.isEmpty()) }
                release.complete(Unit)
                compose.waitUntil(10_000) { results.size == 1 }
                val failureMessage = sqlFailure.await().message.orEmpty()
                assertTrue(failureMessage.contains("task764_step_write_failure"))
                compose.onNodeWithText(localized.getString(R.string.counter_step_save_failed)).assertExists()
                input.performScrollTo().assertTextEquals("12")
                save.performScrollTo().assertIsEnabled()
                compose.runOnIdle {
                    assertEquals(listOf(false), results)
                    assertTrue(visible)
                    assertEquals(1, model.uiState.value.counter.stepSize)
                }
                assertEquals(before, readCommittedProject(id))
                assertEquals(beforeTables, databaseSnapshot())
                withContext(Dispatchers.IO) {
                    database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_main_step")
                }
                save.performClick()
                compose.waitUntil(10_000) { results.size == 2 }
                compose.runOnIdle {
                    assertEquals(listOf(false, true), results)
                    assertFalse(visible)
                }
                val after = readCommittedProject(id)
                assertEquals(before.copy(stepSize = 12, updatedAt = after.updatedAt), after)
                assertEquals(beforeTables - "counter_projects", databaseSnapshot() - "counter_projects")
                assertEquals(otherBefore, readCommittedProject(otherId))
            }
        }

    private suspend fun databaseSnapshot(): Map<String, List<List<String?>>> =
        withContext(Dispatchers.IO) {
            val sql = database.openHelper.readableDatabase
            val tables =
                sql
                    .query(
                        "SELECT name FROM sqlite_master WHERE type = 'table' " +
                            "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'",
                    ).use { cursor ->
                        buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                    }
            tables.associateWith { table ->
                sql.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
                    buildList { while (cursor.moveToNext()) add(List(cursor.columnCount) { cursor.getString(it) }) }
                }
            }
        }

    @Test fun savingSameStepUpdatesOnlyTimestampEvenForCompletedProject() =
        runBlocking {
            val dao = database.counterProjectDao()
            val id =
                dao.insert(
                    CounterProjectEntity(
                        name = "Completed step",
                        count = 12,
                        stepSize = 5,
                        updatedAt = 123L,
                        isCompleted = true,
                        stitchCount = 40,
                        stitchTrackingEnabled = true,
                        currentStitch = 7,
                    ),
                )
            dao.insertHistory(
                CounterHistoryEntity(projectId = id, action = "increment", previousValue = 7, newValue = 12),
            )
            val before = requireNotNull(dao.getProject(id))
            val history = dao.observeCounterHistory(id).first()
            val started = System.currentTimeMillis()

            assertTrue(fixture.counterRepository().updateProjectStepSize(id, 5))

            val after = readCommittedProject(id)
            assertTrue(after.updatedAt in started..System.currentTimeMillis())
            assertEquals(before.copy(updatedAt = after.updatedAt), after)
            assertEquals(history, dao.observeCounterHistory(id).first())
        }

    @Test fun lateCommittedResultPreservesCountObservedFromRoom() =
        runBlocking {
            withTimeout(15_000) {
                val id = database.counterProjectDao().insert(CounterProjectEntity(name = "Late step", count = 12))
                val (gate, model, result, job) = committedStepChange(id)
                assertFalse(result.isCompleted)

                assertTrue(fixture.counterRepository().applyMainCounterChange(id, MainCounterChange.Increment))
                model.awaitState { it.counter.count == 17 }
                gate.release.complete(Unit)
                job.join()

                assertTrue(result.await())
                assertEquals(17, model.uiState.value.counter.count)
                assertEquals(5, model.uiState.value.counter.stepSize)
                assertEquals(17, readCommittedProject(id).count)
            }
        }

    @Test fun widgetCommitBeforeStepTransactionRetainsCountAndAllWidgetSideEffects() =
        runBlocking {
            withTimeout(15_000) {
                val id = seedRacingProject()
                val otherBefore = database.counterProjectDao().getAllProjectsOnce().single { it.id != id }
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val paused =
                    fixture.counterRepository(
                        transactions =
                            object : DatabaseTransactionRunner {
                                override suspend fun <T> run(block: suspend () -> T): T {
                                    entered.complete(Unit)
                                    release.await()
                                    return fixture.transactionRunner.run(block)
                                }
                            },
                    )
                val save = async { paused.updateProjectStepSize(id, 5) }
                entered.await()
                assertFalse(save.isCompleted)
                assertTrue(fixture.counterRepository().applyWidgetCountChange(id, increment = true))
                assertWidgetChange(id, expectedCount = 14, expectedStep = 2, expectedLinked = 6)
                val afterWidget = readCommittedProject(id)
                val tablesAfterWidget = databaseSnapshot()

                release.complete(Unit)
                assertTrue(save.await())

                val after = readCommittedProject(id)
                assertEquals(afterWidget.copy(stepSize = 5, updatedAt = after.updatedAt), after)
                assertEquals(tablesAfterWidget - "counter_projects", databaseSnapshot() - "counter_projects")
                assertEquals(2, database.counterProjectDao().getAllProjectsOnce().size)
                val other = database.counterProjectDao().getAllProjectsOnce().single { it.id != id }
                assertEquals(otherBefore, other)
            }
        }

    @Test fun queuedPresetCustomAndRepeatedSavesCommitWithoutCountSideEffects() =
        runBlocking {
            withTimeout(15_000) {
                val id = database.counterProjectDao().insert(CounterProjectEntity(name = "Queued", count = 12))
                val gate = StepCommitGate(id)
                val model = createModel(gate)
                model.awaitState { it.projectId == id }
                val before = databaseSnapshot()
                val beforeProject = readCommittedProject(id)
                val preset = CompletableDeferred<Boolean>()
                val custom = CompletableDeferred<Boolean>()
                val repeated = CompletableDeferred<Boolean>()
                val first = withContext(Dispatchers.Main) { model.setStepSize(5, id) { preset.complete(it) } }
                gate.committed.await()
                val second = withContext(Dispatchers.Main) { model.setStepSize(12, id) { custom.complete(it) } }
                val third = withContext(Dispatchers.Main) { model.setStepSize(12, id) { repeated.complete(it) } }
                assertFalse(preset.isCompleted)
                assertFalse(custom.isCompleted)
                assertFalse(repeated.isCompleted)
                assertEquals(5, readCommittedProject(id).stepSize)
                gate.release.complete(Unit)
                first.join()
                second.join()
                third.join()

                assertTrue(preset.await())
                assertTrue(custom.await())
                assertTrue(repeated.await())
                val after = readCommittedProject(id)
                assertEquals(12, after.count)
                assertEquals(12, after.stepSize)
                assertEquals(beforeProject.copy(stepSize = 12, updatedAt = after.updatedAt), after)
                assertEquals(before - "counter_projects", databaseSnapshot() - "counter_projects")
                assertEquals(12, model.uiState.value.counter.stepSize)
            }
        }

    @Test fun legacyPositiveStepAboveEditorLimitStillCountsButNewSavesValidateRange() =
        runBlocking {
            val dao = database.counterProjectDao()
            val id = dao.insert(CounterProjectEntity(name = "Legacy", count = 12, stepSize = 125, targetRows = 20))
            val repository = fixture.counterRepository()
            assertTrue(repository.applyWidgetCountChange(id, increment = true))
            assertEquals(137, readCommittedProject(id).count)
            assertEquals(125, readCommittedProject(id).stepSize)
            val before = databaseSnapshot()
            for (invalid in listOf(Int.MIN_VALUE, -1, 0, 100, Int.MAX_VALUE)) {
                assertFalse(repository.updateProjectStepSize(id, invalid))
            }
            assertEquals(before, databaseSnapshot())
            for (valid in listOf(1, 99)) {
                assertTrue(repository.updateProjectStepSize(id, valid))
                assertEquals(valid, readCommittedProject(id).stepSize)
                assertEquals(137, readCommittedProject(id).count)
                assertEquals(before - "counter_projects", databaseSnapshot() - "counter_projects")
            }
        }

    @Test fun widgetWaitsBetweenStepReadAndWriteThenUsesCommittedStep() =
        runBlocking {
            withTimeout(15_000) {
                val id = seedRacingProject()
                val before = databaseSnapshot()
                val read = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val dao = database.counterProjectDao()
                val beforeProject = requireNotNull(dao.getProject(id))
                val otherBefore = dao.getAllProjectsOnce().single { it.id != id }
                val pausedDao =
                    object : CounterProjectDao by dao {
                        override suspend fun getProject(id: Long): CounterProjectEntity? {
                            val row = dao.getProject(id)
                            read.complete(Unit)
                            release.await()
                            return row
                        }
                    }
                val save = async { fixture.counterRepository(projectDao = pausedDao).updateProjectStepSize(id, 5) }
                read.await()
                val widget =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        fixture.counterRepository().applyWidgetCountChange(id, increment = true)
                    }
                assertFalse(save.isCompleted)
                assertFalse(widget.isCompleted)
                release.complete(Unit)

                assertTrue(save.await())
                assertTrue(widget.await())
                assertWidgetChange(id, expectedCount = 17, expectedStep = 5, expectedLinked = 9)
                val after = readCommittedProject(id)
                assertEquals(
                    beforeProject.copy(count = 17, stepSize = 5, currentStitch = 0, updatedAt = after.updatedAt),
                    after,
                )
                val changedTables = setOf("counter_projects", "counter_history", "project_counters")
                assertEquals(before - changedTables, databaseSnapshot() - changedTables)
                assertEquals(otherBefore, dao.getAllProjectsOnce().single { it.id != id })
            }
        }

    private suspend fun seedRacingProject(): Long {
        val id =
            database.counterProjectDao().insert(
                CounterProjectEntity(
                    name = "Racing step",
                    count = 12,
                    stepSize = 2,
                    targetRows = 10,
                    stitchCount = 40,
                    stitchTrackingEnabled = true,
                    currentStitch = 7,
                ),
            )
        database.counterProjectDao().insert(CounterProjectEntity(name = "Other", count = 24, stepSize = 3))
        database.projectCounterDao().insert(
            ProjectCounterEntity(projectId = id, name = "Linked", count = 4, linkedToMainCounter = true),
        )
        return id
    }

    private suspend fun assertWidgetChange(
        id: Long,
        expectedCount: Int,
        expectedStep: Int,
        expectedLinked: Int,
    ) {
        val project = readCommittedProject(id)
        assertEquals(expectedCount, project.count)
        assertEquals(expectedStep, project.stepSize)
        assertEquals(10, project.targetRows)
        assertEquals(0, project.currentStitch)
        val history =
            database
                .counterProjectDao()
                .observeCounterHistory(id)
                .first()
                .single()
        assertEquals("increment", history.action)
        assertEquals(12, history.previousValue)
        assertEquals(expectedCount, history.newValue)
        val linked =
            database
                .projectCounterDao()
                .getCountersForProject(id)
                .first()
                .single()
        assertEquals(expectedLinked, linked.count)
    }

    @Test fun lateCommittedResultCannotUpdateAnotherProject() =
        runBlocking {
            checkStaleSelection(reopen = false)
        }

    @Test fun lateCommittedResultCannotUpdateReopenedSelection() =
        runBlocking {
            checkStaleSelection(reopen = true)
        }

    private suspend fun checkStaleSelection(reopen: Boolean) =
        withTimeout(15_000) {
            val dao = database.counterProjectDao()
            val id = dao.insert(CounterProjectEntity(name = "Original", count = 12))
            val (gate, model, result, job) = committedStepChange(id)
            val otherId = dao.insert(CounterProjectEntity(name = "Other", count = 24, stepSize = 3))
            val repository = fixture.counterRepository()
            val other = requireNotNull(repository.getProject(otherId))
            withContext(Dispatchers.Main) { model.selectProject(other) }
            model.awaitState { it.projectId == otherId }
            if (reopen) {
                val original = requireNotNull(repository.getProject(id))
                withContext(Dispatchers.Main) { model.selectProject(original) }
                model.awaitState { it.projectId == id }
                assertTrue(repository.updateProjectStepSize(id, 2))
                model.awaitState { it.counter.stepSize == 2 }
            }
            gate.release.complete(Unit)
            job.join()

            assertFalse(result.await())
            assertEquals(if (reopen) id else otherId, model.uiState.value.projectId)
            assertEquals(if (reopen) 2 else 3, model.uiState.value.counter.stepSize)
            assertEquals(if (reopen) 12 else 24, model.uiState.value.counter.count)
            assertEquals(if (reopen) 2 else 5, readCommittedProject(id).stepSize)
            assertEquals(3, readCommittedProject(otherId).stepSize)
        }

    @Test fun cancellationAfterCommitDoesNotRollbackOrReportFailureAndRetryWorks() =
        runBlocking {
            withTimeout(15_000) {
                val id =
                    database.counterProjectDao().insert(
                        CounterProjectEntity(name = "Cancelled result", count = 12),
                    )
                val (gate, model, result, job) = committedStepChange(id)

                job.cancelAndJoin()

                assertTrue(job.isCancelled)
                assertFalse(result.isCompleted)
                val persisted = readCommittedProject(id)
                assertEquals(5, persisted.stepSize)
                assertEquals(12, persisted.count)
                model.awaitState { it.counter.stepSize == 5 }
                val retry = CompletableDeferred<Boolean>()
                withContext(Dispatchers.Main) { model.setStepSize(2, id) { retry.complete(it) } }.join()
                assertTrue(retry.await())
                assertEquals(2, readCommittedProject(id).stepSize)
            }
        }

    private data class PendingStepChange(
        val gate: StepCommitGate,
        val model: CounterViewModel,
        val result: CompletableDeferred<Boolean>,
        val job: Job,
    )

    private suspend fun committedStepChange(id: Long): PendingStepChange {
        val gate = StepCommitGate(id)
        val model = createModel(gate)
        model.awaitState { it.projectId == id }
        val result = CompletableDeferred<Boolean>()
        val job = withContext(Dispatchers.Main) { model.setStepSize(5, id) { result.complete(it) } }
        gate.committed.await()
        assertEquals(5, readCommittedProject(id).stepSize)
        return PendingStepChange(gate, model, result, job)
    }

    private suspend fun CounterViewModel.awaitState(predicate: (CounterUiState) -> Boolean) = uiState.first(predicate)

    private suspend fun readCommittedProject(id: Long): CounterProjectEntity {
        val reader = Room.databaseBuilder(context, KnitToolsDatabase::class.java, databaseName).build()
        return try {
            requireNotNull(reader.counterProjectDao().getProject(id))
        } finally {
            reader.close()
        }
    }

    private inner class StepCommitGate(
        private val projectId: Long,
    ) : DatabaseTransactionRunner {
        val committed = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        override suspend fun <T> run(block: suspend () -> T): T {
            var changed = false
            val result =
                fixture.transactionRunner.run {
                    val before = database.counterProjectDao().getProject(projectId)?.stepSize
                    val value = block()
                    changed = before != database.counterProjectDao().getProject(projectId)?.stepSize
                    value
                }
            if (changed && !committed.isCompleted) {
                committed.complete(Unit)
                release.await()
            }
            return result
        }
    }

    private suspend fun createModel(transactions: DatabaseTransactionRunner): CounterViewModel =
        withContext(Dispatchers.Main) {
            val pro = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager()
            fixture
                .counterViewModel(
                    pro,
                    applicationScope,
                    transactions = transactions,
                ).also { models.put("main-step", it) }
        }
}
