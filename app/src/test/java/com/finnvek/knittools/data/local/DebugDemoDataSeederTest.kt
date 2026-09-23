package com.finnvek.knittools.data.local

import dagger.Lazy
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

        fun start(scope: kotlinx.coroutines.CoroutineScope) {
            DebugDemoDataSeeder.seedIfNeeded(
                applicationScope = scope,
                ioDispatcher = Dispatchers.Unconfined,
                database = Lazy { database },
                transactionRunner = Lazy { ImmediateDatabaseTransactionRunner },
                addCounter = { 1L },
                saveYarnCard = { 1L },
            )
        }
    }
}
