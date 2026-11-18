package com.finnvek.knittools.domain.calculator

enum class ProjectSetupItem { TARGET, PATTERN, YARN, WORK_TIME }

data class ProjectSetupProgress(
    val completed: Set<ProjectSetupItem>,
) {
    val doneCount: Int get() = completed.size
    val items: List<ProjectSetupItem> get() = ProjectSetupItem.entries.sortedBy(::isDone)

    fun isDone(item: ProjectSetupItem): Boolean = item in completed
}

fun projectSetupProgress(
    targetRows: Int?,
    hasPattern: Boolean,
    hasYarn: Boolean,
    hasSessions: Boolean,
): ProjectSetupProgress =
    ProjectSetupProgress(
        listOfNotNull(
            ProjectSetupItem.TARGET.takeIf { targetRows != null },
            ProjectSetupItem.PATTERN.takeIf { hasPattern },
            ProjectSetupItem.YARN.takeIf { hasYarn },
            ProjectSetupItem.WORK_TIME.takeIf { hasSessions },
        ).toSet(),
    )
