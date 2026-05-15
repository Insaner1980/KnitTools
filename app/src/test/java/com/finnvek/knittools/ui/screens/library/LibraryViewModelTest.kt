package com.finnvek.knittools.ui.screens.library

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var savedPatternRepository: SavedPatternRepository
    private lateinit var yarnCardRepository: YarnCardRepository
    private lateinit var progressPhotoRepository: ProgressPhotoRepository
    private lateinit var counterRepository: CounterRepository

    private lateinit var savedPatterns: MutableStateFlow<List<SavedPattern>>
    private lateinit var yarnCards: MutableStateFlow<List<YarnCard>>
    private lateinit var photos: MutableStateFlow<List<ProgressPhoto>>
    private lateinit var projects: MutableStateFlow<List<CounterProject>>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        savedPatternRepository = mockk(relaxed = true)
        yarnCardRepository = mockk(relaxed = true)
        progressPhotoRepository = mockk(relaxed = true)
        counterRepository = mockk(relaxed = true)

        savedPatterns = MutableStateFlow(emptyList())
        yarnCards = MutableStateFlow(emptyList())
        photos = MutableStateFlow(emptyList())
        projects = MutableStateFlow(emptyList())

        every { savedPatternRepository.getCount() } returns flowOf(0)
        every { yarnCardRepository.getCardCount() } returns flowOf(0)
        every { progressPhotoRepository.getAllPhotoCount() } returns flowOf(0)
        every { savedPatternRepository.getAll() } returns savedPatterns
        every { yarnCardRepository.getAllCards() } returns yarnCards
        every { progressPhotoRepository.getAllPhotos() } returns photos
        every { counterRepository.getAllProjects() } returns projects
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        LibraryViewModel(
            savedPatternRepository = savedPatternRepository,
            yarnCardRepository = yarnCardRepository,
            progressPhotoRepository = progressPhotoRepository,
            counterRepository = counterRepository,
        )

    @Test
    fun `failed pattern delete clears selection state`() =
        runTest {
            coEvery { savedPatternRepository.deleteByIds(listOf(1L)) } throws RuntimeException("delete failed")
            val viewModel = createViewModel()

            viewModel.enterPatternSelectMode(1L)
            viewModel.deleteSelectedPatterns()
            advanceUntilIdle()

            assertFalse(viewModel.isPatternSelectMode.value)
            assertTrue(viewModel.selectedPatternIds.value.isEmpty())
            assertEquals(1L, viewModel.patternDeleteErrorId.value)
        }

    @Test
    fun `failed yarn delete clears selection state`() =
        runTest {
            coEvery { yarnCardRepository.deleteCards(listOf(2L)) } throws RuntimeException("delete failed")
            val viewModel = createViewModel()

            viewModel.enterYarnSelectMode(2L)
            viewModel.deleteSelectedYarn()
            advanceUntilIdle()

            assertFalse(viewModel.isYarnSelectMode.value)
            assertTrue(viewModel.selectedYarnIds.value.isEmpty())
            assertEquals(1L, viewModel.yarnDeleteErrorId.value)
        }

    @Test
    fun `failed photo delete clears selection state`() =
        runTest {
            coEvery { progressPhotoRepository.deletePhotos(listOf(3L)) } throws RuntimeException("delete failed")
            val viewModel = createViewModel()

            viewModel.enterPhotoSelectMode(3L)
            viewModel.deleteSelectedPhotos()
            advanceUntilIdle()

            assertFalse(viewModel.isPhotoSelectMode.value)
            assertTrue(viewModel.selectedPhotoIds.value.isEmpty())
        }

    @Test
    fun `pattern refresh removes missing selected ids and exits when none remain`() =
        runTest {
            savedPatterns.value = listOf(savedPattern(1L), savedPattern(2L))
            val viewModel = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.savedPatterns.collect()
            }

            viewModel.enterPatternSelectMode(1L)
            viewModel.togglePatternSelection(2L)
            savedPatterns.value = listOf(savedPattern(2L))
            advanceUntilIdle()

            assertEquals(setOf(2L), viewModel.selectedPatternIds.value)

            savedPatterns.value = emptyList()
            advanceUntilIdle()

            assertFalse(viewModel.isPatternSelectMode.value)
            assertTrue(viewModel.selectedPatternIds.value.isEmpty())
        }

    @Test
    fun `yarn refresh removes missing selected ids and exits when none remain`() =
        runTest {
            yarnCards.value = listOf(YarnCard(id = 1L), YarnCard(id = 2L))
            val viewModel = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.yarnCards.collect()
            }

            viewModel.enterYarnSelectMode(1L)
            viewModel.toggleYarnSelection(2L)
            yarnCards.value = listOf(YarnCard(id = 2L))
            advanceUntilIdle()

            assertEquals(setOf(2L), viewModel.selectedYarnIds.value)

            yarnCards.value = emptyList()
            advanceUntilIdle()

            assertFalse(viewModel.isYarnSelectMode.value)
            assertTrue(viewModel.selectedYarnIds.value.isEmpty())
        }

    @Test
    fun `photo refresh removes missing selected ids and exits when none remain`() =
        runTest {
            photos.value = listOf(progressPhoto(1L), progressPhoto(2L))
            val viewModel = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.allPhotos.collect()
            }

            viewModel.enterPhotoSelectMode(1L)
            viewModel.togglePhotoSelection(2L)
            photos.value = listOf(progressPhoto(2L))
            advanceUntilIdle()

            assertEquals(setOf(2L), viewModel.selectedPhotoIds.value)

            photos.value = emptyList()
            advanceUntilIdle()

            assertFalse(viewModel.isPhotoSelectMode.value)
            assertTrue(viewModel.selectedPhotoIds.value.isEmpty())
        }

    private fun savedPattern(id: Long) =
        SavedPattern(
            id = id,
            ravelryId = id.toInt(),
            name = "Pattern $id",
            designerName = "Designer",
        )

    private fun progressPhoto(id: Long) =
        ProgressPhoto(
            id = id,
            projectId = 1L,
            photoUri = "file://photo-$id.jpg",
            rowNumber = id.toInt(),
        )
}
