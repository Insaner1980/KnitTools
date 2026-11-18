package com.finnvek.knittools.ui.screens.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.TrialJourneyStep
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.FormSheetContentColumn
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.ComponentDimens

@Composable
fun TrialJourneyContent(
    steps: List<TrialJourneyStep>,
    onAction: (TrialJourneyAction) -> Unit,
    modifier: Modifier = Modifier,
    firstProject: Boolean = false,
    // Sheetissä otsikko kertoo jo kokeilusta, joten osiomerkin tilalla on lyhyt johdanto.
    showLabel: Boolean = true,
) {
    if (steps.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing)) {
        if (showLabel) {
            SectionLabel(stringResource(R.string.trial_journey_title))
        } else {
            Text(stringResource(R.string.trial_journey_intro), style = MaterialTheme.typography.bodyLarge)
        }
        Column {
            steps.forEach { step ->
                OverviewLinkRow(
                    title =
                        stringResource(
                            if (firstProject && step.action == TrialJourneyAction.AnotherProject) {
                                R.string.journey_first_project
                            } else {
                                step.action.titleResource()
                            },
                        ),
                    subtitle = null,
                    glyph = null,
                    done = step.done,
                    onClick = { onAction(step.action) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        // Edistyminen tekstinä: kudottu silmukkarivi luki koristeena eikä kertonut mitään lisää.
        Text(
            stringResource(R.string.trial_journey_progress, steps.count { it.done }, steps.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialJourneySheet(
    steps: List<TrialJourneyStep>,
    onAction: (TrialJourneyAction) -> Unit,
    onDismiss: () -> Unit,
    firstProject: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        dragHandle = null,
        properties =
            ModalBottomSheetProperties(
                shouldDismissOnBackPress = false,
                shouldDismissOnClickOutside = false,
            ),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
    ) {
        FormSheetContentColumn {
            SheetTitle(stringResource(R.string.trial_started_screen_title))
            TrialJourneyContent(
                steps = steps,
                firstProject = firstProject,
                onAction = onAction,
                showLabel = false,
            )
            CancelButton(
                onClick = onDismiss,
                text = stringResource(R.string.completed_close),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

private fun TrialJourneyAction.titleResource(): Int =
    when (this) {
        TrialJourneyAction.Widget -> R.string.trial_action_widget
        TrialJourneyAction.RepeatCounter -> R.string.trial_action_counter
        TrialJourneyAction.AnotherProject -> R.string.trial_action_project
        TrialJourneyAction.WeeklyChart -> R.string.trial_action_chart
        TrialJourneyAction.PatternCamera -> R.string.trial_action_pattern_pdf
    }
