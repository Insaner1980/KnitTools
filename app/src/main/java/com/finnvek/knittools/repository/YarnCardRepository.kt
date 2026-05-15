package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YarnCardRepository
    @Inject
    constructor(
        private val dao: YarnCardDao,
        private val counterProjectDao: CounterProjectDao,
        @param:ApplicationContext private val context: Context,
        private val transactionRunner: DatabaseTransactionRunner,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun getAllCards(): Flow<List<YarnCard>> = dao.getAllCards().map { cards -> cards.map { it.toDomain() } }

        fun observeCard(id: Long): Flow<YarnCard?> = dao.observeCard(id).map { it?.toDomain() }

        suspend fun getCard(id: Long): YarnCard? = dao.getCard(id)?.toDomain()

        suspend fun getCards(ids: List<Long>): List<YarnCard> = dao.getCards(ids).map { it.toDomain() }

        suspend fun saveCard(card: YarnCard): Long =
            transactionRunner.run {
                val existingCard = card.id.takeIf { it != 0L }?.let { dao.getCard(it) }
                val projects = counterProjectDao.getAllProjectsOnce()
                val linkedProjectId =
                    card.linkedProjectId?.takeIf { projectId ->
                        projects.any { it.id == projectId }
                    }
                val normalizedCard =
                    card.copy(
                        status = YarnCardStatus.normalize(card.status),
                        linkedProjectId = linkedProjectId,
                    )
                val upsertedId = dao.upsert(normalizedCard.toEntity())
                val savedId = normalizedCard.id.takeIf { it != 0L } ?: upsertedId
                val linkChanged = existingCard?.linkedProjectId != linkedProjectId
                if (linkedProjectId != null || linkChanged) {
                    updateProjectYarnLinks(
                        projects = projects,
                        cardId = savedId,
                        projectId = linkedProjectId,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                savedId
            }

        fun getCardCount() = dao.getCardCount()

        suspend fun updateQuantity(
            id: Long,
            quantity: Int,
        ): Boolean = dao.updateQuantity(id, quantity) > 0

        suspend fun updateStatus(
            id: Long,
            status: String,
        ): Boolean = dao.updateStatus(id, status) > 0

        suspend fun updateLinkedProjectId(
            id: Long,
            projectId: Long?,
        ): Boolean =
            transactionRunner.run {
                if (dao.getCard(id) == null) return@run false
                val projects = counterProjectDao.getAllProjectsOnce()
                if (projectId != null && projects.none { it.id == projectId }) return@run false

                updateProjectYarnLinks(
                    projects = projects,
                    cardId = id,
                    projectId = projectId,
                    updatedAt = System.currentTimeMillis(),
                )
                dao.updateLinkedProjectId(id, projectId) > 0
            }

        suspend fun clearLinkedProject(projectId: Long) = dao.clearLinkedProject(projectId)

        suspend fun deleteCard(id: Long) = deleteCards(listOf(id))

        suspend fun deleteCards(ids: List<Long>) {
            if (ids.isEmpty()) return
            val cards =
                transactionRunner.run {
                    val cards = dao.getCards(ids)
                    removeCardIdsFromProjects(ids.toSet())
                    dao.deleteByIds(ids)
                    cards
                }
            withContext(ioDispatcher) {
                cards.forEach { card -> AppFileStorage.deleteIfAppOwned(context, card.photoUri) }
            }
        }

        private suspend fun removeCardIdsFromProjects(cardIds: Set<Long>) {
            val updatedAt = System.currentTimeMillis()
            counterProjectDao.getAllProjectsOnce().forEach { project ->
                val currentIds = project.yarnCardIds.toYarnCardIds()
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

        private suspend fun updateProjectYarnLinks(
            projects: List<CounterProjectEntity>,
            cardId: Long,
            projectId: Long?,
            updatedAt: Long,
        ) {
            projects.forEach { project ->
                val currentIds = project.yarnCardIds.toYarnCardIds()
                val nextIds =
                    if (project.id == projectId) {
                        if (cardId in currentIds) currentIds else currentIds + cardId
                    } else {
                        currentIds.filterNot { it == cardId }
                    }
                val shouldUpdate =
                    if (project.id == projectId) {
                        cardId !in currentIds
                    } else {
                        cardId in currentIds
                    }
                if (shouldUpdate) {
                    counterProjectDao.updateYarnCardIds(
                        id = project.id,
                        yarnCardIds = nextIds.joinToString(","),
                        updatedAt = updatedAt,
                    )
                }
            }
        }
    }

private fun String.toYarnCardIds(): List<Long> = split(",").mapNotNull { it.trim().toLongOrNull() }
