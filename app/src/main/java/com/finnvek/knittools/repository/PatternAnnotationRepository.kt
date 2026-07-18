package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.PatternAnnotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatternAnnotationRepository
    @Inject
    constructor(
        private val dao: PatternAnnotationDao,
    ) {
        fun observePage(
            layerId: Long,
            page: Int,
        ): Flow<List<PatternAnnotation>> =
            dao
                .observePage(layerId, page)
                .map { annotations -> annotations.mapNotNull { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        suspend fun insertAnnotation(annotation: PatternAnnotation): Long = dao.insert(annotation.toEntity())

        suspend fun updateAnnotation(annotation: PatternAnnotation) = dao.update(annotation.toEntity())

        suspend fun clearProject(projectId: Long) = dao.deleteForProject(projectId)

        suspend fun clearPage(
            layerId: Long,
            page: Int,
        ) = dao.deleteForPage(layerId, page)

        suspend fun deleteById(id: Long) = dao.deleteById(id)

        suspend fun restoreBatch(annotations: List<PatternAnnotation>) =
            dao.restoreBatch(annotations.map(PatternAnnotation::toEntity))

        suspend fun reorderAnnotation(
            id: Long,
            zIndex: Long,
            updatedAt: Long,
        ) = dao.updateZIndex(id, zIndex, updatedAt)
    }
