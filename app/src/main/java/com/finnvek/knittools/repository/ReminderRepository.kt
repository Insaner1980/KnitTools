package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ReminderMutationResult {
    data class Success(
        val reminderId: Long,
    ) : ReminderMutationResult

    data object ProjectUnavailable : ReminderMutationResult

    data object ReminderUnavailable : ReminderMutationResult

    data object StaleAction : ReminderMutationResult

    data object InvalidReminder : ReminderMutationResult

    data object FeatureUnavailable : ReminderMutationResult

    data object PersistenceFailure : ReminderMutationResult
}

@Singleton
class ReminderRepository
    @Inject
    constructor(
        private val dao: RowReminderDao,
        private val projectDao: CounterProjectDao,
        private val proManager: ProManager,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        fun getRemindersForProject(projectId: Long): Flow<List<RowReminder>> =
            dao
                .getRemindersForProject(projectId)
                .map { reminders -> reminders.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        suspend fun insert(reminder: RowReminder): ReminderMutationResult {
            val validated =
                ReminderLogic.validatedForPersistence(reminder)
                    ?: return ReminderMutationResult.InvalidReminder
            return runMutation {
                if (!proManager.hasFeature(ProFeature.ROW_REMINDERS)) {
                    return@runMutation ReminderMutationResult.FeatureUnavailable
                }
                if (projectDao.getProject(validated.projectId) == null) {
                    return@runMutation ReminderMutationResult.ProjectUnavailable
                }
                ReminderMutationResult.Success(dao.insert(validated.toEntity()))
            }
        }

        suspend fun update(
            projectId: Long,
            reminder: RowReminder,
        ): ReminderMutationResult {
            val validated =
                ReminderLogic.validatedForPersistence(reminder)
                    ?: return ReminderMutationResult.InvalidReminder
            return mutateOwnedReminder(projectId, validated.id) { current ->
                dao.update(
                    validated
                        .copy(
                            id = current.id,
                            projectId = current.projectId,
                            createdAt = current.createdAt,
                        ).toEntity(),
                )
            }
        }

        suspend fun delete(
            projectId: Long,
            id: Long,
        ): ReminderMutationResult =
            mutateOwnedReminder(projectId, id) {
                dao.delete(id)
            }

        suspend fun deleteAllForProject(projectId: Long) = dao.deleteAllForProject(projectId)

        private suspend fun mutateOwnedReminder(
            projectId: Long,
            reminderId: Long,
            mutation: suspend (RowReminder) -> Unit,
        ): ReminderMutationResult =
            runMutation {
                if (projectDao.getProject(projectId) == null) {
                    return@runMutation ReminderMutationResult.ProjectUnavailable
                }
                val current =
                    dao.getReminder(reminderId)?.toDomain()
                        ?: return@runMutation ReminderMutationResult.ReminderUnavailable
                if (current.projectId != projectId) {
                    return@runMutation ReminderMutationResult.StaleAction
                }
                mutation(current)
                ReminderMutationResult.Success(reminderId)
            }

        private suspend fun runMutation(block: suspend () -> ReminderMutationResult): ReminderMutationResult =
            try {
                transactionRunner.run(block)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ReminderMutationResult.PersistenceFailure
            }
    }
