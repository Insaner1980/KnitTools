package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.RowReminderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository
    @Inject
    constructor(
        private val dao: RowReminderDao,
    ) {
        fun getRemindersForProject(projectId: Long): Flow<List<RowReminderEntity>> =
            dao.getRemindersForProject(projectId)

        suspend fun insert(reminder: RowReminderEntity): Long = dao.insert(reminder)

        suspend fun update(reminder: RowReminderEntity) = dao.update(reminder)

        suspend fun delete(id: Long) = dao.delete(id)

        suspend fun deleteAllForProject(projectId: Long) = dao.deleteAllForProject(projectId)
    }
