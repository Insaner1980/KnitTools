package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YarnCardRepository
    @Inject
    constructor(
        private val dao: YarnCardDao,
    ) {
        fun getAllCards(): Flow<List<YarnCardEntity>> = dao.getAllCards()

        suspend fun getCard(id: Long): YarnCardEntity? = dao.getCard(id)

        suspend fun getCards(ids: List<Long>): List<YarnCardEntity> = dao.getCards(ids)

        suspend fun saveCard(card: YarnCardEntity): Long = dao.upsert(card)

        fun getCardCount() = dao.getCardCount()

        suspend fun updateQuantity(
            id: Long,
            quantity: Int,
        ) = dao.updateQuantity(id, quantity)

        suspend fun updateStatus(
            id: Long,
            status: String,
        ) = dao.updateStatus(id, status)

        suspend fun updateLinkedProjectId(
            id: Long,
            projectId: Long?,
        ) = dao.updateLinkedProjectId(id, projectId)

        suspend fun deleteCard(id: Long) = dao.delete(id)

        suspend fun deleteCards(ids: List<Long>) = dao.deleteByIds(ids)
    }
