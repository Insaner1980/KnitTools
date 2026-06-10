package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.domain.model.READING_LINE_MAX_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MIN_Y_FRACTION
import com.finnvek.knittools.domain.model.resolvedMainCounterLabelType
import com.finnvek.knittools.domain.model.sanitizeMainCounterCustomLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CounterRepository
    @Inject
    constructor(
        private val dao: CounterProjectDao,
        private val projectCounterDao: ProjectCounterDao,
        private val sessionDao: SessionDao,
        private val photoStorage: ProgressPhotoStorage,
        private val patternDocumentStorage: PatternDocumentStorage,
        @param:ApplicationContext private val context: Context,
        private val yarnCardRepository: YarnCardRepository,
        private val savedPatternRepository: SavedPatternRepository,
        private val patternAnnotationRepository: PatternAnnotationRepository,
        private val transactionRunner: DatabaseTransactionRunner,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun getAllProjects(): Flow<List<CounterProject>> =
            dao.getAllProjects().map { projects ->
                projects.map { it.toDomain() }
            }

        fun getActiveProjects(): Flow<List<CounterProject>> =
            dao.getActiveProjects().map { projects -> projects.map { it.toDomain() } }

        fun getActiveProjects(sortOrder: ProjectSortOrder): Flow<List<CounterProject>> =
            when (sortOrder) {
                ProjectSortOrder.NAME -> dao.getActiveProjectsByName()
                ProjectSortOrder.CREATED -> dao.getActiveProjectsByCreated()
                ProjectSortOrder.UPDATED -> dao.getActiveProjects()
            }.map { projects -> projects.map { it.toDomain() } }

        fun getCompletedProjects(): Flow<List<CounterProject>> =
            dao.getCompletedProjects().map { projects -> projects.map { it.toDomain() } }

        fun getCompletedProjects(sortOrder: ProjectSortOrder): Flow<List<CounterProject>> =
            when (sortOrder) {
                ProjectSortOrder.NAME -> dao.getCompletedProjectsByName()
                ProjectSortOrder.CREATED -> dao.getCompletedProjectsByCreated()
                ProjectSortOrder.UPDATED -> dao.getCompletedProjects()
            }.map { projects -> projects.map { it.toDomain() } }

        suspend fun getActiveProjectCount(): Int = dao.getActiveProjectCount()

        suspend fun getProject(id: Long): CounterProject? = dao.getProject(id)?.toDomain()

        fun observeProject(id: Long): Flow<CounterProject?> = dao.observeProject(id).map { it?.toDomain() }

        suspend fun createProject(
            name: String,
            craftType: CraftType = CraftType.KNITTING,
            mainCounterLabelType: MainCounterLabelType = craftType.defaultMainCounterLabelType(),
            mainCounterCustomLabel: String? = null,
        ): Long? {
            val projectName = uniqueProjectName(name) ?: return null
            val labelType =
                validatedMainCounterLabelType(
                    craftType = craftType,
                    labelType = mainCounterLabelType,
                    customLabel = mainCounterCustomLabel,
                ) ?: return null
            val now = System.currentTimeMillis()
            return dao.insert(
                CounterProject(
                    name = projectName,
                    craftType = craftType,
                    mainCounterLabelType = labelType,
                    mainCounterCustomLabel = sanitizeMainCounterCustomLabel(mainCounterCustomLabel),
                    createdAt = now,
                    updatedAt = now,
                ).toEntity(),
            )
        }

        suspend fun updateProject(project: CounterProject) {
            val projectName = uniqueProjectName(project.name, excludedProjectId = project.id) ?: return
            val normalized = normalizeProjectDetails(project.copy(name = projectName)) ?: return
            dao.update(normalized.copy(updatedAt = System.currentTimeMillis()).toEntity())
        }

        suspend fun updateProjectDetails(
            id: Long,
            name: String,
            craftType: CraftType,
            mainCounterLabelType: MainCounterLabelType,
            mainCounterCustomLabel: String?,
        ): CounterProject? {
            val projectName = uniqueProjectName(name, excludedProjectId = id) ?: return null
            val labelType =
                validatedMainCounterLabelType(
                    craftType = craftType,
                    labelType = mainCounterLabelType,
                    customLabel = mainCounterCustomLabel,
                ) ?: return null
            val customLabel = sanitizeMainCounterCustomLabel(mainCounterCustomLabel)
            val updatedAt = System.currentTimeMillis()
            dao.updateProjectDetails(
                id = id,
                name = projectName,
                craftType = craftType.persistedValue,
                mainCounterLabelType = labelType.persistedValue,
                mainCounterCustomLabel = customLabel.takeIf { labelType == MainCounterLabelType.CUSTOM },
                updatedAt = updatedAt,
            )
            return dao.getProject(id)?.toDomain()
        }

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

        suspend fun applyMainCounterChange(
            id: Long,
            change: MainCounterChange,
        ): Boolean =
            transactionRunner.run {
                val project = dao.getProject(id)?.toDomain() ?: return@run false
                if (project.isCompleted) return@run false

                when (change) {
                    MainCounterChange.Undo -> undoMainCounterChange(id, project)
                    else -> applyHistoryTrackedMainCounterChange(id, project, change)
                }
            }

        suspend fun applyWidgetCountChange(
            id: Long,
            increment: Boolean,
        ): Boolean =
            applyMainCounterChange(
                id = id,
                change = if (increment) MainCounterChange.Increment else MainCounterChange.Decrement,
            )

        suspend fun updateProjectName(
            id: Long,
            name: String,
        ): String? {
            val projectName = uniqueProjectName(name, excludedProjectId = id) ?: return null
            dao.updateName(id, projectName, System.currentTimeMillis())
            return projectName
        }

        suspend fun updateProjectNotes(
            id: Long,
            notes: String,
        ) = dao.updateNotes(id, notes, System.currentTimeMillis())

        suspend fun saveProjectNotes(
            id: Long,
            baseNotes: String,
            requestedNotes: String,
        ): CounterProject? =
            transactionRunner.run {
                val current = dao.getProject(id)?.toDomain() ?: return@run null
                val notesToSave =
                    mergeProjectNotes(
                        baseNotes = baseNotes,
                        requestedNotes = requestedNotes,
                        currentNotes = current.notes,
                    )
                val updatedAt = System.currentTimeMillis()
                dao.updateNotes(id, notesToSave, updatedAt)
                current.copy(notes = notesToSave, updatedAt = updatedAt)
            }

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

        suspend fun attachPattern(
            id: Long,
            patternUri: String,
            patternName: String,
            currentPatternPage: Int,
            patternRowMapping: String?,
        ) {
            val previousPatternUri = dao.getProject(id)?.patternUri
            transactionRunner.run {
                savedPatternRepository.saveImportedPatternIfMissing(patternUri, patternName)
                patternAnnotationRepository.clearProject(id)
                updatePattern(
                    id = id,
                    patternUri = patternUri,
                    patternName = patternName,
                    currentPatternPage = currentPatternPage,
                    patternRowMapping = patternRowMapping,
                )
            }
            previousPatternUri
                ?.takeIf { it != patternUri }
                ?.let { savedPatternRepository.deleteLocalPatternFileIfUnused(it) }
        }

        suspend fun detachPattern(id: Long) {
            val patternUri = dao.getProject(id)?.patternUri
            transactionRunner.run {
                patternAnnotationRepository.clearProject(id)
                updatePattern(
                    id = id,
                    patternUri = null,
                    patternName = null,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            }
            patternUri?.let { savedPatternRepository.deleteLocalPatternFileIfUnused(it) }
        }

        suspend fun updateCurrentPatternPage(
            id: Long,
            page: Int,
        ) = dao.updateCurrentPatternPage(id, page, System.currentTimeMillis())

        suspend fun updatePatternRowMapping(
            id: Long,
            mapping: String?,
        ) = dao.updatePatternRowMapping(id, mapping, System.currentTimeMillis())

        suspend fun updateReadingLine(
            id: Long,
            enabled: Boolean,
            yFraction: Float,
        ) {
            val sanitizedYFraction =
                yFraction.coerceIn(READING_LINE_MIN_Y_FRACTION, READING_LINE_MAX_Y_FRACTION)
            dao.updateReadingLine(id, enabled, sanitizedYFraction, System.currentTimeMillis())
        }

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
            withContext(ioDispatcher) {
                photoStorage.deleteProjectPhotos(context, id)
                patternDocumentStorage.deleteProjectCaptureImages(context, id)
            }
            val patternUri = dao.getProject(id)?.patternUri
            transactionRunner.run {
                yarnCardRepository.clearLinkedProject(id)
                dao.delete(id) // CASCADE poistaa liittyvät rivit muista tauluista
            }
            patternUri?.let { savedPatternRepository.deleteLocalPatternFileIfUnused(it) }
        }

        suspend fun getProjectCount(): Int = dao.getProjectCount()

        suspend fun getLatestActiveProject(): CounterProject? = dao.getLatestActiveProject()?.toDomain()

        private suspend fun uniqueProjectName(
            name: String,
            excludedProjectId: Long? = null,
        ): String? {
            val existingNames =
                dao
                    .getAllProjectsOnce()
                    .asSequence()
                    .filter { project -> excludedProjectId == null || project.id != excludedProjectId }
                    .map { it.name }
                    .toList()
            return ProjectNameRules.uniqueName(name, existingNames)
        }

        private fun normalizeProjectDetails(project: CounterProject): CounterProject? {
            val labelType =
                validatedMainCounterLabelType(
                    craftType = project.craftType,
                    labelType = project.mainCounterLabelType,
                    customLabel = project.mainCounterCustomLabel,
                ) ?: return null
            return project.copy(
                mainCounterLabelType = labelType,
                mainCounterCustomLabel =
                    sanitizeMainCounterCustomLabel(project.mainCounterCustomLabel)
                        .takeIf { labelType == MainCounterLabelType.CUSTOM },
            )
        }

        private fun validatedMainCounterLabelType(
            craftType: CraftType,
            labelType: MainCounterLabelType,
            customLabel: String?,
        ): MainCounterLabelType? =
            if (labelType == MainCounterLabelType.CUSTOM && sanitizeMainCounterCustomLabel(customLabel) == null) {
                null
            } else {
                resolvedMainCounterLabelType(craftType, labelType, customLabel)
            }

        private suspend fun applyHistoryTrackedMainCounterChange(
            id: Long,
            project: CounterProject,
            change: MainCounterChange,
        ): Boolean {
            val before = CounterState(count = project.count, stepSize = project.stepSize)
            val after = change.applyTo(before)
            if (after.count == before.count) return false

            val updatedAt = System.currentTimeMillis()
            dao.updateCounterStateWithHistory(
                projectId = id,
                count = after.count,
                stepSize = after.stepSize,
                action = change.historyAction,
                previousValue = before.count,
                newValue = after.count,
                updatedAt = updatedAt,
            )
            resetCurrentStitchIfNeeded(id, project, updatedAt)
            applyLinkedCounterDelta(id, after.count - before.count)
            return true
        }

        private suspend fun undoMainCounterChange(
            id: Long,
            project: CounterProject,
        ): Boolean {
            val history = dao.getLatestHistory(id) ?: return false
            val updatedAt = System.currentTimeMillis()
            dao.updateCount(id, history.previousValue, updatedAt)
            dao.deleteHistoryById(history.id)
            resetCurrentStitchIfNeeded(id, project, updatedAt)
            applyLinkedCounterDelta(id, history.previousValue - history.newValue)
            return true
        }

        private suspend fun resetCurrentStitchIfNeeded(
            id: Long,
            project: CounterProject,
            updatedAt: Long,
        ) {
            if (project.stitchTrackingEnabled) {
                dao.updateCurrentStitch(id, 0, updatedAt)
            }
        }

        private fun MainCounterChange.applyTo(before: CounterState): CounterState =
            when (this) {
                MainCounterChange.Increment -> CounterLogic.increment(before)
                MainCounterChange.Decrement -> CounterLogic.decrement(before)
                MainCounterChange.Reset -> CounterState(count = 0, stepSize = before.stepSize)
                MainCounterChange.Undo -> before
            }

        private val MainCounterChange.historyAction: String
            get() =
                when (this) {
                    MainCounterChange.Increment -> "increment"
                    MainCounterChange.Decrement -> "decrement"
                    MainCounterChange.Reset -> "reset"
                    MainCounterChange.Undo -> "undo"
                }

        private suspend fun applyLinkedCounterDelta(
            projectId: Long,
            delta: Int,
        ) {
            if (delta == 0) return
            projectCounterDao
                .getCountersForProject(projectId)
                .first()
                .filter { it.linkedToMainCounter }
                .forEach { counter ->
                    val updatedCount = (counter.count + delta).coerceAtLeast(0)
                    if (updatedCount != counter.count) {
                        projectCounterDao.updateCount(counter.id, updatedCount)
                    }
                }
        }

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
            sessionDao.getSessionsForProject(projectId).toDomainSessions()

        fun getAllSessions(projectId: Long?): Flow<List<KnitSession>> =
            sessionDao.getAllSessions(projectId).toDomainSessions()

        fun getCompletedProjectCount(): Flow<Int> = sessionDao.getCompletedProjectCount()

        fun getSessionsForInsights(
            projectId: Long?,
            start: Long?,
        ): Flow<List<KnitSession>> {
            val sessions =
                when {
                    projectId == null && start == null -> sessionDao.getAllSessionsForInsights()
                    projectId == null && start != null -> sessionDao.getAllSessionsForInsightsSince(start)
                    projectId != null && start == null -> sessionDao.getProjectSessionsForInsights(projectId)
                    projectId != null && start != null ->
                        sessionDao.getProjectSessionsForInsightsSince(
                            projectId = projectId,
                            start = start,
                        )
                    else -> sessionDao.getAllSessionsForInsights()
                }
            return sessions.toDomainSessions()
        }

        suspend fun insertSession(session: KnitSession): Long = sessionDao.insert(session.toEntity())

        suspend fun deleteSession(id: Long) = sessionDao.deleteById(id)

        suspend fun deleteSessionsBefore(
            projectId: Long,
            before: Long,
        ) = sessionDao.deleteSessionsBefore(projectId, before)

        suspend fun getTotalMinutesForProject(projectId: Long): Int = sessionDao.getTotalMinutes(projectId)

        suspend fun getLatestSession(projectId: Long): KnitSession? = sessionDao.getLatestSession(projectId)?.toDomain()

        private fun Flow<List<SessionEntity>>.toDomainSessions(): Flow<List<KnitSession>> =
            distinctUntilChanged()
                .map { sessions -> sessions.map { it.toDomain() } }
                .flowOn(ioDispatcher)
    }

internal fun mergeProjectNotes(
    baseNotes: String,
    requestedNotes: String,
    currentNotes: String,
): String {
    if (currentNotes == baseNotes || currentNotes == requestedNotes) return requestedNotes
    if (requestedNotes == baseNotes) return currentNotes
    if (baseNotes.isEmpty()) return combineNoteBlocks(requestedNotes, currentNotes)

    val requestedSuffix = requestedNotes.removeKnownPrefix(baseNotes)
    val currentSuffix = currentNotes.removeKnownPrefix(baseNotes)

    return when {
        requestedSuffix != null && currentSuffix != null -> currentNotes + requestedSuffix
        requestedSuffix != null -> currentNotes + requestedSuffix
        currentSuffix != null -> requestedNotes + currentSuffix
        else -> combineNoteBlocks(requestedNotes, currentNotes)
    }
}

private fun String.removeKnownPrefix(prefix: String): String? =
    if (startsWith(prefix)) {
        removePrefix(prefix)
    } else {
        null
    }

private fun combineNoteBlocks(
    primary: String,
    secondary: String,
): String =
    when {
        primary.isBlank() -> secondary
        secondary.isBlank() -> primary
        else -> "${primary.trimEnd()}\n\n---\n\n${secondary.trimStart()}"
    }
