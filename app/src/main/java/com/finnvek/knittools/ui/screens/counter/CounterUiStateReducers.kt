package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.DEFAULT_READING_GUIDE_FRACTION
import com.finnvek.knittools.domain.model.DEFAULT_READING_LINE_Y_FRACTION
import com.finnvek.knittools.domain.model.RowReminder

internal val CounterUiState.shouldLeaveCounter: Boolean
    get() = !isRestoringProject && projectsLoaded && projects.isEmpty() && projectId == null

internal fun CounterUiState.withStartedProject(project: CounterProject): CounterUiState =
    copy(
        projectId = project.id,
        remindersLoadedProjectId = null,
        countersLoadedProjectId = null,
        documentsLoadedProjectId = null,
        sessionsLoadedProjectId = null,
        hasSessions = false,
        isRestoringProject = false,
        isCompleted = project.isCompleted,
        completedAt = project.completedAt,
        projectName = project.name,
        // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
        counter = CounterState(count = project.count, stepSize = project.stepSize),
        canUndo = false,
        craftType = project.craftType,
        mainCounterLabelType = project.mainCounterLabelType,
        mainCounterCustomLabel = project.mainCounterCustomLabel,
        secondaryCount = project.secondaryCount,
        secondaryCounterUsed = project.secondaryCounterUsed,
        notes = project.notes,
        notesCreated = project.notesCreated,
        canUseNotes = canCreateNotes || project.notesCreated,
        canUseSecondaryCounter = canCreateSecondaryCounter || project.secondaryCounterUsed,
        sectionName = project.sectionName,
        stitchCount = project.stitchCount,
        stitchTrackingEnabled = project.stitchTrackingEnabled,
        currentStitch = project.currentStitch,
        linkedPattern = null,
        projectDocuments = emptyList(),
        projectDocumentAvailability = emptyMap(),
        // CPD-ON
        patternUri = null,
        patternName = null,
        currentPatternPage = 0,
        readingLineEnabled = false,
        readingLineYFraction = DEFAULT_READING_LINE_Y_FRACTION,
        readingLineFollowCurrentRow = true,
        verticalReadingGuideEnabled = false,
        verticalReadingGuideXFraction = DEFAULT_READING_GUIDE_FRACTION,
        patternRowMapping = null,
        totalRows = project.totalRows,
        targetRows = project.targetRows,
        linkedYarns = emptyList(),
        projectYarnNotes = emptyList(),
        reminders = emptyList(),
        projectCounters = emptyList(),
        latestPhotos = emptyList(),
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
        isCompleted = project.isCompleted,
        completedAt = project.completedAt,
        counter = observedCounter,
        craftType = project.craftType,
        mainCounterLabelType = project.mainCounterLabelType,
        mainCounterCustomLabel = project.mainCounterCustomLabel,
        secondaryCount = project.secondaryCount,
        secondaryCounterUsed = project.secondaryCounterUsed,
        notes = project.notes,
        notesCreated = project.notesCreated,
        canUseNotes = canCreateNotes || project.notesCreated,
        canUseSecondaryCounter = canCreateSecondaryCounter || project.secondaryCounterUsed,
        sectionName = project.sectionName,
        stitchCount = project.stitchCount,
        stitchTrackingEnabled = project.stitchTrackingEnabled,
        currentStitch = project.currentStitch,
        linkedPattern = linkedPattern?.takeIf { it.id == project.linkedPatternId },
        totalRows = project.totalRows,
        targetRows = project.targetRows,
        dismissedReminderTrigger = dismissal,
        activeAlert = activeReminder(projectReminders, project.count, dismissal),
    )
}

internal fun CounterUiState.withCounterChange(
    updatedCounter: CounterState,
    resetStitch: Boolean,
): CounterUiState {
    val dismissal = dismissedReminderTrigger?.takeIf { it.row == updatedCounter.count }
    return copy(
        counter = updatedCounter,
        currentStitch = if (resetStitch) 0 else currentStitch,
        dismissedReminderTrigger = dismissal,
        activeAlert = activeReminder(selectedProjectReminders(reminders), updatedCounter.count, dismissal),
    )
}

internal fun CounterUiState.withReminderList(reminders: List<RowReminder>): CounterUiState {
    val projectReminders = selectedProjectReminders(reminders)
    val dismissal = dismissedReminderTrigger?.takeIf { it.row == counter.count }
    return copy(
        reminders = reminders,
        dismissedReminderTrigger = dismissal,
        activeAlert = activeReminder(projectReminders, counter.count, dismissal),
    )
}

internal fun CounterUiState.withDismissedReminder(reminderId: Long): CounterUiState {
    val dismissedIds = dismissedReminderTrigger?.takeIf { it.row == counter.count }?.reminderIds.orEmpty()
    val dismissal = DismissedReminderTrigger(reminderIds = dismissedIds + reminderId, row = counter.count)
    return copy(
        activeAlert = activeReminder(selectedProjectReminders(reminders), counter.count, dismissal),
        dismissedReminderTrigger = dismissal,
    )
}

private fun CounterUiState.selectedProjectReminders(source: List<RowReminder>): List<RowReminder> =
    source.filter { reminder -> projectId == null || reminder.projectId == projectId }

private fun activeReminder(
    reminders: List<RowReminder>,
    currentRow: Int,
    dismissedReminderTrigger: DismissedReminderTrigger?,
): RowReminder? =
    ReminderLogic
        .activeReminders(reminders, currentRow)
        .firstOrNull { reminder ->
            dismissedReminderTrigger == null ||
                reminder.id !in dismissedReminderTrigger.reminderIds ||
                dismissedReminderTrigger.row != currentRow
        }
