package com.finnvek.knittools.repository

import android.database.sqlite.SQLiteConstraintException
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectFolderAssignmentEntity
import com.finnvek.knittools.data.local.ProjectFolderDao
import com.finnvek.knittools.data.local.ProjectFolderEntity
import com.finnvek.knittools.data.local.ProjectFolderOrganizationRow
import com.finnvek.knittools.data.local.distinctSqliteQueryChunks
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.FolderNameValidationError
import com.finnvek.knittools.domain.model.ProjectFolder
import com.finnvek.knittools.domain.model.ProjectFolderMembership
import com.finnvek.knittools.domain.model.ProjectFolderMoveDirection
import com.finnvek.knittools.domain.model.ProjectFolderNameValidationResult
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.domain.model.inProjectFolderOrder
import com.finnvek.knittools.domain.model.projectFolderMoveTarget
import com.finnvek.knittools.domain.model.validateProjectFolderName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ProjectFolderMutationResult {
    data class Created(
        val folder: ProjectFolder,
    ) : ProjectFolderMutationResult

    data class Renamed(
        val folder: ProjectFolder,
    ) : ProjectFolderMutationResult

    data class Reordered(
        val folder: ProjectFolder,
    ) : ProjectFolderMutationResult

    data class Deleted(
        val folder: ProjectFolder,
        val affectedProjectCount: Int,
    ) : ProjectFolderMutationResult

    data class Assigned(
        val projectId: Long,
        val folderId: Long,
    ) : ProjectFolderMutationResult

    data class Unassigned(
        val projectId: Long,
    ) : ProjectFolderMutationResult

    data class ProjectsMoved(
        val projectIds: Set<Long>,
        val folderId: Long?,
    ) : ProjectFolderMutationResult

    data class AlreadyAssigned(
        val projectIds: Set<Long>,
    ) : ProjectFolderMutationResult

    data object FolderMissing : ProjectFolderMutationResult

    data object ProjectMissing : ProjectFolderMutationResult

    data object DuplicateName : ProjectFolderMutationResult

    data class InvalidName(
        val error: FolderNameValidationError,
    ) : ProjectFolderMutationResult

    data object BoundaryMove : ProjectFolderMutationResult

    data object StaleAction : ProjectFolderMutationResult

    data object PersistenceFailure : ProjectFolderMutationResult
}

val ProjectFolderMutationResult.isSuccess: Boolean
    get() =
        this is ProjectFolderMutationResult.Created ||
            this is ProjectFolderMutationResult.Renamed ||
            this is ProjectFolderMutationResult.Reordered ||
            this is ProjectFolderMutationResult.Deleted ||
            this is ProjectFolderMutationResult.Assigned ||
            this is ProjectFolderMutationResult.Unassigned ||
            this is ProjectFolderMutationResult.ProjectsMoved ||
            this is ProjectFolderMutationResult.AlreadyAssigned

