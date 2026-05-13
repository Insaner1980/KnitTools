package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CounterRepositoryDomainApiTest {
    private lateinit var projectDao: CounterProjectDao
    private lateinit var sessionDao: SessionDao
    private lateinit var yarnCardRepository: YarnCardRepository
    private lateinit var repository: CounterRepository

    @Before
    fun setup() {
        projectDao = mockk(relaxed = true)
        sessionDao = mockk(relaxed = true)
        yarnCardRepository = mockk(relaxed = true)
        repository =
            CounterRepository(
                dao = projectDao,
                sessionDao = sessionDao,
                photoStorage = mockk<ProgressPhotoStorage>(relaxed = true),
                context = mockk<Context>(relaxed = true),
                yarnCardRepository = yarnCardRepository,
                savedPatternRepository = mockk(relaxed = true),
                patternAnnotationRepository = mockk(relaxed = true),
                transactionRunner = ImmediateDatabaseTransactionRunner,
                ioDispatcher = Dispatchers.Unconfined,
            )
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
            repository.deleteProject(7L)

            coVerifyOrder {
                yarnCardRepository.clearLinkedProject(7L)
                projectDao.delete(7L)
            }
        }
}
