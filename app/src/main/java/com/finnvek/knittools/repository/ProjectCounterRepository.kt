package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.domain.calculator.ProjectCounterLogic
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectCounterRepository
    @Inject
    constructor(
        private val dao: ProjectCounterDao,
    ) {
        fun getCountersForProject(projectId: Long): Flow<List<ProjectCounterEntity>> =
            dao.getCountersForProject(projectId)

        suspend fun addCounter(counter: ProjectCounterEntity): Long =
            dao.insert(counter.copy(name = counter.name.take(50)))

        suspend fun incrementCounter(counter: ProjectCounterEntity) {
            val updated = ProjectCounterLogic.increment(counter)
            dao.updateCount(counter.id, updated.count)
        }

        suspend fun decrementCounter(counter: ProjectCounterEntity) {
            val updated = ProjectCounterLogic.decrement(counter)
            dao.updateCount(counter.id, updated.count)
        }

        suspend fun resetCounter(id: Long) = dao.updateCount(id, 0)

        suspend fun deleteCounter(id: Long) = dao.delete(id)

        suspend fun renameCounter(
            id: Long,
            name: String,
        ) = dao.updateName(id, name.take(50))

        suspend fun updateRepeatSectionState(
            id: Long,
            count: Int,
            currentRepeat: Int?,
        ) = dao.updateRepeatSectionState(id, count, currentRepeat)
    }
