package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProjectCounterRepositoryTest {
    private lateinit var fakeDao: FakeProjectCounterDao
    private lateinit var projectDao: CounterProjectDao
    private lateinit var proManager: ProManager
    private lateinit var repository: ProjectCounterRepository

    @Before
    fun setup() {
        fakeDao = FakeProjectCounterDao()
        projectDao = mockk(relaxed = true)
        proManager = mockk()
        coEvery { projectDao.getProject(any()) } answers {
            CounterProjectEntity(id = firstArg(), name = "Project")
        }
        every { proManager.hasFeature(any()) } returns true
        repository = ProjectCounterRepository(fakeDao, projectDao, proManager, ImmediateDatabaseTransactionRunner)
    }

    @Test
    fun `addCounter truncates name to 50 chars`() =
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
            repository.addCounter(
                ProjectCounter(
                    projectId = 1L,
                    name = "Test",
                    stepSize = 1,
                    repeatAt = 10,
                    counterType = ProjectCounterType.REPEATING,
                ),
            )

            assertEquals(10, fakeDao.lastInserted!!.repeatAt)
        }

    @Test
    fun `addCounter trims names and rejects blank values`() =
        runTest {
            val success = repository.addCounter(ProjectCounter(projectId = 1L, name = "  Sleeve  "))
            val invalid = repository.addCounter(ProjectCounter(projectId = 1L, name = "   "))

            assertEquals(ProjectCounterMutationResult.Success(1L), success)
            assertEquals("Sleeve", fakeDao.lastInserted!!.name)
            assertEquals(ProjectCounterMutationResult.InvalidCounter, invalid)
        }

    @Test
    fun `addCounter enforces creation entitlement in repository`() =
        runTest {
            every { proManager.hasFeature(ProFeature.MULTIPLE_COUNTERS) } returns false

            val result = repository.addCounter(ProjectCounter(projectId = 1L, name = "Sleeve"))

            assertEquals(ProjectCounterMutationResult.FeatureUnavailable, result)
        }

    @Test
    fun `addCounter clears main-counter link for repeat sections`() =
        runTest {
            repository.addCounter(
                ProjectCounter(
                    projectId = 1L,
                    name = "Repeat section",
                    counterType = ProjectCounterType.REPEAT_SECTION,
                    repeatStartRow = 1,
                    repeatEndRow = 4,
                    totalRepeats = 2,
                    currentRepeat = 1,
                    linkedToMainCounter = true,
                ),
            )

            assertEquals(false, fakeDao.lastInserted!!.linkedToMainCounter)
        }

    @Test
    fun `incrementCounter applies counter increment rules`() =
        runTest {
            val counter = ProjectCounter(id = 1, projectId = 1, name = "Test", count = 5, stepSize = 2)
            fakeDao.store(counter)
            repository.incrementCounter(counter)

            // ProjectCounterLogic.increment: 5 + 2 = 7
            assertEquals(1L, fakeDao.lastUpdatedId)
            assertEquals(7, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `incrementCounter resets at repeatAt`() =
        runTest {
            val counter =
                ProjectCounter(
                    id = 1,
                    projectId = 1,
                    name = "Test",
                    count = 9,
                    stepSize = 1,
                    repeatAt = 10,
                    counterType = ProjectCounterType.REPEATING,
                )
            fakeDao.store(counter)
            repository.incrementCounter(counter)

            // ProjectCounterLogic.increment: 9 + 1 = 10 >= repeatAt(10) -> reset to 0
            assertEquals(0, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `decrementCounter does not go below zero`() =
        runTest {
            val counter = ProjectCounter(id = 1, projectId = 1, name = "Test", count = 1, stepSize = 3)
            fakeDao.store(counter)
            repository.decrementCounter(counter)

            // ProjectCounterLogic.decrement: (1 - 3).coerceAtLeast(0) = 0
            assertEquals(0, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `consecutive increments use latest stored count`() =
        runTest {
            val counter = ProjectCounter(id = 1, projectId = 1, name = "Sleeve", count = 5, stepSize = 1)
            fakeDao.store(counter)

            repository.incrementCounter(counter)
            repository.incrementCounter(counter)

            assertEquals(7, fakeDao.storedCount(1L))
        }

    @Test
    fun `consecutive decrements use latest stored count`() =
        runTest {
            val counter = ProjectCounter(id = 1, projectId = 1, name = "Sleeve", count = 5, stepSize = 2)
            fakeDao.store(counter)

            repository.decrementCounter(counter)
            repository.decrementCounter(counter)

            assertEquals(1, fakeDao.storedCount(1L))
        }

    @Test
    fun `consecutive repeating increments continue after reset`() =
        runTest {
            val counter =
                ProjectCounter(
                    id = 1,
                    projectId = 1,
                    name = "Repeat",
                    count = 7,
                    stepSize = 1,
                    repeatAt = 8,
                    counterType = ProjectCounterType.REPEATING,
                )
            fakeDao.store(counter)

            repository.incrementCounter(counter)
            repository.incrementCounter(counter)

            assertEquals(1, fakeDao.storedCount(1L))
        }

    @Test
    fun `resetCounter sets count to zero`() =
        runTest {
            fakeDao.store(ProjectCounter(id = 5L, projectId = 1L, name = "Counter", count = 4))
            repository.resetCounter(1L, 5L)
            assertEquals(5L, fakeDao.lastUpdatedId)
            assertEquals(0, fakeDao.lastUpdatedCount)
        }

    @Test
    fun `renameCounter truncates name to 50 chars`() =
        runTest {
            val longName = "B".repeat(80)
            fakeDao.store(ProjectCounter(id = 3L, projectId = 1L, name = "Counter"))
            repository.renameCounter(1L, 3L, longName)

            assertEquals(3L, fakeDao.lastRenamedId)
            assertEquals(50, fakeDao.lastRenamedName!!.length)
        }

    @Test
    fun `deleteCounter calls dao delete`() =
        runTest {
            fakeDao.store(ProjectCounter(id = 7L, projectId = 1L, name = "Counter"))
            repository.deleteCounter(1L, 7L)
            assertEquals(7L, fakeDao.lastDeletedId)
        }

    @Test
    fun `mutations reject a counter owned by another project`() =
        runTest {
            fakeDao.store(ProjectCounter(id = 7L, projectId = 2L, name = "Counter"))

            val result = repository.deleteCounter(1L, 7L)

            assertEquals(ProjectCounterMutationResult.StaleAction, result)
            assertEquals(-1L, fakeDao.lastDeletedId)
        }

    @Test
    fun `delete distinguishes missing project counter and persistence failure`() =
        runTest {
            assertEquals(
                ProjectCounterMutationResult.CounterUnavailable,
                repository.deleteCounter(1L, 99L),
            )

            fakeDao.store(ProjectCounter(id = 7L, projectId = 1L, name = "Counter"))
            fakeDao.failDelete = true
            assertEquals(
                ProjectCounterMutationResult.PersistenceFailure,
                repository.deleteCounter(1L, 7L),
            )
        }

    // -- Fake DAO --

    private class FakeProjectCounterDao : ProjectCounterDao {
        private val storedCounters = mutableMapOf<Long, ProjectCounterEntity>()
        var lastInserted: ProjectCounterEntity? = null
        var lastUpdatedId: Long = -1
        var lastUpdatedCount: Int = -1
        var lastDeletedId: Long = -1
        var lastRenamedId: Long = -1
        var lastRenamedName: String? = null
        var failDelete = false

        override fun getCountersForProject(projectId: Long): Flow<List<ProjectCounterEntity>> = flowOf(emptyList())

        override suspend fun getCounter(id: Long): ProjectCounterEntity? = storedCounters[id]

        fun store(counter: ProjectCounter) {
            storedCounters[counter.id] = counter.toEntity()
        }

        fun storedCount(id: Long): Int = storedCounters.getValue(id).count

        override suspend fun insert(counter: ProjectCounterEntity): Long {
            lastInserted = counter
            return 1L
        }

        override suspend fun update(counter: ProjectCounterEntity) {
            storedCounters[counter.id] = counter
        }

        override suspend fun delete(id: Long) {
            if (failDelete) error("delete failed")
            lastDeletedId = id
        }

        override suspend fun updateCount(
            id: Long,
            count: Int,
        ) {
            storedCounters[id]?.let { storedCounters[id] = it.copy(count = count) }
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
