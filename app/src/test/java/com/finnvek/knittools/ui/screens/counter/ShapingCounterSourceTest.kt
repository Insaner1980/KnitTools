package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
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

    private fun isAddCounterFormValidByReflection(params: AddCounterFormParams): Boolean {
        val method =
            Class
                .forName("com.finnvek.knittools.ui.screens.counter.MultiCounterComponentsKt")
                .getDeclaredMethod("isAddCounterFormValid", AddCounterFormParams::class.java)
        method.isAccessible = true
        return method.invoke(null, params) as Boolean
    }

    private companion object {
        const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        const val PROJECT_COUNTER_LOGIC =
            "app/src/main/java/com/finnvek/knittools/domain/calculator/ProjectCounterLogic.kt"
    }
}
