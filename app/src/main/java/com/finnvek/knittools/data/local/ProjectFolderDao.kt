package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ProjectFolderOrganizationRow(
    val folderId: Long?,
    val folderName: String?,
    val folderSortOrder: Int?,
    val projectId: Long?,
    val assignedFolderId: Long?,
    val isCompleted: Boolean?,
)

@Dao
interface ProjectFolderDao {
    @Query("SELECT * FROM project_folders ORDER BY sortOrder ASC, id ASC")
    suspend fun getFolders(): List<ProjectFolderEntity>

    @Query("SELECT * FROM project_folders WHERE id = :folderId LIMIT 1")
    suspend fun getById(folderId: Long): ProjectFolderEntity?

    @Query("SELECT * FROM project_folders WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): ProjectFolderEntity?

    @Query("SELECT MAX(sortOrder) FROM project_folders")
    suspend fun getNextSortOrder(): Int?

    @Insert
    suspend fun insert(folder: ProjectFolderEntity): Long

    @Query(
        "UPDATE project_folders SET name = :name, normalizedName = :normalizedName WHERE id = :folderId",
    )
    suspend fun rename(
        folderId: Long,
        name: String,
        normalizedName: String,
    ): Int

    @Query("UPDATE project_folders SET sortOrder = :sortOrder WHERE id = :folderId")
    suspend fun updateSortOrder(
        folderId: Long,
        sortOrder: Int,
    ): Int

    @Query("DELETE FROM project_folders WHERE id = :folderId")
    suspend fun delete(folderId: Long): Int

    @Query("SELECT COUNT(*) FROM project_folder_assignments WHERE folderId = :folderId")
    suspend fun countAssignments(folderId: Long): Int

    @Query("SELECT * FROM project_folder_assignments WHERE projectId IN (:projectIds)")
    suspend fun getAssignmentsForProjects(projectIds: List<Long>): List<ProjectFolderAssignmentEntity>

    @Query("SELECT * FROM project_folder_assignments WHERE projectId = :projectId LIMIT 1")
    suspend fun getAssignment(projectId: Long): ProjectFolderAssignmentEntity?

    @Query("SELECT id FROM counter_projects WHERE id IN (:projectIds)")
    suspend fun getExistingProjectIds(projectIds: List<Long>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAssignment(assignment: ProjectFolderAssignmentEntity)

    @Query("DELETE FROM project_folder_assignments WHERE projectId IN (:projectIds)")
    suspend fun deleteAssignmentsForProjects(projectIds: List<Long>): Int

    @Query(
        """
        SELECT
            folders.id AS folderId,
            folders.name AS folderName,
            folders.sortOrder AS folderSortOrder,
            projects.id AS projectId,
            assignments.folderId AS assignedFolderId,
            projects.isCompleted AS isCompleted
        FROM project_folders AS folders
        LEFT JOIN project_folder_assignments AS assignments ON assignments.folderId = folders.id
        LEFT JOIN counter_projects AS projects ON projects.id = assignments.projectId
        UNION ALL
        SELECT
            NULL AS folderId,
            NULL AS folderName,
            NULL AS folderSortOrder,
            projects.id AS projectId,
            NULL AS assignedFolderId,
            projects.isCompleted AS isCompleted
        FROM counter_projects AS projects
        LEFT JOIN project_folder_assignments AS assignments ON assignments.projectId = projects.id
        WHERE assignments.projectId IS NULL
        ORDER BY folderSortOrder ASC, folderId ASC, projectId ASC
        """,
    )
    fun observeOrganizationRows(): Flow<List<ProjectFolderOrganizationRow>>
}
