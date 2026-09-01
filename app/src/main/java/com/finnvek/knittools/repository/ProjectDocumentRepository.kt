package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.DEFAULT_READING_GUIDE_FRACTION
import com.finnvek.knittools.domain.model.DEFAULT_READING_LINE_Y_FRACTION
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectDocumentLabelValidation
import com.finnvek.knittools.domain.model.inDocumentOrder
import com.finnvek.knittools.domain.model.primaryFallback
import com.finnvek.knittools.domain.model.sanitizeReadingGuideFraction
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.domain.model.validateProjectDocumentLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ProjectDocumentMutationResult {
    data class Added(
        val document: ProjectDocument,
    ) : ProjectDocumentMutationResult

    data object AlreadyAttached : ProjectDocumentMutationResult

    data object DuplicateUri : ProjectDocumentMutationResult

    data object DuplicateDocumentKey : ProjectDocumentMutationResult

    data object MissingProject : ProjectDocumentMutationResult

    data object MissingDocument : ProjectDocumentMutationResult

    data object MissingSavedPattern : ProjectDocumentMutationResult

    data object MetadataOnlyPattern : ProjectDocumentMutationResult

    data object PdfUnavailable : ProjectDocumentMutationResult

    data object InvalidLabel : ProjectDocumentMutationResult

    data object StaleAction : ProjectDocumentMutationResult

    data object PrimaryChanged : ProjectDocumentMutationResult

    data object Reordered : ProjectDocumentMutationResult

    data object Renamed : ProjectDocumentMutationResult

    data object Selected : ProjectDocumentMutationResult

    data class Removed(
        val document: ProjectDocument,
        val newPrimary: ProjectDocument?,
    ) : ProjectDocumentMutationResult

    data object ViewerStateUpdated : ProjectDocumentMutationResult

    data object PersistenceFailure : ProjectDocumentMutationResult
}

