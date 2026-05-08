package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.SessionInsightsTotals
import com.finnvek.knittools.data.local.SessionProjectSummary
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CounterRepository
    @Inject
    constructor(
        private val dao: CounterProjectDao,
        private val sessionDao: SessionDao,
        private val photoStorage: ProgressPhotoStorage,
        @param:ApplicationContext private val context: Context,
    ) {
        fun getAllProjects(): Flow<List<CounterProjectEntity>> = dao.getAllProjects()

        fun getActiveProjects(): Flow<List<CounterProjectEntity>> = dao.getActiveProjects()

        fun getActiveProjects(sortOrder: String): Flow<List<CounterProjectEntity>> =
            when (sortOrder) {
                "name" -> dao.getActiveProjectsByName()
                "created" -> dao.getActiveProjectsByCreated()
                else -> dao.getActiveProjects()
            }

        fun getCompletedProjects(): Flow<List<CounterProjectEntity>> = dao.getCompletedProjects()

        fun getCompletedProjects(sortOrder: String): Flow<List<CounterProjectEntity>> =
            when (sortOrder) {
                "name" -> dao.getCompletedProjectsByName()
                "created" -> dao.getCompletedProjectsByCreated()
                else -> dao.getCompletedProjects()
            }

        suspend fun getActiveProjectCount(): Int = dao.getActiveProjectCount()

        suspend fun getProject(id: Long): CounterProjectEntity? = dao.getProject(id)

        fun observeProject(id: Long): Flow<CounterProjectEntity?> = dao.observeProject(id)

        suspend fun createProject(name: String): Long = dao.insert(CounterProjectEntity(name = name))

        suspend fun updateProject(project: CounterProjectEntity) =
            dao.update(project.copy(updatedAt = System.currentTimeMillis()))

        suspend fun adjustProjectCount(
            id: Long,
            delta: Int,
        ) = dao.adjustCount(id, delta, System.currentTimeMillis())

        suspend fun adjustProjectCountWithHistory(
            id: Long,
            delta: Int,
            stepSize: Int,
            action: String,
            previousValue: Int,
            newValue: Int,
        ) = dao.adjustCountWithHistory(
            projectId = id,
            delta = delta,
            stepSize = stepSize,
            action = action,
            previousValue = previousValue,
            newValue = newValue,
            updatedAt = System.currentTimeMillis(),
        )

        suspend fun updateProjectCounterStateWithHistory(
            id: Long,
            count: Int,
            stepSize: Int,
            action: String,
            previousValue: Int,
            newValue: Int,
        ) = dao.updateCounterStateWithHistory(
            projectId = id,
            count = count,
            stepSize = stepSize,
            action = action,
            previousValue = previousValue,
            newValue = newValue,
            updatedAt = System.currentTimeMillis(),
        )

        suspend fun updateProjectName(
            id: Long,
            name: String,
        ) = dao.updateName(id, name, System.currentTimeMillis())

        suspend fun updateProjectNotes(
            id: Long,
            notes: String,
        ) = dao.updateNotes(id, notes, System.currentTimeMillis())

        suspend fun updateProjectSecondaryCount(
            id: Long,
            secondaryCount: Int,
        ) = dao.updateSecondaryCount(id, secondaryCount, System.currentTimeMillis())

        suspend fun updateProjectSectionName(
            id: Long,
            sectionName: String?,
        ) = dao.updateSectionName(id, sectionName, System.currentTimeMillis())

        suspend fun updateProjectStitchCount(
            id: Long,
            stitchCount: Int?,
        ) = dao.updateStitchCount(id, stitchCount, System.currentTimeMillis())

        suspend fun updateCurrentStitch(
            id: Long,
            stitch: Int,
        ) = dao.updateCurrentStitch(id, stitch, System.currentTimeMillis())

        suspend fun updateStitchTrackingEnabled(
            id: Long,
            enabled: Boolean,
        ) = dao.updateStitchTrackingEnabled(id, enabled, System.currentTimeMillis())

        suspend fun updatePattern(
            id: Long,
            patternUri: String?,
            patternName: String?,
            currentPatternPage: Int,
            patternRowMapping: String?,
        ) = dao.updatePattern(
            id = id,
            patternUri = patternUri,
            patternName = patternName,
            currentPatternPage = currentPatternPage,
            patternRowMapping = patternRowMapping,
            updatedAt = System.currentTimeMillis(),
        )

        suspend fun updateCurrentPatternPage(
            id: Long,
            page: Int,
        ) = dao.updateCurrentPatternPage(id, page, System.currentTimeMillis())

        suspend fun updatePatternRowMapping(
            id: Long,
            mapping: String?,
        ) = dao.updatePatternRowMapping(id, mapping, System.currentTimeMillis())

        suspend fun updateProjectStepSize(
            id: Long,
            stepSize: Int,
        ) = dao.updateStepSize(id, stepSize, System.currentTimeMillis())

        suspend fun updateProjectYarnCardIds(
            id: Long,
            yarnCardIds: String,
        ) = dao.updateYarnCardIds(id, yarnCardIds, System.currentTimeMillis())

        suspend fun archiveProject(
            id: Long,
            totalRows: Int,
            completedAt: Long,
        ) = dao.archiveProject(id, totalRows, completedAt, System.currentTimeMillis())

        suspend fun reactivateProject(id: Long) = dao.reactivateProject(id, System.currentTimeMillis())

        suspend fun deleteProject(id: Long) {
            photoStorage.deleteProjectPhotos(context, id)
            dao.delete(id) // CASCADE poistaa liittyvät rivit muista tauluista
        }

        suspend fun getProjectCount(): Int = dao.getProjectCount()

        suspend fun getFirstProject(): CounterProjectEntity? = dao.getFirstProject()

        suspend fun deleteHistoryBefore(
            projectId: Long,
            before: Long,
        ) = dao.deleteHistoryBefore(projectId, before)

        suspend fun undoLastChange(projectId: Long) = dao.undoLastChange(projectId, System.currentTimeMillis())

        suspend fun setTargetRows(
            projectId: Long,
            targetRows: Int?,
        ) = dao.updateTargetRows(projectId, targetRows, System.currentTimeMillis())

        // Session-metodit
        fun getSessionsForProject(projectId: Long): Flow<List<SessionEntity>> =
            sessionDao.getSessionsForProject(projectId)

        fun getAllSessions(projectId: Long?): Flow<List<SessionEntity>> = sessionDao.getAllSessions(projectId)

        fun getCompletedProjectCount(): Flow<Int> = sessionDao.getCompletedProjectCount()

        fun getTotalDurationMinutes(projectId: Long?): Flow<Int> = sessionDao.getTotalDurationMinutes(projectId)

        fun getInsightsTotals(
            projectId: Long?,
            start: Long?,
        ): Flow<SessionInsightsTotals> = sessionDao.getInsightsTotals(projectId, start)

        fun getSessionsForInsights(
            projectId: Long?,
            start: Long?,
        ): Flow<List<SessionEntity>> = sessionDao.getSessionsForInsights(projectId, start)

        fun getProjectTimeSummaries(
            projectId: Long?,
            start: Long?,
        ): Flow<List<SessionProjectSummary>> = sessionDao.getProjectTimeSummaries(projectId, start)

        suspend fun insertSession(session: SessionEntity): Long = sessionDao.insert(session)

        suspend fun deleteSessionsBefore(
            projectId: Long,
            before: Long,
        ) = sessionDao.deleteSessionsBefore(projectId, before)

        suspend fun getTotalMinutesForProject(projectId: Long): Int = sessionDao.getTotalMinutes(projectId)

        suspend fun getLatestSession(projectId: Long): SessionEntity? = sessionDao.getLatestSession(projectId)
    }
