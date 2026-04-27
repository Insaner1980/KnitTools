package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.SessionInsightsTotals
import com.finnvek.knittools.data.local.SessionProjectSummary
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterRepositoryTargetRowsTest {
    @Test
    fun `undoLastChange palauttaa count previousValue-arvoon ja poistaa history-entryn`() = runTest {
        val fake = FakeCounterProjectDao(
            latestHistory = CounterHistoryEntity(
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
    fun `undoLastChange on no-op jos historiaa ei ole`() = runTest {
        val fake = FakeCounterProjectDao(latestHistory = null)
        val repository = buildRepository(fake)

        repository.undoLastChange(projectId = 1)

        assertNull(fake.lastUpdateCount)
        assertNull(fake.lastDeletedHistoryId)
    }

    @Test
    fun `setTargetRows delegoi daoon ja hyvaksyy nullin`() = runTest {
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
        context = mockk<Context>(relaxed = true),
    )

private class FakeCounterProjectDao(
    private val latestHistory: CounterHistoryEntity?,
) : CounterProjectDao {

    var lastUpdateCount: Int? = null
    var lastDeletedHistoryId: Long? = null
    var lastTargetRows: Int? = null
    var targetRowsWasSetToNull: Boolean = false

    // --- uudet metodit joita testataan ---

    override suspend fun getLatestHistory(projectId: Long): CounterHistoryEntity? = latestHistory

    override suspend fun deleteHistoryById(id: Long) {
        lastDeletedHistoryId = id
    }

    override suspend fun updateCount(id: Long, count: Int, updatedAt: Long) {
        lastUpdateCount = count
    }

    override suspend fun updateTargetRows(id: Long, targetRows: Int?, updatedAt: Long) {
        lastTargetRows = targetRows
        if (targetRows == null) targetRowsWasSetToNull = true
    }

    // --- muut DAO-metodit (ei käytetä näissä testeissä) ---

    override fun getAllProjects(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())
    override suspend fun getProject(id: Long): CounterProjectEntity? = null
    override fun observeProject(id: Long): Flow<CounterProjectEntity?> = flowOf(null)
    override suspend fun insert(project: CounterProjectEntity): Long = 0L
    override suspend fun update(project: CounterProjectEntity) = Unit
    override suspend fun adjustCount(id: Long, delta: Int, updatedAt: Long) = Unit
    override suspend fun adjustCountAndStepSize(id: Long, delta: Int, stepSize: Int, updatedAt: Long) = Unit
    override suspend fun updateCounterState(id: Long, count: Int, stepSize: Int, updatedAt: Long) = Unit
    override suspend fun updateName(id: Long, name: String, updatedAt: Long) = Unit
    override suspend fun updateNotes(id: Long, notes: String, updatedAt: Long) = Unit
    override suspend fun updateSecondaryCount(id: Long, secondaryCount: Int, updatedAt: Long) = Unit
    override suspend fun updateSectionName(id: Long, sectionName: String?, updatedAt: Long) = Unit
    override suspend fun updateStitchCount(id: Long, stitchCount: Int?, updatedAt: Long) = Unit
    override suspend fun updateCurrentStitch(id: Long, stitch: Int, updatedAt: Long) = Unit
    override suspend fun updateStitchTrackingEnabled(id: Long, enabled: Boolean, updatedAt: Long) = Unit
    override suspend fun updatePattern(
        id: Long,
        patternUri: String?,
        patternName: String?,
        currentPatternPage: Int,
        patternRowMapping: String?,
        updatedAt: Long,
    ) = Unit
    override suspend fun updateCurrentPatternPage(id: Long, page: Int, updatedAt: Long) = Unit
    override suspend fun updatePatternRowMapping(id: Long, mapping: String?, updatedAt: Long) = Unit
    override suspend fun updateStepSize(id: Long, stepSize: Int, updatedAt: Long) = Unit
    override suspend fun updateYarnCardIds(id: Long, yarnCardIds: String, updatedAt: Long) = Unit
    override suspend fun archiveProject(id: Long, totalRows: Int, completedAt: Long, updatedAt: Long) = Unit
    override suspend fun reactivateProject(id: Long, updatedAt: Long) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun getProjectCount(): Int = 0
    override suspend fun getFirstProject(): CounterProjectEntity? = null
    override suspend fun insertHistory(entry: CounterHistoryEntity) = Unit
    override suspend fun deleteHistoryBefore(projectId: Long, before: Long) = Unit
    override fun getActiveProjects(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())
    override fun getActiveProjectsByName(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())
    override fun getActiveProjectsByCreated(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())
    override fun getCompletedProjects(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())
    override fun getCompletedProjectsByName(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())
    override fun getCompletedProjectsByCreated(): Flow<List<CounterProjectEntity>> = flowOf(emptyList())
    override suspend fun getActiveProjectCount(): Int = 0
}

private class StubSessionDao : SessionDao {
    override fun getSessionsForProject(projectId: Long): Flow<List<SessionEntity>> = flowOf(emptyList())
    override suspend fun insert(session: SessionEntity): Long = 0L
    override suspend fun deleteSessionsBefore(projectId: Long, before: Long) = Unit
    override suspend fun getTotalMinutes(projectId: Long): Int = 0
    override fun getTotalDurationMinutes(projectId: Long?): Flow<Int> = flowOf(0)
    override fun getInsightsTotals(projectId: Long?, start: Long?): Flow<SessionInsightsTotals> =
        flowOf(SessionInsightsTotals(0, 0, 0))

    override fun getSessionsInRange(start: Long, end: Long, projectId: Long?): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun getAllSessions(projectId: Long?): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun getSessionsForInsights(projectId: Long?, start: Long?): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun getProjectTimeSummaries(projectId: Long?, start: Long?): Flow<List<SessionProjectSummary>> = flowOf(emptyList())
    override suspend fun getLatestSession(projectId: Long): SessionEntity? = null
    override fun getCompletedProjectCount(): Flow<Int> = flowOf(0)
}
