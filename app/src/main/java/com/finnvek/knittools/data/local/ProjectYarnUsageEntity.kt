package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finnvek.knittools.domain.model.ProjectYarnUsage
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource

@Entity(
    tableName = "project_yarn_usage",
    foreignKeys = [
        ForeignKey(
            entity = CounterProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = YarnCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["yarnCardId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ProjectYarnNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectYarnNoteId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["projectId", "yarnCardId"], unique = true),
        Index(value = ["projectId", "projectYarnNoteId"], unique = true),
        Index("yarnCardId"),
        Index("projectYarnNoteId"),
    ],
)
data class ProjectYarnUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val yarnCardId: Long? = null,
    val projectYarnNoteId: Long? = null,
    val sourceNameSnapshot: String,
    val plannedMeters: Double? = null,
    val allocatedMeters: Double? = null,
    val usedMeters: Double? = null,
    val metersPerSkein: Double? = null,
    val gramsPerSkein: Double? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain(): ProjectYarnUsage =
        ProjectYarnUsage(
            id,
            projectId,
            YarnUsageSource(yarnCardId, projectYarnNoteId),
            sourceNameSnapshot,
            YarnUsageAmounts(plannedMeters, allocatedMeters, usedMeters, metersPerSkein, gramsPerSkein),
            createdAt,
            updatedAt,
        )

    fun withAmounts(
        amounts: YarnUsageAmounts,
        timestamp: Long,
    ): ProjectYarnUsageEntity =
        copy(
            plannedMeters = amounts.plannedMeters,
            allocatedMeters = amounts.allocatedMeters,
            usedMeters = amounts.usedMeters,
            metersPerSkein = amounts.metersPerSkein,
            gramsPerSkein = amounts.gramsPerSkein,
            updatedAt = timestamp,
        )
}
