package com.finnvek.knittools.ui.screens.gauge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class GaugeViewModelTest {
    private val preferences = MutableStateFlow(AppPreferences())
    private val pro = MutableStateFlow(ProState())
    private val project = MutableStateFlow<CounterProject?>(CounterProject(id = 7, name = "Measurement fixture"))
    private val manager = mockk<PreferencesManager>()
    private val repository = mockk<CounterRepository>()
    private val proManager = mockk<ProManager>()
    private val stores = mutableListOf<ViewModelStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { manager.preferences } returns preferences
        every { proManager.proState } returns pro
        every { repository.observeProject(7) } returns project
    }

    @After
    fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun model(handle: SavedStateHandle = SavedStateHandle()): GaugeViewModel =
        GaugeViewModel(handle, manager, repository, proManager).also { model ->
            stores += ViewModelStore().apply { put("gauge", model) }
        }

    private fun GaugeViewModel.edit(
        field: GaugeField,
        text: String,
        locale: Locale = Locale.US,
    ) = onAction(GaugeAction.Edit(field, text), locale)

    private fun GaugeViewModel.swatch() {
        edit(GaugeField.SWATCH_WIDTH, "14")
        edit(GaugeField.SWATCH_STITCHES, "33")
    }

    @Test
    fun `legacy task starts empty and preferences only seed local defaults once`() {
        preferences.value = AppPreferences(useImperial = true)
        val vm = model()
        assertEquals(GaugeTask.ADJUST, vm.state.value.task)
        assertEquals(GaugeBasis.PER_4_INCHES, vm.state.value.basis)
        assertFalse(vm.state.value.hasResult)
        preferences.value = AppPreferences(useImperial = false)
        assertEquals(GaugeBasis.PER_4_INCHES, vm.state.value.basis)
        vm.onAction(GaugeAction.Basis(GaugeBasis.PER_10_CM))
        assertEquals(GaugeBasis.PER_10_CM, vm.state.value.basis)
        verify(exactly = 1) { manager.preferences }
        confirmVerified(manager)
    }

    @Test
    fun `all tasks and operations retain independent hidden values`() {
        val vm = model()
        vm.edit(GaugeField.ACTUAL_STITCHES, "20")
        vm.edit(GaugeField.ACTUAL_ROWS, "28")
        vm.edit(GaugeField.TARGET_WIDTH, "45")
        vm.edit(GaugeField.TARGET_HEIGHT, "30")
        GaugeTask.entries.forEach {
            vm.onAction(GaugeAction.Task(it))
            assertEquals(it, vm.state.value.task)
        }
        vm.onAction(GaugeAction.Task(GaugeTask.CALCULATE))
        assertEquals(
            90,
            vm.state.value.countResult
                ?.roundedCount,
        )
        vm.onAction(GaugeAction.Axis(GaugeAxis.ROWS))
        assertEquals(
            84,
            vm.state.value.countResult
                ?.roundedCount,
        )
        vm.onAction(GaugeAction.Operation(GaugeOperation.SIZE_FROM_COUNT))
        vm.edit(GaugeField.ROW_COUNT, "84")
        assertEquals(300.0, vm.state.value.sizeResultMm ?: 0.0, 1e-9)
        vm.onAction(GaugeAction.Axis(GaugeAxis.STITCHES))
        assertNull(vm.state.value.sizeResultMm)
        vm.edit(GaugeField.STITCH_COUNT, "90")
        assertEquals(450.0, vm.state.value.sizeResultMm ?: 0.0, 1e-9)
    }

    @Test
    fun `unit switches preserve physical values counts and full precision`() {
        val vm = model()
        vm.swatch()
        vm.edit(GaugeField.PATTERN_STITCHES, "20")
        vm.edit(GaugeField.PATTERN_STITCH_COUNT, "1000")
        val before = vm.state.value.stitchAdjustment
        assertEquals(1179, before?.roundedCount)
        repeat(10) {
            vm.onAction(GaugeAction.Basis(GaugeBasis.PER_4_INCHES))
            assertEquals(
                33.0,
                vm.state.value
                    .input(GaugeField.SWATCH_STITCHES)
                    .canonicalValue ?: 0.0,
                0.0,
            )
            assertEquals(
                1179,
                vm.state.value.stitchAdjustment
                    ?.roundedCount,
            )
            vm.onAction(GaugeAction.Basis(GaugeBasis.PER_10_CM))
        }
        assertEquals(before, vm.state.value.stitchAdjustment)
        assertEquals(33.0 / 140.0, vm.state.value.stitchDensity ?: 0.0, 0.0)
    }

    @Test
    fun `converter unit changes retain canonical value and copy uses target unit`() {
        val vm = model()
        vm.onAction(GaugeAction.Task(GaugeTask.CONVERT))
        vm.edit(GaugeField.CONVERSION, " 25,4 ", Locale.forLanguageTag("fi-FI"))
        assertEquals(10.0, vm.state.value.convertedLength ?: 0.0, 1e-12)
        vm.onAction(GaugeAction.FromUnit(MeasurementUnit.INCH))
        assertEquals(
            "10",
            vm.state.value
                .input(GaugeField.CONVERSION)
                .text,
        )
        vm.onAction(GaugeAction.ToUnit(MeasurementUnit.CM))
        assertEquals(25.4, vm.state.value.convertedLength ?: 0.0, 1e-12)
        vm.onAction(GaugeAction.FromUnit(MeasurementUnit.METER))
        vm.edit(GaugeField.CONVERSION, "1")
        vm.onAction(GaugeAction.ToUnit(MeasurementUnit.YARD))
        assertEquals(1.0 / 0.9144, vm.state.value.convertedLength ?: 0.0, 1e-12)
    }

    @Test
    fun `swatch axes are independent and invalidation only clears automatic provenance`() {
        val vm = model()
        vm.onAction(GaugeAction.Task(GaugeTask.MEASURE))
        vm.swatch()
        assertNotNull(vm.state.value.stitchSwatchDensity)
        assertNull(vm.state.value.rowSwatchDensity)
        vm.edit(GaugeField.SWATCH_WIDTH, "")
        assertNull(vm.state.value.stitchDensity)
        assertEquals(
            "",
            vm.state.value
                .input(GaugeField.ACTUAL_STITCHES)
                .text,
        )
        vm.edit(GaugeField.ACTUAL_STITCHES, "22")
        vm.edit(GaugeField.SWATCH_WIDTH, "-2")
        assertEquals(0.22, vm.state.value.stitchDensity ?: 0.0, 0.0)
        vm.edit(GaugeField.SWATCH_HEIGHT, "10")
        vm.edit(GaugeField.SWATCH_ROWS, "30")
        assertEquals(0.3, vm.state.value.rowDensity ?: 0.0, 0.0)
        vm.edit(GaugeField.SWATCH_ROWS, "1e3")
        assertNull(vm.state.value.rowDensity)
        assertEquals(0.22, vm.state.value.stitchDensity ?: 0.0, 0.0)
    }

    @Test
    fun `malformed edits stay raw and unit conversion refuses incomplete physical inputs`() =
        runTest {
            val vm = model()
            vm.edit(GaugeField.SWATCH_STITCHES, "12.5")
            assertEquals(
                "12.5",
                vm.state.value
                    .input(GaugeField.SWATCH_STITCHES)
                    .text,
            )
            assertNull(
                vm.state.value
                    .input(GaugeField.SWATCH_STITCHES)
                    .canonicalValue,
            )
            vm.edit(GaugeField.SWATCH_WIDTH, "10,")
            vm.events.test {
                vm.onAction(GaugeAction.Basis(GaugeBasis.PER_4_INCHES))
                assertTrue(awaitItem() is GaugeUiEvent.Message)
            }
            assertEquals(GaugeBasis.PER_10_CM, vm.state.value.basis)
            assertTrue(
                vm.state.value
                    .input(GaugeField.SWATCH_WIDTH)
                    .touched,
            )
        }

    @Test
    fun `pattern adjustment works for either axis without requiring the other`() {
        val vm = model()
        vm.edit(GaugeField.ACTUAL_ROWS, "22")
        vm.edit(GaugeField.PATTERN_ROWS, "20")
        vm.edit(GaugeField.PATTERN_ROW_COUNT, "100")
        assertNull(vm.state.value.stitchAdjustment)
        assertEquals(
            110,
            vm.state.value.rowAdjustment
                ?.roundedCount,
        )
        assertEquals(
            500.0,
            vm.state.value.rowAdjustment
                ?.originalLengthMm ?: 0.0,
            1e-9,
        )
        assertEquals(
            100.0 / 0.22,
            vm.state.value.rowAdjustment
                ?.unchangedLengthMm ?: 0.0,
            1e-9,
        )
        vm.edit(GaugeField.ACTUAL_STITCHES, "22")
        vm.edit(GaugeField.PATTERN_STITCHES, "20")
        vm.edit(GaugeField.PATTERN_STITCH_COUNT, "100")
        assertEquals(vm.state.value.stitchAdjustment, vm.state.value.rowAdjustment)
    }

    @Test
    fun `out of range results cannot become saturated whole counts`() {
        val vm = model()
        vm.onAction(GaugeAction.Task(GaugeTask.CALCULATE))
        vm.edit(GaugeField.ACTUAL_STITCHES, "1000000000")
        vm.edit(GaugeField.TARGET_WIDTH, "1000000000")
        assertFalse(vm.state.value.hasResult)
        assertEquals(MeasurementNumberError.TOO_LARGE, vm.state.value.resultError)
    }

    @Test
    fun `restored state keeps exact derived values while a fresh session is empty`() {
        val handle = SavedStateHandle()
        val vm = model(handle)
        vm.swatch()
        vm.edit(GaugeField.PATTERN_STITCHES, "20")
        vm.edit(GaugeField.PATTERN_STITCH_COUNT, "1000")
        vm.onAction(GaugeAction.Basis(GaugeBasis.PER_4_INCHES))
        vm.onAction(GaugeAction.Axis(GaugeAxis.ROWS))
        val restored = model(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(vm.state.value, restored.state.value)
        assertEquals(
            1179,
            restored.state.value.stitchAdjustment
                ?.roundedCount,
        )
        val fresh = model()
        assertFalse(fresh.state.value.hasResult)
        assertTrue(
            fresh.state.value.fields
                .isEmpty(),
        )
    }

    @Test
    fun `optional project observation never writes and deletion preserves calculator values`() {
        val vm = model(SavedStateHandle(mapOf("projectId" to "7")))
        vm.swatch()
        assertEquals("Measurement fixture", vm.state.value.projectName)
        project.value = null
        assertNull(vm.state.value.projectName)
        assertTrue(vm.state.value.projectUnavailable)
        assertNotNull(vm.state.value.stitchDensity)
        verify(exactly = 1) { repository.observeProject(7) }
        confirmVerified(repository)
    }

    @Test
    fun `generic and invalid contexts never access project repository`() {
        assertFalse(model().state.value.projectUnavailable)
        listOf("-1", "0", "no-id", Long.MAX_VALUE.toString() + "0").forEach {
            assertTrue(model(SavedStateHandle(mapOf("projectId" to it))).state.value.projectUnavailable)
        }
        confirmVerified(repository)
    }

    @Test
    fun `copy is a single event and is not replayed by restoration`() =
        runTest {
            val handle = SavedStateHandle()
            val vm = model(handle)
            vm.onAction(GaugeAction.Task(GaugeTask.CONVERT))
            vm.edit(GaugeField.CONVERSION, "25.4")
            vm.events.test {
                vm.onAction(GaugeAction.Copy("25.4 cm = 10 in"))
                assertEquals(GaugeUiEvent.Copy("25.4 cm = 10 in"), awaitItem())
                expectNoEvents()
            }
            model(
                SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }),
            ).events.test { expectNoEvents() }
        }

    @Test
    fun `Pro paste converts exact basis and is not replayed on recreation`() {
        val handle = SavedStateHandle()
        val vm = model(handle)
        val paste = GaugeAction.Paste(ParsedInstruction.Gauge(20.0, 28.0, ParsedInstruction.GaugeUnit.PER_4_INCHES))
        vm.onAction(paste)
        assertNull(vm.state.value.stitchDensity)
        pro.value = ProState(status = ProStatus.PRO_PURCHASED)
        vm.onAction(paste)
        assertEquals(20.0 / 101.6, vm.state.value.stitchDensity ?: 0.0, 1e-15)
        vm.edit(GaugeField.ACTUAL_STITCHES, "22")
        val restored = model(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(0.22, restored.state.value.stitchDensity ?: 0.0, 0.0)
    }

    @Test
    fun `partial swatch paste preserves manual opposite axis and rejects nonfinite paste`() {
        pro.value = ProState(status = ProStatus.PRO_PURCHASED)
        val vm = model()
        vm.edit(GaugeField.ACTUAL_ROWS, "30")
        vm.onAction(GaugeAction.Paste(ParsedInstruction.GaugeSwatch(width = 14.0, stitches = 33)))
        assertEquals(0.3, vm.state.value.rowDensity ?: 0.0, 0.0)
        assertEquals(33.0 / 140.0, vm.state.value.stitchDensity ?: 0.0, 0.0)
        vm.onAction(GaugeAction.Paste(ParsedInstruction.Gauge(Double.POSITIVE_INFINITY, 28.0)))
        assertNull(vm.state.value.stitchDensity)
        assertNotNull(
            vm.state.value
                .input(GaugeField.ACTUAL_STITCHES)
                .error,
        )
    }

    @Test
    fun `finite swatch inputs with overflowed density produce an actionable range error`() {
        val vm = model()
        vm.onAction(GaugeAction.Task(GaugeTask.MEASURE))
        vm.edit(GaugeField.SWATCH_STITCHES, Int.MAX_VALUE.toString())
        vm.edit(GaugeField.SWATCH_WIDTH, "0." + "0".repeat(310) + "1")
        assertFalse(vm.state.value.hasResult)
        assertEquals(MeasurementNumberError.TOO_LARGE, vm.state.value.resultError)
    }

    @Test
    fun `overflowed normalized swatch gauge is reported in every gauge task and axis`() {
        GaugeAxis.entries.forEach { axis ->
            val vm = model()
            val stitches = axis == GaugeAxis.STITCHES
            vm.onAction(GaugeAction.Axis(axis))
            vm.edit(
                if (stitches) GaugeField.SWATCH_WIDTH else GaugeField.SWATCH_HEIGHT,
                MeasurementNumberFormatter.formatEditing(1e-308),
            )
            vm.edit(if (stitches) GaugeField.SWATCH_STITCHES else GaugeField.SWATCH_ROWS, "1")
            vm.edit(if (stitches) GaugeField.TARGET_WIDTH else GaugeField.TARGET_HEIGHT, "20")
            vm.edit(if (stitches) GaugeField.PATTERN_STITCHES else GaugeField.PATTERN_ROWS, "20")
            vm.edit(if (stitches) GaugeField.PATTERN_STITCH_COUNT else GaugeField.PATTERN_ROW_COUNT, "100")
            listOf(GaugeTask.MEASURE, GaugeTask.CALCULATE, GaugeTask.ADJUST).forEach { task ->
                vm.onAction(GaugeAction.Task(task))
                assertFalse(vm.state.value.hasResult)
                assertEquals("$axis $task", MeasurementNumberError.TOO_LARGE, vm.state.value.resultError)
            }
        }
    }

    @Test
    fun `converter refuses a unit change that would turn a positive editing value into zero`() =
        runTest {
            val vm = model()
            vm.onAction(GaugeAction.Task(GaugeTask.CONVERT))
            vm.edit(GaugeField.CONVERSION, MeasurementNumberFormatter.formatEditing(Double.MIN_VALUE))
            val before = vm.state.value.input(GaugeField.CONVERSION)
            vm.events.test {
                vm.onAction(GaugeAction.FromUnit(MeasurementUnit.METER))
                assertTrue(awaitItem() is GaugeUiEvent.Message)
            }
            assertEquals(MeasurementUnit.CM, vm.state.value.fromUnit)
            assertEquals(before, vm.state.value.input(GaugeField.CONVERSION))
        }

    @Test
    fun `gauge basis refuses a length conversion that would underflow to zero`() =
        runTest {
            val vm = model()
            vm.edit(GaugeField.SWATCH_WIDTH, MeasurementNumberFormatter.formatEditing(Double.MIN_VALUE))
            val before = vm.state.value.input(GaugeField.SWATCH_WIDTH)
            vm.events.test {
                vm.onAction(GaugeAction.Basis(GaugeBasis.PER_4_INCHES))
                assertTrue(awaitItem() is GaugeUiEvent.Message)
            }
            assertEquals(GaugeBasis.PER_10_CM, vm.state.value.basis)
            assertEquals(before, vm.state.value.input(GaugeField.SWATCH_WIDTH))
        }

    @Test
    fun `an abandoned invalid manual gauge cannot block conversion of the active swatch`() =
        runTest {
            val vm = model()
            vm.edit(GaugeField.ACTUAL_STITCHES, "abc")
            vm.onAction(GaugeAction.SwatchInput(true))
            vm.swatch()
            vm.events.test {
                vm.onAction(GaugeAction.Basis(GaugeBasis.PER_4_INCHES))
                expectNoEvents()
            }
            assertEquals(GaugeBasis.PER_4_INCHES, vm.state.value.basis)
            assertEquals(33.0 / 140.0, vm.state.value.stitchDensity ?: 0.0, 0.0)
        }

    @Test
    fun `an invalid hidden swatch axis does not report an error for the selected valid axis`() {
        val vm = model()
        vm.edit(GaugeField.SWATCH_ROWS, Int.MAX_VALUE.toString())
        vm.edit(GaugeField.SWATCH_HEIGHT, MeasurementNumberFormatter.formatEditing(Double.MIN_VALUE))
        vm.edit(GaugeField.ACTUAL_STITCHES, "20")
        vm.edit(GaugeField.TARGET_WIDTH, "10")
        vm.onAction(GaugeAction.Task(GaugeTask.CALCULATE))
        assertEquals(
            20,
            vm.state.value.countResult
                ?.roundedCount,
        )
        assertNull(vm.state.value.resultError)
        vm.onAction(GaugeAction.Axis(GaugeAxis.ROWS))
        assertEquals(MeasurementNumberError.TOO_LARGE, vm.state.value.resultError)
    }
}
