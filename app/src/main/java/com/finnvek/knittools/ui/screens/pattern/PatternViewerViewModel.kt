package com.finnvek.knittools.ui.screens.pattern

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.model.PatternBookmark
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.repository.ActivePatternBookmarks
import com.finnvek.knittools.repository.PatternBookmarkMutationResult
import com.finnvek.knittools.repository.PatternBookmarkRepository
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PatternBookmarkError {
    EMPTY_NAME,
    NAME_TOO_LONG,
    INVALID_LOCATION,
    STALE_DOCUMENT,
    NOT_FOUND,
    SAVE_FAILURE,
    RENAME_FAILURE,
    DELETE_FAILURE,
}

data class PatternBookmarkUiState(
    val documentKey: String? = null,
    val bookmarks: List<PatternBookmark> = emptyList(),
    val selectedBookmarkId: Long? = null,
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val error: PatternBookmarkError? = null,
) {
    val selectedIndex: Int
        get() = bookmarks.indexOfFirst { it.id == selectedBookmarkId }

    val canGoPrevious: Boolean
        get() = selectedIndex > 0

    val canGoNext: Boolean
        get() = selectedIndex >= 0 && selectedIndex < bookmarks.lastIndex
}

enum class ProjectDocumentError {
    INVALID_LABEL,
    DUPLICATE,
    UNAVAILABLE,
    STALE_ACTION,
    MUTATION_FAILURE,
}

data class ProjectDocumentUiState(
    val documents: List<ProjectDocument> = emptyList(),
    val selectedDocumentId: Long? = null,
    val availability: Map<Long, Boolean> = emptyMap(),
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val error: ProjectDocumentError? = null,
) {
    val selectedDocument: ProjectDocument?
        get() = documents.firstOrNull { it.id == selectedDocumentId }

    fun isAvailable(documentId: Long): Boolean = availability[documentId] == true
}

sealed interface PatternViewerEvent {
    data class BookmarkJumped(
        val requestId: Long,
        val bookmark: PatternBookmark,
    ) : PatternViewerEvent
}

private data class PatternBookmarkOperationState(
    val isMutating: Boolean = false,
    val error: PatternBookmarkError? = null,
)

private data class ProjectDocumentOperationState(
    val isMutating: Boolean = false,
    val error: ProjectDocumentError? = null,
)

