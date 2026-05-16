package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.IncreaseDecreaseMessage
import com.finnvek.knittools.domain.model.IncreaseDecreaseMode
import com.finnvek.knittools.domain.model.IncreaseDecreaseResult
import com.finnvek.knittools.domain.model.KnittingStyle
import kotlin.math.ceil

object IncreaseDecreaseCalculator {
    fun calculate(
        currentStitches: Int,
        changeBy: Int,
        mode: IncreaseDecreaseMode,
        style: KnittingStyle = KnittingStyle.FLAT,
    ): IncreaseDecreaseResult {
        if (currentStitches <= 0) {
            return errorResult(IncreaseDecreaseMessage.CurrentStitchesMustBePositive)
        }
        if (changeBy <= 0) {
            return errorResult(IncreaseDecreaseMessage.ChangeMustBePositive)
        }
        if (mode == IncreaseDecreaseMode.DECREASE && changeBy >= currentStitches) {
            return errorResult(IncreaseDecreaseMessage.CannotDecreaseBy(changeBy, currentStitches))
        }
        val requiredDecreaseStitches = changeBy.toLong() * 2
        if (mode == IncreaseDecreaseMode.DECREASE && requiredDecreaseStitches > currentStitches.toLong()) {
            return errorResult(
                IncreaseDecreaseMessage.NotEnoughStitchesForDecrease(
                    changeBy = changeBy,
                    requiredStitches = requiredDecreaseStitches,
                ),
            )
        }

        val totalStitchesLong =
            when (mode) {
                IncreaseDecreaseMode.INCREASE -> currentStitches.toLong() + changeBy.toLong()
                IncreaseDecreaseMode.DECREASE -> currentStitches.toLong() - changeBy.toLong()
            }
        if (totalStitchesLong !in 1L..Int.MAX_VALUE.toLong()) {
            return errorResult(IncreaseDecreaseMessage.TotalStitchesOutOfRange)
        }
        val totalStitches = totalStitchesLong.toInt()

        val availableForKnitLong =
            when (mode) {
                IncreaseDecreaseMode.INCREASE -> currentStitches.toLong()
                IncreaseDecreaseMode.DECREASE -> currentStitches.toLong() - requiredDecreaseStitches
            }

        // For increase: M1 creates a stitch from nothing, so K stitches sum to currentStitches
        // For decrease: K2tog consumes 2 stitches, so plain K stitches = currentStitches - 2*changeBy
        val availableForKnit = availableForKnitLong.toInt()

        val warningMessage =
            if (mode == IncreaseDecreaseMode.INCREASE && changeBy > currentStitches) {
                IncreaseDecreaseMessage.IncreaseMoreThanCurrent
            } else {
                null
            }

        val easyPattern = buildEasyPattern(availableForKnit, changeBy, mode, style, totalStitches)
        val balancedPattern = buildBalancedPattern(availableForKnit, changeBy, mode, style, totalStitches)

        return IncreaseDecreaseResult(
            totalStitches = totalStitches,
            easyPattern = easyPattern,
            balancedPattern = balancedPattern,
            isValid = true,
            message = warningMessage,
        )
    }

    private fun buildEasyPattern(
        availableForKnit: Int,
        sections: Int,
        mode: IncreaseDecreaseMode,
        style: KnittingStyle,
        totalStitches: Int,
    ): String {
        val actionAbbrev =
            when (mode) {
                IncreaseDecreaseMode.INCREASE -> "M1"
                IncreaseDecreaseMode.DECREASE -> "K2tog"
            }

        val base = availableForKnit / sections
        val remainder = availableForKnit % sections

        return when (style) {
            KnittingStyle.FLAT -> buildFlatEasy(base, sections, remainder, actionAbbrev, totalStitches)
            KnittingStyle.CIRCULAR -> buildCircularEasy(base, sections, remainder, actionAbbrev, totalStitches)
        }
    }

    private fun buildFlatEasy(
        base: Int,
        sections: Int,
        remainder: Int,
        actionAbbrev: String,
        totalStitches: Int,
    ): String {
        if (remainder == 0) {
            return "${formatRepeatedActionGroup(base, actionAbbrev, sections)} — total: $totalStitches stitches"
        }
        val half = remainder / 2
        val parts = mutableListOf<String>()
        if (half > 0) parts.add("K$half")
        parts.add(formatRepeatedActionGroup(base, actionAbbrev, sections))
        val trailing = remainder - half
        if (trailing > 0) parts.add("K$trailing")
        return "${parts.joinToString(", ")} — total: $totalStitches stitches"
    }

