package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternAnnotationDao {
    @Query(
        """
        SELECT * FROM pattern_annotations
        WHERE layerId = :layerId AND page = :page
        ORDER BY zIndex ASC, id ASC
        """,
    )
    fun observePage(
        layerId: Long,
        page: Int,
    ): Flow<List<PatternAnnotationEntity>>

    @Query(
        """
        SELECT * FROM pattern_annotations
        WHERE layerId IN (:layerIds)
        ORDER BY page ASC, zIndex ASC, id ASC
        """,
    )
    suspend fun getForLayers(layerIds: List<Long>): List<PatternAnnotationEntity>

    @Query(
        """
        SELECT * FROM pattern_annotations
        WHERE layerId IN (:layerIds)
        ORDER BY page ASC, zIndex ASC, id ASC
        LIMIT :limit
        """,
    )
    suspend fun getForLayersLimited(
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
    suspend fun getExportStats(layerIds: List<Long>): PatternAnnotationExportStats

    @Transaction
    suspend fun getForLayersBounded(
        layerIds: List<Long>,
        maxAnnotations: Int,
        maxPayloadBytes: Long,
    ): List<PatternAnnotationEntity> {
        require(maxAnnotations in 0 until Int.MAX_VALUE)
        require(maxPayloadBytes >= 0L)
        val stats = getExportStats(layerIds)
        check(stats.annotationCount <= maxAnnotations.toLong()) { "Pattern annotation export limit exceeded" }
        check(stats.payloadBytes <= maxPayloadBytes) { "Pattern annotation export payload limit exceeded" }
        val annotations = getForLayersLimited(layerIds, maxAnnotations + 1)
        check(annotations.size <= maxAnnotations) { "Pattern annotation export limit exceeded" }
        return annotations
    }

    @Insert
    suspend fun insert(annotation: PatternAnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreBatch(annotations: List<PatternAnnotationEntity>)

    @Update
    suspend fun update(annotation: PatternAnnotationEntity)

    @Query("DELETE FROM pattern_annotations WHERE layerId = :layerId AND page = :page")
    suspend fun deleteForPage(
        layerId: Long,
        page: Int,
    )

    @Query("DELETE FROM pattern_annotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pattern_annotations SET zIndex = :zIndex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateZIndex(
        id: Long,
        zIndex: Long,
        updatedAt: Long,
    )
}

data class PatternAnnotationExportStats(
    val annotationCount: Long,
    val payloadBytes: Long,
)
