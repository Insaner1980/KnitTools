package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectCounterDao {
    @Query("SELECT * FROM project_counters WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun getCountersForProject(projectId: Long): Flow<List<ProjectCounterEntity>>

    @Insert
    suspend fun insert(counter: ProjectCounterEntity): Long

    @Update
    suspend fun update(counter: ProjectCounterEntity)

    @Query("DELETE FROM project_counters WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE project_counters SET count = :count WHERE id = :id")
    suspend fun updateCount(
        id: Long,
        count: Int,
    )

    @Query("UPDATE project_counters SET name = :name WHERE id = :id")
    suspend fun updateName(
        id: Long,
        name: String,
    )

    @Query("UPDATE project_counters SET count = :count, currentRepeat = :currentRepeat WHERE id = :id")
    suspend fun updateRepeatSectionState(
        id: Long,
        count: Int,
        currentRepeat: Int?,
    )
}
