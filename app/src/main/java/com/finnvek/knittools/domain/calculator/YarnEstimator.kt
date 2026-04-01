package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.YarnEstimate
import kotlin.math.ceil

object YarnEstimator {
    fun estimate(
        totalYarnNeeded: Double,
        yarnPerSkein: Double,
        weightPerSkein: Double,
    ): YarnEstimate {
        if (yarnPerSkein <= 0) {
            return YarnEstimate(skeinsNeeded = 0, totalWeight = 0.0, exactSkeins = 0.0)
        }

        val exactSkeins = totalYarnNeeded / yarnPerSkein
        val skeinsNeeded = ceil(exactSkeins).toInt()
        val totalWeight = skeinsNeeded * weightPerSkein

        return YarnEstimate(
            skeinsNeeded = skeinsNeeded,
            totalWeight = totalWeight,
            exactSkeins = exactSkeins,
        )
    }
}
