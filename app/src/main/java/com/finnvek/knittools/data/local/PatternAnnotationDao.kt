package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternAnnotationDao {
    @Query(
        """
        SELECT * FROM pattern_annotations
        WHERE projectId = :projectId AND page = :page
        ORDER BY createdAt ASC
        """,
    )
    fun getAnnotationsForPage(
        projectId: Long,
        page: Int,
    ): Flow<List<PatternAnnotationEntity>>

    @Insert
    suspend fun insert(annotation: PatternAnnotationEntity): Long

    @Query("DELETE FROM pattern_annotations WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)

    @Query("DELETE FROM pattern_annotations WHERE projectId = :projectId AND page = :page")
    suspend fun deleteForPage(
        projectId: Long,
        page: Int,
    )

    @Query("DELETE FROM pattern_annotations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
