package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface YarnCardDao {
    @Query("SELECT * FROM yarn_cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<YarnCardEntity>>

    @Query("SELECT * FROM yarn_cards WHERE id = :id")
    suspend fun getCard(id: Long): YarnCardEntity?

    @Query("SELECT * FROM yarn_cards WHERE id IN (:ids)")
    suspend fun getCards(ids: List<Long>): List<YarnCardEntity>

    @Upsert
    suspend fun upsert(card: YarnCardEntity): Long

    @Query("SELECT COUNT(*) FROM yarn_cards")
    fun getCardCount(): Flow<Int>

    @Query("UPDATE yarn_cards SET quantityInStash = :quantity WHERE id = :id")
    suspend fun updateQuantity(
        id: Long,
        quantity: Int,
    )

    @Query("UPDATE yarn_cards SET status = :status WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: String,
    )

    @Query("UPDATE yarn_cards SET linkedProjectId = :projectId WHERE id = :id")
    suspend fun updateLinkedProjectId(
        id: Long,
        projectId: Long?,
    )

    @Query("DELETE FROM yarn_cards WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM yarn_cards WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
