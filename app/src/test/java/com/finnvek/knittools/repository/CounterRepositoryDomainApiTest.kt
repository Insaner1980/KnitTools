package com.finnvek.knittools.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectFolderAssignmentEntity
import com.finnvek.knittools.data.local.ProjectFolderDao
import com.finnvek.knittools.data.local.ProjectFolderEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Yksi testiluokka pitää CounterRepositoryn ja sen yhteisen fixture-tilan samassa sopimusrajassa.
@Suppress("LargeClass")
class CounterRepositoryDomainApiTest {
    private lateinit var projectDao: CounterProjectDao
    private lateinit var sessionDao: SessionDao
    private lateinit var yarnCardRepository: YarnCardRepository
    private lateinit var savedPatternRepository: SavedPatternRepository
    private lateinit var projectDocumentRepository: ProjectDocumentRepository
    private lateinit var projectFolderDao: ProjectFolderDao
    private lateinit var proManager: ProManager
    private lateinit var repository: CounterRepository

    @Before
    fun setup() {
        projectDao = mockk(relaxed = true)
        sessionDao = mockk(relaxed = true)
        yarnCardRepository = mockk(relaxed = true)
        savedPatternRepository = mockk(relaxed = true)
        projectDocumentRepository = mockk(relaxed = true)
        projectFolderDao = mockk(relaxed = true)
        proManager = mockk()
        every { proManager.hasCurrentFeature(any()) } returns true
        coEvery { proManager.hasFeatureAfterInitialLoad(any()) } answers { proManager.hasCurrentFeature(firstArg()) }
        coEvery { projectDao.getAllProjectsOnce() } returns emptyList()
        repository = createRepository()
    }

    private fun createRepository(
        transactionRunner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ): CounterRepository =
        CounterRepository(
            dao = projectDao,
            projectCounterDao = mockk(relaxed = true),
            sessionDao = sessionDao,
            photoStorage = mockk<ProgressPhotoStorage>(relaxed = true),
            patternDocumentStorage = mockk<PatternDocumentStorage>(relaxed = true),
            context = mockk<Context>(relaxed = true),
            yarnCardRepository = yarnCardRepository,
            savedPatternRepository = savedPatternRepository,
            projectDocumentRepository = projectDocumentRepository,
            projectFolderDao = projectFolderDao,
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.Unconfined,
            proManager = proManager,
        )

    @Test
    fun `project existence uses unfiltered queries for active or completed projects`() =
        runTest {
            coEvery { projectDao.hasAnyProject() } returns true
            every { projectDao.observeHasAnyProject() } returns flowOf(true)
            coEvery { projectDao.getActiveProjectCount() } returns 0
            assertTrue(repository.hasAnyProject())
            assertTrue(repository.observeHasAnyProject().first())
            assertEquals(0, repository.getActiveProjectCount())
        }

