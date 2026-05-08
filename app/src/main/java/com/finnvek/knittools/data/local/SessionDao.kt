package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun getSessionsForProject(projectId: Long): Flow<List<SessionEntity>>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE projectId = :projectId AND startedAt < :before")
    suspend fun deleteSessionsBefore(
        projectId: Long,
        before: Long,
    )

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM sessions WHERE projectId = :projectId")
    suspend fun getTotalMinutes(projectId: Long): Int

    // Insights-queryt

    @Query(
        "SELECT COALESCE(SUM(durationMinutes), 0) FROM sessions WHERE (:projectId IS NULL OR projectId = :projectId)",
    )
    fun getTotalDurationMinutes(projectId: Long?): Flow<Int>

    @Query(
        """
        SELECT
            COALESCE(SUM(durationMinutes), 0) AS totalMinutes,
            COALESCE(SUM(CASE WHEN endRow > startRow THEN endRow - startRow ELSE 0 END), 0) AS totalRows,
            COUNT(*) AS sessionCount
        FROM sessions
        WHERE (:projectId IS NULL OR projectId = :projectId)
            AND (:start IS NULL OR startedAt >= :start)
        """,
    )
    fun getInsightsTotals(
        projectId: Long?,
        start: Long?,
    ): Flow<SessionInsightsTotals>

    @Query(
        "SELECT * FROM sessions WHERE startedAt >= :start AND startedAt <= :end AND (:projectId IS NULL OR projectId = :projectId) ORDER BY startedAt",
    )
    fun getSessionsInRange(
        start: Long,
        end: Long,
        projectId: Long?,
    ): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE (:projectId IS NULL OR projectId = :projectId) ORDER BY startedAt")
    fun getAllSessions(projectId: Long?): Flow<List<SessionEntity>>

    @Query(
        "SELECT * FROM sessions WHERE (:projectId IS NULL OR projectId = :projectId) AND (:start IS NULL OR startedAt >= :start) ORDER BY startedAt",
    )
    fun getSessionsForInsights(
        projectId: Long?,
        start: Long?,
    ): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT
            sessions.projectId AS projectId,
            COALESCE(counter_projects.name, 'Project ' || sessions.projectId) AS projectName,
            COALESCE(SUM(sessions.durationMinutes), 0) AS totalMinutes,
            COALESCE(SUM(CASE WHEN sessions.endRow > sessions.startRow THEN sessions.endRow - sessions.startRow ELSE 0 END), 0) AS totalRows,
            MAX(sessions.startedAt) AS lastSessionAt
        FROM sessions
        LEFT JOIN counter_projects ON counter_projects.id = sessions.projectId
        WHERE (:projectId IS NULL OR sessions.projectId = :projectId)
            AND (:start IS NULL OR sessions.startedAt >= :start)
        GROUP BY sessions.projectId, counter_projects.name
        ORDER BY totalMinutes DESC
        """,
    )
    fun getProjectTimeSummaries(
        projectId: Long?,
        start: Long?,
    ): Flow<List<SessionProjectSummary>>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY endedAt DESC LIMIT 1")
    suspend fun getLatestSession(projectId: Long): SessionEntity?

    @Query("SELECT COUNT(*) FROM counter_projects WHERE isCompleted = 1")
    fun getCompletedProjectCount(): Flow<Int>
}
