package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATIONS
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationPageLimitException
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
                .retryOnRepositoryReadFailureIf(retryIf = { it !is PatternAnnotationPageLimitException })

        suspend fun insertAnnotation(annotation: PatternAnnotation): Long = dao.insert(annotation.toEntity())

        suspend fun getForLayers(layerIds: List<Long>): List<PatternAnnotation> =
            if (layerIds.isEmpty()) {
                emptyList()
            } else {
                getForLayersForExport(
                    layerIds,
                    PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
                    PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES,
                )
            }

        suspend fun getForLayersForExport(
            layerIds: List<Long>,
            maxAnnotations: Int,
            maxPayloadBytes: Long,
        ): List<PatternAnnotation> {
            require(maxAnnotations in 0 until Int.MAX_VALUE) {
                "Pattern annotation export limit must allow a bounded overflow probe"
            }
            require(maxPayloadBytes >= 0L) { "Pattern annotation export payload limit must be non-negative" }
            return if (layerIds.isEmpty()) {
                emptyList()
            } else {
                val entities = dao.getForLayersBounded(layerIds, maxAnnotations, maxPayloadBytes)
                orderForLayers(layerIds, entities)
            }
        }

        suspend fun updateAnnotation(annotation: PatternAnnotation) = dao.update(annotation.toEntity())

        suspend fun clearPage(
            layerId: Long,
            page: Int,
        ) = dao.deleteForPage(layerId, page)

        suspend fun deleteAnnotation(id: Long) = dao.deleteById(id)

        suspend fun restoreBatch(annotations: List<PatternAnnotation>) =
            dao.restoreBatch(annotations.map(PatternAnnotation::toEntity))

        suspend fun reorderAnnotation(
            id: Long,
            zIndex: Long,
            updatedAt: Long,
        ) = dao.updateZIndex(id, zIndex, updatedAt)

        private fun orderForLayers(
            layerIds: List<Long>,
            annotations: List<PatternAnnotationEntity>,
        ): List<PatternAnnotation> {
            val layerOrder = layerIds.withIndex().associate { (index, layerId) -> layerId to index }
            return annotations
                .mapNotNull { it.toDomain() }
                .sortedWith(
                    compareBy<PatternAnnotation> { layerOrder[it.layerId] ?: Int.MAX_VALUE }
                        .thenBy(PatternAnnotation::page)
                        .thenBy(PatternAnnotation::zIndex)
                        .thenBy(PatternAnnotation::id),
                )
        }
    }
