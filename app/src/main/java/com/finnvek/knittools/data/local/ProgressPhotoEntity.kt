package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress_photos",
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
data class ProgressPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val photoUri: String,
    val rowNumber: Int,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
