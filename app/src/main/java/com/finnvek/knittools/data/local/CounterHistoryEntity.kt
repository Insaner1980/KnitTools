package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counter_history",
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
data class CounterHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val action: String,
    val previousValue: Int,
    val newValue: Int,
    val timestamp: Long = System.currentTimeMillis(),
)
