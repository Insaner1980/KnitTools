package com.finnvek.knittools.ui.screens.yarncard

import android.net.Uri
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class YarnCardViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: YarnCardRepository
    private lateinit var counterRepository: CounterRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        counterRepository = mockk(relaxed = true)

        every { repository.getAllCards() } returns flowOf(emptyList())
        every { counterRepository.getActiveProjects() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = YarnCardViewModel(repository, counterRepository)

    @Test
    fun completedLinkedProjectKeepsItsNameAndUpdatesFromObservation() =
        runTest {
            val project =
                kotlinx.coroutines.flow.MutableStateFlow<com.finnvek.knittools.domain.model.CounterProject?>(
                    com.finnvek.knittools.domain.model
                        .CounterProject(id = 9L, name = "Completed", isCompleted = true),
                )
            every { counterRepository.observeProject(9L) } returns project
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, yarnName = "Yarn", linkedProjectId = 9L))
            backgroundScope.launch(testDispatcher) { vm.linkedProjectName.collect {} }
            assertEquals("Completed", vm.linkedProjectName.value)
            project.value = project.value?.copy(name = "Renamed")
            assertEquals("Renamed", vm.linkedProjectName.value)
            project.value = null
            assertNull(vm.linkedProjectName.value)
        }

    @Test
    fun `detail write failures report errors without changing observed card and permit retry`() =
        runTest {
            val vm = createViewModel()
            val card = YarnCard(id = 7L, yarnName = "Original", quantityInStash = 3)
            vm.loadFromCard(card)
            val original = vm.formState.value
            val errors = mutableListOf<Unit>()
            backgroundScope.launch(testDispatcher) { vm.mutationErrors.collect { errors.add(it) } }
            coEvery { repository.changeQuantity(7L, 1) } throws IOException("quantity")
            coEvery { repository.updateStatus(7L, YarnCardStatus.IN_USE) } throws IOException("status")
            coEvery { repository.updateLinkedProjectId(7L, 99L) } throws IOException("link")
            coEvery { repository.saveCard(any()) } throws IOException("details")

            vm.updateQuantity(1)
            vm.updateStatus(YarnCardStatus.IN_USE)
            vm.setLinkedProject(99L)
            vm.updateManualDetails(ManualYarnCardInput(yarnName = "Changed"))
            advanceUntilIdle()

            assertEquals(4, errors.size)
            assertEquals(original, vm.formState.value)
            coEvery { repository.changeQuantity(7L, 1) } returns true
            vm.updateQuantity(1)
            advanceUntilIdle()
            coVerify(exactly = 2) { repository.changeQuantity(7L, 1) }
            assertEquals(4, errors.size)
        }

    @Test
    fun `rejected detail writes are reported rather than treated as success`() =
        runTest {
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, yarnName = "Original"))
            val errors = mutableListOf<Unit>()
            backgroundScope.launch(testDispatcher) { vm.mutationErrors.collect { errors.add(it) } }
            coEvery { repository.changeQuantity(7L, 1) } returns false
            coEvery { repository.updateStatus(7L, YarnCardStatus.IN_USE) } returns false
            coEvery { repository.updateLinkedProjectId(7L, 99L) } returns false
            coEvery { repository.saveCard(any()) } returns null

            vm.updateQuantity(1)
            vm.updateStatus(YarnCardStatus.IN_USE)
            vm.setLinkedProject(99L)
            vm.updateManualDetails(ManualYarnCardInput(yarnName = "Changed"))
            advanceUntilIdle()

            assertEquals(4, errors.size)
        }

    @Test
    fun `failed delete invokes failure callback and only successful retry invokes navigation`() =
        runTest {
            val vm = createViewModel()
            val errors = mutableListOf<Unit>()
            backgroundScope.launch(testDispatcher) { vm.mutationErrors.collect { errors.add(it) } }
            var failed = 0
            var deleted = 0
            coEvery { repository.deleteCard(7L) } throws IOException("delete")
            vm.deleteCard(7L, onFailure = { failed++ }) { deleted++ }
            advanceUntilIdle()
            assertEquals(1, failed)
            assertEquals(0, deleted)
            assertEquals(1, errors.size)

            coEvery { repository.deleteCard(7L) } returns false
            vm.deleteCard(7L, onFailure = { failed++ }) { deleted++ }
            advanceUntilIdle()
            assertEquals(2, failed)
            assertEquals(0, deleted)

            coEvery { repository.deleteCard(7L) } returns true
            vm.deleteCard(7L, onFailure = { failed++ }) { deleted++ }
            advanceUntilIdle()
            assertEquals(2, failed)
            assertEquals(1, deleted)
            assertEquals(2, errors.size)
        }

    @Test
    fun `cancelled detail write does not report failure or successful deletion`() =
        runTest {
            val vm = createViewModel()
            val errors = mutableListOf<Unit>()
            backgroundScope.launch(testDispatcher) { vm.mutationErrors.collect { errors.add(it) } }
            coEvery { repository.deleteCard(7L) } throws CancellationException("cancelled")
            var callbacks = 0

            vm.deleteCard(7L, onFailure = { callbacks++ }) { callbacks++ }
            advanceUntilIdle()

            assertEquals(0, callbacks)
            assertTrue(errors.isEmpty())
        }

    @Test
    fun `loadFromCard sets editingCardId`() {
        val card =
            YarnCard(
                id = 5,
                brand = "Drops",
                yarnName = "Alpaca",
                careSymbols = 42L,
            )

        val vm = createViewModel()
        vm.loadFromCard(card)

        val form = vm.formState.value
        assertEquals(5L, form.editingCardId)
        assertEquals("Drops", form.brand)
        assertEquals("Alpaca", form.yarnName)
        assertEquals(42L, form.careSymbols)
    }

    @Test
    fun `updateStatus ignores unsupported values`() =
        runTest {
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, status = YarnCardStatus.IN_STASH))

            vm.updateStatus("BROKEN")

            assertEquals(YarnCardStatus.IN_STASH, vm.formState.value.status)
            coVerify(exactly = 0) { repository.updateStatus(any(), any()) }
        }

    @Test
    fun `updateQuantity leaves detail state unchanged when card update is rejected`() =
        runTest {
            coEvery { repository.changeQuantity(7L, 1) } returns false
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, quantityInStash = 3))

            vm.updateQuantity(1)
            advanceUntilIdle()

            assertEquals(3, vm.formState.value.quantityInStash)
            coVerify { repository.changeQuantity(7L, 1) }
        }

    @Test
    fun `updateStatus leaves detail state unchanged when card update is rejected`() =
        runTest {
            coEvery { repository.updateStatus(7L, YarnCardStatus.IN_USE) } returns false
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, status = YarnCardStatus.IN_STASH))

            vm.updateStatus(YarnCardStatus.IN_USE)
            advanceUntilIdle()

            assertEquals(YarnCardStatus.IN_STASH, vm.formState.value.status)
        }

    @Test
    fun `setLinkedProject leaves detail state unchanged when relink is rejected`() =
        runTest {
            coEvery { repository.updateLinkedProjectId(7L, 99L) } returns false
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, linkedProjectId = null))

            vm.setLinkedProject(99L)
            advanceUntilIdle()

            assertNull(vm.formState.value.linkedProjectId)
        }

    @Test
    fun `updateManualDetails saves existing card with trimmed optional yarn details`() =
        runTest {
            val vm = createViewModel()
            for (quantity in listOf(0, 3)) {
                val savedCard = slot<YarnCard>()
                coEvery { repository.saveCard(capture(savedCard)) } returns 7L
                vm.loadFromCard(
                    YarnCard(
                        id = 7L,
                        brand = "Old brand",
                        yarnName = "Old yarn",
                        quantityInStash = quantity,
                        status = YarnCardStatus.IN_USE,
                        linkedProjectId = 11L,
                    ),
                )

                vm.updateManualDetails(
                    ManualYarnCardInput(
                        yarnName = "  Blue sock yarn  ",
                        brand = "  Regia  ",
                        quantity = 1,
                        weightCategory = "  Fingering  ",
                        colorName = "  Blue  ",
                        colorNumber = "  123  ",
                        dyeLot = "  A7  ",
                    ),
                )
                advanceUntilIdle()

                assertEquals(7L, savedCard.captured.id)
                assertEquals("Blue sock yarn", savedCard.captured.yarnName)
                assertEquals("Regia", savedCard.captured.brand)
                assertEquals(quantity, savedCard.captured.quantityInStash)
                assertEquals(YarnCardStatus.IN_USE, savedCard.captured.status)
                assertEquals(11L, savedCard.captured.linkedProjectId)
                assertEquals("Fingering", savedCard.captured.weightCategory)
                assertEquals("Blue", savedCard.captured.colorName)
                assertEquals("123", savedCard.captured.colorNumber)
                assertEquals("A7", savedCard.captured.dyeLot)
            }
        }

    @Test
    fun `updateManualDetails ignores missing cards and blank yarn names`() =
        runTest {
            val vm = createViewModel()

            vm.updateManualDetails(ManualYarnCardInput(yarnName = " "))
            advanceUntilIdle()

            coVerify(exactly = 0) { repository.saveCard(any()) }
        }

    @Test
    fun `updatePhotoUri saves selected yarn photo for existing card`() =
        runTest {
            val sourceUri = mockk<Uri>()
            coEvery { repository.updatePhotoUri(7L, sourceUri) } returns true
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, yarnName = "Sock yarn"))

            vm.updatePhotoUri(sourceUri)
            advanceUntilIdle()

            coVerify { repository.updatePhotoUri(7L, sourceUri) }
        }

    @Test
    fun `updatePhotoUri ignores missing cards`() =
        runTest {
            val sourceUri = mockk<Uri>()
            val vm = createViewModel()

            vm.updatePhotoUri(sourceUri)
            advanceUntilIdle()

            coVerify(exactly = 0) { repository.updatePhotoUri(any(), any()) }
        }

    @Test
    fun `clearFormState resets form`() {
        val vm = createViewModel()
        vm.loadFromCard(YarnCard(id = 5L, brand = "Novita", yarnName = "Nalle", photoUri = "content://photo/1"))

        vm.clearFormState()

        val form = vm.formState.value
        assertEquals("", form.brand)
        assertEquals("", form.yarnName)
        assertEquals("", form.photoUri)
    }
}
