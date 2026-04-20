package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pattern_annotations",
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
data class PatternAnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val page: Int,
    val pathData: String,
    val color: String,
    val strokeWidth: Float,
    val createdAt: Long = System.currentTimeMillis(),
)
