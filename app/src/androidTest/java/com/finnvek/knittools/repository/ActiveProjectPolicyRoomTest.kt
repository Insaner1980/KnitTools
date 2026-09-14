package com.finnvek.knittools.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectCompletionEntity
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveProjectPolicyRoomTest {
    private val context: android.content.Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: CounterRepository

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        repository = newCounterRepository()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun completedHistoryDoesNotBlockCreation() =
        runBlocking {
            val first = seed("Completed 1", completed = true)
            seed("Completed 2", completed = true)
            val before = database.counterProjectDao().getProject(first)
            assertTrue(create("Next") is ProjectCreationResult.Created)
            assertEquals(1, repository.getActiveProjectCount())
            assertEquals(3, repository.getProjectCount())
            assertEquals(before, database.counterProjectDao().getProject(first))
        }

    @Test
    fun activeProjectBlocksCreationWithoutAnyDatabaseChange() =
        runBlocking {
            seed("Active")
            val before = database.counterProjectDao().getAllProjectsOnce()
            assertEquals(ProjectCreationResult.LimitReached, create("Blocked"))
            assertEquals(before, database.counterProjectDao().getAllProjectsOnce())
        }

    @Test
    fun reactivationRequiresAnEmptyActiveSlotAndPreservesOtherFields() =
        runBlocking {
            val first = seed("Completed 1", completed = true)
            val second = seed("Completed 2", completed = true)
            val before = requireNotNull(database.counterProjectDao().getProject(first))
            val secondBefore = database.counterProjectDao().getProject(second)
            assertEquals(ProjectReactivationResult.Reactivated, reactivate(first))
            val after = requireNotNull(database.counterProjectDao().getProject(first))
            assertEquals(
                before.copy(isCompleted = false, completedAt = null, totalRows = null, updatedAt = after.updatedAt),
                after,
            )
            assertEquals(ProjectReactivationResult.LimitReached, reactivate(second))
            assertEquals(secondBefore, database.counterProjectDao().getProject(second))
            assertNull(database.sessionDao().getActiveSession())
        }

    @Test
    fun legacyMultipleActiveProjectsRemainUsableUntilTheLastOneCompletes() =
        runBlocking {
            val first = seed("Legacy 1")
            val second = seed("Legacy 2")
            assertTrue(repository.applyMainCounterChange(first, MainCounterChange.Increment))
            assertTrue(repository.applyMainCounterChange(second, MainCounterChange.Increment))
            assertTrue(repository.applyMainCounterChange(first, MainCounterChange.Decrement))
            assertEquals(2, repository.getActiveProjectCount())
            repository.archiveProject(first, 200L)
            assertEquals(1, repository.getActiveProjectCount())
            assertEquals(ProjectCreationResult.LimitReached, create("Blocked"))
            repository.archiveProject(second, 300L)
            assertEquals(0, repository.getActiveProjectCount())
            assertTrue(create("Allowed") is ProjectCreationResult.Created)
            assertNotNull(repository.getProject(first))
            assertNotNull(repository.getProject(second))
        }

    @Test
    fun createCreateRaceAdmitsOnlyOneActiveProject() =
        runBlocking {
            race({ create("First") }, { create("Second") })
        }

    @Test
    fun createReactivateRaceAdmitsOnlyOneActiveProject() =
        runBlocking {
            val completed = seed("Completed", completed = true)
            race({ create("New") }, { reactivate(completed) })
        }

    @Test
    fun reactivateReactivateRaceAdmitsOnlyOneActiveProject() =
        runBlocking {
            val first = seed("First", completed = true)
            val second = seed("Second", completed = true)
            race({ reactivate(first) }, { reactivate(second) })
        }

    @Test
    fun reactivationPreservesMalformedCompletedProjectActiveSession() =
        runBlocking {
            val id = seed("Legacy session")
            assertTrue(repository.startSession(id) is StartSessionResult.Started)
            database.counterProjectDao().archiveProject(id, 0, 100L, 100L)
            val session = database.sessionDao().getActiveSession()
            assertNotNull(session)
            assertEquals(ProjectReactivationResult.Reactivated, reactivate(id))
            assertEquals(session, database.sessionDao().getActiveSession())
        }

    @Test
    fun staleCompletionAndGenericUpdateCannotBypassReactivation() =
        runBlocking {
            val id = seed("Completed", completed = true)
            val before = requireNotNull(repository.getProject(id))
            assertEquals(ProjectReactivationResult.ProjectUnavailable, repository.reactivateProject(id, 99L, false))
            repository.updateProject(before.copy(isCompleted = false))
            assertEquals(before, repository.getProject(id))
            assertEquals(0, repository.getActiveProjectCount())
        }

    @Test
    fun blockedPatternCreationLeavesBothTablesUntouched() =
        runBlocking {
            seed("Active")
            val before = database.counterProjectDao().getAllProjectsOnce()
            val pattern =
                SavedPattern(
                    source = SavedPatternSource.Ravelry,
                    ravelryPatternId = 42,
                    name = "Pattern",
                    designerName = "Designer",
                )
            assertEquals(
                ProjectCreationResult.LimitReached,
                repository.createProject("From pattern", canCreateAdditionalProjects = false, linkedPattern = pattern),
            )
            assertEquals(before, database.counterProjectDao().getAllProjectsOnce())
            assertTrue(
                database
                    .savedPatternDao()
                    .getAll()
                    .first()
                    .isEmpty(),
            )
        }

    @Test
    fun completionHistoryPreservesCyclesFiltersAndCascades() =
        runBlocking {
            val first = seed("Cycles")
            val second = seed("Other")
            assertTrue(repository.observeCompletions().first().isEmpty())
            assertEquals(
                ProjectCompletionResult.Completed,
                repository.completeProjectWithSessionChoice(first, null, 200L),
            )
            repository.archiveProject(first, 300L)
            assertEquals(1, repository.observeCompletions(first).first().size)
            assertEquals(
                java.time.ZoneId
                    .systemDefault()
                    .id,
                repository
                    .observeCompletions(first)
                    .first()
                    .single()
                    .zoneId,
            )
            assertEquals(ProjectReactivationResult.Reactivated, repository.reactivateProject(first, 200L, true))
            assertEquals(1, repository.observeCompletions(first).first().size)
            repository.archiveProject(first, 400L)
            repository.archiveProject(second, 500L)
            assertEquals(listOf(400L, 200L), repository.observeCompletions(first).first().map { it.completedAt })
            assertEquals(3, repository.observeCompletions().first().size)
            repository.deleteProject(first)
            assertEquals(listOf(second), repository.observeCompletions().first().map { it.projectId })
        }

    @Test
    fun completionEventFailureRollsBackProjectAndSession() =
        runBlocking {
            val id = seed("Rollback")
            repository.startSession(id)
            val before = repository.getProject(id)
            val session = database.sessionDao().getActiveSession()
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_completion BEFORE INSERT ON project_completions " +
                    "BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
            assertEquals(
                ProjectCompletionResult.PersistenceFailure,
                repository.completeProjectWithSessionChoice(id, ActiveSessionCompletionChoice.DISCARD),
            )
            assertEquals(before, repository.getProject(id))
            assertEquals(session, database.sessionDao().getActiveSession())
            assertTrue(repository.observeCompletions().first().isEmpty())
        }

    @Test
    fun bulkCompletionRecordsOnlySuccessfulTransitions() =
        runBlocking {
            val first = seed("First")
            val blocked = seed("Needs choice")
            repository.startSession(blocked)
            val results =
                listOf(
                    first,
                    blocked,
                    first,
                    999L,
                ).map { repository.completeProjectWithSessionChoice(it, null) }
            assertEquals(ProjectCompletionResult.Completed, results[0])
            assertTrue(results[1] is ProjectCompletionResult.NeedsActiveSessionChoice)
            assertEquals(ProjectCompletionResult.ProjectUnavailable, results[3])
            assertEquals(listOf(first), repository.observeCompletions().first().map { it.projectId })
        }

    @Test
    fun cancelledCompletionRollsBackBothWritesAndPropagates() =
        runBlocking {
            val id = seed("Cancelled")
            val before = repository.getProject(id)
            val dao = database.counterProjectDao()
            val cancelling =
                object : CounterProjectDao by dao {
                    override suspend fun insertCompletion(event: ProjectCompletionEntity): Long {
                        dao.insertCompletion(event)
                        throw CancellationException("test cancellation")
                    }
                }
            var cancelled = false
            try {
                newCounterRepository(cancelling).completeProjectWithSessionChoice(id, null)
            } catch (_: CancellationException) {
                cancelled = true
            }
            assertTrue(cancelled)
            assertEquals(before, repository.getProject(id))
            assertTrue(repository.observeCompletions().first().isEmpty())
        }

    @Test
    fun concurrentCompletionCreatesExactlyOneEvent() =
        runBlocking {
            val id = seed("Concurrent")
            coroutineScope {
                listOf(
                    async(Dispatchers.IO) {
                        repository.completeProjectWithSessionChoice(id, null)
                    },
                    async(Dispatchers.IO) { repository.completeProjectWithSessionChoice(id, null) },
                ).awaitAll()
            }
            assertEquals(1, repository.observeCompletions(id).first().size)
        }

    private suspend fun race(
        first: suspend () -> Any,
        second: suspend () -> Any,
    ) = coroutineScope {
        val ready = CompletableDeferred<Unit>()
        val results =
            listOf(first, second).map { operation ->
                async(Dispatchers.IO) {
                    ready.await()
                    operation()
                }
            }
        ready.complete(Unit)
        val completed = results.awaitAll()
        assertEquals(
            1,
            completed.count {
                it is ProjectCreationResult.Created ||
                    it == ProjectReactivationResult.Reactivated
            },
        )
        assertEquals(
            1,
            completed.count {
                it == ProjectCreationResult.LimitReached ||
                    it == ProjectReactivationResult.LimitReached
            },
        )
        assertEquals(1, repository.getActiveProjectCount())
    }

    private suspend fun seed(
        name: String,
        completed: Boolean = false,
    ): Long =
        database.counterProjectDao().insert(
            CounterProjectEntity(name = name, isCompleted = completed, completedAt = if (completed) 100L else null),
        )

    private suspend fun create(name: String) = repository.createProject(name, canCreateAdditionalProjects = false)

    private suspend fun reactivate(id: Long) =
        repository.reactivateProject(id, 100L, canCreateAdditionalProjects = false)

    private fun newCounterRepository(projectDao: CounterProjectDao = database.counterProjectDao()): CounterRepository =
        counterHistoryTestRepository(database, context, projectDao)
}
