package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finnvek.knittools.domain.model.ProjectFolder

@Entity(
    tableName = "project_folders",
    indices = [
        Index(
            value = ["normalizedName"],
            unique = true,
        ),
    ],
)
data class ProjectFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val sortOrder: Int,
)

fun ProjectFolderEntity.toDomain(): ProjectFolder =
    ProjectFolder(
        id = id,
        name = name,
        sortOrder = sortOrder,
    )
