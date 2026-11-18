package com.finnvek.knittools.domain.tips

import com.finnvek.knittools.domain.model.MainCounterLabelType

enum class TipScreen {
    PROJECTS,
    COUNTER,
    OVERVIEW,
    PROJECT_READER,
    TOOLS,
}

data class TipContext(
    val screen: TipScreen,
    val projectId: Long? = null,
    val isStarter: Boolean = false,
    val count: Int = 0,
    val target: Int? = null,
    val stepSize: Int = 1,
    val mainCounterLabelType: MainCounterLabelType = MainCounterLabelType.ROWS,
    val hasDocuments: Boolean = false,
    val onboardingPatternReady: Boolean = false,
    val hasWebPattern: Boolean = false,
    val hasSessions: Boolean = false,
    val hasActiveSession: Boolean = false,
    val isCompleted: Boolean = false,
    val projectCount: Int = 0,
    val keepScreenAwake: Boolean = false,
    val widgetPinSupported: Boolean = false,
    val screenReady: Boolean = true,
    val blocked: Boolean = false,
    val millisSinceEntry: Long = 0L,
    val today: Long = 0L,
    val tipsSeen: Set<TipId> = emptySet(),
    val shownThisProcess: Int = 0,
    val firstUseEpochDay: Long = 0L,
    val counterOpenCount: Int = 0,
    val counterOpenDays: Int = 0,
    val backupCreated: Boolean = false,
    val firstSessionTipPending: Boolean = false,
    val incrementTimesMillis: List<Long> = emptyList(),
    val incrementsInProject: Int = 0,
    val undoAfterFirstRow: Boolean = false,
    val targetReached: Boolean = false,
)
