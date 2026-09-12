package com.finnvek.knittools.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
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

    private fun newCounterRepository(): CounterRepository {
        val transactionRunner = RoomDatabaseTransactionRunner(database)
        val savedPatternRepository =
            SavedPatternRepository(
                dao = database.savedPatternDao(),
                context = context,
                counterProjectDao = database.counterProjectDao(),
                transactionRunner = transactionRunner,
                ioDispatcher = Dispatchers.IO,
                projectDocumentDao = database.projectDocumentDao(),
            )
        val projectDocumentRepository =
            ProjectDocumentRepository(
                documentDao = database.projectDocumentDao(),
                projectDao = database.counterProjectDao(),
                savedPatternRepository = savedPatternRepository,
                layerRepository =
                    PatternAnnotationLayerRepository(
                        database.patternAnnotationLayerDao(),
                        transactionRunner,
                    ),
                transactionRunner = transactionRunner,
                fileAvailability = ProjectDocumentFileAvailability(context, Dispatchers.IO),
            )
        return CounterRepository(
            dao = database.counterProjectDao(),
            projectCounterDao = database.projectCounterDao(),
            sessionDao = database.sessionDao(),
            photoStorage = ProgressPhotoStorage(),
            patternDocumentStorage = PatternDocumentStorage(),
            context = context,
            yarnCardRepository =
                YarnCardRepository(
                    dao = database.yarnCardDao(),
                    counterProjectDao = database.counterProjectDao(),
                    context = context,
                    transactionRunner = transactionRunner,
                    ioDispatcher = Dispatchers.IO,
                ),
            savedPatternRepository = savedPatternRepository,
            projectDocumentRepository = projectDocumentRepository,
            projectFolderDao = database.projectFolderDao(),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.IO,
        )
    }
}
