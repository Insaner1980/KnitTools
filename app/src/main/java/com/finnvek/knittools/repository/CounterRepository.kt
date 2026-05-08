package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.SessionInsightsSummary
import com.finnvek.knittools.domain.model.SessionProjectTimeSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        fun getAllProjects(): Flow<List<CounterProject>> = dao.getAllProjects().map { projects -> projects.map { it.toDomain() } }

        fun getActiveProjects(): Flow<List<CounterProject>> =
            dao.getActiveProjects().map { projects -> projects.map { it.toDomain() } }

        fun getActiveProjects(sortOrder: String): Flow<List<CounterProject>> =
            when (sortOrder) {
                "name" -> dao.getActiveProjectsByName()
                "created" -> dao.getActiveProjectsByCreated()
                else -> dao.getActiveProjects()
            }.map { projects -> projects.map { it.toDomain() } }

        fun getCompletedProjects(): Flow<List<CounterProject>> =
            dao.getCompletedProjects().map { projects -> projects.map { it.toDomain() } }

        fun getCompletedProjects(sortOrder: String): Flow<List<CounterProject>> =
            when (sortOrder) {
                "name" -> dao.getCompletedProjectsByName()
                "created" -> dao.getCompletedProjectsByCreated()
                else -> dao.getCompletedProjects()
            }.map { projects -> projects.map { it.toDomain() } }

        suspend fun getActiveProjectCount(): Int = dao.getActiveProjectCount()

        suspend fun getProject(id: Long): CounterProject? = dao.getProject(id)?.toDomain()

        fun observeProject(id: Long): Flow<CounterProject?> = dao.observeProject(id).map { it?.toDomain() }

        suspend fun createProject(name: String): Long = dao.insert(CounterProject(name = name).toEntity())

        suspend fun updateProject(project: CounterProject) =
            dao.update(project.copy(updatedAt = System.currentTimeMillis()).toEntity())

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

        suspend fun getFirstProject(): CounterProject? = dao.getFirstProject()?.toDomain()

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
        fun getSessionsForProject(projectId: Long): Flow<List<KnitSession>> =
            sessionDao.getSessionsForProject(projectId).map { sessions -> sessions.map { it.toDomain() } }

        fun getAllSessions(projectId: Long?): Flow<List<KnitSession>> =
            sessionDao.getAllSessions(projectId).map { sessions -> sessions.map { it.toDomain() } }

        fun getCompletedProjectCount(): Flow<Int> = sessionDao.getCompletedProjectCount()

        fun getTotalDurationMinutes(projectId: Long?): Flow<Int> = sessionDao.getTotalDurationMinutes(projectId)

        fun getInsightsTotals(
            projectId: Long?,
            start: Long?,
        ): Flow<SessionInsightsSummary> =
            sessionDao.getInsightsTotals(projectId, start).map { totals ->
                SessionInsightsSummary(
                    totalMinutes = totals.totalMinutes,
                    totalRows = totals.totalRows,
                    sessionCount = totals.sessionCount,
                )
            }

        fun getSessionsForInsights(
            projectId: Long?,
            start: Long?,
        ): Flow<List<KnitSession>> =
            sessionDao.getSessionsForInsights(projectId, start).map { sessions -> sessions.map { it.toDomain() } }

        fun getProjectTimeSummaries(
            projectId: Long?,
            start: Long?,
        ): Flow<List<SessionProjectTimeSummary>> =
            sessionDao.getProjectTimeSummaries(projectId, start).map { summaries ->
                summaries.map {
                    SessionProjectTimeSummary(
                        projectId = it.projectId,
                        projectName = it.projectName,
                        totalMinutes = it.totalMinutes,
                        totalRows = it.totalRows,
                        lastSessionAt = it.lastSessionAt,
                    )
                }
            }

        suspend fun insertSession(session: KnitSession): Long = sessionDao.insert(session.toEntity())

        suspend fun deleteSessionsBefore(
            projectId: Long,
            before: Long,
        ) = sessionDao.deleteSessionsBefore(projectId, before)

        suspend fun getTotalMinutesForProject(projectId: Long): Int = sessionDao.getTotalMinutes(projectId)

        suspend fun getLatestSession(projectId: Long): KnitSession? = sessionDao.getLatestSession(projectId)?.toDomain()
    }
