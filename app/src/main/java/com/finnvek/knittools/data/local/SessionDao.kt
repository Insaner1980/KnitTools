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
}
