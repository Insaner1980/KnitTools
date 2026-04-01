package com.finnvek.knittools.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isDecimal: Boolean = false,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered =
                if (isDecimal) {
                    newValue.filter { it.isDigit() || it == '.' }
                } else {
                    newValue.filter { it.isDigit() }
                }
            onValueChange(filtered)
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number,
            ),
        singleLine = true,
        suffix = suffix?.let { { Text(it) } },
    )
}
