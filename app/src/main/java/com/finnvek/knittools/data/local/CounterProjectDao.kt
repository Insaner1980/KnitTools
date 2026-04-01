package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterProjectDao {
    @Query("SELECT * FROM counter_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<CounterProjectEntity>>

    @Query("SELECT * FROM counter_projects WHERE id = :id")
    suspend fun getProject(id: Long): CounterProjectEntity?

    @Insert
    suspend fun insert(project: CounterProjectEntity): Long

    @Update
    suspend fun update(project: CounterProjectEntity)

    @Query("DELETE FROM counter_projects WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM counter_projects")
    suspend fun getProjectCount(): Int

    @Insert
    suspend fun insertHistory(entry: CounterHistoryEntity)

    @Query("SELECT * FROM counter_history WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getHistory(projectId: Long): Flow<List<CounterHistoryEntity>>

    @Query("DELETE FROM counter_history WHERE projectId = :projectId AND timestamp < :before")
    suspend fun deleteHistoryBefore(
        projectId: Long,
        before: Long,
    )
}
