package com.finnvek.knittools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import java.text.DecimalFormatSymbols
import java.util.Locale

data class NumberInputOptions(
    val isDecimal: Boolean = false,
    val allowNegative: Boolean = false,
    val suffix: String? = null,
    val isLast: Boolean = false,
    val preserveRawInput: Boolean = false,
    val allowZero: Boolean = true,
)

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    options: NumberInputOptions = NumberInputOptions(),
    errorMessage: String? = null,
    semanticLabel: String? = null,
    onFocusLost: () -> Unit = {},
    inputModifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val inputFieldShape = RoundedCornerShape(14.dp)
    val locale = rememberCurrentLocale()
    var hasFocused by remember { mutableStateOf(false) }
    var hasBlurred by remember { mutableStateOf(false) }
    val parsed =
        MeasurementNumberParser.parse(
            value,
            locale,
            integer = !options.isDecimal,
            allowZero = options.allowZero,
            allowNegative = options.allowNegative,
        )
    val localError =
        if (hasBlurred &&
            value.isNotBlank()
        ) {
            parsed.error
                ?: MeasurementNumberError.INVALID_NUMBER.takeIf { parsed.incomplete && !isFocused }
        } else {
            null
        }
    val displayedError =
        errorMessage ?: localError?.let {
            stringResource(
                when (it) {
                    MeasurementNumberError.INVALID_NUMBER -> R.string.measurement_invalid_number
                    MeasurementNumberError.MUST_BE_POSITIVE -> R.string.measurement_positive_required
                    MeasurementNumberError.TOO_LARGE -> R.string.measurement_too_large
                },
            )
        }
    val visualTransformation =
        remember(options.isDecimal, locale) {
            if (options.isDecimal && !options.preserveRawInput) {
                localizedDecimalVisualTransformation(locale)
            } else {
                VisualTransformation.None
            }
        }

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
                onValueChange(
                    if (options.preserveRawInput) {
                        newValue
                    } else {
                        filterNumericInput(
                            newValue,
                            options.isDecimal,
                            options.allowNegative,
                        )
                    },
                )
            },
            modifier =
                inputModifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(
                        if (options.preserveRawInput) {
                            Modifier
                        } else {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                inputFieldShape,
                            )
                        },
                    ).then(
                        if (isFocused && !options.preserveRawInput) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primaryContainer, inputFieldShape)
                        } else {
                            Modifier
                        },
                    ).semantics {
                        contentDescription = semanticLabel ?: label
                        displayedError?.let { error(it) }
                    }.onFocusChanged {
                        if (it.isFocused) {
                            hasFocused = true
                        } else if (hasFocused) {
                            hasBlurred = true
                            onFocusLost()
                        }
                    },
            isError = displayedError != null,
            supportingText = displayedError?.let { message -> { Text(message) } },
            textStyle = MaterialTheme.typography.titleSmall,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = numericKeyboardType(options.isDecimal, options.allowNegative),
                    imeAction = if (options.isLast) ImeAction.Done else ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() },
                ),
            singleLine = true,
            visualTransformation = visualTransformation,
            suffix =
                options.suffix?.let {
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

internal fun localizeDecimalSeparatorForDisplay(
    value: String,
    locale: Locale,
): String = value.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)

private fun localizedDecimalVisualTransformation(locale: Locale): VisualTransformation =
    VisualTransformation { text ->
        TransformedText(
            text = AnnotatedString(localizeDecimalSeparatorForDisplay(text.text, locale)),
            offsetMapping = OffsetMapping.Identity,
        )
    }

private fun numericKeyboardType(
    isDecimal: Boolean,
    allowNegative: Boolean,
): KeyboardType =
    when {
        isDecimal && allowNegative -> KeyboardType.Text
        isDecimal -> KeyboardType.Decimal
        allowNegative -> KeyboardType.Text
        else -> KeyboardType.Number
    }

private fun filterNumericInput(
    value: String,
    isDecimal: Boolean,
    allowNegative: Boolean,
): String {
    val locale = Locale.getDefault()
    val parsed =
        MeasurementNumberParser.parse(
            value,
            locale,
            integer = !isDecimal,
            allowZero = true,
            allowNegative = allowNegative,
        )
    if (parsed.value == null) return value
    return value.trim().replace(DecimalFormatSymbols.getInstance(locale).decimalSeparator, '.')
}
