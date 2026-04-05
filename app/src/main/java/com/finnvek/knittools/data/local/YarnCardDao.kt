package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface YarnCardDao {
    @Query("SELECT * FROM yarn_cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<YarnCardEntity>>

    @Query("SELECT * FROM yarn_cards WHERE id = :id")
    suspend fun getCard(id: Long): YarnCardEntity?

    @Query("SELECT * FROM yarn_cards WHERE id IN (:ids)")
    suspend fun getCards(ids: List<Long>): List<YarnCardEntity>

    @Insert
    suspend fun insert(card: YarnCardEntity): Long

    @Query("DELETE FROM yarn_cards WHERE id = :id")
    suspend fun delete(id: Long)
}
