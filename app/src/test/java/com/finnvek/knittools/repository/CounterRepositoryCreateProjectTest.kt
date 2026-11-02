package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.domain.calculator.MAX_PROJECT_START_COUNT
import com.finnvek.knittools.domain.calculator.MAX_TARGET_ROWS
import com.finnvek.knittools.domain.calculator.ProjectStartValues
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CounterRepositoryCreateProjectTest {
    @Test
    fun `onboarding replay returns the receipt even after the free slot is used`() =
        runTest {
            var receipt: Long? = null
            val transaction = TrackingTransactionRunner()
            coEvery { dao.findOnboardingProjectId("stable") } answers { receipt }
            coEvery { dao.insert(any()) } answers {
                assertTrue(transaction.inTransaction)
                7L
            }
            coEvery { dao.insertOnboardingProject(any()) } answers {
                assertTrue(transaction.inTransaction)
                receipt = firstArg<com.finnvek.knittools.data.local.OnboardingProjectEntity>().projectId
            }
            val repository = repository(transaction)
            val draft =
                com.finnvek.knittools.domain.model
                    .OnboardingDraft("stable", name = "My socks", initialCount = "47")
            assertEquals(ProjectCreationResult.Created(7), repository.createOnboardingProject(draft, false))
            coEvery { dao.getActiveProjectCount() } returns 1
            assertEquals(ProjectCreationResult.Created(7), repository.createOnboardingProject(draft, false))
            coVerify(exactly = 1) { dao.insert(any()) }
            coVerify(exactly = 1) { dao.insertOnboardingProject(any()) }
        }

    @Test
    fun `invalid onboarding form never creates a receipt or project`() =
        runTest {
            coEvery { dao.findOnboardingProjectId(any()) } returns null
            val repository = repository()
            val draft =
                com.finnvek.knittools.domain.model
                    .OnboardingDraft("stable")
            listOf(
                draft,
                draft.copy(name = "My socks", initialCount = "bad"),
                draft.copy(name = "My socks", targetRows = "-1"),
            ).forEach {
                assertEquals(ProjectCreationResult.InvalidProject, repository.createOnboardingProject(it, false))
            }
            coVerify(exactly = 0) { dao.insert(any()) }
            coVerify(exactly = 0) { dao.insertOnboardingProject(any()) }
        }

    private val dao = mockk<CounterProjectDao>(relaxed = true)
    private val counterDao = mockk<ProjectCounterDao>(relaxed = true)
    private val sessionDao = mockk<SessionDao>(relaxed = true)
    private val proManager = mockk<ProManager>()

    @Before
    fun prepare() {
        coEvery { dao.getAllProjectsOnce() } returns emptyList()
        coEvery { dao.getActiveProjectCount() } returns 0
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
    }

    @Test
    fun `creation persists starting state without counter history or related mutations`() =
        runTest {
            val row = slot<CounterProjectEntity>()
            coEvery { dao.insert(capture(row)) } returns 7L
            coEvery { dao.getProject(7L) } answers { row.captured.copy(id = 7L) }
            coEvery { dao.getLatestHistory(7L) } returns null
            val repository = repository()

            val result =
                repository.createProject(
                    "Striped socks",
                    canCreateAdditionalProjects = false,
                    startValues = ProjectStartValues(37, 120),
                )

            assertEquals(ProjectCreationResult.Created(7L), result)
            assertEquals(37, row.captured.count)
            assertEquals(120, row.captured.targetRows)
            coVerify(exactly = 0) { dao.updateCounterStateWithHistory(any(), any(), any(), any(), any(), any(), any()) }
            verify { counterDao wasNot Called }
            verify { sessionDao wasNot Called }
            assertFalse(repository.applyMainCounterChange(7L, MainCounterChange.Undo))
            assertEquals(37, row.captured.count)
            coVerify(exactly = 0) { dao.updateCount(any(), any(), any()) }
        }

    @Test
    fun `default creation remains zero without target and permits maximum values`() =
        runTest {
            val rows = mutableListOf<CounterProjectEntity>()
            coEvery { dao.insert(capture(rows)) } returns 7L
            val repository = repository()

            repository.createProject("New project", canCreateAdditionalProjects = false)
            repository.createProject(
                "Maximum project",
                canCreateAdditionalProjects = false,
                startValues = ProjectStartValues(MAX_PROJECT_START_COUNT, MAX_TARGET_ROWS),
            )

            assertEquals(0, rows[0].count)
            assertEquals(null, rows[0].targetRows)
            assertEquals(MAX_PROJECT_START_COUNT, rows[1].count)
            assertEquals(MAX_TARGET_ROWS, rows[1].targetRows)
        }

    @Test
    fun `invalid starting values never insert a project`() =
        runTest {
            val repository = repository()
            listOf(-1 to null, 100000 to null, 0 to 0, 0 to -1, 0 to 100000).forEach { (start, target) ->
                assertEquals(
                    ProjectCreationResult.InvalidProject,
                    repository.createProject(
                        "Invalid",
                        canCreateAdditionalProjects = false,
                        startValues = ProjectStartValues(start, target),
                    ),
                )
            }
            coVerify(exactly = 0) { dao.insert(any()) }
        }

    @Test
    fun `quota is checked first inside transaction even for invalid input`() =
        runTest {
            val transaction = TrackingTransactionRunner()
            coEvery { dao.getActiveProjectCount() } answers {
                assertTrue(transaction.inTransaction)
                1
            }

            val result =
                repository(
                    transaction,
                ).createProject(
                    "Invalid",
                    canCreateAdditionalProjects = false,
                    startValues = ProjectStartValues(initialCount = -1),
                )

            assertEquals(ProjectCreationResult.LimitReached, result)
            coVerify(exactly = 0) { dao.insert(any()) }
            coVerify(exactly = 0) { dao.getAllProjectsOnce() }
        }

    private class TrackingTransactionRunner : DatabaseTransactionRunner {
        var inTransaction = false
            private set

        override suspend fun <T> run(block: suspend () -> T): T {
            inTransaction = true
            return try {
                block()
            } finally {
                inTransaction = false
            }
        }
    }

    private fun repository(
        transactionRunner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ): CounterRepository =
        CounterRepository(
            dao = dao,
            projectCounterDao = counterDao,
            sessionDao = sessionDao,
            photoStorage = mockk(relaxed = true),
            // CPD-OFF: Luontitestien käyttämättömät riippuvuudet täytetään konstruktorin sopimuksen mukaisesti.
            patternDocumentStorage = mockk(relaxed = true),
            context = mockk<Context>(relaxed = true),
            yarnCardRepository = mockk(relaxed = true),
            savedPatternRepository = mockk(relaxed = true),
            projectDocumentRepository = mockk(relaxed = true),
            projectFolderDao = mockk(relaxed = true),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.Unconfined,
            // CPD-ON
            proManager = proManager,
        )
}
