package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectYarnNoteDao {
    @Query(
        """
        SELECT note.id, note.projectId, note.name, note.description, note.quantity, note.notes,
               card.id AS savedYarnCardId, note.createdAt, note.updatedAt
        FROM project_yarn_notes AS note
        LEFT JOIN yarn_cards AS card ON card.id = note.savedYarnCardId
        WHERE note.projectId = :projectId
        ORDER BY note.createdAt DESC, note.id DESC
        """,
    )
    fun observeForProject(projectId: Long): Flow<List<ProjectYarnNoteEntity>>

    @Query("SELECT * FROM project_yarn_notes WHERE id = :id")
    suspend fun getById(id: Long): ProjectYarnNoteEntity?

    @Upsert
    suspend fun upsert(note: ProjectYarnNoteEntity): Long

    @Update
    suspend fun update(note: ProjectYarnNoteEntity): Int

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
