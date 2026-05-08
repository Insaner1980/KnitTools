package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectCounterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProjectCounterRepositoryTest {
    private lateinit var fakeDao: FakeProjectCounterDao
    private lateinit var repository: ProjectCounterRepository

    @Before
    fun setup() {
        fakeDao = FakeProjectCounterDao()
        repository = ProjectCounterRepository(fakeDao)
    }

    @Test
    fun `addCounter truncates name to 50 chars`() =
        runTest {
            val longName = "A".repeat(100)
            repository.addCounter(ProjectCounterEntity(projectId = 1L, name = longName, stepSize = 1))

            assertEquals(50, fakeDao.lastInserted!!.name.length)
        }

    @Test
    fun `addCounter keeps short name intact`() =
        runTest {
            repository.addCounter(ProjectCounterEntity(projectId = 1L, name = "Sleeve counter", stepSize = 2))

            assertEquals("Sleeve counter", fakeDao.lastInserted!!.name)
            assertEquals(2, fakeDao.lastInserted!!.stepSize)
        }

    @Test
    fun `addCounter passes repeatAt`() =
        runTest {
            repository.addCounter(ProjectCounterEntity(projectId = 1L, name = "Test", stepSize = 1, repeatAt = 10))

            assertEquals(10, fakeDao.lastInserted!!.repeatAt)
        }

    @Test
    fun `incrementCounter delegates to ProjectCounterLogic`() =
        runTest {
            val counter = ProjectCounterEntity(id = 1, projectId = 1, name = "Test", count = 5, stepSize = 2)
            repository.incrementCounter(counter)

            // ProjectCounterLogic.increment: 5 + 2 = 7
            assertEquals(1L, fakeDao.lastUpdatedId)
            assertEquals(7, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `incrementCounter resets at repeatAt`() =
        runTest {
            val counter =
                ProjectCounterEntity(id = 1, projectId = 1, name = "Test", count = 9, stepSize = 1, repeatAt = 10)
            repository.incrementCounter(counter)

            // ProjectCounterLogic.increment: 9 + 1 = 10 >= repeatAt(10) -> reset to 0
            assertEquals(0, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `decrementCounter does not go below zero`() =
        runTest {
            val counter = ProjectCounterEntity(id = 1, projectId = 1, name = "Test", count = 1, stepSize = 3)
            repository.decrementCounter(counter)

            // ProjectCounterLogic.decrement: (1 - 3).coerceAtLeast(0) = 0
            assertEquals(0, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `resetCounter sets count to zero`() =
        runTest {
            repository.resetCounter(5L)
            assertEquals(5L, fakeDao.lastUpdatedId)
            assertEquals(0, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `renameCounter truncates name to 50 chars`() =
        runTest {
            val longName = "B".repeat(80)
            repository.renameCounter(3L, longName)

            assertEquals(3L, fakeDao.lastRenamedId)
            assertEquals(50, fakeDao.lastRenamedName!!.length)
        }

    @Test
    fun `deleteCounter calls dao delete`() =
        runTest {
            repository.deleteCounter(7L)
            assertEquals(7L, fakeDao.lastDeletedId)
        }

    // -- Fake DAO --

    private class FakeProjectCounterDao : ProjectCounterDao {
        var lastInserted: ProjectCounterEntity? = null
        var lastUpdatedId: Long = -1
        var lastUpdatedCount: Int = -1
        var lastDeletedId: Long = -1
        var lastRenamedId: Long = -1
        var lastRenamedName: String? = null

        override fun getCountersForProject(projectId: Long): Flow<List<ProjectCounterEntity>> = flowOf(emptyList())

        override suspend fun insert(counter: ProjectCounterEntity): Long {
            lastInserted = counter
            return 1L
        }

        override suspend fun update(counter: ProjectCounterEntity) {}

        override suspend fun delete(id: Long) {
            lastDeletedId = id
        }

        override suspend fun updateCount(
            id: Long,
            count: Int,
        ) {
            lastUpdatedId = id
            lastUpdatedCount = count
        }

        override suspend fun updateName(
            id: Long,
            name: String,
        ) {
            lastRenamedId = id
            lastRenamedName = name
        }

        override suspend fun updateRepeatSectionState(
            id: Long,
            count: Int,
            currentRepeat: Int?,
        ) {
            lastUpdatedId = id
            lastUpdatedCount = count
        }
    }
}
