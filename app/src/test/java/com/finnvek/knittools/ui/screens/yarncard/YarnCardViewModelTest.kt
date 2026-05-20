package com.finnvek.knittools.ui.screens.yarncard

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.ParsedYarnLabel
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.YarnLabelScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
    fun `completed scan does not overwrite detail loaded after scan start`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val parsed =
                ParsedYarnLabel(
                    brand = "Scan Brand",
                    yarnName = "Scan Yarn",
                    weightGrams = "100",
                    lengthMeters = "200",
                )
            val photoUri = mockk<Uri>()
            every { photoUri.toString() } returns "content://scan/inactive-before"
            coEvery { aiQuotaManager.hasQuota() } returns true
            coEvery { scanRepository.scanLabel(photoUri) } returns parsed
            every { proManager.hasFeature(ProFeature.OCR) } returns true
            val vm = createViewModel()
            var navigatedToReview = false

            vm.scanWithGemini(photoUri) { navigatedToReview = true }
            vm.loadFromCard(YarnCard(id = 7L, brand = "Detail Brand", yarnName = "Detail Yarn"))
            advanceUntilIdle()

            assertEquals(7L, vm.formState.value.editingCardId)
            assertEquals("Detail Brand", vm.formState.value.brand)
            assertEquals("Detail Yarn", vm.formState.value.yarnName)
            assertEquals(false, navigatedToReview)
            coVerify(exactly = 0) { scanRepository.scanLabel(photoUri) }
            verify { scanRepository.deleteScanPhoto("content://scan/inactive-before") }
        }

    @Test
    fun `scan invalidated after repository result deletes captured photo`() =
        runTest {
            val parsed =
                ParsedYarnLabel(
                    brand = "Scan Brand",
                    yarnName = "Scan Yarn",
                    weightGrams = "100",
                    lengthMeters = "200",
                )
            val photoUri = mockk<Uri>()
            every { photoUri.toString() } returns "content://scan/inactive-after"
            coEvery { aiQuotaManager.hasQuota() } returns true
            every { proManager.hasFeature(ProFeature.OCR) } returns true
            lateinit var vm: YarnCardViewModel
            coEvery { scanRepository.scanLabel(photoUri) } coAnswers {
                vm.loadFromCard(YarnCard(id = 7L, brand = "Detail Brand", yarnName = "Detail Yarn"))
                parsed
            }
            vm = createViewModel()
            var navigatedToReview = false

            vm.scanWithGemini(photoUri) { navigatedToReview = true }
            advanceUntilIdle()

            assertEquals(7L, vm.formState.value.editingCardId)
            assertEquals("Detail Brand", vm.formState.value.brand)
            assertEquals("Detail Yarn", vm.formState.value.yarnName)
            assertEquals(false, navigatedToReview)
            verify { scanRepository.deleteScanPhoto("content://scan/inactive-after") }
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
            every { proManager.hasFeature(ProFeature.UNLIMITED_YARN) } returns false

            val vm = createViewModel()
            vm.saveCard {}

            coVerify(exactly = 0) { repository.saveCard(any()) }
        }

    @Test
    fun `saveCard rejects blank yarn identity`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_YARN) } returns true
            val vm = createViewModel()

            var savedId: Long? = null
            vm.saveCard { savedId = it }

            coVerify(exactly = 0) { repository.saveCard(any()) }
            assertNull(savedId)
        }

    @Test
    fun `saveCard trims text fields before persisting`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_YARN) } returns true
            coEvery { repository.saveCard(any()) } returns 1L
            val vm = createViewModel()
            vm.updateField {
                copy(
                    brand = "  Novita  ",
                    yarnName = "  Nalle  ",
                    weightGrams = "  100  ",
                )
            }

            vm.saveCard {}

            coVerify {
                repository.saveCard(
                    match {
                        it.brand == "Novita" &&
                            it.yarnName == "Nalle" &&
                            it.weightGrams == "100"
                    },
                )
            }
        }

    @Test
    fun `saveCard rejects invalid numeric yarn values`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_YARN) } returns true
            val vm = createViewModel()
            vm.updateField { copy(yarnName = "Nalle", weightGrams = "heavy") }

            vm.saveCard {}

            coVerify(exactly = 0) { repository.saveCard(any()) }
        }

    @Test
    fun `scanWithGemini does not scan without pro`() =
        runTest {
            every { proManager.hasFeature(ProFeature.OCR) } returns false
            val photoUri = mockk<Uri>(relaxed = true)
            val vm = createViewModel()
            var navigatedToReview = false

            vm.scanWithGemini(photoUri) { navigatedToReview = true }
            advanceUntilIdle()

            assertEquals(false, vm.formState.value.isScanning)
            assertEquals(false, navigatedToReview)
            coVerify(exactly = 0) { aiQuotaManager.hasQuota() }
            coVerify(exactly = 0) { scanRepository.scanLabel(any()) }
        }

    @Test
    fun `quota exhausted deletes captured photo`() =
        runTest {
            every { proManager.hasFeature(ProFeature.OCR) } returns true
            coEvery { aiQuotaManager.hasQuota() } returns false
            every { context.getString(R.string.ai_quota_exhausted) } returns "Quota exhausted"
            val photoUri = mockk<Uri>()
            every { photoUri.toString() } returns "content://scan/quota"
            val vm = createViewModel()

            vm.scanWithGemini(photoUri) {}
            advanceUntilIdle()

            verify { scanRepository.deleteScanPhoto("content://scan/quota") }
            coVerify(exactly = 0) { scanRepository.scanLabel(any()) }
        }

    @Test
    fun `failed scan deletes captured photo`() =
        runTest {
            every { proManager.hasFeature(ProFeature.OCR) } returns true
            coEvery { aiQuotaManager.hasQuota() } returns true
            val photoUri = mockk<Uri>()
            every { photoUri.toString() } returns "content://scan/failed"
            coEvery { scanRepository.scanLabel(photoUri) } returns null
            val vm = createViewModel()

            vm.scanWithGemini(photoUri) {}
            advanceUntilIdle()

            verify { scanRepository.deleteScanPhoto("content://scan/failed") }
        }

    @Test
    fun `saveCard saves with pro`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_YARN) } returns true
            coEvery { repository.saveCard(any()) } returns 1L

            val vm = createViewModel()
            vm.updateField { copy(brand = "Test") }

            var savedId: Long? = null
            vm.saveCard { savedId = it }

            coVerify { repository.saveCard(any()) }
            assertEquals(1L, savedId)
        }

    @Test
    fun `updateStatus ignores unsupported values`() =
        runTest {
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, status = "IN_STASH"))

            vm.updateStatus("BROKEN")

            assertEquals("IN_STASH", vm.formState.value.status)
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
            coEvery { repository.updateStatus(7L, "USED_UP") } returns false
            val vm = createViewModel()
            vm.loadFromCard(YarnCard(id = 7L, status = "IN_STASH"))

            vm.updateStatus("USED_UP")
            advanceUntilIdle()

            assertEquals("IN_STASH", vm.formState.value.status)
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
    fun `clearFormState resets form without deleting saved scan photo`() {
        val vm = createViewModel()
        vm.updateField { copy(brand = "Novita", yarnName = "Nalle", photoUri = "content://scan/1") }

        vm.clearFormState()

        val form = vm.formState.value
        assertEquals("", form.brand)
        assertEquals("", form.yarnName)
        assertEquals("", form.photoUri)
        verify(exactly = 0) { scanRepository.deleteScanPhoto(any()) }
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
