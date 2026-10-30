package com.finnvek.knittools.repository

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.ui.screens.counter.CounterScreenActions
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.counter.ProjectContentHost
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/** Real Room and production Compose callbacks, confined to the offline policy-test package. */
class ExtraCounterAdmissionRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private lateinit var database: KnitToolsDatabase
    private lateinit var fixture: RoomCounterTestFixture
    private lateinit var pro: ProManager
    private val models = ViewModelStore()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Before fun setup() =
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            check(context.packageName == "com.finnvek.knittools.activepolicytest" && ProPolicyTestControl.isActive)
            database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
            fixture = RoomCounterTestFixture(database, context)
            pro = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager()
            ProPolicyTestControl.setPurchased(false)
            ProPolicyTestControl.setTrialState(TrialState(isActive = true, hasStarted = true, daysRemaining = 1))
            withTimeout(10_000) { pro.proState.first { it.isPro } }
            Unit
        }

    @After fun teardown() =
        runBlocking {
            withContext(Dispatchers.Main) { models.clear() }
            applicationScope.cancel()
            if (::database.isInitialized) database.close()
            if (ProPolicyTestControl.isActive) ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
        }

    @Test fun completionBeforeAdmissionRejectsExtraCounterCreation() =
        runBlocking {
            withTimeout(30_000) {
                val id = database.counterProjectDao().insert(CounterProjectEntity(name = "Finishing", count = 12))
                val other = database.counterProjectDao().insert(CounterProjectEntity(name = "Other", count = 37))
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val repository = counterRepository(fixture.transactionRunner.pauseBeforeTransaction(entered, release))
                val creation =
                    async {
                        repository.addCounter(
                            ProjectCounter(
                                projectId = id,
                                name = "  Finished detail  ",
                                repeatAt = 99,
                                shapeEveryN = 5,
                            ),
                        )
                    }
                entered.await()
                fixture.counterRepository().archiveProject(id, 123456L)
                val before = databaseSnapshotExceptExtraCounters()
                assertTrue(requireNotNull(database.counterProjectDao().getProject(id)).isCompleted)
                release.complete(Unit)
                assertEquals(ProjectCounterMutationResult.StaleAction, creation.await())
                assertTrue(repository.getCountersForProject(id).first().isEmpty())
                assertTrue(repository.getCountersForProject(other).first().isEmpty())
                assertEquals(before, databaseSnapshotExceptExtraCounters())
            }
        }

    @Test fun activeProjectCreationNormalizesOnlyExtraCounterMetadata() =
        runBlocking {
            val id = database.counterProjectDao().insert(CounterProjectEntity(name = "Active", count = 12))
            val repository = counterRepository()
            val before = databaseSnapshotExceptExtraCounters()
            assertTrue(
                repository.addCounter(
                    ProjectCounter(projectId = id, name = "  Detail  ", repeatAt = 99, shapeEveryN = 5),
                ) is ProjectCounterMutationResult.Success,
            )
            val stored = repository.getCountersForProject(id).first().single()
            assertEquals("Detail", stored.name)
            assertEquals(ProjectCounterType.COUNT_UP, stored.counterType)
            assertEquals(null, stored.repeatAt)
            assertEquals(null, stored.shapeEveryN)
            assertEquals(before, databaseSnapshotExceptExtraCounters())
        }

    @Test fun trialExpiryBeforeAdmissionRejectsExtraCounterCreation() =
        runBlocking {
            withTimeout(30_000) {
                val id = database.counterProjectDao().insert(CounterProjectEntity(name = "Trial boundary"))
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val repository = counterRepository(fixture.transactionRunner.pauseBeforeTransaction(entered, release))
                val before = databaseSnapshotExceptExtraCounters()
                val creation = async { repository.addCounter(ProjectCounter(projectId = id, name = "Rejected")) }
                entered.await()
                ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
                pro.proState.first { !it.isPro }
                release.complete(Unit)
                assertEquals(ProjectCounterMutationResult.FeatureUnavailable, creation.await())
                assertTrue(repository.getCountersForProject(id).first().isEmpty())
                assertEquals(before, databaseSnapshotExceptExtraCounters())
            }
        }

    @Test fun oldRepeatingDialogCallbackCannotCreateInAnotherProject() =
        runBlocking {
            withTimeout(60_000) {
                val first = database.counterProjectDao().insert(CounterProjectEntity(name = "First", count = 12))
                val second = database.counterProjectDao().insert(CounterProjectEntity(name = "Second", count = 37))
                val attempts = AtomicInteger()
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val transactions =
                    object : DatabaseTransactionRunner {
                        override suspend fun <T> run(block: suspend () -> T): T {
                            attempts.incrementAndGet()
                            entered.complete(Unit)
                            release.await()
                            return fixture.transactionRunner.run(block)
                        }
                    }
                val model = createModel(counterRepository(transactions))
                model.uiState.first { !it.isRestoringProject }
                select(model, first)
                model.uiState.first { it.projectId == first && it.canUseMultipleCounters }
                val localized = showHost(model)
                val oldSave = prepareRepeatingDialog(localized, "First draft")
                val secondProject = requireNotNull(fixture.counterRepository().getProject(second))
                // Deliver the actual old button callback before Compose can close the old overlay.
                withContext(Dispatchers.Main) {
                    model.selectProject(secondProject)
                    assertEquals(second, model.uiState.value.projectId)
                    oldSave()
                }
                compose.waitForIdle()
                // The removed button is no longer callable through the live semantics tree.
                compose.onNodeWithText(localized.getString(R.string.save)).assertDoesNotExist()
                if (attempts.get() != 0) {
                    release.complete(Unit)
                    compose.waitForIdle()
                    val misplaced = counterRepository().getCountersForProject(second).first()
                    assertTrue("Old First draft was stored in Second: $misplaced", misplaced.isEmpty())
                }
                assertEquals("A stale dialog must never enter a repository write", 0, attempts.get())
                assertTrue(counterRepository().getCountersForProject(first).first().isEmpty())
                assertTrue(counterRepository().getCountersForProject(second).first().isEmpty())

                val before = databaseSnapshotExceptExtraCounters()
                val currentSave = prepareRepeatingDialog(localized, "Second draft")
                compose.runOnIdle { currentSave() }
                entered.await()
                assertEquals("The accepted save must enter one transaction", 1, attempts.get())
                select(model, first)
                release.complete(Unit)
                val stored = counterRepository().getCountersForProject(second).first { it.isNotEmpty() }.single()
                assertEquals("Second draft", stored.name)
                assertEquals(ProjectCounterType.REPEATING, stored.counterType)
                assertEquals(4, stored.repeatAt)
                assertTrue(counterRepository().getCountersForProject(first).first().isEmpty())
                assertEquals(before, databaseSnapshotExceptExtraCounters())
            }
        }

    private fun showHost(model: CounterViewModel): Context {
        val args = InstrumentationRegistry.getArguments()
        val width = args.getString("counterWidth", "320").toFloat()
        val fontScale = args.getString("counterFontScale", "2").toFloat()
        val localized =
            context.createConfigurationContext(
                Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) },
            )
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalDensity provides Density(context.resources.displayMetrics.widthPixels / width, fontScale),
            ) {
                KnitToolsTheme(isDarkTheme = false) {
                    ProjectContentHost(CounterScreenActions(), { model }, overview = false)
                }
            }
        }
        return localized
    }

    private fun prepareRepeatingDialog(
        localized: Context,
        name: String,
    ): () -> Boolean {
        compose.onNodeWithContentDescription(localized.getString(R.string.project_actions_title)).performClick()
        compose.onNodeWithText(localized.getString(R.string.add_counter)).performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement(name)
        compose.onNodeWithText(localized.getString(R.string.repeating)).performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[1].performScrollTo().performTextReplacement("4")
        val save = compose.onNodeWithText(localized.getString(R.string.save)).assertIsDisplayed().assertIsEnabled()
        return requireNotNull(save.fetchSemanticsNode().config[SemanticsActions.OnClick].action)
    }

    private suspend fun select(
        model: CounterViewModel,
        id: Long,
    ) {
        val project = requireNotNull(fixture.counterRepository().getProject(id))
        withContext(Dispatchers.Main) { model.selectProject(project) }
        model.uiState.first { it.projectId == id }
    }

    private fun counterRepository(transactions: DatabaseTransactionRunner = fixture.transactionRunner) =
        ProjectCounterRepository(database.projectCounterDao(), database.counterProjectDao(), pro, transactions)

    private suspend fun databaseSnapshotExceptExtraCounters(): Map<String, List<List<String?>>> =
        withContext(Dispatchers.IO) {
            val db = database.openHelper.readableDatabase
            val tables =
                db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                }
            tables
                .filterNot { it == "project_counters" || it.startsWith("sqlite_") || it == "room_master_table" }
                .associateWith { table ->
                    db.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) add(List(cursor.columnCount) { cursor.getString(it) })
                        }
                    }
                }
        }

    private suspend fun createModel(counters: ProjectCounterRepository): CounterViewModel =
        withContext(Dispatchers.Main) {
            fixture
                .counterViewModel(
                    pro,
                    applicationScope,
                    counters = counters,
                ).also { models.put("extra-counter", it) }
        }
}
