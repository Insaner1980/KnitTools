package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActiveProjectPolicyTest {
    private val dao = mockk<CounterProjectDao>(relaxed = true)
    private val pro = mockk<ProManager>()
    private val patterns = mockk<SavedPatternRepository>(relaxed = true)
    private val completed = CounterProjectEntity(id = 7L, name = "Completed", isCompleted = true, completedAt = 100L)
    private var status = ProStatus.TRIAL_EXPIRED

    @Before
    fun setup() {
        every { pro.hasFeature(any()) } answers
            { ProState(status).hasFeature(firstArg(), debugUnlockAllFeatures = false) }
        coEvery { dao.getProject(7L) } returns completed
        coEvery { dao.getAllProjectsOnce() } returns listOf(completed)
        coEvery { dao.insert(any()) } returns 8L
    }

    private fun repository(runner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner) =
        CounterRepository(
            dao = dao,
            projectCounterDao = mockk(relaxed = true),
            sessionDao = mockk(relaxed = true),
            photoStorage = mockk(relaxed = true),
            patternDocumentStorage = mockk(relaxed = true),
            context = mockk(relaxed = true),
            yarnCardRepository = mockk(relaxed = true),
            savedPatternRepository = patterns,
            projectDocumentRepository = mockk(relaxed = true),
            projectFolderDao = mockk(relaxed = true),
            transactionRunner = runner,
            ioDispatcher = Dispatchers.Unconfined,
            proManager = pro,
        )

    @Test
    fun `stub distinguishes active projects from total retained history`() =
        runTest {
            val stub = StubCounterProjectDao(listOf(completed, CounterProjectEntity(id = 8L, name = "Active")))
            assertEquals(1, stub.getActiveProjectCount())
            assertEquals(2, stub.getProjectCount())
        }

    @Test
    fun `free creation uses active rows regardless of completed history`() =
        runTest {
            val repository = repository()
            for (total in listOf(0, 5)) {
                coEvery { dao.getProjectCount() } returns total
                coEvery { dao.getActiveProjectCount() } returns 0
                assertEquals(
                    ProjectCreationResult.Created(8L),
                    repository.createProject("Next", canCreateAdditionalProjects = false),
                )
            }
            for (active in listOf(1, 2)) {
                coEvery { dao.getActiveProjectCount() } returns active
                assertEquals(
                    ProjectCreationResult.LimitReached,
                    repository.createProject("Next", canCreateAdditionalProjects = false),
                )
            }
            coVerify(exactly = 2) { dao.insert(any()) }
        }

    @Test
    fun `trial and purchase permit both additions but stale authorization cannot`() =
        runTest {
            coEvery { dao.getActiveProjectCount() } returns 2
            val repository = repository()
            for (entitlement in listOf(ProStatus.TRIAL_ACTIVE, ProStatus.PRO_PURCHASED)) {
                status = entitlement
                assertEquals(
                    ProjectCreationResult.Created(8L),
                    repository.createProject("Next", canCreateAdditionalProjects = true),
                )
                assertEquals(ProjectReactivationResult.Reactivated, repository.reactivateProject(7L, 100L))
            }
            status = ProStatus.TRIAL_EXPIRED
            assertEquals(
                ProjectCreationResult.LimitReached,
                repository.createProject("Next", canCreateAdditionalProjects = true),
            )
            assertEquals(ProjectReactivationResult.LimitReached, repository.reactivateProject(7L, 100L, true))
        }

    @Test
    fun `reactivation validates existence state and completion generation before writing`() =
        runTest {
            val repository = repository()
            coEvery { dao.getProject(7L) } returns null
            assertEquals(ProjectReactivationResult.ProjectUnavailable, repository.reactivateProject(7L, 100L))
            coEvery { dao.getProject(7L) } returns completed.copy(isCompleted = false)
            assertEquals(ProjectReactivationResult.ProjectUnavailable, repository.reactivateProject(7L, 100L))
            coEvery { dao.getProject(7L) } returns completed.copy(completedAt = 200L)
            assertEquals(ProjectReactivationResult.ProjectUnavailable, repository.reactivateProject(7L, 100L))
            coVerify(exactly = 0) { dao.reactivateProject(any(), any()) }
        }

    @Test
    fun `reactivation uses current active count inside the transaction`() =
        runTest {
            var inside = false
            val runner =
                object : DatabaseTransactionRunner {
                    override suspend fun <T> run(block: suspend () -> T): T {
                        inside = true
                        return try {
                            block()
                        } finally {
                            inside = false
                        }
                    }
                }
            var activeCount = 1
            coEvery { dao.getProject(7L) } answers {
                assertTrue(inside)
                completed
            }
            coEvery { dao.getActiveProjectCount() } answers {
                assertTrue(inside)
                activeCount
            }
            coEvery { dao.reactivateProject(7L, any()) } answers { assertTrue(inside) }
            val repository = repository(runner)
            assertEquals(ProjectReactivationResult.LimitReached, repository.reactivateProject(7L, 100L, false))
            activeCount = 0
            assertEquals(ProjectReactivationResult.Reactivated, repository.reactivateProject(7L, 100L, false))
            coVerify(exactly = 1) { dao.reactivateProject(7L, any()) }
        }

    @Test
    fun `reactivation maps commit failure and propagates cancellation`() =
        runTest {
            val failingRunner =
                object : DatabaseTransactionRunner {
                    override suspend fun <T> run(block: suspend () -> T): T {
                        block()
                        error("Commit failed")
                    }
                }
            assertEquals(
                ProjectReactivationResult.PersistenceFailure,
                repository(failingRunner).reactivateProject(7L, 100L),
            )
            coEvery { dao.getProject(7L) } throws CancellationException("cancel")
            try {
                repository().reactivateProject(7L, 100L)
                error("Cancellation was swallowed")
            } catch (cancelled: CancellationException) {
                assertEquals("cancel", cancelled.message)
            }
        }

    @Test
    fun `generic update cannot reactivate but legacy active rows remain editable after entitlement loss`() =
        runTest {
            val repository = repository()
            repository.updateProject(completed.copy(isCompleted = false).toDomain())
            coVerify(exactly = 0) { dao.update(any()) }
            coEvery { dao.getActiveProjectCount() } returns 2
            for (id in listOf(7L, 8L)) {
                val active = completed.copy(id = id, isCompleted = false, completedAt = null)
                coEvery { dao.getProject(id) } returns active
                repository.updateProject(active.copy(name = "Edited $id").toDomain())
                coVerify(
                    exactly = 1,
                ) { dao.update(match { it.id == id && !it.isCompleted && it.name == "Edited $id" }) }
            }
            coVerify(exactly = 0) { dao.getActiveProjectCount() }
        }

    @Test
    fun `Ravelry shares the gate and does not persist a pattern when blocked`() =
        runTest {
            val ravelry = RavelryRepository(mockk(), patterns, repository())
            val detail = PatternDetail(id = 42, name = "Original", permalink = "original")
            coEvery { dao.getActiveProjectCount() } returns 1
            assertEquals(ProjectCreationResult.LimitReached, ravelry.createProjectFromPattern(detail, false))
            coVerify(exactly = 0) { patterns.saveRavelryPatternIfMissingInCurrentTransaction(any()) }
            coVerify(exactly = 0) { dao.insert(any()) }
            coEvery { dao.getActiveProjectCount() } returns 0
            assertEquals(ProjectCreationResult.Created(8L), ravelry.createProjectFromPattern(detail, false))
            coVerify(exactly = 1) {
                patterns.saveRavelryPatternIfMissingInCurrentTransaction(
                    match {
                        it.ravelryPatternId ==
                            42
                    },
                )
            }
        }
}
