package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.model.YarnCard
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YarnCardRepositoryDomainApiTest {
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `yarn card repository exposes domain models and writes entities`() =
        runTest {
            val dao =
                FakeYarnCardDao(
                    yarnCards =
                        listOf(
                            YarnCardEntity(
                                id = 2L,
                                brand = "Finn Wool",
                                yarnName = "Soft DK",
                                quantityInStash = 3,
                                status = "IN_USE",
                            ),
                        ),
                )
            val repository =
                YarnCardRepository(
                    dao,
                    RepositoryDomainFakeCounterProjectDao(),
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val cards: List<YarnCard> = repository.getAllCards().first()
            val card: YarnCard? = repository.getCard(2L)
            val savedId =
                repository.saveCard(
                    YarnCard(
                        brand = "Novita",
                        yarnName = "Nalle",
                        createdAt = 123L,
                        quantityInStash = 5,
                        status = "IN_STASH",
                    ),
                )

            assertEquals("Soft DK", cards.single().yarnName)
            assertEquals("Finn Wool", card?.brand)
            assertEquals(88L, savedId)
            assertEquals(
                YarnCardEntity(
                    brand = "Novita",
                    yarnName = "Nalle",
                    createdAt = 123L,
                    quantityInStash = 5,
                    status = "IN_STASH",
                ),
                dao.lastUpserted,
            )
        }

    @Test
    fun `yarn card repository removes deleted card ids from projects`() =
        runTest {
            val yarnDao =
                FakeYarnCardDao(
                    yarnCards =
                        listOf(
                            YarnCardEntity(id = 2L, yarnName = "Deleted"),
                        ),
                )
            val projectDao =
                RepositoryDomainFakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "1,2,3"),
                            CounterProjectEntity(id = 11L, yarnCardIds = "2"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            repository.deleteCards(listOf(2L))

            assertEquals(mapOf(10L to "1,3", 11L to ""), projectDao.updatedYarnCardIds)
            assertEquals(listOf(2L), yarnDao.deletedIds)
        }

    @Test
    fun `yarn card repository reports rejected detail updates`() =
        runTest {
            val yarnDao = FakeYarnCardDao()
            val repository =
                YarnCardRepository(
                    yarnDao,
                    RepositoryDomainFakeCounterProjectDao(),
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            assertEquals(false, repository.updateQuantity(99L, 4))
            assertEquals(false, repository.updateStatus(99L, "USED_UP"))
            assertEquals(false, repository.updateLinkedProjectId(99L, null))
        }

    @Test
    fun `yarn card save keeps linked project ids consistent`() =
        runTest {
            val yarnDao = FakeYarnCardDao()
            val projectDao =
                RepositoryDomainFakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "1,2"),
                            CounterProjectEntity(id = 11L, yarnCardIds = "3"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val savedId =
                repository.saveCard(
                    YarnCard(
                        brand = "Novita",
                        yarnName = "Nalle",
                        linkedProjectId = 10L,
                    ),
                )

            assertEquals(88L, savedId)
            assertEquals(10L, yarnDao.lastUpserted?.linkedProjectId)
            assertEquals(mapOf(10L to "1,2,88"), projectDao.updatedYarnCardIds)
        }

    @Test
    fun `yarn card relink removes stale project link when explicitly unlinked`() =
        runTest {
            val yarnDao =
                FakeYarnCardDao(
                    yarnCards =
                        listOf(
                            YarnCardEntity(id = 5L, yarnName = "Sock", linkedProjectId = 10L),
                        ),
                )
            val projectDao =
                RepositoryDomainFakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "1,5"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val updated = repository.updateLinkedProjectId(5L, null)

            assertEquals(true, updated)
            assertEquals(5L to null, yarnDao.lastLinkedProjectUpdate)
            assertEquals(mapOf(10L to "1"), projectDao.updatedYarnCardIds)
        }

    @Test
    fun `yarn card relink moves card id between projects`() =
        runTest {
            val yarnDao =
                FakeYarnCardDao(
                    yarnCards =
                        listOf(
                            YarnCardEntity(id = 5L, yarnName = "Sock", linkedProjectId = 10L),
                        ),
                )
            val projectDao =
                RepositoryDomainFakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "1,5"),
                            CounterProjectEntity(id = 11L, yarnCardIds = "2"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val updated = repository.updateLinkedProjectId(5L, 11L)

            assertEquals(true, updated)
            assertEquals(5L to 11L, yarnDao.lastLinkedProjectUpdate)
            assertEquals(mapOf(10L to "1", 11L to "2,5"), projectDao.updatedYarnCardIds)
        }

    @Test
    fun `yarn card save preserves existing detail-only fields when editing same card`() =
        runTest {
            val existingYarnCard = detailedYarnCardEntity()
            val yarnDao =
                FakeYarnCardDao(
                    yarnCards = listOf(existingYarnCard),
                )
            val projectDao =
                RepositoryDomainFakeCounterProjectDao(
                    projects =
                        listOf(
                            CounterProjectEntity(id = 10L, yarnCardIds = "5"),
                        ),
                )
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    ImmediateDatabaseTransactionRunner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            val savedId = repository.saveCard(editedYarnCard())

            assertEquals(5L, savedId)
            assertEquals(
                existingYarnCard.copy(
                    brand = "New brand",
                    yarnName = "New yarn",
                    colorName = "New color",
                    colorNumber = "34",
                    dyeLot = "B",
                    weightCategory = "Fingering",
                ),
                yarnDao.lastUpserted,
            )
            assertEquals(emptyMap<Long, String>(), projectDao.updatedYarnCardIds)
        }
}
