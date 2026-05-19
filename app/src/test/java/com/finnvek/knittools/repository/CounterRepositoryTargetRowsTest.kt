package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterRepositoryTargetRowsTest {
    @Test
    fun `undoLastChange palauttaa count previousValue-arvoon ja poistaa history-entryn`() =
        runTest {
            val fake =
                FakeCounterProjectDao(
                    latestHistory =
                        CounterHistoryEntity(
                            id = 5,
                            projectId = 1,
                            action = "increment",
                            previousValue = 41,
                            newValue = 42,
                        ),
                )
            val repository = buildRepository(fake)

            repository.undoLastChange(projectId = 1)

            assertEquals(41, fake.lastUpdateCount)
            assertEquals(5L, fake.lastDeletedHistoryId)
        }

    @Test
    fun `undoLastChange on no-op jos historiaa ei ole`() =
        runTest {
            val fake = FakeCounterProjectDao(latestHistory = null)
            val repository = buildRepository(fake)

            repository.undoLastChange(projectId = 1)

            assertNull(fake.lastUpdateCount)
            assertNull(fake.lastDeletedHistoryId)
        }

    @Test
    fun `setTargetRows delegoi daoon ja hyvaksyy nullin`() =
        runTest {
            val fake = FakeCounterProjectDao(latestHistory = null)
            val repository = buildRepository(fake)

            repository.setTargetRows(projectId = 1, targetRows = 150)
            assertEquals(150, fake.lastTargetRows)

            repository.setTargetRows(projectId = 1, targetRows = null)
            assertEquals(null, fake.lastTargetRows)
            assertEquals(true, fake.targetRowsWasSetToNull)
        }
}

// ---------------------------------------------------------------------------
// Apurakenteet
// ---------------------------------------------------------------------------

private fun buildRepository(dao: FakeCounterProjectDao): CounterRepository =
    CounterRepository(
        dao = dao,
        sessionDao = StubSessionDao(),
        photoStorage = mockk(relaxed = true),
        patternDocumentStorage = mockk(relaxed = true),
        context = mockk<Context>(relaxed = true),
        yarnCardRepository = mockk(relaxed = true),
        savedPatternRepository = mockk(relaxed = true),
        patternAnnotationRepository = mockk(relaxed = true),
        transactionRunner = ImmediateDatabaseTransactionRunner,
        ioDispatcher = Dispatchers.Unconfined,
    )

private class FakeCounterProjectDao(
    private val latestHistory: CounterHistoryEntity?,
) : StubCounterProjectDao() {
    var lastUpdateCount: Int? = null
    var lastDeletedHistoryId: Long? = null
    var lastTargetRows: Int? = null
    var targetRowsWasSetToNull: Boolean = false

    override suspend fun getLatestHistory(projectId: Long): CounterHistoryEntity? = latestHistory

    override suspend fun deleteHistoryById(id: Long) {
        lastDeletedHistoryId = id
    }

    override suspend fun updateCount(
        id: Long,
        count: Int,
        updatedAt: Long,
    ) {
        lastUpdateCount = count
    }

    override suspend fun updateTargetRows(
        id: Long,
        targetRows: Int?,
        updatedAt: Long,
    ) {
        lastTargetRows = targetRows
        if (targetRows == null) targetRowsWasSetToNull = true
    }
}

private class StubSessionDao : SessionDao {
    override fun getSessionsForProject(projectId: Long): Flow<List<SessionEntity>> = flowOf(emptyList())

    override suspend fun insert(session: SessionEntity): Long = 0L

    override suspend fun deleteById(id: Long) = Unit

    override suspend fun deleteSessionsBefore(
        projectId: Long,
        before: Long,
    ) = Unit

    override suspend fun getTotalMinutes(projectId: Long): Int = 0

    override fun getAllSessions(projectId: Long?): Flow<List<SessionEntity>> = flowOf(emptyList())

    override fun getAllSessionsForInsights(): Flow<List<SessionEntity>> = flowOf(emptyList())

    override fun getAllSessionsForInsightsSince(start: Long): Flow<List<SessionEntity>> = flowOf(emptyList())

    override fun getProjectSessionsForInsights(projectId: Long): Flow<List<SessionEntity>> = flowOf(emptyList())

    override fun getProjectSessionsForInsightsSince(
        projectId: Long,
        start: Long,
    ): Flow<List<SessionEntity>> = flowOf(emptyList())

    override suspend fun getLatestSession(projectId: Long): SessionEntity? = null

    override fun getCompletedProjectCount(): Flow<Int> = flowOf(0)
}
