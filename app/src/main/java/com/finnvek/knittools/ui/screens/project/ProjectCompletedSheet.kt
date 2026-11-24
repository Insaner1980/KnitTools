package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.FormSheetContentColumn
import com.finnvek.knittools.ui.components.ProjectThumbnail
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.mainCounterProjectCardCountText
import com.finnvek.knittools.ui.components.rememberExpandedModalSheetState
import com.finnvek.knittools.ui.components.rememberLocaleDateFormat
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCompletedSheet(
    celebration: ProjectCompletionCelebration,
    onClose: () -> Unit,
    onNextProject: () -> Unit,
) {
    val project = celebration.snapshot.project
    val dateFormat = rememberLocaleDateFormat("yMMMd")
    val completionDateFormat = rememberLocaleDateFormat("yMMMd", eventZoneId = celebration.snapshot.completionZoneId)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberExpandedModalSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
    ) {
        FormSheetContentColumn {
            ProjectThumbnail(
                projectId = project.id,
                craftType = project.craftType,
                photoUri = celebration.photoUri,
                size = OnboardingDimens.CompletedProjectImageSize,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            SheetTitle(
                text =
                    stringResource(
                        if (project.craftType == CraftType.CROCHET) {
                            R.string.completed_title_crochet
                        } else {
                            R.string.completed_title_knitting
                        },
                    ),
            )
            CompletedProjectFact(
                label = stringResource(R.string.completed_time_label),
                value = durationText(DurationDisplayFormatter.fromMinutes(celebration.snapshot.totalSessionMinutes)),
            )
            CompletedProjectFact(
                label = stringResource(R.string.completed_count_label),
                value =
                    mainCounterProjectCardCountText(
                        MainCounterCountSlot(
                            project.count,
                            project.mainCounterLabelType,
                            project.mainCounterCustomLabel,
                        ),
                    ),
            )
            CompletedProjectFact(
                label = stringResource(R.string.completed_started_label),
                value = dateFormat.format(Date(project.createdAt)),
            )
            CompletedProjectFact(
                label = stringResource(R.string.completed_finished_label),
                value = completionDateFormat.format(Date(checkNotNull(project.completedAt))),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
            ) {
                CancelButton(onClick = onClose, text = stringResource(R.string.completed_close))
                Button(onClick = onNextProject) {
                    Text(stringResource(R.string.completed_next_project))
                }
            }
        }
    }
}

@Composable
private fun CompletedProjectFact(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
