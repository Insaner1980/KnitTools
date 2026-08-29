package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectYarnUsageDao {
    @Transaction
    @Query("SELECT id FROM counter_projects WHERE id = :projectId")
    fun observeProject(projectId: Long): Flow<ProjectYarnUsageRelations?>

    @Transaction
    @Query("SELECT id FROM counter_projects WHERE id = :projectId")
    suspend fun getProject(projectId: Long): ProjectYarnUsageRelations?

    @Query("SELECT * FROM project_yarn_usage WHERE id = :id")
    suspend fun getById(id: Long): ProjectYarnUsageEntity?

    @Query(
        "SELECT * FROM project_yarn_usage WHERE projectId = :projectId AND (yarnCardId = :cardId OR projectYarnNoteId = :noteId)",
    )
    suspend fun getForSource(
        projectId: Long,
        cardId: Long?,
        noteId: Long?,
    ): List<ProjectYarnUsageEntity>

    @Insert
    suspend fun insert(usage: ProjectYarnUsageEntity): Long

    @Update
    suspend fun update(usage: ProjectYarnUsageEntity): Int

    @Query("DELETE FROM project_yarn_usage WHERE id = :id AND projectId = :projectId")
    suspend fun delete(
        id: Long,
        projectId: Long,
    ): Int

    suspend fun linkSavedCard(
        projectId: Long,
        noteId: Long,
        cardId: Long,
    ) {
        val rows = getForSource(projectId, cardId, noteId)
        check(rows.size <= 1) { "Conflicting yarn usage identities" }
        rows.singleOrNull()?.let { row ->
            check(update(row.copy(yarnCardId = cardId, projectYarnNoteId = noteId)) == 1)
        }
    }
}

data class YarnUsageProjectId(
    val id: Long,
)

data class ProjectYarnUsageRelations(
    @Embedded val project: YarnUsageProjectId,
    @Relation(parentColumn = "id", entityColumn = "projectId", entity = ProjectYarnNoteEntity::class)
    val notes: List<ResolvedUsageNote>,
    @Relation(parentColumn = "id", entityColumn = "linkedProjectId") val cards: List<YarnCardEntity>,
    @Relation(parentColumn = "id", entityColumn = "projectId", entity = ProjectYarnUsageEntity::class)
    val usages: List<ResolvedProjectYarnUsage>,
)

data class ResolvedUsageNote(
    @Embedded val note: ProjectYarnNoteEntity,
    @Relation(parentColumn = "savedYarnCardId", entityColumn = "id") val card: YarnCardEntity?,
)

data class ResolvedProjectYarnUsage(
    @Embedded val usage: ProjectYarnUsageEntity,
    @Relation(parentColumn = "yarnCardId", entityColumn = "id") val card: YarnCardEntity?,
    @Relation(parentColumn = "projectYarnNoteId", entityColumn = "id") val note: ProjectYarnNoteEntity?,
)
