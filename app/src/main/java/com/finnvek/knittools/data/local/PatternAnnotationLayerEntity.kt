package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// CPD-OFF: Room-viiteavainmaaritys pidetaan tietokantataulun yhteydessa.
@Entity(
    tableName = "pattern_annotation_layers",
    foreignKeys = [
        ForeignKey(
            entity = CounterProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SavedPatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedPatternId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["projectId", "documentKey"], unique = true),
        Index(value = ["savedPatternId", "documentKey"], unique = true),
    ],
)
data class PatternAnnotationLayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long?,
    val savedPatternId: Long?,
    val documentKey: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
// CPD-ON
