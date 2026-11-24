package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternAnnotationLayerDao {
    @Query("SELECT * FROM pattern_annotation_layers WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeProjectLayers(projectId: Long): Flow<List<PatternAnnotationLayerEntity>>

    @Query("SELECT * FROM pattern_annotation_layers WHERE savedPatternId = :savedPatternId ORDER BY createdAt ASC")
    fun observeSavedPatternLayers(savedPatternId: Long): Flow<List<PatternAnnotationLayerEntity>>

    @Query("SELECT * FROM pattern_annotation_layers WHERE projectId = :projectId AND documentKey = :documentKey")
    suspend fun getProjectLayer(
        projectId: Long,
        documentKey: String,
    ): PatternAnnotationLayerEntity?

    @Query(
        "SELECT * FROM pattern_annotation_layers WHERE savedPatternId = :savedPatternId AND documentKey = :documentKey",
    )
    suspend fun getSavedPatternLayer(
        savedPatternId: Long,
        documentKey: String,
    ): PatternAnnotationLayerEntity?

    @Query("SELECT * FROM pattern_annotation_layers WHERE projectId = :projectId AND isActive = 1 LIMIT 1")
    suspend fun getActiveProjectLayer(projectId: Long): PatternAnnotationLayerEntity?

    @Insert
    suspend fun insert(layer: PatternAnnotationLayerEntity): Long

    @Query(
        """
        DELETE FROM pattern_annotations WHERE layerId IN (
            SELECT id FROM pattern_annotation_layers WHERE projectId = :projectId AND documentKey = :documentKey
        )
        """,
    )
    suspend fun deleteProjectLayerAnnotations(
        projectId: Long,
        documentKey: String,
    )

    @Query("DELETE FROM pattern_annotation_layers WHERE projectId = :projectId AND documentKey = :documentKey")
    suspend fun deleteProjectLayer(
        projectId: Long,
        documentKey: String,
    )

    @Query("UPDATE pattern_annotation_layers SET isActive = 0, updatedAt = :updatedAt WHERE projectId = :projectId")
    suspend fun deactivateProjectLayers(
        projectId: Long,
        updatedAt: Long,
    )

    @Query("UPDATE pattern_annotation_layers SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :layerId")
    suspend fun setActive(
        layerId: Long,
        isActive: Boolean,
        updatedAt: Long,
    )
}
