package com.finnvek.knittools.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.calculator.MainCounterLabelSlot
import com.finnvek.knittools.domain.calculator.MainCounterTargetSlot
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType

@Composable
fun craftTypeLabel(craftType: CraftType): String =
    when (craftType) {
        CraftType.KNITTING -> stringResource(R.string.craft_type_knitting)
        CraftType.CROCHET -> stringResource(R.string.craft_type_crochet)
    }

@Composable
fun mainCounterLabelText(
    labelType: MainCounterLabelType,
    customLabel: String?,
): String =
    when (labelType) {
        MainCounterLabelType.ROWS -> stringResource(R.string.main_counter_rows)
        MainCounterLabelType.ROUNDS -> stringResource(R.string.main_counter_rounds)
        MainCounterLabelType.REPEATS -> stringResource(R.string.main_counter_repeats)
        MainCounterLabelType.CUSTOM -> customLabel ?: stringResource(R.string.main_counter_custom)
    }

@Composable
fun mainCounterCountText(slot: MainCounterCountSlot): String =
    when (slot.labelType) {
        MainCounterLabelType.ROWS -> stringResource(R.string.current_row_short, slot.count)
        MainCounterLabelType.ROUNDS -> stringResource(R.string.current_round_short, slot.count)
        MainCounterLabelType.REPEATS -> stringResource(R.string.current_repeat_short, slot.count)
        MainCounterLabelType.CUSTOM ->
            stringResource(
                R.string.main_counter_custom_current_format,
                slot.customLabel ?: stringResource(R.string.main_counter_custom),
                slot.count,
            )
    }

@Composable
fun mainCounterTargetText(slot: MainCounterTargetSlot): String =
    when (slot.labelType) {
        MainCounterLabelType.ROWS -> stringResource(R.string.counter_value_of_target_format, slot.count, slot.target)
        MainCounterLabelType.ROUNDS ->
            stringResource(R.string.counter_round_value_of_target_format, slot.count, slot.target)
        MainCounterLabelType.REPEATS ->
            stringResource(R.string.counter_repeat_value_of_target_format, slot.count, slot.target)
        MainCounterLabelType.CUSTOM ->
            stringResource(
                R.string.counter_custom_value_of_target_format,
                slot.customLabel ?: stringResource(R.string.main_counter_custom),
                slot.count,
                slot.target,
            )
    }

@Composable
fun mainCounterProjectCardCountText(slot: MainCounterCountSlot): String =
    when (slot.labelType) {
        MainCounterLabelType.ROWS -> pluralStringResource(R.plurals.rows_format, slot.count, slot.count)
        MainCounterLabelType.ROUNDS -> pluralStringResource(R.plurals.rounds_format, slot.count, slot.count)
        MainCounterLabelType.REPEATS -> pluralStringResource(R.plurals.repeats_format, slot.count, slot.count)
        MainCounterLabelType.CUSTOM ->
            stringResource(
                R.string.main_counter_custom_count_format,
                slot.count,
                slot.customLabel ?: stringResource(R.string.main_counter_custom),
            )
    }

@Composable
fun mainCounterIncreaseContentDescription(slot: MainCounterLabelSlot): String =
    when (slot.labelType) {
        MainCounterLabelType.ROWS -> stringResource(R.string.counter_add_row)
        MainCounterLabelType.ROUNDS -> stringResource(R.string.counter_add_round)
        MainCounterLabelType.REPEATS -> stringResource(R.string.counter_add_repeat)
        MainCounterLabelType.CUSTOM ->
            stringResource(
                R.string.counter_add_custom,
                slot.customLabel ?: stringResource(R.string.main_counter_custom),
            )
    }

@Composable
fun mainCounterDecreaseContentDescription(slot: MainCounterLabelSlot): String =
    when (slot.labelType) {
        MainCounterLabelType.ROWS -> stringResource(R.string.counter_decrease_row)
        MainCounterLabelType.ROUNDS -> stringResource(R.string.counter_decrease_round)
        MainCounterLabelType.REPEATS -> stringResource(R.string.counter_decrease_repeat)
        MainCounterLabelType.CUSTOM ->
            stringResource(
                R.string.counter_decrease_custom,
                slot.customLabel ?: stringResource(R.string.main_counter_custom),
            )
    }

@Composable
fun projectMetadataText(
    craftType: CraftType,
    labelType: MainCounterLabelType,
    customLabel: String?,
): String =
    stringResource(
        R.string.project_metadata_format,
        craftTypeLabel(craftType),
        mainCounterLabelText(labelType, customLabel),
    )
