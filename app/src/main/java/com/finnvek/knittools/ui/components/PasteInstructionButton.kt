package com.finnvek.knittools.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.InstructionParser
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import kotlinx.coroutines.launch

@Composable
fun PasteInstructionButton(
    proState: ProState,
    proAccessReady: Boolean,
    onResult: (ParsedInstruction) -> Boolean,
    onSeePro: () -> Unit,
    modifier: Modifier = Modifier,
    hintText: String? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showProPrompt by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        PasteInstructionAction(proState, proAccessReady, { expanded = !expanded }, { showProPrompt = true })

        AnimatedVisibility(visible = expanded && proState.hasFeature(ProFeature.INSTRUCTION_PASTE)) {
            InstructionInputForm(
                onResult = { result ->
                    val accepted = proState.hasFeature(ProFeature.INSTRUCTION_PASTE) && onResult(result)
                    if (accepted) expanded = false
                    accepted
                },
                hintText = hintText ?: stringResource(R.string.instruction_hint),
            )
        }
    }
    if (showProPrompt) {
        ProPromptSheet(
            request = ProPromptRequest(ProPromptSource.InstructionPaste),
            onDismiss = { showProPrompt = false },
            onTrialStarted = {
                showProPrompt = false
                expanded = true
            },
            onSeePro = {
                showProPrompt = false
                onSeePro()
            },
        )
    }
}

@Composable
private fun PasteInstructionAction(
    proState: ProState,
    proAccessReady: Boolean,
    onOpen: () -> Unit,
    onRequestPro: () -> Unit,
) {
    val pasteAccess = proActionAccess(proState, proAccessReady, ProFeature.INSTRUCTION_PASTE)
    val canPaste = proState.hasFeature(ProFeature.INSTRUCTION_PASTE)
    TextButton(
        onClick = {
            if (canPaste) {
                onOpen()
            } else if (pasteAccess == ProActionAccess.Locked) {
                onRequestPro()
            }
        },
        enabled = canPaste || pasteAccess == ProActionAccess.Locked,
        colors =
            ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Icon(Icons.Outlined.FilterVintage, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text(stringResource(R.string.paste_instruction), modifier = Modifier.weight(1f, fill = false))
        ProMarker(pasteAccess, modifier = Modifier.padding(start = ProjectOverviewDimens.ContentGap))
    }
}

@Composable
private fun InstructionInputForm(
    onResult: (ParsedInstruction) -> Boolean,
    hintText: String,
) {
    var instructionText by rememberSaveable { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val errorMessages =
        mapOf(
            ParsedInstruction.ErrorType.PARSE_FAILED to stringResource(R.string.instruction_parse_failed),
            ParsedInstruction.ErrorType.UNKNOWN to stringResource(R.string.generic_error_unknown),
        )
    val successMessage = stringResource(R.string.instruction_parsed)

    Column {
        TextField(
            value = instructionText,
            onValueChange = {
                if (it.length <= InstructionParser.MAX_INPUT_LENGTH) {
                    instructionText = it
                    resultMessage = null
                }
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
            shape = MaterialTheme.shapes.medium,
            colors = highContainerTextFieldColors(),
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
