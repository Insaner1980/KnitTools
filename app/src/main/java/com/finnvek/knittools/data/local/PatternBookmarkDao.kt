package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternBookmarkDao {
    @Query(
        """
        SELECT * FROM pattern_bookmarks
        WHERE projectId = :projectId AND documentKey = :documentKey
        ORDER BY pageIndex ASC, yFraction ASC, createdAt ASC, id ASC
        """,
    )
    fun observeForProjectDocument(
        projectId: Long,
        documentKey: String,
    ): Flow<List<PatternBookmarkEntity>>

    @Query("SELECT * FROM pattern_bookmarks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PatternBookmarkEntity?

    @Insert
    suspend fun insert(bookmark: PatternBookmarkEntity): Long

    @Query("UPDATE pattern_bookmarks SET name = :name WHERE id = :id")
    suspend fun updateName(
        id: Long,
        name: String,
    ): Int

    @Query("DELETE FROM pattern_bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
