package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.RowReminderEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReminderRepositoryTest {
    private lateinit var fakeDao: FakeRowReminderDao
    private lateinit var projectDao: CounterProjectDao
    private lateinit var proManager: ProManager
    private lateinit var repository: ReminderRepository

    @Before
    fun setup() {
        fakeDao =
            FakeRowReminderDao(
                reminders =
                    listOf(
                        RowReminderEntity(
                            id = 5L,
                            projectId = 1L,
                            targetRow = 12,
                            repeatInterval = 4,
                            message = "Increase",
                        ),
                    ),
            )
        projectDao = mockk()
        proManager = mockk()
        coEvery { projectDao.getProject(any()) } answers {
            CounterProjectEntity(id = firstArg(), name = "Project")
        }
        every { proManager.hasFeature(any()) } returns true
        repository = ReminderRepository(fakeDao, projectDao, proManager, ImmediateDatabaseTransactionRunner)
    }

    @Test
    fun `getRemindersForProject exposes domain reminders`() =
        runTest {
            val reminders: List<RowReminder> = repository.getRemindersForProject(1L).first()

            assertEquals(12, reminders.single().targetRow)
            assertEquals("Increase", reminders.single().message)
        }

    @Test
    fun `insert accepts domain reminder and writes entity`() =
        runTest {
            val id =
                repository.insert(
                    RowReminder(
                        projectId = 1L,
                        targetRow = 20,
                        repeatInterval = null,
                        message = "Bind off",
                    ),
                )

            assertEquals(ReminderMutationResult.Success(55L), id)
            val inserted = fakeDao.lastInserted ?: error("Reminder was not inserted")
            assertEquals(1L, inserted.projectId)
            assertEquals(20, inserted.targetRow)
            assertEquals(null, inserted.repeatInterval)
            assertEquals("Bind off", inserted.message)
            assertEquals(false, inserted.isCompleted)
        }

    @Test
    fun `update accepts domain reminder and writes entity`() =
        runTest {
            repository.update(
                1L,
                fakeDao.reminder(5L).toDomain().copy(isCompleted = true),
            )

            assertEquals(true, fakeDao.lastUpdated?.isCompleted)
            assertEquals(5L, fakeDao.lastUpdated?.id)
        }

    @Test
    fun `delete calls dao delete`() =
        runTest {
            repository.delete(1L, 5L)

            assertEquals(5L, fakeDao.lastDeletedId)
        }

    @Test
    fun `insert validates and normalizes reminder at repository boundary`() =
        runTest {
            val result =
                repository.insert(
                    RowReminder(
                        projectId = 1L,
                        targetRow = 1,
                        repeatInterval = 2,
                        message = "  ${"x".repeat(199)}\uD83E\uDDF6  ",
                    ),
                )

            assertEquals(ReminderMutationResult.Success(55L), result)
            assertEquals("x".repeat(199), fakeDao.lastInserted?.message)
            assertEquals(
                ReminderMutationResult.InvalidReminder,
                repository.insert(RowReminder(projectId = 1L, targetRow = 0, message = "Message")),
            )
            assertEquals(
                ReminderMutationResult.InvalidReminder,
                repository.insert(RowReminder(projectId = 1L, targetRow = 1, repeatInterval = 0, message = "Message")),
            )
            assertEquals(
                ReminderMutationResult.InvalidReminder,
                repository.insert(RowReminder(projectId = 1L, targetRow = 1, message = "   ")),
            )
        }

    @Test
    fun `repository enforces creation entitlement but keeps existing reminders editable`() =
        runTest {
            every { proManager.hasFeature(ProFeature.ROW_REMINDERS) } returns false

            assertEquals(
                ReminderMutationResult.FeatureUnavailable,
                repository.insert(RowReminder(projectId = 1L, targetRow = 20, message = "Create")),
            )
            assertEquals(
                ReminderMutationResult.Success(5L),
                repository.update(1L, fakeDao.reminder(5L).toDomain().copy(message = "  Existing  ")),
            )
            assertEquals("Existing", fakeDao.lastUpdated?.message)
            assertEquals(ReminderMutationResult.Success(5L), repository.delete(1L, 5L))
        }

    @Test
    fun `mutations reject reminders owned by another project`() =
        runTest {
            val result = repository.delete(2L, 5L)

            assertEquals(ReminderMutationResult.StaleAction, result)
            assertEquals(null, fakeDao.lastDeletedId)
        }

    private class FakeRowReminderDao(
        reminders: List<RowReminderEntity> = emptyList(),
    ) : RowReminderDao {
        private val stored = reminders.associateBy(RowReminderEntity::id).toMutableMap()
        var lastInserted: RowReminderEntity? = null
        var lastUpdated: RowReminderEntity? = null
        var lastDeletedId: Long? = null

        override fun getRemindersForProject(projectId: Long): Flow<List<RowReminderEntity>> =
            flowOf(stored.values.filter { it.projectId == projectId })

        override suspend fun insert(reminder: RowReminderEntity): Long {
            lastInserted = reminder
            return 55L
        }

        override suspend fun getReminder(id: Long): RowReminderEntity? = stored[id]

        override suspend fun update(reminder: RowReminderEntity): Int {
            if (reminder.id !in stored) return 0
            stored[reminder.id] = reminder
            lastUpdated = reminder
            return 1
        }

        override suspend fun delete(id: Long): Int {
            if (stored.remove(id) == null) return 0
            lastDeletedId = id
            return 1
        }

        override suspend fun deleteAllForProject(projectId: Long) = Unit

        fun reminder(id: Long): RowReminderEntity = checkNotNull(stored[id])
    }
}
