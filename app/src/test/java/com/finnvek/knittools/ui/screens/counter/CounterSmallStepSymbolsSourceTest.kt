package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterSmallStepSymbolsSourceTest {
    @Test
    fun `small counter controls use drawn rounded step symbols instead of glyph icons`() {
        val stepperButton = ProjectSourceFiles.read(COUNTER_STEPPER_BUTTON)
        val stitchCounter = ProjectSourceFiles.read(STITCH_COUNTER)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val multiCounter = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)

        listOf(
            "enum class CounterStepSymbol",
            "Canvas(",
            "drawLine(",
            "StrokeCap.Round",
            "CounterStepSymbol.Plus",
            "CounterStepSymbol.Minus",
            "CounterDimens.ExtraCounterStepperIconSize",
            "STEP_SYMBOL_LENGTH_FRACTION = 0.76f",
            "STEP_SYMBOL_STROKE_FRACTION = 0.14f",
            "COUNTER_STEPPER_DISABLED_ALPHA = 0.38f",
            "COUNTER_STEPPER_SUBDUED_ALPHA = 0.64f",
            "enabled: Boolean = true",
            "prominent: Boolean = true",
            "enabled = enabled",
            "indication = null",
            "interactionSource = null",
            "data class CounterStepButtonFaceAppearance",
            "CounterStepButtonFace(",
            "border(",
            "width = appearance.symbolSize * STEP_SYMBOL_STROKE_FRACTION",
            "shape = CircleShape",
        ).forEach { required ->
            assertTrue("CounterStepperButton should contain $required", stepperButton.contains(required))
        }

        listOf(
            "CounterRepeatButtonTouchSize = 48.dp",
            "CounterRepeatButtonVisualSize = 32.dp",
            "ExtraCounterStepperTouchSize = 56.dp",
            "ExtraCounterStepperVisualSize = 42.dp",
        ).forEach { required ->
            assertTrue("CounterDimens should contain $required", dimens.contains(required))
        }

        assertFalse(stepperButton.contains("androidx.compose.material3.Icon"))
        assertFalse(stepperButton.contains("imageVector ="))
        assertFalse(stepperButton.contains("ImageVector"))
        assertFalse(stepperButton.contains("Icons.Filled.Add"))
        assertFalse(stepperButton.contains("Icons.Filled.Remove"))

        assertUsesStepSymbols(stitchCounter)
        assertUsesStepSymbols(multiCounter)
        assertUsesStepSymbols(workspace)

        assertFalse(stitchCounter.contains("Icons.Filled.Add"))
        assertFalse(stitchCounter.contains("Icons.Filled.Remove"))
        assertFalse(multiCounter.contains("Icons.Filled.Add"))
        assertFalse(multiCounter.contains("Icons.Filled.Remove"))
        assertFalse(workspace.contains("icon = Icons.Filled.Add"))
        assertFalse(workspace.contains("icon = Icons.Filled.Remove"))
        assertFalse(workspace.contains("imageVector = icon"))
    }

    @Test
    fun `secondary step controls expose disabled and subdued states without shrinking touch targets`() {
        val stitchCounter = ProjectSourceFiles.read(STITCH_COUNTER)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val multiCounter = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(stitchCounter.contains("val canDecrement = currentStitch > 0"))
        assertTrue(stitchCounter.contains("val canIncrement = currentStitch < totalStitches"))
        assertTrue(stitchCounter.contains("enabled = canDecrement"))
        assertTrue(stitchCounter.contains("enabled = canIncrement"))

        assertTrue(workspace.contains("enabled = count > 0"))
        assertTrue(workspace.contains(".counterClickWithoutIndication(onClick = onClick, enabled = enabled)"))

        assertTrue(multiCounter.contains("val canDecrement = counter.count > 0"))
        assertTrue(multiCounter.contains("val incrementProminent = extraCounterIncrementIsProminent(counter)"))
        assertTrue(multiCounter.contains("enabled = canDecrement"))
        assertTrue(multiCounter.contains("prominent = incrementProminent"))
        assertTrue(multiCounter.contains("CounterValueDisplay.Cycle -> display.current < display.length"))
    }

    private fun assertUsesStepSymbols(source: String) {
        assertTrue(source.contains("symbol = CounterStepSymbol.Minus"))
        assertTrue(source.contains("symbol = CounterStepSymbol.Plus"))
    }

    private companion object {
        private const val COUNTER_STEPPER_BUTTON =
            "app/src/main/java/com/finnvek/knittools/ui/components/CounterStepperButton.kt"
        private const val STITCH_COUNTER =
            "app/src/main/java/com/finnvek/knittools/ui/components/StitchCounter.kt"
        private const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        private const val COUNTER_DIMENS =
            "app/src/main/java/com/finnvek/knittools/ui/theme/CounterDimens.kt"
    }
}
