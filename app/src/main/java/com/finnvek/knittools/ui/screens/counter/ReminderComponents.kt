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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.LabeledTextField
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.RowMenuAction
import com.finnvek.knittools.ui.components.RowOverflowMenu
import com.finnvek.knittools.ui.components.ScrollableFormDialog
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSheet(
    reminders: List<RowReminder>,
    currentRow: Int,
    proStatus: ProStatus,
    onAdd: () -> Unit,
    onEdit: (RowReminder) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
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
                ProBadge(status = proStatus)
                OverviewTextAction(label = stringResource(R.string.project_overview_add), onClick = onAdd)
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
            CounterValueFormatter.forReminderRepeat(reminder, currentRow)?.let { repeatDisplay ->
                Text(
                    text = repeatDisplay.asText(),
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
) {
    var deleteTarget by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(modifier = modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        items(items = reminders, key = { it.id }) { reminder ->
            ReminderListItem(
                reminder = reminder,
                currentRow = currentRow,
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    // Lähestyvä muistutus erottuu tekstin värillä eikä erillisellä pisteellä.
    val isUpcoming = !reminder.isCompleted && reminder.targetRow <= currentRow + UPCOMING_ROW_WINDOW
    val detail =
        listOfNotNull(
            stringResource(R.string.row_label_format, reminder.targetRow),
            CounterValueFormatter.forReminderRepeat(reminder, currentRow)?.asText(),
        ).joinToString(", ")

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
                    RowMenuAction(stringResource(R.string.edit_reminder), onClick),
                    RowMenuAction(stringResource(R.string.delete), onDeleteClick),
                ),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private const val UPCOMING_ROW_WINDOW = 5

@Composable
fun AddReminderDialog(
    reminder: RowReminder? = null,
    onSave: (targetRow: Int, repeatInterval: Int?, message: String) -> Unit,
    onDismiss: () -> Unit,
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

    val isRepeating = selectedType == 1
    val form =
        ReminderDialogForm(
            rowText = rowText,
            selectedType = selectedType,
            intervalText = intervalText,
            message = message,
        )
    val validation = form.validation

    ScrollableFormDialog(
        onDismissRequest = onDismiss,
        title = { ReminderDialogTitle(reminder = reminder) },
        text = {
            ReminderDialogFields(
                form = form,
                onRowTextChange = { rowText = it },
                onSelectedTypeChange = { selectedType = it },
                onIntervalTextChange = { intervalText = it },
                onMessageChange = { message = limitReminderMessage(message, it) },
            )
        },
        confirmButton = {
            ReminderDialogConfirmButton(
                validation = validation,
                isRepeating = isRepeating,
                message = message,
                onSave = onSave,
            )
        },
        dismissButton = {
            ReminderDialogDismissButton(onDismiss = onDismiss)
        },
    )
}

private data class ReminderDialogForm(
    val rowText: String,
    val selectedType: Int,
    val intervalText: String,
    val message: String,
) {
    val isRepeating: Boolean
        get() = selectedType == 1

    val validation: ReminderDialogValidation
        get() {
            val rowNumber = MeasurementNumberParser.parse(rowText, Locale.ROOT, integer = true).value?.toInt()
            val interval = MeasurementNumberParser.parse(intervalText, Locale.ROOT, integer = true).value?.toInt()
            return ReminderDialogValidation(
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

private data class ReminderDialogValidation(
    val rowNumber: Int?,
    val interval: Int?,
    val canSave: Boolean,
)

@Composable
private fun ReminderDialogTitle(reminder: RowReminder?) {
    Text(stringResource(if (reminder == null) R.string.add_reminder else R.string.edit_reminder))
}

@Composable
private fun ReminderDialogFields(
    form: ReminderDialogForm,
    onRowTextChange: (String) -> Unit,
    onSelectedTypeChange: (Int) -> Unit,
    onIntervalTextChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberInputField(
            value = form.rowText,
            onValueChange = onRowTextChange,
            label = stringResource(R.string.row_number),
        )
        SegmentedToggle(
            options =
                listOf(
                    stringResource(R.string.one_time),
                    stringResource(R.string.repeating),
                ),
            selectedIndex = form.selectedType,
            onSelect = onSelectedTypeChange,
        )
        if (form.isRepeating) {
            NumberInputField(
                value = form.intervalText,
                onValueChange = onIntervalTextChange,
                label = stringResource(R.string.repeat_every),
                options = NumberInputOptions(suffix = stringResource(R.string.repeat_every_rows)),
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

@Composable
private fun ReminderDialogConfirmButton(
    validation: ReminderDialogValidation,
    isRepeating: Boolean,
    message: String,
    onSave: (targetRow: Int, repeatInterval: Int?, message: String) -> Unit,
) {
    TextButton(
        onClick = {
            validation.rowNumber?.let { row ->
                onSave(row, if (isRepeating) validation.interval else null, message.trim())
            }
        },
        enabled = validation.canSave,
    ) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun ReminderDialogDismissButton(onDismiss: () -> Unit) {
    CancelButton(onClick = onDismiss)
}

private fun limitReminderMessage(
    current: String,
    next: String,
): String = if (next.length <= ReminderLogic.MESSAGE_MAX_LENGTH) next else current
