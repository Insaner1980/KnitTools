package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.ProjectYarnUsageRelations
import com.finnvek.knittools.data.local.ResolvedProjectYarnUsage
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.domain.model.YarnUsageSourceStatus

internal fun ProjectYarnUsageRelations.items(): List<ProjectYarnUsageItem> {
    val result = mutableListOf<ProjectYarnUsageItem>()
    val matchedUsages = mutableSetOf<Long>()
    val pairedCards = notes.mapNotNull { it.card?.id }.toSet()
    notes
        .sortedWith(
            compareByDescending<com.finnvek.knittools.data.local.ResolvedUsageNote> {
                it.note.createdAt
            }.thenByDescending { it.note.id },
        ).forEach { resolved ->
            val source = YarnUsageSource(resolved.card?.id, resolved.note.id)
            val usage = usages.firstOrNull { it.usage.matches(source) }?.usage?.toDomain()
            usage?.let { matchedUsages.add(it.id) }
            result.add(ProjectYarnUsageItem(source, resolved.note.name, usage))
        }
    cards.filterNot { it.id in pairedCards }.sortedByDescending { it.createdAt }.forEach { card ->
        val source = YarnUsageSource(yarnCardId = card.id)
        val usage = usages.firstOrNull { it.usage.matches(source) }?.usage?.toDomain()
        usage?.let { matchedUsages.add(it.id) }
        result.add(ProjectYarnUsageItem(source, card.usageName(), usage))
    }
    usages.filterNot { it.usage.id in matchedUsages }.sortedBy { it.usage.id }.forEach { resolved ->
        result.add(resolved.orphanItem())
    }
    return result
}

internal fun YarnCardEntity.usageName(): String = listOf(brand, yarnName).filter { it.isNotBlank() }.joinToString(" ")

private fun com.finnvek.knittools.data.local.ProjectYarnUsageEntity.matches(source: YarnUsageSource): Boolean =
    (source.projectYarnNoteId != null && projectYarnNoteId == source.projectYarnNoteId) ||
        (source.yarnCardId != null && yarnCardId == source.yarnCardId)

private fun ResolvedProjectYarnUsage.orphanItem(): ProjectYarnUsageItem =
    ProjectYarnUsageItem(
        source = usage.toDomain().source,
        name = card?.usageName()?.takeIf { it.isNotBlank() } ?: usage.sourceNameSnapshot,
        usage = usage.toDomain(),
        status = if (card != null) YarnUsageSourceStatus.UNLINKED else YarnUsageSourceStatus.UNAVAILABLE,
    )
