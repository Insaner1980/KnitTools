package com.finnvek.knittools.repository

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternBookmarkEntity
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.RowReminderEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
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
import java.io.IOException

/** Real Room rows; each parameter is an authored audit boundary, not a repeated smoke test. */
@RunWith(Parameterized::class)
class AuditMutationBoundaryTest(
    private val prompt: Int,
) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: KnitToolsDatabase
    private lateinit var gate: BoundaryTransactions
    private lateinit var fixture: RoomCounterTestFixture
    private lateinit var saved: SavedPatternRepository
    private lateinit var documents: ProjectDocumentRepository
    private lateinit var counter: CounterRepository
    private lateinit var reminders: ReminderRepository
    private lateinit var files: List<File>
    private lateinit var originalFiles: List<List<Byte>>
    private var now = SessionTimeSnapshot(1_700_000_000_000L, 5_000L, 7L, "Europe/Helsinki")
    private var patternA = 0L
    private var patternB = 0L
    private var revision = 0L
    private var documentId = 0L
    private var token = ""
    private var interval = ""
    private var originalDocuments = emptyList<ProjectDocumentEntity>()
    private val operation = if (prompt < 911) 907 else prompt - (prompt - 911) % 4

    @Before
    fun setUp() =
        runBlocking {
            check(context.packageName.endsWith(".activepolicytest"))
            database = auditDatabase(context)
            fixture = RoomCounterTestFixture(database, context)
            gate = BoundaryTransactions(RoomDatabaseTransactionRunner(database))
            val documentDao =
                object : ProjectDocumentDao by database.projectDocumentDao() {
                    override suspend fun updateLabel(
                        documentId: Long,
                        projectId: Long,
                        label: String,
                        updatedAt: Long,
                    ): Int =
                        gate.statement {
                            database.projectDocumentDao().updateLabel(
                                documentId,
                                projectId,
                                label,
                                updatedAt,
                            )
                        }
                }
            val sessionDao =
                object : SessionDao by database.sessionDao() {
                    override suspend fun deleteById(id: Long) = gate.statement { database.sessionDao().deleteById(id) }
                }
            saved =
                SavedPatternRepository(
                    database.savedPatternDao(),
                    context,
                    database.counterProjectDao(),
                    gate,
                    Dispatchers.IO,
                    database.projectDocumentDao(),
                )
            documents =
                ProjectDocumentRepository(
                    documentDao,
                    database.counterProjectDao(),
                    saved,
                    PatternAnnotationLayerRepository(database.patternAnnotationLayerDao(), gate),
                    gate,
                    ProjectDocumentFileAvailability(context, Dispatchers.IO),
                )
            counter =
                CounterRepository(
                    database.counterProjectDao(),
                    database.projectCounterDao(),
                    sessionDao,
                    ProgressPhotoStorage(),
                    PatternDocumentStorage(Dispatchers.IO),
                    context,
                    fixture.yarnCardRepository,
                    saved,
                    documents,
                    database.projectFolderDao(),
                    transactionRunner = gate,
                    ioDispatcher = Dispatchers.IO,
                    sessionTimeSource =
                        object : SessionTimeSource {
                            override fun snapshot() = now
                        },
                )
            reminders =
                ReminderRepository(
                    database.rowReminderDao(),
                    database.counterProjectDao(),
                    EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager(),
                    gate,
                )
            database.counterProjectDao().insert(
                CounterProjectEntity(
                    id = 1,
                    name = "Audit owner",
                    count = 10,
                    notes = "Base",
                    notesCreated = true,
                ),
            )
            database.counterProjectDao().insert(CounterProjectEntity(id = 2, name = "Untouched", count = 20))
            files =
                (0..3).map { index ->
                    File(context.filesDir, "pattern_pdfs/0/audit-$prompt-$index.pdf").also { file ->
                        file.parentFile!!.mkdirs()
                        val pdf = PdfDocument()
                        try {
                            val page = pdf.startPage(PdfDocument.PageInfo.Builder(100 + index, 100, 1).create())
                            pdf.finishPage(page)
                            file.outputStream().use(pdf::writeTo)
                        } finally {
                            pdf.close()
                        }
                    }
                }
            originalFiles = files.map { it.readBytes().toList() }
            database.rowReminderDao().insert(
                RowReminderEntity(id = 1, projectId = 1, targetRow = 10, message = "Original"),
            )
            database.rowReminderDao().insert(
                RowReminderEntity(id = 2, projectId = 1, targetRow = 10, message = "Other"),
            )
            if (operation in 935..955) {
                assertTrue(counter.startSession(1) is StartSessionResult.Started)
                now = now.copy(wallClockMillis = now.wallClockMillis + 60_000, elapsedRealtimeMillis = 65_000)
                counter.checkpointActiveSession()
                token = database.sessionDao().getActiveSession()!!.sessionToken
                if (operation >= 943) {
                    now = now.copy(bootCount = 8, elapsedRealtimeMillis = 2_000)
                    counter.refreshActiveSession()
                    interval = database.sessionDao().getActiveSession()!!.recoveryIntervalToken!!
                }
            }
            if (operation == 959) {
                database.sessionDao().insert(
                    SessionEntity(
                        id = 1,
                        projectId = 1,
                        startedAt = 1000,
                        endedAt = 61000,
                        startRow = 5,
                        endRow = 10,
                        durationMinutes = 1,
                    ),
                )
                database.sessionDao().insert(
                    SessionEntity(
                        id = 2,
                        projectId = 2,
                        startedAt = 1000,
                        endedAt = 61000,
                        startRow = 5,
                        endRow = 8,
                        durationMinutes = 1,
                    ),
                )
            }
            if (operation in 967..979) {
                patternA =
                    (
                        saved.createWebPattern(WebPatternInput("Original", "", "https://example.invalid/a"))
                            as WebPatternMutationResult.Created
                    ).patternId
                patternB =
                    (
                        saved.createWebPattern(WebPatternInput("Other", "", "https://example.invalid/b"))
                            as WebPatternMutationResult.Created
                    ).patternId
                revision = database.savedPatternDao().getById(patternA)!!.updatedAt
                if (operation == 967 || operation == 975) counter.attachSavedPatternMetadata(1, patternA)
            }
            if (operation in 967..979 || operation >= 999) {
                (0..2).forEach { index ->
                    val added =
                        documents.addImportedPdf(
                            1,
                            uri(index),
                            "Document $index",
                        ) as ProjectDocumentMutationResult.Added
                    documentId = added.document.id
                    documents.updateViewerState(1, documentId, index + 2, "[]", true, 0.3f, false, true, 0.7f)
                    database.patternBookmarkDao().insert(
                        PatternBookmarkEntity(
                            projectId = 1,
                            documentKey = added.document.documentKey,
                            name = "Bookmark $index",
                            pageIndex = index,
                            yFraction = 0.4f,
                            createdAt = 1234,
                        ),
                    )
                }
            }
            if (operation == 979) {
                patternA = database.projectDocumentDao().getById(documentId)!!.savedPatternId!!
            }
            originalDocuments = database.projectDocumentDao().getForProject(1)
        }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        if (::files.isInitialized) files.forEach { it.delete() }
    }

    @Test
    fun authoredMutationBoundary() =
        runBlocking {
            withTimeout(20_000) {
                when (prompt % 4) {
                    3 -> admission()
                    0 -> failure()
                    1 -> racingDeliveries()
                    2 -> cancellation()
                }
                files.forEachIndexed { index, file ->
                    assertTrue("Published or pending PDF must survive: $file", file.isFile)
                    assertEquals(originalFiles[index], file.readBytes().toList())
                }
            }
        }

    private fun uri(index: Int) = Uri.fromFile(files[index]).toString()

    private suspend fun mutate(): Any? =
        when (operation) {
            907 -> {
                val expected =
                    database
                        .rowReminderDao()
                        .getReminder(1)!!
                        .toDomain()
                        .copy(message = "Original")
                reminders.update(1, expected.copy(message = "Edited"), expected)
            }

            911 -> {
                reminders.delete(1, 1)
            }

            915 -> {
                counter.saveProjectNotes(1, "Base", "Draft")
            }

            931 -> {
                counter.startSession(1)
            }

            935 -> {
                counter.stopSession(token, 60, 0, 10)
            }

            939 -> {
                counter.discardActiveSession(token)
            }

            943 -> {
                counter.markRecoveryPromptShown(token, interval)
            }

            947 -> {
                counter.addRecoveryInterval(token, interval, 30)
            }

            951 -> {
                counter.discardRecoveryInterval(token, interval)
            }

            955 -> {
                counter.editRecoveryDurationAndStop(token, interval, 90)
            }

            959 -> {
                counter.deleteSession(1)
            }

            963 -> {
                saved.createWebPattern(WebPatternInput("New", "", "https://example.invalid/new"))
            }

            967 -> {
                saved.updateWebPattern(patternA, revision, WebPatternInput("Edited", "", "https://example.invalid/a"))
            }

            971 -> {
                counter.attachSavedPatternMetadata(1, patternA)
            }

            975 -> {
                counter.unlinkSavedPatternMetadata(1, patternA)
            }

            979 -> {
                saved.deleteById(patternA)
            }

            983 -> {
                saved.saveImportedPatternIfMissing(uri(3), "Incoming")
            }

            987 -> {
                counter.attachPattern(1, uri(3), "Incoming", 0, null)
            }

            995 -> {
                documents.addImportedPdf(1, uri(3), "Added")
            }

            999 -> {
                documents.rename(1, documentId, "Renamed")
            }

            1003 -> {
                documents.moveEarlier(1, documentId)
            }

            1007 -> {
                documents.setPrimary(1, documentId)
            }

            else -> {
                error("Unmapped prompt $prompt")
            }
        }

    private suspend fun failure() {
        val before = database.auditSnapshot()
        gate.arm(Phase.FAIL)
        val result = runCatching { mutate() }
        assertTrue("Failure hook was reached", gate.reached.isCompleted)
        assertTrue(
            "Failure must not be reported as success: $result",
            result.exceptionOrNull() is IOException || result.getOrNull().toString().contains("PersistenceFailure"),
        )
        assertEquals("All tables roll back at the last pre-commit boundary", before, database.auditSnapshot())
        val target =
            when (operation) {
                907 -> "UPDATE ON row_reminders"
                911 -> "DELETE ON row_reminders"
                915, 967, 971, 975, 987 -> "UPDATE ON counter_projects"
                931 -> "INSERT ON active_sessions"
                935, 939, 951, 955 -> "DELETE ON active_sessions"
                943, 947 -> "UPDATE ON active_sessions"
                959 -> "DELETE ON sessions"
                963, 983 -> "INSERT ON saved_patterns"
                979 -> "DELETE ON saved_patterns"
                995 -> "INSERT ON pattern_annotation_layers"
                999 -> "UPDATE ON project_documents"
                1003 -> "UPDATE ON project_documents WHEN OLD.id = ${documents.getDocuments(1)[1].id}"
                else -> error("No owning SQL write for $prompt")
            }
        val sqlite = database.openHelper.writableDatabase
        sqlite.execSQL(
            "CREATE TEMP TRIGGER audit_failure BEFORE $target BEGIN SELECT RAISE(ABORT, 'audit-$prompt'); END",
        )
        try {
            val sqlFailure = runCatching { mutate() }
            assertTrue(
                "Owning SQLite write must reject: $sqlFailure",
                sqlFailure.exceptionOrNull()?.message?.contains("audit-$prompt") == true ||
                    sqlFailure.getOrNull().toString().contains("PersistenceFailure"),
            )
            assertEquals("Owning DAO failure leaves no partial related rows", before, database.auditSnapshot())
        } finally {
            sqlite.execSQL("DROP TRIGGER audit_failure")
        }
    }

    private suspend fun cancellation() =
        coroutineScope {
            val before = database.auditSnapshot()
            gate.arm(Phase.BEFORE_COMMIT)
            val pending = async(Dispatchers.Default) { mutate() }
            gate.reached.await()
            pending.cancelAndJoin()
            assertTrue(pending.isCancelled)
            assertEquals("Cancellation before commit rolls back all writes", before, database.auditSnapshot())
            gate.arm(Phase.AFTER_COMMIT)
            val committed = async(Dispatchers.Default) { mutate() }
            gate.reached.await()
            assertSuccess()
            assertUnrelatedTables(before)
            val durable = database.auditSnapshot()
            committed.cancelAndJoin()
            assertTrue(committed.isCancelled)
            assertEquals("Late cancellation preserves durable work", durable, database.auditSnapshot())
        }

    private suspend fun racingDeliveries() =
        coroutineScope {
            val before = database.auditSnapshot()
            gate.arm(Phase.BEFORE_COMMIT)
            val first = async(Dispatchers.Default) { mutate() }
            gate.reached.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) { mutate() }
            gate.release.complete(Unit)
            val results = listOf(first.await(), second.await())
            assertFalse(
                "Neither racing delivery may report a storage failure: $results",
                results.any { it.toString().contains("PersistenceFailure") },
            )
            assertSuccess(repeated = true)
            assertUnrelatedTables(before)
        }

    private suspend fun admission() =
        coroutineScope {
            gate.arm(Phase.BEFORE_WRITE)
            val pending = async(Dispatchers.Default) { runCatching { mutate() } }
            gate.reached.await()
            when (operation) {
                911 -> {
                    database.rowReminderDao().update(
                        database.rowReminderDao().getReminder(2)!!.copy(message = "New alert"),
                    )
                }

                915 -> {
                    fixture.counterRepository().saveProjectNotes(1, "Base", "Other editor")
                }

                931 -> {
                    counter.startSession(2)
                }

                935 -> {
                    counter.stopSession(token, 60, 0, 10)
                }

                939 -> {
                    counter.discardActiveSession(token)
                    counter.startSession(2)
                }

                943, 955 -> {
                    database.sessionDao().updateActiveSession(
                        database.sessionDao().getActiveSession()!!.copy(recoveryIntervalToken = "replacement"),
                    )
                }

                947 -> {
                    counter.addRecoveryInterval(token, interval, 30)
                }

                951 -> {
                    counter.editRecoveryDurationAndStop(token, interval, 90)
                }

                959 -> {
                    database.sessionDao().deleteById(1)
                }

                963 -> {
                    fixture.savedPatternRepository.createWebPattern(
                        WebPatternInput("Competitor", "", "https://example.invalid/new"),
                    )
                }

                967 -> {
                    fixture.savedPatternRepository.updateWebPattern(
                        patternA,
                        revision,
                        WebPatternInput("Competitor", "", "https://example.invalid/a"),
                    )
                }

                971 -> {
                    fixture.counterRepository().attachSavedPatternMetadata(1, patternB)
                }

                975 -> {
                    fixture.counterRepository().attachSavedPatternMetadata(1, patternB, patternA)
                }

                979 -> {
                    fixture.projectDocumentRepository.addSavedPattern(2, patternA)
                }

                983 -> {
                    fixture.savedPatternRepository.saveImportedPatternIfMissing(uri(3), "Competitor")
                }

                987 -> {
                    database.counterProjectDao().delete(1)
                }

                995 -> {
                    fixture.projectDocumentRepository.addImportedPdf(1, uri(3), "Competitor")
                }

                999 -> {
                    fixture.projectDocumentRepository.remove(1, documentId)
                }

                1003 -> {
                    fixture.projectDocumentRepository.remove(1, documents.getDocuments(1).first().id)
                }

                1007 -> {
                    fixture.projectDocumentRepository.setPrimary(1, documents.getDocuments(1)[1].id)
                }

                else -> {
                    error("No competitor for $prompt")
                }
            }
            val competitorState = database.auditSnapshot()
            gate.release.complete(Unit)
            val result = pending.await()
            when (operation) {
                931 -> {
                    assertTrue(result.getOrThrow() is StartSessionResult.ProjectConflict)
                }

                935 -> {
                    assertEquals(StopSessionResult.NoActiveSession, result.getOrThrow())
                }

                939 -> {
                    assertEquals(StopSessionResult.StaleAction, result.getOrThrow())
                }

                943 -> {
                    assertEquals(false, result.getOrThrow())
                }

                947, 951, 955 -> {
                    assertEquals(RecoveryResolutionResult.StaleAction, result.getOrThrow())
                }

                963 -> {
                    assertTrue(result.getOrThrow() is WebPatternMutationResult.Duplicate)
                }

                967 -> {
                    assertEquals(WebPatternMutationResult.StaleAction, result.getOrThrow())
                }

                971 -> {
                    assertTrue(result.getOrThrow() is SavedPatternMetadataMutationResult.ReplacementRequired)
                }

                975 -> {
                    assertEquals(SavedPatternMetadataMutationResult.StaleAction, result.getOrThrow())
                }

                987 -> {
                    assertTrue(result.isFailure)
                }

                995 -> {
                    assertEquals(ProjectDocumentMutationResult.DuplicateUri, result.getOrThrow())
                }

                999 -> {
                    assertTrue(
                        result.getOrThrow() in
                            listOf(
                                ProjectDocumentMutationResult.MissingDocument,
                                ProjectDocumentMutationResult.StaleAction,
                            ),
                    )
                }

                else -> {
                    assertTrue(result.isSuccess)
                }
            }
            if (operation in listOf(931, 935, 939, 943, 947, 951, 955, 959, 963, 967, 971, 975, 983, 987, 995, 999)) {
                assertEquals(
                    "Rejected/stale/no-op delivery preserves competitor",
                    competitorState,
                    database.auditSnapshot(),
                )
            } else if (operation == 915) {
                val notes = database.counterProjectDao().getProject(1)!!.notes
                assertTrue(notes.contains("Draft"))
                assertTrue(notes.contains("Other editor"))
            } else {
                assertSuccess()
            }
        }

    private suspend fun assertSuccess(repeated: Boolean = false) {
        assertEquals(10, database.counterProjectDao().getProject(1)!!.count)
        assertEquals(20, database.counterProjectDao().getProject(2)!!.count)
        for (original in originalDocuments) {
            val current = database.projectDocumentDao().getById(original.id) ?: continue
            assertEquals(
                "Document identity and reader state survive metadata mutation",
                original,
                current.copy(
                    label = original.label,
                    sortOrder = original.sortOrder,
                    isPrimary = original.isPrimary,
                    updatedAt = original.updatedAt,
                    savedPatternId = original.savedPatternId,
                ),
            )
        }
        val active = database.sessionDao().getActiveSession()
        val history = database.sessionDao().getSessionsForProject(1).first()
        when (operation) {
            907 -> {
                assertEquals("Edited", database.rowReminderDao().getReminder(1)!!.message)
            }

            911 -> {
                assertNull(database.rowReminderDao().getReminder(1))
                assertNotNull(database.rowReminderDao().getReminder(2))
            }

            915 -> {
                assertEquals("Draft", database.counterProjectDao().getProject(1)!!.notes)
            }

            931 -> {
                assertEquals(1L, active!!.projectId)
                assertTrue(active.sessionToken.isNotBlank())
                assertTrue(history.isEmpty())
            }

            935, 951, 955 -> {
                assertNull(active)
                assertEquals(1, history.size)
                assertEquals(if (operation == 955) 90L else 60L, history.single().durationSeconds)
                assertEquals(0, history.single().rowsWorked)
                assertEquals(10, history.single().endRow)
            }

            939 -> {
                assertNull(active)
                assertTrue(history.isEmpty())
            }

            943 -> {
                assertTrue(active!!.recoveryPromptShown)
                assertEquals(60L, active.checkpointedDurationSeconds)
                assertTrue(history.isEmpty())
            }

            947 -> {
                assertEquals(90L, active!!.checkpointedDurationSeconds)
                assertNull(active.recoveryIntervalToken)
                assertTrue(history.isEmpty())
            }

            959 -> {
                assertTrue(history.isEmpty())
                assertTrue(database.sessionDao().getProjectInsightSessionBatch(1, 0).isEmpty())
                assertEquals(
                    1,
                    database
                        .sessionDao()
                        .getSessionsForProject(2)
                        .first()
                        .size,
                )
            }

            963 -> {
                val rows = database.savedPatternDao().getAllOnce()
                assertEquals(1, rows.size)
                assertNull(rows.single().localPdfUri)
                assertFalse(rows.single().isAvailableOffline)
            }

            967 -> {
                assertEquals("Edited", database.savedPatternDao().getById(patternA)!!.name)
                assertEquals("Edited", database.counterProjectDao().getProject(1)!!.patternName)
            }

            971 -> {
                assertEquals(patternA, database.counterProjectDao().getProject(1)!!.linkedPatternId)
            }

            975 -> {
                assertNull(database.counterProjectDao().getProject(1)!!.linkedPatternId)
            }

            979 -> {
                assertNull(database.savedPatternDao().getById(patternA))
                assertNull(database.projectDocumentDao().getById(documentId)!!.savedPatternId)
            }

            983 -> {
                assertEquals(1, database.savedPatternDao().getAllOnce().count { it.localPdfUri == uri(3) })
            }

            987, 995 -> {
                val rows = documents.getDocuments(1)
                assertEquals(1, rows.size)
                assertTrue(rows.single().isPrimary)
                assertEquals(uri(3), rows.single().localPdfUri)
            }

            999 -> {
                assertEquals("Renamed", documents.getDocument(documentId)!!.label)
            }

            1003 -> {
                val rows = documents.getDocuments(1)
                assertEquals(
                    if (repeated ||
                        rows.size == 2
                    ) {
                        0
                    } else {
                        1
                    },
                    rows.indexOfFirst { it.id == documentId },
                )
                assertEquals(1, rows.count { it.isPrimary })
            }

            1007 -> {
                assertEquals(listOf(documentId), documents.getDocuments(1).filter { it.isPrimary }.map { it.id })
            }
        }
    }

    private fun assertUnrelatedTables(before: Map<String, List<List<String?>>>) {
        val allowed =
            when (operation) {
                907, 911 -> {
                    setOf("row_reminders")
                }

                915 -> {
                    setOf("counter_projects")
                }

                in 931..959 -> {
                    setOf("active_sessions", "sessions")
                }

                963, 983 -> {
                    setOf("saved_patterns")
                }

                967, 971, 975 -> {
                    setOf("saved_patterns", "counter_projects")
                }

                979 -> {
                    setOf(
                        "saved_patterns",
                        "counter_projects",
                        "project_documents",
                        "pattern_annotation_layers",
                        "pattern_annotations",
                    )
                }

                987, 995 -> {
                    setOf("saved_patterns", "counter_projects", "project_documents", "pattern_annotation_layers")
                }

                else -> {
                    setOf("project_documents")
                }
            }
        val after = database.auditSnapshot()
        before.filterKeys { it !in allowed }.forEach { (table, rows) -> assertEquals(table, rows, after[table]) }
    }

    private enum class Phase { BEFORE_WRITE, FAIL, BEFORE_COMMIT, AFTER_COMMIT }

    private class BoundaryTransactions(
        private val room: DatabaseTransactionRunner,
    ) : BoundaryPause<Phase>(),
        DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            val selected = phase.getAndSet(null)
            if (selected == Phase.BEFORE_WRITE) pause()
            val result =
                room.run {
                    val value = block()
                    beforeCommit(selected)
                    value
                }
            if (selected == Phase.AFTER_COMMIT) pause()
            return result
        }

        suspend fun <T> statement(block: suspend () -> T): T {
            val selected = phase.getAndSet(null)
            if (selected == Phase.BEFORE_WRITE) pause()
            beforeCommit(selected)
            val result = block()
            if (selected == Phase.AFTER_COMMIT) pause()
            return result
        }

        private suspend fun beforeCommit(selected: Phase?) {
            if (selected == Phase.FAIL) {
                reached.complete(Unit)
                throw IOException("Injected storage failure at final pre-commit boundary")
            }
            if (selected == Phase.BEFORE_COMMIT) pause()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "prompt_{0}")
        fun prompts(): List<Array<Int>> = ((908..918) + (931..990) + (995..1007)).map { arrayOf(it) }
    }
}
