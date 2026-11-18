package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MAX_TARGET_ROWS
import com.finnvek.knittools.domain.calculator.TargetInput
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.calculator.parseTargetRows
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.ScrollableFormDialog
import com.finnvek.knittools.ui.components.dialogTextFieldColors
import com.finnvek.knittools.ui.components.rememberCurrentLocale

@Composable
fun TargetRowsDialog(
    currentTarget: Int?,
    counterLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentTarget?.toString() ?: "") }
    val parsed = parseTargetRows(text)
    val locale = rememberCurrentLocale()

    ScrollableFormDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.target_rows_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(counterLabel) },
                isError = parsed == TargetInput.Invalid,
                supportingText =
                    if (parsed == TargetInput.Invalid) {
                        {
                            Text(
                                stringResource(
                                    R.string.project_target_error,
                                    formatIntegerForDisplay(MAX_TARGET_ROWS.toLong(), locale),
                                ),
                            )
                        }
                    } else {
                        null
                    },
                singleLine = true,
                colors = dialogTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Row {
                if (currentTarget != null) {
                    TextButton(onClick = onRemove) {
                        Text(stringResource(R.string.target_rows_remove))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(
                    onClick = {
                        when (parsed) {
                            is TargetInput.Valid -> onConfirm(parsed.value)
                            TargetInput.NoTarget -> onRemove()
                            TargetInput.Invalid -> Unit
                        }
                    },
                    enabled = parsed is TargetInput.Valid || parsed == TargetInput.NoTarget && currentTarget != null,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        },
        dismissButton = {
            CancelButton(onClick = onDismiss)
        },
    )
}
