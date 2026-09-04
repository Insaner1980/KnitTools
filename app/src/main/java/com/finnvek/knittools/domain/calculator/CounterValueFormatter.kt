package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.resolvedMainCounterLabelType
import com.finnvek.knittools.domain.model.sanitizeMainCounterCustomLabel

/**
 * Yhtenäinen näyttömalli laskuriarvoille. Compose-kerros kääntää tämän
 * merkkijonoresursseiksi, jotta puhdas logiikka pysyy testattavana eikä
 * sisällä kovakoodattuja tekstejä.
 */
sealed interface CounterValueDisplay {
    /** Pelkkä luku ilman nimittäjää (COUNT_UP-lisälaskuri). */
    data class Plain(
        val count: Int,
    ) : CounterValueDisplay

    /** Syklinen eteneminen jakson sisällä (REPEATING- ja SHAPING-lisälaskurit). */
    data class Cycle(
        val current: Int,
        val length: Int,
    ) : CounterValueDisplay

    /** Toistojakson eteneminen: monesko toisto ja rivi toiston sisällä. */
    data class Section(
        val repeat: Int,
        val totalRepeats: Int,
        val rowInRepeat: Int,
        val rowsInRepeat: Int,
    ) : CounterValueDisplay

    /** Toistojakso suoritettu loppuun. */
    data object SectionComplete : CounterValueDisplay

    /** Toistuva muistutus: monesko esiintymä ja toistoväli riveinä. */
    data class ReminderRepeat(
        val occurrence: Int,
        val intervalRows: Int,
    ) : CounterValueDisplay
}

data class MainCounterValueDisplay(
    val heroTitle: MainCounterCountSlot,
    val targetLine: MainCounterTargetSlot?,
    val increaseContentDescription: MainCounterLabelSlot,
    val decreaseContentDescription: MainCounterLabelSlot,
    val projectCardCount: MainCounterCountSlot,
)

data class MainCounterLabelSlot(
    val labelType: MainCounterLabelType,
    val customLabel: String? = null,
)

data class MainCounterCountSlot(
    val count: Int,
    val labelType: MainCounterLabelType,
    val customLabel: String? = null,
)

data class MainCounterTargetSlot(
    val count: Int,
    val target: Int,
    val labelType: MainCounterLabelType,
    val customLabel: String? = null,
)

/**
 * Yksi totuuden lähde laskuriarvojen näyttömuodon laskennalle. Korvaa aiemmin
 * hajallaan olleet näyttölogiikat (lisälaskurit, toistojaksot, muistutukset).
 */
object CounterValueFormatter {
    fun forMainCounter(project: CounterProject): MainCounterValueDisplay {
        val customLabel = sanitizeMainCounterCustomLabel(project.mainCounterCustomLabel)
        val labelType =
            resolvedMainCounterLabelType(
                craftType = project.craftType,
                labelType = project.mainCounterLabelType,
                customLabel = customLabel,
            )
        val resolvedCustomLabel = customLabel.takeIf { labelType == MainCounterLabelType.CUSTOM }
        val target = project.targetRows?.takeIf { it > 0 }
        val labelSlot = MainCounterLabelSlot(labelType = labelType, customLabel = resolvedCustomLabel)

        return MainCounterValueDisplay(
            heroTitle =
                MainCounterCountSlot(
                    count = project.count,
                    labelType = labelType,
                    customLabel = resolvedCustomLabel,
                ),
            targetLine =
                target?.let {
                    MainCounterTargetSlot(
                        count = project.count,
                        target = it,
                        labelType = labelType,
                        customLabel = resolvedCustomLabel,
                    )
                },
            increaseContentDescription = labelSlot,
            decreaseContentDescription = labelSlot,
            projectCardCount =
                MainCounterCountSlot(
                    count = project.count,
                    labelType = labelType,
                    customLabel = resolvedCustomLabel,
                ),
        )
    }

    /**
     * Syklinen sijainti jakson sisällä: 0 kun count on 0, muuten 1..length
     * (esim. length = 3 tuottaa sarjan 1, 2, 3, 1, 2, 3 ...).
     */
    fun cyclePosition(
        count: Int,
        length: Int,
    ): Int {
        if (length <= 0) return count
        if (count <= 0) return 0
        val remainder = count % length
        return if (remainder == 0) length else remainder
    }

    /** Näyttömalli lisälaskurille (COUNT_UP, REPEATING tai SHAPING). */
    fun forExtraCounter(counter: ProjectCounter): CounterValueDisplay {
        val length =
            when (counter.counterType) {
                ProjectCounterType.SHAPING -> counter.shapeEveryN
                ProjectCounterType.REPEATING -> counter.repeatAt
                else -> null
            }?.takeIf { it > 0 }
        return if (length == null) {
            CounterValueDisplay.Plain(counter.count)
        } else {
            CounterValueDisplay.Cycle(
                current = cyclePosition(counter.count, length),
                length = length,
            )
        }
    }

    /** Näyttömalli toistojaksolle suhteessa päälaskurin riviin. */
    fun forRepeatSection(
        counter: ProjectCounter,
        mainRowCount: Int,
    ): CounterValueDisplay {
        val startRow = counter.repeatStartRow
        val endRow = counter.repeatEndRow
        val totalRepeats = counter.totalRepeats
        if (counter.counterType != ProjectCounterType.REPEAT_SECTION ||
            startRow == null ||
            endRow == null ||
            totalRepeats == null
        ) {
            return CounterValueDisplay.Plain(counter.count)
        }
        if (RepeatSectionLogic.isComplete(counter, mainRowCount)) {
            return CounterValueDisplay.SectionComplete
        }
        val repeat =
            RepeatSectionLogic
                .updatePosition(counter, mainRowCount)
                .currentRepeat
                ?.coerceIn(1, totalRepeats) ?: 1
        return CounterValueDisplay.Section(
            repeat = repeat,
            totalRepeats = totalRepeats,
            rowInRepeat = RepeatSectionLogic.currentRowInRepeat(counter, mainRowCount),
            rowsInRepeat = endRow - startRow + 1,
        )
    }

    /**
     * Näyttömalli toistuvalle muistutukselle: monesko esiintymä ja toistoväli.
     * Palauttaa null, jos muistutus ei ole toistuva.
     */
    fun forReminderRepeat(
        reminder: RowReminder,
        currentRow: Int,
    ): CounterValueDisplay.ReminderRepeat? {
        val interval = reminder.repeatInterval?.takeIf { it > 0 } ?: return null
        return CounterValueDisplay.ReminderRepeat(
            occurrence = ReminderLogic.repeatCount(reminder, currentRow),
            intervalRows = interval,
        )
    }
}
