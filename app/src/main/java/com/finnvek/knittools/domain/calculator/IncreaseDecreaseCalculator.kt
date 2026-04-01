package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.IncreaseDecreaseMode
import com.finnvek.knittools.domain.model.IncreaseDecreaseResult
import com.finnvek.knittools.domain.model.KnittingStyle
import kotlin.math.ceil
import kotlin.math.floor

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

        val totalStitches = when (mode) {
            IncreaseDecreaseMode.INCREASE -> currentStitches + changeBy
            IncreaseDecreaseMode.DECREASE -> currentStitches - changeBy
        }

        val easyPattern = buildEasyPattern(currentStitches, changeBy, mode, style)
        val balancedPattern = buildBalancedPattern(currentStitches, changeBy, mode, style)

        return IncreaseDecreaseResult(
            totalStitches = totalStitches,
            easyPattern = easyPattern,
            balancedPattern = balancedPattern,
            isValid = true,
        )
    }

    private fun buildEasyPattern(
        currentStitches: Int,
        changeBy: Int,
        mode: IncreaseDecreaseMode,
        style: KnittingStyle,
    ): String {
        val sections = changeBy
        val stitchesPerSection = currentStitches / sections
        val remainder = currentStitches % sections

        val actionAbbrev = when (mode) {
            IncreaseDecreaseMode.INCREASE -> "M1"
            IncreaseDecreaseMode.DECREASE -> "K2tog"
        }
        val knitAbbrev = "K"

        return when (style) {
            KnittingStyle.FLAT -> buildFlatEasy(stitchesPerSection, sections, remainder, knitAbbrev, actionAbbrev, mode, currentStitches, changeBy)
            KnittingStyle.CIRCULAR -> buildCircularEasy(stitchesPerSection, sections, remainder, knitAbbrev, actionAbbrev, mode, currentStitches, changeBy)
        }
    }

    private fun buildFlatEasy(
        stitchesPerSection: Int,
        sections: Int,
        remainder: Int,
        knitAbbrev: String,
        actionAbbrev: String,
        mode: IncreaseDecreaseMode,
        currentStitches: Int,
        changeBy: Int,
    ): String {
        val totalStitches = when (mode) {
            IncreaseDecreaseMode.INCREASE -> currentStitches + changeBy
            IncreaseDecreaseMode.DECREASE -> currentStitches - changeBy
        }

        return if (remainder == 0) {
            "($knitAbbrev$stitchesPerSection, $actionAbbrev) × $sections — total: $totalStitches stitches"
        } else {
            val half = remainder / 2
            val parts = mutableListOf<String>()
            if (half > 0) parts.add("$knitAbbrev$half")
            parts.add("($knitAbbrev$stitchesPerSection, $actionAbbrev) × $sections")
            val trailing = remainder - half
            if (trailing > 0) parts.add("$knitAbbrev$trailing")
            "${parts.joinToString(", ")} — total: $totalStitches stitches"
        }
    }

    private fun buildCircularEasy(
        stitchesPerSection: Int,
        sections: Int,
        remainder: Int,
        knitAbbrev: String,
        actionAbbrev: String,
        mode: IncreaseDecreaseMode,
        currentStitches: Int,
        changeBy: Int,
    ): String {
        val totalStitches = when (mode) {
            IncreaseDecreaseMode.INCREASE -> currentStitches + changeBy
            IncreaseDecreaseMode.DECREASE -> currentStitches - changeBy
        }

        return if (remainder == 0) {
            "($knitAbbrev$stitchesPerSection, $actionAbbrev) × $sections — total: $totalStitches stitches"
        } else {
            val mainSections = sections - remainder
            val biggerSections = remainder
            val parts = mutableListOf<String>()
            if (mainSections > 0) {
                parts.add("($knitAbbrev$stitchesPerSection, $actionAbbrev) × $mainSections")
            }
            parts.add("($knitAbbrev${stitchesPerSection + 1}, $actionAbbrev) × $biggerSections")
            "${parts.joinToString(", ")} — total: $totalStitches stitches"
        }
    }

    private fun buildBalancedPattern(
        currentStitches: Int,
        changeBy: Int,
        mode: IncreaseDecreaseMode,
        style: KnittingStyle,
    ): String {
        val totalStitches = when (mode) {
            IncreaseDecreaseMode.INCREASE -> currentStitches + changeBy
            IncreaseDecreaseMode.DECREASE -> currentStitches - changeBy
        }

        val sections = changeBy
        val base = currentStitches / sections
        val remainder = currentStitches % sections

        val actionAbbrev = when (mode) {
            IncreaseDecreaseMode.INCREASE -> "M1"
            IncreaseDecreaseMode.DECREASE -> "K2tog"
        }
        val knitAbbrev = "K"

        if (remainder == 0) {
            return "($knitAbbrev$base, $actionAbbrev) × $sections — total: $totalStitches stitches"
        }

        val bigCount = remainder
        val smallCount = sections - remainder
        val bigSize = base + 1

        val halfBig = ceil(bigCount / 2.0).toInt()
        val halfSmall = ceil(smallCount / 2.0).toInt()
        val restBig = bigCount - halfBig
        val restSmall = smallCount - halfSmall

        val parts = mutableListOf<String>()

        fun addGroup(size: Int, count: Int) {
            if (count <= 0) return
            if (count == 1) {
                parts.add("$knitAbbrev$size, $actionAbbrev")
            } else {
                parts.add("($knitAbbrev$size, $actionAbbrev) × $count")
            }
        }

        if (style == KnittingStyle.FLAT) {
            val startPad = floor(remainder / 2.0).toInt()
            if (startPad > 0) parts.add("$knitAbbrev$startPad")
        }

        addGroup(bigSize, halfBig)
        addGroup(base, halfSmall)
        addGroup(bigSize, restBig)
        addGroup(base, restSmall)

        return "${parts.joinToString(", ")} — total: $totalStitches stitches"
    }

    private fun errorResult(message: String) = IncreaseDecreaseResult(
        totalStitches = 0,
        easyPattern = "",
        balancedPattern = "",
        isValid = false,
        errorMessage = message,
    )
}
