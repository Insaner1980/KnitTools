package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.domain.model.ProjectCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        fakeDao =
            FakeProjectCounterDao(
                counters =
                    listOf(
                        ProjectCounterEntity(
                            id = 42L,
                            projectId = 1L,
                            name = "Sleeve counter",
                            count = 3,
                            stepSize = 2,
                        ),
                    ),
            )
        repository = ProjectCounterRepository(fakeDao)
    }

    @Test
    fun `getCountersForProject exposes domain counters`() =
        runTest {
            val counters: List<ProjectCounter> = repository.getCountersForProject(1L).first()

            assertEquals("Sleeve counter", counters.single().name)
            assertEquals(3, counters.single().count)
        }

    @Test
    fun `addCounter accepts domain counter and truncates name to 50 chars`() =
        runTest {
            val longName = "A".repeat(100)
            repository.addCounter(ProjectCounter(projectId = 1L, name = longName, stepSize = 1))

            assertEquals(50, fakeDao.lastInserted!!.name.length)
        }

    @Test
    fun `addCounter keeps short name intact`() =
        runTest {
            repository.addCounter(ProjectCounter(projectId = 1L, name = "Sleeve counter", stepSize = 2))

            assertEquals("Sleeve counter", fakeDao.lastInserted!!.name)
            assertEquals(2, fakeDao.lastInserted!!.stepSize)
        }

    @Test
    fun `addCounter passes repeatAt`() =
        runTest {
            repository.addCounter(ProjectCounter(projectId = 1L, name = "Test", stepSize = 1, repeatAt = 10))

            assertEquals(10, fakeDao.lastInserted!!.repeatAt)
        }

    @Test
    fun `incrementCounter delegates domain counter to ProjectCounterLogic`() =
        runTest {
            val counter = ProjectCounter(id = 1, projectId = 1, name = "Test", count = 5, stepSize = 2)
            repository.incrementCounter(counter)

            assertEquals(1L, fakeDao.lastUpdatedId)
            assertEquals(7, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `incrementCounter resets at repeatAt`() =
        runTest {
            val counter = ProjectCounter(id = 1, projectId = 1, name = "Test", count = 9, stepSize = 1, repeatAt = 10)
            repository.incrementCounter(counter)

            assertEquals(0, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `decrementCounter does not go below zero`() =
        runTest {
            val counter = ProjectCounter(id = 1, projectId = 1, name = "Test", count = 1, stepSize = 3)
            repository.decrementCounter(counter)

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

    private class FakeProjectCounterDao(
        private val counters: List<ProjectCounterEntity> = emptyList(),
    ) : ProjectCounterDao {
        var lastInserted: ProjectCounterEntity? = null
        var lastUpdatedId: Long = -1
        var lastUpdatedCount: Int = -1
        var lastDeletedId: Long = -1
        var lastRenamedId: Long = -1
        var lastRenamedName: String? = null

        override fun getCountersForProject(projectId: Long): Flow<List<ProjectCounterEntity>> =
            flowOf(counters.filter { it.projectId == projectId })

        override suspend fun insert(counter: ProjectCounterEntity): Long {
            lastInserted = counter
            return 1L
        }

        override suspend fun update(counter: ProjectCounterEntity) {
            // Test fake — ei tarvetta päivitysseurantaan
        }

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
