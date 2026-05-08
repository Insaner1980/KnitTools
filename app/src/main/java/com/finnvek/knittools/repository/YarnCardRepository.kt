package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.YarnCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YarnCardRepository
    @Inject
    constructor(
        private val dao: YarnCardDao,
    ) {
        fun getAllCards(): Flow<List<YarnCard>> = dao.getAllCards().map { cards -> cards.map { it.toDomain() } }

        suspend fun getCard(id: Long): YarnCard? = dao.getCard(id)?.toDomain()

        suspend fun getCards(ids: List<Long>): List<YarnCard> = dao.getCards(ids).map { it.toDomain() }

        suspend fun saveCard(card: YarnCard): Long = dao.upsert(card.toEntity())

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
