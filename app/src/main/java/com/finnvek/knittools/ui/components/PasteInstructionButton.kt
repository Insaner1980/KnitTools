package com.finnvek.knittools.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.nano.InstructionParser
import com.finnvek.knittools.ai.nano.NanoAvailability
import com.finnvek.knittools.ai.nano.NanoStatus
import com.finnvek.knittools.ai.nano.ParsedInstruction
import kotlinx.coroutines.launch

@Composable
fun PasteInstructionButton(
    isPro: Boolean,
    onResult: (ParsedInstruction) -> Boolean,
    modifier: Modifier = Modifier,
    hintText: String? = null,
) {
    var nanoAvailable by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(isPro) {
        if (isPro) {
            nanoAvailable = NanoAvailability.check() != NanoStatus.UNAVAILABLE
        }
    }

    if (!isPro || !nanoAvailable) return

    Column(modifier = modifier) {
        TextButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text(stringResource(R.string.paste_instruction))
        }

        AnimatedVisibility(visible = expanded) {
            InstructionInputForm(
                onResult = { result ->
                    val accepted = onResult(result)
                    if (accepted) expanded = false
                    accepted
                },
                hintText = hintText ?: stringResource(R.string.instruction_hint),
            )
        }
    }
}

@Composable
private fun InstructionInputForm(
    onResult: (ParsedInstruction) -> Boolean,
    hintText: String,
) {
    var instructionText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val errorMessages =
        mapOf(
            ParsedInstruction.ErrorType.BUSY to stringResource(R.string.ai_error_busy),
            ParsedInstruction.ErrorType.QUOTA to stringResource(R.string.ai_error_quota),
            ParsedInstruction.ErrorType.UNAVAILABLE to stringResource(R.string.ai_error_unavailable),
            ParsedInstruction.ErrorType.PARSE_FAILED to stringResource(R.string.instruction_parse_failed),
            ParsedInstruction.ErrorType.UNKNOWN to stringResource(R.string.ai_error_unknown),
        )
    val successMessage = stringResource(R.string.instruction_parsed)

    Column {
        TextField(
            value = instructionText,
            onValueChange = {
                instructionText = it
                resultMessage = null
            },
            label = { Text(hintText) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            trailingIcon = {
                if (isParsing) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
        )

        TextButton(
            onClick = {
                scope.launch {
                    isParsing = true
                    resultMessage = null
                    val result = InstructionParser.parse(instructionText)
                    isParsing = false
                    val (message, clearInput) = handleParseResult(result, onResult, errorMessages, successMessage)
                    resultMessage = message
                    if (clearInput) instructionText = ""
                }
            },
            enabled = instructionText.isNotBlank() && !isParsing,
        ) {
            Text(
                if (isParsing) {
                    stringResource(
                        R.string.parsing_instruction,
                    )
                } else {
                    stringResource(R.string.paste_instruction)
                },
            )
        }

        resultMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun handleParseResult(
    result: ParsedInstruction,
    onResult: (ParsedInstruction) -> Boolean,
    errorMessages: Map<ParsedInstruction.ErrorType, String>,
    successMessage: String,
): Pair<String?, Boolean> =
    if (result is ParsedInstruction.Failure) {
        errorMessages[result.errorType] to false
    } else if (onResult(result)) {
        successMessage to true
    } else {
        errorMessages[ParsedInstruction.ErrorType.PARSE_FAILED] to false
    }
