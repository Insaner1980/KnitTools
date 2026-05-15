package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.RowReminder

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
        linkedPattern = null,
        patternUri = project.patternUri,
        patternName = project.patternName,
        currentPatternPage = project.currentPatternPage,
        patternRowMapping = project.patternRowMapping,
        totalRows = project.totalRows,
        targetRows = project.targetRows,
        projectCounters = emptyList(),
        activeAlert = null,
        dismissedReminderTrigger = null,
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
    val dismissal = dismissedReminderTrigger?.takeIf { it.row == project.count }
    return copy(
        projectName = project.name,
        counter = observedCounter,
        secondaryCount = project.secondaryCount,
        notes = project.notes,
        sectionName = project.sectionName,
        stitchCount = project.stitchCount,
        stitchTrackingEnabled = project.stitchTrackingEnabled,
        currentStitch = project.currentStitch,
        linkedPattern = linkedPattern?.takeIf { it.id == project.linkedPatternId },
        patternUri = project.patternUri,
        patternName = project.patternName,
        currentPatternPage = project.currentPatternPage,
        patternRowMapping = project.patternRowMapping,
        totalRows = project.totalRows,
        targetRows = project.targetRows,
        dismissedReminderTrigger = dismissal,
        activeAlert = activeReminder(projectReminders, project.count, dismissal),
    )
}

internal fun CounterUiState.withCounterChange(
    updatedCounter: CounterState,
    resetStitch: Boolean,
): CounterUiState =
    reminders
        .filter { reminder -> projectId == null || reminder.projectId == projectId }
        .let { projectReminders ->
            val dismissal = dismissedReminderTrigger?.takeIf { it.row == updatedCounter.count }
            copy(
                counter = updatedCounter,
                currentStitch = if (resetStitch) 0 else currentStitch,
                dismissedReminderTrigger = dismissal,
                activeAlert = activeReminder(projectReminders, updatedCounter.count, dismissal),
            )
        }

internal fun CounterUiState.withReminderList(reminders: List<RowReminder>): CounterUiState {
    val projectReminders = reminders.filter { reminder -> projectId == null || reminder.projectId == projectId }
    val dismissal = dismissedReminderTrigger?.takeIf { it.row == counter.count }
    return copy(
        reminders = reminders,
        dismissedReminderTrigger = dismissal,
        activeAlert = activeReminder(projectReminders, counter.count, dismissal),
    )
}

internal fun CounterUiState.withDismissedReminder(reminderId: Long): CounterUiState {
    val reminder = reminders.find { it.id == reminderId }
    val dismissal =
        if (reminder?.repeatInterval != null) {
            DismissedReminderTrigger(reminderId = reminderId, row = counter.count)
        } else {
            null
        }
    return copy(activeAlert = null, dismissedReminderTrigger = dismissal)
}

private fun activeReminder(
    reminders: List<RowReminder>,
    currentRow: Int,
    dismissedReminderTrigger: DismissedReminderTrigger?,
): RowReminder? =
    ReminderLogic
        .activeReminders(reminders, currentRow)
        .firstOrNull { reminder ->
            dismissedReminderTrigger == null ||
                dismissedReminderTrigger.reminderId != reminder.id ||
                dismissedReminderTrigger.row != currentRow
        }
