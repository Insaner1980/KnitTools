package com.finnvek.knittools.repository

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.domain.model.ProjectDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LegacyPatternCleanupRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val filesDir = File(context.filesDir, "pattern_pdfs/dp-f01")
    private lateinit var database: KnitToolsDatabase
    private lateinit var documents: ProjectDocumentRepository
    private lateinit var patterns: SavedPatternRepository
    private lateinit var pdf: File
    private lateinit var originalBytes: ByteArray
    private lateinit var migrated: ProjectDocument
    private var afterCommit: (suspend () -> Unit)? = null

    @get:Rule
    val migrationHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            KnitToolsDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Before
    fun setUp() {
        assertFalse(context.getDatabasePath(TEST_DATABASE).exists())
        assertFalse(filesDir.exists())
        assertTrue(filesDir.mkdirs())
        pdf = createPdf("migrated.pdf")
        originalBytes = pdf.readBytes()
        migrationHelper.createDatabase(TEST_DATABASE, 21).use { legacy ->
            legacy.execSQL(
                """
                INSERT INTO saved_patterns (
                    id, source, name, designerName, availability, originalUrl, canonicalUrl,
                    localPdfUri, isAvailableOffline, savedAt, updatedAt
                ) VALUES (20, 'LOCAL_PDF', 'Migrated chart', '', 'unknown', '', '', ?, 1, 1000, 2000)
                """.trimIndent(),
                arrayOf(uri(pdf)),
            )
            legacy.execSQL(
                """
                INSERT INTO counter_projects (
                    id, name, count, secondaryCount, stepSize, notes, createdAt, updatedAt,
                    linkedPatternId, patternUri, patternName, currentPatternPage
                ) VALUES (1, 'Migrated project', 7, 0, 1, 'Keep notes', 1000, 2000, 20, ?, 'Migrated chart', 0)
                """.trimIndent(),
                arrayOf(uri(pdf)),
            )
            legacy.execSQL(
                """
                INSERT INTO pattern_annotation_layers
                    (projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt)
                VALUES (1, NULL, 'saved:20:v1', 1, 1000, 2000), (NULL, 20, 'saved:20:v1', 1, 1000, 2000)
                """.trimIndent(),
            )
        }
        database =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, TEST_DATABASE)
                .addMigrations(*KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        val room = RoomDatabaseTransactionRunner(database)
        val runner =
            object : DatabaseTransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T {
                    val result = room.run(block)
                    afterCommit?.invoke()
                    return result
                }
            }
        val coordinator = PatternFileReferenceCoordinator()
        patterns =
            SavedPatternRepository(
                dao = database.savedPatternDao(),
                context = context,
                counterProjectDao = database.counterProjectDao(),
                transactionRunner = runner,
                ioDispatcher = Dispatchers.IO,
                projectDocumentDao = database.projectDocumentDao(),
                fileReferenceCoordinator = coordinator,
            )
        documents =
            ProjectDocumentRepository(
                documentDao = database.projectDocumentDao(),
                projectDao = database.counterProjectDao(),
                savedPatternRepository = patterns,
                layerRepository = PatternAnnotationLayerRepository(database.patternAnnotationLayerDao(), runner),
                transactionRunner = runner,
                fileAvailability = ProjectDocumentFileAvailability(context, Dispatchers.IO),
                fileReferenceCoordinator = coordinator,
            )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        context.deleteDatabase(TEST_DATABASE)
        filesDir.deleteRecursively()
        assertFalse(context.getDatabasePath(TEST_DATABASE).exists())
        assertFalse(filesDir.exists())
    }

    @Test
    fun migratedDocumentThenLibraryRemovalDeletesFinalPdf() =
        runTest {
            loadMigratedDocument()
            val projectBefore = requireNotNull(database.counterProjectDao().getProject(PROJECT_ID))

            remove(migrated)

            assertEquals(
                projectBefore.copy(patternUri = null),
                database.counterProjectDao().getProject(PROJECT_ID)?.copy(patternUri = null),
            )
            assertNotNull(patterns.getById(PATTERN_ID))
            assertReadableBytes()
            expectCommittedReferencesGoneBeforeCleanup()
            patterns.deleteById(PATTERN_ID)
            assertFinalCleanup()
        }

    @Test
    fun libraryThenMigratedDocumentRemovalDeletesFinalPdf() =
        runTest {
            loadMigratedDocument()
            patterns.deleteById(PATTERN_ID)

            assertEquals(uri(pdf), database.counterProjectDao().getProject(PROJECT_ID)?.patternUri)
            assertNull(documents.getDocument(migrated.id)?.savedPatternId)
            assertReadableBytes()
            expectCommittedReferencesGoneBeforeCleanup()
            remove(migrated)
            assertFinalCleanup()
        }

    @Test
    fun anotherProjectDocumentKeepsBytesUntilItsOwnRemoval() =
        runTest {
            loadMigratedDocument()
            val otherProject = database.counterProjectDao().insert(CounterProjectEntity(name = "Other owner"))
            val other =
                (documents.addSavedPattern(otherProject, PATTERN_ID) as ProjectDocumentMutationResult.Added).document
            patterns.deleteById(PATTERN_ID)
            val otherBefore = documents.getDocument(other.id)

            remove(migrated)

            assertNull(database.counterProjectDao().getProject(PROJECT_ID)?.patternUri)
            assertEquals(otherBefore, documents.getDocument(other.id))
            assertEquals(uri(pdf), documents.getDocument(other.id)?.localPdfUri)
            assertReadableBytes()
            expectCommittedReferencesGoneBeforeCleanup()
            remove(other)
            assertFinalCleanup()
        }

    @Test
    fun failedDocumentDeletionRollsBackLegacyPrimaryAndLayerChanges() =
        runTest {
            loadMigratedDocument()
            patterns.deleteById(PATTERN_ID)
            val before = database.auditSnapshot()
            var committed = false
            afterCommit = { committed = true }
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TEMP TRIGGER dp_f01_delete_failure AFTER DELETE ON project_documents
                BEGIN
                    SELECT RAISE(ABORT, 'DP-F01 injected deletion failure');
                END
                """.trimIndent(),
            )

            assertEquals(ProjectDocumentMutationResult.PersistenceFailure, documents.remove(PROJECT_ID, migrated.id))

            assertFalse(committed)
            assertEquals(before, database.auditSnapshot())
            assertEquals(uri(pdf), database.counterProjectDao().getProject(PROJECT_ID)?.patternUri)
            assertEquals(migrated.id, documents.getPrimary(PROJECT_ID)?.id)
            assertReadableBytes()
            database.openHelper.writableDatabase.execSQL("DROP TRIGGER dp_f01_delete_failure")
        }

    @Test
    fun legacyOnlyOwnerStillProtectsTheSharedPdf() =
        runTest {
            loadMigratedDocument()
            val legacyOwner =
                CounterProjectEntity(name = "Legacy only", patternUri = uri(pdf), currentPatternPage = 3)
            val ownerId = database.counterProjectDao().insert(legacyOwner)
            patterns.deleteById(PATTERN_ID)

            remove(migrated)

            assertNull(database.counterProjectDao().getProject(PROJECT_ID)?.patternUri)
            assertEquals(legacyOwner.copy(id = ownerId), database.counterProjectDao().getProject(ownerId))
            assertTrue(documents.getDocuments(ownerId).isEmpty())
            patterns.deleteLocalPatternFileIfUnused(uri(pdf))
            assertReadableBytes()
        }

    @Test
    fun legacyFileProviderAliasProtectsThePdfUntilItsLastOwnerIsRemoved() =
        runTest {
            loadMigratedDocument()
            val legacyUri =
                "content://${AppFileStorage.fileProviderAuthority(context)}/pattern_pdfs/dp-f01/${pdf.name}"
            val ownerId =
                database.counterProjectDao().insert(CounterProjectEntity(name = "Legacy alias", patternUri = legacyUri))
            patterns.deleteById(PATTERN_ID)

            remove(migrated)

            assertReadableBytes()
            database.counterProjectDao().delete(ownerId)
            patterns.deleteLocalPatternFileIfUnused(uri(pdf))
            assertFalse(pdf.exists())
        }

    @Test
    fun removingAnotherDocumentPreservesTheProjectsDifferentLegacyUri() =
        runTest {
            loadMigratedDocument()
            val otherFile = createPdf("other.pdf")
            val other =
                (
                    documents.addImportedPdf(PROJECT_ID, uri(otherFile), "Other") as ProjectDocumentMutationResult.Added
                ).document
            val projectBefore = database.counterProjectDao().getProject(PROJECT_ID)
            patterns.deleteById(requireNotNull(other.savedPatternId))

            remove(other)

            assertFalse(otherFile.exists())
            assertEquals(projectBefore, database.counterProjectDao().getProject(PROJECT_ID))
            assertEquals(migrated, documents.getDocument(migrated.id))
            assertReadableBytes()
        }

    private suspend fun loadMigratedDocument() {
        migrated = documents.getDocuments(PROJECT_ID).single()
        assertEquals(PATTERN_ID, migrated.savedPatternId)
        assertEquals(uri(pdf), migrated.localPdfUri)
        assertEquals(uri(pdf), database.counterProjectDao().getProject(PROJECT_ID)?.patternUri)
        assertTrue(migrated.isPrimary)
        assertReadableBytes()
    }

    private suspend fun remove(document: ProjectDocument) {
        assertTrue(documents.remove(document.projectId, document.id) is ProjectDocumentMutationResult.Removed)
        assertNull(documents.getDocument(document.id))
    }

    private fun expectCommittedReferencesGoneBeforeCleanup() {
        afterCommit = {
            assertFalse(database.inTransaction())
            assertNull(patterns.getById(PATTERN_ID))
            assertFalse(database.projectDocumentDao().isUriReferenced(uri(pdf)))
            assertReadableBytes()
            afterCommit = null
        }
    }

    private suspend fun assertFinalCleanup() {
        assertNull("Commit boundary was not checked", afterCommit)
        assertFalse("PDF remains after its final owner was removed", pdf.exists())
        assertEquals(0, database.counterProjectDao().countProjectsUsingPatternUri(uri(pdf)))
    }

    private fun assertReadableBytes() {
        assertArrayEquals(originalBytes, pdf.readBytes())
        PdfPageRenderer(context, Uri.fromFile(pdf)).use { renderer ->
            assertEquals(1, renderer.pageCount)
            renderer.renderPage(0, 120).recycle()
        }
    }

    private fun createPdf(name: String): File =
        File(filesDir, name).also { file ->
            val document = PdfDocument()
            try {
                val page = document.startPage(PdfDocument.PageInfo.Builder(120, 160, 1).create())
                document.finishPage(page)
                file.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }
        }

    private fun uri(file: File): String = Uri.fromFile(file).toString()

    private companion object {
        private const val TEST_DATABASE = "dp-f01-room.db"
        private const val PROJECT_ID = 1L
        private const val PATTERN_ID = 20L
    }
}
