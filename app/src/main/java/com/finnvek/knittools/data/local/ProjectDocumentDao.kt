package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDocumentDao :
    ProjectDocumentQueries,
    ProjectDocumentMutations

interface ProjectDocumentQueries {
    @Query("SELECT * FROM project_documents WHERE projectId = :projectId ORDER BY sortOrder, id")
    fun observeForProject(projectId: Long): Flow<List<ProjectDocumentEntity>>

    @Query("SELECT * FROM project_documents WHERE projectId IN (:projectIds) ORDER BY projectId, sortOrder, id")
    fun observeForProjects(projectIds: List<Long>): Flow<List<ProjectDocumentEntity>>

    @Query("SELECT * FROM project_documents WHERE projectId = :projectId AND isPrimary = 1 LIMIT 1")
    fun observePrimary(projectId: Long): Flow<ProjectDocumentEntity?>

    @Query("SELECT * FROM project_documents WHERE projectId = :projectId ORDER BY sortOrder, id")
    suspend fun getForProject(projectId: Long): List<ProjectDocumentEntity>

    @Query("SELECT * FROM project_documents WHERE projectId IN (:projectIds) ORDER BY projectId, sortOrder, id")
    suspend fun getForProjects(projectIds: List<Long>): List<ProjectDocumentEntity>

    @Query("SELECT * FROM project_documents WHERE id = :documentId")
    suspend fun getById(documentId: Long): ProjectDocumentEntity?

    @Query("SELECT * FROM project_documents WHERE projectId = :projectId AND isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(projectId: Long): ProjectDocumentEntity?

    @Query("SELECT * FROM project_documents WHERE projectId = :projectId AND documentKey = :documentKey")
    suspend fun getByDocumentKey(
        projectId: Long,
        documentKey: String,
    ): ProjectDocumentEntity?

    @Query("SELECT * FROM project_documents WHERE projectId = :projectId AND localPdfUri = :localPdfUri")
    suspend fun getByUri(
        projectId: Long,
        localPdfUri: String,
    ): ProjectDocumentEntity?

    @Query("SELECT * FROM project_documents WHERE projectId = :projectId AND savedPatternId = :savedPatternId")
    suspend fun getBySavedPatternId(
        projectId: Long,
        savedPatternId: Long,
    ): ProjectDocumentEntity?

    @Query("SELECT COUNT(*) FROM project_documents WHERE projectId = :projectId")
    suspend fun countForProject(projectId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM project_documents WHERE projectId = :projectId")
    suspend fun getHighestSortOrder(projectId: Long): Int

    @Query("SELECT DISTINCT localPdfUri FROM project_documents WHERE projectId = :projectId")
    suspend fun getDistinctUris(projectId: Long): List<String>

    @Query("SELECT DISTINCT localPdfUri FROM project_documents WHERE projectId IN (:projectIds)")
    suspend fun getDistinctUris(projectIds: List<Long>): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM project_documents WHERE localPdfUri = :localPdfUri)")
    suspend fun isUriReferenced(localPdfUri: String): Boolean
}

interface ProjectDocumentMutations {
    @Insert
    suspend fun insert(document: ProjectDocumentEntity): Long

    @Query(
        "UPDATE project_documents SET label = :label, updatedAt = :updatedAt WHERE id = :documentId AND projectId = :projectId",
    )
    suspend fun updateLabel(
        documentId: Long,
        projectId: Long,
        label: String,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE project_documents SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :documentId AND projectId = :projectId",
    )
    suspend fun updateSortOrder(
        documentId: Long,
        projectId: Long,
        sortOrder: Int,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE project_documents SET isPrimary = 0, updatedAt = :updatedAt WHERE projectId = :projectId AND isPrimary = 1",
    )
    suspend fun clearPrimary(
        projectId: Long,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE project_documents SET isPrimary = 1, updatedAt = :updatedAt WHERE id = :documentId AND projectId = :projectId",
    )
    suspend fun setPrimary(
        documentId: Long,
        projectId: Long,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE project_documents SET
            currentPage = :currentPage,
            rowMapping = :rowMapping,
            readingLineEnabled = :readingLineEnabled,
            readingLineYFraction = :readingLineYFraction,
            readingLineFollowCurrentRow = :readingLineFollowCurrentRow,
            verticalReadingGuideEnabled = :verticalReadingGuideEnabled,
            verticalReadingGuideXFraction = :verticalReadingGuideXFraction,
            updatedAt = :updatedAt
        WHERE id = :documentId AND projectId = :projectId
        """,
    )
    @Suppress("kotlin:S107") // Room-päivitys välittää yhden dokumenttitilan sarakkeet eksplisiittisesti.
    suspend fun updateViewerState(
        documentId: Long,
        projectId: Long,
        currentPage: Int,
        rowMapping: String?,
        readingLineEnabled: Boolean,
        readingLineYFraction: Float,
        readingLineFollowCurrentRow: Boolean,
        verticalReadingGuideEnabled: Boolean,
        verticalReadingGuideXFraction: Float,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM project_documents WHERE id = :documentId AND projectId = :projectId")
    suspend fun delete(
        documentId: Long,
        projectId: Long,
    ): Int
}
