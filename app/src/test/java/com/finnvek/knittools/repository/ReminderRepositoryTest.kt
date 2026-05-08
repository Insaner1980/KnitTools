package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.RowReminderEntity
import com.finnvek.knittools.domain.model.RowReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReminderRepositoryTest {
    private lateinit var fakeDao: FakeRowReminderDao
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
        repository = ReminderRepository(fakeDao)
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

            assertEquals(55L, id)
            assertEquals(RowReminderEntity(projectId = 1L, targetRow = 20, message = "Bind off"), fakeDao.lastInserted)
        }

    @Test
    fun `update accepts domain reminder and writes entity`() =
        runTest {
            repository.update(
                RowReminder(
                    id = 5L,
                    projectId = 1L,
                    targetRow = 12,
                    repeatInterval = null,
                    message = "Increase",
                    isCompleted = true,
                ),
            )

            assertEquals(true, fakeDao.lastUpdated?.isCompleted)
            assertEquals(5L, fakeDao.lastUpdated?.id)
        }

    @Test
    fun `delete calls dao delete`() =
        runTest {
            repository.delete(5L)

            assertEquals(5L, fakeDao.lastDeletedId)
        }

    private class FakeRowReminderDao(
        private val reminders: List<RowReminderEntity> = emptyList(),
    ) : RowReminderDao {
        var lastInserted: RowReminderEntity? = null
        var lastUpdated: RowReminderEntity? = null
        var lastDeletedId: Long? = null

        override fun getRemindersForProject(projectId: Long): Flow<List<RowReminderEntity>> =
            flowOf(reminders.filter { it.projectId == projectId })

        override suspend fun insert(reminder: RowReminderEntity): Long {
            lastInserted = reminder
            return 55L
        }

        override suspend fun update(reminder: RowReminderEntity) {
            lastUpdated = reminder
        }

        override suspend fun delete(id: Long) {
            lastDeletedId = id
        }

        override suspend fun deleteAllForProject(projectId: Long) = Unit
    }
}
