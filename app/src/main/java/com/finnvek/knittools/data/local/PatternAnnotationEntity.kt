package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pattern_annotations",
    foreignKeys = [
        ForeignKey(
            entity = PatternAnnotationLayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["layerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["layerId", "page", "zIndex"])],
)
data class PatternAnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val layerId: Long,
    val page: Int,
    val kind: String,
    val payloadVersion: Int,
    val payloadJson: String,
    val zIndex: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
