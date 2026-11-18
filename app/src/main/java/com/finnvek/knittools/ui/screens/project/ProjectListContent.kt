package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject

/** An empty list is usable only after all required initial observations have emitted. */
data class ProjectListContent(
    val isLoading: Boolean = true,
    val hasAnyProject: Boolean = false,
    val activeProjectCount: Int = 0,
    val welcomeDismissed: Boolean = false,
    val preferences: AppPreferences = AppPreferences(),
    val active: List<CounterProject> = emptyList(),
    val completed: List<CounterProject> = emptyList(),
    val activeSession: ActiveWorkSession? = null,
) {
    val continueKnitting: ContinueKnittingProject?
        get() =
            active.firstOrNull { it.count > 0 }?.let { project ->
                ContinueKnittingProject(
                    project.id,
                    project.name,
                    project.count,
                    project.sectionName,
                    project.targetRows,
                    project.craftType,
                    project.mainCounterLabelType,
                    project.mainCounterCustomLabel,
                )
            }
}
