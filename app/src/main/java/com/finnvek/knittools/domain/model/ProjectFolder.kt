package com.finnvek.knittools.domain.model

import java.text.Normalizer
import java.util.Locale

const val PROJECT_FOLDER_NAME_MAX_LENGTH = 50

data class ProjectFolder(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)

sealed interface ProjectFolderFilter {
    data object AllProjects : ProjectFolderFilter

    data object Unfiled : ProjectFolderFilter

    data class Folder(
        val folderId: Long,
    ) : ProjectFolderFilter
}

data class ProjectFolderMembership(
    val projectId: Long,
    val folderId: Long?,
    val isCompleted: Boolean,
)

data class ProjectFolderSnapshot(
    val folders: List<ProjectFolder>,
    val memberships: List<ProjectFolderMembership>,
)

enum class ProjectFolderMoveDirection {
    EARLIER,
    LATER,
}

enum class FolderNameValidationError {
    REQUIRED,
    TOO_LONG,
    CONTROL_CHARACTER,
}

sealed interface ProjectFolderNameValidationResult {
    data class Valid(
        val name: String,
        val normalizedName: String,
    ) : ProjectFolderNameValidationResult

    data class Invalid(
        val error: FolderNameValidationError,
    ) : ProjectFolderNameValidationResult
}

fun validateProjectFolderName(input: String): ProjectFolderNameValidationResult {
    if (input.any { it.isISOControl() || it == '\u2028' || it == '\u2029' }) {
        return ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.CONTROL_CHARACTER)
    }
    val name = input.trim()
    if (name.isEmpty()) {
        return ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.REQUIRED)
    }
    if (name.length > PROJECT_FOLDER_NAME_MAX_LENGTH) {
        return ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.TOO_LONG)
    }
    return ProjectFolderNameValidationResult.Valid(
        name = name,
        normalizedName = Normalizer.normalize(name, Normalizer.Form.NFC).lowercase(Locale.ROOT),
    )
}

fun List<ProjectFolder>.inProjectFolderOrder(): List<ProjectFolder> =
    sortedWith(
        compareBy<ProjectFolder> { it.sortOrder }.thenBy { it.id },
    )

fun List<ProjectFolder>.projectFolderMoveTarget(
    folderId: Long,
    direction: ProjectFolderMoveDirection,
): ProjectFolder? {
    val folders = inProjectFolderOrder()
    val index = folders.indexOfFirst { it.id == folderId }
    if (index < 0) return null
    val targetIndex =
        when (direction) {
            ProjectFolderMoveDirection.EARLIER -> index - 1
            ProjectFolderMoveDirection.LATER -> index + 1
        }
    return folders.getOrNull(targetIndex)
}
