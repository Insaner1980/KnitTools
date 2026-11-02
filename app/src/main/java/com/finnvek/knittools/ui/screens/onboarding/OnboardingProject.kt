package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MAX_PROJECT_START_COUNT
import com.finnvek.knittools.domain.calculator.MAX_TARGET_ROWS
import com.finnvek.knittools.domain.calculator.TargetInput
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.calculator.parseProjectStartCount
import com.finnvek.knittools.domain.calculator.parseTargetRows
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.ui.components.ContinueProjectCard
import com.finnvek.knittools.ui.components.LabeledTextField
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.ComponentDimens

@Composable
internal fun JourneyProject(
    draft: OnboardingDraft,
    busy: Boolean,
    actions: OnboardingActions,
) {
    val crochet = draft.craftType == CraftType.CROCHET
    val count = parseProjectStartCount(draft.initialCount)
    val target = parseTargetRows(draft.targetRows)
    val locale = rememberCurrentLocale()
    val canCreate = !busy && draft.name.isNotBlank() && count != null && target != TargetInput.Invalid
    JourneyTitle(
        stringResource(if (crochet) R.string.journey_project_crochet_title else R.string.journey_project_title),
    )
    LabeledTextField(
        value = draft.name,
        onValueChange = { value -> actions.edit { it.copy(name = value) } },
        label = stringResource(R.string.project_name_label),
        placeholder = stringResource(R.string.journey_name_placeholder, journeyExampleName(draft.craftType)),
        inputModifier = Modifier.testTag("onboarding_project_name"),
    )
    NumberInputField(
        value = draft.initialCount,
        onValueChange = { value -> actions.edit { it.copy(initialCount = value) } },
        label =
            stringResource(
                if (crochet) R.string.project_start_count_label_rounds else R.string.project_start_count_label_rows,
            ),
        options = NumberInputOptions(readOnly = busy),
        errorMessage =
            if (count ==
                null
            ) {
                stringResource(
                    R.string.project_start_count_error,
                    formatIntegerForDisplay(MAX_PROJECT_START_COUNT.toLong(), locale),
                )
            } else {
                null
            },
        inputModifier = Modifier.testTag("onboarding_project_count"),
    )
    Text(stringResource(R.string.project_start_count_help_new), style = MaterialTheme.typography.bodyLarge)
    TextButton(onClick = { actions.edit { it.copy(showOptions = !it.showOptions) } }, enabled = !busy) {
        Text(stringResource(R.string.journey_options), style = MaterialTheme.typography.titleMedium)
    }
    if (draft.showOptions) {
        NumberInputField(
            value = draft.targetRows,
            onValueChange = { value -> actions.edit { it.copy(targetRows = value) } },
            label = stringResource(R.string.project_target_label),
            options = NumberInputOptions(readOnly = busy, isLast = true),
            errorMessage =
                if (target ==
                    TargetInput.Invalid
                ) {
                    stringResource(
                        R.string.project_target_error,
                        formatIntegerForDisplay(MAX_TARGET_ROWS.toLong(), locale),
                    )
                } else {
                    null
                },
            inputModifier = Modifier.testTag("onboarding_project_target"),
        )
    }
    if (draft.pdfUri == null) {
        TextButton(onClick = actions.selectPdf, enabled = !busy) {
            Text(stringResource(R.string.journey_attach_pdf), style = MaterialTheme.typography.titleMedium)
        }
    } else {
        JourneyBenefitCard {
            Text(draft.pdfName.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
                TextButton(
                    onClick = actions.selectPdf,
                    enabled = !busy,
                ) { Text(stringResource(R.string.journey_change_pdf)) }
                IconButton(onClick = actions.removePdf, enabled = !busy) {
                    Icon(Icons.Default.Close, stringResource(R.string.remove_pattern))
                }
            }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
        Text(stringResource(R.string.journey_preview), style = MaterialTheme.typography.bodyMedium)
        ContinueProjectCard(
            project =
                CounterProject(
                    name = draft.name.ifBlank { stringResource(R.string.project_name_label) },
                    count = count ?: 0,
                    targetRows = (target as? TargetInput.Valid)?.value,
                    craftType = draft.craftType,
                    mainCounterLabelType = draft.craftType.defaultMainCounterLabelType(),
                ),
            sessionStatus = null,
            onClick = actions.create,
            onClickLabel = stringResource(R.string.journey_create_project),
            onOpenCounter = actions.create,
            enabled = canCreate,
        )
    }
}
