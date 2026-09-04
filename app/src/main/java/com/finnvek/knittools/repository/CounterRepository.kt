package com.finnvek.knittools.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.finnvek.knittools.data.local.ActiveSessionEntity
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectFolderAssignmentEntity
import com.finnvek.knittools.data.local.ProjectFolderDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.data.time.UnavailableBootSessionTimeSource
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.ProjectCounterLogic
import com.finnvek.knittools.domain.calculator.ReadingLineLocationResolution
import com.finnvek.knittools.domain.calculator.evaluateActiveSessionTime
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.resolveReadingLineLocation
import com.finnvek.knittools.domain.calculator.saturatingAdd
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveSessionTimeEvaluation
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.resolvedMainCounterLabelType
import com.finnvek.knittools.domain.model.sanitizeMainCounterCustomLabel
import com.finnvek.knittools.domain.model.sanitizeReadingGuideFraction
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ProjectCreationResult {
    data class Created(
        val projectId: Long,
    ) : ProjectCreationResult

    data object LimitReached : ProjectCreationResult

    data object InvalidProject : ProjectCreationResult

    data object FolderMissing : ProjectCreationResult
}

sealed interface ProjectNotesSaveResult {
    data class Saved(
        val project: CounterProject,
    ) : ProjectNotesSaveResult

    data object ProjectUnavailable : ProjectNotesSaveResult

    data object FeatureUnavailable : ProjectNotesSaveResult

    data object PersistenceFailure : ProjectNotesSaveResult
}

