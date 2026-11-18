package com.finnvek.knittools.ui.screens.project

import androidx.annotation.StringRes
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.FolderNameValidationError
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.repository.ProjectFolderMutationResult

data class ProjectFoldersState(
    val snapshot: ProjectFolderSnapshot? = null,
    val isLoading: Boolean = true,
    val readFailed: Boolean = false,
    val isMutating: Boolean = false,
    val mutationError: ProjectFolderMutationResult? = null,
)

internal fun ProjectFolderFilter.includes(folderId: Long?): Boolean =
    when (this) {
        ProjectFolderFilter.AllProjects -> true
        ProjectFolderFilter.Unfiled -> folderId == null
        is ProjectFolderFilter.Folder -> this.folderId == folderId
    }

@StringRes
internal fun ProjectFolderMutationResult.errorResource(
    @StringRes fallback: Int,
): Int =
    when (this) {
        ProjectFolderMutationResult.DuplicateName -> R.string.folder_name_duplicate
        is ProjectFolderMutationResult.InvalidName ->
            when (error) {
                FolderNameValidationError.REQUIRED -> R.string.folder_name_required
                FolderNameValidationError.TOO_LONG -> R.string.folder_name_max_length
                FolderNameValidationError.CONTROL_CHARACTER -> R.string.folder_name_invalid
            }
        ProjectFolderMutationResult.FolderMissing -> R.string.folder_missing
        ProjectFolderMutationResult.ProjectMissing -> R.string.folder_project_missing
        ProjectFolderMutationResult.StaleAction,
        ProjectFolderMutationResult.BoundaryMove,
        -> R.string.folder_stale_action
        else -> fallback
    }
