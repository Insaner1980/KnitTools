package com.finnvek.knittools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isDecimal: Boolean = false,
    suffix: String? = null,
    isLast: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val inputFieldShape = RoundedCornerShape(14.dp)

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(filterNumericInput(newValue, isDecimal))
            },
            modifier =
                Modifier
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        inputFieldShape,
                    ).then(
                        if (isFocused) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primaryContainer, inputFieldShape)
                        } else {
                            Modifier
                        },
                    ),
            textStyle = MaterialTheme.typography.titleSmall,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() },
                ),
            singleLine = true,
            suffix =
                suffix?.let {
                    {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            shape = inputFieldShape,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            interactionSource = interactionSource,
        )
    }
}

private fun filterNumericInput(
    value: String,
    isDecimal: Boolean,
): String {
    if (!isDecimal) {
        return value.filter(Char::isDigit)
    }

    val result = StringBuilder()
    var hasDecimalSeparator = false

    value.forEach { char ->
        when {
            char.isDigit() -> {
                result.append(char)
            }

            (char == '.' || char == ',') && !hasDecimalSeparator -> {
                result.append('.')
                hasDecimalSeparator = true
            }
        }
    }

    return result.toString()
}
