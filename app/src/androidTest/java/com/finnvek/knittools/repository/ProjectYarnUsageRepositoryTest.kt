package com.finnvek.knittools.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.model.ProjectYarnUsage
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.domain.model.YarnUsageSourceStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectYarnUsageRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: ProjectYarnUsageRepository
    private lateinit var yarnRepository: YarnCardRepository
    private val amounts = YarnUsageAmounts(1200.0, 600.0, 350.0, 200.0, 100.0)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
        repository = newRepository()
        yarnRepository =
            YarnCardRepository(
                database.yarnCardDao(),
                database.counterProjectDao(),
                context,
                RoomDatabaseTransactionRunner(database),
                Dispatchers.IO,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun newRepository(runner: DatabaseTransactionRunner = RoomDatabaseTransactionRunner(database)) =
        ProjectYarnUsageRepository(
            database.projectYarnUsageDao(),
            database.projectYarnNoteDao(),
            database.yarnCardDao(),
            runner,
            Dispatchers.IO,
        )

    private fun noteRepository(runner: DatabaseTransactionRunner = RoomDatabaseTransactionRunner(database)) =
        ProjectYarnNoteRepository(database.projectYarnNoteDao(), yarnRepository, runner, database.projectYarnUsageDao())

    private suspend fun seed(pair: Boolean = false) {
        database.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Project", count = 7, updatedAt = 10))
        database.counterProjectDao().insert(CounterProjectEntity(id = 2, name = "Other", updatedAt = 20))
        database.yarnCardDao().upsert(
            YarnCardEntity(
                id = 3,
                yarnName = "Wool",
                linkedProjectId = 1,
                quantityInStash = 9,
                lengthMeters = "100 g / 200 m",
            ),
        )
        database.projectYarnNoteDao().upsert(
            ProjectYarnNoteEntity(id = 4, projectId = 1, name = "Mohair", savedYarnCardId = if (pair) 3 else null),
        )
    }

    private suspend fun create(source: YarnUsageSource = YarnUsageSource(projectYarnNoteId = 4)): ProjectYarnUsage =
        (repository.create(1, source, amounts, "Fallback") as YarnUsageResult.Created).usage

    @Test
    fun noteAndCardCreationKeepIndependentNullableAmountsAndNeverParseMetadata() =
        runTest {
            seed()
            val note = create()
            val card =
                repository.create(
                    1,
                    YarnUsageSource(yarnCardId = 3),
                    YarnUsageAmounts(usedMeters = 0.0),
                    "Fallback",
                ) as YarnUsageResult.Created
            assertEquals(amounts, note.amounts)
            assertEquals(YarnUsageAmounts(usedMeters = 0.0), card.usage.amounts)
            assertEquals("Mohair", note.sourceNameSnapshot)
            assertEquals("Wool", card.usage.sourceNameSnapshot)
            assertEquals(9, database.yarnCardDao().getCard(3)?.quantityInStash)
            assertEquals(10L, database.counterProjectDao().getProject(1)?.updatedAt)
            assertEquals(7, database.counterProjectDao().getProject(1)?.count)
        }

    @Test
    fun noteCardPairResolvesOneRecordFromEitherSource() =
        runTest {
            seed(pair = true)
            val usage = create()
            assertEquals(YarnUsageSource(3, 4), usage.source)
            assertEquals(
                YarnUsageResult.AlreadyExists(usage),
                repository.create(1, YarnUsageSource(yarnCardId = 3), amounts, "Other"),
            )
            assertEquals(
                YarnUsageResult.AlreadyExists(usage),
                repository.create(1, YarnUsageSource(3, 4), amounts, "Other"),
            )
            val items = requireNotNull(repository.observeForProject(1).first())
            assertEquals(1, items.size)
            assertEquals(usage, items.single().usage)
        }

    @Test
    fun concurrentRepeatedSaveIsTransactionSafe() =
        runTest {
            seed()
            val results =
                (1..8)
                    .map {
                        async(
                            Dispatchers.IO,
                        ) { repository.create(1, YarnUsageSource(projectYarnNoteId = 4), amounts, "Yarn") }
                    }.awaitAll()
            assertEquals(1, results.count { it is YarnUsageResult.Created })
            assertEquals(7, results.count { it is YarnUsageResult.AlreadyExists })
        }

    @Test
    fun editDeleteAndStaleRevisionsHaveTypedResults() =
        runTest {
            seed()
            val original = create()
            val updated =
                repository.update(
                    1,
                    original.id,
                    original.updatedAt,
                    amounts.copy(usedMeters = 700.0),
                ) as YarnUsageResult.Updated
            assertTrue(updated.usage.updatedAt > original.updatedAt)
            assertEquals(YarnUsageResult.StaleAction, repository.update(1, original.id, original.updatedAt, amounts))
            assertEquals(YarnUsageResult.StaleAction, repository.delete(1, original.id, original.updatedAt))
            assertEquals(YarnUsageResult.Deleted, repository.delete(1, original.id, updated.usage.updatedAt))
            assertEquals(YarnUsageResult.UsageMissing, repository.delete(1, original.id, updated.usage.updatedAt))
            assertEquals(
                YarnUsageResult.UsageMissing,
                repository.update(1, original.id, updated.usage.updatedAt, amounts),
            )
            assertNotNull(database.projectYarnNoteDao().getById(4))
        }

    @Test
    fun missingAndForeignSourcesCannotStartTracking() =
        runTest {
            seed()
            assertEquals(
                YarnUsageResult.ProjectMissing,
                repository.create(404, YarnUsageSource(projectYarnNoteId = 4), amounts, "Yarn"),
            )
            listOf(
                YarnUsageSource(),
                YarnUsageSource(yarnCardId = 404),
                YarnUsageSource(projectYarnNoteId = 404),
            ).forEach { source ->
                assertEquals(YarnUsageResult.SourceMissing, repository.create(1, source, amounts, "Yarn"))
            }
            assertEquals(
                YarnUsageResult.SourceNotOwnedByProject,
                repository.create(2, YarnUsageSource(yarnCardId = 3), amounts, "Yarn"),
            )
            assertEquals(
                YarnUsageResult.SourceNotOwnedByProject,
                repository.create(2, YarnUsageSource(projectYarnNoteId = 4), amounts, "Yarn"),
            )
            assertEquals(
                YarnUsageResult.SourceNotOwnedByProject,
                repository.create(1, YarnUsageSource(3, 4), amounts, "Yarn"),
            )
            yarnRepository.updateLinkedProjectId(3, null)
            assertEquals(
                YarnUsageResult.SourceNotOwnedByProject,
                repository.create(1, YarnUsageSource(yarnCardId = 3), amounts, "Yarn"),
            )
        }

    @Test
    fun invalidAmountsAndConversionPairsCannotBePersisted() =
        runTest {
            seed()
            listOf(
                YarnUsageAmounts(),
                YarnUsageAmounts(usedMeters = -1.0),
                YarnUsageAmounts(usedMeters = Double.NaN),
                YarnUsageAmounts(usedMeters = Double.POSITIVE_INFINITY),
            ).forEach { invalid ->
                assertEquals(
                    YarnUsageResult.InvalidAmounts,
                    repository.create(1, YarnUsageSource(projectYarnNoteId = 4), invalid, "Yarn"),
                )
            }
            listOf(
                amounts.copy(gramsPerSkein = null),
                amounts.copy(metersPerSkein = 0.0),
                amounts.copy(gramsPerSkein = -1.0),
            ).forEach { invalid ->
                assertEquals(
                    YarnUsageResult.InvalidConversion,
                    repository.create(1, YarnUsageSource(projectYarnNoteId = 4), invalid, "Yarn"),
                )
            }
        }

    @Test
    fun saveToMyYarnAttachesSameUsageAtomicallyAndRepeatedCallReusesCard() =
        runTest {
            seed()
            val usage = create()
            val cardId = requireNotNull(noteRepository().saveToMyYarn(4))
            assertEquals(cardId, noteRepository().saveToMyYarn(4))
            val row = requireNotNull(database.projectYarnUsageDao().getById(usage.id)).toDomain()
            assertEquals(usage.copy(source = YarnUsageSource(cardId, 4)), row)
            assertEquals(cardId, database.projectYarnNoteDao().getById(4)?.savedYarnCardId)
            assertEquals(
                YarnUsageResult.AlreadyExists(row),
                repository.create(1, YarnUsageSource(yarnCardId = cardId), amounts, "Yarn"),
            )
        }

    @Test
    fun saveToMyYarnWithoutTrackingDoesNotCreateUsage() =
        runTest {
            seed()
            noteRepository().saveToMyYarn(4)
            assertTrue(requireNotNull(repository.observeForProject(1).first()).all { it.usage == null })
        }

    @Test
    fun saveToMyYarnFailureRollsBackCardLinksNoteAndUsage() =
        runTest {
            seed()
            val usage = create()
            val projectBefore = database.counterProjectDao().getProject(1)
            val cardsBefore = database.yarnCardDao().getAllCards().first()
            try {
                noteRepository(failingRunner()).saveToMyYarn(4)
                fail("Expected rollback")
            } catch (_: IllegalStateException) {
                assertEquals(cardsBefore, database.yarnCardDao().getAllCards().first())
            }
            assertNull(database.projectYarnNoteDao().getById(4)?.savedYarnCardId)
            assertEquals(projectBefore, database.counterProjectDao().getProject(1))
            assertEquals(usage, database.projectYarnUsageDao().getById(usage.id)?.toDomain())
        }

    @Test
    fun sourceDeletionRetainsAmountsSnapshotAndEditableSourceLessRecord() =
        runTest {
            seed(pair = true)
            val usage = create()
            database.projectYarnNoteDao().delete(4)
            assertEquals(
                YarnUsageSource(yarnCardId = 3),
                database
                    .projectYarnUsageDao()
                    .getById(usage.id)
                    ?.toDomain()
                    ?.source,
            )
            yarnRepository.deleteCard(3)
            val orphan = requireNotNull(repository.observeForProject(1).first()).single()
            assertEquals(YarnUsageSource(), orphan.source)
            assertEquals(YarnUsageSourceStatus.UNAVAILABLE, orphan.status)
            assertEquals("Mohair", orphan.name)
            assertEquals(amounts, orphan.usage?.amounts)
            val updated =
                repository.update(
                    1,
                    usage.id,
                    usage.updatedAt,
                    amounts.copy(plannedMeters = null),
                ) as YarnUsageResult.Updated
            assertEquals(YarnUsageResult.Deleted, repository.delete(1, usage.id, updated.usage.updatedAt))
        }

    @Test
    fun cardDeletionBeforeNoteDeletionKeepsNoteIdentity() =
        runTest {
            seed(pair = true)
            val usage = create()
            yarnRepository.deleteCard(3)
            val row = requireNotNull(database.projectYarnUsageDao().getById(usage.id))
            assertNull(row.yarnCardId)
            assertEquals(4L, row.projectYarnNoteId)
            assertEquals(amounts, row.toDomain().amounts)
            database.projectYarnNoteDao().delete(4)
            assertNull(database.projectYarnUsageDao().getById(usage.id)?.projectYarnNoteId)
        }

    @Test
    fun unlinkAndRenameKeepSnapshotAndAmountsWhileCompletionReopeningAndDeletionRespectOwnership() =
        runTest {
            seed()
            val usage = create(YarnUsageSource(yarnCardId = 3))
            yarnRepository.updateLinkedProjectId(3, null)
            val item = requireNotNull(repository.observeForProject(1).first()).single { it.usage != null }
            assertEquals(YarnUsageSourceStatus.UNLINKED, item.status)
            assertEquals(amounts, item.usage?.amounts)
            database.openHelper.writableDatabase.execSQL("UPDATE yarn_cards SET yarnName = 'Renamed' WHERE id = 3")
            assertEquals("Wool", database.projectYarnUsageDao().getById(usage.id)?.sourceNameSnapshot)
            database.counterProjectDao().archiveProject(1, 7, 100, 100)
            assertEquals(usage, database.projectYarnUsageDao().getById(usage.id)?.toDomain())
            database.counterProjectDao().reactivateProject(1, 101)
            assertEquals(usage, database.projectYarnUsageDao().getById(usage.id)?.toDomain())
            database.counterProjectDao().delete(1)
            assertNull(database.projectYarnUsageDao().getById(usage.id))
            assertEquals(9, database.yarnCardDao().getCard(3)?.quantityInStash)
        }

    @Test
    fun databaseIndexesRejectDuplicateSourcesAndForeignKeysRejectInvalidOwners() =
        runTest {
            seed(pair = true)
            val usage = create()
            val row = requireNotNull(database.projectYarnUsageDao().getById(usage.id)).copy(id = 0)
            listOf(
                row.copy(projectYarnNoteId = null),
                row.copy(yarnCardId = null),
                row.copy(projectId = 404),
            ).forEach { duplicate ->
                try {
                    database.projectYarnUsageDao().insert(duplicate)
                    fail("Expected constraint")
                } catch (
                    _: android.database.sqlite.SQLiteConstraintException,
                ) {
                }
            }
            assertTrue(
                database.openHelper.writableDatabase
                    .query("PRAGMA foreign_key_check")
                    .use { !it.moveToFirst() },
            )
        }

    @Test
    fun failedMutationRollsBackAndCancellationPropagates() =
        runTest {
            seed()
            val failure =
                newRepository(
                    failingRunner(),
                ).create(1, YarnUsageSource(projectYarnNoteId = 4), amounts, "Yarn")
            assertEquals(YarnUsageResult.PersistenceFailure, failure)
            try {
                newRepository(
                    failingRunner(cancel = true),
                ).create(1, YarnUsageSource(projectYarnNoteId = 4), amounts, "Yarn")
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
                assertTrue(requireNotNull(repository.observeForProject(1).first()).all { it.usage == null })
            }
        }

    private fun failingRunner(cancel: Boolean = false) =
        object : DatabaseTransactionRunner {
            override suspend fun <T> run(block: suspend () -> T): T =
                database.withTransaction {
                    block()
                    if (cancel) throw CancellationException("Fixture cancellation")
                    error("Fixture rollback")
                }
        }
}
