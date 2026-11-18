package com.finnvek.knittools.data.local

import dagger.Lazy
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DebugDemoDataSeederTest {
    @Test
    fun `available capacity inserts the complete demo session set`() =
        runTest {
            val seed = SeedState(MAX_COMPLETED_SESSIONS - 13)

            seed.start(this)

            assertEquals(13, seed.insertedSessions.size)
            assertEquals(MAX_COMPLETED_SESSIONS, seed.completedCount())
            assertTrue(seed.projects.any { it.name == "Forest Cardigan" })
        }

    @Test
    fun `one missing slot skips the complete demo session set`() =
        runTest {
            val seed = SeedState(MAX_COMPLETED_SESSIONS - 12)

            seed.start(this)

            assertTrue(seed.insertedSessions.isEmpty())
            assertEquals(MAX_COMPLETED_SESSIONS - 12, seed.completedCount())
            assertTrue(seed.projects.any { it.name == "Forest Cardigan" })
            assertEquals(5, seed.projects.size)
        }

    @Test
    fun `full history stays unchanged and marker prevents a later retry`() =
        runTest {
            val seed = SeedState(MAX_COMPLETED_SESSIONS)

            seed.start(this)
            assertTrue(seed.insertedSessions.isEmpty())
            assertEquals(MAX_COMPLETED_SESSIONS, seed.completedCount())
            assertTrue(seed.projects.any { it.name == "Forest Cardigan" })

            seed.start(this)
            assertTrue(seed.insertedSessions.isEmpty())
            assertEquals(MAX_COMPLETED_SESSIONS, seed.completedCount())
            assertEquals(5, seed.projects.size)
        }

    @Test
    fun deletingTheOriginalMarkerDoesNotDuplicateRemainingDemoData() =
        runTest {
            val seed = SeedState(0)
            seed.start(this)
            seed.projects.removeAll { it.name == "Forest Cardigan" }
            seed.start(this)
            assertEquals(4, seed.projects.size)
            assertEquals(13, seed.insertedSessions.size)
        }

    @Test
    fun `demo transaction failure leaves sibling startup work running`() =
        runTest {
            val runner = mockk<DatabaseTransactionRunner>()
            coEvery { runner.run<Unit>(any()) } throws IOException("demo database unavailable")
            val seed = SeedState(0)
            var siblingInitialized = false

            seed.start(this, runner)
            launch { siblingInitialized = true }.join()

            assertTrue(siblingInitialized)
            assertTrue(seed.projects.isEmpty())
        }

    private class SeedState(
        private val existingSessions: Long,
    ) {
        val projects = mutableListOf<CounterProjectEntity>()
        val insertedSessions = mutableListOf<SessionEntity>()
        private val database = mockk<KnitToolsDatabase>(relaxed = true)
        private val projectDao = mockk<CounterProjectDao>(relaxed = true)
        private val sessionDao = mockk<SessionDao>(relaxed = true)

        init {
            io.mockk.every { database.counterProjectDao() } returns projectDao
            io.mockk.every { database.sessionDao() } returns sessionDao
            coEvery { projectDao.getAllProjectsOnce() } answers { projects.toList() }
            coEvery { projectDao.insert(any()) } answers {
                projects += firstArg<CounterProjectEntity>()
                projects.size.toLong()
            }
            coEvery { sessionDao.countCompletedSessions() } answers { completedCount() }
            coEvery { sessionDao.insert(any()) } answers {
                insertedSessions += firstArg<SessionEntity>()
                insertedSessions.size.toLong()
            }
        }

        fun completedCount(): Long = existingSessions + insertedSessions.size

        fun start(
            scope: kotlinx.coroutines.CoroutineScope,
            runner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
        ) {
            DebugDemoDataSeeder.seedIfNeeded(
                applicationScope = scope,
                ioDispatcher = Dispatchers.Unconfined,
                database = Lazy { database },
                transactionRunner = Lazy { runner },
                addCounter = { 1L },
                saveYarnCard = { 1L },
            )
        }
    }
}
