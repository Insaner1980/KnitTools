package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.PatternAnnotationLayerDao
import com.finnvek.knittools.data.local.PatternBookmarkDao
import com.finnvek.knittools.data.local.PatternBookmarkEntity
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.PatternBookmark
import com.finnvek.knittools.domain.model.PatternBookmarkNameValidation
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.domain.model.validatePatternBookmarkName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ActivePatternBookmarks(
    val documentKey: String?,
    val bookmarks: List<PatternBookmark>,
)

sealed interface PatternBookmarkMutationResult {
    data class Success(
        val bookmark: PatternBookmark,
    ) : PatternBookmarkMutationResult

    data object EmptyName : PatternBookmarkMutationResult

    data object NameTooLong : PatternBookmarkMutationResult

    data object InvalidLocation : PatternBookmarkMutationResult

    data object StaleDocument : PatternBookmarkMutationResult

    data object NotFound : PatternBookmarkMutationResult
}

@Singleton
class PatternBookmarkRepository
    @Inject
    constructor(
        private val bookmarkDao: PatternBookmarkDao,
        private val annotationLayerDao: PatternAnnotationLayerDao,
        private val projectDocumentRepository: ProjectDocumentRepository,
        private val transactionRunner: DatabaseTransactionRunner,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        fun observeActiveBookmarks(projectId: Long): Flow<ActivePatternBookmarks> =
            annotationLayerDao
                .observeProjectLayers(projectId)
                .flatMapLatest { layers ->
                    val documentKey = layers.singleOrNull { it.isActive }?.documentKey
                    if (documentKey == null) {
                        flowOf(ActivePatternBookmarks(documentKey = null, bookmarks = emptyList()))
                    } else {
                        bookmarkDao
                            .observeForProjectDocument(projectId, documentKey)
                            .map { bookmarks ->
                                ActivePatternBookmarks(
                                    documentKey = documentKey,
                                    bookmarks = bookmarks.map(PatternBookmarkEntity::toDomain),
                                )
                            }
                    }
                }.retryOnRepositoryReadFailure()
                .flowOn(ioDispatcher)

        suspend fun add(
            projectId: Long,
            expectedDocumentKey: String,
            name: String,
            pageIndex: Int,
            yFraction: Float,
        ): PatternBookmarkMutationResult =
            mutateWithValidatedName(name) { validatedName ->
                if (pageIndex < 0 || !yFraction.isFinite()) {
                    return@mutateWithValidatedName PatternBookmarkMutationResult.InvalidLocation
                }
                val activeDocumentKey = activeDocumentKey(projectId)
                if (activeDocumentKey != expectedDocumentKey) {
                    return@mutateWithValidatedName PatternBookmarkMutationResult.StaleDocument
                }
                val bookmark =
                    PatternBookmarkEntity(
                        projectId = projectId,
                        documentKey = expectedDocumentKey,
                        name = validatedName,
                        pageIndex = pageIndex,
                        yFraction = sanitizeReadingLineYFraction(yFraction),
                        createdAt = System.currentTimeMillis(),
                    )
                val id = bookmarkDao.insert(bookmark)
                PatternBookmarkMutationResult.Success(bookmark.copy(id = id).toDomain())
            }

        suspend fun rename(
            projectId: Long,
            expectedDocumentKey: String,
            bookmarkId: Long,
            name: String,
        ): PatternBookmarkMutationResult =
            mutateWithValidatedName(name) { validatedName ->
                val bookmark =
                    validatedBookmark(projectId, expectedDocumentKey, bookmarkId)
                        ?: return@mutateWithValidatedName bookmarkFailure(projectId, expectedDocumentKey, bookmarkId)
                bookmarkDao.updateName(bookmarkId, validatedName)
                PatternBookmarkMutationResult.Success(bookmark.copy(name = validatedName).toDomain())
            }

        suspend fun delete(
            projectId: Long,
            expectedDocumentKey: String,
            bookmarkId: Long,
        ): PatternBookmarkMutationResult =
            withContext(ioDispatcher) {
                transactionRunner.run {
                    val bookmark =
                        validatedBookmark(projectId, expectedDocumentKey, bookmarkId)
                            ?: return@run bookmarkFailure(projectId, expectedDocumentKey, bookmarkId)
                    bookmarkDao.deleteById(bookmarkId)
                    PatternBookmarkMutationResult.Success(bookmark.toDomain())
                }
            }

        suspend fun jumpTo(
            projectId: Long,
            expectedDocumentKey: String,
            bookmarkId: Long,
        ): PatternBookmarkMutationResult =
            withContext(ioDispatcher) {
                transactionRunner.run {
                    val bookmark =
                        validatedBookmark(projectId, expectedDocumentKey, bookmarkId)
                            ?: return@run bookmarkFailure(projectId, expectedDocumentKey, bookmarkId)
                    val document =
                        projectDocumentRepository
                            .getActiveDocument(projectId)
                            ?.takeIf { it.documentKey == expectedDocumentKey }
                            ?: return@run PatternBookmarkMutationResult.StaleDocument
                    val updated =
                        projectDocumentRepository.updateViewerStateInTransaction(
                            document.copy(
                                currentPage = bookmark.pageIndex,
                                readingLineYFraction = sanitizeReadingLineYFraction(bookmark.yFraction),
                                readingLineFollowCurrentRow = false,
                            ),
                        )
                    if (!updated) return@run PatternBookmarkMutationResult.StaleDocument
                    PatternBookmarkMutationResult.Success(bookmark.toDomain())
                }
            }

        @Suppress("kotlin:S6311") // Repository-rajan kirjoitukset käyttävät projektin injektoitua IO-dispatcheria.
        private suspend fun mutateWithValidatedName(
            name: String,
            mutation: suspend (String) -> PatternBookmarkMutationResult,
        ): PatternBookmarkMutationResult =
            when (val validation = validatePatternBookmarkName(name)) {
                PatternBookmarkNameValidation.Empty -> PatternBookmarkMutationResult.EmptyName
                PatternBookmarkNameValidation.TooLong -> PatternBookmarkMutationResult.NameTooLong
                is PatternBookmarkNameValidation.Valid ->
                    withContext(ioDispatcher) {
                        transactionRunner.run { mutation(validation.name) }
                    }
            }

        private suspend fun activeDocumentKey(projectId: Long): String? =
            annotationLayerDao.getActiveProjectLayer(projectId)?.documentKey

        private suspend fun validatedBookmark(
            projectId: Long,
            expectedDocumentKey: String,
            bookmarkId: Long,
        ): PatternBookmarkEntity? {
            if (activeDocumentKey(projectId) != expectedDocumentKey) return null
            return bookmarkDao
                .getById(bookmarkId)
                ?.takeIf { it.projectId == projectId && it.documentKey == expectedDocumentKey }
        }

        private suspend fun bookmarkFailure(
            projectId: Long,
            expectedDocumentKey: String,
            bookmarkId: Long,
        ): PatternBookmarkMutationResult =
            if (activeDocumentKey(projectId) != expectedDocumentKey) {
                PatternBookmarkMutationResult.StaleDocument
            } else if (bookmarkDao.getById(bookmarkId) == null) {
                PatternBookmarkMutationResult.NotFound
            } else {
                PatternBookmarkMutationResult.StaleDocument
            }
    }

private fun PatternBookmarkEntity.toDomain(): PatternBookmark =
    PatternBookmark(
        id = id,
        projectId = projectId,
        documentKey = documentKey,
        name = name,
        pageIndex = pageIndex,
        yFraction = sanitizeReadingLineYFraction(yFraction),
        createdAt = createdAt,
    )
