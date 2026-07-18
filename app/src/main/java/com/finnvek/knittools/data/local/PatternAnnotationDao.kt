package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Insert
    suspend fun insert(annotation: PatternAnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreBatch(annotations: List<PatternAnnotationEntity>)

    @Update
    suspend fun update(annotation: PatternAnnotationEntity)

    @Query(
        """
        DELETE FROM pattern_annotations
        WHERE layerId IN (
            SELECT id FROM pattern_annotation_layers WHERE projectId = :projectId
        )
        """,
    )
    suspend fun deleteForProject(projectId: Long)

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
