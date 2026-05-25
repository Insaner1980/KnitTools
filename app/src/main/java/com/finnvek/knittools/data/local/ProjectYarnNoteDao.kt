package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectYarnNoteDao {
    @Query(
        """
        SELECT * FROM project_yarn_notes
        WHERE projectId = :projectId
        ORDER BY createdAt DESC, id DESC
        """,
    )
    fun observeForProject(projectId: Long): Flow<List<ProjectYarnNoteEntity>>

    @Query("SELECT * FROM project_yarn_notes WHERE id = :id")
    suspend fun getById(id: Long): ProjectYarnNoteEntity?

    @Upsert
    suspend fun upsert(note: ProjectYarnNoteEntity): Long

    @Query(
        """
        UPDATE project_yarn_notes
        SET savedYarnCardId = :savedYarnCardId,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSavedYarnCardId(
        id: Long,
        savedYarnCardId: Long,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM project_yarn_notes WHERE id = :id")
    suspend fun delete(id: Long)
}
