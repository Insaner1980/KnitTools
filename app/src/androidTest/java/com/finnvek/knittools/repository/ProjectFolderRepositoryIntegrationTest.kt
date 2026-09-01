package com.finnvek.knittools.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class ProjectFolderRepositoryIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: ProjectFolderRepository
    private lateinit var counterRepository: CounterRepository
    private lateinit var sql: SupportSQLiteDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    KnitToolsDatabase::class.java,
                ).addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        repository = newRepository(RoomDatabaseTransactionRunner(database))
        counterRepository = newCounterRepository()
        sql = database.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun moveAndDeleteAreMetadataOnlyForActiveCompletedAndRecoveryProjects() =
        runTest {
            val activeProject = createProject("Active", updatedAt = 101L)
            val completedProject = createProject("Completed", completed = true, updatedAt = 202L)
            insertProjectOwnedRows(activeProject)
            insertProjectOwnedRows(completedProject)
            val sentinel =
                File
                    .createTempFile("folder-metadata", ".bin", context.cacheDir)
                    .apply { writeText("folder metadata must not delete files") }
            val sentinelHash = sha256(sentinel)
            sql.execSQL(
                "UPDATE project_documents SET localPdfUri = ? WHERE projectId = ?",
                arrayOf<Any>(sentinel.toURI().toString(), activeProject),
            )
            sql.execSQL(
                "UPDATE progress_photos SET photoUri = ? WHERE projectId = ?",
                arrayOf<Any>(sentinel.toURI().toString(), activeProject),
            )
            val activeBefore = projectOwnedSnapshot(activeProject)
            val completedBefore = projectOwnedSnapshot(completedProject)

            val folder = (repository.createFolder("Gifts") as ProjectFolderMutationResult.Created).folder
            assertEquals(
                ProjectFolderMutationResult.ProjectsMoved(
                    setOf(activeProject, completedProject),
                    folder.id,
                ),
                repository.moveProjects(listOf(activeProject, completedProject), folder.id),
            )
            assertEquals(activeBefore, projectOwnedSnapshot(activeProject))
            assertEquals(completedBefore, projectOwnedSnapshot(completedProject))
            assertEquals(2L, assignmentCount(folder.id))
            assertEquals(sentinelHash, sha256(sentinel))

            database.counterProjectDao().archiveProject(activeProject, 4, 500L, 600L)
            database.counterProjectDao().reactivateProject(activeProject, 700L)
            assertEquals(1L, assignmentCount(folder.id, activeProject))

            val deleted = repository.deleteFolder(folder.id)
            assertEquals(
                ProjectFolderMutationResult.Deleted(folder, affectedProjectCount = 2),
                deleted,
            )
            assertEquals(0L, assignmentCount(folder.id))
            assertEquals(activeBefore.copyProjectLifecycle(700L), projectOwnedSnapshot(activeProject))
            assertEquals(completedBefore, projectOwnedSnapshot(completedProject))
            assertEquals(sentinelHash, sha256(sentinel))
            assertTrue(sentinel.delete())
        }

    @Test
    fun staleProjectOrTargetRejectsTheWholeMoveWithoutPartialAssignment() =
        runTest {
            val validProject = createProject("Valid", updatedAt = 1L)
            val folder = (repository.createFolder("Personal") as ProjectFolderMutationResult.Created).folder

            assertEquals(
                ProjectFolderMutationResult.ProjectMissing,
                repository.moveProjects(listOf(validProject, 999_999L), folder.id),
            )
            assertEquals(0L, assignmentCount(folder.id))

            assertEquals(
                ProjectFolderMutationResult.FolderMissing,
                repository.moveProjects(listOf(validProject), 888_888L),
            )
            assertEquals(0L, assignmentCount())
        }

    @Test
    fun cancellationRollsBackTheFolderMutation() =
        runTest {
            val cancellingRepository =
                newRepository(
                    object : DatabaseTransactionRunner {
                        override suspend fun <T> run(block: suspend () -> T): T =
                            database.withTransaction {
                                block()
                                throw CancellationException("test cancellation")
                            }
                    },
                )

            try {
                cancellingRepository.createFolder("Cancelled")
            } catch (_: CancellationException) {
                // Peruutus kuuluu välittää kutsujalle ja Room-transaktion perua.
            }

            assertEquals(0L, scalarLong("SELECT COUNT(*) FROM project_folders"))
        }

    @Test
    fun createProjectInSelectedFolderCommitsTheProjectAndOneAssignmentTogether() =
        runTest {
            val folder = (repository.createFolder("Projects") as ProjectFolderMutationResult.Created).folder

            val result =
                counterRepository.createProject(
                    name = "Folder cardigan",
                    canCreateAdditionalProjects = true,
                    targetFolderId = folder.id,
                )

            val created = result as ProjectCreationResult.Created
            assertEquals("Folder cardigan", database.counterProjectDao().getProject(created.projectId)?.name)
            assertEquals(
                folder.id,
                scalarLong("SELECT folderId FROM project_folder_assignments WHERE projectId = ${created.projectId}"),
            )
            assertEquals(1L, assignmentCount(folder.id, created.projectId))
        }

    @Test
    fun createProjectWithMissingFolderLeavesProjectsAssignmentsAndLinkedPatternUntouched() =
        runTest {
            val projectsBefore = scalarLong("SELECT COUNT(*) FROM counter_projects")
            val patternsBefore = scalarLong("SELECT COUNT(*) FROM saved_patterns")

            val result =
                counterRepository.createProject(
                    name = "Missing destination",
                    canCreateAdditionalProjects = true,
                    linkedPattern = linkedPattern("Missing destination pattern"),
                    targetFolderId = 404L,
                )

            assertEquals(ProjectCreationResult.FolderMissing, result)
            assertEquals(projectsBefore, scalarLong("SELECT COUNT(*) FROM counter_projects"))
            assertEquals(patternsBefore, scalarLong("SELECT COUNT(*) FROM saved_patterns"))
            assertEquals(0L, assignmentCount())
        }

    @Test
    fun assignmentFailureRollsBackProjectAndLinkedPatternCreation() =
        runTest {
            val folder = (repository.createFolder("Failure target") as ProjectFolderMutationResult.Created).folder
            sql.execSQL(
                "CREATE TRIGGER reject_project_folder_assignment " +
                    "BEFORE INSERT ON project_folder_assignments " +
                    "WHEN NEW.folderId = ${folder.id} " +
                    "BEGIN SELECT RAISE(ABORT, 'assignment rejected'); END",
            )
            val projectsBefore = scalarLong("SELECT COUNT(*) FROM counter_projects")
            val patternsBefore = scalarLong("SELECT COUNT(*) FROM saved_patterns")

            try {
                counterRepository.createProject(
                    name = "Rolled back project",
                    canCreateAdditionalProjects = true,
                    linkedPattern = linkedPattern("Rolled back pattern"),
                    targetFolderId = folder.id,
                )
                throw AssertionError("SQLite constraint failure expected")
            } catch (_: SQLiteConstraintException) {
                // Assignmentin hylkäys peruu saman Room-transaktion aiemmat kirjoitukset.
            }

            assertEquals(projectsBefore, scalarLong("SELECT COUNT(*) FROM counter_projects"))
            assertEquals(patternsBefore, scalarLong("SELECT COUNT(*) FROM saved_patterns"))
            assertEquals(0L, assignmentCount(folder.id))
        }

    private fun newRepository(transactionRunner: DatabaseTransactionRunner): ProjectFolderRepository =
        ProjectFolderRepository(
            folderDao = database.projectFolderDao(),
            transactionRunner = transactionRunner,
        )

    // CPD-OFF: Integraatiotestin repository-kooste pidetaan tietokantafixturen yhteydessa.
    private fun newCounterRepository(): CounterRepository {
        val transactionRunner = RoomDatabaseTransactionRunner(database)
        val savedPatternRepository =
            SavedPatternRepository(
                dao = database.savedPatternDao(),
                context = context,
                counterProjectDao = database.counterProjectDao(),
                transactionRunner = transactionRunner,
                ioDispatcher = Dispatchers.IO,
                projectDocumentDao = database.projectDocumentDao(),
            )
        val projectDocumentRepository =
            ProjectDocumentRepository(
                documentDao = database.projectDocumentDao(),
                projectDao = database.counterProjectDao(),
                savedPatternRepository = savedPatternRepository,
                layerRepository =
                    PatternAnnotationLayerRepository(
                        database.patternAnnotationLayerDao(),
                        transactionRunner,
                    ),
                transactionRunner = transactionRunner,
                fileAvailability = ProjectDocumentFileAvailability(context, Dispatchers.IO),
            )
        return CounterRepository(
            dao = database.counterProjectDao(),
            projectCounterDao = database.projectCounterDao(),
            sessionDao = database.sessionDao(),
            photoStorage = ProgressPhotoStorage(),
            patternDocumentStorage = PatternDocumentStorage(),
            context = context,
            yarnCardRepository =
                YarnCardRepository(
                    dao = database.yarnCardDao(),
                    counterProjectDao = database.counterProjectDao(),
                    context = context,
                    transactionRunner = transactionRunner,
                    ioDispatcher = Dispatchers.IO,
                ),
            savedPatternRepository = savedPatternRepository,
            projectDocumentRepository = projectDocumentRepository,
            projectFolderDao = database.projectFolderDao(),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.IO,
        )
    }
    // CPD-ON

    private fun linkedPattern(name: String): SavedPattern =
        SavedPattern(
            source = SavedPatternSource.Other,
            name = name,
            designerName = "Designer",
        )

    private suspend fun createProject(
        name: String,
        completed: Boolean = false,
        updatedAt: Long,
    ): Long =
        database.counterProjectDao().insert(
            CounterProjectEntity(
                name = name,
                count = 4,
                notes = "Project note",
                createdAt = 100L,
                updatedAt = updatedAt,
                isCompleted = completed,
                totalRows = if (completed) 4 else null,
                completedAt = if (completed) 300L else null,
            ),
        )

    private fun insertProjectOwnedRows(projectId: Long) {
        sql.execSQL(
            "INSERT INTO counter_history (projectId, action, previousValue, newValue, timestamp) VALUES (?, 'increment', 3, 4, 100)",
            arrayOf(projectId),
        )
        sql.execSQL(
            "INSERT INTO sessions (projectId, startedAt, endedAt, startRow, endRow, durationMinutes, durationSeconds, rowsWorked, zoneId) VALUES (?, 1, 61, 0, 4, 1, 60, 4, 'Europe/Helsinki')",
            arrayOf(projectId),
        )
        if (projectId == 1L) {
            sql.execSQL(
                "INSERT INTO active_sessions (singletonId, sessionToken, projectId, startedAtWallMillis, startZoneId, startRow, lastObservedRow, trustedLastObservedRow, trustedRowsWorked, pendingRowsWorked, reviewedRowsWorked, reviewedLastObservedRow, unreviewedRowsWorked, checkpointedDurationSeconds, reviewedDurationBaselineSeconds, segmentStartedAtWallMillis, segmentStartedElapsedRealtimeMillis, bootCount, recoveryReason, recoveryIntervalToken, recoverySuggestedDurationSeconds, recoveryPromptShown, updatedAtWallMillis) VALUES (1, 'recovery', ?, 1, 'Europe/Helsinki', 0, 4, 3, 3, 1, 0, 3, 1, 60, 0, 1, 1, 2, 'REBOOT', 'recovery-token', 60, 0, 100)",
                arrayOf(projectId),
            )
        }
        sql.execSQL(
            "INSERT INTO project_documents (projectId, savedPatternId, documentKey, label, localPdfUri, sortOrder, isPrimary, currentPage, rowMapping, readingLineEnabled, readingLineYFraction, readingLineFollowCurrentRow, verticalReadingGuideEnabled, verticalReadingGuideXFraction, createdAt, updatedAt) VALUES (?, NULL, 'document-key-' || ?, 'Document', 'file:///document-' || ? || '.pdf', 0, 1, 2, '4:2:0.4', 1, 0.4, 1, 1, 0.6, 100, 101)",
            arrayOf(projectId, projectId, projectId),
        )
        val layerId = projectId + 100L
        sql.execSQL(
            "INSERT INTO pattern_annotation_layers (id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt) VALUES (?, ?, NULL, 'document-key-' || ?, 1, 100, 100)",
            arrayOf(layerId, projectId, projectId),
        )
        sql.execSQL(
            "INSERT INTO pattern_annotations (layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt) VALUES (?, 2, 'FREEHAND', 1, '{}', 0, 100, 100)",
            arrayOf(layerId),
        )
        sql.execSQL(
            "INSERT INTO pattern_bookmarks (projectId, documentKey, name, pageIndex, yFraction, createdAt) VALUES (?, 'document-key-' || ?, 'Row 4', 2, 0.4, 100)",
            arrayOf(projectId, projectId),
        )
        sql.execSQL(
            "INSERT INTO progress_photos (projectId, photoUri, rowNumber, note, createdAt) VALUES (?, 'file:///progress-' || ? || '.jpg', 4, 'Progress', 100)",
            arrayOf(projectId, projectId),
        )
        sql.execSQL(
            "INSERT INTO yarn_cards (brand, yarnName, fiberContent, weightGrams, lengthMeters, needleSize, gaugeInfo, colorName, colorNumber, dyeLot, weightCategory, careSymbols, photoUri, createdAt, quantityInStash, status, linkedProjectId) VALUES ('', 'Yarn', '', '', '', '', '', '', '', '', '', 0, '', 100, 1, 'IN_STASH', ?)",
            arrayOf(projectId),
        )
        sql.execSQL(
            "INSERT INTO project_yarn_notes (projectId, name, description, quantity, notes, savedYarnCardId, createdAt, updatedAt) VALUES (?, 'Yarn', '', 1, '', NULL, 100, 100)",
            arrayOf(projectId),
        )
        sql.execSQL(
            "INSERT INTO row_reminders (projectId, targetRow, repeatInterval, message, isCompleted, createdAt) VALUES (?, 5, NULL, 'Reminder', 0, 100)",
            arrayOf(projectId),
        )
        sql.execSQL(
            "INSERT INTO project_counters (projectId, name, count, stepSize, repeatAt, sortOrder, createdAt, counterType, linkedToMainCounter) VALUES (?, 'Counter', 1, 1, NULL, 0, 100, 'COUNT_UP', 0)",
            arrayOf(projectId),
        )
    }

    private fun projectOwnedSnapshot(projectId: Long): ProjectOwnedSnapshot =
        ProjectOwnedSnapshot(
            project = rows("SELECT * FROM counter_projects WHERE id = $projectId"),
            history = rows("SELECT * FROM counter_history WHERE projectId = $projectId"),
            sessions = rows("SELECT * FROM sessions WHERE projectId = $projectId"),
            activeSession = rows("SELECT * FROM active_sessions WHERE projectId = $projectId"),
            documents = rows("SELECT * FROM project_documents WHERE projectId = $projectId"),
            layers = rows("SELECT * FROM pattern_annotation_layers WHERE projectId = $projectId"),
            annotations =
                rows(
                    "SELECT annotations.* FROM pattern_annotations AS annotations INNER JOIN pattern_annotation_layers AS layers ON layers.id = annotations.layerId WHERE layers.projectId = $projectId",
                ),
            bookmarks = rows("SELECT * FROM pattern_bookmarks WHERE projectId = $projectId"),
            photos = rows("SELECT * FROM progress_photos WHERE projectId = $projectId"),
            yarn = rows("SELECT * FROM yarn_cards WHERE linkedProjectId = $projectId"),
            yarnNotes = rows("SELECT * FROM project_yarn_notes WHERE projectId = $projectId"),
            reminders = rows("SELECT * FROM row_reminders WHERE projectId = $projectId"),
            counters = rows("SELECT * FROM project_counters WHERE projectId = $projectId"),
        )

    private fun ProjectOwnedSnapshot.copyProjectLifecycle(updatedAt: Long): ProjectOwnedSnapshot =
        copy(
            project = project.map { row -> row.toMutableList().also { it[17] = updatedAt.toString() } },
        )

    private fun assignmentCount(
        folderId: Long? = null,
        projectId: Long? = null,
    ): Long {
        val predicates =
            buildList {
                folderId?.let { add("folderId = $it") }
                projectId?.let { add("projectId = $it") }
            }
        val where = predicates.takeIf { it.isNotEmpty() }?.joinToString(" AND ", prefix = " WHERE ").orEmpty()
        return scalarLong("SELECT COUNT(*) FROM project_folder_assignments$where")
    }

    private fun rows(query: String): List<List<String?>> =
        sql.query(query).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.values())
                }
            }
        }

    private fun scalarLong(query: String): Long =
        sql.query(query).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun sha256(file: File): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun Cursor.values(): List<String?> =
        List(columnCount) { index -> if (isNull(index)) null else getString(index) }

    private data class ProjectOwnedSnapshot(
        val project: List<List<String?>>,
        val history: List<List<String?>>,
        val sessions: List<List<String?>>,
        val activeSession: List<List<String?>>,
        val documents: List<List<String?>>,
        val layers: List<List<String?>>,
        val annotations: List<List<String?>>,
        val bookmarks: List<List<String?>>,
        val photos: List<List<String?>>,
        val yarn: List<List<String?>>,
        val yarnNotes: List<List<String?>>,
        val reminders: List<List<String?>>,
        val counters: List<List<String?>>,
    )
}
