package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterRepositoryInsightsTest {
    private val dao = mockk<SessionDao>()

    @Test fun firstLocalDateIncludesLaterUtcStartsAndUsesFixedSizeMetadataBatches() =
        runTest {
            val repository = repository(StandardTestDispatcher(testScheduler))
            every { dao.observeSessionChanges() } returns flowOf(true)
            coEvery { dao.hasAnySessions() } returns true
            coEvery { dao.getSessionProjectActivity(7) } returns emptyList()
            val midnight =
                java.time.Instant
                    .parse("2026-01-02T00:00:00Z")
                    .toEpochMilli()
            coEvery { dao.getFirstSessionStart(7) } returns midnight
            coEvery { dao.getInsightFirstDateBatch(7, Long.MIN_VALUE, midnight + 129600000L) } returns
                listOf(
                    com.finnvek.knittools.data.local
                        .SessionStart(1, midnight, "+18:00"),
                    com.finnvek.knittools.data.local
                        .SessionStart(2, midnight + 3600_000, "-18:00"),
                )
            coEvery { dao.getInsightFirstDateBatch(7, 2, midnight + 129600000L) } returns emptyList()
            coEvery { dao.getProjectInsightSessionBatch(7, Long.MIN_VALUE) } returns emptyList()
            val result =
                repository
                    .observeSessionsForInsights(7, null, java.time.ZoneOffset.UTC, {
                        it.firstSessionDate
                    }) { _, _ -> }
                    .first()
            assertEquals(java.time.LocalDate.of(2026, 1, 1), result)
        }

    @Test fun allTimeFoldsEveryFixedSizeBatchWithoutAnArchiveLimit() =
        runTest {
            val repository = repository(StandardTestDispatcher(testScheduler))
            every { dao.observeSessionChanges() } returns flowOf(true)
            stubEmptySessionMetadata()
            // Reuse a single 256-row fixture; only IDs vary between reads.
            var page = 0
            coEvery { dao.getInsightSessionBatch(any()) } answers {
                if (page++ < 41) List(256) { row -> session((page - 1L) * 256 + row + 1) } else emptyList()
            }
            val result =
                repository
                    .observeSessionsForInsights(
                        null,
                        null,
                        java.time.ZoneOffset.UTC,
                        { longArrayOf(0) },
                    ) { total, batch ->
                        assertTrue(batch.size <= 256)
                        total[0] += batch.size
                    }.first()
            assertEquals(10_496L, result[0])
            coVerify(exactly = 1) { dao.getInsightSessionBatch(Long.MIN_VALUE) }
            coVerify(exactly = 1) { dao.getInsightSessionBatch(10_496L) }
        }

    @Test fun rangeAndProjectAreBothPassedToEachBatchQuery() =
        runTest {
            val repository = repository(StandardTestDispatcher(testScheduler))
            every { dao.observeSessionChanges() } returns flowOf(true)
            stubEmptySessionMetadata(projectId = 7)
            coEvery { dao.getProjectInsightSessionBatchSince(7, Long.MIN_VALUE, 123) } returns listOf(session(42))
            coEvery { dao.getProjectInsightSessionBatchSince(7, 42, 123) } returns emptyList()
            val count =
                repository
                    .observeSessionsForInsights(7, 123, java.time.ZoneOffset.UTC, { intArrayOf(0) }) { total, batch ->
                        total[0] +=
                            batch.size
                    }.first()
            assertEquals(1, count[0])
            coVerify(exactly = 0) { dao.getInsightSessionBatch(any()) }
            coEvery { dao.getSessionProjectActivity(null) } returns emptyList()
            coEvery { dao.getInsightSessionBatchSince(Long.MIN_VALUE, 123) } returns emptyList()
            assertTrue(
                repository
                    .observeSessionsForInsights(null, 123, java.time.ZoneOffset.UTC, {
                        it.hasAnySessionData
                    }) { _, _ -> }
                    .first(),
            )
            coEvery { dao.hasAnySessions() } returns false
            coEvery { dao.getInsightSessionBatch(any()) } returns emptyList()
            assertEquals(
                false,
                repository
                    .observeSessionsForInsights(null, null, java.time.ZoneOffset.UTC, {
                        it.hasAnySessionData
                    }) { _, _ -> }
                    .first(),
            )
        }

    @Test fun factsFirstDateAndEveryBatchShareOneTransactionBeforePublication() =
        runTest {
            var inTransaction = false
            var transactions = 0
            val runner =
                object : DatabaseTransactionRunner {
                    override suspend fun <T> run(block: suspend () -> T): T {
                        transactions++
                        inTransaction = true
                        return try {
                            block()
                        } finally {
                            inTransaction = false
                        }
                    }
                }
            val repository = repository(StandardTestDispatcher(testScheduler), runner)
            every { dao.observeSessionChanges() } returns flowOf(true)
            coEvery { dao.hasAnySessions() } answers {
                assertTrue(inTransaction)
                true
            }
            coEvery { dao.getSessionProjectActivity(null) } answers {
                assertTrue(inTransaction)
                emptyList()
            }
            coEvery { dao.getFirstSessionStart(null) } answers {
                assertTrue(inTransaction)
                1000L
            }
            coEvery { dao.getInsightFirstDateBatch(any(), any(), any()) } answers {
                assertTrue(inTransaction)
                emptyList()
            }
            coEvery { dao.getInsightSessionBatch(any()) } answers {
                assertTrue(inTransaction)
                (1L..257L).filter { it > firstArg<Long>() }.take(256).map(::session)
            }

            repository
                .observeSessionsForInsights(null, null, java.time.ZoneOffset.UTC, { intArrayOf(0) }) { total, batch ->
                    assertTrue(inTransaction)
                    total[0] += batch.size
                }.collect { total ->
                    assertFalse(inTransaction)
                    assertEquals(257, total[0])
                }

            assertFalse(inTransaction)
            assertEquals(1, transactions)
            coVerify(exactly = 1) { dao.getInsightSessionBatch(256) }
            coVerify(exactly = 1) { dao.getInsightSessionBatch(257) }
        }

    @Test fun sameExistenceInvalidationCancelsAnIncompleteSnapshot() =
        runTest {
            val changes = MutableSharedFlow<Boolean>(replay = 1)
            every { dao.observeSessionChanges() } returns changes
            stubEmptySessionMetadata()
            coEvery { dao.getInsightSessionBatch(Long.MIN_VALUE) } returns listOf(session(1))
            coEvery { dao.getInsightSessionBatch(1) } returns emptyList()
            var cancelled = false
            var first = true
            var inTransaction = false
            var releasedTransactions = 0
            val runner =
                object : DatabaseTransactionRunner {
                    override suspend fun <T> run(block: suspend () -> T): T {
                        assertFalse(inTransaction)
                        inTransaction = true
                        return try {
                            block()
                        } finally {
                            inTransaction = false
                            releasedTransactions++
                        }
                    }
                }
            val values = mutableListOf<Int>()
            val repository = repository(StandardTestDispatcher(testScheduler), runner)
            backgroundScope.launch {
                repository
                    .observeSessionsForInsights(
                        null,
                        null,
                        java.time.ZoneOffset.UTC,
                        { intArrayOf(0) },
                    ) { total, batch ->
                        assertTrue(inTransaction)
                        if (first) {
                            first = false
                            try {
                                awaitCancellation()
                            } finally {
                                cancelled = true
                            }
                        }
                        total[0] += batch.size
                    }.collect { values += it[0] }
            }
            changes.emit(true)
            runCurrent()
            assertTrue(inTransaction)
            assertEquals(0, releasedTransactions)
            changes.emit(true)
            runCurrent()
            assertTrue(cancelled)
            assertFalse(inTransaction)
            assertEquals(2, releasedTransactions)
            assertEquals(listOf(1), values)
        }

    @Test fun consecutiveChangesRebuildCompleteSnapshotsAcrossBatchBoundary() =
        runTest {
            val changes = MutableSharedFlow<Boolean>(replay = 1)
            val rows = (1L..257L).map(::session).toMutableList()
            every { dao.observeSessionChanges() } returns changes
            coEvery { dao.getFirstSessionStart(any()) } returns null
            coEvery { dao.hasAnySessions() } returns true
            coEvery { dao.getSessionProjectActivity(null) } returns emptyList()
            coEvery { dao.getInsightSessionBatch(any()) } answers {
                rows.filter { it.id > firstArg<Long>() }.take(256)
            }
            val counts = mutableListOf<Int>()
            backgroundScope.launch {
                repository(StandardTestDispatcher(testScheduler))
                    .observeSessionsForInsights(
                        null,
                        null,
                        java.time.ZoneOffset.UTC,
                        { intArrayOf(0) },
                    ) { total, batch ->
                        total[0] += batch.size
                    }.collect { counts += it[0] }
            }

            changes.emit(true)
            runCurrent()
            rows += session(258)
            changes.emit(true)
            runCurrent()

            assertEquals(listOf(257, 258), counts)
            coVerify(exactly = 2) { dao.getInsightSessionBatch(Long.MIN_VALUE) }
        }

    private fun stubEmptySessionMetadata(projectId: Long? = null) {
        coEvery { dao.getFirstSessionStart(any()) } returns null
        coEvery { dao.getInsightFirstDateBatch(any(), any(), any()) } returns emptyList()
        coEvery { dao.hasAnySessions() } returns true
        coEvery { dao.getSessionProjectActivity(projectId) } returns emptyList()
    }

    private fun repository(
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
        runner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ) = CounterRepository(
        dao = mockk(),
        projectCounterDao = mockk(),
        sessionDao = dao,
        photoStorage = mockk(),
        patternDocumentStorage = mockk(),
        context = mockk(),
        yarnCardRepository = mockk(),
        savedPatternRepository = mockk(),
        projectDocumentRepository = mockk(),
        projectFolderDao = mockk(),
        transactionRunner = runner,
        ioDispatcher = dispatcher,
    )

    private fun session(id: Long) = SessionEntity(id, 7, 1000, 2000, 0, 1, 1, 1, 1)
}
