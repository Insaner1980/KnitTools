package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.calculator.MainCounterLabelSlot
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.FormSheet
import com.finnvek.knittools.ui.components.FormSheetConfirm
import com.finnvek.knittools.ui.components.LabeledTextField
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.RowMenuAction
import com.finnvek.knittools.ui.components.RowOverflowMenu
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.SegmentedToggleSurface
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.components.mainCounterCountTextInline
import com.finnvek.knittools.ui.components.mainCounterLabelText
import com.finnvek.knittools.ui.components.rememberExpandedModalSheetState
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSheet(
    reminders: List<RowReminder>,
    currentRow: Int,
    proState: ProState,
    proAccessReady: Boolean,
    onAdd: () -> Unit,
    onEdit: (RowReminder) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
    label: MainCounterLabelSlot = MainCounterLabelSlot(MainCounterLabelType.ROWS),
) {
    val sheetState = rememberExpandedModalSheetState()
    val reminderAccess = proActionAccess(proState, proAccessReady, ProFeature.ROW_REMINDERS)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ComponentDimens.FormSheetHorizontalPadding)
                    .padding(bottom = ComponentDimens.FormSheetBottomPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.FormSheetItemSpacing),
        ) {
            // Sama otsikko ja oikean reunan lisäystoiminto kuin muissa sheeteissä ja projektinäkymän osioissa.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SheetTitle(text = stringResource(R.string.reminders), modifier = Modifier.weight(1f))
                OverviewTextAction(
                    label = stringResource(R.string.project_overview_add),
                    onClick = onAdd,
                    enabled =
                        reminderAccess != ProActionAccess.Unknown ||
                            proState.hasFeature(
                                ProFeature.ROW_REMINDERS,
                            ),
                    trailingContent = { ProMarker(reminderAccess) },
                )
            }
            if (reminders.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.knitToolsColors.emptyStateText,
                )
            } else {
                ReminderList(
                    reminders = reminders,
                    currentRow = currentRow,
                    label = label,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
fun ReminderAlertCard(
    reminder: RowReminder,
    currentRow: Int,
    onDismiss: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: MainCounterLabelSlot = MainCounterLabelSlot(MainCounterLabelType.ROWS),
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.knitToolsColors.cardContainer)
                .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (reminder.repeatInterval != null) {
                Text(
                    text = reminderDetailText(reminder, currentRow, label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = { onDismiss(reminder.id) }) {
            Text(
                text = stringResource(R.string.dismiss),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun ReminderList(
    reminders: List<RowReminder>,
    currentRow: Int,
    onEdit: (RowReminder) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: MainCounterLabelSlot = MainCounterLabelSlot(MainCounterLabelType.ROWS),
) {
    var deleteTarget by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(modifier = modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        items(items = reminders, key = { it.id }) { reminder ->
            ReminderListItem(
                reminder = reminder,
                currentRow = currentRow,
                label = label,
                onClick = { onEdit(reminder) },
                onDeleteClick = { deleteTarget = reminder.id },
            )
        }
    }

    val targetReminder = reminders.find { it.id == deleteTarget }
    LaunchedEffect(deleteTarget, targetReminder) {
        if (deleteTarget != null && targetReminder == null) deleteTarget = null
    }
    targetReminder?.let { reminder ->
        ConfirmationDialog(
            title = stringResource(R.string.delete_reminder_title),
            message = stringResource(R.string.delete_reminder_confirm, reminder.message),
            confirmText = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm = {
                onDelete(reminder.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

/**
 * Hiusviivarivi kuten istuntohistoriassa: viesti otsikkona, rivi ja toisto sen alla, muokkaus ja poisto
 * ⋮-valikossa. Harmaa laatikko, toisto- ja pistekuvake sekä kynä ja roskakori jokaisella rivillä erosivat
 * muista listoista.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderListItem(
    reminder: RowReminder,
    currentRow: Int,
    label: MainCounterLabelSlot,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    // Lähestyvä muistutus erottuu tekstin värillä eikä erillisellä pisteellä.
    val isUpcoming = ReminderLogic.isUpcoming(reminder, currentRow, UPCOMING_ROW_WINDOW)
    val detail = reminderDetailText(reminder, currentRow, label)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onDeleteClick)
                    .padding(vertical = ComponentDimens.StandardSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.message, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (isUpcoming) {
                            MaterialTheme.knitToolsColors.primaryReadable
                        } else {
                            MaterialTheme.knitToolsColors.onSurfaceMuted
                        },
                )
            }
            RowOverflowMenu(
                listOf(
                    RowMenuAction(stringResource(R.string.edit_reminder), onClick = onClick),
                    RowMenuAction(stringResource(R.string.delete), isDestructive = true, onClick = onDeleteClick),
                ),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun reminderDetailText(
    reminder: RowReminder,
    currentRow: Int,
    label: MainCounterLabelSlot,
): String {
    val target = MainCounterCountSlot(reminder.targetRow, label.labelType, label.customLabel)
    val count = mainCounterCountText(target)
    val interval = reminder.repeatInterval?.takeIf { it > 0 } ?: return count
    val intervalText =
        when (label.labelType) {
            MainCounterLabelType.ROWS -> {
                pluralStringResource(R.plurals.reminder_interval_rows, interval, interval)
            }

            MainCounterLabelType.ROUNDS -> {
                pluralStringResource(R.plurals.reminder_interval_rounds, interval, interval)
            }

            MainCounterLabelType.REPEATS -> {
                pluralStringResource(
                    R.plurals.reminder_interval_repeats,
                    interval,
                    interval,
                )
            }

            MainCounterLabelType.CUSTOM -> {
                stringResource(
                    R.string.reminder_interval_custom,
                    interval,
                    mainCounterLabelText(label.labelType, label.customLabel),
                )
            }
        }
    val next =
        if (currentRow >= reminder.targetRow) {
            ReminderLogic.nextOccurrence(reminder, currentRow)?.let {
                stringResource(R.string.reminder_next_format, mainCounterCountTextInline(target.copy(count = it)))
            }
        } else {
            null
        }
    return listOfNotNull(count, intervalText, next).joinToString(", ")
}

private const val UPCOMING_ROW_WINDOW = 5

@Composable
fun ReminderFormSheet(
    reminder: RowReminder? = null,
    onSave: (targetRow: Int, repeatInterval: Int?, message: String) -> Unit,
    onDismiss: () -> Unit,
    label: MainCounterLabelSlot = MainCounterLabelSlot(MainCounterLabelType.ROWS),
) {
    var rowText by rememberSaveable(reminder?.id) {
        mutableStateOf(reminder?.targetRow?.toString().orEmpty())
    }
    var selectedType by rememberSaveable(reminder?.id) {
        mutableIntStateOf(if (reminder?.repeatInterval != null) 1 else 0)
    }
    var intervalText by rememberSaveable(reminder?.id) {
        mutableStateOf(reminder?.repeatInterval?.toString().orEmpty())
    }
    var message by rememberSaveable(reminder?.id) { mutableStateOf(reminder?.message.orEmpty()) }

    val form =
        ReminderForm(
            rowText = rowText,
            selectedType = selectedType,
            intervalText = intervalText,
            message = message,
        )
    val validation = form.validation

    FormSheet(
        title = stringResource(if (reminder == null) R.string.add_reminder else R.string.edit_reminder),
        confirm =
            FormSheetConfirm(
                text = stringResource(R.string.save),
                enabled = validation.canSave,
                onClick = {
                    validation.rowNumber?.let { row ->
                        onSave(row, if (form.isRepeating) validation.interval else null, message.trim())
                    }
                },
            ),
        onDismiss = onDismiss,
    ) {
        ReminderFormFields(
            form = form,
            label = label,
            onRowTextChange = { rowText = it },
            onSelectedTypeChange = { selectedType = it },
            onIntervalTextChange = { intervalText = it },
            onMessageChange = { message = limitReminderMessage(message, it) },
        )
    }
}

private data class ReminderForm(
    val rowText: String,
    val selectedType: Int,
    val intervalText: String,
    val message: String,
) {
    val isRepeating: Boolean
        get() = selectedType == 1

    val validation: ReminderValidation
        get() {
            val rowNumber = MeasurementNumberParser.parse(rowText, Locale.ROOT, integer = true).value?.toInt()
            val interval = MeasurementNumberParser.parse(intervalText, Locale.ROOT, integer = true).value?.toInt()
            return ReminderValidation(
                rowNumber = rowNumber,
                interval = interval,
                canSave =
                    rowNumber != null &&
                        rowNumber > 0 &&
                        message.isNotBlank() &&
                        (!isRepeating || (interval != null && interval > 0)),
            )
        }
}

private data class ReminderValidation(
    val rowNumber: Int?,
    val interval: Int?,
    val canSave: Boolean,
)

@Composable
private fun ReminderFormFields(
    form: ReminderForm,
    label: MainCounterLabelSlot,
    onRowTextChange: (String) -> Unit,
    onSelectedTypeChange: (Int) -> Unit,
    onIntervalTextChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
) {
    val unit =
        when (label.labelType) {
            MainCounterLabelType.ROWS -> stringResource(R.string.counter_unit_rows)
            MainCounterLabelType.ROUNDS -> stringResource(R.string.counter_unit_rounds)
            MainCounterLabelType.REPEATS -> stringResource(R.string.counter_unit_repeats)
            MainCounterLabelType.CUSTOM -> mainCounterLabelText(label.labelType, label.customLabel)
        }
    Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.FormSheetItemSpacing)) {
        NumberInputField(
            value = form.rowText,
            onValueChange = onRowTextChange,
            label =
                mainCounterLabelText(
                    labelType = label.labelType,
                    customLabel = label.customLabel,
                    rows = R.string.reminder_at_row,
                    rounds = R.string.reminder_at_round,
                    repeats = R.string.reminder_at_repeat,
                    custom = R.string.reminder_at_custom,
                ),
        )
        SegmentedToggle(
            options =
                listOf(
                    stringResource(R.string.one_time),
                    stringResource(R.string.repeating),
                ),
            selectedIndex = form.selectedType,
            onSelect = onSelectedTypeChange,
            surface = SegmentedToggleSurface.Dialog,
        )
        if (form.isRepeating) {
            NumberInputField(
                value = form.intervalText,
                onValueChange = onIntervalTextChange,
                label = stringResource(R.string.repeat_every),
                options = NumberInputOptions(suffix = unit),
            )
        }
        // Sama kehys kuin kerrosnumerolla: tumma dialogikenttä ja kentän sisäinen nimike näyttivät eri lomakkeelta.
        LabeledTextField(
            value = form.message,
            onValueChange = onMessageChange,
            label = stringResource(R.string.reminder_message),
            placeholder = stringResource(R.string.reminder_message_hint),
        )
    }
}

private fun limitReminderMessage(
    current: String,
    next: String,
): String = if (next.length <= ReminderLogic.MESSAGE_MAX_LENGTH) next else current
