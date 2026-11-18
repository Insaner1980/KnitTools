package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.PatternDisplayNames
import com.finnvek.knittools.ui.components.CraftTypeToggle
import com.finnvek.knittools.ui.components.LabelWithInfo
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
    val crochet = draft.projectCraftType == CraftType.CROCHET
    val count = parseProjectStartCount(draft.initialCount)
    val target = parseTargetRows(draft.targetRows)
    val locale = rememberCurrentLocale()
    JourneyTitle(
        stringResource(if (crochet) R.string.journey_project_crochet_title else R.string.journey_project_title),
    )
    LabeledTextField(
        value = draft.name,
        onValueChange = { value -> actions.edit { it.copy(name = value) } },
        label = stringResource(R.string.project_name_label),
        placeholder = stringResource(R.string.journey_name_placeholder, journeyExampleName(draft.projectCraftType)),
        inputModifier = Modifier.testTag("onboarding_project_name"),
    )
    LabelWithInfo(label = stringResource(R.string.craft_type_label), info = null)
    CraftTypeToggle(
        selected = draft.projectCraftType,
        onSelect = { craft -> actions.edit { it.copy(projectCraft = craft) } },
    )
    NumberInputField(
        value = draft.initialCount,
        onValueChange = { value -> actions.edit { it.copy(initialCount = value) } },
        label =
            stringResource(
                if (crochet) R.string.project_start_count_label_rounds else R.string.project_start_count_label_rows,
            ),
        options = NumberInputOptions(readOnly = busy, preserveRawInput = true),
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
    // Lisävalinnat ja PDF ovat yksi tiivis ryhmä: osioiden väli erotti ne turhaan toisistaan.
    Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing)) {
        val optionsOpen = draft.showOptions || target == TargetInput.Invalid
        TextButton(
            onClick = { actions.edit { it.copy(showOptions = !it.showOptions) } },
            enabled = !busy && target != TargetInput.Invalid,
            contentPadding = ComponentDimens.FlushTextButtonPadding,
        ) {
            Text(
                stringResource(
                    if (optionsOpen) R.string.project_create_fewer_options else R.string.journey_options,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (optionsOpen) {
            NumberInputField(
                value = draft.targetRows,
                onValueChange = { value -> actions.edit { it.copy(targetRows = value) } },
                label = stringResource(R.string.project_target_label),
                options = NumberInputOptions(readOnly = busy, isLast = true, preserveRawInput = true),
                errorMessage =
                    if (target == TargetInput.Invalid) {
                        stringResource(
                            R.string.project_target_error,
                            formatIntegerForDisplay(MAX_TARGET_ROWS.toLong(), locale),
                        )
                    } else {
                        null
                    },
                inputModifier = Modifier.testTag("onboarding_project_target"),
            )
            Text(
                stringResource(R.string.project_target_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (draft.pdfUri == null) {
            TextButton(
                onClick = actions.selectPdf,
                enabled = !busy,
                contentPadding = ComponentDimens.FlushTextButtonPadding,
            ) {
                Text(stringResource(R.string.journey_attach_pdf), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            JourneyAttachedPdf(draft.pdfName.orEmpty(), busy, actions)
        }
    }
    // Ei esikatselukorttia: se jäi alapainikkeen alle, ja Ready-vaihe näyttää saman kortin heti perään.
}

/** Liitetty PDF hiusviivojen välissä: nimi ja tekstitoiminnot ilman korttia tai ikonipainiketta. */
@Composable
private fun JourneyAttachedPdf(
    pdfName: String,
    busy: Boolean,
    actions: OnboardingActions,
) {
    // Väli erottaa liitetyn ohjeen yläpuolisesta apuritekstistä, joka muuten jäi kiinni hiusviivaan.
    Column(Modifier.fillMaxWidth().padding(top = ComponentDimens.StandardSpacing)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        // Tiedostonimi siistitään samoin kuin muualla sovelluksessa.
        Text(
            PatternDisplayNames.forDisplay(pdfName),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(top = ComponentDimens.StandardSpacing),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing)) {
            TextButton(
                onClick = actions.selectPdf,
                enabled = !busy,
                contentPadding = ComponentDimens.FlushTextButtonPadding,
            ) { Text(stringResource(R.string.journey_change_pdf)) }
            TextButton(
                onClick = actions.removePdf,
                enabled = !busy,
                contentPadding = ComponentDimens.FlushTextButtonPadding,
            ) { Text(stringResource(R.string.remove_pattern)) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
