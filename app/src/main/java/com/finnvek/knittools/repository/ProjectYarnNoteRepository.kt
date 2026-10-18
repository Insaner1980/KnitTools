package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.LinkSavedCardUsageResult
import com.finnvek.knittools.data.local.ProjectYarnNoteDao
import com.finnvek.knittools.data.local.ProjectYarnUsageDao
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ProjectYarnNoteEditResult {
    Saved,
    InvalidName,
    Missing,
    Conflict,
    Failed,
}

@Singleton
class ProjectYarnNoteRepository
    @Inject
    constructor(
        private val dao: ProjectYarnNoteDao,
        private val yarnCardRepository: YarnCardRepository,
        private val transactionRunner: DatabaseTransactionRunner,
        private val usageDao: ProjectYarnUsageDao,
    ) {
        fun observeForProject(projectId: Long): Flow<List<ProjectYarnNote>> =
            dao
                .observeForProject(projectId)
                .map { notes -> notes.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        suspend fun save(note: ProjectYarnNote): Long =
            transactionRunner.run {
                dao.upsert(note.normalized().toEntity())
            }

        suspend fun delete(id: Long) {
            transactionRunner.run {
                dao.delete(id)
            }
        }

        suspend fun edit(
            original: ProjectYarnNote,
            draft: ProjectYarnNote,
        ): ProjectYarnNoteEditResult =
            transactionRunner.run {
                if (draft.name.isBlank()) return@run ProjectYarnNoteEditResult.InvalidName
                val current = dao.getById(original.id)?.toDomain() ?: return@run ProjectYarnNoteEditResult.Missing
                if (current.projectId != original.projectId ||
                    draft.projectId != original.projectId ||
                    draft.id != original.id
                ) {
                    return@run ProjectYarnNoteEditResult.Missing
                }
                val normalized = draft.normalized()
                // A retry after a committed but cancelled response is already satisfied.
                if (current.sameEditableFields(normalized)) return@run ProjectYarnNoteEditResult.Saved
                if (!current.sameEditableFields(original)) return@run ProjectYarnNoteEditResult.Conflict
                val updated =
                    current.copy(
                        name = normalized.name,
                        description = normalized.description,
                        quantity = normalized.quantity,
                        notes = normalized.notes,
                        updatedAt = normalized.updatedAt,
                    )
                if (dao.update(updated.toEntity()) ==
                    1
                ) {
                    ProjectYarnNoteEditResult.Saved
                } else {
                    ProjectYarnNoteEditResult.Missing
                }
            }

        private fun ProjectYarnNote.sameEditableFields(other: ProjectYarnNote): Boolean =
            name == other.name && description == other.description && quantity == other.quantity && notes == other.notes

        suspend fun saveToMyYarn(noteId: Long): Long? {
            return transactionRunner.run {
                val note = dao.getById(noteId)?.toDomain()?.normalized() ?: return@run null
                val existingCard = note.savedYarnCardId?.let { yarnCardRepository.getCard(it) }
                if (existingCard != null) {
                    if (
                        usageDao.linkSavedCard(note.projectId, note.id, existingCard.id) ==
                        LinkSavedCardUsageResult.Conflict
                    ) {
                        return@run null
                    }
                    return@run existingCard.id
                }
                val yarnCardId =
                    yarnCardRepository.saveCardInCurrentTransaction(
                        YarnCard(
                            yarnName = note.name,
                            fiberContent = note.description,
                            quantityInStash = note.quantity,
                            status = YarnCardStatus.IN_USE,
                            linkedProjectId = note.projectId,
                            createdAt = note.createdAt,
                        ),
                    ) ?: return@run null
                dao.updateSavedYarnCardId(
                    id = note.id,
                    savedYarnCardId = yarnCardId,
                    updatedAt = System.currentTimeMillis(),
                )
                if (
                    usageDao.linkSavedCard(note.projectId, note.id, yarnCardId) ==
                    LinkSavedCardUsageResult.Conflict
                ) {
                    return@run null
                }
                yarnCardId
            }
        }

        private fun ProjectYarnNote.normalized(): ProjectYarnNote =
            copy(
                name = name.trim(),
                description = description.trim(),
                quantity = quantity.coerceAtLeast(1),
                notes = notes.trim(),
                updatedAt = System.currentTimeMillis(),
            )
    }
