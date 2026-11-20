package com.finnvek.knittools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
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
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.tips.tipBlockWhenFocused
import java.text.DecimalFormatSymbols
import java.util.Locale

data class NumberInputOptions(
    val isDecimal: Boolean = false,
    val allowNegative: Boolean = false,
    val suffix: String? = null,
    val isLast: Boolean = false,
    val preserveRawInput: Boolean = false,
    val allowZero: Boolean = true,
    // Example value shown when the field is empty.
    val placeholder: String? = null,
    val placeholderIsExample: Boolean = true,
    val readOnly: Boolean = false,
)

@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Syöte- ja saavutettavuusasetukset pidetään näkyvinä.
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    options: NumberInputOptions = NumberInputOptions(),
    errorMessage: String? = null,
    semanticLabel: String? = null,
    onFocusLost: (() -> Unit)? = null,
    inputModifier: Modifier = Modifier,
    info: InfoTipText? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val locale = rememberCurrentLocale()
    val rawIntegerInput = options.preserveRawInput && !options.isDecimal
    val useOwnBorder = !options.preserveRawInput || rawIntegerInput
    var hadFocus by remember { mutableStateOf(false) }
    var hasBlurred by remember { mutableStateOf(false) }
    val localError =
        if (onFocusLost == null &&
            !rawIntegerInput &&
            hasBlurred &&
            value.isNotBlank()
        ) {
            val parsed =
                MeasurementNumberParser.parse(
                    value,
                    locale,
                    integer = !options.isDecimal,
                    allowZero = options.allowZero,
                    allowNegative = options.allowNegative,
                )
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
        LabelWithInfo(label = label, info = info)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            enabled = enabled,
            readOnly = options.readOnly,
            onValueChange = { newValue ->
                onValueChange(
                    if (options.preserveRawInput) {
                        newValue
                    } else {
                        filterNumericInput(
                            newValue,
                            options.isDecimal,
                            options.allowNegative,
                            locale,
                        )
                    },
                )
            },
            modifier =
                inputModifier
                    .tipBlockWhenFocused()
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(
                        if (useOwnBorder || displayedError != null) {
                            Modifier.inputFieldBorder(isFocused, displayedError != null)
                        } else {
                            Modifier
                        },
                    ).semantics {
                        contentDescription = semanticLabel ?: label
                        displayedError?.let { error(it) }
                    }.onFocusChanged {
                        if (hadFocus && !it.isFocused) {
                            hasBlurred = true
                            onFocusLost?.invoke()
                        }
                        hadFocus = it.isFocused
                    },
            isError = displayedError != null,
            placeholder =
                options.placeholder?.let { example ->
                    {
                        Text(
                            if (options.placeholderIsExample) {
                                stringResource(
                                    R.string.field_example_format,
                                    example,
                                )
                            } else {
                                example
                            },
                            color = MaterialTheme.knitToolsColors.fieldPlaceholderText,
                        )
                    }
                },
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
            shape = InputFieldShape,
            colors = cardTextFieldColors(),
            interactionSource = interactionSource,
        )
        InputFieldSupportingText(displayedError, supportingText)
    }
}

/**
 * Tekstikenttä samassa kehyksessä kuin [NumberInputField]: nimike kentän yläpuolella, sama pinta,
 * reunus ja muoto. Samassa lomakkeessa olevat teksti- ja numerokentät näyttävät näin samalta.
 */
@Composable
@Suppress("kotlin:S107")
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    inputModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    errorMessage: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    onFocusLost: (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var hadFocus by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        LabelWithInfo(label = label, info = null)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            readOnly = readOnly,
            trailingIcon = trailingIcon,
            modifier =
                inputModifier
                    .tipBlockWhenFocused()
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .inputFieldBorder(isFocused, errorMessage != null)
                    .semantics {
                        contentDescription = label
                        errorMessage?.let { error(it) }
                    }.onFocusChanged {
                        if (hadFocus && !it.isFocused) onFocusLost?.invoke()
                        hadFocus = it.isFocused
                    },
            placeholder =
                placeholder?.let { example ->
                    { Text(example, color = MaterialTheme.knitToolsColors.fieldPlaceholderText) }
                },
            textStyle = MaterialTheme.typography.titleSmall,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = errorMessage != null,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = InputFieldShape,
            colors = cardTextFieldColors(),
            interactionSource = interactionSource,
        )
        InputFieldSupportingText(errorMessage, supportingText)
    }
}

private val InputFieldShape = RoundedCornerShape(14.dp)

@Composable
private fun Modifier.inputFieldBorder(
    isFocused: Boolean,
    isError: Boolean,
): Modifier {
    val color =
        when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }
    return border(if (isFocused) 2.dp else 1.dp, color, InputFieldShape)
}

@Composable
private fun InputFieldSupportingText(
    errorMessage: String?,
    supportingText: String?,
) {
    val message = errorMessage ?: supportingText ?: return
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (errorMessage !=
                null
            ) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        modifier = Modifier.padding(top = 4.dp),
    )
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
        allowNegative -> KeyboardType.Text
        isDecimal -> KeyboardType.Decimal
        else -> KeyboardType.Number
    }

private fun filterNumericInput(
    value: String,
    isDecimal: Boolean,
    allowNegative: Boolean,
    locale: Locale,
): String {
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
