package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finnvek.knittools.domain.model.ProjectDocument

@Entity(
    tableName = "project_documents",
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
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(
            value = ["projectId", "sortOrder", "id"],
            name = "index_project_documents_project_order",
        ),
        Index(
            value = ["projectId", "isPrimary"],
            name = "index_project_documents_project_primary",
        ),
        Index(
            value = ["projectId", "documentKey"],
            name = "index_project_documents_project_key",
            unique = true,
        ),
        Index(
            value = ["projectId", "localPdfUri"],
            name = "index_project_documents_project_uri",
            unique = true,
        ),
        Index(
            value = ["projectId", "savedPatternId"],
            name = "index_project_documents_project_saved_pattern",
            unique = true,
        ),
        Index(
            value = ["savedPatternId"],
            name = "index_project_documents_savedPatternId",
        ),
        Index(
            value = ["localPdfUri"],
            name = "index_project_documents_localPdfUri",
        ),
    ],
)
data class ProjectDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val savedPatternId: Long?,
    val documentKey: String,
    val label: String,
    val localPdfUri: String,
    val sortOrder: Int,
    val isPrimary: Boolean,
    val currentPage: Int,
    val rowMapping: String?,
    val readingLineEnabled: Boolean,
    val readingLineYFraction: Float,
    val readingLineFollowCurrentRow: Boolean,
    val verticalReadingGuideEnabled: Boolean,
    val verticalReadingGuideXFraction: Float,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ProjectDocumentEntity.toDomain(): ProjectDocument =
    ProjectDocument(
        id = id,
        projectId = projectId,
        savedPatternId = savedPatternId,
        documentKey = documentKey,
        label = label,
        localPdfUri = localPdfUri,
        sortOrder = sortOrder,
        isPrimary = isPrimary,
        currentPage = currentPage,
        rowMapping = rowMapping,
        readingLineEnabled = readingLineEnabled,
        readingLineYFraction = readingLineYFraction,
        readingLineFollowCurrentRow = readingLineFollowCurrentRow,
        verticalReadingGuideEnabled = verticalReadingGuideEnabled,
        verticalReadingGuideXFraction = verticalReadingGuideXFraction,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ).sanitized()

fun ProjectDocument.toEntity(): ProjectDocumentEntity =
    sanitized().let { document ->
        ProjectDocumentEntity(
            id = document.id,
            projectId = document.projectId,
            savedPatternId = document.savedPatternId,
            documentKey = document.documentKey,
            label = document.label,
            localPdfUri = document.localPdfUri,
            sortOrder = document.sortOrder,
            isPrimary = document.isPrimary,
            currentPage = document.currentPage,
            rowMapping = document.rowMapping,
            readingLineEnabled = document.readingLineEnabled,
            readingLineYFraction = document.readingLineYFraction,
            readingLineFollowCurrentRow = document.readingLineFollowCurrentRow,
            verticalReadingGuideEnabled = document.verticalReadingGuideEnabled,
            verticalReadingGuideXFraction = document.verticalReadingGuideXFraction,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt,
        )
    }
