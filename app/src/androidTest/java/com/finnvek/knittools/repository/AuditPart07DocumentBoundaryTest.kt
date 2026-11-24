package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import com.finnvek.knittools.data.local.PatternBookmarkEntity
import com.finnvek.knittools.data.storage.writeTestPdf
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.createCalibrationRowMarkers
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import com.finnvek.knittools.domain.model.ProjectDocument
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Each parameter exercises its authored boundary against production repositories and SQLite. */
@RunWith(Parameterized::class)
class AuditPart07DocumentBoundaryTest(
    private val prompt: Int,
) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: KnitToolsDatabase
    private lateinit var fixture: RoomCounterTestFixture
    private lateinit var gate: DocumentBoundaryTransactions
    private lateinit var documents: ProjectDocumentRepository
    private lateinit var bookmarks: PatternBookmarkRepository
    private lateinit var counter: CounterRepository
    private lateinit var a: ProjectDocument
    private lateinit var b: ProjectDocument
    private lateinit var c: ProjectDocument
    private lateinit var directory: File
    private lateinit var originalFiles: Map<String, List<Byte>>
    private var projectId = 0L
    private var otherProjectId = 0L
    private var bookmarkId = 0L
    private var otherBookmarkId = 0L
    private val operation = 1007 + (prompt - 1007) / 4 * 4
    private val originalMapping = serializeMapping(listOf(RowMarker(99, 2, 0.9f)))
    private val requestedMapping: String
        get() =
            serializeMapping(
                parseMapping(originalMapping) +
                    if (operation == 1019) {
                        listOf(RowMarker(10, 0, 0.65f))
                    } else {
                        requireNotNull(createCalibrationRowMarkers(10, 0, 0.2f, 20, 0, 0.8f))
                    },
            )

    @Before
    fun setUp() =
        runBlocking {
            check(context.packageName == "com.finnvek.knittools.activepolicytest")
            database = auditDatabase(context)
            fixture = RoomCounterTestFixture(database, context)
            gate = DocumentBoundaryTransactions(fixture.transactionRunner)
            documents =
                ProjectDocumentRepository(
                    database.projectDocumentDao(),
                    database.counterProjectDao(),
                    fixture.savedPatternRepository,
                    PatternAnnotationLayerRepository(database.patternAnnotationLayerDao(), fixture.transactionRunner),
                    gate,
                    ProjectDocumentFileAvailability(context, Dispatchers.IO),
                )
            counter = fixture.counterRepository(documents = documents, transactions = gate)
            bookmarks =
                PatternBookmarkRepository(
                    database.patternBookmarkDao(),
                    documents,
                    gate,
                    Dispatchers.IO,
                )
            projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "Audit owner", count = 10))
            otherProjectId = database.counterProjectDao().insert(CounterProjectEntity(name = "Audit other"))
            directory = File(context.filesDir, "pattern_pdfs/audit-part07-$prompt").apply { mkdirs() }
            val files =
                (0..2).map { index ->
                    File(directory, "$index.pdf").also { file ->
                        writeTestPdf(file, 100, 100, color = null)
                    }
                }
            val added =
                files.mapIndexed { index, file ->
                    (
                        documents.addImportedPdf(projectId, Uri.fromFile(file).toString(), "Chart $index")
                            as ProjectDocumentMutationResult.Added
                    ).document
                }
            a = added[0]
            b = added[1]
            c = added[2]
            if (operation == 1011) {
                // The new competing relation must be what preserves the file, not a Library reference.
                fixture.savedPatternRepository.deleteById(requireNotNull(a.savedPatternId))
                a = requireNotNull(documents.getDocument(a.id))
                assertNull(a.savedPatternId)
                if (prompt == 1012 || prompt == 1014) addSharedReference()
            }
            documents.updateViewerState(projectId, a.id, 0, originalMapping, true, 0.4f, true, true, 0.3f)
            val layer = requireNotNull(database.patternAnnotationLayerDao().getProjectLayer(projectId, a.documentKey))
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
            database.patternAnnotationDao().insert(
                PatternAnnotationEntity(
                    layerId = layer.id,
                    page = 0,
                    kind = PatternAnnotationKind.FREEHAND.name,
                    payloadVersion = payload.payloadVersion,
                    payloadJson = payload.payloadJson,
                    zIndex = 0,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            )
            bookmarkId =
                database.patternBookmarkDao().insert(
                    PatternBookmarkEntity(
                        projectId = projectId,
                        documentKey = a.documentKey,
                        name = "Original",
                        pageIndex = 0,
                        yFraction = 0.4f,
                        createdAt = 1,
                    ),
                )
            otherBookmarkId =
                database.patternBookmarkDao().insert(
                    PatternBookmarkEntity(
                        projectId = projectId,
                        documentKey = b.documentKey,
                        name = "Original",
                        pageIndex = 0,
                        yFraction = 0.4f,
                        createdAt = 1,
                    ),
                )
            originalFiles = fileSnapshot()
        }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        if (::directory.isInitialized) directory.deleteRecursively()
    }

    @Test
    fun authoredDocumentBoundary() =
        runBlocking {
            withTimeout(30_000) {
                when ((prompt - 1007) % 4) {
                    0 -> admission()
                    1 -> ordinaryFailure()
                    2 -> overlappingDeliveries()
                    3 -> cancellation()
                }
                assertEquals("PDF bytes and files are preserved", originalFiles, fileSnapshot())
            }
        }

    private suspend fun mutate(): Any =
        when (operation) {
            1007 -> documents.setPrimary(projectId, b.id)
            1011 -> documents.remove(projectId, a.id)
            1015 -> counter.commitManualReadingLinePosition(projectId, 0.65f, a.id)
            1019, 1023 -> counter.updatePatternRowMappingIfUnchanged(projectId, requestedMapping, a.id, originalMapping)
            1027 -> bookmarks.add(projectId, a.documentKey, "  Same name  ", 1, 0.65f)
            1031 -> bookmarks.delete(projectId, a.documentKey, bookmarkId)
            else -> error("Unexpected operation $operation")
        }

    private suspend fun ordinaryFailure() {
        val before = database.auditSnapshot()
        val trigger =
            when (operation) {
                1007 -> "BEFORE UPDATE OF isPrimary ON project_documents WHEN NEW.id = ${b.id} AND NEW.isPrimary = 1"
                1011 -> "BEFORE UPDATE OF sortOrder ON project_documents WHEN NEW.id = ${c.id}"
                1015, 1019, 1023 -> "BEFORE UPDATE OF rowMapping ON project_documents WHEN NEW.id = ${a.id}"
                1027 -> "BEFORE INSERT ON pattern_bookmarks"
                1031 -> "BEFORE DELETE ON pattern_bookmarks WHEN OLD.id = $bookmarkId"
                else -> error("Unexpected operation")
            }
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER audit_document_failure $trigger BEGIN SELECT RAISE(ABORT, 'audit final owning write'); END",
        )
        val result = runCatching { mutate() }
        if (operation == 1007 || operation == 1011) {
            assertEquals(ProjectDocumentMutationResult.PersistenceFailure, result.getOrThrow())
        } else {
            assertTrue("Owning SQLite statement must fail", result.isFailure)
            assertTrue(result.exceptionOrNull().toString().contains("audit final owning write"))
        }
        assertEquals("Ordinary failure rolls back every table", before, database.auditSnapshot())
        assertEquals(originalFiles, fileSnapshot())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER audit_document_failure")
        assertSuccess(mutate())
        assertMutationState()
        assertUnrelated(before)
    }

    private suspend fun cancellation() =
        coroutineScope {
            val before = database.auditSnapshot()
            gate.arm(DocumentPhase.BEFORE_COMMIT)
            val cancelled = async(Dispatchers.IO) { mutate() }
            gate.reached.await()
            cancelled.cancelAndJoin()
            assertTrue(cancelled.isCancelled)
            assertEquals("Cancelled transaction leaves all tables intact", before, database.auditSnapshot())
            assertEquals(originalFiles, fileSnapshot())
            gate.arm(DocumentPhase.AFTER_COMMIT)
            val late = async(Dispatchers.IO) { mutate() }
            gate.reached.await()
            assertMutationState()
            val durable = database.auditSnapshot()
            late.cancelAndJoin()
            assertTrue(late.isCancelled)
            assertEquals("Late cancellation cannot revert committed rows", durable, database.auditSnapshot())
            assertUnrelated(before)
        }

    private suspend fun admission() =
        coroutineScope {
            gate.arm(DocumentPhase.BEFORE_WRITE)
            val pending = async(Dispatchers.IO) { mutate() }
            gate.reached.await()
            when (operation) {
                1011 -> {
                    addSharedReference()
                }

                1015, 1023 -> {
                    assertEquals(ProjectDocumentMutationResult.Selected, documents.select(projectId, b.id))
                }

                1019 -> {
                    fixture.counterRepository().applyMainCounterChange(projectId, MainCounterChange.Increment)
                    fixture.counterRepository().updatePatternRowMapping(
                        projectId,
                        serializeMapping(parseMapping(originalMapping) + RowMarker(11, 1, 0.5f)),
                        a.id,
                    )
                }

                1027 -> {
                    assertTrue(documents.remove(projectId, a.id) is ProjectDocumentMutationResult.Removed)
                }

                1031 -> {
                    assertEquals(ProjectDocumentMutationResult.Selected, documents.select(projectId, b.id))
                }

                else -> {
                    error("No admission prompt for this operation")
                }
            }
            val competingState = database.auditSnapshot()
            val untouchedB = documents.getDocument(b.id)
            gate.release.complete(Unit)
            val result = pending.await()
            when (operation) {
                1019 -> {
                    assertEquals(false, result)
                    assertEquals(11, database.counterProjectDao().getProject(projectId)?.count)
                    assertEquals(competingState, database.auditSnapshot())
                    // A count-only change does not reinterpret the explicit captured row as the new count.
                    val freshMapping = requireNotNull(documents.getDocument(a.id)).rowMapping
                    val confirmed = serializeMapping(parseMapping(freshMapping) + RowMarker(10, 0, 0.65f))
                    assertTrue(counter.updatePatternRowMappingIfUnchanged(projectId, confirmed, a.id, freshMapping))
                    val stored = parseMapping(requireNotNull(documents.getDocument(a.id)).rowMapping)
                    assertTrue(stored.contains(RowMarker(10, 0, 0.65f)))
                    assertTrue(stored.contains(RowMarker(11, 1, 0.5f)))
                    assertTrue(stored.contains(RowMarker(99, 2, 0.9f)))
                    assertEquals(11, database.counterProjectDao().getProject(projectId)?.count)
                    assertUnrelated(competingState)
                }

                1027, 1031 -> {
                    assertEquals(PatternBookmarkMutationResult.StaleDocument, result)
                    assertEquals(competingState, database.auditSnapshot())
                    assertNotNull(database.patternBookmarkDao().getById(bookmarkId))
                    assertNotNull(database.patternBookmarkDao().getById(otherBookmarkId))
                }

                else -> {
                    assertSuccess(result)
                    assertMutationState()
                    assertUnrelated(competingState)
                    if (operation == 1015 || operation == 1023) {
                        assertEquals(
                            "Late callback updates its initiating relation only",
                            untouchedB,
                            documents.getDocument(b.id),
                        )
                        assertEquals(b.id, documents.getActiveDocument(projectId)?.id)
                    }
                }
            }
        }

    private suspend fun overlappingDeliveries() =
        coroutineScope {
            val before = database.auditSnapshot()
            if (operation == 1011) {
                // Addition occurs after authoritative deletion but before its physical cleanup.
                gate.arm(DocumentPhase.AFTER_COMMIT)
                val removal = async(Dispatchers.IO) { mutate() }
                gate.reached.await()
                addSharedReference()
                gate.release.complete(Unit)
                assertSuccess(removal.await())
                assertMutationState()
                assertEquals(1, documents.getDocuments(otherProjectId).size)
                return@coroutineScope
            }
            gate.arm(DocumentPhase.BEFORE_COMMIT)
            val first = async(Dispatchers.IO) { mutate() }
            gate.reached.await()
            val secondStarted = CompletableDeferred<Unit>()
            val second =
                async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                    secondStarted.complete(Unit)
                    if (operation == 1007) documents.setPrimary(projectId, c.id) else mutate()
                }
            secondStarted.await()
            gate.release.complete(Unit)
            assertSuccess(first.await())
            val next = second.await()
            when (operation) {
                1007 -> {
                    assertEquals(ProjectDocumentMutationResult.PrimaryChanged, next)
                    assertEquals(c.id, documents.getPrimary(projectId)?.id)
                    assertEquals(a.id, documents.getActiveDocument(projectId)?.id)
                    assertEquals(1, documents.getDocuments(projectId).count { it.isPrimary })
                }

                1019, 1023 -> {
                    assertEquals("Old mapping generation cannot overwrite first delivery", false, next)
                    assertMutationState()
                }

                1031 -> {
                    assertEquals(PatternBookmarkMutationResult.NotFound, next)
                    assertMutationState()
                }

                else -> {
                    assertSuccess(next)
                    assertMutationState(repeated = operation == 1027)
                }
            }
            assertUnrelated(before)
        }

    private suspend fun addSharedReference() {
        assertTrue(
            documents.addImportedPdf(
                otherProjectId,
                a.localPdfUri,
                "Shared chart",
            ) is ProjectDocumentMutationResult.Added,
        )
        assertEquals(a.localPdfUri, documents.getDocuments(otherProjectId).single().localPdfUri)
    }

    private fun assertSuccess(result: Any) {
        when (operation) {
            1007 -> assertEquals(ProjectDocumentMutationResult.PrimaryChanged, result)
            1011 -> assertTrue(result is ProjectDocumentMutationResult.Removed)
            1015 -> assertEquals(Unit, result)
            1019, 1023 -> assertEquals(true, result)
            1027, 1031 -> assertTrue(result is PatternBookmarkMutationResult.Success)
        }
    }

    private suspend fun assertMutationState(repeated: Boolean = false) {
        val current = documents.getDocument(a.id)
        when (operation) {
            1007 -> {
                assertEquals(b.id, documents.getPrimary(projectId)?.id)
                assertEquals(a.id, documents.getActiveDocument(projectId)?.id)
                assertEquals(1, documents.getDocuments(projectId).count { it.isPrimary })
            }

            1011 -> {
                assertNull(current)
                assertEquals(listOf(b.id, c.id), documents.getDocuments(projectId).map { it.id })
                assertEquals(listOf(0, 1), documents.getDocuments(projectId).map { it.sortOrder })
                assertEquals(b.id, documents.getPrimary(projectId)?.id)
                assertEquals(b.id, documents.getActiveDocument(projectId)?.id)
                assertNotNull(database.patternBookmarkDao().getById(bookmarkId))
            }

            1015 -> {
                assertEquals(0.65f, requireNotNull(current).readingLineYFraction)
                assertFalse(current.readingLineFollowCurrentRow)
                assertEquals(originalMapping, current.rowMapping)
                assertEquals(0, current.currentPage)
                assertEquals(0.3f, current.verticalReadingGuideXFraction)
            }

            1019, 1023 -> {
                assertEquals(parseMapping(requestedMapping), parseMapping(requireNotNull(current).rowMapping))
                assertEquals(RowMarker(99, 2, 0.9f), parseMapping(current.rowMapping).single { it.page == 2 })
                assertEquals(if (operation == 1019) 0.65f else 0.2f, current.readingLineYFraction)
                assertTrue(current.readingLineFollowCurrentRow)
                assertEquals(0, current.currentPage)
            }

            1027 -> {
                val rows = database.patternBookmarkDao().observeForProjectDocument(projectId, a.documentKey).first()
                val added = rows.filter { it.id != bookmarkId }
                assertEquals(if (repeated) 2 else 1, added.size)
                added.forEach {
                    assertEquals("Same name", it.name)
                    assertEquals(a.documentKey, it.documentKey)
                    assertEquals(1, it.pageIndex)
                    assertEquals(0.65f, it.yFraction)
                }
                assertEquals(added.size, added.map { it.id }.distinct().size)
            }

            1031 -> {
                assertNull(database.patternBookmarkDao().getById(bookmarkId))
                assertNotNull(database.patternBookmarkDao().getById(otherBookmarkId))
            }
        }
    }

    private fun assertUnrelated(before: Map<String, List<List<String?>>>) {
        val allowed =
            when (operation) {
                1011 -> setOf("project_documents", "pattern_annotation_layers")
                1027, 1031 -> setOf("pattern_bookmarks")
                else -> setOf("project_documents")
            }
        val after = database.auditSnapshot()
        before.filterKeys { it !in allowed }.forEach { (table, rows) -> assertEquals(table, rows, after[table]) }
    }

    private fun fileSnapshot(): Map<String, List<Byte>> =
        directory
            .walkTopDown()
            .filter { it.isFile }
            .associate { it.relativeTo(directory).path to it.readBytes().toList() }

    private enum class DocumentPhase { BEFORE_WRITE, BEFORE_COMMIT, AFTER_COMMIT }

    private class DocumentBoundaryTransactions(
        private val room: DatabaseTransactionRunner,
    ) : BoundaryPause<DocumentPhase>(),
        DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            val selected = phase.getAndSet(null)
            if (selected == DocumentPhase.BEFORE_WRITE) pause()
            val result =
                room.run {
                    val value = block()
                    if (selected == DocumentPhase.BEFORE_COMMIT) pause()
                    value
                }
            if (selected == DocumentPhase.AFTER_COMMIT) pause()
            return result
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "prompt_{0}")
        fun prompts(): List<Array<Int>> = (1008..1034).map { arrayOf(it) }
    }
}
