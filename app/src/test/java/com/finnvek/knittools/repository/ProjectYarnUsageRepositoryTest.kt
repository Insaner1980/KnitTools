package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectYarnNoteDao
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.ProjectYarnUsageDao
import com.finnvek.knittools.data.local.ProjectYarnUsageEntity
import com.finnvek.knittools.data.local.ProjectYarnUsageRelations
import com.finnvek.knittools.data.local.ResolvedProjectYarnUsage
import com.finnvek.knittools.data.local.ResolvedUsageNote
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.YarnUsageProjectId
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.domain.model.YarnUsageSourceStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectYarnUsageRepositoryTest {
    private val amounts = YarnUsageAmounts(1_200.0, 600.0, 350.0, 200.0, 100.0)

    @Test
    fun `relations merge paired notes cards and orphan usages deterministically`() {
        val pairedCard = card(id = 3, brand = "Brand", name = "Wool", createdAt = 30)
        val looseCard = card(id = 6, name = "Sock", createdAt = 60)
        val blankCard = card(id = 7, createdAt = 70)
        val pairedUsage = usage(id = 100, cardId = 3, noteId = 4, snapshot = "Paired")
        val looseUsage = usage(id = 101, cardId = 6, snapshot = "Loose")
        val unlinkedUsage = usage(id = 102, cardId = 8, snapshot = "Old card")
        val unavailableUsage = usage(id = 103, snapshot = "Deleted source")
        val relations =
            relations(
                notes =
                    listOf(
                        resolvedNote(id = 4, name = "Mohair", createdAt = 10, card = pairedCard),
                        resolvedNote(id = 5, name = "Newest note", createdAt = 20),
                    ),
                cards = listOf(pairedCard, looseCard, blankCard),
                usages =
                    listOf(
                        resolvedUsage(pairedUsage, pairedCard),
                        resolvedUsage(looseUsage, looseCard),
                        resolvedUsage(unlinkedUsage, card(id = 8, brand = "Old", name = "Yarn")),
                        resolvedUsage(unavailableUsage),
                    ),
            )

        val items = relations.items()

        assertEquals(listOf("Newest note", "Mohair", "", "Sock", "Old Yarn", "Deleted source"), items.map { it.name })
        assertEquals(pairedUsage.toDomain(), items[1].usage)
        assertEquals(looseUsage.toDomain(), items[3].usage)
        assertEquals(YarnUsageSourceStatus.UNLINKED, items[4].status)
        assertEquals(YarnUsageSourceStatus.UNAVAILABLE, items[5].status)
        assertEquals("Brand Wool", pairedCard.usageName())
        assertEquals("Sock", looseCard.usageName())
    }

    @Test
    fun `observation maps a project snapshot and preserves missing project`() =
        runTest {
            val snapshot = relations(cards = listOf(card(id = 3, name = "Wool")))

            val available = repository(FakeUsageDao(snapshot)).observeForProject(1).first()
            val missing = repository(FakeUsageDao(null)).observeForProject(1).first()

            assertEquals("Wool", available?.single()?.name)
            assertNull(missing)
        }

    @Test
    fun `create validates amounts source ownership duplicates and fallback name`() =
        runTest {
            val card = card(id = 3)
            val dao = FakeUsageDao(relations(cards = listOf(card)))
            val repository = repository(dao)

            assertEquals(
                YarnUsageResult.InvalidAmounts,
                repository.create(1, YarnUsageSource(yarnCardId = 3), YarnUsageAmounts(), "Fallback"),
            )
            assertEquals(
                YarnUsageResult.InvalidConversion,
                repository.create(
                    1,
                    YarnUsageSource(yarnCardId = 3),
                    amounts.copy(gramsPerSkein = null),
                    "Fallback",
                ),
            )

            val created =
                repository.create(
                    1,
                    YarnUsageSource(yarnCardId = 3),
                    amounts,
                    "  Fallback  ",
                ) as YarnUsageResult.Created
            assertEquals("Fallback", created.usage.sourceNameSnapshot)
            assertEquals(amounts, created.usage.amounts)
            assertEquals(88L, created.usage.id)

            dao.snapshot = relations(cards = listOf(card), usages = listOf(resolvedUsage(requireNotNull(dao.row))))
            assertEquals(
                YarnUsageResult.AlreadyExists(created.usage),
                repository.create(1, YarnUsageSource(yarnCardId = 3), amounts, "Other"),
            )
        }

    @Test
    fun `create distinguishes missing project source and foreign source`() =
        runTest {
            val dao = FakeUsageDao(null)
            val noteDao = mockk<ProjectYarnNoteDao>(relaxed = true)
            val cardDao = mockk<YarnCardDao>(relaxed = true)
            val repository = repository(dao, noteDao, cardDao)
            coEvery { noteDao.getById(any()) } returns null
            coEvery { cardDao.getCard(any()) } returns null

            assertEquals(
                YarnUsageResult.ProjectMissing,
                repository.create(1, YarnUsageSource(projectYarnNoteId = 4), amounts, "Yarn"),
            )

            dao.snapshot = relations()
            assertEquals(
                YarnUsageResult.SourceMissing,
                repository.create(1, YarnUsageSource(), amounts, "Yarn"),
            )
            assertEquals(
                YarnUsageResult.SourceMissing,
                repository.create(1, YarnUsageSource(yarnCardId = 404), amounts, "Yarn"),
            )

            coEvery { noteDao.getById(4) } returns ProjectYarnNoteEntity(id = 4, projectId = 2, name = "Foreign")
            coEvery { cardDao.getCard(3) } returns card(id = 3, name = "Foreign")
            assertEquals(
                YarnUsageResult.SourceNotOwnedByProject,
                repository.create(1, YarnUsageSource(projectYarnNoteId = 4), amounts, "Yarn"),
            )
            assertEquals(
                YarnUsageResult.SourceNotOwnedByProject,
                repository.create(1, YarnUsageSource(yarnCardId = 3), amounts, "Yarn"),
            )

            dao.snapshot = relations(cards = listOf(card(id = 7)))
            assertEquals(
                YarnUsageResult.SourceMissing,
                repository.create(1, YarnUsageSource(yarnCardId = 7), amounts, "   "),
            )
        }

    @Test
    fun `update and delete return typed current revision results`() =
        runTest {
            val dao = FakeUsageDao(relations())
            val repository = repository(dao)

            assertEquals(YarnUsageResult.UsageMissing, repository.update(1, 9, 10, amounts))
            dao.row = usage(id = 9, projectId = 2, updatedAt = 10)
            assertEquals(YarnUsageResult.SourceNotOwnedByProject, repository.update(1, 9, 10, amounts))
            dao.row = usage(id = 9, updatedAt = 11)
            assertEquals(YarnUsageResult.StaleAction, repository.update(1, 9, 10, amounts))

            val updated = repository.update(1, 9, 11, amounts.copy(usedMeters = 400.0)) as YarnUsageResult.Updated
            assertTrue(updated.usage.updatedAt > 11)
            assertEquals(400.0, updated.usage.amounts.usedMeters)
            assertEquals(YarnUsageResult.Deleted, repository.delete(1, 9, updated.usage.updatedAt))
            assertEquals(YarnUsageResult.UsageMissing, repository.delete(1, 9, updated.usage.updatedAt))

            dao.snapshot = null
            assertEquals(YarnUsageResult.ProjectMissing, repository.delete(1, 9, updated.usage.updatedAt))
        }

    @Test
    fun `mutation converts persistence failure but propagates cancellation`() =
        runTest {
            val snapshot = relations(cards = listOf(card(id = 3, name = "Wool")))
            val failure =
                repository(
                    FakeUsageDao(snapshot),
                    runner = throwingRunner(IllegalStateException("failure")),
                )
            val cancellation = CancellationException("cancelled")
            val cancelled = repository(FakeUsageDao(snapshot), runner = throwingRunner(cancellation))

            assertEquals(
                YarnUsageResult.PersistenceFailure,
                failure.create(1, YarnUsageSource(yarnCardId = 3), amounts, "Wool"),
            )
            try {
                cancelled.create(1, YarnUsageSource(yarnCardId = 3), amounts, "Wool")
                throw AssertionError("CancellationException expected")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }

    private fun repository(
        dao: ProjectYarnUsageDao,
        noteDao: ProjectYarnNoteDao = mockk(relaxed = true),
        cardDao: YarnCardDao = mockk(relaxed = true),
        runner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ) = ProjectYarnUsageRepository(
        dao = dao,
        noteDao = noteDao,
        cardDao = cardDao,
        transactionRunner = runner,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun throwingRunner(error: Throwable) =
        object : DatabaseTransactionRunner {
            override suspend fun <T> run(block: suspend () -> T): T = throw error
        }

    private fun relations(
        notes: List<ResolvedUsageNote> = emptyList(),
        cards: List<YarnCardEntity> = emptyList(),
        usages: List<ResolvedProjectYarnUsage> = emptyList(),
    ) = ProjectYarnUsageRelations(YarnUsageProjectId(1), notes, cards, usages)

    private fun resolvedNote(
        id: Long,
        name: String,
        createdAt: Long,
        card: YarnCardEntity? = null,
    ) = ResolvedUsageNote(
        ProjectYarnNoteEntity(id = id, projectId = 1, name = name, savedYarnCardId = card?.id, createdAt = createdAt),
        card,
    )

    private fun resolvedUsage(
        usage: ProjectYarnUsageEntity,
        card: YarnCardEntity? = null,
    ) = ResolvedProjectYarnUsage(usage, card, null)

    private fun card(
        id: Long,
        brand: String = "",
        name: String = "",
        createdAt: Long = id,
    ) = YarnCardEntity(id = id, brand = brand, yarnName = name, linkedProjectId = 1, createdAt = createdAt)

    private fun usage(
        id: Long,
        projectId: Long = 1,
        cardId: Long? = null,
        noteId: Long? = null,
        snapshot: String = "Yarn",
        updatedAt: Long = 10,
    ) = ProjectYarnUsageEntity(
        id = id,
        projectId = projectId,
        yarnCardId = cardId,
        projectYarnNoteId = noteId,
        sourceNameSnapshot = snapshot,
        plannedMeters = amounts.plannedMeters,
        allocatedMeters = amounts.allocatedMeters,
        usedMeters = amounts.usedMeters,
        metersPerSkein = amounts.metersPerSkein,
        gramsPerSkein = amounts.gramsPerSkein,
        createdAt = 1,
        updatedAt = updatedAt,
    )

    private class FakeUsageDao(
        var snapshot: ProjectYarnUsageRelations?,
    ) : ProjectYarnUsageDao {
        var row: ProjectYarnUsageEntity? = null

        override fun observeProject(projectId: Long): Flow<ProjectYarnUsageRelations?> = flowOf(snapshot)

        override suspend fun getProject(projectId: Long): ProjectYarnUsageRelations? = snapshot

        override suspend fun getById(id: Long): ProjectYarnUsageEntity? = row?.takeIf { it.id == id }

        override suspend fun getForSource(
            projectId: Long,
            cardId: Long?,
            noteId: Long?,
        ): List<ProjectYarnUsageEntity> = emptyList()

        override suspend fun insert(usage: ProjectYarnUsageEntity): Long {
            row = usage.copy(id = 88)
            return 88
        }

        override suspend fun update(usage: ProjectYarnUsageEntity): Int {
            row = usage
            return 1
        }

        override suspend fun delete(
            id: Long,
            projectId: Long,
        ): Int {
            val current = row
            if (current?.id != id || current.projectId != projectId) return 0
            row = null
            return 1
        }
    }
}
