package com.finnvek.knittools.domain.model

data class YarnUsageAmounts(
    val plannedMeters: Double? = null,
    val allocatedMeters: Double? = null,
    val usedMeters: Double? = null,
    val metersPerSkein: Double? = null,
    val gramsPerSkein: Double? = null,
)

enum class YarnUsageUnit { METERS, YARDS, GRAMS, SKEINS }

data class YarnUsageSource(
    val yarnCardId: Long? = null,
    val projectYarnNoteId: Long? = null,
)

data class ProjectYarnUsage(
    val id: Long,
    val projectId: Long,
    val source: YarnUsageSource,
    val sourceNameSnapshot: String,
    val amounts: YarnUsageAmounts,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class YarnUsageSourceStatus { AVAILABLE, UNLINKED, UNAVAILABLE }

data class ProjectYarnUsageItem(
    val source: YarnUsageSource,
    val name: String,
    val usage: ProjectYarnUsage? = null,
    val status: YarnUsageSourceStatus = YarnUsageSourceStatus.AVAILABLE,
) {
    val key: String
        get() =
            source.projectYarnNoteId?.let { "note:$it" }
                ?: source.yarnCardId?.let { "card:$it" }
                ?: "usage:${usage?.id}"
}
