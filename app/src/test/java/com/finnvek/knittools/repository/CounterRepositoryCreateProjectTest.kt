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
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.ProjectDocument
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
    fun `onboarding validates draft and PDF before writing and honors project quota`() =
        runTest {
            coEvery { dao.findOnboardingProjectId(any()) } returns null
            val repository = repository()
            val draft = OnboardingDraft("draft", name = "Socks", initialCount = "")
            for (invalid in listOf(draft.copy(id = ""), draft.copy(targetRows = "bad"))) {
                assertEquals(ProjectCreationResult.InvalidProject, repository.createOnboardingProject(invalid, false))
            }
            coEvery { documents.preflightImportedPdf(any(), any(), any()) } returns
                ProjectDocumentMutationResult.PdfUnavailable
            assertEquals(
                ProjectCreationResult.InvalidProject,
                repository.createOnboardingProject(draft.copy(pdfUri = "file:///missing.pdf"), false),
            )
            coEvery { dao.getActiveProjectCount() } returns 1
            assertEquals(ProjectCreationResult.LimitReached, repository.createOnboardingProject(draft, false))
            coVerify(exactly = 0) { dao.insert(any()) }
            coVerify(exactly = 0) { dao.insertOnboardingProject(any()) }
        }

    @Test
    fun `PDF attachment and receipt are written inside the project transaction`() =
        runTest {
            val transaction = TrackingTransactionRunner()
            val document = mockk<ProjectDocument>(relaxed = true)
            every { document.isPrimary } returns true
            every { document.label } returns "pattern.pdf"
            coEvery { dao.findOnboardingProjectId(any()) } returns null
            coEvery { dao.insert(any()) } returns 7L
            coEvery { documents.preflightImportedPdf(any(), any(), any()) } returns null
            coEvery { documents.addImportedPdfInCurrentTransaction(any(), any(), any(), any()) } coAnswers {
                assertTrue(transaction.inTransaction)
                ProjectDocumentMutationResult.Added(document)
            }
            coEvery { dao.insertOnboardingProject(any()) } coAnswers { assertTrue(transaction.inTransaction) }
            val draft = OnboardingDraft("pdf", name = "Socks", pdfUri = "file:///pattern.pdf", pdfName = "pattern.pdf")
            assertEquals(ProjectCreationResult.Created(7), repository(transaction).createOnboardingProject(draft, true))
            coVerify(exactly = 1) { dao.updatePatternInformation(7L, any(), "pattern.pdf", any()) }
            coVerify(exactly = 1) { dao.insertOnboardingProject(any()) }
        }

    @Test
    fun `receipt created before transaction starts prevents a second insertion`() =
        runTest {
            coEvery { dao.findOnboardingProjectId("race") } returnsMany listOf(null, 7L)
            val draft = OnboardingDraft("race", name = "Socks", targetRows = "120")
            assertEquals(ProjectCreationResult.Created(7), repository().createOnboardingProject(draft, false))
            coVerify(exactly = 0) { dao.insert(any()) }
            coVerify(exactly = 0) { dao.insertOnboardingProject(any()) }
        }

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
                draft.copy(name = "My socks", initialCount = " "),
                draft.copy(name = "My socks", initialCount = "+2"),
                draft.copy(name = "My socks", targetRows = "-1"),
                draft.copy(name = "My socks", targetRows = " "),
                draft.copy(name = "My socks", targetRows = "+2"),
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
    private val documents = mockk<ProjectDocumentRepository>(relaxed = true)

    @Before
    fun prepare() {
        coEvery { dao.getAllProjectsOnce() } returns emptyList()
        coEvery { dao.getActiveProjectCount() } returns 0
        every { proManager.hasCurrentFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
        coEvery { proManager.hasFeatureAfterInitialLoad(any()) } answers { proManager.hasCurrentFeature(firstArg()) }
    }

    @Test
    fun `creation persists starting state without counter history or related mutations`() =
        runTest {
            val row = slot<CounterProjectEntity>()
            coEvery { dao.hasAnyProject() } returns false
            coEvery { dao.insert(capture(row)) } returns 7L
            coEvery { dao.getProject(7L) } answers { row.captured.copy(id = 7L) }
            coEvery { dao.getLatestHistory(7L) } returns null
            val repository = repository()

            val result =
                repository.createProject(
                    "Striped socks",
                    canCreateAdditionalProjects = false,
                    options = ProjectCreationOptions(ProjectStartValues(37, 120)),
                )

            assertEquals(ProjectCreationResult.Created(7L, wasFirstProject = true), result)
            assertEquals(37, row.captured.count)
            assertEquals(120, row.captured.targetRows)
            coVerify(
                exactly = 0,
            ) { dao.updateCounterStateWithHistory(any(), any(), any(), any(), any(), any(), any()) }
            verify { counterDao wasNot Called }
            verify { sessionDao wasNot Called }
            assertFalse(repository.applyMainCounterChange(7L, MainCounterChange.Undo))
            assertEquals(37, row.captured.count)
            coVerify(exactly = 0) { dao.updateCount(any(), any(), any()) }
        }

    @Test
    fun `first project status comes from the unfiltered snapshot inside insertion transaction`() =
        runTest {
            val transaction = TrackingTransactionRunner()
            var hasAnyProject = false
            coEvery { dao.hasAnyProject() } answers {
                assertTrue(transaction.inTransaction)
                hasAnyProject
            }
            coEvery { dao.insert(any()) } answers {
                hasAnyProject = true
                7L
            }
            val repository = repository(transaction)

            val first = repository.createProject("First", canCreateAdditionalProjects = false)
            val afterCompleted = repository.createProject("After completed", canCreateAdditionalProjects = false)

            assertEquals(ProjectCreationResult.Created(7L, wasFirstProject = true), first)
            assertEquals(ProjectCreationResult.Created(7L, wasFirstProject = false), afterCompleted)
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
                options = ProjectCreationOptions(ProjectStartValues(MAX_PROJECT_START_COUNT, MAX_TARGET_ROWS)),
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
                        options = ProjectCreationOptions(ProjectStartValues(start, target)),
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
                    options = ProjectCreationOptions(ProjectStartValues(initialCount = -1)),
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
            projectDocumentRepository = documents,
            projectFolderDao = mockk(relaxed = true),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.Unconfined,
            // CPD-ON
            proManager = proManager,
        )
}
