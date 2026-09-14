package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finnvek.knittools.domain.model.ProjectCompletion

@Entity(
    tableName = "project_completions",
    foreignKeys = [
        ForeignKey(
            entity = CounterProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class ProjectCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val completedAt: Long,
    val zoneId: String?,
)

fun ProjectCompletionEntity.toDomain(): ProjectCompletion = ProjectCompletion(id, projectId, completedAt, zoneId)
