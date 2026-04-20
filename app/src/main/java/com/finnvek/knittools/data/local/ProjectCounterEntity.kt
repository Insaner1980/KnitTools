package com.finnvek.knittools.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_counters",
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
data class ProjectCounterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val count: Int = 0,
    val stepSize: Int = 1,
    val repeatAt: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "COUNT_UP")
    val counterType: String = "COUNT_UP",
    @ColumnInfo(defaultValue = "NULL")
    val startingStitches: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val stitchChange: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val shapeEveryN: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val repeatStartRow: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val repeatEndRow: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val totalRepeats: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val currentRepeat: Int? = null,
)
