package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.PatternAnnotationLayerDao
import com.finnvek.knittools.data.local.PatternAnnotationLayerEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatternAnnotationLayerRepository
    @Inject
    constructor(
        private val dao: PatternAnnotationLayerDao,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        fun observeLayers(owner: PatternAnnotationOwner): Flow<List<PatternAnnotationLayer>> {
            val entities =
                when (owner) {
                    is PatternAnnotationOwner.Project -> dao.observeProjectLayers(owner.projectId)
                    is PatternAnnotationOwner.SavedPattern -> dao.observeSavedPatternLayers(owner.savedPatternId)
                }
            return entities
                .map { layers -> layers.mapNotNull(PatternAnnotationLayerEntity::toDomain) }
                .retryOnRepositoryReadFailure()
        }

        suspend fun getOrCreateMasterLayer(
            savedPatternId: Long,
            documentKey: String,
        ): PatternAnnotationLayer =
            transactionRunner.run {
                require(savedPatternId > 0L) { "Saved pattern id must be positive" }
                require(documentKey.isNotBlank()) { "Document key must not be blank" }
                dao.getSavedPatternLayer(savedPatternId, documentKey)?.toDomain()
                    ?: createMasterLayer(savedPatternId, documentKey)
            }

        suspend fun activateProjectLayer(
            projectId: Long,
            documentKey: String,
        ): PatternAnnotationLayer =
            transactionRunner.run {
                activateProjectLayerInTransaction(projectId, documentKey)
            }

        suspend fun activateProjectLayerInTransaction(
            projectId: Long,
            documentKey: String,
        ): PatternAnnotationLayer {
            require(projectId > 0L) { "Project id must be positive" }
            require(documentKey.isNotBlank()) { "Document key must not be blank" }
            val updatedAt = System.currentTimeMillis()
            dao.deactivateProjectLayers(projectId, updatedAt)
            val existing = dao.getProjectLayer(projectId, documentKey)
            if (existing != null) {
                dao.setActive(existing.id, true, updatedAt)
                return checkNotNull(existing.copy(isActive = true, updatedAt = updatedAt).toDomain())
            }

            val entity =
                PatternAnnotationLayerEntity(
                    projectId = projectId,
                    savedPatternId = null,
                    documentKey = documentKey,
                    isActive = true,
                    createdAt = updatedAt,
                    updatedAt = updatedAt,
                )
            val id = dao.insert(entity)
            return checkNotNull(entity.copy(id = id).toDomain())
        }

        suspend fun deactivateProjectLayers(projectId: Long) {
            transactionRunner.run {
                deactivateProjectLayersInTransaction(projectId)
            }
        }

        suspend fun deactivateProjectLayersInTransaction(projectId: Long) {
            require(projectId > 0L) { "Project id must be positive" }
            dao.deactivateProjectLayers(projectId, System.currentTimeMillis())
        }

        suspend fun getActiveProjectLayer(projectId: Long): PatternAnnotationLayer? =
            dao.getActiveProjectLayer(projectId)?.toDomain()

        private suspend fun createMasterLayer(
            savedPatternId: Long,
            documentKey: String,
        ): PatternAnnotationLayer {
            val now = System.currentTimeMillis()
            val entity =
                PatternAnnotationLayerEntity(
                    projectId = null,
                    savedPatternId = savedPatternId,
                    documentKey = documentKey,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                )
            val id = dao.insert(entity)
            return checkNotNull(entity.copy(id = id).toDomain())
        }
    }
