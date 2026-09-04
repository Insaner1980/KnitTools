package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.YarnEstimate
import kotlin.math.ceil

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
        val roundedSkeins = ceil(exactSkeins)
        if (!roundedSkeins.isFinite() || roundedSkeins > Int.MAX_VALUE.toDouble()) return null
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
