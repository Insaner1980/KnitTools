package com.finnvek.knittools.repository

import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.backup.BackupArchive
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupFormat
import com.finnvek.knittools.data.backup.BackupLimits
import com.finnvek.knittools.data.backup.BackupManifest
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.data.backup.BackupProviderIo
import com.finnvek.knittools.data.backup.BackupRestoreFiles
import com.finnvek.knittools.data.backup.BackupTables
import com.finnvek.knittools.data.backup.ContentResolverBackupProviderIo
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.MAX_COMPLETED_SESSIONS
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.domain.calculator.evaluateActiveSessionTime
import com.finnvek.knittools.domain.model.ActiveSessionTimeEvaluation
import com.finnvek.knittools.domain.model.CounterHistoryAction
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var db: KnitToolsDatabase
    private lateinit var repository: BackupRepository
    private lateinit var patternFiles: PatternFileReferenceCoordinator
    private lateinit var yarnCards: YarnCardRepository
    private var processProbe: BackupRestoreProcessProbe? = null
    private var selectionId = UUID.randomUUID()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun prepare(uri: Uri): BackupPreview {
        selectionId = UUID.randomUUID()
        return repository.prepare(uri, selectionId)
    }

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val processPhase = BackupRestoreProcessProbe.phase()
        directory = File(app.cacheDir, processPhase?.let { "backup-process-$it" } ?: "backup-test-${UUID.randomUUID()}")
        if (processPhase != null) {
            val verifying = InstrumentationRegistry.getArguments().getString("backupCrashVerify") == "true"
            check(directory.exists() == verifying)
            processProbe = BackupRestoreProcessProbe(directory)
        }
        directory.mkdirs()
        context =
            object : ContextWrapper(app) {
                override fun getApplicationContext(): Context = this

                override fun getFilesDir() = File(directory, "files").apply { mkdirs() }

                override fun getNoBackupFilesDir() = File(directory, "private").apply { mkdirs() }
            }
        db =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, File(directory, "live.db").absolutePath)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .apply { processProbe?.let { openHelperFactory(it.factory) } }
                .build()
        patternFiles = PatternFileReferenceCoordinator()
        yarnCards =
            YarnCardRepository(
                db.yarnCardDao(),
                db.counterProjectDao(),
                context,
                RoomDatabaseTransactionRunner(db),
                Dispatchers.IO,
            )
        repository = createRepository(ContentResolverBackupProviderIo(context))
    }

    @After fun tearDown() {
        db.close()
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test fun restoredSharedCardCannotStealAnotherNotesUsage() =
        runBlocking {
            seed()
            db.openHelper.writableDatabase.execSQL(
                "UPDATE project_yarn_usage SET plannedMeters = 1200, allocatedMeters = 600, usedMeters = 350, " +
                    "metersPerSkein = 200, gramsPerSkein = 100, sourceNameSnapshot = 'First note usage'",
            )
            val archive = File(directory, "shared-card.knittools-backup")
            repository.export(archive.toUri())
            val payload = File(directory, "shared-card-payload").apply { mkdirs() }
            val manifest = BackupArchive.extract(archive, payload)
            val table = File(payload, "tables/project_yarn_notes.jsonl")
            val lines = table.readLines()
            val header = BackupFormat.json.parseToJsonElement(lines.first()).jsonArray
            val second =
                BackupFormat.json
                    .parseToJsonElement(lines[1])
                    .jsonArray
                    .toMutableList()
            second[header.indexOf(JsonPrimitive("id"))] = JsonPrimitive(2)
            second[header.indexOf(JsonPrimitive("name"))] = JsonPrimitive("Second note")
            table.appendText(JsonArray(second).toString() + "\n")
            rewriteArchive(payload, archive, manifest)
            prepare(archive.toUri())
            repository.restore(selectionId)

            val project = db.counterProjectDao().getAllProjectsOnce().single()
            val notes = db.projectYarnNoteDao().observeForProject(project.id).first()
            assertEquals(2, notes.size)
            val secondNote = notes.single { it.name == "Second note" }
            val before = requireNotNull(db.projectYarnUsageDao().getProject(project.id)).usages.single().usage
            assertTrue(before.projectYarnNoteId != secondNote.id)
            assertEquals(before.yarnCardId, secondNote.savedYarnCardId)
            val noteRepository =
                ProjectYarnNoteRepository(
                    db.projectYarnNoteDao(),
                    yarnCards,
                    RoomDatabaseTransactionRunner(db),
                    db.projectYarnUsageDao(),
                )
            val result = noteRepository.saveToMyYarn(secondNote.id)
            assertEquals(before, db.projectYarnUsageDao().getById(before.id))
            assertEquals(null, result)
            assertEquals(notes, db.projectYarnNoteDao().observeForProject(project.id).first())
            assertEquals(project, db.counterProjectDao().getProject(project.id))
            val items = requireNotNull(db.projectYarnUsageDao().getProject(project.id)).items()
            assertEquals(null, items.single { it.source.projectYarnNoteId == secondNote.id }.usage)
            assertEquals(1, items.count { it.usage?.id == before.id })
            assertEquals(1L, number("SELECT COUNT(*) FROM yarn_cards"))
            assertEquals(1L, number("SELECT COUNT(*) FROM project_yarn_usage"))
        }

    @Test fun allEighteenTablesFilesRelationshipsAndCompletionEventsSurviveRealReplacement() =
        runBlocking {
            seed()
            val originalProject =
                db.counterProjectDao().getAllProjectsOnce().single().copy(
                    craftType = "CROCHET",
                    mainCounterLabelType = "CUSTOM",
                    mainCounterCustomLabel = "Kerrokset",
                    readingLineEnabled = true,
                    readingLineYFraction = 0.25f,
                    readingLineFollowCurrentRow = false,
                    verticalReadingGuideEnabled = true,
                    verticalReadingGuideXFraction = 0.75f,
                    secondaryCount = 17,
                    secondaryCounterUsed = true,
                    stepSize = 7,
                    notesCreated = true,
                    createdAt = 1_600_000_000_000L,
                    updatedAt = 1_800_000_000_000L,
                    sectionName = "Hihan osa",
                    stitchCount = 99,
                    totalRows = 123,
                    patternName = "Virkattu hihansuu",
                    currentPatternPage = 2,
                    patternRowMapping = """[{"row":12,"page":2,"yPosition":0.25}]""",
                    stitchTrackingEnabled = true,
                    currentStitch = 11,
                    targetRows = 222,
                )
            db.counterProjectDao().update(originalProject)
            listOf(Long.MIN_VALUE, Long.MAX_VALUE).forEach { timestamp ->
                db.counterProjectDao().insertHistory(
                    CounterHistoryEntity(
                        projectId = originalProject.id,
                        action = "future-action",
                        previousValue = Int.MIN_VALUE,
                        newValue = Int.MAX_VALUE,
                        timestamp = timestamp,
                    ),
                )
            }
            val originalHistory = db.counterProjectDao().observeCounterHistory(originalProject.id).first()
            val archive = File(directory, "backup.knittools-backup")
            repository.export(archive.toUri())
            if (InstrumentationRegistry.getArguments().getString("backupFixture") == "true") {
                val app = ApplicationProvider.getApplicationContext<Context>()
                archive.copyTo(File(app.cacheDir, "backup-device-fixture.knittools-backup"), overwrite = true)
            }
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Later edit'")
            val preview = prepare(archive.toUri())
            assertEquals(1, preview.projects)
            assertEquals(2, preview.patterns)
            assertEquals(1, preview.yarnCards)
            assertEquals(4, preview.files)
            processProbe?.arm(db.openHelper.writableDatabase, context)
            repository.restore(selectionId)
            BackupFormat.tables.forEach {
                assertEquals(
                    when (it) {
                        "counter_history" -> 3L
                        "saved_patterns", "project_documents" -> 2L
                        else -> 1L
                    },
                    number("SELECT COUNT(*) FROM `$it`"),
                )
            }
            val project = db.counterProjectDao().getAllProjectsOnce().single()
            assertTrue(project.id > 1)
            assertEquals("Cardigan", project.name)
            assertEquals("Private notes", project.notes)
            assertEquals(12, project.count)
            val restoredHistory = counterHistoryTestRepository(db, context).observeCounterHistory(project.id).first()
            assertEquals(3, restoredHistory.size)
            val restoredEntities = db.counterProjectDao().observeCounterHistory(project.id).first()
            originalHistory.zip(restoredEntities).forEach { (original, restored) ->
                assertTrue(restored.id > originalHistory.maxOf { it.id })
                assertEquals(original.copy(id = restored.id, projectId = project.id), restored)
            }
            val increased = restoredHistory.single { it.action == CounterHistoryAction.INCREASE }
            assertEquals(1_700_000_000_000L, increased.timestamp)
            assertEquals(11 to 12, increased.let { it.previousValue to it.newValue })
            assertEquals(number("SELECT id FROM counter_history WHERE action = 'increment'"), increased.id)
            assertEquals(
                number("SELECT previousValue FROM counter_history WHERE action = 'increment'").toInt(),
                increased.previousValue,
            )
            assertEquals(
                number("SELECT newValue FROM counter_history WHERE action = 'increment'").toInt(),
                increased.newValue,
            )
            assertEquals(2, restoredHistory.count { it.action == CounterHistoryAction.CHANGED })

            assertEquals(project.id, number("SELECT projectId FROM project_completions"))
            assertEquals(1_700_000_000_000L, number("SELECT completedAt FROM project_completions"))
            assertEquals(project.id, number("SELECT projectId FROM pattern_bookmarks"))
            assertEquals(number("SELECT id FROM yarn_cards"), number("SELECT yarnCardId FROM project_yarn_usage"))
            assertEquals(
                text("SELECT documentKey FROM pattern_annotation_layers"),
                text("SELECT documentKey FROM pattern_bookmarks"),
            )
            val active = db.sessionDao().getActiveSession()
            assertEquals(null, active?.bootCount)
            assertEquals("BOOT_IDENTITY_UNAVAILABLE", active?.recoveryReason)
            assertEquals(null, active?.recoverySuggestedDurationSeconds)
            assertEquals(120L, active?.checkpointedDurationSeconds)
            val evaluation =
                evaluateActiveSessionTime(
                    checkNotNull(active).toDomain().timingAnchors,
                    SessionTimeSnapshot(1_900_000_000_000L, 8_000_000L, 1, "Europe/Helsinki"),
                )
            assertTrue(evaluation is ActiveSessionTimeEvaluation.NeedsReview)
            val references = BackupTables.references(db.openHelper.writableDatabase)
            assertEquals(4, references.size)
            references.forEach { assertTrue(AppFileStorage.resolveAppOwnedFile(context, it.toUri())?.isFile == true) }
            val pdfUri = text("SELECT localPdfUri FROM project_documents WHERE isPrimary = 1")
            assertEquals(
                originalProject.copy(
                    id = project.id,
                    yarnCardIds = number("SELECT id FROM yarn_cards").toString(),
                    linkedPatternId = number("SELECT savedPatternId FROM project_documents WHERE isPrimary = 1"),
                    patternUri = pdfUri,
                ),
                project,
            )
            PdfRenderer(
                ParcelFileDescriptor.open(File(checkNotNull(pdfUri.toUri().path)), ParcelFileDescriptor.MODE_READ_ONLY),
            ).use {
                assertEquals(1, it.pageCount)
            }
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    fun verifyRecoveredProcessFixture() =
        runBlocking {
            val phase = checkNotNull(BackupRestoreProcessProbe.phase())
            val checkpoint = File(directory, "checkpoint.txt").readText().split(" ")
            assertFalse(checkpoint[0].toInt() == android.os.Process.myPid())
            assertEquals(phase, checkpoint[1])
            assertEquals((phase == "precommit").toString(), checkpoint[2])
            repository.recoverInterruptedOperations()
            val sql = db.openHelper.writableDatabase
            assertEquals("ok", text("PRAGMA quick_check"))
            sql.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            val after = File(directory, "after")
            BackupTables.export(sql, after, { _, _ -> }, check = {})
            BackupFormat.tables.forEach { table ->
                if (phase == "precommit") {
                    assertEquals(
                        table,
                        File(directory, "before/tables/$table.jsonl").readText(),
                        File(after, "tables/$table.jsonl").readText(),
                    )
                } else {
                    assertEquals(
                        table,
                        when (table) {
                            "counter_history" -> 3L
                            "saved_patterns", "project_documents" -> 2L
                            else -> 1L
                        },
                        number("SELECT COUNT(*) FROM `$table`"),
                    )
                }
            }
            assertEquals(
                if (phase ==
                    "precommit"
                ) {
                    "Later edit"
                } else {
                    "Cardigan"
                },
                text("SELECT name FROM counter_projects"),
            )
            assertEquals(12L, number("SELECT count FROM counter_projects"))
            assertEquals(11L, number("SELECT currentStitch FROM counter_projects"))
            val hashes =
                BackupTables
                    .references(sql)
                    .map { checkNotNull(AppFileStorage.resolveAppOwnedFile(context, it.toUri())).canonicalFile }
                    .distinct()
                    .map { file ->
                        assertTrue(file.isFile)
                        BackupFormat.digest(file) {}
                    }.sorted()
            assertEquals(File(directory, "file-hashes.txt").readText(), hashes.joinToString("\n"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
            val app = ApplicationProvider.getApplicationContext<Context>()
            val proof = File(app.cacheDir, "backup-process-proof-$phase")
            check(!proof.exists())
            File(directory, "before").copyRecursively(File(proof, "before"))
            after.copyRecursively(File(proof, "after"))
            File(directory, "checkpoint.txt").copyTo(File(proof, "checkpoint.txt"))
            File(directory, "file-hashes.txt").copyTo(File(proof, "file-hashes.txt"))
        }

    @Test fun auditedRowsPreserveEveryColumnAfterReplacementAndDatabaseReopen() =
        runBlocking {
            seed()
            val sql = db.openHelper.writableDatabase
            sql.execSQL(
                "UPDATE yarn_cards SET brand = ?, yarnName = ?, fiberContent = '70% wool', weightGrams = '50,5', " +
                    "lengthMeters = '123.75', needleSize = '3–4 mm', gaugeInfo = '22 × 31', colorName = 'Sininen', " +
                    "colorNumber = '0007', dyeLot = 'Erä 007', weightCategory = 'Imported weight', careSymbols = ?, " +
                    "createdAt = 1600000000000, quantityInStash = 2147483647, status = 'IN_USE', linkedProjectId = 1",
                arrayOf<Any>("Å brand ".repeat(80), "Ö yarn\n第二行", Long.MIN_VALUE),
            )
            sql.execSQL(
                "UPDATE sessions SET startedAt = 1700000000000, endedAt = 1700000121000, startRow = 17, endRow = 24, " +
                    "durationMinutes = 3, durationSeconds = 121, rowsWorked = 7, zoneId = 'Europe/Helsinki'",
            )
            sql.execSQL(
                "UPDATE active_sessions SET startedAtWallMillis = 1700000000000, startRow = 10, " +
                    "lastObservedRow = 23, trustedLastObservedRow = 14, trustedRowsWorked = 4, " +
                    "pendingRowsWorked = 9, reviewedRowsWorked = 3, " +
                    "reviewedLastObservedRow = 17, unreviewedRowsWorked = 6, checkpointedDurationSeconds = 123, " +
                    "reviewedDurationBaselineSeconds = 60, segmentStartedAtWallMillis = 1700000123000, " +
                    "segmentStartedElapsedRealtimeMillis = 89000, bootCount = 7, recoveryReason = 'REBOOTED', " +
                    "recoveryIntervalToken = 'old-interval', recoverySuggestedDurationSeconds = 61, " +
                    "recoveryPromptShown = 1, updatedAtWallMillis = 1700000184000",
            )
            sql.execSQL(
                "UPDATE row_reminders SET targetRow = 2147483647, repeatInterval = 7, message = 'Älä unohda 🧶', " +
                    "isCompleted = 1, createdAt = 1600000000001",
            )
            sql.execSQL("UPDATE progress_photos SET rowNumber = 123, note = 'Kuva 🧶', createdAt = 1600000000002")
            sql.execSQL(
                "UPDATE project_counters SET name = 'Hiha 🧶', count = 7, stepSize = 3, repeatAt = 11, " +
                    "sortOrder = 5, " +
                    "createdAt = 1600000000003, counterType = 'SHAPING', startingStitches = 56, stitchChange = -2, " +
                    "shapeEveryN = 4, repeatStartRow = 2, repeatEndRow = 6, totalRepeats = 8, currentRepeat = 3, " +
                    "linkedToMainCounter = 1",
            )
            val tables =
                listOf(
                    "yarn_cards",
                    "sessions",
                    "active_sessions",
                    "row_reminders",
                    "progress_photos",
                    "project_counters",
                )

            fun row(table: String): Map<String, Any?> =
                db.openHelper.writableDatabase.query("SELECT * FROM `$table`").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    val values =
                        cursorValues(cursor)
                    assertFalse(cursor.moveToNext())
                    values
                }
            val before = tables.associateWith(::row)
            tables.forEach { table ->
                val original = before.getValue(table)
                BackupTables
                    .columns(sql, table)
                    .filterNot { it.name == "id" || it.name == "singletonId" }
                    .forEach { column ->
                        val mutation = runCatching { sql.execSQL("UPDATE `$table` SET `${column.name}` = NULL") }
                        if (column.required) {
                            assertTrue("$table.${column.name} rejects null", mutation.isFailure)
                            assertEquals(original, row(table))
                        } else {
                            mutation.getOrThrow()
                            assertEquals(original + (column.name to null), row(table))
                            sql.execSQL("UPDATE `$table` SET `${column.name}` = ?", arrayOf(original[column.name]))
                            assertEquals(original, row(table))
                        }
                    }
                if (table != "yarn_cards") {
                    assertTrue(runCatching { sql.execSQL("UPDATE `$table` SET projectId = 99999") }.isFailure)
                    assertEquals(original, row(table))
                }
            }
            val oldPhoto =
                checkNotNull(
                    AppFileStorage.resolveAppOwnedFile(
                        context,
                        (before.getValue("progress_photos")["photoUri"] as String).toUri(),
                    ),
                )
            val photoBytes = oldPhoto.readBytes()
            restoreAndReopenDatabase("audited-columns.knittools-backup")
            val projectId = number("SELECT id FROM counter_projects")
            tables.forEach { table ->
                val actual = row(table)
                val expected = before.getValue(table).toMutableMap()
                if (table == "active_sessions") {
                    expected["bootCount"] = null
                    expected["recoveryReason"] = "BOOT_IDENTITY_UNAVAILABLE"
                    expected["recoverySuggestedDurationSeconds"] = null
                    expected["recoveryPromptShown"] = 0L
                    listOf("sessionToken", "recoveryIntervalToken").forEach { token ->
                        val value = actual.getValue(token) as String
                        UUID.fromString(value)
                        assertFalse(value == expected[token])
                        expected[token] = value
                    }
                } else {
                    assertTrue((actual.getValue("id") as Long) > (expected.getValue("id") as Long))
                    expected["id"] = actual["id"]
                }
                expected[if (table == "yarn_cards") "linkedProjectId" else "projectId"] = projectId
                if (table == "yarn_cards" || table == "progress_photos") {
                    expected["photoUri"] = actual["photoUri"]
                    val uri = (actual.getValue("photoUri") as String).toUri()
                    assertTrue(checkNotNull(AppFileStorage.resolveAppOwnedFile(context, uri)).isFile)
                    if (table == "progress_photos") {
                        assertTrue(photoBytes.contentEquals(File(checkNotNull(uri.path)).readBytes()))
                    }
                }
                assertEquals(table, expected, actual)
            }
        }

    @Test fun partThreeColumnsSurviveIdentityRebasingAndDatabaseReopen() =
        runBlocking {
            seed()
            val sql = db.openHelper.writableDatabase
            sql.execSQL(
                "UPDATE project_yarn_notes SET name = ?, description = ?, quantity = 2147483647, notes = ?, " +
                    "savedYarnCardId = 1, createdAt = ?, updatedAt = ?",
                arrayOf<Any>("Ö yarn 🧶", "description\n第二行", "note ".repeat(80), Long.MIN_VALUE, Long.MAX_VALUE),
            )
            sql.execSQL(
                "UPDATE saved_patterns SET ravelryPatternId = 2147483647, name = ?, designerName = ?, " +
                    "thumbnailUrl = 'https://example.invalid/image', difficulty = ?, " +
                    "gaugeStitches = ?, gaugeRows = ?, " +
                    "needleSize = '', yarnWeight = 'Imported', yardage = 2147483647, availability = 'paid', " +
                    "originalUrl = 'https://example.invalid/original', " +
                    "canonicalUrl = 'https://example.invalid/canonical', " +
                    "isAvailableOffline = 1, savedAt = ?, updatedAt = ?, lastSyncedAt = ? WHERE id = 1",
                arrayOf<Any>(
                    "Ohje 🧶",
                    "Å designer",
                    Float.MAX_VALUE,
                    Float.MIN_VALUE,
                    -Float.MAX_VALUE,
                    Long.MIN_VALUE,
                    Long.MAX_VALUE,
                    17L,
                ),
            )
            sql.execSQL(
                "UPDATE pattern_annotation_layers SET createdAt = ?, updatedAt = ?",
                arrayOf(Long.MIN_VALUE, Long.MAX_VALUE),
            )
            sql.execSQL(
                "UPDATE pattern_annotations SET page = 1, zIndex = ?, createdAt = ?, updatedAt = ?",
                arrayOf(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE),
            )
            sql.execSQL(
                "UPDATE pattern_bookmarks SET name = 'Merkki 🧶', pageIndex = 1, yFraction = 0.875, createdAt = ?",
                arrayOf(Long.MIN_VALUE),
            )
            sql.execSQL(
                "UPDATE project_documents SET label = 'Ohje 🧶', currentPage = 1, rowMapping = '[]', " +
                    "readingLineEnabled = 1, " +
                    "readingLineYFraction = 0.25, readingLineFollowCurrentRow = 1, verticalReadingGuideEnabled = 1, " +
                    "verticalReadingGuideXFraction = 0.75, createdAt = ?, updatedAt = ? WHERE id = 1",
                arrayOf(Long.MIN_VALUE, Long.MAX_VALUE),
            )
            sql.execSQL(
                "UPDATE project_folders SET name = ?, normalizedName = ?, sortOrder = 2147483647",
                arrayOf("Å" + "x".repeat(49), "å" + "x".repeat(49)),
            )
            val tables =
                listOf(
                    "project_yarn_notes",
                    "saved_patterns",
                    "pattern_annotation_layers",
                    "pattern_annotations",
                    "pattern_bookmarks",
                    "project_documents",
                    "project_folders",
                    "project_folder_assignments",
                    "project_yarn_usage",
                )

            fun rows(table: String): List<Map<String, Any?>> =
                db.openHelper.writableDatabase.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                cursorValues(cursor),
                            )
                        }
                    }
                }
            val before = tables.associateWith(::rows)
            val pdfBytes =
                before.getValue("saved_patterns").associate { row ->
                    row.getValue("localPdfUri") to
                        File(checkNotNull((row.getValue("localPdfUri") as String).toUri().path)).readBytes()
                }
            restoreAndReopenDatabase("part-three.knittools-backup")
            val after = tables.associateWith(::rows)
            val ids =
                tables.filterNot { it == "project_folder_assignments" }.associateWith { table ->
                    val oldRows = before.getValue(table)
                    val newRows = after.getValue(table)
                    assertEquals(table, oldRows.size, newRows.size)
                    oldRows.zip(newRows).associate { (old, new) ->
                        val oldId = old.getValue("id") as Long
                        val newId = new.getValue("id") as Long
                        assertTrue("$table fresh identity", newId > (oldRows.maxOf { it.getValue("id") as Long }))
                        oldId to newId
                    }
                }
            val projectId = number("SELECT id FROM counter_projects")
            val cardId = number("SELECT id FROM yarn_cards")
            assertTrue(projectId > 1L)
            assertTrue(cardId > 1L)
            val foreignTables =
                mapOf(
                    "savedPatternId" to "saved_patterns",
                    "layerId" to "pattern_annotation_layers",
                    "folderId" to "project_folders",
                    "projectYarnNoteId" to "project_yarn_notes",
                )
            tables.forEach { table ->
                assertEquals(table, before.getValue(table).size, after.getValue(table).size)
                before.getValue(table).zip(after.getValue(table)).forEach { (old, actual) ->
                    val expected = old.toMutableMap()
                    if ("id" in old) expected["id"] = ids.getValue(table).getValue(old.getValue("id") as Long)
                    if (old["projectId"] != null) expected["projectId"] = projectId
                    listOf("savedYarnCardId", "yarnCardId").filter { old[it] != null }.forEach { expected[it] = cardId }
                    foreignTables.forEach { (column, parent) ->
                        if (old[column] != null) expected[column] = ids.getValue(parent).getValue(old[column] as Long)
                    }
                    (old["documentKey"] as? String)?.let { key ->
                        val savedId = key.removePrefix("saved:").removeSuffix(":v1").toLong()
                        expected["documentKey"] = "saved:${ids.getValue("saved_patterns").getValue(savedId)}:v1"
                    }
                    (old["localPdfUri"] as? String)?.let { oldUri ->
                        val newUri = actual.getValue("localPdfUri") as String
                        assertFalse(oldUri == newUri)
                        val file = checkNotNull(AppFileStorage.resolveAppOwnedFile(context, newUri.toUri()))
                        assertTrue(pdfBytes.getValue(oldUri).contentEquals(file.readBytes()))
                        expected["localPdfUri"] = newUri
                    }
                    assertEquals(table, expected, actual)
                }
            }
        }

    @Test fun failedValidationAndCancelledPreviewDoNotChangeCurrentRowsOrFiles() =
        runBlocking {
            seed()
            val references = BackupTables.references(db.openHelper.writableDatabase)
            val invalid = File(directory, "invalid").apply { writeText("not a backup") }
            expectBackupFailure { prepare(invalid.toUri()) }
            assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            repository.cancelPreview(selectionId)
            assertReferencesAndPreviewCleanedUp(references)
        }

    @Test fun oversizedFieldsAreRejectedBeforePreviewAndAgainAtConfirmationWithoutLiveMutation() =
        runBlocking {
            seed()
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val archive = File(directory, "oversized-source")
            repository.export(archive.toUri())
            rewriteTableValue(
                archive,
                "counter_projects",
                "notes",
                JsonPrimitive("x".repeat(BackupLimits.MAX_FIELD_CHARACTERS + 1)),
            )

            expectBackupFailure { prepare(archive.toUri()) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())

            val valid = File(directory, "valid-source")
            repository.export(valid.toUri())
            prepare(valid.toUri())
            val staged = File(context.noBackupFilesDir, "manual-backup/$selectionId/backup.zip")
            rewriteTableValue(
                staged,
                "counter_projects",
                "notes",
                JsonPrimitive("x".repeat(BackupLimits.MAX_FIELD_CHARACTERS + 1)),
            )

            expectBackupFailure { repository.restore(selectionId) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun exportRefusesContentThatTheRestoreBudgetWouldReject() =
        runBlocking {
            seed()
            val oversized = "x".repeat(BackupLimits.MAX_FIELD_CHARACTERS + 1)
            db.openHelper.writableDatabase.execSQL(
                "UPDATE counter_projects SET notes = ?",
                arrayOf(oversized),
            )
            val archive = File(directory, "rejected-export")

            expectBackupFailure { repository.export(archive.toUri()) }

            assertEquals(oversized.length.toLong(), number("SELECT LENGTH(notes) FROM counter_projects"))
            assertFalse(archive.exists())
        }

    @Test fun sessionHistoryRoundTripsAtOldAndNativeLimits() =
        runBlocking {
            seed()
            for (count in listOf(10_000L, 10_001L, MAX_COMPLETED_SESSIONS)) {
                replaceSessionRows(count)
                val previousProjectId = number("SELECT id FROM counter_projects")
                val previousSessionId = number("SELECT MAX(id) FROM sessions")
                val archive = File(directory, "sessions-$count")
                repository.export(archive.toUri())
                db.openHelper.writableDatabase.execSQL("UPDATE sessions SET durationSeconds = 0")

                prepare(archive.toUri())
                repository.restore(selectionId)

                assertEquals(count, number("SELECT COUNT(*) FROM sessions"))
                assertTrue(number("SELECT id FROM counter_projects") > previousProjectId)
                assertTrue(number("SELECT MIN(id) FROM sessions") > previousSessionId)
                assertEquals(count * 60L, number("SELECT SUM(durationSeconds) FROM sessions"))
                assertEquals(count, number("SELECT COUNT(*) FROM sessions WHERE zoneId = 'Europe/Helsinki'"))
                assertEquals(
                    count,
                    number("SELECT COUNT(*) FROM sessions s JOIN counter_projects p ON p.id = s.projectId"),
                )
                assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
            }
        }

    @Test fun oversizedSessionsAreRejectedBeforePreviewAndAtConfirmationWithoutLiveMutation() =
        runBlocking {
            seed()
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val archive = File(directory, "oversized-sessions")
            val sessionId = number("SELECT id FROM sessions")
            val durationSeconds = number("SELECT durationSeconds FROM sessions")
            val references = BackupTables.references(db.openHelper.writableDatabase)
            repository.export(archive.toUri())
            rewriteOversizedSessions(archive)
            expectBackupFailure(BackupError.CORRUPT) { prepare(archive.toUri()) }
            assertCurrentSessionUnchanged(sessionId, durationSeconds, references)
            val valid = File(directory, "valid-sessions")
            repository.export(valid.toUri())
            prepare(valid.toUri())
            rewriteOversizedSessions(File(context.noBackupFilesDir, "manual-backup/$selectionId/backup.zip"))
            expectBackupFailure(BackupError.CORRUPT) { repository.restore(selectionId) }
            assertCurrentSessionUnchanged(sessionId, durationSeconds, references)
        }

    @Test fun exportRefusesSessionsAboveTheRestoreCeiling() =
        runBlocking {
            seed()
            replaceSessionRows(BackupLimits.MAX_SESSION_ROWS + 1)
            val archive = File(directory, "rejected-session-export")
            expectBackupFailure(BackupError.CORRUPT) { repository.export(archive.toUri()) }
            assertFalse(archive.exists())
            assertEquals(BackupLimits.MAX_SESSION_ROWS + 1, number("SELECT COUNT(*) FROM sessions"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    private fun replaceSessionRows(count: Long) {
        val sql = db.openHelper.writableDatabase
        sql.execSQL("DELETE FROM sessions")
        sql.execSQL(
            "WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x + 1 FROM n WHERE x < ?) " +
                "INSERT INTO sessions(projectId, startedAt, endedAt, startRow, endRow, " +
                "durationMinutes, durationSeconds, rowsWorked, zoneId) " +
                "SELECT (SELECT MIN(id) FROM counter_projects), 1000, 61000, 0, 1, 1, 60, 1, 'Europe/Helsinki' FROM n",
            arrayOf(count),
        )
    }

    private fun rewriteOversizedSessions(archive: File) {
        val payload = File(directory, "session-rewrite-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val manifest = BackupArchive.extract(archive, payload)
            val table = File(payload, "tables/sessions.jsonl")
            val lines = table.readLines()
            val idColumn =
                BackupFormat.json
                    .parseToJsonElement(lines.first())
                    .jsonArray
                    .indexOf(JsonPrimitive("id"))
            val row =
                BackupFormat.json
                    .parseToJsonElement(lines[1])
                    .jsonArray
                    .toMutableList()
            table.bufferedWriter().use { writer ->
                writer.appendLine(lines.first())
                repeat((BackupLimits.MAX_SESSION_ROWS + 1).toInt()) { index ->
                    row[idColumn] = JsonPrimitive(index + 1L)
                    writer.appendLine(JsonArray(row).toString())
                }
            }
            rewriteArchive(payload, archive, manifest)
        } finally {
            payload.deleteRecursively()
        }
    }

    @Test fun annotationBudgetsRejectBeforePreviewAndAgainBeforeLiveMutation() =
        runBlocking {
            seed()
            for (workLimit in listOf(false, true)) {
                val archive = File(directory, "annotation-source-$workLimit")
                repository.export(archive.toUri())
                rewriteAnnotationRows(archive, unsafeAnnotationPayloads(workLimit))
                expectBackupFailure { prepare(archive.toUri()) }
                assertEquals(1L, number("SELECT COUNT(*) FROM pattern_annotations"))
                assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
                val valid = File(directory, "annotation-valid-$workLimit")
                repository.export(valid.toUri())
                prepare(valid.toUri())
                rewriteAnnotationRows(
                    File(context.noBackupFilesDir, "manual-backup/$selectionId/backup.zip"),
                    unsafeAnnotationPayloads(workLimit),
                )
                expectBackupFailure { repository.restore(selectionId) }
                assertEquals(1L, number("SELECT COUNT(*) FROM pattern_annotations"))
                assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
            }
        }

    @Test fun annotationExportRefusesTheSameCountAndWorkAsRestoreWithoutDeletingRows() =
        runBlocking {
            seed()
            val sql = db.openHelper.writableDatabase
            for (workLimit in listOf(false, true)) {
                sql.execSQL("DELETE FROM pattern_annotations WHERE id != (SELECT MIN(id) FROM pattern_annotations)")
                val payloads = unsafeAnnotationPayloads(workLimit)
                sql.execSQL("UPDATE pattern_annotations SET payloadJson = ?", arrayOf(payloads.first()))
                payloads.drop(1).forEach { payload ->
                    sql.execSQL(
                        "INSERT INTO pattern_annotations(layerId,page,kind,payloadVersion,payloadJson," +
                            "zIndex,createdAt,updatedAt) SELECT layerId,page,kind,payloadVersion,?,zIndex," +
                            "createdAt,updatedAt FROM pattern_annotations LIMIT 1",
                        arrayOf(payload),
                    )
                }
                val archive = File(directory, "annotation-rejected-$workLimit")
                expectBackupFailure { repository.export(archive.toUri()) }
                assertFalse(archive.exists())
                assertEquals(payloads.size.toLong(), number("SELECT COUNT(*) FROM pattern_annotations"))
                expectBackupFailure { BackupTables.verify(sql) }
            }
        }

    private fun unsafeAnnotationPayloads(workLimit: Boolean): List<String> {
        val sizes = if (workLimit) List(8) { 2_048 } + 1 else List(257) { 1 }
        return sizes.map { size ->
            requireNotNull(
                PatternAnnotationPayloadCodec.encode(
                    PatternAnnotationKind.FREEHAND,
                    FreehandPayload(List(size) { NormalizedPatternPoint(it / 2_048f, 0.5f) }, 0, 2f),
                ),
            ).payloadJson
        }
    }

    private fun rewriteAnnotationRows(
        archive: File,
        payloads: List<String>,
    ) {
        val directory = File(this.directory, "annotation-rewrite-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val manifest = BackupArchive.extract(archive, directory)
            val table = File(directory, "tables/pattern_annotations.jsonl")
            val lines = table.readLines()
            val header = BackupFormat.json.parseToJsonElement(lines.first()).jsonArray
            val row =
                BackupFormat.json
                    .parseToJsonElement(lines[1])
                    .jsonArray
                    .toMutableList()
            table.bufferedWriter().use { writer ->
                writer.appendLine(lines.first())
                payloads.forEachIndexed { index, payload ->
                    row[header.indexOf(JsonPrimitive("id"))] = JsonPrimitive(index + 1)
                    row[header.indexOf(JsonPrimitive("payloadJson"))] = JsonPrimitive(payload)
                    writer.appendLine(JsonArray(row).toString())
                }
            }
            val updated =
                manifest.copy(
                    entries =
                        manifest.entries.map { entry ->
                            val file = File(directory, entry.path)
                            entry.copy(size = file.length(), sha256 = BackupFormat.digest(file))
                        },
                )
            BackupArchive.write(directory, archive, updated)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun stalePreviewCannotRestoreOrDiscardAnotherSelectionsBackup() =
        runBlocking {
            seed()
            val first = File(directory, "first")
            val second = File(directory, "second")
            repository.export(first.toUri())
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Second backup'")
            repository.export(second.toUri())
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val firstId = UUID.randomUUID()
            val secondId = UUID.randomUUID()
            repository.prepare(first.toUri(), firstId)
            repository.prepare(second.toUri(), secondId)
            repository.cancelPreview(firstId)
            repository.releasePreview(firstId)
            expectBackupFailure { repository.restore(firstId) }
            assertEquals("Current", text("SELECT name FROM counter_projects"))
            repository.restore(secondId)
            assertEquals("Second backup", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun competingPreparationsCannotPublishOrDeleteAnotherSelectionsPreview() =
        runBlocking {
            seed()
            val archive = File(directory, "racing-source")
            repository.export(archive.toUri())
            db.openHelper.writableDatabase.execSQL("UPDATE counter_projects SET name = 'Current'")
            val racingIo = RacingReadProviderIo(archive)
            val racingRepository = createRepository(racingIo)
            val firstId = UUID.randomUUID()
            val secondId = UUID.randomUUID()
            val first = async(Dispatchers.IO) { runCatching { racingRepository.prepare(mockUri("first"), firstId) } }
            racingIo.firstStarted.await()

            val second = async(Dispatchers.IO) { racingRepository.prepare(mockUri("second"), secondId) }
            assertEquals(1, second.await().projects)
            racingIo.releaseFirst.complete(Unit)

            assertTrue(first.await().exceptionOrNull() is CancellationException)
            racingRepository.cancelPreview(firstId)
            racingRepository.restore(secondId)
            assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun stalledExternalReferenceDoesNotHoldBackupContentOrDatabaseLocks() =
        runBlocking {
            seed()
            db.openHelper.writableDatabase.execSQL(
                "UPDATE yarn_cards SET photoUri = 'content://stalled.provider/photo'",
            )
            val stalledIo = StalledReadProviderIo()
            val stalledRepository = createRepository(stalledIo)
            val export = async(Dispatchers.IO) { stalledRepository.export(mockUri("destination")) }
            stalledIo.started.await()

            withTimeout(2_000) {
                stalledRepository.cancelPreview(null)
                patternFiles.withReferenceLock {
                    yarnCards.withPhotoStorageLock {
                        db.withTransaction {
                            db.openHelper.writableDatabase
                                .query("SELECT COUNT(*) FROM yarn_cards")
                                .close()
                        }
                    }
                }
            }

            export.cancelAndJoin()
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun providerWriteFailureMapsToWriteAndCleansLocalArchive() =
        runBlocking {
            seed()
            val failingRepository = createRepository(FailingWriteProviderIo())

            val failure = runCatching { failingRepository.export(mockUri("destination")) }.exceptionOrNull()

            assertTrue(failure is BackupException)
            assertEquals(BackupError.WRITE, (failure as BackupException).error)
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun liveInsertFailureRollsBackRowsAndKeepsOldFiles() =
        runBlocking {
            seed()
            val references = BackupTables.references(db.openHelper.writableDatabase)
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_backup BEFORE INSERT ON counter_projects " +
                    "BEGIN SELECT RAISE(ABORT, 'test failure'); END",
            )
            expectBackupFailure { repository.restore(selectionId) }
            assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
            assertEquals(references, BackupTables.references(db.openHelper.writableDatabase))
            references.forEach { assertTrue(AppFileStorage.resolveAppOwnedFile(context, it.toUri())?.exists() == true) }
            assertFalse(context.filesDir.walkTopDown().any { it.isFile && it.name.startsWith("restore-") })
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
        }

    @Test fun validHashesCannotHideBrokenForeignKeysOrInvalidColumnTypes() =
        runBlocking {
            seed()
            val references = BackupTables.references(db.openHelper.writableDatabase)
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            val payload = File(directory, "tampered").apply { mkdirs() }
            val manifest = BackupArchive.extract(archive, payload)
            val table = File(payload, "tables/counter_history.jsonl")
            val original = table.readLines()
            val header = BackupFormat.json.parseToJsonElement(original.first()).jsonArray
            val column = header.indexOf(JsonPrimitive("projectId"))
            for (value in listOf(JsonPrimitive(999), JsonPrimitive("not a number"))) {
                val row =
                    BackupFormat.json
                        .parseToJsonElement(original[1])
                        .jsonArray
                        .toMutableList()
                row[column] = value
                table.writeText(original.first() + "\n" + JsonArray(row) + "\n")
                rewriteArchive(payload, archive, manifest)
                expectBackupFailure { prepare(archive.toUri()) }
                assertEquals("Cardigan", text("SELECT name FROM counter_projects"))
                assertEquals(references, BackupTables.references(db.openHelper.writableDatabase))
            }
        }

    @Test fun invalidMetadataAndFolderKeysAreRejectedBeforePreviewAndAtConfirmationWithoutLiveMutation() =
        runBlocking {
            seed()
            db.counterProjectDao().insert(CounterProjectEntity(id = 2, name = "Other project"))
            val sql = db.openHelper.writableDatabase

            fun rows() =
                BackupFormat.tables.associateWith { table ->
                    sql.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                add(List(cursor.columnCount) { column -> cursor.getString(column) })
                            }
                        }
                    }
                }
            val originalRows = rows()
            val references = BackupTables.references(sql)
            val archive = File(directory, "valid-metadata")
            repository.export(archive.toUri())
            val validBytes = archive.readBytes()
            val invalidValues =
                listOf(
                    Triple("saved_patterns", "ravelryPatternId", JsonPrimitive(Int.MAX_VALUE.toLong() + 1)),
                    Triple("saved_patterns", "ravelryPatternId", JsonPrimitive(Long.MAX_VALUE)),
                    Triple("saved_patterns", "difficulty", JsonPrimitive(1e300)),
                    Triple("saved_patterns", "gaugeStitches", JsonPrimitive(-1e300)),
                    Triple("saved_patterns", "gaugeRows", JsonPrimitive(1e300)),
                    Triple("project_folders", "normalizedName", JsonPrimitive("wrong-key")),
                    Triple("project_folders", "name", JsonPrimitive("")),
                    Triple("project_folders", "name", JsonPrimitive(" Wool ")),
                    Triple("project_folders", "name", JsonPrimitive("x".repeat(51))),
                    Triple("project_yarn_usage", "projectId", JsonPrimitive(2)),
                    Triple("project_yarn_usage", "plannedMeters", JsonPrimitive(-1.0)),
                    Triple("project_yarn_usage", "allocatedMeters", JsonPrimitive(-1.0)),
                    Triple("project_yarn_usage", "usedMeters", JsonPrimitive(-1.0)),
                    Triple("project_yarn_usage", "metersPerSkein", JsonPrimitive(0.0)),
                    Triple("project_yarn_usage", "gramsPerSkein", JsonPrimitive(100.0)),
                )
            for ((table, column, value) in invalidValues) {
                archive.writeBytes(validBytes)
                rewriteTableValue(archive, table, column, value)
                expectBackupFailure(BackupError.VALIDATION) { prepare(archive.toUri()) }
                assertEquals(originalRows, rows())
                assertReferencesAndPreviewCleanedUp(references)

                archive.writeBytes(validBytes)
                prepare(archive.toUri())
                val staged = File(context.noBackupFilesDir, "manual-backup/$selectionId/backup.zip")
                rewriteTableValue(staged, table, column, value)
                expectBackupFailure(BackupError.VALIDATION) { repository.restore(selectionId) }
                assertEquals(originalRows, rows())
                assertReferencesAndPreviewCleanedUp(references)
            }
        }

    @Test fun startupJournalRecoveryKeepsCommittedReferencesAndRemovesUncommittedFiles() =
        runBlocking {
            seed()
            for (committed in listOf(false, true)) {
                val operation =
                    File(
                        context.noBackupFilesDir,
                        "manual-backup/${UUID.randomUUID()}/payload",
                    ).apply { mkdirs() }
                val content =
                    File(operation, "files/${"a".repeat(64)}.bin").apply {
                        checkNotNull(parentFile).mkdirs()
                        writeText("photo")
                    }
                val files = BackupRestoreFiles(context, operation)
                val row =
                    mutableMapOf<String, JsonElement>(
                        "photoUri" to JsonPrimitive(content.relativeTo(operation).invariantSeparatorsPath),
                        "id" to JsonPrimitive(1),
                    )
                files.rebase("yarn_cards", row)
                files.journal(emptySet())
                files.publish {}
                val newUri = row.getValue("photoUri").toString().trim('"')
                if (committed) {
                    db.openHelper.writableDatabase.execSQL(
                        "UPDATE yarn_cards SET photoUri = ?",
                        arrayOf(newUri),
                    )
                }
                repository.recoverInterruptedOperations()
                assertEquals(committed, AppFileStorage.resolveAppOwnedFile(context, newUri.toUri())?.exists())
                assertFalse(checkNotNull(operation.parentFile).exists())
            }
        }

    @Test fun chartTrackerIdsAndDanglingMetadataAreRebasedWithoutAccidentalLinks() =
        runBlocking {
            seed()
            val payload =
                """
                {"region":{"bounds":{"left":0.1,"top":0.2,"right":0.9,"bottom":0.8},
                "name":"Chart","rows":20,"columns":30,"rowDirection":"BOTTOM_TO_TOP",
                "columnDirection":"ALTERNATING"},"trackingMode":"CROSSHAIR","counterType":"EXTRA",
                "extraCounterId":1,"counterStartValue":0,"gridStartIndex":0,"wrapAtEnd":true,
                "highlightArgb":1,"highlightAlpha":0.4}
                """.trimIndent()
            db.openHelper.writableDatabase.execSQL(
                "UPDATE pattern_annotations SET kind = 'CHART_TRACKER', payloadJson = ?",
                arrayOf(payload),
            )
            db.openHelper.writableDatabase.execSQL("UPDATE project_yarn_notes SET savedYarnCardId = 999")
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            repository.restore(selectionId)

            fun counterId(): Long =
                BackupFormat.json
                    .parseToJsonElement(
                        text("SELECT payloadJson FROM pattern_annotations"),
                    ).jsonObject
                    .getValue("extraCounterId")
                    .jsonPrimitive.long
            assertEquals(number("SELECT id FROM project_counters"), counterId())
            assertEquals(0L, number("SELECT COUNT(*) FROM project_yarn_notes WHERE savedYarnCardId IS NOT NULL"))
            db.openHelper.writableDatabase.execSQL("DELETE FROM project_counters")
            repository.export(archive.toUri())
            prepare(archive.toUri())
            repository.restore(selectionId)
            val missing = counterId()
            insert("project_counters", mapOf("id" to null, "projectId" to number("SELECT id FROM counter_projects")))
            assertTrue(number("SELECT id FROM project_counters") > missing)
        }

    @Test fun orphanTempAndInstallationStateAreNeverArchivedOrReplaced() =
        runBlocking {
            val preferences = PreferencesManager(context)
            preferences.setThemeMode(ThemeMode.DARK)
            preferences.setUseImperial(true)
            val before = preferences.preferences.first()
            seed()
            val sentinel =
                File(context.filesDir, "datastore/trial_state.preferences_pb").apply {
                    checkNotNull(parentFile).mkdirs()
                    writeText("trial sentinel")
                }
            val orphan =
                File(context.filesDir, "pattern_captures/999/abandoned.jpg").apply {
                    checkNotNull(parentFile).mkdirs()
                    writeText("orphan")
                }
            val archive = File(directory, "backup")
            repository.export(archive.toUri())
            java.util.zip.ZipFile(archive).use { zip ->
                val names =
                    zip
                        .entries()
                        .asSequence()
                        .map { it.name }
                        .toList()
                assertTrue(names.all { it == BackupFormat.MANIFEST || BackupFormat.allowedPath(it) })
                assertEquals(BackupFormat.tables.size + 5, names.size)
            }
            prepare(archive.toUri())
            repository.restore(selectionId)
            assertEquals("trial sentinel", sentinel.readText())
            assertEquals("orphan", orphan.readText())
            assertEquals(before, preferences.preferences.first())
        }

    private suspend fun seed() {
        val pdf = File(context.filesDir, "patterns/legacy.pdf").apply { checkNotNull(parentFile).mkdirs() }
        writePdf(pdf, 100, 100)
        val secondPdf = File(context.filesDir, "pattern_pdfs/1/second.pdf").apply { checkNotNull(parentFile).mkdirs() }
        writePdf(secondPdf, 120, 150)
        val photo = File(context.filesDir, "progress_photos/1/photo.jpg").apply { checkNotNull(parentFile).mkdirs() }
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.eraseColor(android.graphics.Color.RED)
        val yarn = File(context.filesDir, "yarn_photos/1/yarn.jpg").apply { checkNotNull(parentFile).mkdirs() }
        yarn.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        val annotationJson =
            """{"points":[{"x":0.1,"y":0.1},{"x":0.5,"y":0.5}],"argb":-16777216,"strokeWidth":2.0}"""
        db.withTransaction {
            BackupFormat.tables.forEach { table ->
                val changes: Map<String, Any?> =
                    when (table) {
                        "counter_projects" ->
                            mapOf(
                                "name" to "Cardigan",
                                "notes" to "Private notes",
                                "count" to 12,
                                "yarnCardIds" to "1",
                                "patternUri" to
                                    "content://${AppFileStorage.fileProviderAuthority(context)}/patterns/legacy.pdf",
                                "linkedPatternId" to 1,
                            )
                        "saved_patterns" ->
                            mapOf(
                                "source" to "LOCAL_FILE",
                                "name" to "Chart",
                                "localPdfUri" to pdf.toUri().toString(),
                                "availability" to "unknown",
                            )
                        "yarn_cards" -> mapOf("photoUri" to yarn.toUri().toString(), "linkedProjectId" to 1)
                        "project_folders" -> mapOf("name" to "Wool", "normalizedName" to "wool")
                        "progress_photos" -> mapOf("photoUri" to photo.toUri().toString())
                        "pattern_annotation_layers" ->
                            mapOf(
                                "savedPatternId" to null,
                                "documentKey" to "saved:1:v1",
                                "isActive" to 1,
                            )
                        "pattern_bookmarks" -> mapOf("documentKey" to "saved:1:v1")
                        "pattern_annotations" ->
                            mapOf(
                                "kind" to "FREEHAND",
                                "payloadVersion" to 1,
                                "payloadJson" to annotationJson,
                            )
                        "project_documents" ->
                            mapOf(
                                "documentKey" to "saved:1:v1",
                                "localPdfUri" to pdf.toUri().toString(),
                                "isPrimary" to 1,
                            )
                        "project_completions" ->
                            mapOf(
                                "completedAt" to 1_700_000_000_000L,
                                "zoneId" to "Europe/Helsinki",
                            )
                        "counter_history" ->
                            mapOf(
                                "action" to "increment",
                                "previousValue" to 11,
                                "newValue" to 12,
                                "timestamp" to 1_700_000_000_000L,
                            )
                        "active_sessions" ->
                            mapOf(
                                "singletonId" to 1,
                                "sessionToken" to "old-session",
                                "bootCount" to 1,
                                "checkpointedDurationSeconds" to 120,
                                "startZoneId" to "Europe/Helsinki",
                            )
                        else -> emptyMap()
                    }
                insert(table, changes)
            }
            insert(
                "saved_patterns",
                mapOf(
                    "id" to 2,
                    "source" to "LOCAL_FILE",
                    "localPdfUri" to secondPdf.toUri().toString(),
                ),
            )
            insert(
                "project_documents",
                mapOf(
                    "id" to 2,
                    "savedPatternId" to 2,
                    "documentKey" to "saved:2:v1",
                    "localPdfUri" to secondPdf.toUri().toString(),
                    "isPrimary" to 0,
                ),
            )
        }
    }

    private fun writePdf(
        file: File,
        width: Int,
        height: Int,
    ) {
        val document = PdfDocument()
        try {
            document.finishPage(document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create()))
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
    }

    private fun assertCurrentSessionUnchanged(
        sessionId: Long,
        durationSeconds: Long,
        references: Set<String>,
    ) {
        assertEquals("Current", text("SELECT name FROM counter_projects"))
        assertEquals(1L, number("SELECT COUNT(*) FROM sessions"))
        assertEquals(sessionId, number("SELECT id FROM sessions"))
        assertEquals(durationSeconds, number("SELECT durationSeconds FROM sessions"))
        assertReferencesAndPreviewCleanedUp(references)
    }

    private fun assertReferencesAndPreviewCleanedUp(references: Set<String>) {
        assertEquals(references, BackupTables.references(db.openHelper.writableDatabase))
        references.forEach { assertTrue(AppFileStorage.resolveAppOwnedFile(context, it.toUri())?.exists() == true) }
        assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
    }

    private fun rewriteArchive(
        payload: File,
        archive: File,
        manifest: BackupManifest,
    ) {
        val updated =
            manifest.copy(
                entries =
                    manifest.entries.map { entry ->
                        val file = File(payload, entry.path)
                        entry.copy(size = file.length(), sha256 = BackupFormat.digest(file))
                    },
            )
        BackupArchive.write(payload, archive, updated)
    }

    private fun rewriteTableValue(
        archive: File,
        tableName: String,
        columnName: String,
        value: JsonPrimitive,
    ) {
        val payload = File(directory, "rewrite-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val manifest = BackupArchive.extract(archive, payload)
            val table = File(payload, "tables/$tableName.jsonl")
            val lines = table.readLines().toMutableList()
            val header = BackupFormat.json.parseToJsonElement(lines.first()).jsonArray
            val column = header.indexOf(JsonPrimitive(columnName))
            val row =
                BackupFormat.json
                    .parseToJsonElement(lines[1])
                    .jsonArray
                    .toMutableList()
            row[column] = value
            lines[1] = JsonArray(row).toString()
            table.writeText(lines.joinToString("\n", postfix = "\n"))
            rewriteArchive(payload, archive, manifest)
        } finally {
            payload.deleteRecursively()
        }
    }

    private fun insert(
        table: String,
        changes: Map<String, Any?>,
    ) {
        val sql = db.openHelper.writableDatabase
        val columns = BackupTables.columns(sql, table)
        val values =
            columns.map { column ->
                if (changes.containsKey(column.name)) {
                    changes[column.name]
                } else {
                    when {
                        column.name == "id" || column.name.endsWith("Id") -> 1
                        !column.required -> null
                        column.type == "TEXT" -> ""
                        else -> 0
                    }
                }
            }
        sql.execSQL(
            "INSERT INTO `$table` (${columns.joinToString(
                ",",
            ) { "`${it.name}`" }}) VALUES (${columns.joinToString(",") { "?" }})",
            values.toTypedArray(),
        )
    }

    private fun number(query: String): Long =
        db.openHelper.writableDatabase.query(query).use {
            it.moveToFirst()
            it.getLong(0)
        }

    private fun text(query: String): String =
        db.openHelper.writableDatabase.query(query).use {
            it.moveToFirst()
            it.getString(0)
        }

    private fun createRepository(providerIo: BackupProviderIo): BackupRepository =
        BackupRepository(
            context,
            db,
            patternFiles,
            yarnCards,
            Dispatchers.IO,
            scope,
            providerIo,
        )

    private fun mockUri(name: String): Uri = "content://backup-test/$name".toUri()

    private suspend fun expectBackupFailure(
        expectedError: BackupError? = null,
        block: suspend () -> Unit,
    ) {
        try {
            block()
            throw AssertionError("Expected backup failure")
        } catch (failure: BackupException) {
            if (expectedError != null) assertEquals(expectedError, failure.error)
        }
    }

    private class RacingReadProviderIo(
        private val archive: File,
    ) : BackupProviderIo {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        private val reads = AtomicInteger()

        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) {
            if (reads.incrementAndGet() == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
            check()
            archive.copyTo(target)
        }

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) = error("Unexpected write")
    }

    private class StalledReadProviderIo : BackupProviderIo {
        val started = CompletableDeferred<Unit>()

        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) {
            started.complete(Unit)
            awaitCancellation()
        }

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) = error("Unexpected write")
    }

    private class FailingWriteProviderIo : BackupProviderIo {
        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) = error("Unexpected read")

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) = throw IOException("provider unavailable")
    }

    private fun cursorValues(cursor: Cursor): Map<String, Any?> =
        cursor.columnNames
            .mapIndexed { index, name ->
                name to
                    when (cursor.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> null
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                        else -> cursor.getString(index)
                    }
            }.toMap()

    private suspend fun restoreAndReopenDatabase(archiveName: String) {
        val archive = File(directory, archiveName)
        repository.export(archive.toUri())
        prepare(archive.toUri())
        repository.restore(selectionId)
        db.close()
        db =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, File(directory, "live.db").absolutePath)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
    }
}
