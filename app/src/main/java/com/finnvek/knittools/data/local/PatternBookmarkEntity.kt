package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// CPD-OFF: Room-viiteavainmaaritys pidetaan tietokantataulun yhteydessa.
@Entity(
    tableName = "pattern_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = CounterProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["projectId", "documentKey", "pageIndex", "yFraction", "createdAt", "id"],
            name = "index_pattern_bookmarks_project_document_position",
        ),
    ],
)
data class PatternBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val documentKey: String,
    val name: String,
    val pageIndex: Int,
    val yFraction: Float,
    val createdAt: Long,
)
// CPD-ON
