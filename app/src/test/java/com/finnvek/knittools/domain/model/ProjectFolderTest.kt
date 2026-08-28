package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectFolderTest {
    @Test
    fun `name validation trims while preserving internal spaces`() {
        assertEquals(
            ProjectFolderNameValidationResult.Valid(
                name = "Personal  projects",
                normalizedName = "personal  projects",
            ),
            validateProjectFolderName("  Personal  projects  "),
        )
    }

    @Test
    fun `name validation accepts Unicode emoji and canonicalizes equivalent Unicode`() {
        val composed = validateProjectFolderName("Caf\u00e9 \ud83e\uddf6")
        val decomposed = validateProjectFolderName("CAFE\u0301 \ud83e\uddf6")

        assertEquals(
            ProjectFolderNameValidationResult.Valid("Caf\u00e9 \ud83e\uddf6", "caf\u00e9 \ud83e\uddf6"),
            composed,
        )
        assertEquals(
            (composed as ProjectFolderNameValidationResult.Valid).normalizedName,
            (decomposed as ProjectFolderNameValidationResult.Valid).normalizedName,
        )
    }

    @Test
    fun `name validation preserves decomposed display text and measures its original length`() {
        val decomposedAtLimit = "e\u0301".repeat(25)
        val decomposedOverLimit = decomposedAtLimit + "x"

        assertEquals(
            ProjectFolderNameValidationResult.Valid(decomposedAtLimit, "é".repeat(25)),
            validateProjectFolderName(decomposedAtLimit),
        )
        assertEquals(
            ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.TOO_LONG),
            validateProjectFolderName(decomposedOverLimit),
        )
    }

    @Test
    fun `name validation rejects empty control and over-limit names`() {
        assertEquals(
            ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.REQUIRED),
            validateProjectFolderName("   "),
        )
        assertEquals(
            ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.CONTROL_CHARACTER),
            validateProjectFolderName("Projects\n2026"),
        )
        assertEquals(
            ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.CONTROL_CHARACTER),
            validateProjectFolderName("Projects\t2026"),
        )
        assertEquals(
            ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.CONTROL_CHARACTER),
            validateProjectFolderName("Projects\u0007"),
        )
        assertEquals(
            ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.CONTROL_CHARACTER),
            validateProjectFolderName("Projects\u2028next"),
        )
        assertEquals(
            ProjectFolderNameValidationResult.Invalid(FolderNameValidationError.TOO_LONG),
            validateProjectFolderName("x".repeat(PROJECT_FOLDER_NAME_MAX_LENGTH + 1)),
        )
    }

    @Test
    fun `name validation accepts the exact 50 character boundary`() {
        val name = "x".repeat(PROJECT_FOLDER_NAME_MAX_LENGTH)

        assertEquals(
            ProjectFolderNameValidationResult.Valid(name, name),
            validateProjectFolderName(name),
        )
    }

    @Test
    fun `filters use virtual views without fake folder identities`() {
        val virtualFilters: List<ProjectFolderFilter> =
            listOf(ProjectFolderFilter.AllProjects, ProjectFolderFilter.Unfiled)
        val folder = ProjectFolderFilter.Folder(42L)

        assertTrue(virtualFilters.none { it is ProjectFolderFilter.Folder })
        assertEquals(42L, folder.folderId)
    }

    @Test
    fun `folder ordering uses sort order then stable identity`() {
        val folders =
            listOf(
                ProjectFolder(id = 9L, name = "Third", sortOrder = 1),
                ProjectFolder(id = 7L, name = "Second", sortOrder = 1),
                ProjectFolder(id = 11L, name = "First", sortOrder = 0),
            )

        assertEquals(listOf(11L, 7L, 9L), folders.inProjectFolderOrder().map(ProjectFolder::id))
    }

    @Test
    fun `move decisions reject the first earlier and last later boundaries`() {
        val folders =
            listOf(
                ProjectFolder(id = 1L, name = "First", sortOrder = 0),
                ProjectFolder(id = 2L, name = "Second", sortOrder = 1),
            )

        assertEquals(
            null,
            folders.projectFolderMoveTarget(1L, ProjectFolderMoveDirection.EARLIER),
        )
        assertEquals(
            null,
            folders.projectFolderMoveTarget(2L, ProjectFolderMoveDirection.LATER),
        )
        assertEquals(
            2L,
            folders.projectFolderMoveTarget(1L, ProjectFolderMoveDirection.LATER)?.id,
        )
    }
}
