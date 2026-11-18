package com.finnvek.knittools.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.PatternBookmarkEntity
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import com.finnvek.knittools.domain.model.ProjectDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ProjectDocumentRuntimeIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val filesDir = File(context.filesDir, "pattern_pdfs/project-document-runtime")
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: ProjectDocumentRepository
    private lateinit var savedPatternRepository: SavedPatternRepository
    private lateinit var layerRepository: PatternAnnotationLayerRepository

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE)
        filesDir.deleteRecursively()
        filesDir.mkdirs()
        openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DATABASE)
        filesDir.deleteRecursively()
    }

    @Test
    fun multipleDocumentsKeepIndependentStateAndOwnedDataAcrossDatabaseReopen() =
        runTest {
            val projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "Runtime cardigan"))
            val documentA = addPdf(projectId, createPdf("a.pdf", Color.RED), "Main chart")
            val documentB = addPdf(projectId, createPdf("b.pdf", Color.BLUE), "Sleeve chart")
            val documentC = addPdf(projectId, createPdf("c.pdf", Color.GREEN), "Schematic")

            assertTrue(documentA.isPrimary)
            assertFalse(documentB.isPrimary)
            assertEquals(listOf(0, 1, 2), repository.getDocuments(projectId).map(ProjectDocument::sortOrder))
            renderEach(documentA, documentB, documentC)

            persistDistinctState(projectId, documentA, documentB, documentC)
            addOwnedData(projectId, documentA, documentB)
            assertOwnedDataIsIsolated(projectId, documentA, documentB)

            assertEquals(ProjectDocumentMutationResult.Reordered, repository.moveEarlier(projectId, documentC.id))
            assertEquals(ProjectDocumentMutationResult.PrimaryChanged, repository.setPrimary(projectId, documentC.id))
            val orderBeforeReopen = repository.getDocuments(projectId).map(ProjectDocument::id)

            database.close()
            openDatabase()

            assertEquals(orderBeforeReopen, repository.getDocuments(projectId).map(ProjectDocument::id))
            assertEquals(documentC.id, repository.getPrimary(projectId)?.id)
            assertEquals(8, repository.getDocument(documentA.id)?.currentPage)
            assertEquals(0.22f, repository.getDocument(documentA.id)?.readingLineYFraction)
            assertEquals(3, repository.getDocument(documentB.id)?.currentPage)
            assertEquals(0.77f, repository.getDocument(documentB.id)?.readingLineYFraction)
        }

    @Test
    fun failureAttachingFirstPdfRollsBackProjectAndLibraryRow() =
        runTest {
            val existing =
                database
                    .counterProjectDao()
                    .insert(CounterProjectEntity(name = "Existing", isCompleted = true))
            val file = createPdf("new-project.pdf", Color.RED)
            val uri = Uri.fromFile(file).toString()
            val counter = RoomCounterTestFixture(database, context).counterRepository(documents = repository)
            val sqlite = database.openHelper.writableDatabase
            sqlite.execSQL(
                """
                CREATE TRIGGER reject_first_pdf BEFORE INSERT ON project_documents
                BEGIN SELECT RAISE(ABORT, 'Controlled document attachment failure'); END
                """.trimIndent(),
            )
            try {
                val failure =
                    runCatching {
                        counter.createProjectWithImportedPdf("New project", false, uri, "First chart")
                    }.exceptionOrNull()
                assertTrue(failure is SQLiteConstraintException)
                assertEquals(listOf(existing), counter.getAllProjects().first().map { it.id })
                assertTrue(database.savedPatternDao().getAllOnce().isEmpty())
                assertFalse(database.projectDocumentDao().isUriReferenced(uri))
                assertTrue(file.isFile)
            } finally {
                sqlite.execSQL("DROP TRIGGER reject_first_pdf")
            }

            val created = counter.createProjectWithImportedPdf("New project", false, uri, "First chart")
            val projectId = (created as ProjectCreationResult.Created).projectId
            assertEquals(uri, repository.getPrimary(projectId)?.localPdfUri)
        }

    @Test
    fun libraryOnlyImportPublishesOnlyAnExistingPrivateFileAndReusesItsRow() =
        runTest {
            val file = createPdf("library-only.pdf", Color.BLUE)
            val uri = Uri.fromFile(file).toString()
            val missingUri = Uri.fromFile(File(filesDir, "missing.pdf")).toString()

            assertNull(savedPatternRepository.saveImportedPatternIfMissing(missingUri, "Unavailable"))
            val id = requireNotNull(savedPatternRepository.saveImportedPatternIfMissing(uri, "Library chart"))
            assertEquals(id, savedPatternRepository.saveImportedPatternIfMissing(uri, "Different label"))

            val pattern = database.savedPatternDao().getAllOnce().single()
            assertEquals(id, pattern.id)
            assertEquals("Library chart", pattern.name)
            assertEquals(uri, pattern.localPdfUri)
            assertTrue(pattern.isAvailableOffline)
            assertTrue(file.isFile)
            assertTrue(
                database
                    .counterProjectDao()
                    .getAllProjects()
                    .first()
                    .isEmpty(),
            )
            assertFalse(database.projectDocumentDao().isUriReferenced(uri))
        }

    @Test
    fun savedPatternDeletionMissingFilesRemovalAndSharedCleanupRemainReferenceAware() =
        runTest {
            val firstProject = database.counterProjectDao().insert(CounterProjectEntity(name = "First"))
            val secondProject = database.counterProjectDao().insert(CounterProjectEntity(name = "Second"))
            val sharedFile = createPdf("shared.pdf", Color.MAGENTA)
            val firstDocument = addPdf(firstProject, sharedFile, "Shared chart")
            val secondDocument = addPdf(secondProject, sharedFile, "Shared chart")
            val savedPatternId = requireNotNull(firstDocument.savedPatternId)

            savedPatternRepository.deleteById(savedPatternId)

            assertNull(repository.getDocument(firstDocument.id)?.savedPatternId)
            assertNull(repository.getDocument(secondDocument.id)?.savedPatternId)
            assertTrue(sharedFile.exists())

            assertTrue(repository.remove(firstProject, firstDocument.id) is ProjectDocumentMutationResult.Removed)
            assertTrue(sharedFile.exists())
            sharedFile.delete()
            assertEquals(
                ProjectDocumentMutationResult.PdfUnavailable,
                repository.select(secondProject, secondDocument.id),
            )
            assertEquals(secondDocument.id, repository.getDocument(secondDocument.id)?.id)

            assertTrue(repository.remove(secondProject, secondDocument.id) is ProjectDocumentMutationResult.Removed)
            assertTrue(repository.getDocuments(secondProject).isEmpty())
            assertNull(layerRepository.getActiveProjectLayer(secondProject))
        }

    private fun openDatabase() {
        database =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, TEST_DATABASE)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        val transactionRunner = RoomDatabaseTransactionRunner(database)
        savedPatternRepository =
            SavedPatternRepository(
                dao = database.savedPatternDao(),
                context = context,
                counterProjectDao = database.counterProjectDao(),
                transactionRunner = transactionRunner,
                ioDispatcher = Dispatchers.IO,
                projectDocumentDao = database.projectDocumentDao(),
            )
        layerRepository = PatternAnnotationLayerRepository(database.patternAnnotationLayerDao(), transactionRunner)
        repository =
            ProjectDocumentRepository(
                documentDao = database.projectDocumentDao(),
                projectDao = database.counterProjectDao(),
                savedPatternRepository = savedPatternRepository,
                layerRepository = layerRepository,
                transactionRunner = transactionRunner,
                fileAvailability = ProjectDocumentFileAvailability(context, Dispatchers.IO),
            )
    }

    private suspend fun addPdf(
        projectId: Long,
        file: File,
        label: String,
    ): ProjectDocument =
        (
            repository.addImportedPdf(
                projectId,
                Uri.fromFile(file).toString(),
                label,
            ) as ProjectDocumentMutationResult.Added
        ).document

    private suspend fun persistDistinctState(
        projectId: Long,
        documentA: ProjectDocument,
        documentB: ProjectDocument,
        documentC: ProjectDocument,
    ) {
        repository.updateViewerState(projectId, documentA.id, 8, "[]", true, 0.22f, false, true, 0.31f)
        repository.select(projectId, documentB.id)
        repository.updateViewerState(projectId, documentB.id, 3, null, true, 0.77f, true, false, 0.69f)
        repository.select(projectId, documentC.id)
        repository.updateViewerState(projectId, documentC.id, 1, null, false, 0.44f, true, true, 0.58f)
        repository.select(projectId, documentA.id)
        assertEquals(documentA.id, repository.getActiveDocument(projectId)?.id)
        repository.select(projectId, documentB.id)
        assertEquals(documentB.id, repository.getActiveDocument(projectId)?.id)
    }

    private suspend fun addOwnedData(
        projectId: Long,
        documentA: ProjectDocument,
        documentB: ProjectDocument,
    ) {
        val layerA =
            requireNotNull(database.patternAnnotationLayerDao().getProjectLayer(projectId, documentA.documentKey))
        val layerB =
            requireNotNull(database.patternAnnotationLayerDao().getProjectLayer(projectId, documentB.documentKey))
        database.patternBookmarkDao().insert(bookmark(projectId, documentA.documentKey, "Body"))
        database.patternBookmarkDao().insert(bookmark(projectId, documentB.documentKey, "Sleeve"))
        repository.select(projectId, documentA.id)
        database.patternAnnotationDao().insert(annotation(layerA.id, Color.RED.toLong()))
        repository.select(projectId, documentB.id)
        database.patternAnnotationDao().insert(annotation(layerB.id, Color.BLUE.toLong()))
    }

    private suspend fun assertOwnedDataIsIsolated(
        projectId: Long,
        documentA: ProjectDocument,
        documentB: ProjectDocument,
    ) {
        val bookmarksA =
            database
                .patternBookmarkDao()
                .observeForProjectDocument(
                    projectId,
                    documentA.documentKey,
                ).first()
        val bookmarksB =
            database
                .patternBookmarkDao()
                .observeForProjectDocument(
                    projectId,
                    documentB.documentKey,
                ).first()
        assertEquals(listOf("Body"), bookmarksA.map { it.name })
        assertEquals(listOf("Sleeve"), bookmarksB.map { it.name })
        val layerA =
            requireNotNull(database.patternAnnotationLayerDao().getProjectLayer(projectId, documentA.documentKey))
        val layerB =
            requireNotNull(database.patternAnnotationLayerDao().getProjectLayer(projectId, documentB.documentKey))
        assertEquals(
            Color.RED.toLong(),
            database
                .patternAnnotationDao()
                .getForLayers(listOf(layerA.id))
                .single()
                .zIndex,
        )
        assertEquals(
            Color.BLUE.toLong(),
            database
                .patternAnnotationDao()
                .getForLayers(listOf(layerB.id))
                .single()
                .zIndex,
        )
    }

    private fun renderEach(vararg documents: ProjectDocument) {
        documents.forEach { document ->
            PdfPageRenderer(context, Uri.parse(document.localPdfUri)).use { renderer ->
                assertEquals(1, renderer.pageCount)
                val bitmap = renderer.renderPage(0, 240)
                assertTrue(bitmap.width > 0)
                assertTrue(bitmap.height > 0)
                bitmap.recycle()
            }
        }
    }

    private fun createPdf(
        name: String,
        color: Int,
    ): File {
        val file = File(filesDir, name)
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(300, 400, 1).create())
            page.canvas.drawColor(color)
            page.canvas.drawText(name, 24f, 48f, Paint().apply { this.color = Color.WHITE })
            document.finishPage(page)
            FileOutputStream(file).use(document::writeTo)
        } finally {
            document.close()
        }
        return file
    }

    private fun bookmark(
        projectId: Long,
        documentKey: String,
        name: String,
    ) = PatternBookmarkEntity(
        projectId = projectId,
        documentKey = documentKey,
        name = name,
        pageIndex = 0,
        yFraction = 0.4f,
        createdAt = System.currentTimeMillis(),
    )

    private fun annotation(
        layerId: Long,
        zIndex: Long,
    ): PatternAnnotationEntity {
        // Lukupolku hylkää dekoodautumattoman payloadin, joten fixture on oikea vapaakäsiviiva.
        val payload =
            requireNotNull(
                PatternAnnotationPayloadCodec.encode(
                    PatternAnnotationKind.FREEHAND,
                    FreehandPayload(
                        listOf(NormalizedPatternPoint(0.1f, 0.1f), NormalizedPatternPoint(0.9f, 0.9f)),
                        0,
                        2f,
                    ),
                ),
            )
        return PatternAnnotationEntity(
            layerId = layerId,
            page = 0,
            kind = PatternAnnotationKind.FREEHAND.name,
            payloadVersion = payload.payloadVersion,
            payloadJson = payload.payloadJson,
            zIndex = zIndex,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    private companion object {
        private const val TEST_DATABASE = "project-document-runtime.db"
    }
}
