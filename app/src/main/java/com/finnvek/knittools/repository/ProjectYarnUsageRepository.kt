package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectYarnNoteDao
import com.finnvek.knittools.data.local.ProjectYarnUsageDao
import com.finnvek.knittools.data.local.ProjectYarnUsageEntity
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.YarnUsageCalculator
import com.finnvek.knittools.domain.model.ProjectYarnUsage
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface YarnUsageResult {
    data class Created(
        val usage: ProjectYarnUsage,
    ) : YarnUsageResult

    data class Updated(
        val usage: ProjectYarnUsage,
    ) : YarnUsageResult

    data object Deleted : YarnUsageResult

    data class AlreadyExists(
        val usage: ProjectYarnUsage,
    ) : YarnUsageResult

    data object ProjectMissing : YarnUsageResult

    data object SourceMissing : YarnUsageResult

    data object SourceNotOwnedByProject : YarnUsageResult

    data object UsageMissing : YarnUsageResult

    data object InvalidAmounts : YarnUsageResult

    data object InvalidConversion : YarnUsageResult

    data object StaleAction : YarnUsageResult

    data object PersistenceFailure : YarnUsageResult
}

@Singleton
class ProjectYarnUsageRepository
    @Inject
    constructor(
        private val dao: ProjectYarnUsageDao,
        private val noteDao: ProjectYarnNoteDao,
        private val cardDao: YarnCardDao,
        private val transactionRunner: DatabaseTransactionRunner,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun observeForProject(projectId: Long): Flow<List<ProjectYarnUsageItem>?> =
            dao
                .observeProject(projectId)
                .map { it?.items() }
                .retryOnRepositoryReadFailure()
                .flowOn(ioDispatcher)

        suspend fun create(
            projectId: Long,
            source: YarnUsageSource,
            amounts: YarnUsageAmounts,
            fallbackName: String,
        ): YarnUsageResult =
            mutate {
                validate(amounts)?.let { return@mutate it }
                val snapshot = dao.getProject(projectId) ?: return@mutate YarnUsageResult.ProjectMissing
                val item =
                    snapshot.items().firstOrNull { item ->
                        (
                            source.projectYarnNoteId == null ||
                                source.projectYarnNoteId == item.source.projectYarnNoteId
                        ) &&
                            (source.yarnCardId == null || source.yarnCardId == item.source.yarnCardId) &&
                            source != YarnUsageSource()
                    } ?: return@mutate sourceError(source)
                if (item.usage != null) return@mutate YarnUsageResult.AlreadyExists(item.usage)
                if (item.source.projectYarnNoteId == null && snapshot.cards.none { it.id == item.source.yarnCardId }) {
                    return@mutate YarnUsageResult.SourceNotOwnedByProject
                }
                val name = item.name.ifBlank { fallbackName.trim() }
                if (name.isBlank()) return@mutate YarnUsageResult.SourceMissing
                val now = System.currentTimeMillis()
                val row =
                    ProjectYarnUsageEntity(
                        projectId = projectId,
                        yarnCardId = item.source.yarnCardId,
                        projectYarnNoteId = item.source.projectYarnNoteId,
                        sourceNameSnapshot = name,
                        createdAt = now,
                        updatedAt = now,
                    ).withAmounts(amounts, now)
                YarnUsageResult.Created(row.copy(id = dao.insert(row)).toDomain())
            }

        suspend fun update(
            projectId: Long,
            id: Long,
            expectedUpdatedAt: Long,
            amounts: YarnUsageAmounts,
        ): YarnUsageResult =
            mutate {
                validate(amounts)?.let { return@mutate it }
                checkCurrent(projectId, id, expectedUpdatedAt)?.let { return@mutate it }
                val current = requireNotNull(dao.getById(id))
                val next = current.withAmounts(amounts, maxOf(System.currentTimeMillis(), current.updatedAt + 1))
                check(dao.update(next) == 1)
                YarnUsageResult.Updated(next.toDomain())
            }

        suspend fun delete(
            projectId: Long,
            id: Long,
            expectedUpdatedAt: Long,
        ): YarnUsageResult =
            mutate {
                checkCurrent(projectId, id, expectedUpdatedAt)?.let { return@mutate it }
                check(dao.delete(id, projectId) == 1)
                YarnUsageResult.Deleted
            }

        private suspend fun checkCurrent(
            projectId: Long,
            id: Long,
            expectedUpdatedAt: Long,
        ): YarnUsageResult? {
            if (dao.getProject(projectId) == null) return YarnUsageResult.ProjectMissing
            val row = dao.getById(id) ?: return YarnUsageResult.UsageMissing
            if (row.projectId != projectId) return YarnUsageResult.SourceNotOwnedByProject
            return YarnUsageResult.StaleAction.takeIf { row.updatedAt != expectedUpdatedAt }
        }

        private suspend fun sourceError(source: YarnUsageSource): YarnUsageResult {
            val note = source.projectYarnNoteId?.let { noteDao.getById(it) }
            val card = source.yarnCardId?.let { cardDao.getCard(it) }
            return if (note == null &&
                card == null
            ) {
                YarnUsageResult.SourceMissing
            } else {
                YarnUsageResult.SourceNotOwnedByProject
            }
        }

        private fun validate(amounts: YarnUsageAmounts): YarnUsageResult? =
            when {
                !YarnUsageCalculator.validAmounts(amounts) -> YarnUsageResult.InvalidAmounts
                (amounts.metersPerSkein != null || amounts.gramsPerSkein != null) &&
                    !YarnUsageCalculator.validConversion(
                        amounts.metersPerSkein,
                        amounts.gramsPerSkein,
                    ) -> YarnUsageResult.InvalidConversion
                else -> null
            }

        private suspend fun mutate(block: suspend () -> YarnUsageResult): YarnUsageResult =
            withContext(ioDispatcher) {
                try {
                    transactionRunner.run(block)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    YarnUsageResult.PersistenceFailure
                }
            }
    }
