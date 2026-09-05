package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.RowReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterValueFormatterTest {
    private fun counter(
        count: Int = 0,
        counterType: ProjectCounterType = ProjectCounterType.COUNT_UP,
        repeatAt: Int? = null,
        shapeEveryN: Int? = null,
        repeatStartRow: Int? = null,
        repeatEndRow: Int? = null,
        totalRepeats: Int? = null,
    ) = ProjectCounter(
        id = 1,
        projectId = 1,
        name = "Test",
        count = count,
        counterType = counterType,
        repeatAt = repeatAt,
        shapeEveryN = shapeEveryN,
        repeatStartRow = repeatStartRow,
        repeatEndRow = repeatEndRow,
        totalRepeats = totalRepeats,
    )

    private fun reminder(
        targetRow: Int = 4,
        repeatInterval: Int? = null,
    ) = RowReminder(
        id = 1,
        projectId = 1,
        targetRow = targetRow,
        repeatInterval = repeatInterval,
        message = "Reminder",
    )

    private fun mainProject(
        count: Int = 18,
        targetRows: Int? = 40,
        craftType: CraftType = CraftType.KNITTING,
        labelType: MainCounterLabelType = MainCounterLabelType.ROWS,
        customLabel: String? = null,
    ) = CounterProject(
        id = 1,
        name = "Test",
        count = count,
        targetRows = targetRows,
        craftType = craftType,
        mainCounterLabelType = labelType,
        mainCounterCustomLabel = customLabel,
    )

    @Test
    fun `main counter formatter returns all display slots for built in labels`() {
        listOf(
            MainCounterLabelType.ROWS,
            MainCounterLabelType.ROUNDS,
            MainCounterLabelType.REPEATS,
        ).forEach { labelType ->
            val display = CounterValueFormatter.forMainCounter(mainProject(labelType = labelType))
            val targetLine = checkNotNull(display.targetLine)

            assertEquals(labelType, display.heroTitle.labelType)
            assertEquals(18, display.heroTitle.count)
            assertNull(display.heroTitle.customLabel)
            assertEquals(labelType, targetLine.labelType)
            assertEquals(18, targetLine.count)
            assertEquals(40, targetLine.target)
            assertEquals(labelType, display.increaseContentDescription.labelType)
            assertEquals(labelType, display.decreaseContentDescription.labelType)
            assertEquals(labelType, display.projectCardCount.labelType)
        }
    }

    @Test
    fun `main counter formatter trims and limits custom label in every slot`() {
        val display =
            CounterValueFormatter.forMainCounter(
                mainProject(
                    labelType = MainCounterLabelType.CUSTOM,
                    customLabel = "  123456789012345678901234567890123456  ",
                ),
            )
        val expectedLabel = "12345678901234567890123456789012"
        val targetLine = checkNotNull(display.targetLine)

        assertEquals(MainCounterLabelType.CUSTOM, display.heroTitle.labelType)
        assertEquals(expectedLabel, display.heroTitle.customLabel)
        assertEquals(expectedLabel, targetLine.customLabel)
        assertEquals(expectedLabel, display.increaseContentDescription.customLabel)
        assertEquals(expectedLabel, display.decreaseContentDescription.customLabel)
        assertEquals(expectedLabel, display.projectCardCount.customLabel)
    }

    @Test
    fun `main counter formatter falls back from blank custom label to craft default in every slot`() {
        listOf(
            CraftType.KNITTING to MainCounterLabelType.ROWS,
            CraftType.CROCHET to MainCounterLabelType.ROUNDS,
        ).forEach { (craftType, expectedLabelType) ->
            val display =
                CounterValueFormatter.forMainCounter(
                    mainProject(
                        craftType = craftType,
                        labelType = MainCounterLabelType.CUSTOM,
                        customLabel = "   ",
                    ),
                )
            val targetLine = checkNotNull(display.targetLine)

            assertEquals(expectedLabelType, display.heroTitle.labelType)
            assertNull(display.heroTitle.customLabel)
            assertEquals(expectedLabelType, targetLine.labelType)
            assertNull(targetLine.customLabel)
            assertEquals(expectedLabelType, display.increaseContentDescription.labelType)
            assertNull(display.increaseContentDescription.customLabel)
            assertEquals(expectedLabelType, display.decreaseContentDescription.labelType)
            assertNull(display.decreaseContentDescription.customLabel)
            assertEquals(expectedLabelType, display.projectCardCount.labelType)
            assertNull(display.projectCardCount.customLabel)
        }
    }

    @Test
    fun `main counter formatter omits target slot when project has no target`() {
        val display = CounterValueFormatter.forMainCounter(mainProject(targetRows = null))

        assertNull(display.targetLine)
    }

    @Test
    fun `cyclePosition wraps below equal and above the cycle length`() {
        // length = 3
        assertEquals(0, CounterValueFormatter.cyclePosition(count = 0, length = 3))
        assertEquals(1, CounterValueFormatter.cyclePosition(count = 1, length = 3))
        assertEquals(3, CounterValueFormatter.cyclePosition(count = 3, length = 3))
        assertEquals(1, CounterValueFormatter.cyclePosition(count = 4, length = 3))
        assertEquals(3, CounterValueFormatter.cyclePosition(count = 6, length = 3))
        // length = 2
        assertEquals(1, CounterValueFormatter.cyclePosition(count = 1, length = 2))
        assertEquals(2, CounterValueFormatter.cyclePosition(count = 2, length = 2))
        assertEquals(1, CounterValueFormatter.cyclePosition(count = 3, length = 2))
    }

    @Test
    fun `cyclePosition returns raw count when length is not positive`() {
        assertEquals(7, CounterValueFormatter.cyclePosition(count = 7, length = 0))
        assertEquals(7, CounterValueFormatter.cyclePosition(count = 7, length = -1))
    }

    @Test
    fun `count up counter without repeat is plain`() {
        assertEquals(
            CounterValueDisplay.Plain(12),
            CounterValueFormatter.forExtraCounter(counter(count = 12)),
        )
    }

    @Test
    fun `repeating counter cycles within repeatAt`() {
        assertEquals(
            CounterValueDisplay.Cycle(current = 2, length = 3),
            CounterValueFormatter.forExtraCounter(
                counter(count = 2, counterType = ProjectCounterType.REPEATING, repeatAt = 3),
            ),
        )
        assertEquals(
            CounterValueDisplay.Cycle(current = 1, length = 3),
            CounterValueFormatter.forExtraCounter(
                counter(count = 4, counterType = ProjectCounterType.REPEATING, repeatAt = 3),
            ),
        )
    }

    @Test
    fun `shaping counter wraps total count into shapeEveryN cycle`() {
        assertEquals(
            CounterValueDisplay.Cycle(current = 3, length = 4),
            CounterValueFormatter.forExtraCounter(
                counter(count = 3, counterType = ProjectCounterType.SHAPING, shapeEveryN = 4),
            ),
        )
        assertEquals(
            CounterValueDisplay.Cycle(current = 4, length = 4),
            CounterValueFormatter.forExtraCounter(
                counter(count = 4, counterType = ProjectCounterType.SHAPING, shapeEveryN = 4),
            ),
        )
        assertEquals(
            CounterValueDisplay.Cycle(current = 3, length = 4),
            CounterValueFormatter.forExtraCounter(
                counter(count = 7, counterType = ProjectCounterType.SHAPING, shapeEveryN = 4),
            ),
        )
    }

    @Test
    fun `repeat section reports clamped progress while in range`() {
        val section =
            counter(
                counterType = ProjectCounterType.REPEAT_SECTION,
                repeatStartRow = 1,
                repeatEndRow = 2,
                totalRepeats = 3,
            )
        // rowRange = 2, total tracked rows = 6
        assertEquals(
            CounterValueDisplay.Section(repeat = 1, totalRepeats = 3, rowInRepeat = 1, rowsInRepeat = 2),
            CounterValueFormatter.forRepeatSection(section, mainRowCount = 1),
        )
        assertEquals(
            CounterValueDisplay.Section(repeat = 2, totalRepeats = 3, rowInRepeat = 1, rowsInRepeat = 2),
            CounterValueFormatter.forRepeatSection(section, mainRowCount = 3),
        )
    }

    @Test
    fun `repeat section before start clamps to first repeat`() {
        val section =
            counter(
                counterType = ProjectCounterType.REPEAT_SECTION,
                repeatStartRow = 5,
                repeatEndRow = 6,
                totalRepeats = 3,
            )
        assertEquals(
            CounterValueDisplay.Section(repeat = 1, totalRepeats = 3, rowInRepeat = 1, rowsInRepeat = 2),
            CounterValueFormatter.forRepeatSection(section, mainRowCount = 1),
        )
    }

    @Test
    fun `repeat section is complete at and past the final tracked row`() {
        val section =
            counter(
                counterType = ProjectCounterType.REPEAT_SECTION,
                repeatStartRow = 1,
                repeatEndRow = 2,
                totalRepeats = 3,
            )
        // final tracked row = 1 + (2 * 3) - 1 = 6
        assertEquals(CounterValueDisplay.SectionComplete, CounterValueFormatter.forRepeatSection(section, 6))
        assertEquals(CounterValueDisplay.SectionComplete, CounterValueFormatter.forRepeatSection(section, 10))
    }

    @Test
    fun `repeat section without configuration is plain`() {
        assertEquals(
            CounterValueDisplay.Plain(0),
            CounterValueFormatter.forRepeatSection(
                counter(counterType = ProjectCounterType.REPEAT_SECTION),
                mainRowCount = 5,
            ),
        )
    }

    @Test
    fun `invalid repeat section bounds use the plain counter value`() {
        val zeroRepeats =
            counter(
                counterType = ProjectCounterType.REPEAT_SECTION,
                repeatStartRow = 2,
                repeatEndRow = 4,
                totalRepeats = 0,
            )
        val invertedRows =
            counter(
                counterType = ProjectCounterType.REPEAT_SECTION,
                repeatStartRow = 4,
                repeatEndRow = 2,
                totalRepeats = 2,
            )

        assertEquals(
            CounterValueDisplay.Plain(zeroRepeats.count),
            CounterValueFormatter.forRepeatSection(zeroRepeats, 2),
        )
        assertEquals(
            CounterValueDisplay.Plain(invertedRows.count),
            CounterValueFormatter.forRepeatSection(invertedRows, 2),
        )
    }

    @Test
    fun `reminder repeat reports occurrence and interval`() {
        // targetRow = 4, interval = 3, currentRow = 13 -> (13 - 4) / 3 + 1 = 4
        assertEquals(
            CounterValueDisplay.ReminderRepeat(occurrence = 4, intervalRows = 3),
            CounterValueFormatter.forReminderRepeat(reminder(targetRow = 4, repeatInterval = 3), currentRow = 13),
        )
    }

    @Test
    fun `non repeating reminder has no repeat display`() {
        assertNull(
            CounterValueFormatter.forReminderRepeat(
                reminder(targetRow = 4, repeatInterval = null),
                currentRow = 13,
            ),
        )
        assertNull(
            CounterValueFormatter.forReminderRepeat(
                reminder(targetRow = 4, repeatInterval = 0),
                currentRow = 13,
            ),
        )
    }
}
