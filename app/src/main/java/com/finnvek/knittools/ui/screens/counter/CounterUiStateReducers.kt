package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.CounterProject

internal fun CounterUiState.withStartedProject(project: CounterProject): CounterUiState =
    copy(
        projectId = project.id,
        projectName = project.name,
        counter = CounterState(count = project.count, stepSize = project.stepSize),
        secondaryCount = project.secondaryCount,
        notes = project.notes,
        sectionName = project.sectionName,
        stitchCount = project.stitchCount,
        stitchTrackingEnabled = project.stitchTrackingEnabled,
        currentStitch = project.currentStitch,
        patternUri = project.patternUri,
        patternName = project.patternName,
        currentPatternPage = project.currentPatternPage,
        patternRowMapping = project.patternRowMapping,
        totalRows = project.totalRows,
        targetRows = project.targetRows,
        activeAlert = null,
        sessionSeconds = 0,
    )

internal fun CounterUiState.withObservedProject(project: CounterProject): CounterUiState {
    val observedCounter =
        if (project.count == counter.count) {
            counter.copy(count = project.count, stepSize = project.stepSize)
        } else {
            CounterState(count = project.count, stepSize = project.stepSize)
        }
    val projectReminders = reminders.filter { it.projectId == project.id }
    return copy(
        projectName = project.name,
        counter = observedCounter,
        secondaryCount = project.secondaryCount,
        notes = project.notes,
        sectionName = project.sectionName,
        stitchCount = project.stitchCount,
        stitchTrackingEnabled = project.stitchTrackingEnabled,
        currentStitch = project.currentStitch,
        patternUri = project.patternUri,
        patternName = project.patternName,
        currentPatternPage = project.currentPatternPage,
        patternRowMapping = project.patternRowMapping,
        totalRows = project.totalRows,
        targetRows = project.targetRows,
        activeAlert = ReminderLogic.activeReminders(projectReminders, project.count).firstOrNull(),
    )
}

internal fun CounterUiState.withCounterChange(
    updatedCounter: CounterState,
    resetStitch: Boolean,
): CounterUiState =
    reminders
        .filter { reminder -> projectId == null || reminder.projectId == projectId }
        .let { projectReminders ->
            copy(
                counter = updatedCounter,
                currentStitch = if (resetStitch) 0 else currentStitch,
                activeAlert = ReminderLogic.activeReminders(projectReminders, updatedCounter.count).firstOrNull(),
            )
        }
