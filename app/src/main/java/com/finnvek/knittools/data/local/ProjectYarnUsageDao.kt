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
    // SQL:n null-arvo ei vastaa null-saraketta; haku löytää vain annetun kortin tai muistiinpanon tunnisteen.
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

    @Transaction
    suspend fun linkSavedCard(
        projectId: Long,
        noteId: Long,
        cardId: Long,
    ): LinkSavedCardUsageResult {
        val rows = getForSource(projectId, cardId, noteId)
        if (rows.isEmpty()) return LinkSavedCardUsageResult.NoUsage
        if (rows.size == 1) {
            check(update(rows.single().copy(yarnCardId = cardId, projectYarnNoteId = noteId)) == 1)
            return LinkSavedCardUsageResult.Linked
        }

        val merged = mergeLinkedUsageRows(rows, cardId, noteId) ?: return LinkSavedCardUsageResult.Conflict
        val removed = rows.single { it.id != merged.id }
        check(delete(removed.id, projectId) == 1)
        check(update(merged) == 1)
        return LinkSavedCardUsageResult.Linked
    }
}

sealed interface LinkSavedCardUsageResult {
    data object NoUsage : LinkSavedCardUsageResult

    data object Linked : LinkSavedCardUsageResult

    data object Conflict : LinkSavedCardUsageResult
}

private fun mergeLinkedUsageRows(
    rows: List<ProjectYarnUsageEntity>,
    cardId: Long,
    noteId: Long,
): ProjectYarnUsageEntity? {
    if (rows.size != 2) return null
    val survivor = rows.firstOrNull { it.projectYarnNoteId == noteId } ?: rows.minBy { it.id }
    val other = rows.single { it.id != survivor.id }

    fun mergedValue(
        first: Double?,
        second: Double?,
    ): Double? =
        when {
            first == null -> second
            second == null || first == second -> first
            else -> Double.NaN
        }

    val plannedMeters = mergedValue(survivor.plannedMeters, other.plannedMeters)
    val allocatedMeters = mergedValue(survivor.allocatedMeters, other.allocatedMeters)
    val usedMeters = mergedValue(survivor.usedMeters, other.usedMeters)
    val metersPerSkein = mergedValue(survivor.metersPerSkein, other.metersPerSkein)
    val gramsPerSkein = mergedValue(survivor.gramsPerSkein, other.gramsPerSkein)
    if (listOf(plannedMeters, allocatedMeters, usedMeters, metersPerSkein, gramsPerSkein).any { it?.isNaN() == true }) {
        return null
    }
    return survivor.copy(
        yarnCardId = cardId,
        projectYarnNoteId = noteId,
        sourceNameSnapshot = survivor.sourceNameSnapshot.ifBlank { other.sourceNameSnapshot },
        plannedMeters = plannedMeters,
        allocatedMeters = allocatedMeters,
        usedMeters = usedMeters,
        metersPerSkein = metersPerSkein,
        gramsPerSkein = gramsPerSkein,
        createdAt = minOf(survivor.createdAt, other.createdAt),
        updatedAt = maxOf(survivor.updatedAt, other.updatedAt),
    )
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
