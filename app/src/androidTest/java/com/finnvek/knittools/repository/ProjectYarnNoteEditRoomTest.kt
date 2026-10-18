package com.finnvek.knittools.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID

class ProjectYarnNoteEditRoomTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "yarn-note-edit-${UUID.randomUUID()}.db"
    private lateinit var database: KnitToolsDatabase
    private lateinit var original: ProjectYarnNote
    private lateinit var sentinel: File

    @Before
    fun setUp(): Unit =
        runBlocking {
            check(context.packageName == "com.finnvek.knittools.activepolicytest")
            database = openDatabase()
            database.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Owner", count = 7, updatedAt = 10))
            database.counterProjectDao().insert(
                CounterProjectEntity(id = 2, name = "Other", count = 11, updatedAt = 20),
            )
            database.yarnCardDao().upsert(YarnCardEntity(id = 3, yarnName = "Library wool", quantityInStash = 9))
            database.projectYarnNoteDao().upsert(
                ProjectYarnNoteEntity(
                    id = 4,
                    projectId = 1,
                    name = "Wool",
                    description = "Soft",
                    quantity = 3,
                    notes = "Cuffs",
                    createdAt = 100,
                    updatedAt = 100,
                ),
            )
            database.projectYarnNoteDao().upsert(ProjectYarnNoteEntity(id = 5, projectId = 2, name = "Other note"))
            original = note()
            sentinel = File(context.filesDir, "$databaseName-sentinel").apply { writeText("unchanged") }
            database.sessionDao().insert(
                SessionEntity(
                    projectId = 1,
                    startedAt = 1,
                    endedAt = 60001,
                    startRow = 2,
                    endRow = 7,
                    durationMinutes = 1,
                ),
            )
            database.projectDocumentDao().insert(
                ProjectDocumentEntity(
                    id = 9,
                    projectId = 1,
                    savedPatternId = null,
                    documentKey = "synthetic-document",
                    label = "Pattern",
                    localPdfUri = sentinel.toURI().toString(),
                    sortOrder = 0,
                    isPrimary = true,
                    currentPage = 2,
                    rowMapping = null,
                    readingLineEnabled = true,
                    readingLineYFraction = 0.4f,
                    readingLineFollowCurrentRow = false,
                    verticalReadingGuideEnabled = false,
                    verticalReadingGuideXFraction = 0.5f,
                    createdAt = 100,
                    updatedAt = 100,
                ),
            )
        }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
        assertEquals("unchanged", sentinel.readText())
        sentinel.delete()
    }

    @Test
    fun updatesSameRowAndPersistsAfterDatabaseReopen() =
        runBlocking {
            val before = database.auditSnapshot()
            assertEquals(ProjectYarnNoteEditResult.Saved, repository().edit(original, draft()))
            assertEdited()
            assertUnrelated(before)
            database.close()
            database = openDatabase()
            assertEdited()
            assertUnrelated(before)
        }

    @Test
    fun linkedCardAndUsageRemainByteForByteUnchanged() =
        runBlocking {
            database.projectYarnNoteDao().updateSavedYarnCardId(4, 3, 101)
            createUsage()
            original = note()
            val before = database.auditSnapshot()
            assertEquals(ProjectYarnNoteEditResult.Saved, repository().edit(original, draft()))
            assertEdited(savedCard = 3)
            assertUnrelated(before)
        }

    @Test
    fun unlinkedUsageRemainsOneRecordWithSameIdentityAndAmounts() =
        runBlocking {
            createUsage()
            val before = database.auditSnapshot()
            assertEquals(ProjectYarnNoteEditResult.Saved, repository().edit(original, draft()))
            assertEdited()
            assertUnrelated(before)
        }

    @Test
    fun blankNameIsRejectedAndCreationNormalizationIsReused() =
        runBlocking {
            val before = database.auditSnapshot()
            assertEquals(ProjectYarnNoteEditResult.InvalidName, repository().edit(original, draft().copy(name = "  ")))
            assertEquals(before, database.auditSnapshot())
            assertEquals(ProjectYarnNoteEditResult.Saved, repository().edit(original, draft().copy(quantity = 0)))
            val edited = note()
            val addedId = repository().save(draft().copy(id = 0, quantity = 0))
            val added = requireNotNull(database.projectYarnNoteDao().getById(addedId))
            assertEquals(
                listOf(added.name, added.description, added.quantity, added.notes),
                listOf(edited.name, edited.description, edited.quantity, edited.notes),
            )
            assertEquals(1, edited.quantity)
        }

    @Test
    fun failureBeforeCommitRollsBackAndRetryUsesOriginalDraft() =
        runBlocking {
            val before = database.auditSnapshot()
            val failing =
                object : DatabaseTransactionRunner {
                    override suspend fun <T> run(block: suspend () -> T): T =
                        RoomDatabaseTransactionRunner(database).run {
                            block()
                            throw IOException("Synthetic pre-commit failure")
                        }
                }
            try {
                repository(failing).edit(original, draft())
                error("Expected failure")
            } catch (_: IOException) {
                assertEquals(before, database.auditSnapshot())
            }
            assertEquals(ProjectYarnNoteEditResult.Saved, repository().edit(original, draft()))
            assertEdited()
            assertUnrelated(before)
        }

    @Test
    fun cancellationBeforeCommitRollsBack() =
        runBlocking {
            cancellation(afterCommit = false)
        }

    @Test
    fun cancellationAfterCommitPreservesCommittedRowAndRetryDoesNotDuplicate() =
        runBlocking {
            cancellation(afterCommit = true)
        }

    @Test
    fun competingEditableFieldChangesAreRejected() =
        runBlocking {
            val changes =
                listOf(
                    original.copy(name = "Newer"),
                    original.copy(description = "Newer"),
                    original.copy(quantity = 8),
                    original.copy(notes = "Newer"),
                )
            changes.forEach { newer ->
                database.projectYarnNoteDao().update(
                    com.finnvek.knittools.data.local.ProjectYarnNoteEntity(
                        4,
                        1,
                        newer.name,
                        newer.description,
                        newer.quantity,
                        newer.notes,
                        createdAt = 100,
                    ),
                )
                val before = database.auditSnapshot()
                assertEquals(ProjectYarnNoteEditResult.Conflict, repository().edit(original, draft()))
                assertEquals(before, database.auditSnapshot())
            }
        }

    @Test
    fun competingDeletionDoesNotRecreateNoteOrUsage() =
        runBlocking {
            createUsage()
            repository().delete(4)
            val before = database.auditSnapshot()
            assertEquals(ProjectYarnNoteEditResult.Missing, repository().edit(original, draft()))
            assertNull(database.projectYarnNoteDao().getById(4))
            assertEquals(before, database.auditSnapshot())
        }

    @Test
    fun wrongProjectCannotBeOverwrittenOrMoved() =
        runBlocking {
            val before = database.auditSnapshot()
            assertEquals(
                ProjectYarnNoteEditResult.Missing,
                repository().edit(original.copy(projectId = 2), draft().copy(projectId = 2)),
            )
            assertEquals(ProjectYarnNoteEditResult.Missing, repository().edit(original, draft().copy(projectId = 2)))
            assertEquals(ProjectYarnNoteEditResult.Missing, repository().edit(original, draft().copy(id = 5)))
            assertEquals(before, database.auditSnapshot())
        }

    @Test
    fun saveToMyYarnWhileEditorIsOpenPreservesNewLinkAndUsage() =
        runBlocking {
            createUsage()
            val cardId = repository().saveToMyYarn(4)
            assertNotNull(cardId)
            val before = database.auditSnapshot()
            assertEquals(ProjectYarnNoteEditResult.Saved, repository().edit(original, draft()))
            assertEdited(savedCard = cardId)
            assertUnrelated(before)
            assertEquals(cardId, repository().saveToMyYarn(4))
            assertUnrelated(before)
        }

    @Test
    fun simultaneousEditsHaveOneWinnerAndOneConflict() =
        runBlocking {
            val before = database.auditSnapshot()
            val results =
                coroutineScope {
                    val first = async(Dispatchers.IO) { repository().edit(original, draft()) }
                    val second =
                        async(Dispatchers.IO) { repository().edit(original, draft().copy(name = "Competitor")) }
                    listOf(first.await(), second.await())
                }
            assertEquals(1, results.count { it == ProjectYarnNoteEditResult.Saved })
            assertEquals(1, results.count { it == ProjectYarnNoteEditResult.Conflict })
            assertUnrelated(before)
        }

    private suspend fun cancellation(afterCommit: Boolean) =
        coroutineScope {
            val before = database.auditSnapshot()
            val reached = CompletableDeferred<Unit>()
            val hold = CompletableDeferred<Unit>()
            val gated =
                object : DatabaseTransactionRunner {
                    override suspend fun <T> run(block: suspend () -> T): T {
                        val result =
                            RoomDatabaseTransactionRunner(database).run {
                                val value = block()
                                if (!afterCommit) {
                                    reached.complete(Unit)
                                    hold.await()
                                }
                                value
                            }
                        if (afterCommit) {
                            reached.complete(Unit)
                            hold.await()
                        }
                        return result
                    }
                }
            val pending = async(Dispatchers.IO) { repository(gated).edit(original, draft()) }
            reached.await()
            pending.cancelAndJoin()
            if (afterCommit) {
                assertEdited()
                val committed = database.auditSnapshot()
                assertEquals(ProjectYarnNoteEditResult.Saved, repository().edit(original, draft()))
                assertEquals(committed, database.auditSnapshot())
            } else {
                assertEquals(before, database.auditSnapshot())
            }
            assertUnrelated(before)
        }

    private fun openDatabase() = Room.databaseBuilder(context, KnitToolsDatabase::class.java, databaseName).build()

    private fun repository(
        runner: DatabaseTransactionRunner = RoomDatabaseTransactionRunner(database),
    ): ProjectYarnNoteRepository {
        val yarn =
            YarnCardRepository(database.yarnCardDao(), database.counterProjectDao(), context, runner, Dispatchers.IO)
        return ProjectYarnNoteRepository(database.projectYarnNoteDao(), yarn, runner, database.projectYarnUsageDao())
    }

    private suspend fun createUsage() {
        val usage =
            ProjectYarnUsageRepository(
                database.projectYarnUsageDao(),
                database.projectYarnNoteDao(),
                database.yarnCardDao(),
                RoomDatabaseTransactionRunner(database),
                Dispatchers.IO,
            )
        assertTrue(
            usage.create(
                1,
                YarnUsageSource(projectYarnNoteId = 4),
                YarnUsageAmounts(1200.0, 600.0, 350.0),
                "Wool",
            ) is YarnUsageResult.Created,
        )
    }

    private suspend fun note() = requireNotNull(database.projectYarnNoteDao().getById(4)).toDomain()

    private fun draft() =
        original.copy(name = "  Edited  ", description = "  Silk  ", quantity = 6, notes = "  Sleeves  ")

    private suspend fun assertEdited(savedCard: Long? = null) {
        val value = note()
        assertEquals(
            original.copy(
                name = "Edited",
                description = "Silk",
                quantity = 6,
                notes = "Sleeves",
                savedYarnCardId = savedCard,
                updatedAt = value.updatedAt,
            ),
            value,
        )
        assertEquals(
            1,
            database
                .projectYarnNoteDao()
                .observeForProject(1)
                .first()
                .size,
        )
    }

    private fun assertUnrelated(before: Map<String, List<List<String?>>>) {
        assertEquals(before - "project_yarn_notes", database.auditSnapshot() - "project_yarn_notes")
        assertEquals(
            before.getValue("project_yarn_notes").filter { it.first() != "4" },
            database.auditSnapshot().getValue("project_yarn_notes").filter {
                it.first() !=
                    "4"
            },
        )
    }
}
