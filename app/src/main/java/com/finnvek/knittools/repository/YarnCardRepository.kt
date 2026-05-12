package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.domain.model.YarnCard
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YarnCardRepository
    @Inject
    constructor(
        private val dao: YarnCardDao,
        private val counterProjectDao: CounterProjectDao,
        @param:ApplicationContext private val context: Context,
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

        suspend fun clearLinkedProject(projectId: Long) = dao.clearLinkedProject(projectId)

        suspend fun deleteCard(id: Long) = deleteCards(listOf(id))

        suspend fun deleteCards(ids: List<Long>) {
            if (ids.isEmpty()) return
            val cards = dao.getCards(ids)
            removeCardIdsFromProjects(ids.toSet())
            dao.deleteByIds(ids)
            cards.forEach { card -> AppFileStorage.deleteIfAppOwned(context, card.photoUri) }
        }

        private suspend fun removeCardIdsFromProjects(cardIds: Set<Long>) {
            val updatedAt = System.currentTimeMillis()
            counterProjectDao.getAllProjectsOnce().forEach { project ->
                val currentIds = project.yarnCardIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                val nextIds = currentIds.filterNot { it in cardIds }
                if (nextIds.size != currentIds.size) {
                    counterProjectDao.updateYarnCardIds(
                        id = project.id,
                        yarnCardIds = nextIds.joinToString(","),
                        updatedAt = updatedAt,
                    )
                }
            }
        }
    }
