package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.domain.model.ProjectCompletionSnapshot

data class ProjectCompletionCelebration(
    val snapshot: ProjectCompletionSnapshot,
    val photoUri: String?,
)
