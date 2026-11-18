package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RowReminderDao {
    @Query("SELECT * FROM row_reminders WHERE projectId = :projectId ORDER BY targetRow ASC")
    fun getRemindersForProject(projectId: Long): Flow<List<RowReminderEntity>>

    @Insert
    suspend fun insert(reminder: RowReminderEntity): Long

    @Query("SELECT * FROM row_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminder(id: Long): RowReminderEntity?

    @Update
    suspend fun update(reminder: RowReminderEntity): Int

    @Query("DELETE FROM row_reminders WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM row_reminders WHERE projectId = :projectId")
    suspend fun deleteAllForProject(projectId: Long)
}
