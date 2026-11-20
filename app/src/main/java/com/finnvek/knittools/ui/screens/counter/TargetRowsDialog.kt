package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MAX_TARGET_ROWS
import com.finnvek.knittools.domain.calculator.TargetInput
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.calculator.parseTargetRows
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.ScrollableFormDialog
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
            Column {
                NumberInputField(
                    value = text,
                    onValueChange = { text = it },
                    label = counterLabel,
                    errorMessage =
                        if (parsed == TargetInput.Invalid) {
                            stringResource(
                                R.string.project_target_error,
                                formatIntegerForDisplay(MAX_TARGET_ROWS.toLong(), locale),
                            )
                        } else {
                            null
                        },
                    options = NumberInputOptions(isLast = true, preserveRawInput = true),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (currentTarget != null) {
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.target_rows_remove))
                    }
                }
            }
        },
        confirmButton = {
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
}
