package com.finnvek.knittools.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onResult: (ParsedInstruction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nanoAvailable by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var instructionText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(isPro) {
        if (isPro) {
            nanoAvailable = NanoAvailability.check() != NanoStatus.UNAVAILABLE
        }
    }

    // Hide entirely if not Pro or Nano not available
    if (!isPro || !nanoAvailable) return

    Column(modifier = modifier) {
        TextButton(
            onClick = { expanded = !expanded },
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(stringResource(R.string.paste_instruction))
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                OutlinedTextField(
                    value = instructionText,
                    onValueChange = {
                        instructionText = it
                        resultMessage = null
                    },
                    label = { Text(stringResource(R.string.instruction_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        if (isParsing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    },
                )

                TextButton(
                    onClick = {
                        scope.launch {
                            isParsing = true
                            resultMessage = null
                            val result = InstructionParser.parse(instructionText)
                            isParsing = false
                            when (result) {
                                is ParsedInstruction.Failure -> {
                                    resultMessage = result.reason
                                }

                                else -> {
                                    resultMessage = context.getString(R.string.instruction_parsed)
                                    onResult(result)
                                    expanded = false
                                    instructionText = ""
                                }
                            }
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
    }
}
