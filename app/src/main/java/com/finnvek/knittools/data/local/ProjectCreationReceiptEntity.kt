package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_creation_receipts",
    indices = [Index(value = ["projectId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = CounterProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProjectCreationReceiptEntity(
    @PrimaryKey val attemptId: String,
    val projectId: Long,
    val wasFirstProject: Boolean,
)