    private fun buildCircularEasy(
        base: Int,
        sections: Int,
        remainder: Int,
        actionAbbrev: String,
        totalStitches: Int,
    ): String {
        if (remainder == 0) {
            return "${formatRepeatedActionGroup(base, actionAbbrev, sections)} — total: $totalStitches stitches"
        }
        val mainSections = sections - remainder
        val biggerSections = remainder
        val parts = mutableListOf<String>()
        if (mainSections > 0) {
            parts.add(formatRepeatedActionGroup(base, actionAbbrev, mainSections))
        }
        parts.add(formatRepeatedActionGroup(base + 1, actionAbbrev, biggerSections))
        return "${parts.joinToString(", ")} — total: $totalStitches stitches"
    }

    private fun buildBalancedPattern(
        availableForKnit: Int,
        sections: Int,
        mode: IncreaseDecreaseMode,
        style: KnittingStyle,
        totalStitches: Int,
    ): String =
        when (style) {
            KnittingStyle.FLAT -> buildFlatBalancedPattern(availableForKnit, sections, mode, totalStitches)
            KnittingStyle.CIRCULAR -> buildCircularBalancedPattern(availableForKnit, sections, mode, totalStitches)
        }

    private fun buildFlatBalancedPattern(
        availableForKnit: Int,
        sections: Int,
        mode: IncreaseDecreaseMode,
        totalStitches: Int,
    ): String {
        val gaps = sections + 1
        val trailingKnit = availableForKnit / gaps + if (availableForKnit % gaps > 0) 1 else 0
        val actionKnit = availableForKnit - trailingKnit
        val parts = buildBalancedActionGroups(actionKnit, sections, mode).toMutableList()
        if (trailingKnit > 0) {
            parts.add("K$trailingKnit")
        }
        return "${parts.joinToString(", ")} — total: $totalStitches stitches"
    }

    private fun buildCircularBalancedPattern(
        availableForKnit: Int,
        sections: Int,
        mode: IncreaseDecreaseMode,
        totalStitches: Int,
    ): String {
        val groups =
            buildBalancedActionGroups(
                availableForKnit,
                sections,
                mode,
            ).joinToString(", ")
        return "$groups — total: $totalStitches stitches"
    }

    private fun buildBalancedActionGroups(
        availableForKnit: Int,
        sections: Int,
        mode: IncreaseDecreaseMode,
    ): List<String> {
        val actionAbbrev =
            when (mode) {
                IncreaseDecreaseMode.INCREASE -> "M1"
                IncreaseDecreaseMode.DECREASE -> "K2tog"
            }

        val base = availableForKnit / sections
        val remainder = availableForKnit % sections

        if (remainder == 0) {
            return listOf(formatRepeatedActionGroup(base, actionAbbrev, sections))
        }

        val bigCount = remainder
        val smallCount = sections - remainder
        val bigSize = base + 1

        val halfBig = ceil(bigCount / 2.0).toInt()
        val halfSmall = ceil(smallCount / 2.0).toInt()
        val restBig = bigCount - halfBig
        val restSmall = smallCount - halfSmall

        val parts = mutableListOf<String>()

        fun addGroup(
            size: Int,
            count: Int,
        ) {
            if (count <= 0) return
            parts.add(formatRepeatedActionGroup(size, actionAbbrev, count))
        }

        addGroup(bigSize, halfBig)
        addGroup(base, halfSmall)
        addGroup(bigSize, restBig)
        addGroup(base, restSmall)

        return parts
    }

    private fun formatActionGroup(
        knitStitches: Int,
        actionAbbrev: String,
    ): String =
        if (knitStitches == 0) {
            actionAbbrev
        } else {
            "K$knitStitches, $actionAbbrev"
        }

    private fun formatRepeatedActionGroup(
        knitStitches: Int,
        actionAbbrev: String,
        count: Int,
    ): String {
        val group = formatActionGroup(knitStitches, actionAbbrev)
        return if (count == 1) {
            group
        } else if (knitStitches == 0) {
            "$group × $count"
        } else {
            "($group) × $count"
        }
    }

    private fun errorResult(message: IncreaseDecreaseMessage) =
        IncreaseDecreaseResult(
            totalStitches = 0,
            easyPattern = "",
            balancedPattern = "",
            isValid = false,
            message = message,
        )
}
