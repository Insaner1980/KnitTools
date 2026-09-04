package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.calculator.ProjectCounterLogic
import com.finnvek.knittools.domain.calculator.RepeatSectionLogic
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ProjectCounterMutationResult {
    data class Success(
        val counterId: Long,
    ) : ProjectCounterMutationResult

    data object ProjectUnavailable : ProjectCounterMutationResult

    data object CounterUnavailable : ProjectCounterMutationResult

    data object StaleAction : ProjectCounterMutationResult

    data object InvalidCounter : ProjectCounterMutationResult

    data object FeatureUnavailable : ProjectCounterMutationResult

    data object PersistenceFailure : ProjectCounterMutationResult
}

@Singleton
class ProjectCounterRepository
    @Inject
    constructor(
        private val dao: ProjectCounterDao,
        private val projectDao: CounterProjectDao,
        private val proManager: ProManager,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        fun getCountersForProject(projectId: Long): Flow<List<ProjectCounter>> =
            dao
                .getCountersForProject(projectId)
                .map { counters -> counters.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        suspend fun addCounter(counter: ProjectCounter): ProjectCounterMutationResult {
            val validated =
                ProjectCounterLogic.validatedForPersistence(counter)
                    ?: return ProjectCounterMutationResult.InvalidCounter
            return runMutation {
                if (!canCreateCounter(validated.counterType)) {
                    return@runMutation ProjectCounterMutationResult.FeatureUnavailable
                }
                val project =
                    projectDao.getProject(validated.projectId)?.toDomain()
                        ?: return@runMutation ProjectCounterMutationResult.ProjectUnavailable
                val initialCounter =
                    if (validated.counterType == ProjectCounterType.REPEAT_SECTION) {
                        RepeatSectionLogic.updatePosition(validated, project.count)
                    } else {
                        validated
                    }
                ProjectCounterMutationResult.Success(dao.insert(initialCounter.toEntity()))
            }
        }

        suspend fun incrementCounter(counter: ProjectCounter): ProjectCounterMutationResult =
            updateCounterCount(counter, ProjectCounterLogic::increment)

        suspend fun decrementCounter(counter: ProjectCounter): ProjectCounterMutationResult =
            updateCounterCount(counter, ProjectCounterLogic::decrement)

        suspend fun resetCounter(
            projectId: Long,
            id: Long,
        ): ProjectCounterMutationResult =
            mutateOwnedCounter(projectId, id) { current ->
                if (current.count != 0) dao.updateCount(id, 0)
            }

        suspend fun deleteCounter(
            projectId: Long,
            id: Long,
        ): ProjectCounterMutationResult =
            mutateOwnedCounter(projectId, id) {
                dao.delete(id)
            }

        suspend fun renameCounter(
            projectId: Long,
            id: Long,
            name: String,
        ): ProjectCounterMutationResult {
            val normalizedName = name.trim().take(ProjectCounterLogic.MAX_NAME_LENGTH)
            if (normalizedName.isBlank()) return ProjectCounterMutationResult.InvalidCounter
            return mutateOwnedCounter(projectId, id) { current ->
                if (current.name != normalizedName) dao.updateName(id, normalizedName)
            }
        }

        suspend fun updateRepeatSectionState(
            projectId: Long,
            id: Long,
        ): ProjectCounterMutationResult =
            mutateOwnedCounter(projectId, id) { current ->
                if (current.counterType != ProjectCounterType.REPEAT_SECTION) {
                    throw MutationAbort(ProjectCounterMutationResult.InvalidCounter)
                }
                val mainRowCount =
                    projectDao.getProject(projectId)?.count
                        ?: throw MutationAbort(ProjectCounterMutationResult.ProjectUnavailable)
                val updated = RepeatSectionLogic.updatePosition(current, mainRowCount)
                if (updated.count != current.count || updated.currentRepeat != current.currentRepeat) {
                    dao.updateRepeatSectionState(id, updated.count, updated.currentRepeat)
                }
            }

        private suspend fun updateCounterCount(
            requested: ProjectCounter,
            update: (ProjectCounter) -> ProjectCounter,
        ): ProjectCounterMutationResult =
            mutateOwnedCounter(requested.projectId, requested.id) { current ->
                val updated = update(current)
                if (updated.count != current.count) {
                    dao.updateCount(requested.id, updated.count)
                }
            }

        private suspend fun mutateOwnedCounter(
            projectId: Long,
            counterId: Long,
            mutation: suspend (ProjectCounter) -> Unit,
        ): ProjectCounterMutationResult =
            runMutation {
                if (projectDao.getProject(projectId) == null) {
                    return@runMutation ProjectCounterMutationResult.ProjectUnavailable
                }
                val current =
                    dao.getCounter(counterId)?.toDomain()
                        ?: return@runMutation ProjectCounterMutationResult.CounterUnavailable
                if (current.projectId != projectId) {
                    return@runMutation ProjectCounterMutationResult.StaleAction
                }
                mutation(current)
                ProjectCounterMutationResult.Success(counterId)
            }

        private fun canCreateCounter(type: ProjectCounterType): Boolean =
            proManager.hasFeature(ProFeature.MULTIPLE_COUNTERS) &&
                when (type) {
                    ProjectCounterType.SHAPING -> proManager.hasFeature(ProFeature.SHAPING_COUNTER)
                    ProjectCounterType.REPEAT_SECTION -> proManager.hasFeature(ProFeature.REPEAT_SECTION)
                    else -> true
                }

        private suspend fun runMutation(
            block: suspend () -> ProjectCounterMutationResult,
        ): ProjectCounterMutationResult =
            try {
                transactionRunner.run(block)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (abort: MutationAbort) {
                abort.result
            } catch (_: Exception) {
                ProjectCounterMutationResult.PersistenceFailure
            }

        private class MutationAbort(
            val result: ProjectCounterMutationResult,
        ) : RuntimeException()
    }
