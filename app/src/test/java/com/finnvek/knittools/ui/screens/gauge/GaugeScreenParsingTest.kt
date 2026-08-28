package com.finnvek.knittools.ui.screens.gauge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import com.finnvek.knittools.domain.model.GaugeBasis
import com.finnvek.knittools.domain.model.MeasurementUnit
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class GaugeScreenParsingTest {
    private lateinit var viewModel: GaugeViewModel
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val preferences = mockk<PreferencesManager>()
        val pro = mockk<ProManager>()
        every { preferences.preferences } returns flowOf(AppPreferences())
        every { pro.proState } returns MutableStateFlow(ProState(status = ProStatus.PRO_PURCHASED))
        viewModel = GaugeViewModel(SavedStateHandle(), preferences, mockk<CounterRepository>(), pro)
        store.put("gauge", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `metric gauge pasted into imperial screen preserves exact physical density`() {
        viewModel.onAction(GaugeAction.Basis(GaugeBasis.PER_4_INCHES))
        viewModel.onAction(
            GaugeAction.Paste(
                ParsedInstruction.Gauge(22.0, 30.0, ParsedInstruction.GaugeUnit.PER_10_CM),
            ),
        )

        assertEquals(
            22.352,
            viewModel.state.value
                .input(GaugeField.ACTUAL_STITCHES)
                .text
                .toDouble(),
            1e-12,
        )
        assertEquals(
            30.48,
            viewModel.state.value
                .input(GaugeField.ACTUAL_ROWS)
                .text
                .toDouble(),
            1e-12,
        )
        assertEquals(0.22, requireNotNull(viewModel.state.value.stitchDensity), 1e-12)
        assertEquals(0.3, requireNotNull(viewModel.state.value.rowDensity), 1e-12)
    }

    @Test
    fun `metric swatch pasted into imperial screen keeps unrounded measurement`() {
        viewModel.onAction(GaugeAction.Basis(GaugeBasis.PER_4_INCHES))
        viewModel.onAction(
            GaugeAction.Paste(
                ParsedInstruction.GaugeSwatch(
                    width = 10.0,
                    stitches = 24,
                    height = 10.0,
                    rows = 32,
                    lengthUnit = ParsedInstruction.LengthUnit.CM,
                ),
            ),
        )

        val state = viewModel.state.value
        assertEquals(
            MeasurementUnit.INCH.fromMillimeters(100.0),
            state.input(GaugeField.SWATCH_WIDTH).text.toDouble(),
            1e-12,
        )
        assertEquals(100.0, requireNotNull(state.input(GaugeField.SWATCH_WIDTH).canonicalValue), 1e-12)
        assertEquals("24", state.input(GaugeField.SWATCH_STITCHES).text)
        assertEquals("32", state.input(GaugeField.SWATCH_ROWS).text)
        assertEquals(0.24, requireNotNull(state.stitchDensity), 1e-12)
        assertEquals(0.32, requireNotNull(state.rowDensity), 1e-12)
    }

    @Test
    fun `imperial gauge pasted into metric screen is physically converted before autofill`() {
        viewModel.onAction(
            GaugeAction.Paste(
                ParsedInstruction.Gauge(22.0, 28.0, ParsedInstruction.GaugeUnit.PER_4_INCHES),
            ),
        )

        val state = viewModel.state.value
        assertEquals(22.0 / 101.6, requireNotNull(state.stitchDensity), 1e-12)
        assertEquals(28.0 / 101.6, requireNotNull(state.rowDensity), 1e-12)
        assertEquals(22.0 / 1.016, state.input(GaugeField.ACTUAL_STITCHES).text.toDouble(), 1e-12)
    }

    @Test
    fun `conversion rejects zero or negative personal gauge without blocking the valid axis`() {
        validAdjustment()
        listOf("0", "-1").forEach { invalid ->
            edit(GaugeField.ACTUAL_STITCHES, invalid)
            assertNull(viewModel.state.value.stitchAdjustment)
            assertNotNull(viewModel.state.value.rowAdjustment)
        }
        edit(GaugeField.ACTUAL_STITCHES, "22")
        edit(GaugeField.ACTUAL_ROWS, "0")
        assertNotNull(viewModel.state.value.stitchAdjustment)
        assertNull(viewModel.state.value.rowAdjustment)
    }

    @Test
    fun `conversion rejects zero or negative pattern instruction counts independently`() {
        validAdjustment()
        listOf("0", "-1").forEach { invalid ->
            edit(GaugeField.PATTERN_STITCH_COUNT, invalid)
            assertNull(viewModel.state.value.stitchAdjustment)
            assertNotNull(viewModel.state.value.rowAdjustment)
        }
        edit(GaugeField.PATTERN_STITCH_COUNT, "100")
        edit(GaugeField.PATTERN_ROW_COUNT, "0")
        assertNotNull(viewModel.state.value.stitchAdjustment)
        assertNull(viewModel.state.value.rowAdjustment)
    }

    private fun validAdjustment() {
        edit(GaugeField.PATTERN_STITCHES, "20")
        edit(GaugeField.PATTERN_ROWS, "30")
        edit(GaugeField.ACTUAL_STITCHES, "22")
        edit(GaugeField.ACTUAL_ROWS, "32")
        edit(GaugeField.PATTERN_STITCH_COUNT, "100")
        edit(GaugeField.PATTERN_ROW_COUNT, "80")
    }

    private fun edit(
        field: GaugeField,
        value: String,
    ) {
        viewModel.onAction(GaugeAction.Edit(field, value), Locale.US)
    }
}
