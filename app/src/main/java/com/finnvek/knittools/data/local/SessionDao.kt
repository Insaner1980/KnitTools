package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startedAt DESC, id DESC")
    fun getSessionsForProject(projectId: Long): Flow<List<SessionEntity>>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sessions WHERE projectId = :projectId AND startedAt < :before")
    suspend fun deleteSessionsBefore(
        projectId: Long,
        before: Long,
    )

    @Query(
        """
        SELECT CAST((COALESCE(SUM(durationSeconds), 0) + 59) / 60 AS INTEGER)
        FROM sessions
        WHERE projectId = :projectId
        """,
    )
    suspend fun getTotalMinutes(projectId: Long): Int

    // Insights-queryt

    @Query(
        """
        SELECT *
        FROM sessions
        WHERE endedAt >= :start
            AND startedAt <= :end
            AND (:projectId IS NULL OR projectId = :projectId)
        ORDER BY startedAt, id
        """,
    )
    fun getSessionsInRange(
        start: Long,
        end: Long,
        projectId: Long?,
    ): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE (:projectId IS NULL OR projectId = :projectId) ORDER BY startedAt, id")
    fun getAllSessions(projectId: Long?): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT *
        FROM sessions
        WHERE (:projectId IS NULL OR projectId = :projectId)
            AND (:start IS NULL OR endedAt >= :start)
        ORDER BY startedAt, id
        """,
    )
    fun getSessionsForInsights(
        projectId: Long?,
        start: Long?,
    ): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY endedAt DESC LIMIT 1")
    suspend fun getLatestSession(projectId: Long): SessionEntity?

    @Query("SELECT COUNT(*) FROM counter_projects WHERE isCompleted = 1")
    fun getCompletedProjectCount(): Flow<Int>
}
