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
import com.finnvek.knittools.domain.calculator.TargetInput
import com.finnvek.knittools.domain.calculator.parseTargetRows
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.ScrollableFormDialog

@Composable
fun TargetRowsDialog(
    currentTarget: Int?,
    counterLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentTarget?.toString() ?: "") }
    val parsed = (parseTargetRows(text) as? TargetInput.Valid)?.value

    ScrollableFormDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.target_rows_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(5) },
                label = { Text(counterLabel) },
                singleLine = true,
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
                    onClick = { parsed?.let(onConfirm) },
                    enabled = parsed != null,
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
