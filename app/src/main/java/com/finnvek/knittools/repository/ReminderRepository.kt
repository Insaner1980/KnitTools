package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.RowReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository
    @Inject
    constructor(
        private val dao: RowReminderDao,
    ) {
        fun getRemindersForProject(projectId: Long): Flow<List<RowReminder>> =
            dao.getRemindersForProject(projectId).map { reminders -> reminders.map { it.toDomain() } }

        suspend fun insert(reminder: RowReminder): Long = dao.insert(reminder.toEntity())

        suspend fun update(reminder: RowReminder) = dao.update(reminder.toEntity())

        suspend fun delete(id: Long) = dao.delete(id)

        suspend fun deleteAllForProject(projectId: Long) = dao.deleteAllForProject(projectId)
    }
