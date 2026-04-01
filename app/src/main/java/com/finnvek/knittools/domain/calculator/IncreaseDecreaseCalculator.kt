package com.finnvek.knittools.domain.calculator

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
            return errorResult("Current stitches must be greater than 0")
        }
        if (changeBy <= 0) {
            return errorResult("Number of stitches to change must be greater than 0")
        }
        if (mode == IncreaseDecreaseMode.DECREASE && changeBy >= currentStitches) {
            return errorResult("Cannot decrease by $changeBy — only $currentStitches stitches available")
        }
        if (mode == IncreaseDecreaseMode.DECREASE && changeBy * 2 >= currentStitches) {
            return errorResult("Not enough stitches — need at least ${changeBy * 2 + 1} for $changeBy decreases")
        }

        val totalStitches =
            when (mode) {
                IncreaseDecreaseMode.INCREASE -> currentStitches + changeBy
                IncreaseDecreaseMode.DECREASE -> currentStitches - changeBy
            }

        // For increase: M1 creates a stitch from nothing, so K stitches sum to currentStitches
        // For decrease: K2tog consumes 2 stitches, so plain K stitches = currentStitches - 2*changeBy
        val availableForKnit =
            when (mode) {
                IncreaseDecreaseMode.INCREASE -> currentStitches
                IncreaseDecreaseMode.DECREASE -> currentStitches - 2 * changeBy
            }

        val warningMessage =
            if (mode == IncreaseDecreaseMode.INCREASE && changeBy > currentStitches) {
                "Warning: increasing more than current stitch count — some sections will have no knit stitches between increases"
            } else {
                null
            }

        val easyPattern = buildEasyPattern(availableForKnit, changeBy, mode, style, totalStitches)
        val balancedPattern = buildBalancedPattern(availableForKnit, changeBy, mode, totalStitches)

        return IncreaseDecreaseResult(
            totalStitches = totalStitches,
            easyPattern = easyPattern,
            balancedPattern = balancedPattern,
            isValid = true,
            errorMessage = warningMessage,
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
            return "(K$base, $actionAbbrev) × $sections — total: $totalStitches stitches"
        }
        val half = remainder / 2
        val parts = mutableListOf<String>()
        if (half > 0) parts.add("K$half")
        parts.add("(K$base, $actionAbbrev) × $sections")
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
            return "(K$base, $actionAbbrev) × $sections — total: $totalStitches stitches"
        }
        val mainSections = sections - remainder
        val biggerSections = remainder
        val parts = mutableListOf<String>()
        if (mainSections > 0) {
            parts.add("(K$base, $actionAbbrev) × $mainSections")
        }
        parts.add("(K${base + 1}, $actionAbbrev) × $biggerSections")
        return "${parts.joinToString(", ")} — total: $totalStitches stitches"
    }

    private fun buildBalancedPattern(
        availableForKnit: Int,
        sections: Int,
        mode: IncreaseDecreaseMode,
        totalStitches: Int,
    ): String {
        val actionAbbrev =
            when (mode) {
                IncreaseDecreaseMode.INCREASE -> "M1"
                IncreaseDecreaseMode.DECREASE -> "K2tog"
            }

        val base = availableForKnit / sections
        val remainder = availableForKnit % sections

        if (remainder == 0) {
            return "(K$base, $actionAbbrev) × $sections — total: $totalStitches stitches"
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
            if (count == 1) {
                parts.add("K$size, $actionAbbrev")
            } else {
                parts.add("(K$size, $actionAbbrev) × $count")
            }
        }

        addGroup(bigSize, halfBig)
        addGroup(base, halfSmall)
        addGroup(bigSize, restBig)
        addGroup(base, restSmall)

        return "${parts.joinToString(", ")} — total: $totalStitches stitches"
    }

    private fun errorResult(message: String) =
        IncreaseDecreaseResult(
            totalStitches = 0,
            easyPattern = "",
            balancedPattern = "",
            isValid = false,
            errorMessage = message,
        )
}