@HiltViewModel
class PatternViewerViewModel
    @Inject
    constructor(
        private val bookmarkRepository: PatternBookmarkRepository,
        private val documentRepository: ProjectDocumentRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val projectId = savedStateHandle.get<Long>(PROJECT_ID_KEY) ?: 0L
        private val selectedBookmarkId = MutableStateFlow<Long?>(null)
        private val operationState = MutableStateFlow(PatternBookmarkOperationState())
        private val observedBookmarks =
            bookmarkRepository
                .observeActiveBookmarks(projectId)
                .map<ActivePatternBookmarks, ActivePatternBookmarks?> { it }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )
        val uiState: StateFlow<PatternBookmarkUiState> =
            combine(observedBookmarks, selectedBookmarkId, operationState) { observed, selectedId, operation ->
                if (observed == null) {
                    return@combine PatternBookmarkUiState(
                        selectedBookmarkId = selectedId,
                        isLoading = true,
                        isMutating = operation.isMutating,
                        error = operation.error,
                    )
                }
                PatternBookmarkUiState(
                    documentKey = observed.documentKey,
                    bookmarks = observed.bookmarks,
                    selectedBookmarkId = selectedId?.takeIf { id -> observed.bookmarks.any { it.id == id } },
                    isLoading = false,
                    isMutating = operation.isMutating,
                    error = operation.error,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PatternBookmarkUiState(),
            )

        private val documents = MutableStateFlow<List<ProjectDocument>?>(null)
        private val selectedDocumentId =
            MutableStateFlow(savedStateHandle.get<Long>(SELECTED_DOCUMENT_ID_KEY))
        private val documentAvailability = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
        private val documentOperationState = MutableStateFlow(ProjectDocumentOperationState())
        val documentUiState: StateFlow<ProjectDocumentUiState> =
            combine(
                documents,
                selectedDocumentId,
                documentAvailability,
                documentOperationState,
            ) { currentDocuments, selectedId, availability, operation ->
                ProjectDocumentUiState(
                    documents = currentDocuments.orEmpty(),
                    selectedDocumentId = selectedId,
                    availability = availability,
                    isLoading = currentDocuments == null,
                    isMutating = operation.isMutating,
                    error = operation.error,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProjectDocumentUiState(),
            )

        private val eventChannel = Channel<PatternViewerEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()
        private var nextRequestId = 1L
        private var initialDocumentSelectionApplied = false

        init {
            viewModelScope.launch {
                documentRepository.observeDocuments(projectId).collect { observed ->
                    val previousIds = documents.value?.mapTo(mutableSetOf(), ProjectDocument::id)
                    documents.value = observed
                    documentAvailability.value =
                        observed.associate { document ->
                            document.id to
                                try {
                                    documentRepository.isAvailable(document)
                                } catch (cancellation: CancellationException) {
                                    throw cancellation
                                } catch (_: Exception) {
                                    false
                                }
                        }
                    val addedDocument =
                        previousIds
                            ?.let { ids -> observed.lastOrNull { it.id !in ids } }
                    if (addedDocument != null && documentAvailability.value[addedDocument.id] == true) {
                        selectDocument(addedDocument.id)
                    } else {
                        reconcileDocumentSelection(observed)
                    }
                }
            }
        }

        fun selectDocument(documentId: Long) {
            val document =
                documents.value?.firstOrNull { it.id == documentId }
                    ?: return setDocumentError(ProjectDocumentError.STALE_ACTION)
            if (documentAvailability.value[documentId] != true) {
                return setDocumentError(ProjectDocumentError.UNAVAILABLE)
            }
            runDocumentMutation(
                mutation = { documentRepository.select(projectId, document.id) },
                onSuccess = { _ ->
                    selectedDocumentId.value = document.id
                    savedStateHandle[SELECTED_DOCUMENT_ID_KEY] = document.id
                },
            )
        }

        fun addSavedPattern(savedPatternId: Long) {
            runDocumentMutation(mutation = { documentRepository.addSavedPattern(projectId, savedPatternId) })
        }

        fun handleDocumentAddResult(result: ProjectDocumentMutationResult) {
            when (result) {
                is ProjectDocumentMutationResult.Added -> Unit
                ProjectDocumentMutationResult.AlreadyAttached,
                ProjectDocumentMutationResult.DuplicateUri,
                ProjectDocumentMutationResult.DuplicateDocumentKey,
                -> setDocumentError(ProjectDocumentError.DUPLICATE)
                ProjectDocumentMutationResult.InvalidLabel -> setDocumentError(ProjectDocumentError.INVALID_LABEL)
                ProjectDocumentMutationResult.PdfUnavailable -> setDocumentError(ProjectDocumentError.UNAVAILABLE)
                ProjectDocumentMutationResult.PersistenceFailure ->
                    setDocumentError(ProjectDocumentError.MUTATION_FAILURE)
                ProjectDocumentMutationResult.MissingProject,
                ProjectDocumentMutationResult.MissingDocument,
                ProjectDocumentMutationResult.MissingSavedPattern,
                ProjectDocumentMutationResult.MetadataOnlyPattern,
                ProjectDocumentMutationResult.PrimaryChanged,
                ProjectDocumentMutationResult.Reordered,
                ProjectDocumentMutationResult.Renamed,
                ProjectDocumentMutationResult.Selected,
                is ProjectDocumentMutationResult.Removed,
                ProjectDocumentMutationResult.ViewerStateUpdated,
                ProjectDocumentMutationResult.StaleAction,
                -> setDocumentError(ProjectDocumentError.STALE_ACTION)
            }
        }

        fun renameDocument(
            documentId: Long,
            label: String,
        ) {
            runDocumentMutation(mutation = { documentRepository.rename(projectId, documentId, label) })
        }

        fun moveDocumentEarlier(documentId: Long) {
            runDocumentMutation(mutation = { documentRepository.moveEarlier(projectId, documentId) })
        }

        fun moveDocumentLater(documentId: Long) {
            runDocumentMutation(mutation = { documentRepository.moveLater(projectId, documentId) })
        }

        fun setPrimaryDocument(documentId: Long) {
            runDocumentMutation(mutation = { documentRepository.setPrimary(projectId, documentId) })
        }

        fun removeDocument(documentId: Long) {
            runDocumentMutation(mutation = { documentRepository.remove(projectId, documentId) })
        }

        fun clearDocumentError() {
            documentOperationState.update { it.copy(error = null) }
        }

        fun selectNearestBookmark(
            pageIndex: Int,
            yFraction: Float,
        ) {
            selectedBookmarkId.value =
                bookmarkIndexAtOrAfter(uiState.value.bookmarks, pageIndex, yFraction)
                    ?.let(uiState.value.bookmarks::get)
                    ?.id
        }

        fun addBookmark(
            name: String,
            pageIndex: Int,
            yFraction: Float,
        ) {
            val documentKey = uiState.value.documentKey ?: return setError(PatternBookmarkError.STALE_DOCUMENT)
            runMutation(
                mutation = { bookmarkRepository.add(projectId, documentKey, name, pageIndex, yFraction) },
                onSuccess = { bookmark -> selectedBookmarkId.value = bookmark.id },
                failureError = PatternBookmarkError.SAVE_FAILURE,
            )
        }

        fun renameBookmark(
            bookmarkId: Long,
            name: String,
        ) {
            val documentKey = uiState.value.documentKey ?: return setError(PatternBookmarkError.STALE_DOCUMENT)
            runMutation(
                mutation = { bookmarkRepository.rename(projectId, documentKey, bookmarkId, name) },
                onSuccess = { bookmark -> selectedBookmarkId.value = bookmark.id },
                failureError = PatternBookmarkError.RENAME_FAILURE,
            )
        }

        fun deleteBookmark(bookmarkId: Long) {
            val documentKey = uiState.value.documentKey ?: return setError(PatternBookmarkError.STALE_DOCUMENT)
            runMutation(
                mutation = { bookmarkRepository.delete(projectId, documentKey, bookmarkId) },
                onSuccess = { selectedBookmarkId.value = null },
                failureError = PatternBookmarkError.DELETE_FAILURE,
            )
        }

        fun jumpToBookmark(bookmarkId: Long) {
            val documentKey = uiState.value.documentKey ?: return setError(PatternBookmarkError.STALE_DOCUMENT)
            runMutation(
                mutation = { bookmarkRepository.jumpTo(projectId, documentKey, bookmarkId) },
                onSuccess = { bookmark ->
                    selectedBookmarkId.value = bookmark.id
                    eventChannel.send(
                        PatternViewerEvent.BookmarkJumped(
                            requestId = nextRequestId++,
                            bookmark = bookmark,
                        ),
                    )
                },
                failureError = PatternBookmarkError.NOT_FOUND,
            )
        }

        fun jumpToPreviousBookmark() {
            adjacentBookmarkId(uiState.value.bookmarks, uiState.value.selectedBookmarkId, offset = -1)
                ?.let(::jumpToBookmark)
        }

        fun jumpToNextBookmark() {
            adjacentBookmarkId(uiState.value.bookmarks, uiState.value.selectedBookmarkId, offset = 1)
                ?.let(::jumpToBookmark)
        }

        fun clearError() {
            operationState.update { it.copy(error = null) }
        }

        private fun runMutation(
            mutation: suspend () -> PatternBookmarkMutationResult,
            onSuccess: suspend (PatternBookmark) -> Unit,
            failureError: PatternBookmarkError,
        ) {
            viewModelScope.launch {
                operationState.value = PatternBookmarkOperationState(isMutating = true)
                try {
                    when (val result = mutation()) {
                        is PatternBookmarkMutationResult.Success -> onSuccess(result.bookmark)
                        PatternBookmarkMutationResult.EmptyName -> setError(PatternBookmarkError.EMPTY_NAME)
                        PatternBookmarkMutationResult.NameTooLong -> setError(PatternBookmarkError.NAME_TOO_LONG)
                        PatternBookmarkMutationResult.InvalidLocation -> setError(PatternBookmarkError.INVALID_LOCATION)
                        PatternBookmarkMutationResult.StaleDocument -> setError(PatternBookmarkError.STALE_DOCUMENT)
                        PatternBookmarkMutationResult.NotFound -> setError(PatternBookmarkError.NOT_FOUND)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    setError(failureError)
                } finally {
                    operationState.update { it.copy(isMutating = false) }
                }
            }
        }

        private fun setError(error: PatternBookmarkError) {
            operationState.update { it.copy(error = error) }
        }

        private fun reconcileDocumentSelection(observed: List<ProjectDocument>) {
            if (observed.isEmpty()) {
                selectedDocumentId.value = null
                savedStateHandle.remove<Long>(SELECTED_DOCUMENT_ID_KEY)
                initialDocumentSelectionApplied = true
                return
            }
            val restored = selectedDocumentId.value?.let { id -> observed.firstOrNull { it.id == id } }
            val primary = observed.firstOrNull(ProjectDocument::isPrimary)
            val ordered = observed.sortedWith(compareBy(ProjectDocument::sortOrder, ProjectDocument::id))
            val fallback =
                sequenceOf(restored, primary)
                    .filterNotNull()
                    .plus(ordered)
                    .distinctBy(ProjectDocument::id)
                    .firstOrNull { documentAvailability.value[it.id] == true }
                    ?: restored
                    ?: primary
                    ?: ordered.first()
            if (!initialDocumentSelectionApplied || restored?.id != fallback.id) {
                initialDocumentSelectionApplied = true
                if (documentAvailability.value[fallback.id] == true) {
                    selectDocument(fallback.id)
                } else {
                    selectedDocumentId.value = fallback.id
                    savedStateHandle[SELECTED_DOCUMENT_ID_KEY] = fallback.id
                    setDocumentError(ProjectDocumentError.UNAVAILABLE)
                }
            }
        }

        private fun runDocumentMutation(
            mutation: suspend () -> ProjectDocumentMutationResult,
            onSuccess: (ProjectDocumentMutationResult) -> Unit = {},
        ) {
            viewModelScope.launch {
                documentOperationState.value = ProjectDocumentOperationState(isMutating = true)
                try {
                    when (val result = mutation()) {
                        is ProjectDocumentMutationResult.Added,
                        ProjectDocumentMutationResult.PrimaryChanged,
                        ProjectDocumentMutationResult.Reordered,
                        ProjectDocumentMutationResult.Renamed,
                        ProjectDocumentMutationResult.Selected,
                        is ProjectDocumentMutationResult.Removed,
                        ProjectDocumentMutationResult.ViewerStateUpdated,
                        -> onSuccess(result)
                        ProjectDocumentMutationResult.InvalidLabel ->
                            setDocumentError(ProjectDocumentError.INVALID_LABEL)
                        ProjectDocumentMutationResult.AlreadyAttached,
                        ProjectDocumentMutationResult.DuplicateUri,
                        ProjectDocumentMutationResult.DuplicateDocumentKey,
                        -> setDocumentError(ProjectDocumentError.DUPLICATE)
                        ProjectDocumentMutationResult.PdfUnavailable ->
                            setDocumentError(ProjectDocumentError.UNAVAILABLE)
                        ProjectDocumentMutationResult.MissingDocument,
                        ProjectDocumentMutationResult.MissingProject,
                        ProjectDocumentMutationResult.MissingSavedPattern,
                        ProjectDocumentMutationResult.MetadataOnlyPattern,
                        ProjectDocumentMutationResult.StaleAction,
                        -> setDocumentError(ProjectDocumentError.STALE_ACTION)
                        ProjectDocumentMutationResult.PersistenceFailure ->
                            setDocumentError(ProjectDocumentError.MUTATION_FAILURE)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    setDocumentError(ProjectDocumentError.MUTATION_FAILURE)
                } finally {
                    documentOperationState.update { it.copy(isMutating = false) }
                }
            }
        }

        private fun setDocumentError(error: ProjectDocumentError) {
            documentOperationState.update { it.copy(error = error) }
        }

        private companion object {
            const val PROJECT_ID_KEY = "projectId"
            const val SELECTED_DOCUMENT_ID_KEY = "selectedProjectDocumentId"
        }
    }

internal fun bookmarkIndexAtOrAfter(
    bookmarks: List<PatternBookmark>,
    pageIndex: Int,
    yFraction: Float,
): Int? {
    if (bookmarks.isEmpty()) return null
    val index =
        bookmarks.indexOfFirst {
            it.pageIndex > pageIndex ||
                (it.pageIndex == pageIndex && it.yFraction >= yFraction)
        }
    return if (index >= 0) index else bookmarks.lastIndex
}

internal fun adjacentBookmarkId(
    bookmarks: List<PatternBookmark>,
    selectedBookmarkId: Long?,
    offset: Int,
): Long? {
    val selectedIndex = bookmarks.indexOfFirst { it.id == selectedBookmarkId }
    if (selectedIndex < 0) return null
    return bookmarks.getOrNull(selectedIndex + offset)?.id
}
