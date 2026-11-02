package com.finnvek.knittools.domain.tips

object TipRules {
    const val ENTRY_DELAY_MILLIS = 2_000L
    const val MAX_AUTOMATIC_TIPS = 2
    const val STEP_INCREMENT_THRESHOLD = 30
    const val BURST_INCREMENT_COUNT = 3
    const val BURST_WINDOW_MILLIS = 2_000L
    private const val BACKUP_AFTER_DAYS = 7L
    private const val WORK_SESSION_OPEN_DAYS = 2
    private const val PATTERN_OPEN_COUNT = 3
    private const val KEEP_SCREEN_OPEN_COUNT = 4
    private const val WIDGET_OPEN_DAYS = 3
    private const val FOLDER_PROJECT_COUNT = 4

    fun next(context: TipContext): TipId? {
        if (!context.screenReady || context.blocked || context.millisSinceEntry < ENTRY_DELAY_MILLIS) return null
        return candidates(context).firstOrNull { id ->
            id !in context.tipsSeen &&
                (context.shownThisProcess < MAX_AUTOMATIC_TIPS || id == TipId.UNDO && context.undoAfterFirstRow)
        }
    }

    private fun candidates(context: TipContext): List<TipId> =
        when (context.screen) {
            TipScreen.COUNTER -> counterTips(context)
            TipScreen.OVERVIEW ->
                if (context.projectId == null) {
                    emptyList()
                } else {
                    listOfNotNull(
                        TipId.OVERVIEW_INTRO,
                        TipId.FIRST_SESSION_SAVED.takeIf { context.firstSessionTipPending },
                    )
                }
            TipScreen.PROJECT_READER ->
                listOfNotNull(TipId.READING_LINE.takeIf { context.projectId != null && context.hasDocuments })
            TipScreen.PROJECTS ->
                listOfNotNull(
                    TipId.BACKUP.takeIf {
                        context.projectCount > 0 &&
                            !context.backupCreated &&
                            context.today - context.firstUseEpochDay >= BACKUP_AFTER_DAYS
                    },
                    TipId.FOLDERS.takeIf { context.projectCount >= FOLDER_PROJECT_COUNT },
                )
            TipScreen.TOOLS -> listOf(TipId.TOOLS_INTRO)
        }

    private fun counterTips(context: TipContext): List<TipId> {
        if (context.projectId == null || context.isCompleted) return emptyList()
        return listOfNotNull(
            TipId.FIRST_ROW.takeIf { context.isStarter },
            TipId.UNDO.takeIf { context.undoAfterFirstRow },
            TipId.STEP_SIZE.takeIf { context.stepSize == 1 && countedManyTimes(context) },
            TipId.WORK_SESSION.takeIf {
                context.counterOpenDays >= WORK_SESSION_OPEN_DAYS && !context.hasSessions && !context.hasActiveSession
            },
            TipId.PATTERN_TILE.takeIf { shouldShowPatternTip(context) },
            TipId.KEEP_SCREEN_ON.takeIf {
                context.counterOpenCount >= KEEP_SCREEN_OPEN_COUNT && !context.keepScreenAwake
            },
            TipId.TARGET_REACHED.takeIf {
                context.targetReached &&
                    context.target != null &&
                    context.count >= context.target
            },
            TipId.WIDGET.takeIf {
                context.widgetPinSupported &&
                    (
                        context.hasSessions ||
                            context.firstSessionTipPending ||
                            context.counterOpenDays >= WIDGET_OPEN_DAYS
                    )
            },
        )
    }

    private fun shouldShowPatternTip(context: TipContext): Boolean =
        (context.onboardingPatternReady && context.hasDocuments) ||
            (context.counterOpenCount >= PATTERN_OPEN_COUNT && !context.hasDocuments && !context.hasWebPattern)

    private fun countedManyTimes(context: TipContext): Boolean {
        if (context.incrementsInProject >= STEP_INCREMENT_THRESHOLD) return true
        val recent = context.incrementTimesMillis.sorted().takeLast(BURST_INCREMENT_COUNT)
        return recent.size == BURST_INCREMENT_COUNT && recent.last() - recent.first() <= BURST_WINDOW_MILLIS
    }
}
