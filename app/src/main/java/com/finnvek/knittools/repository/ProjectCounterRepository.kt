package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.ProjectCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectCounterRepository
    @Inject
    constructor(
        private val dao: ProjectCounterDao,
    ) {
        fun getCountersForProject(projectId: Long): Flow<List<ProjectCounter>> =
            dao.getCountersForProject(projectId).map { counters -> counters.map { it.toDomain() } }

        suspend fun addCounter(counter: ProjectCounter): Long =
            dao.insert(counter.copy(name = counter.name.take(50)).toEntity())

        suspend fun incrementCounter(counter: ProjectCounter) = dao.incrementCount(counter.id)

        suspend fun decrementCounter(counter: ProjectCounter) = dao.decrementCount(counter.id)

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
