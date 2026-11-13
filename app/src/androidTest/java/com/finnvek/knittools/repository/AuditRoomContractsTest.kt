package com.finnvek.knittools.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.data.local.ActiveSessionEntity
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class AuditRoomContractsTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var database: KnitToolsDatabase
    private val createdFiles = mutableListOf<File>()

    @Test fun photoFlowsBreakEqualTimestampTiesByNewestIdentity() =
        runBlocking {
            val owner = database.counterProjectDao().insert(CounterProjectEntity(name = "Photo order"))
            val dao = database.progressPhotoDao()
            val ids =
                List(3) { index ->
                    dao.insert(
                        ProgressPhotoEntity(
                            projectId = owner,
                            photoUri = "content://fixture/$index",
                            rowNumber = index,
                            createdAt = 123L,
                        ),
                    )
                }
            assertEquals(ids.reversed(), dao.getPhotosForProject(owner).first().map { it.id })
            assertEquals(ids.reversed(), dao.getAllPhotos().first().map { it.id })
        }

    @Test fun bulkPhotoDeletionRollsBackEveryChunkBeforeFileCleanup() =
        runBlocking {
            val owner =
                database.counterProjectDao().insert(
                    CounterProjectEntity(id = System.nanoTime(), name = "Bulk photos"),
                )
            val root = File(context.filesDir, "progress_photos/$owner")
            check(!root.exists())
            check(root.mkdirs())
            try {
                val first = File(root, "first.jpg").apply { writeText("first") }
                val last = File(root, "last.jpg").apply { writeText("last") }
                val dao = database.progressPhotoDao()
                val runner = RoomDatabaseTransactionRunner(database)
                val ids =
                    runner.run {
                        List(901) { index ->
                            dao.insert(
                                ProgressPhotoEntity(
                                    projectId = owner,
                                    photoUri = Uri.fromFile(if (index == 900) last else first).toString(),
                                    rowNumber = index,
                                ),
                            )
                        }
                    }
                var calls = 0
                val failing =
                    object : ProgressPhotoDao by dao {
                        override suspend fun deleteByIds(ids: List<Long>) {
                            if (++calls == 2) throw java.io.IOException("Synthetic second-chunk failure")
                            dao.deleteByIds(ids)
                        }
                    }
                val repository =
                    ProgressPhotoRepository(failing, ProgressPhotoStorage(), context, Dispatchers.IO, runner)
                val failure = runCatching { repository.deletePhotos(ids) }.exceptionOrNull()
                assertTrue(failure is java.io.IOException)
                assertEquals(2, calls)
                assertEquals(ids.toSet(), dao.getAllPhotosOnce().map { it.id }.toSet())
                assertEquals("first", first.readText())
                assertEquals("last", last.readText())
                ProgressPhotoRepository(dao, ProgressPhotoStorage(), context, Dispatchers.IO, runner).deletePhotos(ids)
                assertTrue(dao.getAllPhotosOnce().isEmpty())
                assertFalse(first.exists())
                assertFalse(last.exists())
            } finally {
                root.deleteRecursively()
            }
        }

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        directory = File(context.cacheDir, "audit-room-${UUID.randomUUID()}").apply { mkdirs() }
        open()
    }

    private fun open() {
        database =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, File(directory, "live.db").absolutePath)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .build()
    }

    @After fun teardown() {
        database.close()
        createdFiles.forEach { it.delete() }
        directory.deleteRecursively()
    }

    @Test fun pausedYarnMetadataWritePreservesQuantityAndEveryOtherField() =
        runBlocking {
            withTimeout(15_000) {
                val fixture = RoomCounterTestFixture(database, context)
                val projects = database.counterProjectDao()
                val owner = projects.insert(CounterProjectEntity(name = "Owner"))
                val other = projects.insert(CounterProjectEntity(name = "Other"))
                val original =
                    YarnCard(
                        brand = "Å brand",
                        yarnName = "Ö yarn",
                        fiberContent = "70% wool",
                        weightGrams = "50,5",
                        lengthMeters = "123.75",
                        needleSize = "3–4 mm",
                        gaugeInfo = "22 × 31",
                        colorName = "Sininen",
                        colorNumber = "0007",
                        dyeLot = "Erä 007",
                        weightCategory = "Imported weight",
                        careSymbols = Long.MIN_VALUE,
                        photoUri = "",
                        createdAt = 123L,
                        quantityInStash = 8,
                        status = YarnCardStatus.IN_STASH,
                        linkedProjectId = owner,
                    )
                val id = checkNotNull(fixture.yarnCardRepository.saveCard(original))
                val saved = original.copy(id = id)
                assertEquals(saved, fixture.yarnCardRepository.observeCard(id).first())
                val read = CompletableDeferred<Unit>()
                val resume = CompletableDeferred<Unit>()
                val dao = database.yarnCardDao()
                val pausedDao =
                    object : YarnCardDao by dao {
                        override suspend fun getCard(id: Long): YarnCardEntity? {
                            val row = dao.getCard(id)
                            read.complete(Unit)
                            resume.await()
                            return row
                        }
                    }
                val paused = YarnCardRepository(pausedDao, projects, context, fixture.transactionRunner, Dispatchers.IO)
                val metadata = async { paused.saveCard(saved.copy(brand = "New brand", quantityInStash = 999)) }
                read.await()
                val quantity =
                    async(start = CoroutineStart.UNDISPATCHED) { fixture.yarnCardRepository.changeQuantity(id, 2) }
                assertFalse(quantity.isCompleted)
                resume.complete(Unit)
                assertEquals(id, metadata.await())
                assertTrue(quantity.await())
                assertEquals(
                    saved.copy(brand = "New brand", quantityInStash = 10),
                    fixture.yarnCardRepository.getCard(id),
                )
                assertFalse(fixture.yarnCardRepository.updateQuantity(id, -1))
                assertTrue(fixture.yarnCardRepository.updateQuantity(id, Int.MAX_VALUE))
                assertFalse(fixture.yarnCardRepository.changeQuantity(id, 1))
                assertTrue(fixture.yarnCardRepository.updateQuantity(id, 0))
                assertTrue(fixture.yarnCardRepository.updateLinkedProjectId(id, other))
                assertEquals("", projects.getProject(owner)?.yarnCardIds)
                assertEquals(id.toString(), projects.getProject(other)?.yarnCardIds)
                fixture.counterRepository().deleteProject(other)
                assertEquals(
                    saved.copy(brand = "New brand", quantityInStash = 0, linkedProjectId = null),
                    dao.getCard(id)?.toDomain(),
                )
                assertTrue(fixture.yarnCardRepository.deleteCard(id))
                assertEquals(null, fixture.yarnCardRepository.observeCard(id).first())
                assertEquals(null, fixture.yarnCardRepository.saveCard(saved))
            }
        }

    @Test fun pausedReminderEditRejectsOldCompletionAndPreservesTheWholeNewRow() =
        runBlocking {
            withTimeout(15_000) {
                val fixture = RoomCounterTestFixture(database, context)
                val project = database.counterProjectDao().insert(CounterProjectEntity(name = "Reminder"))
                val dao = database.rowReminderDao()
                val original = RowReminder(projectId = project, targetRow = 7, message = "Original", createdAt = 123L)
                val saved = original.copy(id = dao.insert(original.toEntity()))
                val read = CompletableDeferred<Unit>()
                val resume = CompletableDeferred<Unit>()
                val pausedDao =
                    object : RowReminderDao by dao {
                        override suspend fun getReminder(id: Long) =
                            dao.getReminder(id).also {
                                read.complete(Unit)
                                resume.await()
                            }
                    }
                val pro = (context as App).proManager.get()
                val repository = ReminderRepository(dao, database.counterProjectDao(), pro, fixture.transactionRunner)
                val paused = ReminderRepository(pausedDao, database.counterProjectDao(), pro, fixture.transactionRunner)
                val edited = saved.copy(targetRow = 22, repeatInterval = 3, message = "Uusi 🧶")
                val edit = async { paused.update(project, edited) }
                read.await()
                val completion =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        repository.update(project, saved.copy(isCompleted = true), expectedReminder = saved)
                    }
                assertFalse(completion.isCompleted)
                resume.complete(Unit)
                assertTrue(edit.await() is ReminderMutationResult.Success)
                assertEquals(ReminderMutationResult.StaleAction, completion.await())
                assertEquals(edited, repository.getRemindersForProject(project).first().single())
                assertTrue(
                    repository.update(
                        project,
                        edited.copy(isCompleted = true),
                        edited,
                    ) is ReminderMutationResult.Success,
                )
                assertEquals(edited.copy(isCompleted = true), dao.getReminder(saved.id)?.toDomain())
                database.counterProjectDao().delete(project)
                assertTrue(repository.getRemindersForProject(project).first().isEmpty())
                assertEquals(ReminderMutationResult.ProjectUnavailable, repository.update(project, saved))
            }
        }

    @Test fun pausedCounterIncrementAndRenamePreserveDormantConfigurationAndIdentity() =
        runBlocking {
            withTimeout(15_000) {
                val fixture = RoomCounterTestFixture(database, context)
                val project = database.counterProjectDao().insert(CounterProjectEntity(name = "Counter"))
                val dao = database.projectCounterDao()
                val initial =
                    ProjectCounterEntity(
                        projectId = project,
                        name = "Hiha",
                        count = 7,
                        stepSize = 3,
                        repeatAt = 11,
                        sortOrder = 5,
                        createdAt = 123L,
                        startingStitches = 56,
                        stitchChange = -2,
                        shapeEveryN = 4,
                        repeatStartRow = 2,
                        repeatEndRow = 6,
                        totalRepeats = 8,
                        currentRepeat = 3,
                        linkedToMainCounter = true,
                    )
                val saved = initial.copy(id = dao.insert(initial))
                val read = CompletableDeferred<Unit>()
                val resume = CompletableDeferred<Unit>()
                val pausedDao =
                    object : ProjectCounterDao by dao {
                        override suspend fun getCounter(id: Long) =
                            dao.getCounter(id).also {
                                read.complete(Unit)
                                resume.await()
                            }
                    }
                val pro = (context as App).proManager.get()
                val repository =
                    ProjectCounterRepository(dao, database.counterProjectDao(), pro, fixture.transactionRunner)
                val paused =
                    ProjectCounterRepository(pausedDao, database.counterProjectDao(), pro, fixture.transactionRunner)
                val increment = async { paused.incrementCounter(saved.toDomain()) }
                read.await()
                val rename =
                    async(
                        start = CoroutineStart.UNDISPATCHED,
                    ) { repository.renameCounter(project, saved.id, "Uusi nimi") }
                assertFalse(rename.isCompleted)
                resume.complete(Unit)
                assertTrue(increment.await() is ProjectCounterMutationResult.Success)
                assertTrue(rename.await() is ProjectCounterMutationResult.Success)
                val expected = saved.copy(count = 10, name = "Uusi nimi")
                assertEquals(expected, dao.getCounter(saved.id))
                assertEquals(expected.toDomain(), repository.getCountersForProject(project).first().single())
                assertTrue(repository.resetCounter(project, saved.id) is ProjectCounterMutationResult.Success)
                assertEquals(expected.copy(count = 0), dao.getCounter(saved.id))
                database.counterProjectDao().delete(project)
                assertEquals(null, dao.getCounter(saved.id))
            }
        }

    @Test fun realPhotoProviderCopyNotePruningAndFailedInsertPreserveRowsAndFiles() =
        runBlocking {
            val project = database.counterProjectDao().insert(CounterProjectEntity(id = 501L, name = "Photo"))
            val storage = ProgressPhotoStorage()
            val repository =
                ProgressPhotoRepository(
                    database.progressPhotoDao(),
                    storage,
                    context,
                    Dispatchers.IO,
                    transactionRunner = RoomDatabaseTransactionRunner(database),
                )
            val (capture, captureUri) = repository.createPhotoCaptureTarget(project)
            createdFiles += capture
            context.contentResolver.openOutputStream(captureUri).use { stream ->
                val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
                try {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, checkNotNull(stream)))
                } finally {
                    bitmap.recycle()
                }
            }
            val id = repository.savePhoto(project, captureUri, 42, "Original")
            assertTrue(id > 0)
            assertFalse(capture.exists())
            val saved = database.progressPhotoDao().getByIds(listOf(id)).single()
            val photoFile = File(checkNotNull(Uri.parse(saved.photoUri).path))
            createdFiles += photoFile
            val bytes = photoFile.readBytes()
            repository.updatePhotoNote(id, "N".repeat(101))
            assertEquals(saved.copy(note = "N".repeat(100)), database.progressPhotoDao().getByIds(listOf(id)).single())
            assertTrue(bytes.contentEquals(photoFile.readBytes()))
            repository.updatePhotoNote(id, "  ")
            assertEquals(saved.copy(note = null), database.progressPhotoDao().getByIds(listOf(id)).single())
            assertTrue(photoFile.delete())
            assertTrue(repository.getPhotosForProject(project).first().isEmpty())
            assertTrue(database.progressPhotoDao().getByIds(listOf(id)).isEmpty())
            val source = File(directory, "source.png")
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                bitmap.recycle()
            }
            assertTrue(runCatching { repository.savePhoto(502L, Uri.fromFile(source), 43) }.isFailure)
            assertTrue(database.progressPhotoDao().getAllPhotosOnce().isEmpty())
            assertTrue(File(context.filesDir, "progress_photos/502").listFiles().orEmpty().isEmpty())
        }

    @Test fun pausedActiveSessionCheckpointSerializesWidgetRowsAndPreservesEveryAnchor() =
        runBlocking {
            withTimeout(15_000) {
                val fixture = RoomCounterTestFixture(database, context)
                var now = SessionTimeSnapshot(1_700_000_000_000L, 5_000L, 7L, "Europe/Helsinki")

                fun repository(dao: SessionDao) =
                    CounterRepository(
                        database.counterProjectDao(),
                        database.projectCounterDao(),
                        dao,
                        ProgressPhotoStorage(),
                        PatternDocumentStorage(Dispatchers.IO),
                        context,
                        fixture.yarnCardRepository,
                        fixture.savedPatternRepository,
                        fixture.projectDocumentRepository,
                        database.projectFolderDao(),
                        fixture.transactionRunner,
                        Dispatchers.IO,
                        object : SessionTimeSource {
                            override fun snapshot() = now
                        },
                    )
                val project =
                    database.counterProjectDao().insert(
                        CounterProjectEntity(name = "Active", count = 7, stepSize = 3),
                    )
                val normal = repository(database.sessionDao())
                assertTrue(normal.startSession(project) is StartSessionResult.Started)
                val original = checkNotNull(database.sessionDao().getActiveSession())
                val read = CompletableDeferred<Unit>()
                val resume = CompletableDeferred<Unit>()
                val dao = database.sessionDao()
                val pausedDao =
                    object : SessionDao by dao {
                        override suspend fun getActiveSession() =
                            dao.getActiveSession().also {
                                if (!read.isCompleted) {
                                    read.complete(Unit)
                                    resume.await()
                                }
                            }
                    }
                now = now.copy(wallClockMillis = now.wallClockMillis + 61_000L, elapsedRealtimeMillis = 66_000L)
                val main = async { repository(pausedDao).applyMainCounterChange(project, MainCounterChange.Increment) }
                read.await()
                val widget = async(start = CoroutineStart.UNDISPATCHED) { normal.applyWidgetCountChange(project, true) }
                assertFalse(widget.isCompleted)
                resume.complete(Unit)
                assertTrue(main.await())
                assertTrue(widget.await())
                assertEquals(
                    original.copy(
                        lastObservedRow = 13,
                        trustedLastObservedRow = 13,
                        trustedRowsWorked = 6,
                        unreviewedRowsWorked = 6,
                        checkpointedDurationSeconds = 61L,
                        segmentStartedAtWallMillis = now.wallClockMillis,
                        segmentStartedElapsedRealtimeMillis = now.elapsedRealtimeMillis,
                        updatedAtWallMillis = now.wallClockMillis,
                    ),
                    dao.getActiveSession(),
                )
                assertEquals(13, database.counterProjectDao().getProject(project)?.count)
                assertEquals(2, normal.observeCounterHistory(project).first().size)
            }
        }

    @Test fun pausedPhotoDeleteAllowsNoteCommitThenDeletesTheOwnedRowBeforeItsFile() =
        runBlocking {
            withTimeout(15_000) {
                val project = database.counterProjectDao().insert(CounterProjectEntity(id = 501L, name = "Photo"))
                val file = File(context.filesDir, "progress_photos/$project/paused-${UUID.randomUUID()}.jpg")
                file.parentFile?.mkdirs()
                file.writeBytes(byteArrayOf(1, 2, 3))
                createdFiles += file
                val dao = database.progressPhotoDao()
                val original =
                    ProgressPhotoEntity(
                        projectId = project,
                        photoUri = Uri.fromFile(file).toString(),
                        rowNumber = 42,
                        note = "Original",
                        createdAt = 123L,
                    )
                val saved = original.copy(id = dao.insert(original))
                val read = CompletableDeferred<Unit>()
                val resume = CompletableDeferred<Unit>()
                val pausedDao =
                    object : ProgressPhotoDao by dao {
                        override suspend fun getByIds(ids: List<Long>) =
                            dao.getByIds(ids).also {
                                read.complete(Unit)
                                resume.await()
                            }
                    }
                val repository =
                    ProgressPhotoRepository(
                        dao,
                        ProgressPhotoStorage(),
                        context,
                        Dispatchers.IO,
                        transactionRunner = RoomDatabaseTransactionRunner(database),
                    )
                val paused =
                    ProgressPhotoRepository(
                        pausedDao,
                        ProgressPhotoStorage(),
                        context,
                        Dispatchers.IO,
                        transactionRunner = RoomDatabaseTransactionRunner(database),
                    )
                val deletion = async { paused.deletePhoto(saved.toDomain()) }
                read.await()
                repository.updatePhotoNote(saved.id, "New note")
                assertEquals(saved.copy(note = "New note"), dao.getByIds(listOf(saved.id)).single())
                assertTrue(file.isFile)
                resume.complete(Unit)
                deletion.await()
                assertTrue(dao.getByIds(listOf(saved.id)).isEmpty())
                assertFalse(file.exists())
                assertTrue(repository.getPhotosForProject(project).first().isEmpty())
            }
        }

    @Test fun completedAndActiveSessionColumnsSurviveReopenWithNullableAndExtremeValues() =
        runBlocking {
            val project = database.counterProjectDao().insert(CounterProjectEntity(name = "Sessions"))
            val original =
                SessionEntity(
                    projectId = project,
                    startedAt = 123L,
                    endedAt = 456L,
                    startRow = 9,
                    endRow = 12,
                    durationMinutes = 1,
                    durationSeconds = 61L,
                    rowsWorked = 3,
                    zoneId = null,
                )
            val saved = original.copy(id = database.sessionDao().insert(original))
            val second =
                original.copy(
                    id = 0,
                    durationSeconds = Long.MAX_VALUE,
                    rowsWorked = Int.MAX_VALUE,
                    zoneId = "Europe/Helsinki",
                )
            val extreme = second.copy(id = database.sessionDao().insert(second))
            val active =
                ActiveSessionEntity(
                    sessionToken = "stable-session",
                    projectId = project,
                    startedAtWallMillis = 1_000L,
                    startZoneId = "Europe/Helsinki",
                    startRow = 10,
                    lastObservedRow = 23,
                    trustedLastObservedRow = 14,
                    trustedRowsWorked = 4,
                    pendingRowsWorked = 9,
                    reviewedRowsWorked = 3,
                    reviewedLastObservedRow = 17,
                    unreviewedRowsWorked = 6,
                    checkpointedDurationSeconds = 123L,
                    reviewedDurationBaselineSeconds = 60L,
                    segmentStartedAtWallMillis = 124_000L,
                    segmentStartedElapsedRealtimeMillis = 89_000L,
                    bootCount = null,
                    recoveryReason = "BOOT_IDENTITY_UNAVAILABLE",
                    recoveryIntervalToken = "stable-interval",
                    recoverySuggestedDurationSeconds = null,
                    recoveryPromptShown = true,
                    updatedAtWallMillis = 185_000L,
                )
            database.sessionDao().insertActiveSession(active)
            database.close()
            open()
            assertEquals(listOf(extreme, saved), database.sessionDao().getSessionsForProject(project).first())
            assertEquals(active, database.sessionDao().observeActiveSession().first())
            assertEquals(0, database.sessionDao().deleteActiveSession("stale-token"))
            assertEquals(active, database.sessionDao().getActiveSession())
            assertTrue(runCatching { database.sessionDao().insert(original.copy(projectId = project + 1)) }.isFailure)
            database.counterProjectDao().delete(project)
            assertTrue(
                database
                    .sessionDao()
                    .getSessionsForProject(project)
                    .first()
                    .isEmpty(),
            )
            assertEquals(null, database.sessionDao().getActiveSession())
        }
}
