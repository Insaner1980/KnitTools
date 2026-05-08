package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPatternDao {
    @Query("SELECT * FROM saved_patterns ORDER BY savedAt DESC")
    fun getAll(): Flow<List<SavedPatternEntity>>

    @Query("SELECT * FROM saved_patterns WHERE id = :id")
    suspend fun getById(id: Long): SavedPatternEntity?

    @Query("SELECT * FROM saved_patterns WHERE ravelryId = :ravelryId")
    suspend fun getByRavelryId(ravelryId: Int): SavedPatternEntity?

    @Query("SELECT * FROM saved_patterns WHERE patternUrl = :patternUrl LIMIT 1")
    suspend fun getByPatternUrl(patternUrl: String): SavedPatternEntity?

    @Insert
    suspend fun insert(pattern: SavedPatternEntity): Long

    @Query("DELETE FROM saved_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_patterns WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM saved_patterns")
    fun getCount(): Flow<Int>
}
