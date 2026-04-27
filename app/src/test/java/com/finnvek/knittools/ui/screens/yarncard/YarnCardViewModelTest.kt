package com.finnvek.knittools.ui.screens.yarncard

import android.content.Context
import com.finnvek.knittools.ai.ocr.ParsedYarnLabel
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.YarnLabelScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private lateinit var proManager: ProManager
    private lateinit var scanRepository: YarnLabelScanRepository
    private lateinit var aiQuotaManager: com.finnvek.knittools.ai.AiQuotaManager
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        counterRepository = mockk(relaxed = true)
        proManager = mockk()
        scanRepository = mockk(relaxed = true)
        aiQuotaManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { repository.getAllCards() } returns flowOf(emptyList())
        coEvery { counterRepository.getFirstProject() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        YarnCardViewModel(repository, counterRepository, proManager, scanRepository, aiQuotaManager, context)

    @Test
    fun `loadFromScan populates form state`() {
        val parsed =
            ParsedYarnLabel(
                brand = "Novita",
                yarnName = "7 veljestä",
                fiberContent = "75% wool, 25% polyamide",
                weightGrams = "100",
                lengthMeters = "200",
                needleSize = "3.5",
                gaugeInfo = "22 st / 10cm",
                colorName = "Charcoal",
                colorNumber = "044",
                dyeLot = "A123",
                weightCategory = "DK",
                careSymbols = 9L,
            )

        val vm = createViewModel()
        vm.loadFromScan(parsed, null)

        val form = vm.formState.value
        assertEquals("Novita", form.brand)
        assertEquals("7 veljestä", form.yarnName)
        assertEquals("75% wool, 25% polyamide", form.fiberContent)
        assertEquals("100", form.weightGrams)
        assertEquals("200", form.lengthMeters)
        assertEquals("3.5", form.needleSize)
        assertEquals("DK", form.weightCategory)
        assertEquals(9L, form.careSymbols)
        assertEquals("", form.photoUri)
    }

    @Test
    fun `loadFromCard sets editingCardId`() {
        val card =
            YarnCardEntity(
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
    fun `updateField modifies form state`() {
        val vm = createViewModel()
        vm.updateField { copy(brand = "Sandnes Garn") }

        assertEquals("Sandnes Garn", vm.formState.value.brand)
    }

    @Test
    fun `getCalculatorValues returns form values`() {
        val vm = createViewModel()
        vm.updateField { copy(weightGrams = "50", lengthMeters = "175", needleSize = "4.0") }

        val (weight, length, needle) = vm.getCalculatorValues()
        assertEquals("50", weight)
        assertEquals("175", length)
        assertEquals("4.0", needle)
    }

    @Test
    fun `saveCard does nothing without pro`() =
        runTest {
            every { proManager.hasFeature(ProFeature.OCR) } returns false

            val vm = createViewModel()
            vm.saveCard {}

            coVerify(exactly = 0) { repository.saveCard(any()) }
        }

    @Test
    fun `saveCard saves with pro`() =
        runTest {
            every { proManager.hasFeature(ProFeature.OCR) } returns true
            coEvery { repository.saveCard(any()) } returns 1L

            val vm = createViewModel()
            vm.updateField { copy(brand = "Test") }

            var savedId: Long? = null
            vm.saveCard { savedId = it }

            coVerify { repository.saveCard(any()) }
            assertEquals(1L, savedId)
        }

    @Test
    fun `setPendingCalcValues and clear`() {
        val vm = createViewModel()
        vm.setPendingCalcValues("100", "200", "4.0")

        val pending = vm.pendingCalcValues.value
        assertEquals(Triple("100", "200", "4.0"), pending)

        vm.clearPendingCalcValues()
        assertNull(vm.pendingCalcValues.value)
    }

    @Test
    fun `setScanning updates form state`() {
        val vm = createViewModel()
        vm.setScanning(true)
        assertEquals(true, vm.formState.value.isScanning)

        vm.setScanning(false)
        assertEquals(false, vm.formState.value.isScanning)
    }

    @Test
    fun `discardScan resets form state`() {
        val vm = createViewModel()
        vm.updateField { copy(brand = "Novita", yarnName = "Nalle") }
        vm.discardScan()

        val form = vm.formState.value
        assertEquals("", form.brand)
        assertEquals("", form.yarnName)
    }

    @Test
    fun `linkCardToProject updates yarn card ids`() =
        runTest {
            val project =
                mockk<com.finnvek.knittools.data.local.CounterProjectEntity> {
                    every { yarnCardIds } returns ""
                }
            coEvery { counterRepository.getProject(1L) } returns project

            val vm = createViewModel()
            vm.linkCardToProject(5L, 1L)

            coVerify { counterRepository.updateProjectYarnCardIds(1L, "5") }
        }

    @Test
    fun `linkCardToProject skips duplicate`() =
        runTest {
            val project =
                mockk<com.finnvek.knittools.data.local.CounterProjectEntity> {
                    every { yarnCardIds } returns "5"
                }
            coEvery { counterRepository.getProject(1L) } returns project

            val vm = createViewModel()
            vm.linkCardToProject(5L, 1L)

            coVerify(exactly = 0) { counterRepository.updateProjectYarnCardIds(any(), any()) }
        }
}
