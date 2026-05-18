package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

open class StubCounterProjectDao(
    private val projects: List<CounterProjectEntity> = emptyList(),
) : CounterProjectDao {
    override fun getAllProjects(): Flow<List<CounterProjectEntity>> = flowOf(projects)

    override suspend fun getAllProjectsOnce(): List<CounterProjectEntity> = projects

    override suspend fun getProject(id: Long): CounterProjectEntity? = getProjectSync(id)

    override fun observeProject(id: Long): Flow<CounterProjectEntity?> = flowOf(getProjectSync(id))

    override suspend fun insert(project: CounterProjectEntity): Long = 0L

    override suspend fun update(project: CounterProjectEntity) = Unit

    override suspend fun adjustCount(
        id: Long,
        delta: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun adjustCountAndStepSize(
        id: Long,
        delta: Int,
        stepSize: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateCounterState(
        id: Long,
        count: Int,
        stepSize: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateName(
        id: Long,
        name: String,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateNotes(
        id: Long,
        notes: String,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateSecondaryCount(
        id: Long,
        secondaryCount: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateSectionName(
        id: Long,
        sectionName: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateStitchCount(
        id: Long,
        stitchCount: Int?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateCurrentStitch(
        id: Long,
        stitch: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateStitchTrackingEnabled(
        id: Long,
        enabled: Boolean,
        updatedAt: Long,
    ) = Unit

    override suspend fun updatePattern(
        id: Long,
        patternUri: String?,
        patternName: String?,
        currentPatternPage: Int,
        patternRowMapping: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateCurrentPatternPage(
        id: Long,
        page: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updatePatternRowMapping(
        id: Long,
        mapping: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateStepSize(
        id: Long,
        stepSize: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateYarnCardIds(
        id: Long,
        yarnCardIds: String,
        updatedAt: Long,
    ) = Unit

    override suspend fun clearLinkedPatternIds(
        patternIds: List<Long>,
        updatedAt: Long,
    ) = Unit

    override suspend fun countProjectsUsingPatternUri(patternUri: String): Int =
        projects.count { it.patternUri == patternUri }

    override suspend fun archiveProject(
        id: Long,
        totalRows: Int,
        completedAt: Long,
        updatedAt: Long,
    ) = Unit

    override suspend fun reactivateProject(
        id: Long,
        updatedAt: Long,
    ) = Unit

    override suspend fun delete(id: Long) = Unit

    override suspend fun getProjectCount(): Int = projects.size

    override suspend fun getLatestActiveProject(): CounterProjectEntity? = projects.firstOrNull()

    override suspend fun insertHistory(entry: CounterHistoryEntity) = Unit

    override suspend fun deleteHistoryBefore(
        projectId: Long,
        before: Long,
    ) = Unit

    override suspend fun getLatestHistory(projectId: Long): CounterHistoryEntity? = null

    override suspend fun deleteHistoryById(id: Long) = Unit

    override suspend fun updateCount(
        id: Long,
        count: Int,
        updatedAt: Long,
    ) = Unit

    override fun getActiveProjects(): Flow<List<CounterProjectEntity>> = flowOf(projects)

    override fun getActiveProjectsByName(): Flow<List<CounterProjectEntity>> = flowOf(projects)

    override fun getActiveProjectsByCreated(): Flow<List<CounterProjectEntity>> = flowOf(projects)

    override fun getCompletedProjects(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())

    override fun getCompletedProjectsByName(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())

    override fun getCompletedProjectsByCreated(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())

    override suspend fun getActiveProjectCount(): Int = projects.size

    override suspend fun updateTargetRows(
        id: Long,
        targetRows: Int?,
        updatedAt: Long,
    ) = Unit

    private fun getProjectSync(id: Long): CounterProjectEntity? = projects.firstOrNull { it.id == id }
}
