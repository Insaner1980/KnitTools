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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.finnvek.knittools.data.local.RowReminderEntity
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.SegmentedToggle

@Composable
fun ReminderAlertCard(
    reminder: RowReminderEntity,
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
    reminders: List<RowReminderEntity>,
    currentRow: Int,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteTarget by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        reminders.forEach { reminder ->
            ReminderListItem(
                reminder = reminder,
                currentRow = currentRow,
                onLongClick = { deleteTarget = reminder.id },
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
    reminder: RowReminderEntity,
    currentRow: Int,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
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
                    onClick = {},
                    onLongClick = onLongClick,
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
    }
}

@Composable
fun AddReminderDialog(
    onSave: (targetRow: Int, repeatInterval: Int?, message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var rowText by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableIntStateOf(0) } // 0 = one-time, 1 = repeating
    var intervalText by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    val isRepeating = selectedType == 1
    val rowNumber = rowText.toIntOrNull()
    val interval = intervalText.toIntOrNull()
    val canSave =
        rowNumber != null &&
            rowNumber > 0 &&
            message.isNotBlank() &&
            (!isRepeating || (interval != null && interval > 0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_reminder)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberInputField(
                    value = rowText,
                    onValueChange = { rowText = it },
                    label = stringResource(R.string.row_number),
                )
                SegmentedToggle(
                    options =
                        listOf(
                            stringResource(R.string.one_time),
                            stringResource(R.string.repeating),
                        ),
                    selectedIndex = selectedType,
                    onSelect = { selectedType = it },
                )
                if (isRepeating) {
                    NumberInputField(
                        value = intervalText,
                        onValueChange = { intervalText = it },
                        label = stringResource(R.string.repeat_every),
                        suffix = stringResource(R.string.repeat_every_rows),
                    )
                }
                TextField(
                    value = message,
                    onValueChange = { if (it.length <= 200) message = it },
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    rowNumber?.let { row ->
                        onSave(row, if (isRepeating) interval else null, message.trim())
                    }
                },
                enabled = canSave,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
