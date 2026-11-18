package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.YarnEstimate
import java.math.RoundingMode

object YarnEstimator {
    fun estimate(
        totalYarnNeeded: Double,
        yarnPerSkein: Double,
        weightPerSkein: Double,
    ): YarnEstimate? {
        if (!totalYarnNeeded.isFinite() || totalYarnNeeded <= 0.0) return null
        if (!yarnPerSkein.isFinite() || yarnPerSkein <= 0.0) return null
        if (!weightPerSkein.isFinite() || weightPerSkein <= 0.0) return null

        val exactSkeins = totalYarnNeeded / yarnPerSkein
        if (!exactSkeins.isFinite() || exactSkeins <= 0.0) return null
        val roundedSkeins = totalYarnNeeded.toBigDecimal().divide(yarnPerSkein.toBigDecimal(), 0, RoundingMode.CEILING)
        if (roundedSkeins > Int.MAX_VALUE.toBigDecimal()) return null
        val skeinsNeeded = roundedSkeins.toInt()
        val totalWeight = skeinsNeeded * weightPerSkein
        if (!totalWeight.isFinite()) return null

        return YarnEstimate(
            skeinsNeeded = skeinsNeeded,
            totalWeight = totalWeight,
            exactSkeins = exactSkeins,
        )
    }
}
