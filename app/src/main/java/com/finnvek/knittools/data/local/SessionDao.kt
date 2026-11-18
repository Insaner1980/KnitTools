package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

internal const val MAX_COMPLETED_SESSIONS = 100_000L

@Dao
@Suppress("TooManyFunctions") // Valmiit ja aktiivinen istunto kuuluvat samaan Room-istuntorajapintaan.
interface SessionDao {
    @Query("SELECT * FROM active_sessions WHERE singletonId = 1")
    fun observeActiveSession(): Flow<ActiveSessionEntity?>

    @Query("SELECT * FROM active_sessions WHERE singletonId = 1")
    suspend fun getActiveSession(): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActiveSession(session: ActiveSessionEntity)

    @Update
    suspend fun updateActiveSession(session: ActiveSessionEntity): Int

    @Query("DELETE FROM active_sessions WHERE singletonId = 1 AND sessionToken = :sessionToken")
    suspend fun deleteActiveSession(sessionToken: String): Int

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startedAt DESC, id DESC")
    fun getSessionsForProject(projectId: Long): Flow<List<SessionEntity>>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun countCompletedSessions(): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sessions WHERE projectId = :projectId AND startedAt < :before")
    suspend fun deleteSessionsBefore(
        projectId: Long,
        before: Long,
    )

    @Query(
        """
        SELECT CAST(MIN(2147483647, (TOTAL(durationSeconds) + 59) / 60) AS INTEGER)
        FROM sessions
        WHERE projectId = :projectId
        """,
    )
    suspend fun getTotalMinutes(projectId: Long): Int

    // Pelkkiä rivejä sisältävä istunto tallennetaan nollakestolla, joten olemassaolo ei näy minuuttisummasta.
    @Query("SELECT EXISTS(SELECT 1 FROM sessions WHERE projectId = :projectId)")
    suspend fun hasSessions(projectId: Long): Boolean

    @Query("SELECT * FROM sessions WHERE (:projectId IS NULL OR projectId = :projectId) ORDER BY startedAt, id")
    fun getAllSessions(projectId: Long?): Flow<List<SessionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM sessions)")
    fun observeSessionChanges(): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM sessions)")
    suspend fun hasAnySessions(): Boolean

    @Query(
        "SELECT projectId, MAX(startedAt) AS lastSessionAt FROM sessions " +
            "GROUP BY projectId ORDER BY MIN(id)",
    )
    suspend fun getAllSessionProjectActivity(): List<SessionProjectActivity>

    @Query(
        "SELECT projectId, MAX(startedAt) AS lastSessionAt FROM sessions WHERE projectId = :projectId GROUP BY projectId",
    )
    suspend fun getProjectSessionActivity(projectId: Long): List<SessionProjectActivity>

    suspend fun getSessionProjectActivity(projectId: Long?): List<SessionProjectActivity> =
        if (projectId == null) getAllSessionProjectActivity() else getProjectSessionActivity(projectId)

    @Query("SELECT MIN(startedAt) FROM sessions")
    suspend fun getFirstSessionStart(): Long?

    @Query("SELECT MIN(startedAt) FROM sessions WHERE projectId = :projectId")
    suspend fun getProjectFirstSessionStart(projectId: Long): Long?

    suspend fun getFirstSessionStart(projectId: Long?): Long? =
        if (projectId == null) getFirstSessionStart() else getProjectFirstSessionStart(projectId)

    @Query(
        "SELECT id, startedAt, zoneId FROM sessions WHERE id > :afterId AND startedAt <= :latestStart ORDER BY id LIMIT 256",
    )
    suspend fun getInsightFirstDateBatch(
        afterId: Long,
        latestStart: Long,
    ): List<SessionStart>

    @Query(
        "SELECT id, startedAt, zoneId FROM sessions WHERE projectId = :projectId " +
            "AND id > :afterId AND startedAt <= :latestStart ORDER BY id LIMIT 256",
    )
    suspend fun getProjectInsightFirstDateBatch(
        projectId: Long,
        afterId: Long,
        latestStart: Long,
    ): List<SessionStart>

    suspend fun getInsightFirstDateBatch(
        projectId: Long?,
        afterId: Long,
        latestStart: Long,
    ): List<SessionStart> =
        if (projectId == null) {
            getInsightFirstDateBatch(afterId, latestStart)
        } else {
            getProjectInsightFirstDateBatch(projectId, afterId, latestStart)
        }

    @Query("SELECT * FROM sessions WHERE id > :afterId ORDER BY id LIMIT 256")
    suspend fun getInsightSessionBatch(afterId: Long): List<SessionEntity>

    @Query(
        "SELECT * FROM sessions WHERE id > :afterId " +
            "AND (endedAt >= :start OR endedAt < :start AND " + SESSION_EFFECTIVE_END + " >= :start) " +
            "ORDER BY id LIMIT 256",
    )
    suspend fun getInsightSessionBatchSince(
        afterId: Long,
        start: Long,
    ): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId AND id > :afterId ORDER BY id LIMIT 256")
    suspend fun getProjectInsightSessionBatch(
        projectId: Long,
        afterId: Long,
    ): List<SessionEntity>

    @Query(
        "SELECT * FROM sessions WHERE projectId = :projectId AND id > :afterId " +
            "AND (endedAt >= :start OR " + SESSION_EFFECTIVE_END + " >= :start) ORDER BY id LIMIT 256",
    )
    suspend fun getProjectInsightSessionBatchSince(
        projectId: Long,
        afterId: Long,
        start: Long,
    ): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY endedAt DESC LIMIT 1")
    suspend fun getLatestSession(projectId: Long): SessionEntity?

    @Query("SELECT COUNT(*) FROM counter_projects WHERE isCompleted = 1")
    fun getCompletedProjectCount(): Flow<Int>
}

// Säilyttää SessionMetricsin vanhan kesto- ja rivivaralaskennan ennen aikarajausta.
private const val SESSION_EFFECTIVE_END =
    "startedAt + (CASE WHEN durationSeconds > 0 THEN durationSeconds " +
        "WHEN durationMinutes > 0 THEN durationMinutes * 60 ELSE 1 END) * 1000"

data class SessionProjectActivity(
    val projectId: Long,
    val lastSessionAt: Long,
)

data class SessionStart(
    val id: Long,
    val startedAt: Long,
    val zoneId: String?,
)
