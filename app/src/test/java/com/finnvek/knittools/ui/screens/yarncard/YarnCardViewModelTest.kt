package com.finnvek.knittools.ui.screens.yarncard

import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

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
    fun `loadCardForDetail clears stale card state when card is missing`() =
        runTest {
            coEvery { repository.getCard(99L) } returns null
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 5L, brand = "Old", yarnName = "Card"))

            var loaded: Boolean? = null
            vm.loadCardForDetail(99L) { loaded = it }
            advanceUntilIdle()

            assertEquals(false, loaded)
            assertNull(vm.formState.value.editingCardId)
            assertEquals("", vm.formState.value.brand)
            assertEquals("", vm.formState.value.yarnName)
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
            coEvery { repository.updateQuantity(7L, 4) } returns false
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, quantityInStash = 3))

            vm.updateQuantity(1)
            advanceUntilIdle()

            assertEquals(3, vm.formState.value.quantityInStash)
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
    fun `clearFormState resets form`() {
        val vm = createViewModel()
        vm.loadFromCard(YarnCard(id = 5L, brand = "Novita", yarnName = "Nalle", photoUri = "content://photo/1"))

        vm.clearFormState()

        val form = vm.formState.value
        assertEquals("", form.brand)
        assertEquals("", form.yarnName)
        assertEquals("", form.photoUri)
    }

    @Test
    fun `linkCardToProject delegates consistent project link update`() =
        runTest {
            val vm = createViewModel()
            vm.linkCardToProject(5L, 1L)

            coVerify { repository.updateLinkedProjectId(5L, 1L) }
        }

    @Test
    fun `linkCardToProject does not bypass yarn repository for duplicate checks`() =
        runTest {
            val vm = createViewModel()
            vm.linkCardToProject(5L, 1L)

            coVerify(exactly = 0) { counterRepository.updateProjectYarnCardIds(any(), any()) }
            coVerify { repository.updateLinkedProjectId(5L, 1L) }
        }
}
