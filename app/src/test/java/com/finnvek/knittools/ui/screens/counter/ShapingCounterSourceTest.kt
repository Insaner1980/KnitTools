package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.domain.calculator.CounterValueDisplay
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapingCounterSourceTest {
    @Test
    fun `stitch change field accepts signed numeric input`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)
        val stitchChangeField =
            Regex(
                """NumberInputField\(\s*value = stitchChangeText,.*?label = stringResource\(R\.string\.stitch_change\),.*?allowNegative = true""",
                RegexOption.DOT_MATCHES_ALL,
            )

        assertTrue(stitchChangeField.containsMatchIn(source))
    }

    @Test
    fun `repeat section validation accepts single-row repeat sections`() {
        assertTrue(
            isAddCounterFormValidByReflection(
                AddCounterFormParams(
                    name = "Sleeve repeat",
                    stepSize = 1,
                    isRepeating = false,
                    repeatAt = null,
                    isShaping = false,
                    startingStitches = null,
                    stitchChange = null,
                    shapeEveryN = null,
                    isRepeatSection = true,
                    repeatStartRow = 12,
                    repeatEndRow = 12,
                    totalRepeats = 4,
                ),
            ),
        )
    }

    @Test
    fun `domain repeating increment preserves overflow with modulo`() {
        val source = ProjectSourceFiles.read(PROJECT_COUNTER_LOGIC)

        assertTrue(source.contains("newCount % repeatAt"))
    }

    @Test
    fun `shaping counter uses compact progress formatting instead of next row copy`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertFalse(source.contains("R.string.next_shaping_counter_format"))
        assertFalse(source.contains("ShapingCounterLogic.nextShapingRow"))
        assertTrue(source.contains("CounterValueFormatter.forExtraCounter(counter)"))
        assertTrue(source.contains("R.string.repeating_counter_value_format"))
    }

    @Test
    fun `shaping display wraps total count into interval progress`() {
        assertEquals(
            CounterValueDisplay.Cycle(current = 3, length = 4),
            CounterValueFormatter.forExtraCounter(shapingCounter(count = 3)),
        )
        assertEquals(
            CounterValueDisplay.Cycle(current = 4, length = 4),
            CounterValueFormatter.forExtraCounter(shapingCounter(count = 4)),
        )
        assertEquals(
            CounterValueDisplay.Cycle(current = 3, length = 4),
            CounterValueFormatter.forExtraCounter(shapingCounter(count = 7)),
        )
    }

    @Test
    fun `extra counter names use ellipsis instead of expanding layout`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(source.contains("TextOverflow.Ellipsis"))
        assertTrue(source.contains("maxLines = 1"))
    }

    private fun isAddCounterFormValidByReflection(params: AddCounterFormParams): Boolean {
        val method =
            Class
                .forName("com.finnvek.knittools.ui.screens.counter.MultiCounterComponentsKt")
                .getDeclaredMethod("isAddCounterFormValid", AddCounterFormParams::class.java)
        method.isAccessible = true
        return method.invoke(null, params) as Boolean
    }

    private fun shapingCounter(count: Int): ProjectCounter =
        ProjectCounter(
            id = 1,
            projectId = 1,
            name = "Gusset decreases",
            count = count,
            counterType = ProjectCounterType.SHAPING,
            shapeEveryN = 4,
        )

    private companion object {
        const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        const val PROJECT_COUNTER_LOGIC =
            "app/src/main/java/com/finnvek/knittools/domain/calculator/ProjectCounterLogic.kt"
    }
}
