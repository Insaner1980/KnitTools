package com.finnvek.knittools.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counter_projects",
    indices = [Index("linkedPatternId")],
)
data class CounterProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val count: Int = 0,
    @ColumnInfo(defaultValue = "'KNITTING'")
    val craftType: String = "KNITTING",
    @ColumnInfo(defaultValue = "'ROWS'")
    val mainCounterLabelType: String = "ROWS",
    @ColumnInfo(defaultValue = "NULL")
    val mainCounterCustomLabel: String? = null,
    @ColumnInfo(defaultValue = "0")
    val readingLineEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0.5")
    val readingLineYFraction: Float = 0.5f,
    val secondaryCount: Int = 0,
    val stepSize: Int = 1,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL")
    val sectionName: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val stitchCount: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val totalRows: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val completedAt: Long? = null,
    @ColumnInfo(defaultValue = "")
    val yarnCardIds: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val linkedPatternId: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val patternUri: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val patternName: String? = null,
    @ColumnInfo(defaultValue = "0")
    val currentPatternPage: Int = 0,
    @ColumnInfo(defaultValue = "NULL")
    val patternRowMapping: String? = null,
    @ColumnInfo(defaultValue = "0")
    val stitchTrackingEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val currentStitch: Int = 0,
    @ColumnInfo(defaultValue = "NULL")
    val targetRows: Int? = null,
)
