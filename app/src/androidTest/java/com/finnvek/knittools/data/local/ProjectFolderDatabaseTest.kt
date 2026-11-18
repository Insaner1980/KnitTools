package com.finnvek.knittools.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectFolderDatabaseTest {
    private lateinit var database: KnitToolsDatabase
    private lateinit var dao: ProjectFolderDao
    private lateinit var sql: SupportSQLiteDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    KnitToolsDatabase::class.java,
                ).build()
        dao = database.projectFolderDao()
        sql = database.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun organizationTablesEnforceUniqueCanonicalNamesOneAssignmentAndCascades() =
        runTest {
            assertTrue(tableExists("project_folders"))
            assertTrue(tableExists("project_folder_assignments"))
            assertIndexExists("project_folders", "index_project_folders_normalizedName")
            assertIndexExists("project_folder_assignments", "index_project_folder_assignments_folderId")

            val activeProject = database.counterProjectDao().insert(CounterProjectEntity(name = "Active"))
            val completedProject =
                database.counterProjectDao().insert(
                    CounterProjectEntity(name = "Completed", isCompleted = true, completedAt = 200L),
                )

            dao.insert(
                ProjectFolderEntity(id = 1L, name = "Gifts", normalizedName = "gifts", sortOrder = 1),
            )
            dao.insert(
                ProjectFolderEntity(id = 2L, name = "Personal", normalizedName = "personal", sortOrder = 1),
            )
            assertEquals(listOf(1L, 2L), dao.getFolders().map { it.id })

            assertSqliteFailure {
                dao.insert(
                    ProjectFolderEntity(
                        name = "GIFTS",
                        normalizedName = "gifts",
                        sortOrder = 2,
                    ),
                )
            }

            dao.insertOrReplaceAssignment(
                ProjectFolderAssignmentEntity(projectId = activeProject, folderId = 1L),
            )
            dao.insertOrReplaceAssignment(
                ProjectFolderAssignmentEntity(projectId = activeProject, folderId = 2L),
            )
            dao.insertOrReplaceAssignment(
                ProjectFolderAssignmentEntity(projectId = completedProject, folderId = 1L),
            )
            assertEquals(
                2L,
                dao.getAssignmentsForProjects(listOf(activeProject)).single().folderId,
            )
            assertEquals(
                setOf(activeProject, completedProject),
                dao.getAssignmentsForProjects(listOf(activeProject, completedProject)).map { it.projectId }.toSet(),
            )
            assertEquals(2L, scalarLong("SELECT COUNT(*) FROM project_folder_assignments"))
            assertEquals(0L, scalarLong("SELECT COUNT(*) FROM project_folder_assignments WHERE projectId = 999999"))

            sql.execSQL("DELETE FROM project_folders WHERE id = 1")
            assertEquals(1L, scalarLong("SELECT COUNT(*) FROM counter_projects WHERE id = $completedProject"))
            assertTrue(dao.getAssignmentsForProjects(listOf(completedProject)).isEmpty())

            database.counterProjectDao().delete(activeProject)
            assertTrue(dao.getAssignmentsForProjects(listOf(activeProject)).isEmpty())
            assertEquals(1L, scalarLong("SELECT COUNT(*) FROM project_folders WHERE id = 2"))
            assertFalse(tableExists("project_folder_memberships"))
        }

    private fun tableExists(table: String): Boolean =
        scalarLong("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$table'") == 1L

    // CPD-OFF: Skeematestin indeksitarkistus pidetaan taulufixturen yhteydessa.
    private fun assertIndexExists(
        table: String,
        indexName: String,
    ) {
        sql.query("PRAGMA index_list('$table')").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == indexName) {
                    found = true
                }
            }
            assertTrue("Missing index $indexName", found)
        }
    }
    // CPD-ON

    private fun scalarLong(query: String): Long =
        sql.query(query).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private suspend fun assertSqliteFailure(block: suspend () -> Unit) {
        try {
            block()
            fail("SQLite constraint accepted an invalid folder state")
        } catch (_: SQLiteConstraintException) {
            // Skeemaraja hylkäsi virheellisen tilan odotetusti.
        }
    }
}