    @Test
    fun `saving stitch count for requested activation enables tracking in one write`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L, name = "Project")
            var storedCount: Int? = null
            var storedTracking = false
            var writes = 0
            coEvery { projectDao.updateStitchState(7L, any(), any(), any(), any()) } coAnswers {
                storedCount = secondArg()
                storedTracking = thirdArg()
                writes += 1
            }

            assertTrue(repository.updateProjectStitchCount(7L, 100, enableTracking = true))

            assertEquals(100, storedCount)
            assertTrue(storedTracking)
            assertEquals(1, writes)
        }

    @Test
    fun `ordinary stitch count changes preserve tracking choice and clearing disables it`() =
        runTest {
            var storedProject =
                CounterProjectEntity(
                    id = 7L,
                    name = "Project",
                    stitchCount = 12,
                    currentStitch = 8,
                    stitchTrackingEnabled = true,
                )
            coEvery { projectDao.getProject(7L) } coAnswers { storedProject }
            coEvery { projectDao.updateStitchState(7L, any(), any(), any(), any()) } coAnswers {
                storedProject =
                    storedProject.copy(
                        stitchCount = secondArg(),
                        stitchTrackingEnabled = thirdArg(),
                        currentStitch = arg(3),
                    )
            }

            assertTrue(repository.updateProjectStitchCount(7L, 3))
            assertEquals(3, storedProject.currentStitch)
            assertTrue(storedProject.stitchTrackingEnabled)
            assertTrue(repository.updateStitchTrackingEnabled(7L, false))
            assertEquals(3, storedProject.stitchCount)
            assertEquals(0, storedProject.currentStitch)
            assertTrue(repository.updateProjectStitchCount(7L, 100))
            assertEquals(100, storedProject.stitchCount)
            assertFalse(storedProject.stitchTrackingEnabled)
            assertTrue(repository.updateStitchTrackingEnabled(7L, true))
            assertTrue(repository.updateProjectStitchCount(7L, null))
            assertEquals(null, storedProject.stitchCount)
            assertFalse(storedProject.stitchTrackingEnabled)
            assertEquals(0, storedProject.currentStitch)
        }

    @Test
    fun `requested stitch activation rejects invalid totals and missing or completed projects`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Done", isCompleted = true)
            coEvery { projectDao.getProject(8L) } returns null

            assertFalse(repository.updateProjectStitchCount(7L, 0, enableTracking = true))
            assertFalse(repository.updateProjectStitchCount(7L, -1, enableTracking = true))
            assertFalse(repository.updateProjectStitchCount(7L, 100, enableTracking = true))
            assertFalse(repository.updateProjectStitchCount(8L, 100, enableTracking = true))

            coVerify(exactly = 0) { projectDao.updateStitchState(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `counter repository exposes project domain models`() =
        runTest {
            every { projectDao.getAllProjects() } returns
                flowOf(
                    listOf(
                        CounterProjectEntity(
                            id = 7L,
                            name = "Cardigan",
                            count = 32,
                            secondaryCount = 4,
                            stepSize = 2,
                            notes = "Body",
                            createdAt = 100L,
                            updatedAt = 200L,
                            sectionName = "Sleeve",
                            stitchCount = 96,
                            isCompleted = true,
                            totalRows = 120,
                            completedAt = 300L,
                            yarnCardIds = "1,2",
                            linkedPatternId = 9L,
                            patternUri = "content://pattern",
                            patternName = "Rib cardigan",
                            currentPatternPage = 5,
                            patternRowMapping = "1=10",
                            stitchTrackingEnabled = true,
                            currentStitch = 44,
                        ),
                    ),
                )

            val projects: List<CounterProject> = repository.getAllProjects().first()

            assertEquals(7L, projects.single().id)
            assertEquals("Cardigan", projects.single().name)
            assertEquals(300L, projects.single().completedAt)
            assertEquals("content://pattern", projects.single().patternUri)
            assertTrue(projects.single().stitchTrackingEnabled)
            assertEquals(44, projects.single().currentStitch)
        }

    @Test
    fun `counter repository accepts project domain model when updating`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L, name = "Existing")
            val updatedEntity = slot<CounterProjectEntity>()
            coEvery { projectDao.update(capture(updatedEntity)) } returns Unit
            val beforeUpdate = System.currentTimeMillis()

            repository.updateProject(
                CounterProject(
                    id = 7L,
                    name = "Updated cardigan",
                    count = 40,
                    createdAt = 100L,
                    updatedAt = 200L,
                    completedAt = 300L,
                    linkedPatternId = 9L,
                    patternUri = "content://pattern",
                    patternName = "Rib cardigan",
                    stitchCount = 96,
                    stitchTrackingEnabled = true,
                    currentStitch = 44,
                ),
            )

            coVerify { projectDao.update(any()) }
            assertEquals(7L, updatedEntity.captured.id)
            assertEquals("Updated cardigan", updatedEntity.captured.name)
            assertEquals(300L, updatedEntity.captured.completedAt)
            assertEquals(9L, updatedEntity.captured.linkedPatternId)
            assertEquals("content://pattern", updatedEntity.captured.patternUri)
            assertTrue(updatedEntity.captured.stitchTrackingEnabled)
            assertTrue(updatedEntity.captured.updatedAt >= beforeUpdate)
        }

    @Test
    fun `createProject normalisoi nimen ja kayttaa samaa aikaleimaa`() =
        runTest {
            val insertedProject = slot<CounterProjectEntity>()
            coEvery { projectDao.getAllProjectsOnce() } returns emptyList()
            coEvery { projectDao.insert(capture(insertedProject)) } returns 9L

            val result =
                repository.createProject(
                    name = "  Sukat  ",
                    canCreateAdditionalProjects = true,
                )

            assertEquals(ProjectCreationResult.Created(9L, wasFirstProject = true), result)
            assertEquals("Sukat", insertedProject.captured.name)
            assertEquals(insertedProject.captured.createdAt, insertedProject.captured.updatedAt)
        }

    @Test
    fun `createProject tekee nimestä uniikin jos nimi on jo kaytossa`() =
        runTest {
            val insertedProject = slot<CounterProjectEntity>()
            coEvery { projectDao.getAllProjectsOnce() } returns
                listOf(
                    CounterProjectEntity(id = 1L, name = "Project 3"),
                    CounterProjectEntity(id = 2L, name = "Project 3 (2)"),
                )
            coEvery { projectDao.insert(capture(insertedProject)) } returns 9L

            repository.createProject(
                name = "Project 3",
                canCreateAdditionalProjects = true,
            )

            assertEquals("Project 3 (3)", insertedProject.captured.name)
        }

    @Test
    fun `createProject hylkaa tyhjan nimen ennen tietokantakirjoitusta`() =
        runTest {
            val result =
                repository.createProject(
                    name = "   ",
                    canCreateAdditionalProjects = true,
                )

            assertEquals(ProjectCreationResult.InvalidProject, result)
            coVerify(exactly = 0) { projectDao.insert(any()) }
        }

    @Test
    fun `createProject validates target folder and assigns the new project inside its transaction`() =
        runTest {
            val runner = ProjectCreationTransactionRunner()
            coEvery { projectFolderDao.getById(99L) } returns null

            assertEquals(
                ProjectCreationResult.FolderMissing,
                createRepository(transactionRunner = runner).createProject(
                    name = "Missing folder",
                    canCreateAdditionalProjects = true,
                    targetFolderId = 99L,
                ),
            )
            coVerify(exactly = 0) { projectDao.insert(any()) }

            coEvery { projectFolderDao.getById(5L) } returns
                ProjectFolderEntity(id = 5L, name = "Gifts", normalizedName = "gifts", sortOrder = 0)
            coEvery { projectDao.insert(any()) } returns 12L
            val assignment = slot<ProjectFolderAssignmentEntity>()
            coEvery { projectFolderDao.insertOrReplaceAssignment(capture(assignment)) } returns Unit

            assertEquals(
                ProjectCreationResult.Created(12L, wasFirstProject = true),
                createRepository(transactionRunner = runner).createProject(
                    name = "In folder",
                    canCreateAdditionalProjects = true,
                    targetFolderId = 5L,
                ),
            )
            assertEquals(ProjectFolderAssignmentEntity(projectId = 12L, folderId = 5L), assignment.captured)
            assertEquals(2, runner.runCount)
        }

    @Test
    fun `createProject maps an assignment constraint after folder validation to FolderMissing`() =
        runTest {
            coEvery { projectFolderDao.getById(5L) } returnsMany
                listOf(
                    ProjectFolderEntity(id = 5L, name = "Gifts", normalizedName = "gifts", sortOrder = 0),
                    null,
                )
            coEvery { projectDao.insert(any()) } returns 12L

            val result =
                createRepository(
                    transactionRunner =
                        object : DatabaseTransactionRunner {
                            override suspend fun <T> run(block: suspend () -> T): T {
                                block()
                                throw SQLiteConstraintException("Folder deleted")
                            }
                        },
                ).createProject(
                    name = "Race",
                    canCreateAdditionalProjects = true,
                    targetFolderId = 5L,
                )

            assertEquals(ProjectCreationResult.FolderMissing, result)
        }

    @Test
    fun `createProject preserves an unrelated assignment constraint when target still exists`() =
        runTest {
            coEvery { projectFolderDao.getById(5L) } returns
                ProjectFolderEntity(id = 5L, name = "Gifts", normalizedName = "gifts", sortOrder = 0)
            coEvery { projectDao.insert(any()) } returns 12L

            val error =
                runCatching {
                    createRepository(
                        transactionRunner =
                            object : DatabaseTransactionRunner {
                                override suspend fun <T> run(block: suspend () -> T): T {
                                    block()
                                    throw SQLiteConstraintException("Assignment trigger rejected")
                                }
                            },
                    ).createProject(
                        name = "Trigger",
                        canCreateAdditionalProjects = true,
                        targetFolderId = 5L,
                    )
                }.exceptionOrNull()

            assertTrue(error is SQLiteConstraintException)
        }

    @Test
    fun `createProject tarkistaa ilmaisen projektikiintiön ja kirjoittaa samassa transaktiossa`() =
        runTest {
            val runner = ProjectCreationTransactionRunner()
            coEvery { projectDao.getActiveProjectCount() } returns 0
            coEvery { projectDao.insert(any()) } returns 11L
            val transactionRepository = createRepository(transactionRunner = runner)

            val result =
                transactionRepository.createProject(
                    name = "Sukat",
                    canCreateAdditionalProjects = false,
                )

            assertEquals(ProjectCreationResult.Created(11L, wasFirstProject = true), result)
            assertEquals(1, runner.runCount)
            coVerifyOrder {
                projectDao.hasAnyProject()
                projectDao.getActiveProjectCount()
                projectDao.getAllProjectsOnce()
                projectDao.insert(any())
            }
        }

    @Test
    fun `createProject blocks another active project`() =
        runTest {
            coEvery { projectDao.getActiveProjectCount() } returns 1

            val result =
                repository.createProject(
                    name = "Toinen projekti",
                    canCreateAdditionalProjects = false,
                )

            assertEquals(ProjectCreationResult.LimitReached, result)
            coVerify(exactly = 0) { projectDao.getAllProjectsOnce() }
            coVerify(exactly = 0) { projectDao.insert(any()) }
        }

    @Test
    fun `imported PDF creation rejects an entitlement lost during preflight`() =
        runTest {
            val uri = "file:///staged.pdf"
            var entitled = true
            every { proManager.hasCurrentFeature(ProFeature.UNLIMITED_PROJECTS) } answers { entitled }
            coEvery { projectDocumentRepository.preflightImportedPdf(uri, "Chart") } answers {
                entitled = false
                null
            }
            coEvery { projectDocumentRepository.getFirstProjectIdByUri(uri) } returns null
            coEvery { projectDao.getActiveProjectCount() } returns 1

            val result = repository.createProjectWithImportedPdf("New", true, uri, "Chart")

            assertEquals(ProjectCreationResult.LimitReached, result)
            coVerify(exactly = 0) { projectDao.insert(any()) }
            coVerify(exactly = 0) { projectDocumentRepository.addImportedPdfInCurrentTransaction(any(), any(), any()) }
        }

    @Test
    fun `createProject rechecks additional project entitlement in the repository`() =
        runTest {
            coEvery { proManager.hasFeatureAfterInitialLoad(ProFeature.UNLIMITED_PROJECTS) } returns true
            every { proManager.hasCurrentFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { projectDao.getActiveProjectCount() } returns 1

            val result =
                repository.createProject(
                    name = "Second project",
                    canCreateAdditionalProjects = true,
                )

            assertEquals(ProjectCreationResult.LimitReached, result)
            coVerify(exactly = 0) { projectDao.insert(any()) }
        }

    @Test
    fun `completed history does not consume the free active project slot`() =
        runTest {
            every { proManager.hasCurrentFeature(any()) } returns false
            coEvery { projectDao.getActiveProjectCount() } returns 0
            coEvery { projectDao.getProjectCount() } returns 5
            coEvery { projectDao.hasAnyProject() } returns true
            coEvery { projectDao.insert(any()) } returns 11L

            assertEquals(
                ProjectCreationResult.Created(11L),
                repository.createProject("Next project", canCreateAdditionalProjects = false),
            )
            coVerify(exactly = 0) { projectDao.getProjectCount() }
        }

    @Test
    fun `secondary mutation rejects completed missing and negative input without writing`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, isCompleted = true, secondaryCounterUsed = true)
            assertFalse(repository.updateProjectSecondaryCount(7L, 2))
            coEvery { projectDao.getProject(7L) } returns null
            assertFalse(repository.updateProjectSecondaryCount(7L, 2))
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L, secondaryCounterUsed = true)
            assertFalse(repository.updateProjectSecondaryCount(7L, -1))
            coVerify(exactly = 0) { projectDao.updateSecondaryCount(any(), any(), any()) }

            assertTrue(repository.updateProjectSecondaryCount(7L, Int.MAX_VALUE))
            coVerify(exactly = 1) { projectDao.updateSecondaryCount(7L, Int.MAX_VALUE, any()) }
        }

    @Test
    fun `saveProjectNotes enforces first-use entitlement at repository boundary`() =
        runTest {
            coEvery { proManager.hasFeatureAfterInitialLoad(ProFeature.NOTES) } returns true
            every { proManager.hasCurrentFeature(ProFeature.NOTES) } returns false
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Project", notesCreated = false)

            val result = repository.saveProjectNotes(7L, "", "First note")

            assertEquals(ProjectNotesSaveResult.FeatureUnavailable, result)
            coVerify(exactly = 0) { projectDao.updateNotes(any(), any(), any(), any()) }
        }

    @Test
    fun `blank notes do not set the first-use flag`() =
        runTest {
            every { proManager.hasCurrentFeature(ProFeature.NOTES) } returns false
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)

            val saved = repository.saveProjectNotes(7L, "", "\n\t") as ProjectNotesSaveResult.Saved

            assertFalse(saved.project.notesCreated)
            coVerify(exactly = 1) { projectDao.updateNotes(7L, "\n\t", any(), false) }
        }

    @Test
    fun `saveProjectNotes allows existing notes and preserves monotonic use flag when blanked`() =
        runTest {
            every { proManager.hasCurrentFeature(ProFeature.NOTES) } returns false
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(
                    id = 7L,
                    name = "Project",
                    notes = "Existing note",
                    notesCreated = true,
                )

            val result = repository.saveProjectNotes(7L, "Existing note", "")

            val saved = result as ProjectNotesSaveResult.Saved
            assertEquals("", saved.project.notes)
            assertTrue(saved.project.notesCreated)
            coVerify(exactly = 1) { projectDao.updateNotes(7L, "", any(), false) }
        }

    @Test
    fun `saveProjectNotes maps database failure without reporting success`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Project", notesCreated = true)
            val failureRepository =
                createRepository(
                    transactionRunner =
                        object : DatabaseTransactionRunner {
                            override suspend fun <T> run(block: suspend () -> T): T {
                                block()
                                error("Database failure")
                            }
                        },
                )

            assertEquals(
                ProjectNotesSaveResult.PersistenceFailure,
                failureRepository.saveProjectNotes(7L, "", "Note"),
            )
        }

    @Test
    fun `updateProjectSecondaryCount gates first use inside transaction`() =
        runTest {
            coEvery { proManager.hasFeatureAfterInitialLoad(ProFeature.SECONDARY_COUNTER) } returns true
            every { proManager.hasCurrentFeature(ProFeature.SECONDARY_COUNTER) } returns false
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Project", secondaryCounterUsed = false)

            assertFalse(repository.updateProjectSecondaryCount(7L, 1))
            coVerify(exactly = 0) { projectDao.updateSecondaryCount(any(), any(), any()) }
        }

    @Test
    fun `secondary steps apply both directions to the stored count after entitlement lapses`() =
        runTest {
            every { proManager.hasCurrentFeature(ProFeature.SECONDARY_COUNTER) } returns false
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Project", secondaryCount = 4, secondaryCounterUsed = true)

            assertTrue(repository.applySecondaryCounterStep(7L, increment = true))
            assertTrue(repository.applySecondaryCounterStep(7L, increment = false))
            coVerify(exactly = 1) { projectDao.updateSecondaryCount(7L, 5, any()) }
            coVerify(exactly = 1) { projectDao.updateSecondaryCount(7L, 3, any()) }
        }

    @Test
    fun `updateProjectSecondaryCount keeps an existing secondary counter usable`() =
        runTest {
            every { proManager.hasCurrentFeature(ProFeature.SECONDARY_COUNTER) } returns false
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, name = "Project", secondaryCounterUsed = true)

            assertTrue(repository.updateProjectSecondaryCount(7L, 2))
            coVerify(exactly = 1) { projectDao.updateSecondaryCount(7L, 2, any()) }
        }

    @Test
    fun `counter repository exposes knit session domain models`() =
        runTest {
            every { sessionDao.getAllSessions(null) } returns
                flowOf(
                    listOf(
                        SessionEntity(
                            id = 11L,
                            projectId = 7L,
                            startedAt = 1_000L,
                            endedAt = 2_800L,
                            startRow = 12,
                            endRow = 18,
                            durationMinutes = 30,
                        ),
                    ),
                )

            val sessions: List<KnitSession> = repository.getAllSessions(null).first()

            assertEquals(11L, sessions.single().id)
            assertEquals(7L, sessions.single().projectId)
            assertEquals(12, sessions.single().startRow)
            assertEquals(18, sessions.single().endRow)
            assertEquals(30, sessions.single().durationMinutes)
        }

    @Test
    fun `counter repository accepts knit session domain model when inserting`() =
        runTest {
            val insertedSession = slot<SessionEntity>()
            coEvery { sessionDao.insert(capture(insertedSession)) } returns 55L

            val insertedId =
                repository.insertSession(
                    KnitSession(
                        projectId = 7L,
                        startedAt = 1_000L,
                        endedAt = 2_800L,
                        startRow = 12,
                        endRow = 18,
                        durationMinutes = 30,
                    ),
                )

            assertEquals(55L, insertedId)
            assertEquals(7L, insertedSession.captured.projectId)
            assertEquals(12, insertedSession.captured.startRow)
            assertEquals(18, insertedSession.captured.endRow)
            assertEquals(30, insertedSession.captured.durationMinutes)
        }

    @Test
    fun `deleteProject clears yarn card project links before deleting project`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)
            coEvery { sessionDao.getActiveSession() } returns null

            repository.deleteProject(7L)

            coVerifyOrder {
                yarnCardRepository.clearLinkedProject(7L)
                projectDao.delete(7L)
            }
        }

    @Test
    fun `deleteProject asks saved pattern repository to clean detached local PDF`() =
        runTest {
            val patternUri = "file:///data/user/0/com.finnvek.knittools/files/pattern_pdfs/7/pattern.pdf"
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L, patternUri = patternUri)

            repository.deleteProject(7L)

            coVerifyOrder {
                yarnCardRepository.clearLinkedProject(7L)
                projectDao.delete(7L)
                savedPatternRepository.deleteLocalPatternFileIfUnused(patternUri)
            }
        }

    @Test
    fun `detachPattern delegates primary removal to project document repository`() =
        runTest {
            val patternUri = "file:///data/user/0/com.finnvek.knittools/files/pattern_pdfs/7/pattern.pdf"
            val document = projectDocument(patternUri, "Pattern")
            coEvery { projectDocumentRepository.getPrimary(7L) } returns document
            coEvery { projectDocumentRepository.remove(7L, document.id) } returns
                ProjectDocumentMutationResult.Removed(document, null)

            repository.detachPattern(7L)

            coVerifyOrder {
                projectDocumentRepository.getPrimary(7L)
                projectDocumentRepository.remove(7L, document.id)
            }
        }

    @Test
    fun `detachPattern preserves the document repository failure result`() =
        runTest {
            val document = projectDocument("content://pattern/7", "Pattern")
            coEvery { projectDocumentRepository.getPrimary(7L) } returns document
            coEvery { projectDocumentRepository.remove(7L, document.id) } returns
                ProjectDocumentMutationResult.PersistenceFailure

            assertEquals(ProjectDocumentMutationResult.PersistenceFailure, repository.detachPattern(7L))
        }

    @Test
    fun `attachPattern adds a document and mirrors primary metadata`() =
        runTest {
            val newPatternUri = "file:///data/user/0/com.finnvek.knittools/files/pattern_pdfs/7/new.pdf"
            val document = projectDocument(newPatternUri, "New pattern")
            coEvery { projectDocumentRepository.preflightImportedPdf(newPatternUri, "New pattern") } returns null
            coEvery {
                projectDocumentRepository.addImportedPdfInCurrentTransaction(7L, newPatternUri, "New pattern")
            } returns
                ProjectDocumentMutationResult.Added(document)
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)

            val result = repository.attachPattern(7L, newPatternUri, "New pattern", 0, null)

            assertEquals(ProjectDocumentMutationResult.Added(document), result)
            coVerifyOrder {
                projectDocumentRepository.preflightImportedPdf(newPatternUri, "New pattern")
                projectDocumentRepository.addImportedPdfInCurrentTransaction(7L, newPatternUri, "New pattern")
                projectDao.updatePatternInformation(
                    id = 7L,
                    linkedPatternId = null,
                    patternName = "New pattern",
                    updatedAt = any(),
                )
            }
        }

    @Test
    fun `attachPattern preserves an existing web metadata relationship`() =
        runTest {
            val newPatternUri = "file:///data/user/0/com.finnvek.knittools/files/pattern_pdfs/7/new.pdf"
            val document = projectDocument(newPatternUri, "New pattern")
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, linkedPatternId = 99L, patternName = "Web pattern")
            coEvery { projectDocumentRepository.preflightImportedPdf(newPatternUri, "New pattern") } returns null
            coEvery {
                projectDocumentRepository.addImportedPdfInCurrentTransaction(7L, newPatternUri, "New pattern")
            } returns
                ProjectDocumentMutationResult.Added(document)

            val result = repository.attachPattern(7L, newPatternUri, "New pattern", 0, null)

            assertEquals(ProjectDocumentMutationResult.Added(document), result)
            coVerify(exactly = 0) { projectDao.updatePatternInformation(any(), any(), any(), any()) }
        }

    @Test
    fun `attach web metadata verifies both rows and leaves documents untouched`() =
        runTest {
            val pattern = webPattern(id = 99L, name = "Web pattern")
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)
            coEvery { savedPatternRepository.getById(99L) } returns pattern

            val result = repository.attachSavedPatternMetadata(7L, 99L)

            assertEquals(SavedPatternMetadataMutationResult.Attached(99L), result)
            coVerify(exactly = 1) {
                projectDao.updatePatternInformation(
                    id = 7L,
                    linkedPatternId = 99L,
                    patternName = "Web pattern",
                    updatedAt = any(),
                )
            }
            coVerify(exactly = 0) { projectDocumentRepository.addSavedPattern(any(), any()) }
            coVerify(exactly = 0) { projectDocumentRepository.remove(any(), any()) }
        }

    @Test
    fun `attach web metadata is idempotent and rejects missing or non-web rows`() =
        runTest {
            val pattern = webPattern(id = 99L, name = "Web pattern")
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, linkedPatternId = 99L, patternName = "Web pattern")
            coEvery { savedPatternRepository.getById(99L) } returns pattern
            assertEquals(
                SavedPatternMetadataMutationResult.AlreadyAttached(99L),
                repository.attachSavedPatternMetadata(7L, 99L),
            )
            coVerify(exactly = 0) { projectDao.updatePatternInformation(any(), any(), any(), any()) }

            coEvery { projectDao.getProject(8L) } returns null
            assertEquals(
                SavedPatternMetadataMutationResult.ProjectMissing,
                repository.attachSavedPatternMetadata(8L, 99L),
            )

            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)
            coEvery { savedPatternRepository.getById(100L) } returns null
            assertEquals(
                SavedPatternMetadataMutationResult.PatternMissing,
                repository.attachSavedPatternMetadata(7L, 100L),
            )

            coEvery { savedPatternRepository.getById(101L) } returns
                pattern.copy(
                    id = 101L,
                    source = SavedPatternSource.LocalFile,
                    originalUrl = "content://pattern.pdf",
                    canonicalUrl = "",
                    localPdfUri = "content://pattern.pdf",
                )
            assertEquals(
                SavedPatternMetadataMutationResult.NotWebPattern,
                repository.attachSavedPatternMetadata(7L, 101L),
            )
        }

    @Test
    fun `attach web metadata requires explicit replacement authorization`() =
        runTest {
            val pattern = webPattern(id = 99L, name = "Replacement")
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, linkedPatternId = 88L, patternName = "Current")
            coEvery { savedPatternRepository.getById(99L) } returns pattern

            assertEquals(
                SavedPatternMetadataMutationResult.ReplacementRequired(88L),
                repository.attachSavedPatternMetadata(7L, 99L),
            )
            coVerify(exactly = 0) { projectDao.updatePatternInformation(any(), any(), any(), any()) }

            assertEquals(
                SavedPatternMetadataMutationResult.Attached(99L),
                repository.attachSavedPatternMetadata(7L, 99L, expectedExistingSavedPatternId = 88L),
            )
            coVerify(exactly = 1) {
                projectDao.updatePatternInformation(7L, 99L, "Replacement", any())
            }

            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, linkedPatternId = 77L, patternName = "Concurrent replacement")
            assertEquals(
                SavedPatternMetadataMutationResult.StaleAction,
                repository.attachSavedPatternMetadata(7L, 99L, expectedExistingSavedPatternId = 88L),
            )
            coVerify(exactly = 1) {
                projectDao.updatePatternInformation(7L, 99L, "Replacement", any())
            }
        }

    @Test
    fun `legacy general attach cannot bypass web metadata replacement confirmation`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, linkedPatternId = 88L, patternName = "Current")
            coEvery { savedPatternRepository.getById(99L) } returns webPattern(99L, "Replacement")
            coEvery { projectDocumentRepository.prepareSavedPatternDocument(7L, 99L) } returns
                SavedPatternDocumentPreparation.MetadataOnlyPattern

            assertEquals(null, repository.attachSavedPattern(7L, 99L))
            coVerify(exactly = 0) { projectDao.updatePatternInformation(any(), any(), any(), any()) }
        }

    @Test
    fun `ready saved pattern attachment adds the document and preserves existing metadata`() =
        runTest {
            val patternUri = "file:///data/user/0/com.finnvek.knittools/files/pattern_pdfs/7/pattern.pdf"
            val pattern =
                webPattern(12L, "Cardigan").copy(
                    source = SavedPatternSource.LocalFile,
                    originalUrl = patternUri,
                    canonicalUrl = "",
                    localPdfUri = patternUri,
                )
            val document = projectDocument(patternUri, pattern.name).copy(savedPatternId = pattern.id)
            coEvery { projectDao.getProject(7L) } returnsMany
                listOf(
                    CounterProjectEntity(id = 7L),
                    CounterProjectEntity(id = 7L, linkedPatternId = 99L, patternName = "Web pattern"),
                )
            coEvery { savedPatternRepository.getById(pattern.id) } returns pattern
            coEvery { projectDocumentRepository.prepareSavedPatternDocument(7L, pattern.id) } returns
                SavedPatternDocumentPreparation.Ready(patternUri)
            coEvery {
                projectDocumentRepository.addSavedPatternInCurrentTransaction(7L, pattern.id, patternUri)
            } returns ProjectDocumentMutationResult.Added(document)

            assertEquals(pattern, repository.attachSavedPattern(7L, pattern.id))
            assertEquals(pattern, repository.attachSavedPattern(7L, pattern.id))

            coVerify(exactly = 2) {
                projectDocumentRepository.addSavedPatternInCurrentTransaction(7L, pattern.id, patternUri)
            }
            coVerify(exactly = 1) {
                projectDao.updatePatternInformation(7L, pattern.id, pattern.name, any())
            }
        }

    @Test
    fun `already attached saved pattern is revalidated inside the transaction`() =
        runTest {
            val pattern = webPattern(12L, "Cardigan")
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)
            coEvery { savedPatternRepository.getById(pattern.id) } returns pattern
            coEvery { projectDocumentRepository.prepareSavedPatternDocument(7L, pattern.id) } returns
                SavedPatternDocumentPreparation.AlreadyAttached
            coEvery {
                projectDocumentRepository.hasSavedPatternDocumentInCurrentTransaction(7L, pattern.id)
            } returnsMany listOf(true, false)

            assertEquals(pattern, repository.attachSavedPattern(7L, pattern.id))
            assertEquals(null, repository.attachSavedPattern(7L, pattern.id))
        }

    @Test
    fun `dedicated metadata unlink clears only the expected relationship`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, linkedPatternId = 99L, patternName = "Web pattern")
            coEvery { projectDao.clearPatternInformationIfLinked(7L, 99L, any()) } returns 1

            assertEquals(
                SavedPatternMetadataMutationResult.Unlinked,
                repository.unlinkSavedPatternMetadata(7L, 99L),
            )
            coVerify(exactly = 0) { projectDocumentRepository.remove(any(), any()) }

            coEvery { projectDao.getProject(7L) } returns
                CounterProjectEntity(id = 7L, linkedPatternId = 100L, patternName = "New relationship")
            assertEquals(
                SavedPatternMetadataMutationResult.StaleAction,
                repository.unlinkSavedPatternMetadata(7L, 99L),
            )
            coVerify(exactly = 1) { projectDao.clearPatternInformationIfLinked(7L, 99L, any()) }
        }

    @Test
    fun `attachPattern returns already attached without rewriting primary metadata`() =
        runTest {
            val patternUri = "file:///data/user/0/com.finnvek.knittools/files/pattern_pdfs/7/pattern.pdf"
            coEvery { projectDocumentRepository.preflightImportedPdf(patternUri, "Pattern") } returns null
            coEvery {
                projectDocumentRepository.addImportedPdfInCurrentTransaction(7L, patternUri, "Pattern")
            } returns
                ProjectDocumentMutationResult.AlreadyAttached

            val result = repository.attachPattern(7L, patternUri, "Pattern", 0, null)

            assertEquals(ProjectDocumentMutationResult.AlreadyAttached, result)
            coVerify(exactly = 0) { projectDao.updatePatternInformation(any(), any(), any(), any()) }
        }

    @Test
    fun `attachPattern returns duplicate uri without rewriting primary metadata`() =
        runTest {
            val patternUri = "file:///data/user/0/com.finnvek.knittools/files/pattern_pdfs/7/pattern.pdf"
            coEvery { projectDocumentRepository.preflightImportedPdf(patternUri, "Pattern") } returns null
            coEvery {
                projectDocumentRepository.addImportedPdfInCurrentTransaction(7L, patternUri, "Pattern")
            } returns
                ProjectDocumentMutationResult.DuplicateUri

            val result = repository.attachPattern(7L, patternUri, "Pattern", 0, null)

            assertEquals(ProjectDocumentMutationResult.DuplicateUri, result)
            coVerify(exactly = 0) { projectDao.updatePatternInformation(any(), any(), any(), any()) }
        }

    @Test
    fun `document mapping replacement rejects a changed base and accepts the current base`() =
        runTest {
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L, name = "Project")
            var stored =
                projectDocument("file:///chart.pdf", "Chart").copy(
                    id = 42L,
                    rowMapping = "[{\"row\":20,\"page\":4,\"yPosition\":0.8}]",
                    readingLineFollowCurrentRow = false,
                )
            val currentMapping = stored.rowMapping
            coEvery { projectDocumentRepository.getDocument(42L) } coAnswers { stored }
            coEvery { projectDocumentRepository.updateViewerStateInTransaction(any()) } coAnswers {
                stored = firstArg()
                true
            }

            assertFalse(repository.updatePatternRowMappingIfUnchanged(7L, "[]", 42L, expectedMapping = null))
            assertEquals(currentMapping, stored.rowMapping)
            coVerify(exactly = 0) { projectDocumentRepository.updateViewerStateInTransaction(any()) }
            assertTrue(repository.updatePatternRowMappingIfUnchanged(7L, "[]", 42L, currentMapping))
            assertEquals("[]", stored.rowMapping)
            assertEquals(42L, stored.id)
            assertEquals("project:7:document", stored.documentKey)
            assertFalse(repository.updatePatternRowMappingIfUnchanged(8L, null, 42L, "[]"))
            assertEquals("[]", stored.rowMapping)
        }

    private fun projectDocument(
        localPdfUri: String,
        label: String,
    ): ProjectDocument =
        ProjectDocument(
            id = 13L,
            projectId = 7L,
            savedPatternId = null,
            documentKey = "project:7:document",
            label = label,
            localPdfUri = localPdfUri,
            sortOrder = 0,
            isPrimary = true,
            currentPage = 0,
            rowMapping = null,
            readingLineEnabled = false,
            readingLineYFraction = 0.5f,
            readingLineFollowCurrentRow = true,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = 0.5f,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun webPattern(
        id: Long,
        name: String,
    ): SavedPattern =
        SavedPattern(
            id = id,
            source = SavedPatternSource.WebLink,
            name = name,
            designerName = "",
            originalUrl = "https://example.com/pattern",
            canonicalUrl = "https://example.com/pattern",
        )
}

private class ProjectCreationTransactionRunner : DatabaseTransactionRunner {
    var runCount: Int = 0

    override suspend fun <T> run(block: suspend () -> T): T {
        runCount += 1
        return block()
    }
}