@Singleton
class ProjectFolderRepository
    @Inject
    constructor(
        private val folderDao: ProjectFolderDao,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        fun observeOrganization(onReadFailure: () -> Unit = {}): Flow<ProjectFolderSnapshot> =
            folderDao
                .observeOrganizationRows()
                .map { rows -> rows.toSnapshot() }
                .retryOnRepositoryReadFailure(onReadFailure)

        suspend fun createFolder(name: String): ProjectFolderMutationResult {
            val validation = validateProjectFolderName(name)
            if (validation is ProjectFolderNameValidationResult.Invalid) {
                return ProjectFolderMutationResult.InvalidName(validation.error)
            }
            validation as ProjectFolderNameValidationResult.Valid
            return runMutation(onConstraint = ProjectFolderMutationResult.DuplicateName) {
                if (folderDao.getByNormalizedName(validation.normalizedName) != null) {
                    return@runMutation ProjectFolderMutationResult.DuplicateName
                }
                val sortOrder = nextSortOrder()
                val folder =
                    ProjectFolderEntity(
                        name = validation.name,
                        normalizedName = validation.normalizedName,
                        sortOrder = sortOrder,
                    )
                ProjectFolderMutationResult.Created(folder.copy(id = folderDao.insert(folder)).toDomain())
            }
        }

        suspend fun renameFolder(
            folderId: Long,
            name: String,
        ): ProjectFolderMutationResult {
            val validation = validateProjectFolderName(name)
            if (validation is ProjectFolderNameValidationResult.Invalid) {
                return ProjectFolderMutationResult.InvalidName(validation.error)
            }
            validation as ProjectFolderNameValidationResult.Valid
            return runMutation(onConstraint = ProjectFolderMutationResult.DuplicateName) {
                val existing = folderDao.getById(folderId) ?: return@runMutation ProjectFolderMutationResult.StaleAction
                if (existing.normalizedName == validation.normalizedName && existing.name == validation.name) {
                    return@runMutation ProjectFolderMutationResult.Renamed(existing.toDomain())
                }
                val duplicate = folderDao.getByNormalizedName(validation.normalizedName)
                if (duplicate != null && duplicate.id != folderId) {
                    return@runMutation ProjectFolderMutationResult.DuplicateName
                }
                if (folderDao.rename(folderId, validation.name, validation.normalizedName) != 1) {
                    return@runMutation ProjectFolderMutationResult.StaleAction
                }
                ProjectFolderMutationResult.Renamed(
                    existing.copy(name = validation.name, normalizedName = validation.normalizedName).toDomain(),
                )
            }
        }

        suspend fun moveFolder(
            folderId: Long,
            direction: ProjectFolderMoveDirection,
        ): ProjectFolderMutationResult =
            runMutation {
                val folders = folderDao.getFolders().map(ProjectFolderEntity::toDomain).inProjectFolderOrder()
                val current =
                    folders.firstOrNull { it.id == folderId }
                        ?: return@runMutation ProjectFolderMutationResult.StaleAction
                val target =
                    folders.projectFolderMoveTarget(folderId, direction)
                        ?: return@runMutation ProjectFolderMutationResult.BoundaryMove
                val hasSortOrderTies = folders.map(ProjectFolder::sortOrder).toSet().size != folders.size
                if (hasSortOrderTies) {
                    val reordered = folders.toMutableList()
                    val currentIndex = reordered.indexOfFirst { it.id == current.id }
                    val targetIndex = reordered.indexOfFirst { it.id == target.id }
                    reordered[currentIndex] = target
                    reordered[targetIndex] = current
                    reordered.forEachIndexed { index, folder ->
                        if (folder.sortOrder != index && folderDao.updateSortOrder(folder.id, index) != 1) {
                            throw MutationAbort(ProjectFolderMutationResult.StaleAction)
                        }
                    }
                    return@runMutation ProjectFolderMutationResult.Reordered(
                        current.copy(sortOrder = targetIndex),
                    )
                }
                if (folderDao.updateSortOrder(current.id, target.sortOrder) != 1) {
                    throw MutationAbort(ProjectFolderMutationResult.StaleAction)
                }
                if (folderDao.updateSortOrder(target.id, current.sortOrder) != 1) {
                    throw MutationAbort(ProjectFolderMutationResult.StaleAction)
                }
                ProjectFolderMutationResult.Reordered(current.copy(sortOrder = target.sortOrder))
            }

        suspend fun deleteFolder(folderId: Long): ProjectFolderMutationResult =
            runMutation {
                val folder = folderDao.getById(folderId) ?: return@runMutation ProjectFolderMutationResult.StaleAction
                val affectedProjectCount = folderDao.countAssignments(folderId)
                if (folderDao.delete(folderId) != 1) {
                    return@runMutation ProjectFolderMutationResult.StaleAction
                }
                ProjectFolderMutationResult.Deleted(folder.toDomain(), affectedProjectCount)
            }

        @Suppress("kotlin:S3776") // Monisiirron validointi ja jäsenyyskirjoitus kuuluvat samaan atomiseen operaatioon.
        suspend fun moveProjects(
            projectIds: Collection<Long>,
            folderId: Long?,
        ): ProjectFolderMutationResult {
            val idChunks = projectIds.distinctSqliteQueryChunks()
            if (idChunks.isEmpty()) return ProjectFolderMutationResult.StaleAction
            val ids = idChunks.flatten().toCollection(linkedSetOf())
            return runMutation {
                val existingProjectIds =
                    idChunks
                        .flatMap { chunk -> folderDao.getExistingProjectIds(chunk) }
                        .toSet()
                if (existingProjectIds != ids) {
                    return@runMutation ProjectFolderMutationResult.ProjectMissing
                }
                if (folderId != null && folderDao.getById(folderId) == null) {
                    return@runMutation ProjectFolderMutationResult.FolderMissing
                }
                val currentAssignments =
                    idChunks
                        .flatMap { chunk -> folderDao.getAssignmentsForProjects(chunk) }
                        .associateBy { it.projectId }
                if (ids.all { currentAssignments[it]?.folderId == folderId }) {
                    return@runMutation ProjectFolderMutationResult.AlreadyAssigned(ids)
                }
                if (folderId == null) {
                    idChunks.forEach { chunk -> folderDao.deleteAssignmentsForProjects(chunk) }
                } else {
                    ids.forEach { projectId ->
                        folderDao.insertOrReplaceAssignment(
                            ProjectFolderAssignmentEntity(projectId = projectId, folderId = folderId),
                        )
                    }
                }
                if (ids.size == 1) {
                    val projectId = ids.single()
                    if (folderId == null) {
                        ProjectFolderMutationResult.Unassigned(projectId)
                    } else {
                        ProjectFolderMutationResult.Assigned(projectId, folderId)
                    }
                } else {
                    ProjectFolderMutationResult.ProjectsMoved(ids, folderId)
                }
            }
        }

        private suspend fun runMutation(
            onConstraint: ProjectFolderMutationResult = ProjectFolderMutationResult.PersistenceFailure,
            block: suspend () -> ProjectFolderMutationResult,
        ): ProjectFolderMutationResult =
            try {
                transactionRunner.run(block)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (abort: MutationAbort) {
                abort.result
            } catch (_: SQLiteConstraintException) {
                onConstraint
            } catch (_: Exception) {
                ProjectFolderMutationResult.PersistenceFailure
            }

        private suspend fun nextSortOrder(): Int {
            val currentMaximum = folderDao.getNextSortOrder() ?: return 0
            if (currentMaximum != Int.MAX_VALUE) return currentMaximum + 1

            val folders = folderDao.getFolders()
            folders.forEachIndexed { index, folder ->
                if (folder.sortOrder != index && folderDao.updateSortOrder(folder.id, index) != 1) {
                    throw MutationAbort(ProjectFolderMutationResult.PersistenceFailure)
                }
            }
            return folders.size
        }

        private fun List<ProjectFolderOrganizationRow>.toSnapshot(): ProjectFolderSnapshot {
            val folders =
                asSequence()
                    .mapNotNull { row ->
                        row.folderId?.let { id ->
                            ProjectFolder(
                                id = id,
                                name = requireNotNull(row.folderName),
                                sortOrder = requireNotNull(row.folderSortOrder),
                            )
                        }
                    }.distinctBy(ProjectFolder::id)
                    .toList()
                    .inProjectFolderOrder()
            val memberships =
                mapNotNull { row ->
                    row.projectId?.let { projectId ->
                        ProjectFolderMembership(
                            projectId = projectId,
                            folderId = row.assignedFolderId,
                            isCompleted = requireNotNull(row.isCompleted),
                        )
                    }
                }
            return ProjectFolderSnapshot(folders = folders, memberships = memberships)
        }

        private class MutationAbort(
            val result: ProjectFolderMutationResult,
        ) : RuntimeException()
    }
