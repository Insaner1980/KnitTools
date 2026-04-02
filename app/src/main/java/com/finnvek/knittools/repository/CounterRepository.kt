package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CounterRepository
    @Inject
    constructor(
        private val dao: CounterProjectDao,
    ) {
        fun getAllProjects(): Flow<List<CounterProjectEntity>> = dao.getAllProjects()

        suspend fun getProject(id: Long): CounterProjectEntity? = dao.getProject(id)

        suspend fun createProject(name: String): Long = dao.insert(CounterProjectEntity(name = name))

        suspend fun updateProject(project: CounterProjectEntity) =
            dao.update(project.copy(updatedAt = System.currentTimeMillis()))

        suspend fun deleteProject(id: Long) = dao.delete(id)

        suspend fun getProjectCount(): Int = dao.getProjectCount()

        suspend fun getFirstProject(): CounterProjectEntity? = dao.getFirstProject()

        suspend fun addHistoryEntry(
            projectId: Long,
            action: String,
            previousValue: Int,
            newValue: Int,
        ) {
            dao.insertHistory(
                CounterHistoryEntity(
                    projectId = projectId,
                    action = action,
                    previousValue = previousValue,
                    newValue = newValue,
                ),
            )
        }

        fun getHistory(projectId: Long): Flow<List<CounterHistoryEntity>> = dao.getHistory(projectId)

        suspend fun deleteHistoryBefore(
            projectId: Long,
            before: Long,
        ) = dao.deleteHistoryBefore(projectId, before)
    }
