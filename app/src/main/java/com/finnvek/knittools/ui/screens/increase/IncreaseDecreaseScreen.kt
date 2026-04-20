package com.finnvek.knittools.ui.screens.increase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.nano.ParsedInstruction
import com.finnvek.knittools.domain.calculator.IncreaseDecreaseCalculator
import com.finnvek.knittools.domain.model.IncreaseDecreaseMode
import com.finnvek.knittools.domain.model.IncreaseDecreaseResult
import com.finnvek.knittools.domain.model.KnittingStyle
import com.finnvek.knittools.ui.components.AnimatedResultNumber
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.InfoTip
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.PasteInstructionButton
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ResultNumberInset
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.screens.home.HomeViewModel

@Composable
fun IncreaseDecreaseScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val proState by homeViewModel.proState.collectAsStateWithLifecycle()
    var currentStitches by rememberSaveable { mutableStateOf("") }
    var changeBy by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(IncreaseDecreaseMode.INCREASE) }
    var style by rememberSaveable { mutableStateOf(KnittingStyle.FLAT) }

    val modeOptions = listOf(stringResource(R.string.mode_increase), stringResource(R.string.mode_decrease))
    val styleOptions = listOf(stringResource(R.string.style_flat), stringResource(R.string.style_circular))

    val result by remember(currentStitches, changeBy, mode, style) {
        derivedStateOf {
            val current = currentStitches.toIntOrNull() ?: return@derivedStateOf null
            val change = changeBy.toIntOrNull() ?: return@derivedStateOf null
            IncreaseDecreaseCalculator.calculate(current, change, mode, style)
        }
    }

    ToolScreenScaffold(
        title = stringResource(R.string.tool_increase_decrease),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PasteInstructionButton(
                isPro = proState.isPro,
                hintText = stringResource(R.string.instruction_hint_increase),
                onResult = { parsed ->
                    applyIncDecInstruction(parsed)?.let { (st, ch, m) ->
                        currentStitches = st
                        changeBy = ch
                        mode = m
                        true
                    } ?: false
                },
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SegmentedToggle(
                            options = modeOptions,
                            selectedIndex = mode.ordinal,
                            onSelect = { mode = IncreaseDecreaseMode.entries[it] },
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SegmentedToggle(
                            options = styleOptions,
                            selectedIndex = style.ordinal,
                            onSelect = { style = KnittingStyle.entries[it] },
                        )
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            InfoTip(
                                title = stringResource(R.string.tip_flat_vs_circular_title),
                                description = stringResource(R.string.tip_flat_vs_circular_desc),
                            )
                        }
                    }

                    NumberInputField(
                        value = currentStitches,
                        onValueChange = { currentStitches = it },
                        label = stringResource(R.string.current_stitches),
                        suffix = stringResource(R.string.unit_st),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NumberInputField(
                        value = changeBy,
                        onValueChange = { changeBy = it },
                        label = stringResource(mode.labelRes()),
                        suffix = stringResource(R.string.unit_st),
                        modifier = Modifier.fillMaxWidth(),
                        isLast = true,
                    )
                }
            }

            result?.let { r -> IncreaseDecreaseResultSection(r) }
        }
    }
}

@Composable
private fun IncreaseDecreaseResultSection(result: IncreaseDecreaseResult) {
    if (!result.isValid) {
        Text(
            text = result.errorMessage ?: stringResource(R.string.invalid_input),
            color = MaterialTheme.colorScheme.error,
        )
    } else {
        result.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        BadgePill(
            text =
                stringResource(R.string.unit_st).let { unit ->
                    stringResource(R.string.total_stitches_format, result.totalStitches, unit)
                },
        )

        Spacer(modifier = Modifier.height(8.dp))

        ResultCard(title = stringResource(R.string.easy_to_remember)) {
            AnimatedResultNumber(targetValue = result.easyPattern) { value ->
                ResultNumberInset {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        ResultCard(title = stringResource(R.string.balanced)) {
            AnimatedResultNumber(targetValue = result.balancedPattern) { value ->
                ResultNumberInset {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun applyIncDecInstruction(parsed: ParsedInstruction): Triple<String, String, IncreaseDecreaseMode>? =
    if (parsed is ParsedInstruction.IncreaseDecrease) {
        Triple(
            parsed.currentStitches.toString(),
            parsed.changeBy.toString(),
            if (parsed.isIncrease) IncreaseDecreaseMode.INCREASE else IncreaseDecreaseMode.DECREASE,
        )
    } else {
        null
    }

private fun IncreaseDecreaseMode.labelRes(): Int =
    when (this) {
        IncreaseDecreaseMode.INCREASE -> R.string.increase_by
        IncreaseDecreaseMode.DECREASE -> R.string.decrease_by
    }
