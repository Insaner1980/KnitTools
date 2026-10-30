package com.finnvek.knittools.domain.tips

enum class TipId(
    val key: String,
) {
    FIRST_ROW("first_row"),
    UNDO("undo"),
    STEP_SIZE("step_size"),
    WORK_SESSION("work_session"),
    OVERVIEW_INTRO("overview_intro"),
    PATTERN_TILE("pattern_tile"),
    READING_LINE("reading_line"),
    KEEP_SCREEN_ON("keep_screen_on"),
    TARGET_REACHED("target_reached"),
    FIRST_SESSION_SAVED("first_session_saved"),
    BACKUP("backup"),
    FOLDERS("folders"),
    WIDGET("widget"),
    TOOLS_INTRO("tools_intro"),
}

val BASIC_TIPS = setOf(TipId.FIRST_ROW, TipId.UNDO, TipId.OVERVIEW_INTRO, TipId.STEP_SIZE)
