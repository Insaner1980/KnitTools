package com.finnvek.knittools.repository

import android.database.sqlite.SQLiteConstraintException
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectFolderAssignmentEntity
import com.finnvek.knittools.data.local.ProjectFolderDao
import com.finnvek.knittools.data.local.ProjectFolderEntity
import com.finnvek.knittools.domain.model.FolderNameValidationError
import com.finnvek.knittools.domain.model.ProjectFolder
import com.finnvek.knittools.domain.model.ProjectFolderMoveDirection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ProjectFolderRepositoryTest {
    private lateinit var folderDao: ProjectFolderDao
    private lateinit var repository: ProjectFolderRepository

    @Before
    fun setUp() {
        folderDao = mockk(relaxed = true)
        coEvery { folderDao.getFolders() } returns emptyList()
        coEvery { folderDao.getByNormalizedName(any()) } returns null
        coEvery { folderDao.getNextSortOrder() } returns null
        coEvery { folderDao.insert(any()) } returns 41L
        coEvery { folderDao.updateSortOrder(any(), any()) } returns 1
        coEvery { folderDao.delete(any()) } returns 1
        coEvery { folderDao.countAssignments(any()) } returns 0
        coEvery { folderDao.getAssignmentsForProjects(any()) } returns emptyList()
        coEvery { folderDao.getExistingProjectIds(any()) } answers { firstArg<List<Long>>() }
        coEvery { folderDao.getById(any()) } answers {
            ProjectFolderEntity(id = firstArg(), name = "Folder", normalizedName = "folder", sortOrder = 0)
        }
        repository = createRepository()
    }

    @Test
    fun `create validates normalizes appends and returns the persisted folder`() =
        runTest {
            coEvery { folderDao.getNextSortOrder() } returns 8
            val inserted = slot<ProjectFolderEntity>()
            coEvery { folderDao.insert(capture(inserted)) } returns 42L

            val result = repository.createFolder("  Personal  ")

            assertEquals(
                ProjectFolderMutationResult.Created(ProjectFolder(42L, "Personal", 9)),
                result,
            )
            assertEquals("personal", inserted.captured.normalizedName)
            assertEquals(9, inserted.captured.sortOrder)
        }

    @Test
    fun `create normalizes extreme sort order before appending without overflow`() =
        runTest {
            coEvery { folderDao.getNextSortOrder() } returns Int.MAX_VALUE
            coEvery { folderDao.getFolders() } returns
                listOf(
                    ProjectFolderEntity(1L, "First", "first", Int.MAX_VALUE),
                    ProjectFolderEntity(2L, "Second", "second", Int.MAX_VALUE),
                )
            val inserted = slot<ProjectFolderEntity>()
            coEvery { folderDao.insert(capture(inserted)) } returns 3L

            assertEquals(
                ProjectFolderMutationResult.Created(ProjectFolder(3L, "Third", 2)),
                repository.createFolder("Third"),
            )
            assertEquals(2, inserted.captured.sortOrder)
            coVerify { folderDao.updateSortOrder(1L, 0) }
            coVerify { folderDao.updateSortOrder(2L, 1) }
        }

    @Test
    fun `create maps invalid duplicate and concurrent duplicate values`() =
        runTest {
            assertEquals(
                ProjectFolderMutationResult.InvalidName(FolderNameValidationError.REQUIRED),
                repository.createFolder("   "),
            )

            coEvery { folderDao.getByNormalizedName("personal") } returns
                ProjectFolderEntity(5L, "Personal", "personal", 0)
            assertEquals(ProjectFolderMutationResult.DuplicateName, repository.createFolder("personal"))

            coEvery { folderDao.getByNormalizedName("gifts") } returns null
            coEvery { folderDao.insert(any()) } throws SQLiteConstraintException("unique")
            assertEquals(ProjectFolderMutationResult.DuplicateName, repository.createFolder("Gifts"))
        }

    @Test
    fun `rename allows an idempotent canonical value but rejects a stale folder`() =
        runTest {
            val folder = ProjectFolderEntity(7L, "Personal", "personal", 3)
            coEvery { folderDao.getById(7L) } returns folder

            assertEquals(
                ProjectFolderMutationResult.Renamed(ProjectFolder(7L, "Personal", 3)),
                repository.renameFolder(7L, " Personal "),
            )
            coVerify(exactly = 0) { folderDao.rename(any(), any(), any()) }

            coEvery { folderDao.getById(7L) } returns null
            assertEquals(ProjectFolderMutationResult.StaleAction, repository.renameFolder(7L, "Gifts"))
        }

    @Test
    fun `rename updates display case when canonical name remains the same`() =
        runTest {
            coEvery { folderDao.getById(7L) } returns ProjectFolderEntity(7L, "Personal", "personal", 3)
            coEvery { folderDao.rename(7L, "PERSONAL", "personal") } returns 1

            assertEquals(
                ProjectFolderMutationResult.Renamed(ProjectFolder(7L, "PERSONAL", 3)),
                repository.renameFolder(7L, "PERSONAL"),
            )
            coVerify { folderDao.rename(7L, "PERSONAL", "personal") }
        }

    @Test
    fun `reorder swaps adjacent folder sort orders and rejects a boundary`() =
        runTest {
            coEvery { folderDao.getFolders() } returns
                listOf(
                    ProjectFolderEntity(1L, "First", "first", 0),
                    ProjectFolderEntity(2L, "Second", "second", 1),
                )

            assertEquals(
                ProjectFolderMutationResult.BoundaryMove,
                repository.moveFolder(1L, ProjectFolderMoveDirection.EARLIER),
            )
            assertTrue(
                repository.moveFolder(1L, ProjectFolderMoveDirection.LATER) is ProjectFolderMutationResult.Reordered,
            )
            coVerifyOrder {
                folderDao.updateSortOrder(1L, 1)
                folderDao.updateSortOrder(2L, 0)
            }
        }

    @Test
    fun `reorder normalizes tied sort orders deterministically`() =
        runTest {
            coEvery { folderDao.getFolders() } returns
                listOf(
                    ProjectFolderEntity(1L, "First", "first", 0),
                    ProjectFolderEntity(2L, "Second", "second", 0),
                )

            assertEquals(
                ProjectFolderMutationResult.Reordered(ProjectFolder(1L, "First", 1)),
                repository.moveFolder(1L, ProjectFolderMoveDirection.LATER),
            )
            coVerify { folderDao.updateSortOrder(1L, 1) }
        }

    @Test
    fun `delete returns the complete assignment count and only deletes the folder`() =
        runTest {
            val folder = ProjectFolderEntity(8L, "Gifts", "gifts", 1)
            coEvery { folderDao.getById(8L) } returns folder
            coEvery { folderDao.countAssignments(8L) } returns 4

            assertEquals(
                ProjectFolderMutationResult.Deleted(ProjectFolder(8L, "Gifts", 1), 4),
                repository.deleteFolder(8L),
            )
            coVerify { folderDao.delete(8L) }
        }

    @Test
    fun `move projects verifies every id and writes a real folder atomically`() =
        runTest {
            coEvery { folderDao.getById(6L) } returns ProjectFolderEntity(6L, "Gifts", "gifts", 0)

            assertEquals(
                ProjectFolderMutationResult.ProjectsMoved(setOf(2L, 3L), 6L),
                repository.moveProjects(listOf(2L, 3L, 2L), 6L),
            )
            coVerify { folderDao.insertOrReplaceAssignment(ProjectFolderAssignmentEntity(2L, 6L)) }
            coVerify { folderDao.insertOrReplaceAssignment(ProjectFolderAssignmentEntity(3L, 6L)) }
        }

    @Test
    fun `move projects rejects a stale member or folder before writing anything`() =
        runTest {
            coEvery { folderDao.getExistingProjectIds(listOf(2L, 3L)) } returns listOf(2L)
            assertEquals(ProjectFolderMutationResult.ProjectMissing, repository.moveProjects(listOf(2L, 3L), null))

            coEvery { folderDao.getExistingProjectIds(listOf(2L)) } returns listOf(2L)
            coEvery { folderDao.getById(99L) } returns null
            assertEquals(ProjectFolderMutationResult.FolderMissing, repository.moveProjects(listOf(2L), 99L))
            coVerify(exactly = 0) { folderDao.insertOrReplaceAssignment(any()) }
        }

    @Test
    fun `moving to Unfiled deletes assignments without mutating the project row`() =
        runTest {
            coEvery { folderDao.getAssignmentsForProjects(listOf(2L)) } returns
                listOf(ProjectFolderAssignmentEntity(2L, 5L))

            assertEquals(ProjectFolderMutationResult.Unassigned(2L), repository.moveProjects(listOf(2L), null))
            coVerify { folderDao.deleteAssignmentsForProjects(listOf(2L)) }
        }

    @Test
    fun `already assigned is idempotent and cancellation propagates`() =
        runTest {
            coEvery { folderDao.getAssignmentsForProjects(listOf(2L)) } returns
                listOf(ProjectFolderAssignmentEntity(2L, 5L))
            coEvery { folderDao.getById(5L) } returns ProjectFolderEntity(5L, "Personal", "personal", 0)

            assertEquals(
                ProjectFolderMutationResult.AlreadyAssigned(setOf(2L)),
                repository.moveProjects(listOf(2L), 5L),
            )

            repository =
                createRepository(
                    transactionRunner =
                        object : DatabaseTransactionRunner {
                            override suspend fun <T> run(block: suspend () -> T): T =
                                throw CancellationException("cancelled")
                        },
                )
            try {
                repository.createFolder("Personal")
                fail("CancellationException expected")
            } catch (_: CancellationException) {
                // Peruutus kuuluu välittää kutsujalle, ei palauttaa persistence failure -tuloksena.
            }
        }

    private fun createRepository(
        transactionRunner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ): ProjectFolderRepository =
        ProjectFolderRepository(
            folderDao = folderDao,
            transactionRunner = transactionRunner,
        )
}
