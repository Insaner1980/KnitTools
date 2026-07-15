package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.YarnPhotoStorage
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.domain.model.formatYarnCardIds
import com.finnvek.knittools.domain.model.parseYarnCardIds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        private val yarnPhotoStorage: YarnPhotoStorage = YarnPhotoStorage(),
    ) {
        fun getAllCards(): Flow<List<YarnCard>> =
            dao
                .getAllCards()
                .map { cards -> cards.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        fun observeCard(id: Long): Flow<YarnCard?> =
            dao
                .observeCard(id)
                .map { it?.toDomain() }
                .retryOnRepositoryReadFailure()

        suspend fun getCard(id: Long): YarnCard? = dao.getCard(id)?.toDomain()

        suspend fun getCards(ids: List<Long>): List<YarnCard> = dao.getCards(ids).map { it.toDomain() }

        suspend fun saveCard(card: YarnCard): Long =
            transactionRunner.run {
                saveCardInCurrentTransaction(card)
            }

        internal suspend fun saveCardInCurrentTransaction(card: YarnCard): Long {
            val existingCard = card.id.takeIf { it != 0L }?.let { dao.getCard(it) }
            val projects = counterProjectDao.getAllProjectsOnce()
            val cardWithPreservedDetails = card.preserveSameCardDetails(existingCard)
            val linkedProjectId =
                cardWithPreservedDetails.linkedProjectId?.takeIf { projectId ->
                    projects.any { it.id == projectId }
                }
            val normalizedCard =
                cardWithPreservedDetails.copy(
                    status = YarnCardStatus.normalize(cardWithPreservedDetails.status),
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
            return savedId
        }

        private fun YarnCard.preserveSameCardDetails(existingCard: YarnCardEntity?): YarnCard =
            existingCard?.let {
                copy(
                    fiberContent = it.fiberContent,
                    weightGrams = it.weightGrams,
                    lengthMeters = it.lengthMeters,
                    needleSize = it.needleSize,
                    gaugeInfo = it.gaugeInfo,
                    careSymbols = it.careSymbols,
                    photoUri = it.photoUri,
                    createdAt = it.createdAt,
                    quantityInStash = it.quantityInStash,
                    status = it.status,
                    linkedProjectId = it.linkedProjectId,
                )
            } ?: this

        fun getCardCount(): Flow<Int> = dao.getCardCount().retryOnRepositoryReadFailure()

        suspend fun updateQuantity(
            id: Long,
            quantity: Int,
        ): Boolean = dao.updateQuantity(id, quantity) > 0

        suspend fun updateStatus(
            id: Long,
            status: String,
        ): Boolean = dao.updateStatus(id, status) > 0

        suspend fun updatePhotoUri(
            id: Long,
            sourceUri: Uri,
        ): Boolean {
            val currentCard = dao.getCard(id) ?: return false
            val copiedPhotoUri =
                withContext(ioDispatcher) {
                    yarnPhotoStorage.copyPhoto(context, id, sourceUri)
                }
            val updateResult = runCatching { dao.updatePhotoUri(id, copiedPhotoUri) }
            updateResult.exceptionOrNull()?.let { failure ->
                deleteAppOwnedPhoto(copiedPhotoUri)
                throw failure
            }
            val updated = updateResult.getOrThrow() > 0
            if (updated) {
                deleteAppOwnedPhoto(currentCard.photoUri)
            } else {
                deleteAppOwnedPhoto(copiedPhotoUri)
            }
            return updated
        }

        suspend fun pruneUnreferencedPhotoFiles() {
            val referencedPhotoUris =
                dao
                    .getAllCards()
                    .first()
                    .map { card -> card.photoUri }
                    .filter { photoUri -> photoUri.isNotBlank() }
                    .toSet()
            withContext(ioDispatcher) {
                yarnPhotoStorage.pruneUnreferencedPhotos(context, referencedPhotoUris)
            }
        }

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
            withContext(ioDispatcher + NonCancellable) {
                cards.forEach { card -> AppFileStorage.deleteIfAppOwned(context, card.photoUri) }
            }
        }

        private suspend fun removeCardIdsFromProjects(cardIds: Set<Long>) {
            val updatedAt = System.currentTimeMillis()
            counterProjectDao.getAllProjectsOnce().forEach { project ->
                val currentIds = parseYarnCardIds(project.yarnCardIds)
                val nextIds = currentIds.filterNot { it in cardIds }
                if (nextIds.size != currentIds.size) {
                    counterProjectDao.updateYarnCardIds(
                        id = project.id,
                        yarnCardIds = formatYarnCardIds(nextIds),
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
                val currentIds = parseYarnCardIds(project.yarnCardIds)
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
                        yarnCardIds = formatYarnCardIds(nextIds),
                        updatedAt = updatedAt,
                    )
                }
            }
        }

        private suspend fun deleteAppOwnedPhoto(photoUri: String) {
            withContext(ioDispatcher + NonCancellable) {
                AppFileStorage.deleteIfAppOwned(context, photoUri)
            }
        }
    }
