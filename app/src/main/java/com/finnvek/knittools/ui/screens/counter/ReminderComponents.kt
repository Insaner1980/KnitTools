package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.SegmentedToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSheet(
    reminders: List<RowReminder>,
    currentRow: Int,
    onAdd: () -> Unit,
    onEdit: (RowReminder) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reminders),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_reminder))
                }
            }
            if (reminders.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kullanvärinen vasen reuna
        Box(
            modifier =
                Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(12.dp))
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
                val count = ReminderLogic.repeatCount(reminder, currentRow)
                Text(
                    text = stringResource(R.string.repeat_count_format, count, reminder.repeatInterval),
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

    if (deleteTarget != null) {
        val targetReminder = reminders.find { it.id == deleteTarget }
        ConfirmationDialog(
            title = stringResource(R.string.delete_reminder_title),
            message = stringResource(R.string.delete_reminder_confirm, targetReminder?.message ?: ""),
            confirmText = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm = {
                deleteTarget?.let { onDelete(it) }
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderListItem(
    reminder: RowReminder,
    currentRow: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    val isUpcoming = !reminder.isCompleted && reminder.targetRow <= currentRow + 5
    val dotColor =
        if (isUpcoming) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onDeleteClick,
                ).background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reminder.repeatInterval != null) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.repeating),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.row_label_format, reminder.targetRow),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = reminder.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.edit_reminder),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.delete),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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

    AlertDialog(
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

private const val REMINDER_MESSAGE_MAX_LENGTH = 200

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
            val rowNumber = rowText.toIntOrNull()
            val interval = intervalText.toIntOrNull()
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
        TextField(
            value = form.message,
            onValueChange = onMessageChange,
            label = { Text(stringResource(R.string.reminder_message)) },
            placeholder = { Text(stringResource(R.string.reminder_message_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
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
    TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
    }
}

private fun limitReminderMessage(
    current: String,
    next: String,
): String = if (next.length <= REMINDER_MESSAGE_MAX_LENGTH) next else current