@Singleton
@Suppress("LargeClass") // Pää- ja widget-laskurin atomiset invariantit kuuluvat samaan repository-rajaan.
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
        private val projectDocumentRepository: ProjectDocumentRepository,
        private val projectFolderDao: ProjectFolderDao,
        private val transactionRunner: DatabaseTransactionRunner,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val sessionTimeSource: SessionTimeSource = UnavailableBootSessionTimeSource,
        private val proManager: ProManager? = null,
    ) {
        fun getAllProjects(): Flow<List<CounterProject>> =
            dao
                .getAllProjects()
                .map { projects -> projects.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        fun getActiveProjects(): Flow<List<CounterProject>> =
            dao
                .getActiveProjects()
                .map { projects -> projects.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        fun getActiveProjects(sortOrder: ProjectSortOrder): Flow<List<CounterProject>> =
            when (sortOrder) {
                ProjectSortOrder.NAME -> dao.getActiveProjectsByName()
                ProjectSortOrder.CREATED -> dao.getActiveProjectsByCreated()
                ProjectSortOrder.UPDATED -> dao.getActiveProjects()
            }.map { projects -> projects.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        fun getCompletedProjects(): Flow<List<CounterProject>> =
            dao
                .getCompletedProjects()
                .map { projects -> projects.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        fun getCompletedProjects(sortOrder: ProjectSortOrder): Flow<List<CounterProject>> =
            when (sortOrder) {
                ProjectSortOrder.NAME -> dao.getCompletedProjectsByName()
                ProjectSortOrder.CREATED -> dao.getCompletedProjectsByCreated()
                ProjectSortOrder.UPDATED -> dao.getCompletedProjects()
            }.map { projects -> projects.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        suspend fun getActiveProjectCount(): Int = dao.getActiveProjectCount()

        suspend fun getProject(id: Long): CounterProject? = dao.getProject(id)?.toDomain()

        suspend fun isPatternDocumentAttached(
            projectId: Long,
            localPdfUri: String,
        ): Boolean = projectDocumentRepository.getDocuments(projectId).any { it.localPdfUri == localPdfUri }

        fun observeProject(id: Long): Flow<CounterProject?> =
            dao
                .observeProject(id)
                .map { it?.toDomain() }
                .retryOnRepositoryReadFailure()

        suspend fun createProject(
            name: String,
            craftType: CraftType = CraftType.KNITTING,
            mainCounterLabelType: MainCounterLabelType = craftType.defaultMainCounterLabelType(),
            mainCounterCustomLabel: String? = null,
            canCreateAdditionalProjects: Boolean,
            linkedPattern: SavedPattern? = null,
            targetFolderId: Long? = null,
        ): ProjectCreationResult =
            try {
                transactionRunner.run {
                    val canCreateAdditionalProjectsNow = hasAdditionalProjectAccess(canCreateAdditionalProjects)
                    if (!canCreateAdditionalProjectsNow && dao.getProjectCount() >= 1) {
                        return@run ProjectCreationResult.LimitReached
                    }
                    val projectName = uniqueProjectName(name) ?: return@run ProjectCreationResult.InvalidProject
                    val labelType =
                        validatedMainCounterLabelType(
                            craftType = craftType,
                            labelType = mainCounterLabelType,
                            customLabel = mainCounterCustomLabel,
                        ) ?: return@run ProjectCreationResult.InvalidProject
                    if (targetFolderId != null && projectFolderDao.getById(targetFolderId) == null) {
                        return@run ProjectCreationResult.FolderMissing
                    }
                    val linkedPatternId =
                        linkedPattern?.let { pattern ->
                            savedPatternRepository.saveRavelryPatternIfMissingInCurrentTransaction(pattern)
                        }
                    val now = System.currentTimeMillis()
                    val projectId =
                        dao.insert(
                            CounterProject(
                                name = projectName,
                                craftType = craftType,
                                mainCounterLabelType = labelType,
                                mainCounterCustomLabel = sanitizeMainCounterCustomLabel(mainCounterCustomLabel),
                                createdAt = now,
                                updatedAt = now,
                                linkedPatternId = linkedPatternId,
                            ).toEntity(),
                        )
                    if (targetFolderId != null) {
                        projectFolderDao.insertOrReplaceAssignment(
                            ProjectFolderAssignmentEntity(projectId = projectId, folderId = targetFolderId),
                        )
                    }
                    ProjectCreationResult.Created(projectId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (constraint: SQLiteConstraintException) {
                if (targetFolderId != null && projectFolderDao.getById(targetFolderId) == null) {
                    ProjectCreationResult.FolderMissing
                } else {
                    throw constraint
                }
            }

        suspend fun updateProject(project: CounterProject) {
            transactionRunner.run {
                val projectName = uniqueProjectName(project.name, excludedProjectId = project.id) ?: return@run
                val normalized = normalizeProjectDetails(project.copy(name = projectName)) ?: return@run
                dao.update(normalized.copy(updatedAt = System.currentTimeMillis()).toEntity())
            }
        }

        suspend fun updateProjectDetails(
            id: Long,
            name: String,
            craftType: CraftType,
            mainCounterLabelType: MainCounterLabelType,
            mainCounterCustomLabel: String?,
        ): CounterProject? =
            transactionRunner.run {
                val projectName = uniqueProjectName(name, excludedProjectId = id) ?: return@run null
                val labelType =
                    validatedMainCounterLabelType(
                        craftType = craftType,
                        labelType = mainCounterLabelType,
                        customLabel = mainCounterCustomLabel,
                    ) ?: return@run null
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
                dao.getProject(id)?.toDomain()
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

                val undoHistory =
                    if (change == MainCounterChange.Undo) dao.getLatestHistory(id) else null
                val changed =
                    when (change) {
                        MainCounterChange.Undo -> undoMainCounterChange(id, project, undoHistory)
                        else -> applyHistoryTrackedMainCounterChange(id, project, change)
                    }
                if (changed) {
                    dao.getProject(id)?.let { updated ->
                        checkpointActiveSessionForRowChange(
                            projectId = id,
                            newRow = updated.count,
                            undoneReset = undoHistory?.action == MainCounterChange.Reset.historyAction,
                        )
                    }
                }
                changed
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
        ): String? =
            transactionRunner.run {
                val projectName = uniqueProjectName(name, excludedProjectId = id) ?: return@run null
                dao.updateName(id, projectName, System.currentTimeMillis())
                projectName
            }

        suspend fun saveProjectNotes(
            id: Long,
            baseNotes: String,
            requestedNotes: String,
            creationAuthorized: Boolean = false,
        ): ProjectNotesSaveResult =
            try {
                transactionRunner.run {
                    val current =
                        dao.getProject(id)?.toDomain()
                            ?: return@run ProjectNotesSaveResult.ProjectUnavailable
                    val canCreateNotes =
                        current.notesCreated ||
                            current.notes.isNotBlank() ||
                            creationAuthorized ||
                            proManager?.hasFeature(ProFeature.NOTES) == true
                    if (!canCreateNotes && requestedNotes.isNotBlank()) {
                        return@run ProjectNotesSaveResult.FeatureUnavailable
                    }
                    val notesToSave =
                        mergeProjectNotes(
                            baseNotes = baseNotes,
                            requestedNotes = requestedNotes,
                            currentNotes = current.notes,
                        )
                    val updatedAt = System.currentTimeMillis()
                    dao.updateNotes(id, notesToSave, updatedAt)
                    ProjectNotesSaveResult.Saved(
                        current.copy(
                            notes = notesToSave,
                            notesCreated = current.notesCreated || notesToSave.isNotBlank(),
                            updatedAt = updatedAt,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ProjectNotesSaveResult.PersistenceFailure
            }

        suspend fun updateProjectSecondaryCount(
            id: Long,
            secondaryCount: Int,
        ): Boolean =
            transactionRunner.run {
                val project = dao.getProject(id)?.toDomain() ?: return@run false
                val canUseSecondaryCounter =
                    project.secondaryCounterUsed ||
                        proManager?.hasFeature(ProFeature.SECONDARY_COUNTER) != false
                if (!canUseSecondaryCounter) return@run false

                dao.updateSecondaryCount(id, secondaryCount, System.currentTimeMillis())
                true
            }

        suspend fun updateProjectSectionName(
            id: Long,
            sectionName: String?,
        ) = dao.updateSectionName(id, sectionName, System.currentTimeMillis())

        suspend fun updateProjectStitchCount(
            id: Long,
            stitchCount: Int?,
        ): Boolean {
            if (stitchCount != null && stitchCount <= 0) return false
            return transactionRunner.run {
                val project = dao.getProject(id)?.toDomain() ?: return@run false
                if (project.isCompleted) return@run false
                val trackingEnabled = project.stitchTrackingEnabled && stitchCount != null
                val currentStitch =
                    stitchCount
                        ?.takeIf { trackingEnabled }
                        ?.let { project.currentStitch.coerceIn(0, it) }
                        ?: 0
                dao.updateStitchState(
                    id = id,
                    stitchCount = stitchCount,
                    trackingEnabled = trackingEnabled,
                    currentStitch = currentStitch,
                    updatedAt = System.currentTimeMillis(),
                )
                true
            }
        }

        suspend fun updateStitchTrackingEnabled(
            id: Long,
            enabled: Boolean,
        ): Boolean =
            transactionRunner.run {
                val project = dao.getProject(id)?.toDomain() ?: return@run false
                if (project.isCompleted || (enabled && project.stitchCount == null)) return@run false
                val currentStitch = if (enabled) project.currentStitch else 0
                dao.updateStitchState(
                    id = id,
                    stitchCount = project.stitchCount,
                    trackingEnabled = enabled,
                    currentStitch = currentStitch,
                    updatedAt = System.currentTimeMillis(),
                )
                true
            }

        suspend fun applyStitchChange(
            id: Long,
            increment: Boolean,
        ): Boolean =
            transactionRunner.run {
                val project = dao.getProject(id) ?: return@run false
                val stitchCount = project.stitchCount ?: return@run false
                if (
                    project.isCompleted ||
                    !project.stitchTrackingEnabled ||
                    stitchCount <= 0 ||
                    project.currentStitch !in 0..stitchCount
                ) {
                    return@run false
                }
                val nextStitch =
                    if (increment) {
                        (project.currentStitch.toLong() + 1L)
                            .coerceAtMost(stitchCount.toLong())
                            .toInt()
                    } else {
                        (project.currentStitch.toLong() - 1L)
                            .coerceAtLeast(0L)
                            .toInt()
                    }
                if (nextStitch == project.currentStitch) return@run false
                dao.updateCurrentStitch(id, nextStitch, System.currentTimeMillis())
                true
            }

        suspend fun updatePattern(
            id: Long,
            patternUri: String?,
            patternName: String?,
            currentPatternPage: Int,
            patternRowMapping: String?,
        ) {
            if (patternUri == null) {
                detachPattern(id)
            } else {
                attachPattern(id, patternUri, patternName.orEmpty(), currentPatternPage, patternRowMapping)
            }
        }

        suspend fun attachPattern(
            id: Long,
            patternUri: String,
            patternName: String,
            currentPatternPage: Int,
            patternRowMapping: String?,
        ): ProjectDocumentMutationResult {
            val preflightFailure = projectDocumentRepository.preflightImportedPdf(patternUri, patternName)
            val result =
                preflightFailure ?: transactionRunner.run {
                    val project = dao.getProject(id)
                    val added =
                        projectDocumentRepository.addImportedPdfInCurrentTransaction(id, patternUri, patternName)
                    if (added is ProjectDocumentMutationResult.Added) {
                        if (added.document.isPrimary && project?.linkedPatternId == null) {
                            dao.updatePatternInformation(
                                id = id,
                                linkedPatternId = added.document.savedPatternId,
                                patternName = added.document.label,
                                updatedAt = System.currentTimeMillis(),
                            )
                        }
                        if (currentPatternPage != 0 || patternRowMapping != null) {
                            check(
                                projectDocumentRepository.updateViewerStateInTransaction(
                                    added.document.copy(
                                        currentPage = currentPatternPage,
                                        rowMapping = patternRowMapping,
                                    ),
                                ),
                            )
                        }
                    }
                    added
                }
            if (result !is ProjectDocumentMutationResult.Added &&
                result != ProjectDocumentMutationResult.AlreadyAttached &&
                result != ProjectDocumentMutationResult.DuplicateUri &&
                result != ProjectDocumentMutationResult.DuplicateDocumentKey
            ) {
                error("Project document attachment failed: $result")
            }
            return result
        }

        // Liitos validoidaan ja kirjoitetaan yhtenä atomisena polkuna.
        suspend fun attachSavedPattern(
            projectId: Long,
            savedPatternId: Long,
        ): SavedPattern? {
            val preparation = projectDocumentRepository.prepareSavedPatternDocument(projectId, savedPatternId)
            if (preparation == SavedPatternDocumentPreparation.MissingSavedPattern ||
                preparation == SavedPatternDocumentPreparation.PdfUnavailable
            ) {
                return null
            }
            return transactionRunner.run {
                val project = dao.getProject(projectId) ?: return@run null
                val pattern = savedPatternRepository.getById(savedPatternId) ?: return@run null
                val result =
                    attachPreparedSavedPattern(
                        projectId = projectId,
                        savedPatternId = savedPatternId,
                        preparation = preparation,
                        linkedPatternId = project.linkedPatternId,
                        pattern = pattern,
                    )
                if (!result.isSuccessfulSavedPatternAttachment()) return@run null
                pattern
            }
        }

        private suspend fun attachPreparedSavedPattern(
            projectId: Long,
            savedPatternId: Long,
            preparation: SavedPatternDocumentPreparation,
            linkedPatternId: Long?,
            pattern: SavedPattern,
        ): ProjectDocumentMutationResult? =
            when (preparation) {
                SavedPatternDocumentPreparation.MetadataOnlyPattern ->
                    attachMetadataOnlySavedPattern(projectId, linkedPatternId, pattern)
                SavedPatternDocumentPreparation.AlreadyAttached ->
                    if (
                        projectDocumentRepository.hasSavedPatternDocumentInCurrentTransaction(
                            projectId,
                            savedPatternId,
                        )
                    ) {
                        ProjectDocumentMutationResult.AlreadyAttached
                    } else {
                        null
                    }
                is SavedPatternDocumentPreparation.Ready ->
                    attachSavedPatternDocument(
                        projectId = projectId,
                        savedPatternId = savedPatternId,
                        verifiedLocalPdfUri = preparation.localPdfUri,
                        linkedPatternId = linkedPatternId,
                        pattern = pattern,
                    )
                SavedPatternDocumentPreparation.MissingSavedPattern,
                SavedPatternDocumentPreparation.PdfUnavailable,
                -> null
            }

        private suspend fun attachMetadataOnlySavedPattern(
            projectId: Long,
            linkedPatternId: Long?,
            pattern: SavedPattern,
        ): ProjectDocumentMutationResult? {
            if (!pattern.localPdfUri.isNullOrBlank()) return null
            if (
                pattern.isWebPatternCompatible &&
                linkedPatternId != null &&
                linkedPatternId != pattern.id
            ) {
                return null
            }
            dao.updatePatternInformation(
                id = projectId,
                linkedPatternId = pattern.id,
                patternName = pattern.name,
                updatedAt = System.currentTimeMillis(),
            )
            return ProjectDocumentMutationResult.MetadataOnlyPattern
        }

        private suspend fun attachSavedPatternDocument(
            projectId: Long,
            savedPatternId: Long,
            verifiedLocalPdfUri: String,
            linkedPatternId: Long?,
            pattern: SavedPattern,
        ): ProjectDocumentMutationResult {
            val added =
                projectDocumentRepository.addSavedPatternInCurrentTransaction(
                    projectId = projectId,
                    savedPatternId = savedPatternId,
                    verifiedLocalPdfUri = verifiedLocalPdfUri,
                )
            if (added is ProjectDocumentMutationResult.Added && added.document.isPrimary && linkedPatternId == null) {
                dao.updatePatternInformation(
                    id = projectId,
                    linkedPatternId = pattern.id,
                    patternName = pattern.name,
                    updatedAt = System.currentTimeMillis(),
                )
            }
            return added
        }

        private fun ProjectDocumentMutationResult?.isSuccessfulSavedPatternAttachment(): Boolean =
            this == ProjectDocumentMutationResult.MetadataOnlyPattern ||
                this is ProjectDocumentMutationResult.Added ||
                this == ProjectDocumentMutationResult.AlreadyAttached

        @Suppress("kotlin:S3776") // Atominen korvauspolku pitää stale-action-tarkistukset samassa transaktiossa.
        suspend fun attachSavedPatternMetadata(
            projectId: Long,
            savedPatternId: Long,
            expectedExistingSavedPatternId: Long? = null,
        ): SavedPatternMetadataMutationResult =
            persistSavedPatternMetadataMutation {
                transactionRunner.run {
                    val project =
                        dao.getProject(projectId)
                            ?: return@run SavedPatternMetadataMutationResult.ProjectMissing
                    val pattern =
                        savedPatternRepository.getById(savedPatternId)
                            ?: return@run SavedPatternMetadataMutationResult.PatternMissing
                    if (!pattern.isWebPatternCompatible) {
                        return@run SavedPatternMetadataMutationResult.NotWebPattern
                    }
                    if (project.linkedPatternId == savedPatternId && project.patternName == pattern.name) {
                        return@run SavedPatternMetadataMutationResult.AlreadyAttached(savedPatternId)
                    }
                    if (project.linkedPatternId == null && expectedExistingSavedPatternId != null) {
                        return@run SavedPatternMetadataMutationResult.StaleAction
                    }
                    if (project.linkedPatternId != null && project.linkedPatternId != savedPatternId) {
                        if (expectedExistingSavedPatternId == null) {
                            return@run SavedPatternMetadataMutationResult.ReplacementRequired(project.linkedPatternId)
                        }
                        if (expectedExistingSavedPatternId != project.linkedPatternId) {
                            return@run SavedPatternMetadataMutationResult.StaleAction
                        }
                    }

                    dao.updatePatternInformation(
                        id = projectId,
                        linkedPatternId = savedPatternId,
                        patternName = pattern.name,
                        updatedAt = System.currentTimeMillis(),
                    )
                    SavedPatternMetadataMutationResult.Attached(savedPatternId)
                }
            }

        suspend fun unlinkSavedPatternMetadata(
            projectId: Long,
            expectedSavedPatternId: Long,
        ): SavedPatternMetadataMutationResult =
            persistSavedPatternMetadataMutation {
                transactionRunner.run {
                    val project =
                        dao.getProject(projectId)
                            ?: return@run SavedPatternMetadataMutationResult.ProjectMissing
                    if (project.linkedPatternId == null) {
                        return@run SavedPatternMetadataMutationResult.AlreadyUnlinked
                    }
                    if (project.linkedPatternId != expectedSavedPatternId) {
                        return@run SavedPatternMetadataMutationResult.StaleAction
                    }

                    val updated =
                        dao.clearPatternInformationIfLinked(
                            projectId = projectId,
                            expectedSavedPatternId = expectedSavedPatternId,
                            updatedAt = System.currentTimeMillis(),
                        )
                    if (updated == 1) {
                        SavedPatternMetadataMutationResult.Unlinked
                    } else {
                        SavedPatternMetadataMutationResult.StaleAction
                    }
                }
            }

        suspend fun detachPattern(id: Long) {
            val primary = projectDocumentRepository.getPrimary(id) ?: return
            projectDocumentRepository.remove(id, primary.id)
        }

        private suspend fun persistSavedPatternMetadataMutation(
            mutation: suspend () -> SavedPatternMetadataMutationResult,
        ): SavedPatternMetadataMutationResult =
            try {
                mutation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                SavedPatternMetadataMutationResult.PersistenceFailure
            }

        suspend fun updateCurrentPatternPage(
            id: Long,
            page: Int,
            documentId: Long? = null,
        ) {
            transactionRunner.run {
                val document = viewerDocument(id, documentId) ?: return@run
                check(
                    projectDocumentRepository.updateViewerStateInTransaction(
                        document.copy(
                            currentPage = page.coerceAtLeast(0),
                            readingLineFollowCurrentRow = false,
                        ),
                    ),
                )
            }
        }

        suspend fun updatePatternRowMapping(
            id: Long,
            mapping: String?,
            documentId: Long? = null,
        ) {
            transactionRunner.run {
                val project = dao.getProject(id)?.toDomain() ?: return@run
                val document = viewerDocument(id, documentId) ?: return@run
                val updatedDocument = document.copy(rowMapping = mapping)
                if (updatedDocument.readingLineFollowCurrentRow) {
                    applyReadingLineFollow(
                        document = updatedDocument,
                        previousRow = project.count,
                        newRow = project.count,
                    )
                } else {
                    check(projectDocumentRepository.updateViewerStateInTransaction(updatedDocument))
                }
            }
        }

        suspend fun updateReadingLine(
            id: Long,
            enabled: Boolean,
            yFraction: Float,
            documentId: Long? = null,
        ) {
            val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)
            transactionRunner.run {
                val document = viewerDocument(id, documentId) ?: return@run
                check(
                    projectDocumentRepository.updateViewerStateInTransaction(
                        document.copy(readingLineEnabled = enabled, readingLineYFraction = sanitizedYFraction),
                    ),
                )
            }
        }

        suspend fun updateReadingLineVisibility(
            id: Long,
            enabled: Boolean,
            documentId: Long? = null,
        ) {
            transactionRunner.run {
                val document = viewerDocument(id, documentId) ?: return@run
                check(
                    projectDocumentRepository.updateViewerStateInTransaction(
                        document.copy(readingLineEnabled = enabled),
                    ),
                )
            }
        }

        suspend fun commitManualReadingLinePosition(
            id: Long,
            yFraction: Float,
            documentId: Long? = null,
        ) {
            transactionRunner.run {
                val document = viewerDocument(id, documentId) ?: return@run
                check(
                    projectDocumentRepository.updateViewerStateInTransaction(
                        document.copy(
                            readingLineYFraction = sanitizeReadingLineYFraction(yFraction),
                            readingLineFollowCurrentRow = false,
                        ),
                    ),
                )
            }
        }

        suspend fun setReadingLineFollowCurrentRow(
            id: Long,
            enabled: Boolean,
            documentId: Long? = null,
        ): ReadingLineLocationResolution? =
            transactionRunner.run {
                val project = dao.getProject(id)?.toDomain() ?: return@run null
                val document = viewerDocument(id, documentId) ?: return@run null
                if (!enabled) {
                    check(
                        projectDocumentRepository.updateViewerStateInTransaction(
                            document.copy(readingLineFollowCurrentRow = false),
                        ),
                    )
                    return@run null
                }
                applyReadingLineFollow(
                    document = document.copy(readingLineFollowCurrentRow = true),
                    previousRow = project.count,
                    newRow = project.count,
                )
            }

        suspend fun updateVerticalReadingGuide(
            id: Long,
            enabled: Boolean?,
            xFraction: Float?,
            documentId: Long? = null,
        ) {
            transactionRunner.run {
                val document = viewerDocument(id, documentId) ?: return@run
                check(
                    projectDocumentRepository.updateViewerStateInTransaction(
                        document.copy(
                            verticalReadingGuideEnabled = enabled ?: document.verticalReadingGuideEnabled,
                            verticalReadingGuideXFraction =
                                xFraction?.let(::sanitizeReadingGuideFraction)
                                    ?: document.verticalReadingGuideXFraction,
                        ),
                    ),
                )
            }
        }

        suspend fun updateProjectStepSize(
            id: Long,
            stepSize: Int,
        ) = dao.updateStepSize(id, stepSize.coerceAtLeast(1), System.currentTimeMillis())

        suspend fun updateProjectYarnCardIds(
            id: Long,
            yarnCardIds: String,
        ) = dao.updateYarnCardIds(id, yarnCardIds, System.currentTimeMillis())

        suspend fun archiveProject(
            id: Long,
            completedAt: Long,
        ) {
            completeProjectWithSessionChoice(
                projectId = id,
                choice = null,
                completedAtMillis = completedAt,
            )
        }

        suspend fun reactivateProject(id: Long): Boolean =
            transactionRunner.run {
                val project = dao.getProject(id) ?: return@run false
                if (!project.isCompleted) return@run false
                dao.reactivateProject(id, System.currentTimeMillis())
                true
            }

        suspend fun deleteProject(id: Long) {
            val cleanup =
                transactionRunner.run {
                    if (sessionDao.getActiveSession()?.projectId == id) return@run null
                    val capturedCleanup = captureProjectCleanup(id) ?: return@run null
                    yarnCardRepository.clearLinkedProject(id)
                    dao.delete(id)
                    capturedCleanup
                }
            cleanup?.let { cleanupProjectFiles(it) }
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

        private fun hasAdditionalProjectAccess(callerAllowsAdditionalProjects: Boolean): Boolean =
            callerAllowsAdditionalProjects &&
                (proManager?.hasFeature(ProFeature.UNLIMITED_PROJECTS) ?: true)

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
            projectDocumentRepository.getActiveDocument(id)?.let { document ->
                applyReadingLineFollow(
                    document = document,
                    previousRow = before.count,
                    newRow = after.count,
                )
            }
            return true
        }

        private suspend fun undoMainCounterChange(
            id: Long,
            project: CounterProject,
            history: CounterHistoryEntity?,
        ): Boolean {
            if (history == null || !history.isReversibleFor(id, project.count)) return false
            val updatedAt = System.currentTimeMillis()
            dao.updateCount(id, history.previousValue, updatedAt)
            dao.deleteHistoryById(history.id)
            resetCurrentStitchIfNeeded(id, project, updatedAt)
            applyLinkedCounterDelta(id, history.previousValue - history.newValue)
            projectDocumentRepository.getActiveDocument(id)?.let { document ->
                applyReadingLineFollow(
                    document = document,
                    previousRow = project.count,
                    newRow = history.previousValue,
                )
            }
            return true
        }

        private fun CounterHistoryEntity.isReversibleFor(
            expectedProjectId: Long,
            currentCount: Int,
        ): Boolean {
            if (
                projectId != expectedProjectId ||
                previousValue < 0 ||
                newValue != currentCount ||
                previousValue == newValue
            ) {
                return false
            }
            return when (action) {
                MainCounterChange.Increment.historyAction -> newValue > previousValue
                MainCounterChange.Decrement.historyAction -> newValue < previousValue
                MainCounterChange.Reset.historyAction -> newValue == 0 && previousValue > 0
                else -> false
            }
        }

        private suspend fun applyReadingLineFollow(
            document: ProjectDocument,
            previousRow: Int,
            newRow: Int,
        ): ReadingLineLocationResolution? {
            if (!document.readingLineFollowCurrentRow) return null
            val resolution =
                resolveReadingLineLocation(
                    markers = parseMapping(document.rowMapping),
                    previousRow = previousRow,
                    newRow = newRow,
                    currentPage = document.currentPage,
                    currentYFraction = document.readingLineYFraction,
                )
            check(
                projectDocumentRepository.updateViewerStateInTransaction(
                    document.copy(
                        currentPage = resolution.targetPage,
                        readingLineYFraction = resolution.targetYFraction,
                        readingLineFollowCurrentRow = true,
                    ),
                ),
            )
            return resolution
        }

        private suspend fun viewerDocument(
            projectId: Long,
            documentId: Long?,
        ): ProjectDocument? =
            if (documentId == null) {
                projectDocumentRepository.getActiveDocument(projectId)
            } else {
                projectDocumentRepository.getDocument(documentId)?.takeIf { it.projectId == projectId }
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
                .filter { counter ->
                    counter.linkedToMainCounter &&
                        ProjectCounterLogic.canLinkToMainCounter(
                            ProjectCounterType.fromPersistedValue(counter.counterType),
                        )
                }.forEach { counter ->
                    val updatedCount =
                        (counter.count.toLong() + delta)
                            .coerceIn(0L, Int.MAX_VALUE.toLong())
                            .toInt()
                    if (updatedCount != counter.count) {
                        projectCounterDao.updateCount(counter.id, updatedCount)
                    }
                }
        }

        suspend fun deleteHistoryBefore(
            projectId: Long,
            before: Long,
        ) = dao.deleteHistoryBefore(projectId, before)

        suspend fun undoLastChange(projectId: Long) {
            applyMainCounterChange(projectId, MainCounterChange.Undo)
        }

        suspend fun setTargetRows(
            projectId: Long,
            targetRows: Int?,
        ) = dao.updateTargetRows(projectId, targetRows, System.currentTimeMillis())

        fun observeActiveSession(): Flow<ActiveWorkSession?> =
            sessionDao
                .observeActiveSession()
                .map { it?.toDomain() }
                .distinctUntilChanged()
                .retryOnRepositoryReadFailure()

        suspend fun refreshActiveSession(): ActiveWorkSession? =
            transactionRunner.run {
                synchronizeActiveSession(sessionTimeSource.snapshot())?.toDomain()
            }

        fun activeSessionDurationSeconds(session: ActiveWorkSession): Long =
            when (val evaluation = evaluateActiveSessionTime(session.timingAnchors, sessionTimeSource.snapshot())) {
                is ActiveSessionTimeEvaluation.Trusted -> evaluation.totalDurationSeconds
                is ActiveSessionTimeEvaluation.NeedsReview ->
                    saturatingAdd(
                        session.timingAnchors.checkpointedDurationSeconds,
                        session.recoverySuggestedDurationSeconds ?: 0L,
                    )
            }

        fun activeSessionNeedsRecovery(session: ActiveWorkSession): Boolean =
            session.needsRecoveryReview ||
                evaluateActiveSessionTime(
                    session.timingAnchors,
                    sessionTimeSource.snapshot(),
                ) is ActiveSessionTimeEvaluation.NeedsReview

        suspend fun startSession(projectId: Long): StartSessionResult =
            runSessionMutation(StartSessionResult.PersistenceFailure) {
                val project =
                    dao.getProject(projectId)?.toDomain()
                        ?: return@runSessionMutation StartSessionResult.ProjectMissing
                if (project.isCompleted) return@runSessionMutation StartSessionResult.ProjectCompleted
                val current = synchronizeActiveSession(sessionTimeSource.snapshot())
                if (current != null) {
                    val active = current.toDomain()
                    return@runSessionMutation if (current.projectId == projectId) {
                        StartSessionResult.AlreadyActive(active)
                    } else {
                        StartSessionResult.ProjectConflict(active, projectId)
                    }
                }

                createStartedSession(projectId, project.count, sessionTimeSource.snapshot())
            }

        suspend fun checkpointActiveSession(): ActiveWorkSession? =
            transactionRunner.run {
                val now = sessionTimeSource.snapshot()
                val active = synchronizeActiveSession(now) ?: return@run null
                checkpointTrustedSession(active, now).toDomain()
            }

        suspend fun markRecoveryPromptShown(
            sessionToken: String,
            recoveryIntervalToken: String,
        ): Boolean =
            transactionRunner.run {
                val active = sessionDao.getActiveSession() ?: return@run false
                if (active.sessionToken != sessionToken || active.recoveryIntervalToken != recoveryIntervalToken) {
                    return@run false
                }
                if (!active.recoveryPromptShown) {
                    sessionDao.updateActiveSession(
                        active.copy(
                            recoveryPromptShown = true,
                            updatedAtWallMillis = sessionTimeSource.snapshot().wallClockMillis,
                        ),
                    )
                }
                true
            }

        suspend fun stopSession(
            sessionToken: String,
            reviewedDurationSeconds: Long,
            reviewedRowsWorked: Int,
            reviewedEndRow: Int,
        ): StopSessionResult =
            runSessionMutation(StopSessionResult.PersistenceFailure) {
                val now = sessionTimeSource.snapshot()
                val active =
                    synchronizeActiveSession(now)
                        ?: return@runSessionMutation StopSessionResult.NoActiveSession
                if (active.sessionToken != sessionToken) return@runSessionMutation StopSessionResult.StaleAction
                if (active.recoveryReason != null) {
                    return@runSessionMutation StopSessionResult.NeedsRecoveryReview(active.toDomain())
                }
                val currentDurationSeconds =
                    (evaluateActiveSessionTime(active.timingAnchors(), now) as? ActiveSessionTimeEvaluation.Trusted)
                        ?.totalDurationSeconds
                        ?: return@runSessionMutation StopSessionResult.NeedsRecoveryReview(active.toDomain())
                if (
                    reviewedDurationSeconds !in active.checkpointedDurationSeconds..currentDurationSeconds ||
                    reviewedRowsWorked != active.trustedRowsWorked ||
                    reviewedEndRow != active.trustedLastObservedRow
                ) {
                    return@runSessionMutation StopSessionResult.StaleAction
                }
                val completedSessionId =
                    insertCompletedSession(
                        active = active,
                        durationSeconds = reviewedDurationSeconds,
                        rowsWorked = reviewedRowsWorked,
                        endRow = reviewedEndRow,
                    )
                sessionDao.deleteActiveSession(active.sessionToken)
                StopSessionResult.Saved(completedSessionId)
            }

        suspend fun discardActiveSession(sessionToken: String): StopSessionResult =
            runSessionMutation(StopSessionResult.PersistenceFailure) {
                val active =
                    sessionDao.getActiveSession()
                        ?: return@runSessionMutation StopSessionResult.NoActiveSession
                if (active.sessionToken != sessionToken) return@runSessionMutation StopSessionResult.StaleAction
                sessionDao.deleteActiveSession(sessionToken)
                StopSessionResult.Discarded
            }

        suspend fun addRecoveryInterval(
            sessionToken: String,
            recoveryIntervalToken: String,
            durationSeconds: Long,
        ): RecoveryResolutionResult =
            runSessionMutation(RecoveryResolutionResult.PersistenceFailure) {
                if (durationSeconds < 0L) return@runSessionMutation RecoveryResolutionResult.InvalidDuration
                val active =
                    activeRecoverySession(sessionToken, recoveryIntervalToken)
                        ?: return@runSessionMutation RecoveryResolutionResult.StaleAction
                val now = sessionTimeSource.snapshot()
                val checkpointed = saturatingAdd(active.checkpointedDurationSeconds, durationSeconds)
                val updated =
                    active.copy(
                        trustedRowsWorked = saturatingRows(active.trustedRowsWorked, active.pendingRowsWorked),
                        pendingRowsWorked = 0,
                        trustedLastObservedRow = active.lastObservedRow,
                        reviewedRowsWorked = saturatingRows(active.trustedRowsWorked, active.pendingRowsWorked),
                        reviewedLastObservedRow = active.lastObservedRow,
                        unreviewedRowsWorked = 0,
                        checkpointedDurationSeconds = checkpointed,
                        reviewedDurationBaselineSeconds = checkpointed,
                        segmentStartedAtWallMillis = now.wallClockMillis,
                        segmentStartedElapsedRealtimeMillis = now.elapsedRealtimeMillis,
                        bootCount = now.bootCount,
                        recoveryReason = null,
                        recoveryIntervalToken = null,
                        recoverySuggestedDurationSeconds = null,
                        recoveryPromptShown = false,
                        updatedAtWallMillis = now.wallClockMillis,
                    )
                sessionDao.updateActiveSession(updated)
                RecoveryResolutionResult.Continued(updated.toDomain())
            }

        suspend fun editRecoveryDurationAndStop(
            sessionToken: String,
            recoveryIntervalToken: String,
            totalDurationSeconds: Long,
        ): RecoveryResolutionResult =
            runSessionMutation(RecoveryResolutionResult.PersistenceFailure) {
                val active =
                    activeRecoverySession(sessionToken, recoveryIntervalToken)
                        ?: return@runSessionMutation RecoveryResolutionResult.StaleAction
                if (!canRepresentCompletedDuration(active.startedAtWallMillis, totalDurationSeconds)) {
                    return@runSessionMutation RecoveryResolutionResult.InvalidDuration
                }
                val completedSessionId =
                    insertCompletedSession(
                        active = active,
                        durationSeconds = totalDurationSeconds,
                        rowsWorked = saturatingRows(active.trustedRowsWorked, active.pendingRowsWorked),
                        endRow = active.lastObservedRow,
                    )
                sessionDao.deleteActiveSession(active.sessionToken)
                RecoveryResolutionResult.EditedAndStopped(completedSessionId)
            }

        suspend fun discardRecoveryInterval(
            sessionToken: String,
            recoveryIntervalToken: String,
        ): RecoveryResolutionResult =
            runSessionMutation(RecoveryResolutionResult.PersistenceFailure) {
                val active =
                    sessionDao.getActiveSession()
                        ?: return@runSessionMutation RecoveryResolutionResult.StaleAction
                if (
                    active.sessionToken != sessionToken ||
                    active.recoveryIntervalToken != recoveryIntervalToken ||
                    active.recoveryReason == null
                ) {
                    return@runSessionMutation RecoveryResolutionResult.StaleAction
                }
                val completedSessionId =
                    insertCompletedSession(
                        active = active,
                        durationSeconds = active.checkpointedDurationSeconds,
                        rowsWorked = active.trustedRowsWorked,
                        endRow = active.trustedLastObservedRow,
                    )
                sessionDao.deleteActiveSession(active.sessionToken)
                RecoveryResolutionResult.DiscardedAndStopped(completedSessionId)
            }

        private suspend fun activeRecoverySession(
            sessionToken: String,
            recoveryIntervalToken: String,
        ): ActiveSessionEntity? =
            sessionDao.getActiveSession()?.takeIf { active ->
                active.sessionToken == sessionToken &&
                    active.recoveryIntervalToken == recoveryIntervalToken &&
                    active.recoveryReason != null
            }

        suspend fun replaceActiveSession(
            requestedProjectId: Long,
            expectedSessionToken: String,
            saveCurrent: Boolean,
        ): StartSessionResult =
            runSessionMutation(StartSessionResult.PersistenceFailure) {
                val requested =
                    dao.getProject(requestedProjectId)?.toDomain()
                        ?: return@runSessionMutation StartSessionResult.ProjectMissing
                if (requested.isCompleted) return@runSessionMutation StartSessionResult.ProjectCompleted
                val now = sessionTimeSource.snapshot()
                val current = synchronizeActiveSession(now)
                if (current == null) {
                    return@runSessionMutation createStartedSession(requestedProjectId, requested.count, now)
                }
                if (current.sessionToken != expectedSessionToken) {
                    return@runSessionMutation StartSessionResult.ProjectConflict(current.toDomain(), requestedProjectId)
                }
                if (saveCurrent && current.recoveryReason != null) {
                    return@runSessionMutation StartSessionResult.ProjectConflict(current.toDomain(), requestedProjectId)
                }
                if (saveCurrent) {
                    saveAndDeleteActiveSession(current, now)
                } else {
                    sessionDao.deleteActiveSession(current.sessionToken)
                }
                createStartedSession(requestedProjectId, requested.count, now)
            }

        suspend fun completeProjectWithSessionChoice(
            projectId: Long,
            choice: ActiveSessionCompletionChoice?,
            completedAtMillis: Long? = null,
        ): ProjectCompletionResult =
            runSessionMutation(ProjectCompletionResult.PersistenceFailure) {
                val project = dao.getProject(projectId)
                if (project == null) {
                    return@runSessionMutation ProjectCompletionResult.ProjectUnavailable
                }
                if (project.isCompleted) return@runSessionMutation ProjectCompletionResult.Completed
                val now = sessionTimeSource.snapshot()
                val active = synchronizeActiveSession(now)
                val activeSessionResult = finalizeActiveSessionForCompletion(projectId, choice, active, now)
                if (activeSessionResult != null) return@runSessionMutation activeSessionResult
                val completedAt = completedAtMillis ?: now.wallClockMillis
                dao.archiveProject(projectId, project.count, completedAt, completedAt)
                ProjectCompletionResult.Completed
            }

        private suspend fun finalizeActiveSessionForCompletion(
            projectId: Long,
            choice: ActiveSessionCompletionChoice?,
            active: ActiveSessionEntity?,
            now: com.finnvek.knittools.domain.model.SessionTimeSnapshot,
        ): ProjectCompletionResult? {
            if (active?.projectId != projectId) return null
            if (choice == null) return ProjectCompletionResult.NeedsActiveSessionChoice(active.toDomain())
            if (choice == ActiveSessionCompletionChoice.SAVE && active.recoveryReason != null) {
                return ProjectCompletionResult.NeedsRecoveryReview(active.toDomain())
            }
            if (choice == ActiveSessionCompletionChoice.SAVE) {
                saveAndDeleteActiveSession(active, now)
            } else {
                sessionDao.deleteActiveSession(active.sessionToken)
            }
            return null
        }

        suspend fun deleteProjectResolvingActiveSession(
            id: Long,
            discardActiveSession: Boolean,
        ): ProjectDeletionResult =
            try {
                deleteProjectResolvingActiveSessionInternal(id, discardActiveSession)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ProjectDeletionResult.PersistenceFailure
            }

        private suspend fun deleteProjectResolvingActiveSessionInternal(
            id: Long,
            discardActiveSession: Boolean,
        ): ProjectDeletionResult {
            val conflict =
                transactionRunner.run {
                    val project = dao.getProject(id) ?: return@run ProjectDeletionResult.ProjectUnavailable
                    val active = sessionDao.getActiveSession()
                    if (active?.projectId == project.id && !discardActiveSession) {
                        ProjectDeletionResult.NeedsActiveSessionDiscard(active.toDomain())
                    } else {
                        null
                    }
                }
            if (conflict != null) return conflict

            var cleanup: ProjectFileCleanup? = null
            val result =
                transactionRunner.run {
                    val project = dao.getProject(id) ?: return@run ProjectDeletionResult.ProjectUnavailable
                    val active = sessionDao.getActiveSession()
                    if (active?.projectId == project.id) {
                        if (!discardActiveSession) {
                            return@run ProjectDeletionResult.NeedsActiveSessionDiscard(active.toDomain())
                        }
                        sessionDao.deleteActiveSession(active.sessionToken)
                    }
                    cleanup = captureProjectCleanup(id) ?: return@run ProjectDeletionResult.ProjectUnavailable
                    yarnCardRepository.clearLinkedProject(id)
                    dao.delete(id)
                    ProjectDeletionResult.Deleted
                }
            if (result == ProjectDeletionResult.Deleted) {
                cleanup?.let { cleanupProjectFiles(it) }
            }
            return result
        }

        private suspend fun captureProjectCleanup(id: Long): ProjectFileCleanup? {
            val project = dao.getProject(id) ?: return null
            val patternUris =
                (projectDocumentRepository.getDistinctUris(id) + listOfNotNull(project.patternUri))
                    .filter(String::isNotBlank)
                    .distinct()
            return ProjectFileCleanup(projectId = id, patternUris = patternUris)
        }

        private suspend fun cleanupProjectFiles(cleanup: ProjectFileCleanup) {
            withContext(NonCancellable) {
                withContext(ioDispatcher) {
                    runCatching { photoStorage.deleteProjectPhotos(context, cleanup.projectId) }
                    runCatching { patternDocumentStorage.deleteProjectCaptureImages(context, cleanup.projectId) }
                }
                cleanup.patternUris.forEach { patternUri ->
                    runCatching { savedPatternRepository.deleteLocalPatternFileIfUnused(patternUri) }
                }
            }
        }

        private data class ProjectFileCleanup(
            val projectId: Long,
            val patternUris: List<String>,
        )

        // Session-metodit
        fun getSessionsForProject(projectId: Long): Flow<List<KnitSession>> =
            sessionDao.getSessionsForProject(projectId).toDomainSessions()

        fun getAllSessions(projectId: Long?): Flow<List<KnitSession>> =
            sessionDao.getAllSessions(projectId).toDomainSessions()

        fun getCompletedProjectCount(): Flow<Int> = sessionDao.getCompletedProjectCount().retryOnRepositoryReadFailure()

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

        private suspend fun synchronizeActiveSession(
            now: com.finnvek.knittools.domain.model.SessionTimeSnapshot,
        ): ActiveSessionEntity? {
            val active = sessionDao.getActiveSession() ?: return null
            if (active.recoveryReason != null) {
                if (active.recoveryIntervalToken != null) return active
                val repaired =
                    active.copy(
                        recoveryReason = ActiveSessionRecoveryReason.INVALID_ANCHORS.name,
                        recoveryIntervalToken = UUID.randomUUID().toString(),
                        recoverySuggestedDurationSeconds = null,
                        recoveryPromptShown = false,
                        updatedAtWallMillis = now.wallClockMillis,
                    )
                sessionDao.updateActiveSession(repaired)
                return repaired
            }
            if (
                active.sessionToken.isBlank() ||
                active.projectId <= 0L ||
                runCatching { ZoneId.of(active.startZoneId) }.isFailure
            ) {
                val malformed =
                    active.copy(
                        recoveryReason = ActiveSessionRecoveryReason.INVALID_ANCHORS.name,
                        recoveryIntervalToken = UUID.randomUUID().toString(),
                        recoverySuggestedDurationSeconds = null,
                        recoveryPromptShown = false,
                        updatedAtWallMillis = now.wallClockMillis,
                    )
                sessionDao.updateActiveSession(malformed)
                return malformed
            }
            return when (val evaluation = evaluateActiveSessionTime(active.timingAnchors(), now)) {
                is ActiveSessionTimeEvaluation.Trusted -> active
                is ActiveSessionTimeEvaluation.NeedsReview -> {
                    val recoveryBase =
                        if (evaluation.reason == ActiveSessionRecoveryReason.LONG_RUNNING) {
                            active.copy(
                                checkpointedDurationSeconds = active.reviewedDurationBaselineSeconds,
                                trustedLastObservedRow = active.reviewedLastObservedRow,
                                trustedRowsWorked = active.reviewedRowsWorked,
                                pendingRowsWorked =
                                    saturatingRows(active.pendingRowsWorked, active.unreviewedRowsWorked),
                                unreviewedRowsWorked = 0,
                            )
                        } else {
                            active
                        }
                    val recovery =
                        recoveryBase.copy(
                            recoveryReason = evaluation.reason.name,
                            recoveryIntervalToken = UUID.randomUUID().toString(),
                            recoverySuggestedDurationSeconds = evaluation.suggestedPendingDurationSeconds,
                            recoveryPromptShown = false,
                            updatedAtWallMillis = now.wallClockMillis,
                        )
                    sessionDao.updateActiveSession(recovery)
                    recovery
                }
            }
        }

        private suspend fun checkpointTrustedSession(
            active: ActiveSessionEntity,
            now: com.finnvek.knittools.domain.model.SessionTimeSnapshot,
        ): ActiveSessionEntity {
            if (active.recoveryReason != null) return active
            return when (val evaluation = evaluateActiveSessionTime(active.timingAnchors(), now)) {
                is ActiveSessionTimeEvaluation.NeedsReview ->
                    synchronizeActiveSession(now) ?: active
                is ActiveSessionTimeEvaluation.Trusted -> {
                    val remainingMillis =
                        (now.elapsedRealtimeMillis - active.segmentStartedElapsedRealtimeMillis) % 1_000L
                    val updated =
                        active.copy(
                            checkpointedDurationSeconds = evaluation.totalDurationSeconds,
                            segmentStartedAtWallMillis = now.wallClockMillis - remainingMillis,
                            segmentStartedElapsedRealtimeMillis = now.elapsedRealtimeMillis - remainingMillis,
                            bootCount = now.bootCount,
                            updatedAtWallMillis = now.wallClockMillis,
                        )
                    sessionDao.updateActiveSession(updated)
                    updated
                }
            }
        }

        private suspend fun checkpointActiveSessionForRowChange(
            projectId: Long,
            newRow: Int,
            undoneReset: Boolean,
        ) {
            val now = sessionTimeSource.snapshot()
            val synchronized = synchronizeActiveSession(now) ?: return
            if (synchronized.projectId != projectId) return
            val delta = newRow.toLong() - synchronized.lastObservedRow.toLong()
            if (synchronized.recoveryReason != null) {
                val pendingRowsWorked =
                    if (undoneReset) {
                        positiveRowDelta(synchronized.trustedLastObservedRow, newRow)
                    } else {
                        adjustRowsWorked(synchronized.pendingRowsWorked, delta)
                    }
                sessionDao.updateActiveSession(
                    synchronized.copy(
                        lastObservedRow = newRow,
                        pendingRowsWorked = pendingRowsWorked,
                        updatedAtWallMillis = now.wallClockMillis,
                    ),
                )
                return
            }
            val checkpointed = checkpointTrustedSession(synchronized, now)
            val unreviewedRowsWorked =
                if (undoneReset) {
                    positiveRowDelta(checkpointed.reviewedLastObservedRow, newRow)
                } else {
                    adjustRowsWorked(checkpointed.unreviewedRowsWorked, delta)
                }
            val trustedRowsWorked =
                if (undoneReset) {
                    saturatingRows(checkpointed.reviewedRowsWorked, unreviewedRowsWorked)
                } else {
                    adjustRowsWorked(checkpointed.trustedRowsWorked, delta)
                }
            sessionDao.updateActiveSession(
                checkpointed.copy(
                    lastObservedRow = newRow,
                    trustedLastObservedRow = newRow,
                    trustedRowsWorked = trustedRowsWorked,
                    unreviewedRowsWorked = unreviewedRowsWorked,
                    updatedAtWallMillis = now.wallClockMillis,
                ),
            )
        }

        private suspend fun saveAndDeleteActiveSession(
            active: ActiveSessionEntity,
            now: com.finnvek.knittools.domain.model.SessionTimeSnapshot,
        ): Long? {
            val evaluation = evaluateActiveSessionTime(active.timingAnchors(), now)
            val trusted = evaluation as? ActiveSessionTimeEvaluation.Trusted
            val completedSessionId =
                insertCompletedSession(
                    active = active,
                    durationSeconds = trusted?.totalDurationSeconds ?: active.checkpointedDurationSeconds,
                    rowsWorked = active.trustedRowsWorked,
                    endRow = active.trustedLastObservedRow,
                )
            sessionDao.deleteActiveSession(active.sessionToken)
            return completedSessionId
        }

        private suspend fun insertCompletedSession(
            active: ActiveSessionEntity,
            durationSeconds: Long,
            rowsWorked: Int,
            endRow: Int,
        ): Long? {
            val safeDuration = durationSeconds.coerceAtLeast(0L)
            val safeRows = rowsWorked.coerceAtLeast(0)
            if (safeDuration < 1L && safeRows == 0) return null
            val durationMinutes =
                saturatingAdd(safeDuration, 59L)
                    .div(60L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            val durationMillis =
                if (safeDuration > Long.MAX_VALUE / 1_000L) Long.MAX_VALUE else safeDuration * 1_000L
            val endedAt = saturatingAdd(active.startedAtWallMillis.coerceAtLeast(0L), durationMillis)
            return sessionDao.insert(
                SessionEntity(
                    projectId = active.projectId,
                    startedAt = active.startedAtWallMillis.coerceAtLeast(0L),
                    endedAt = endedAt,
                    startRow = active.startRow,
                    endRow = endRow,
                    durationMinutes = durationMinutes,
                    durationSeconds = safeDuration,
                    rowsWorked = safeRows,
                    zoneId = active.startZoneId,
                ),
            )
        }

        private suspend fun createStartedSession(
            projectId: Long,
            startRow: Int,
            now: com.finnvek.knittools.domain.model.SessionTimeSnapshot,
        ): StartSessionResult.Started {
            val session =
                ActiveSessionEntity(
                    sessionToken = UUID.randomUUID().toString(),
                    projectId = projectId,
                    startedAtWallMillis = now.wallClockMillis,
                    startZoneId = now.zoneId,
                    startRow = startRow,
                    lastObservedRow = startRow,
                    trustedLastObservedRow = startRow,
                    trustedRowsWorked = 0,
                    pendingRowsWorked = 0,
                    reviewedRowsWorked = 0,
                    reviewedLastObservedRow = startRow,
                    unreviewedRowsWorked = 0,
                    checkpointedDurationSeconds = 0L,
                    reviewedDurationBaselineSeconds = 0L,
                    segmentStartedAtWallMillis = now.wallClockMillis,
                    segmentStartedElapsedRealtimeMillis = now.elapsedRealtimeMillis,
                    bootCount = now.bootCount,
                    recoveryReason = null,
                    recoveryIntervalToken = null,
                    recoverySuggestedDurationSeconds = null,
                    recoveryPromptShown = false,
                    updatedAtWallMillis = now.wallClockMillis,
                )
            sessionDao.insertActiveSession(session)
            return StartSessionResult.Started((synchronizeActiveSession(now) ?: session).toDomain())
        }

        private fun ActiveSessionEntity.timingAnchors() =
            com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors(
                segmentStartedAtWallMillis = segmentStartedAtWallMillis,
                segmentStartedElapsedRealtimeMillis = segmentStartedElapsedRealtimeMillis,
                bootCount = bootCount,
                checkpointedDurationSeconds = checkpointedDurationSeconds,
                reviewedDurationBaselineSeconds = reviewedDurationBaselineSeconds,
            )

        private fun adjustRowsWorked(
            current: Int,
            delta: Long,
        ): Int =
            (current.toLong() + delta)
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()

        private fun positiveRowDelta(
            previous: Int,
            current: Int,
        ): Int =
            (current.toLong() - previous.toLong())
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()

        private fun saturatingRows(
            trusted: Int,
            pending: Int,
        ): Int =
            (trusted.toLong() + pending.toLong())
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()

        private fun canRepresentCompletedDuration(
            startedAtWallMillis: Long,
            durationSeconds: Long,
        ): Boolean {
            if (startedAtWallMillis < 0L || durationSeconds < 0L || durationSeconds > Long.MAX_VALUE / 1_000L) {
                return false
            }
            return startedAtWallMillis <= Long.MAX_VALUE - durationSeconds * 1_000L
        }

        private suspend fun <T> runSessionMutation(
            persistenceFailure: T,
            block: suspend () -> T,
        ): T =
            try {
                transactionRunner.run(block)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                persistenceFailure
            }

        private fun Flow<List<SessionEntity>>.toDomainSessions(): Flow<List<KnitSession>> =
            distinctUntilChanged()
                .map { sessions -> sessions.map { it.toDomain() } }
                .flowOn(ioDispatcher)
                .retryOnRepositoryReadFailure()
    }

internal fun mergeProjectNotes(
    baseNotes: String,
    requestedNotes: String,
    currentNotes: String,
): String {
    if (currentNotes == baseNotes || currentNotes == requestedNotes) return requestedNotes
    if (requestedNotes == baseNotes) return currentNotes
    if (currentNotes.containsMergedNoteBlock(requestedNotes)) return currentNotes
    if (requestedNotes.containsMergedNoteBlock(currentNotes)) return requestedNotes
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
        else -> "$primary$MERGED_NOTES_SEPARATOR$secondary"
    }

private fun String.containsMergedNoteBlock(block: String): Boolean =
    block.isNotEmpty() && split(MERGED_NOTES_SEPARATOR).any { it == block }

private const val MERGED_NOTES_SEPARATOR = "\n\n---\n\n"