@Singleton
class ProjectDocumentRepository
    @Inject
    constructor(
        private val documentDao: ProjectDocumentDao,
        private val projectDao: CounterProjectDao,
        private val savedPatternRepository: SavedPatternRepository,
        private val layerRepository: PatternAnnotationLayerRepository,
        private val transactionRunner: DatabaseTransactionRunner,
        private val fileAvailability: ProjectDocumentFileAvailability,
    ) {
        private val removal = Removal()

        fun observeDocuments(projectId: Long): Flow<List<ProjectDocument>> =
            documentDao
                .observeForProject(projectId)
                .map { rows -> rows.map(ProjectDocumentEntity::toDomain).inDocumentOrder() }
                .retryOnRepositoryReadFailure()

        fun observeActiveDocument(projectId: Long): Flow<ProjectDocument?> =
            combine(
                observeDocuments(projectId),
                layerRepository.observeLayers(
                    PatternAnnotationOwner.Project(projectId, PatternAnnotationDocumentKey.legacyProject(projectId)),
                ),
            ) { documents, layers ->
                val activeKey = layers.singleOrNull { it.isActive }?.owner?.documentKey
                documents.firstOrNull { it.documentKey == activeKey }
                    ?: documents.firstOrNull(ProjectDocument::isPrimary)
            }

        fun observeDocuments(projectIds: List<Long>): Flow<Map<Long, List<ProjectDocument>>> {
            if (projectIds.isEmpty()) return flowOf(emptyMap())
            return documentDao
                .observeForProjects(projectIds.distinct())
                .map { rows ->
                    rows
                        .map(ProjectDocumentEntity::toDomain)
                        .groupBy(ProjectDocument::projectId)
                        .mapValues { (_, documents) -> documents.inDocumentOrder() }
                }.retryOnRepositoryReadFailure()
        }

        suspend fun getDocuments(projectId: Long): List<ProjectDocument> =
            documentDao.getForProject(projectId).map(ProjectDocumentEntity::toDomain).inDocumentOrder()

        suspend fun getDocuments(projectIds: List<Long>): Map<Long, List<ProjectDocument>> {
            if (projectIds.isEmpty()) return emptyMap()
            return documentDao
                .getForProjects(projectIds.distinct())
                .map(ProjectDocumentEntity::toDomain)
                .groupBy(ProjectDocument::projectId)
                .mapValues { (_, documents) -> documents.inDocumentOrder() }
        }

        suspend fun getDocument(documentId: Long): ProjectDocument? = documentDao.getById(documentId)?.toDomain()

        suspend fun getPrimary(projectId: Long): ProjectDocument? = documentDao.getPrimary(projectId)?.toDomain()

        suspend fun getDistinctUris(projectId: Long): List<String> = documentDao.getDistinctUris(projectId)

        suspend fun getActiveDocument(projectId: Long): ProjectDocument? {
            val activeKey = layerRepository.getActiveProjectLayer(projectId)?.owner?.documentKey
            return activeKey
                ?.let { documentDao.getByDocumentKey(projectId, it)?.toDomain() }
                ?: getPrimary(projectId)
        }

        suspend fun addSavedPattern(
            projectId: Long,
            savedPatternId: Long,
        ): ProjectDocumentMutationResult {
            if (documentDao.getBySavedPatternId(projectId, savedPatternId) != null) {
                return ProjectDocumentMutationResult.AlreadyAttached
            }
            val pattern =
                savedPatternRepository.getById(savedPatternId)
                    ?: return ProjectDocumentMutationResult.MissingSavedPattern
            val localPdfUri = pattern.localPdfUri?.trim().orEmpty()
            if (localPdfUri.isEmpty()) return ProjectDocumentMutationResult.MetadataOnlyPattern
            if (!fileAvailability.isAvailable(localPdfUri)) return ProjectDocumentMutationResult.PdfUnavailable
            return addDocument(
                projectId = projectId,
                savedPatternId = savedPatternId,
                localPdfUri = localPdfUri,
                label = pattern.name,
                documentKey = PatternAnnotationDocumentKey.savedPattern(savedPatternId),
            )
        }

        suspend fun addImportedPdf(
            projectId: Long,
            localPdfUri: String,
            label: String,
            documentKey: String? = null,
        ): ProjectDocumentMutationResult {
            val validatedLabel = validateProjectDocumentLabel(label)
            val normalizedLabel =
                (validatedLabel as? ProjectDocumentLabelValidation.Valid)?.label
                    ?: return ProjectDocumentMutationResult.InvalidLabel
            val normalizedUri = localPdfUri.trim()
            val normalizedRequestedKey = documentKey?.trim()
            val rejection =
                when {
                    normalizedUri.isEmpty() || !fileAvailability.isAvailable(normalizedUri) ->
                        ProjectDocumentMutationResult.PdfUnavailable
                    projectDao.getProject(projectId) == null -> ProjectDocumentMutationResult.MissingProject
                    documentDao.getByUri(projectId, normalizedUri) != null -> ProjectDocumentMutationResult.DuplicateUri
                    !normalizedRequestedKey.isNullOrEmpty() &&
                        documentDao.getByDocumentKey(projectId, normalizedRequestedKey) != null ->
                        ProjectDocumentMutationResult.DuplicateDocumentKey
                    else -> null
                }
            if (rejection != null) return rejection
            val savedPatternId =
                savedPatternRepository.saveImportedPatternIfMissing(normalizedUri, normalizedLabel)
                    ?: return ProjectDocumentMutationResult.PersistenceFailure
            return addDocument(
                projectId = projectId,
                savedPatternId = savedPatternId,
                localPdfUri = normalizedUri,
                label = normalizedLabel,
                documentKey = documentKey ?: PatternAnnotationDocumentKey.savedPattern(savedPatternId),
            )
        }

        suspend fun rename(
            projectId: Long,
            documentId: Long,
            label: String,
        ): ProjectDocumentMutationResult {
            val validated = validateProjectDocumentLabel(label)
            if (validated !is ProjectDocumentLabelValidation.Valid) {
                return ProjectDocumentMutationResult.InvalidLabel
            }
            return mutation {
                val document =
                    ownedDocument(projectId, documentId)
                        ?: return@mutation ProjectDocumentMutationResult.MissingDocument
                val changed =
                    documentDao.updateLabel(
                        document.id,
                        projectId,
                        validated.label,
                        System.currentTimeMillis(),
                    )
                if (changed == 1) ProjectDocumentMutationResult.Renamed else ProjectDocumentMutationResult.StaleAction
            }
        }

        suspend fun moveEarlier(
            projectId: Long,
            documentId: Long,
        ): ProjectDocumentMutationResult = reorder(projectId, documentId, -1)

        suspend fun moveLater(
            projectId: Long,
            documentId: Long,
        ): ProjectDocumentMutationResult = reorder(projectId, documentId, 1)

        suspend fun setPrimary(
            projectId: Long,
            documentId: Long,
        ): ProjectDocumentMutationResult =
            mutation {
                transactionRunner.run {
                    val target =
                        ownedDocument(projectId, documentId)
                            ?: return@run ProjectDocumentMutationResult.MissingDocument
                    if (target.isPrimary) return@run ProjectDocumentMutationResult.PrimaryChanged
                    val now = System.currentTimeMillis()
                    if (documentDao.clearPrimary(projectId, now) != 1) {
                        abortMutation(ProjectDocumentMutationResult.StaleAction)
                    }
                    if (documentDao.setPrimary(documentId, projectId, now) != 1) {
                        abortMutation(ProjectDocumentMutationResult.StaleAction)
                    }
                    ProjectDocumentMutationResult.PrimaryChanged
                }
            }

        suspend fun select(
            projectId: Long,
            documentId: Long,
        ): ProjectDocumentMutationResult =
            mutation {
                val candidate =
                    ownedDocument(projectId, documentId)
                        ?: return@mutation ProjectDocumentMutationResult.MissingDocument
                if (!fileAvailability.isAvailable(candidate.localPdfUri)) {
                    return@mutation ProjectDocumentMutationResult.PdfUnavailable
                }
                transactionRunner.run {
                    val target =
                        ownedDocument(projectId, documentId)
                            ?: return@run ProjectDocumentMutationResult.MissingDocument
                    layerRepository.activateProjectLayerInTransaction(projectId, target.documentKey)
                    ProjectDocumentMutationResult.Selected
                }
            }

        suspend fun remove(
            projectId: Long,
            documentId: Long,
        ): ProjectDocumentMutationResult {
            val removedResult =
                mutation {
                    transactionRunner.run {
                        removal.run(projectId, documentId)
                    }
                }
            if (removedResult is ProjectDocumentMutationResult.Removed) {
                try {
                    savedPatternRepository.deleteLocalPatternFileIfUnused(removedResult.document.localPdfUri)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // Tietokantatila on jo oikein; mahdollinen orpotiedosto voidaan siivota myöhemmin.
                }
            }
            return removedResult
        }

        @Suppress("kotlin:S107") // Repository välittää yhden dokumenttitilan kentät atomisesti DAO:lle.
        suspend fun updateViewerState(
            projectId: Long,
            documentId: Long,
            currentPage: Int,
            rowMapping: String?,
            readingLineEnabled: Boolean,
            readingLineYFraction: Float,
            readingLineFollowCurrentRow: Boolean,
            verticalReadingGuideEnabled: Boolean,
            verticalReadingGuideXFraction: Float,
        ): ProjectDocumentMutationResult =
            mutation {
                val changed =
                    documentDao.updateViewerState(
                        documentId = documentId,
                        projectId = projectId,
                        currentPage = currentPage.coerceAtLeast(0),
                        rowMapping = rowMapping,
                        readingLineEnabled = readingLineEnabled,
                        readingLineYFraction = sanitizeReadingLineYFraction(readingLineYFraction),
                        readingLineFollowCurrentRow = readingLineFollowCurrentRow,
                        verticalReadingGuideEnabled = verticalReadingGuideEnabled,
                        verticalReadingGuideXFraction = sanitizeReadingGuideFraction(verticalReadingGuideXFraction),
                        updatedAt = System.currentTimeMillis(),
                    )
                if (changed == 1) {
                    ProjectDocumentMutationResult.ViewerStateUpdated
                } else {
                    ProjectDocumentMutationResult.MissingDocument
                }
            }

        suspend fun isAvailable(document: ProjectDocument): Boolean = fileAvailability.isAvailable(document.localPdfUri)

        suspend fun updateViewerStateInTransaction(document: ProjectDocument): Boolean =
            documentDao.updateViewerState(
                documentId = document.id,
                projectId = document.projectId,
                currentPage = document.currentPage.coerceAtLeast(0),
                rowMapping = document.rowMapping,
                readingLineEnabled = document.readingLineEnabled,
                readingLineYFraction = sanitizeReadingLineYFraction(document.readingLineYFraction),
                readingLineFollowCurrentRow = document.readingLineFollowCurrentRow,
                verticalReadingGuideEnabled = document.verticalReadingGuideEnabled,
                verticalReadingGuideXFraction = sanitizeReadingGuideFraction(document.verticalReadingGuideXFraction),
                updatedAt = System.currentTimeMillis(),
            ) == 1

        @Suppress("kotlin:S3776") // Lisäys kirjoittaa primääridokumentin invariantit yhdessä.
        private suspend fun addDocument(
            projectId: Long,
            savedPatternId: Long?,
            localPdfUri: String,
            label: String,
            documentKey: String,
        ): ProjectDocumentMutationResult {
            val validated = validateProjectDocumentLabel(label)
            val normalizedUri = localPdfUri.trim()
            val normalizedKey = documentKey.trim()
            if (validated !is ProjectDocumentLabelValidation.Valid ||
                normalizedUri.isEmpty() ||
                normalizedKey.isEmpty()
            ) {
                return ProjectDocumentMutationResult.InvalidLabel
            }
            return mutation {
                transactionRunner.run {
                    if (projectDao.getProject(projectId) == null) {
                        return@run ProjectDocumentMutationResult.MissingProject
                    }
                    if (savedPatternId != null && documentDao.getBySavedPatternId(projectId, savedPatternId) != null) {
                        return@run ProjectDocumentMutationResult.AlreadyAttached
                    }
                    if (documentDao.getByUri(projectId, normalizedUri) != null) {
                        return@run ProjectDocumentMutationResult.DuplicateUri
                    }
                    if (documentDao.getByDocumentKey(projectId, normalizedKey) != null) {
                        return@run ProjectDocumentMutationResult.DuplicateDocumentKey
                    }
                    val existing =
                        documentDao
                            .getForProject(
                                projectId,
                            ).map(ProjectDocumentEntity::toDomain)
                            .inDocumentOrder()
                    val nextSortOrder = documentDao.getHighestSortOrder(projectId) + 1
                    val now = System.currentTimeMillis()
                    val entity =
                        ProjectDocumentEntity(
                            projectId = projectId,
                            savedPatternId = savedPatternId,
                            documentKey = normalizedKey,
                            label = validated.label,
                            localPdfUri = normalizedUri,
                            sortOrder = nextSortOrder.coerceAtLeast(0),
                            isPrimary = existing.isEmpty(),
                            currentPage = 0,
                            rowMapping = null,
                            readingLineEnabled = false,
                            readingLineYFraction = DEFAULT_READING_LINE_Y_FRACTION,
                            readingLineFollowCurrentRow = true,
                            verticalReadingGuideEnabled = false,
                            verticalReadingGuideXFraction = DEFAULT_READING_GUIDE_FRACTION,
                            createdAt = now,
                            updatedAt = now,
                        )
                    val id = documentDao.insert(entity)
                    if (existing.isEmpty()) {
                        layerRepository.activateProjectLayerInTransaction(projectId, normalizedKey)
                    }
                    ProjectDocumentMutationResult.Added(entity.copy(id = id).toDomain())
                }
            }
        }

        private suspend fun reorder(
            projectId: Long,
            documentId: Long,
            direction: Int,
        ): ProjectDocumentMutationResult =
            mutation {
                transactionRunner.run {
                    val ordered =
                        documentDao.getForProject(projectId).map(ProjectDocumentEntity::toDomain).inDocumentOrder()
                    val currentIndex = ordered.indexOfFirst { it.id == documentId }
                    if (currentIndex < 0) return@run ProjectDocumentMutationResult.MissingDocument
                    val targetIndex = currentIndex + direction
                    if (targetIndex !in ordered.indices) return@run ProjectDocumentMutationResult.StaleAction
                    val reordered = ordered.toMutableList()
                    val current = reordered.removeAt(currentIndex)
                    reordered.add(targetIndex, current)
                    val now = System.currentTimeMillis()
                    reordered.forEachIndexed { index, document ->
                        if (document.sortOrder != index &&
                            documentDao.updateSortOrder(document.id, projectId, index, now) != 1
                        ) {
                            abortMutation(ProjectDocumentMutationResult.StaleAction)
                        }
                    }
                    ProjectDocumentMutationResult.Reordered
                }
            }

        private suspend fun ownedDocument(
            projectId: Long,
            documentId: Long,
        ): ProjectDocument? = documentDao.getById(documentId)?.takeIf { it.projectId == projectId }?.toDomain()

        private suspend fun mutation(
            block: suspend () -> ProjectDocumentMutationResult,
        ): ProjectDocumentMutationResult =
            try {
                block()
            } catch (aborted: MutationAborted) {
                aborted.result
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ProjectDocumentMutationResult.PersistenceFailure
            }

        private fun abortMutation(result: ProjectDocumentMutationResult): Nothing = throw MutationAborted(result)

        private inner class Removal {
            suspend fun run(
                projectId: Long,
                documentId: Long,
            ): ProjectDocumentMutationResult {
                val target =
                    ownedDocument(projectId, documentId)
                        ?: return ProjectDocumentMutationResult.MissingDocument
                val remaining =
                    documentDao
                        .getForProject(projectId)
                        .filterNot { it.id == documentId }
                        .map(ProjectDocumentEntity::toDomain)
                        .inDocumentOrder()
                val fallback = remaining.primaryFallback()
                val now = System.currentTimeMillis()
                updatePrimary(target, fallback, now)
                updateActiveLayer(target, fallback)
                if (documentDao.delete(documentId, projectId) != 1) {
                    abortMutation(ProjectDocumentMutationResult.StaleAction)
                }
                normalizeOrder(projectId, remaining, now)
                val resultingPrimary =
                    if (target.isPrimary) fallback else remaining.firstOrNull(ProjectDocument::isPrimary)
                return ProjectDocumentMutationResult.Removed(target, resultingPrimary?.copy(isPrimary = true))
            }

            private suspend fun updatePrimary(
                target: ProjectDocument,
                fallback: ProjectDocument?,
                updatedAt: Long,
            ) {
                if (!target.isPrimary) return
                if (documentDao.clearPrimary(target.projectId, updatedAt) != 1) {
                    abortMutation(ProjectDocumentMutationResult.StaleAction)
                }
                if (fallback != null && documentDao.setPrimary(fallback.id, target.projectId, updatedAt) != 1) {
                    abortMutation(ProjectDocumentMutationResult.StaleAction)
                }
            }

            private suspend fun updateActiveLayer(
                target: ProjectDocument,
                fallback: ProjectDocument?,
            ) {
                val activeKey = layerRepository.getActiveProjectLayer(target.projectId)?.owner?.documentKey
                if (activeKey != target.documentKey) return
                if (fallback == null) {
                    layerRepository.deactivateProjectLayersInTransaction(target.projectId)
                } else {
                    layerRepository.activateProjectLayerInTransaction(target.projectId, fallback.documentKey)
                }
            }

            private suspend fun normalizeOrder(
                projectId: Long,
                documents: List<ProjectDocument>,
                updatedAt: Long,
            ) {
                documents.forEachIndexed { index, document ->
                    if (document.sortOrder != index &&
                        documentDao.updateSortOrder(document.id, projectId, index, updatedAt) != 1
                    ) {
                        abortMutation(ProjectDocumentMutationResult.StaleAction)
                    }
                }
            }
        }

        private class MutationAborted(
            val result: ProjectDocumentMutationResult,
        ) : RuntimeException()
    }
