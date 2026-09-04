package com.finnvek.knittools.ui.screens.pattern

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.PatternBookmark
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.repository.ActivePatternBookmarks
import com.finnvek.knittools.repository.PatternBookmarkMutationResult
import com.finnvek.knittools.repository.PatternBookmarkRepository
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.ui.navigation.Screen
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatternViewerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `active document observation follows document switches and clears stale selection`() =
        runTest {
            val observed = MutableStateFlow(active(DOCUMENT_A, listOf(bookmark(1, DOCUMENT_A))))
            val viewModel = viewModel(observed)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
            advanceUntilIdle()

            viewModel.selectNearestBookmark(pageIndex = 0, yFraction = 0f)
            advanceUntilIdle()
            assertEquals(1L, viewModel.uiState.value.selectedBookmarkId)

            observed.value = active(DOCUMENT_B, listOf(bookmark(2, DOCUMENT_B)))
            advanceUntilIdle()

            assertEquals(DOCUMENT_B, viewModel.uiState.value.documentKey)
            assertEquals(
                listOf(2L),
                viewModel.uiState.value.bookmarks
                    .map { it.id },
            )
            assertNull(viewModel.uiState.value.selectedBookmarkId)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `viewer selects primary then preserves each document reader state across A B A switches`() =
        runTest {
            val bookmarkRepository = mockk<PatternBookmarkRepository>()
            every { bookmarkRepository.observeActiveBookmarks(7L) } returns
                MutableStateFlow(active(DOCUMENT_A, emptyList()))
            val documents =
                MutableStateFlow(
                    listOf(
                        projectDocument(id = 41L, key = DOCUMENT_A, isPrimary = true, page = 2, y = 0.25f),
                        projectDocument(id = 42L, key = DOCUMENT_B, isPrimary = false, page = 7, y = 0.8f),
                    ),
                )
            val documentRepository = mockk<ProjectDocumentRepository>()
            every { documentRepository.observeDocuments(7L) } returns documents
            coEvery { documentRepository.isAvailable(any()) } returns true
            coEvery { documentRepository.select(7L, any()) } returns ProjectDocumentMutationResult.Selected
            val savedState = SavedStateHandle(mapOf("projectId" to 7L))
            val viewModel = PatternViewerViewModel(bookmarkRepository, documentRepository, savedState)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.documentUiState.collect() }
            advanceUntilIdle()

            assertSelectedDocument(viewModel, id = 41L, page = 2, y = 0.25f)

            viewModel.selectDocument(42L)
            advanceUntilIdle()
            assertSelectedDocument(viewModel, id = 42L, page = 7, y = 0.8f)

            viewModel.selectDocument(41L)
            advanceUntilIdle()
            assertSelectedDocument(viewModel, id = 41L, page = 2, y = 0.25f)

            documents.value = documents.value + projectDocument(43L, "saved:93:v1", false, page = 4, y = 0.6f)
            advanceUntilIdle()
            assertEquals(
                43L,
                viewModel.documentUiState.value.selectedDocument
                    ?.id,
            )
            assertEquals(43L, savedState.get<Long>("selectedProjectDocumentId"))
            coVerify { documentRepository.select(7L, 41L) }
            coVerify { documentRepository.select(7L, 42L) }
            coVerify { documentRepository.select(7L, 43L) }
        }

    private fun assertSelectedDocument(
        viewModel: PatternViewerViewModel,
        id: Long,
        page: Int,
        y: Float,
    ) {
        val selected = viewModel.documentUiState.value.selectedDocument
        assertEquals(id, selected?.id)
        assertEquals(page, selected?.currentPage)
        assertEquals(y, selected?.readingLineYFraction)
    }

    @Test
    fun `opening a secondary document carries its selection through the viewer route`() =
        runTest {
            val route = Screen.PatternViewer(projectId = 7L, selectedProjectDocumentId = 42L).route
            val selectedId = route.substringAfter("?selectedProjectDocumentId=", "").toLongOrNull()
            val bookmarkRepository = mockk<PatternBookmarkRepository>()
            every { bookmarkRepository.observeActiveBookmarks(7L) } returns
                MutableStateFlow(active(DOCUMENT_B, emptyList()))
            val documentRepository = mockk<ProjectDocumentRepository>()
            every { documentRepository.observeDocuments(7L) } returns
                MutableStateFlow(
                    listOf(
                        projectDocument(41L, DOCUMENT_A, true, page = 2, y = 0.25f),
                        projectDocument(42L, DOCUMENT_B, false, page = 7, y = 0.8f),
                    ),
                )
            coEvery { documentRepository.isAvailable(any()) } returns true
            coEvery { documentRepository.select(7L, any()) } returns ProjectDocumentMutationResult.Selected
            val viewModel =
                PatternViewerViewModel(
                    bookmarkRepository,
                    documentRepository,
                    SavedStateHandle(mapOf("projectId" to 7L, "selectedProjectDocumentId" to selectedId)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.documentUiState.collect() }
            advanceUntilIdle()

            assertSelectedDocument(viewModel, id = 42L, page = 7, y = 0.8f)
        }

    @Test
    fun `unavailable primary remains visible but cannot activate`() =
        runTest {
            val bookmarkRepository = mockk<PatternBookmarkRepository>()
            every { bookmarkRepository.observeActiveBookmarks(7L) } returns
                MutableStateFlow(active(DOCUMENT_A, emptyList()))
            val unavailable = projectDocument(id = 41L, key = DOCUMENT_A, isPrimary = true)
            val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            every { documentRepository.observeDocuments(7L) } returns MutableStateFlow(listOf(unavailable))
            coEvery { documentRepository.isAvailable(unavailable) } returns false
            val viewModel =
                PatternViewerViewModel(
                    bookmarkRepository,
                    documentRepository,
                    SavedStateHandle(mapOf("projectId" to 7L)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.documentUiState.collect() }
            advanceUntilIdle()

            assertEquals(
                41L,
                viewModel.documentUiState.value.selectedDocument
                    ?.id,
            )
            assertEquals(ProjectDocumentError.UNAVAILABLE, viewModel.documentUiState.value.error)
            assertFalse(viewModel.documentUiState.value.isAvailable(41L))
            coVerify(exactly = 0) { documentRepository.select(any(), any()) }
        }

    @Test
    fun `unavailable restored selection falls back to an available primary`() =
        runTest {
            val bookmarkRepository = mockk<PatternBookmarkRepository>()
            every { bookmarkRepository.observeActiveBookmarks(7L) } returns
                MutableStateFlow(active(DOCUMENT_A, emptyList()))
            val primary = projectDocument(id = 41L, key = DOCUMENT_A, isPrimary = true)
            val restored = projectDocument(id = 42L, key = DOCUMENT_B, isPrimary = false)
            val documentRepository = mockk<ProjectDocumentRepository>()
            every { documentRepository.observeDocuments(7L) } returns MutableStateFlow(listOf(primary, restored))
            coEvery { documentRepository.isAvailable(primary) } returns true
            coEvery { documentRepository.isAvailable(restored) } returns false
            coEvery { documentRepository.select(7L, primary.id) } returns ProjectDocumentMutationResult.Selected
            val savedState = SavedStateHandle(mapOf("projectId" to 7L, "selectedProjectDocumentId" to restored.id))
            val viewModel = PatternViewerViewModel(bookmarkRepository, documentRepository, savedState)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.documentUiState.collect() }
            advanceUntilIdle()

            assertEquals(primary.id, viewModel.documentUiState.value.selectedDocumentId)
            assertEquals(primary.id, savedState.get<Long>("selectedProjectDocumentId"))
            coVerify(exactly = 1) { documentRepository.select(7L, primary.id) }
            coVerify(exactly = 0) { documentRepository.select(7L, restored.id) }
        }

    @Test
    fun `duplicate external document add is exposed as a document error`() =
        runTest {
            val viewModel =
                PatternViewerViewModel(
                    mockk<PatternBookmarkRepository>(relaxed = true).also { repository ->
                        every { repository.observeActiveBookmarks(7L) } returns
                            MutableStateFlow(active(null, emptyList()))
                    },
                    emptyDocumentRepository(),
                    SavedStateHandle(mapOf("projectId" to 7L)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.documentUiState.collect() }

            viewModel.handleDocumentAddResult(ProjectDocumentMutationResult.AlreadyAttached)
            advanceUntilIdle()

            assertEquals(ProjectDocumentError.DUPLICATE, viewModel.documentUiState.value.error)
        }

    @Test
    fun `add rename and delete surface exact mutation errors`() =
        runTest {
            val repository = mockk<PatternBookmarkRepository>()
            every { repository.observeActiveBookmarks(7L) } returns
                MutableStateFlow(active(DOCUMENT_A, listOf(bookmark(1, DOCUMENT_A))))
            coEvery { repository.add(7L, DOCUMENT_A, any(), any(), any()) } returns
                PatternBookmarkMutationResult.EmptyName
            coEvery { repository.rename(7L, DOCUMENT_A, 1L, any()) } returns
                PatternBookmarkMutationResult.NameTooLong
            coEvery { repository.delete(7L, DOCUMENT_A, 1L) } returns
                PatternBookmarkMutationResult.NotFound
            val viewModel =
                PatternViewerViewModel(
                    repository,
                    emptyDocumentRepository(),
                    SavedStateHandle(
                        mapOf(
                            "projectId" to 7L,
                        ),
                    ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
            advanceUntilIdle()

            viewModel.addBookmark("", pageIndex = 0, yFraction = 0.5f)
            advanceUntilIdle()
            assertEquals(PatternBookmarkError.EMPTY_NAME, viewModel.uiState.value.error)

            viewModel.renameBookmark(1L, "x".repeat(51))
            advanceUntilIdle()
            assertEquals(PatternBookmarkError.NAME_TOO_LONG, viewModel.uiState.value.error)

            viewModel.deleteBookmark(1L)
            advanceUntilIdle()
            assertEquals(PatternBookmarkError.NOT_FOUND, viewModel.uiState.value.error)
        }

    // CPD-OFF: Kirjanmerkkitestien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun `stale action fails closed without repository mutation`() =
        runTest {
            val repository = mockk<PatternBookmarkRepository>(relaxed = true)
            every { repository.observeActiveBookmarks(7L) } returns MutableStateFlow(active(null, emptyList()))
            val viewModel =
                PatternViewerViewModel(
                    repository,
                    emptyDocumentRepository(),
                    SavedStateHandle(
                        mapOf(
                            "projectId" to 7L,
                        ),
                    ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
            advanceUntilIdle()

            viewModel.addBookmark("Body", pageIndex = 0, yFraction = 0.5f)
            advanceUntilIdle()

            assertEquals(PatternBookmarkError.STALE_DOCUMENT, viewModel.uiState.value.error)
        }

    @Test
    fun `same-page and cross-page jumps emit one focus event each`() =
        runTest {
            val first = bookmark(1, DOCUMENT_A, page = 0, y = 0.3f)
            val second = bookmark(2, DOCUMENT_A, page = 3, y = 0.8f)
            val repository = mockk<PatternBookmarkRepository>()
            every { repository.observeActiveBookmarks(7L) } returns
                MutableStateFlow(active(DOCUMENT_A, listOf(first, second)))
            coEvery { repository.jumpTo(7L, DOCUMENT_A, 1L) } returns PatternBookmarkMutationResult.Success(first)
            coEvery { repository.jumpTo(7L, DOCUMENT_A, 2L) } returns PatternBookmarkMutationResult.Success(second)
            val viewModel =
                PatternViewerViewModel(
                    repository,
                    emptyDocumentRepository(),
                    SavedStateHandle(
                        mapOf(
                            "projectId" to 7L,
                        ),
                    ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
            advanceUntilIdle()

            val samePageEvent = async { viewModel.events.first() }
            viewModel.jumpToBookmark(1L)
            advanceUntilIdle()
            assertEquals(first, (samePageEvent.await() as PatternViewerEvent.BookmarkJumped).bookmark)

            val crossPageEvent = async { viewModel.events.first() }
            viewModel.jumpToBookmark(2L)
            advanceUntilIdle()
            val jumped = crossPageEvent.await() as PatternViewerEvent.BookmarkJumped
            assertEquals(second, jumped.bookmark)
            assertEquals(2L, viewModel.uiState.value.selectedBookmarkId)
        }

    @Test
    fun `cancellation is not converted into a mutation error`() =
        runTest {
            val repository = mockk<PatternBookmarkRepository>()
            every { repository.observeActiveBookmarks(7L) } returns MutableStateFlow(active(DOCUMENT_A, emptyList()))
            coEvery { repository.add(7L, DOCUMENT_A, any(), any(), any()) } throws CancellationException("cancelled")
            val viewModel =
                PatternViewerViewModel(
                    repository,
                    emptyDocumentRepository(),
                    SavedStateHandle(
                        mapOf(
                            "projectId" to 7L,
                        ),
                    ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
            advanceUntilIdle()

            viewModel.addBookmark("Body", pageIndex = 0, yFraction = 0.5f)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isMutating)
        }

    // CPD-ON

    private fun viewModel(observed: MutableStateFlow<ActivePatternBookmarks>): PatternViewerViewModel {
        val repository = mockk<PatternBookmarkRepository>()
        every { repository.observeActiveBookmarks(7L) } returns observed
        return PatternViewerViewModel(repository, emptyDocumentRepository(), SavedStateHandle(mapOf("projectId" to 7L)))
    }

    private fun emptyDocumentRepository(): ProjectDocumentRepository =
        mockk<ProjectDocumentRepository>(relaxed = true).also { repository ->
            every { repository.observeDocuments(7L) } returns MutableStateFlow(emptyList())
        }

    private fun active(
        documentKey: String?,
        bookmarks: List<PatternBookmark>,
    ) = ActivePatternBookmarks(documentKey = documentKey, bookmarks = bookmarks)

    private fun bookmark(
        id: Long,
        documentKey: String,
        page: Int = 0,
        y: Float = 0.5f,
    ) = PatternBookmark(
        id = id,
        projectId = 7,
        documentKey = documentKey,
        name = "Bookmark $id",
        pageIndex = page,
        yFraction = y,
        createdAt = id,
    )

    private fun projectDocument(
        id: Long,
        key: String,
        isPrimary: Boolean,
        page: Int = 0,
        y: Float = 0.5f,
    ) = ProjectDocument(
        id = id,
        projectId = 7L,
        savedPatternId = null,
        documentKey = key,
        label = "Pattern $id",
        localPdfUri = "content://pattern/$id",
        sortOrder = id.toInt(),
        isPrimary = isPrimary,
        currentPage = page,
        rowMapping = null,
        readingLineEnabled = true,
        readingLineYFraction = y,
        readingLineFollowCurrentRow = true,
        verticalReadingGuideEnabled = false,
        verticalReadingGuideXFraction = 0.5f,
        createdAt = id,
        updatedAt = id,
    )

    private companion object {
        const val DOCUMENT_A = "saved:91:v1"
        const val DOCUMENT_B = "saved:92:v1"
    }
}
