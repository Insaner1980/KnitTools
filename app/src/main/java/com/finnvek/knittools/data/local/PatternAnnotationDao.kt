package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATIONS
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES
import com.finnvek.knittools.domain.model.PatternAnnotationPageBudget
import com.finnvek.knittools.domain.model.PatternAnnotationPageLimitException
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class PatternAnnotationDao {
    suspend fun getForLayers(layerIds: List<Long>): List<PatternAnnotationEntity> =
        getForLayersBounded(
            layerIds,
            PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
            PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES,
        )

    @Query("SELECT EXISTS(SELECT 1 FROM pattern_annotations WHERE layerId = :layerId AND page = :page)")
    protected abstract fun observePageChanges(
        layerId: Long,
        page: Int,
    ): Flow<Boolean>

    fun observePage(
        layerId: Long,
        page: Int,
    ): Flow<List<PatternAnnotationEntity>> = observePageChanges(layerId, page).map { getPageBounded(layerId, page) }

    @Query(
        """
        SELECT COUNT(*) AS annotationCount,
            COALESCE(SUM(LENGTH(CAST(payloadJson AS BLOB))), 0) AS payloadBytes
        FROM (SELECT payloadJson FROM pattern_annotations WHERE layerId = :layerId AND page = :page LIMIT 257)
        """,
    )
    protected abstract suspend fun getPageStats(
        layerId: Long,
        page: Int,
    ): PatternAnnotationExportStats

    @Query(
        """
        SELECT * FROM pattern_annotations
        WHERE layerId = :layerId AND page = :page
        ORDER BY zIndex ASC, id ASC
        LIMIT 257
        """,
    )
    protected abstract suspend fun getPageRows(
        layerId: Long,
        page: Int,
    ): List<PatternAnnotationEntity>

    @Transaction
    open suspend fun getPageBounded(
        layerId: Long,
        page: Int,
    ): List<PatternAnnotationEntity> {
        val stats = getPageStats(layerId, page)
        if (stats.annotationCount > PatternAnnotationPageBudget.MAX_ANNOTATIONS ||
            stats.payloadBytes > PatternAnnotationPageBudget.MAX_PAYLOAD_BYTES
        ) {
            throw PatternAnnotationPageLimitException()
        }
        return getPageRows(layerId, page).also(::validatePages)
    }

    @Query(
        """
        SELECT * FROM pattern_annotations
        WHERE layerId IN (:layerIds)
        ORDER BY page ASC, zIndex ASC, id ASC
        LIMIT :limit
        """,
    )
    protected abstract suspend fun getForLayersLimited(
        layerIds: List<Long>,
        limit: Int,
    ): List<PatternAnnotationEntity>

    @Query(
        """
        SELECT
            COUNT(*) AS annotationCount,
            COALESCE(SUM(LENGTH(CAST(payloadJson AS BLOB))), 0) AS payloadBytes
        FROM pattern_annotations
        WHERE layerId IN (:layerIds)
        """,
    )
    protected abstract suspend fun getExportStats(layerIds: List<Long>): PatternAnnotationExportStats

    @Transaction
    open suspend fun getForLayersBounded(
        layerIds: List<Long>,
        maxAnnotations: Int,
        maxPayloadBytes: Long,
    ): List<PatternAnnotationEntity> {
        require(maxAnnotations in 0..PATTERN_PDF_EXPORT_MAX_ANNOTATIONS)
        require(maxPayloadBytes in 0L..PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES)
        val stats = getExportStats(layerIds)
        check(stats.annotationCount <= maxAnnotations.toLong()) { "Pattern annotation export limit exceeded" }
        check(stats.payloadBytes <= maxPayloadBytes) { "Pattern annotation export payload limit exceeded" }
        val annotations = getForLayersLimited(layerIds, maxAnnotations + 1)
        check(annotations.size <= maxAnnotations) { "Pattern annotation export limit exceeded" }
        validatePages(annotations)
        return annotations
    }

    @Insert
    protected abstract suspend fun insertUnchecked(annotation: PatternAnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun restoreUnchecked(annotations: List<PatternAnnotationEntity>)

    @Update
    protected abstract suspend fun updateUnchecked(annotation: PatternAnnotationEntity)

    @Transaction
    open suspend fun insert(annotation: PatternAnnotationEntity): Long {
        val id = insertUnchecked(annotation)
        getPageBounded(annotation.layerId, annotation.page)
        return id
    }

    @Transaction
    open suspend fun update(annotation: PatternAnnotationEntity) {
        updateUnchecked(annotation)
        getPageBounded(annotation.layerId, annotation.page)
    }

    @Transaction
    open suspend fun restoreBatch(annotations: List<PatternAnnotationEntity>) {
        restoreUnchecked(annotations)
        // Tarkistetaan REPLACE-erän lopputila myös toistuvilla ID-arvoilla ja sivun tai tason vaihtuessa.
        annotations.map { it.layerId to it.page }.distinct().forEach { (layerId, page) ->
            getPageBounded(layerId, page)
        }
    }

    @Query("DELETE FROM pattern_annotations WHERE layerId = :layerId AND page = :page")
    abstract suspend fun deleteForPage(
        layerId: Long,
        page: Int,
    )

    @Query("DELETE FROM pattern_annotations WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("UPDATE pattern_annotations SET zIndex = :zIndex, updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun updateZIndex(
        id: Long,
        zIndex: Long,
        updatedAt: Long,
    )

    private fun validatePages(annotations: List<PatternAnnotationEntity>) {
        val budgets = mutableMapOf<Pair<Long, Int>, PatternAnnotationPageBudget>()
        for (entity in annotations) {
            if (entity.payloadJson.length > PatternAnnotationPayloadCodec.MAX_PAYLOAD_BYTES) {
                throw PatternAnnotationPageLimitException()
            }
            val annotation = entity.toDomain() ?: throw PatternAnnotationPageLimitException()
            budgets
                .getOrPut(entity.layerId to entity.page) { PatternAnnotationPageBudget() }
                .add(
                    annotation.payload,
                    entity.payloadJson
                        .encodeToByteArray()
                        .size
                        .toLong(),
                )
        }
    }
}

data class PatternAnnotationExportStats(
    val annotationCount: Long,
    val payloadBytes: Long,
)
