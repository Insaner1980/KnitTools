package com.finnvek.knittools.ui.tips

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.tips.TipContext
import com.finnvek.knittools.domain.tips.TipId

@Composable
internal fun tipText(
    id: TipId,
    context: TipContext,
): String =
    stringResource(
        when (id) {
            TipId.FIRST_ROW ->
                when (context.mainCounterLabelType) {
                    MainCounterLabelType.ROWS -> R.string.tip_first_row_rows
                    MainCounterLabelType.ROUNDS -> R.string.tip_first_row_rounds
                    MainCounterLabelType.REPEATS -> R.string.tip_first_row_repeats
                    MainCounterLabelType.CUSTOM -> R.string.tip_first_row_custom
                }
            TipId.UNDO -> R.string.tip_undo
            TipId.STEP_SIZE -> R.string.tip_step_size
            TipId.WORK_SESSION -> R.string.tip_work_session
            TipId.OVERVIEW_INTRO -> R.string.tip_overview_intro
            TipId.PATTERN_TILE ->
                if (context.onboardingPatternReady &&
                    context.hasDocuments
                ) {
                    R.string.journey_open_pattern_tip
                } else {
                    R.string.tip_pattern_tile
                }
            TipId.READING_LINE -> R.string.tip_reading_line
            TipId.KEEP_SCREEN_ON -> R.string.tip_keep_screen_on
            TipId.TARGET_REACHED -> R.string.tip_target_reached
            TipId.FIRST_SESSION_SAVED -> R.string.tip_first_session_saved
            TipId.BACKUP -> R.string.tip_backup
            TipId.FOLDERS -> R.string.tip_folders
            TipId.WIDGET -> R.string.tip_widget
            TipId.TOOLS_INTRO -> R.string.tip_tools_intro
        },
    )

@Composable
internal fun tipActionLabel(id: TipId): String? =
    when (id) {
        TipId.KEEP_SCREEN_ON -> stringResource(R.string.tip_keep_screen_on_action)
        TipId.BACKUP -> stringResource(R.string.tip_backup_action)
        TipId.WIDGET -> stringResource(R.string.tip_widget_action)
        else -> null
    }
