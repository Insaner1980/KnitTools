package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.ui.screens.pro.ProPromptViewModel

enum class ProPromptSource {
    Projects,
    ProgressPhotos,
    Notes,
    YarnCards,
    SaveToMyYarn,
    Counters,
    Reminders,
    PatternCamera,
    Widget,
}

data class ProPromptRequest(
    val source: ProPromptSource,
    val existingProjectCount: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S3776") // Sheet kokoaa yhden Pro-pyynnön kaikki tila- ja toimintovaihtoehdot.
fun ProPromptSheet(
    request: ProPromptRequest,
    onDismiss: () -> Unit,
    onTrialStarted: () -> Unit,
    onSeePro: () -> Unit,
    viewModelProvider: @Composable () -> ProPromptViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    var startFailed by remember(request) { mutableStateOf(false) }
    var pendingActionResumed by remember(request) { mutableStateOf(false) }
    val resumePendingAction = {
        if (!pendingActionResumed) {
            pendingActionResumed = true
            onTrialStarted()
        }
    }

    LaunchedEffect(proState.isPro) {
        if (proState.isPro) resumePendingAction()
    }

    LaunchedEffect(proState.status) {
        if (proState.status == ProStatus.TRIAL_EXPIRED) {
            viewModel.markContextualPromptShown()
        }
    }

    LaunchedEffect(viewModel, request) {
        viewModel.trialStartResults.collect { result ->
            when (result) {
                TrialStartResult.Started,
                TrialStartResult.AlreadyStarted,
                -> {
                    startFailed = false
                    resumePendingAction()
                }

                TrialStartResult.Failed -> startFailed = true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = request.title(),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = request.body(proState.status),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (startFailed) {
                Text(
                    text = stringResource(R.string.pro_trial_start_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.pro_prompt_not_now))
                }
                Button(
                    onClick =
                        if (proState.status == ProStatus.TRIAL_NOT_STARTED) {
                            viewModel::startTrial
                        } else {
                            onSeePro
                        },
                ) {
                    Text(
                        stringResource(
                            if (proState.status == ProStatus.TRIAL_NOT_STARTED) {
                                R.string.pro_start_free_trial
                            } else {
                                R.string.pro_prompt_see_pro
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProPromptRequest.title(): String =
    stringResource(
        when (source) {
            ProPromptSource.Projects -> R.string.pro_prompt_projects_title
            ProPromptSource.ProgressPhotos -> R.string.pro_prompt_photos_title
            ProPromptSource.Notes -> R.string.pro_prompt_notes_title
            ProPromptSource.YarnCards -> R.string.pro_prompt_yarn_title
            ProPromptSource.SaveToMyYarn -> R.string.pro_prompt_save_yarn_title
            ProPromptSource.Counters -> R.string.pro_prompt_counters_title
            ProPromptSource.Reminders -> R.string.pro_prompt_reminders_title
            ProPromptSource.PatternCamera -> R.string.pro_prompt_pattern_camera_title
            ProPromptSource.Widget -> R.string.pro_prompt_widget_title
        },
    )

@Composable
private fun ProPromptRequest.body(status: ProStatus): String =
    if (source == ProPromptSource.Projects) {
        pluralStringResource(
            if (status == ProStatus.TRIAL_NOT_STARTED) {
                R.plurals.pro_prompt_projects_trial_body
            } else {
                R.plurals.pro_prompt_projects_body
            },
            existingProjectCount,
            existingProjectCount,
        )
    } else if (status == ProStatus.TRIAL_NOT_STARTED) {
        stringResource(R.string.pro_prompt_trial_body)
    } else {
        stringResource(
            when (source) {
                ProPromptSource.ProgressPhotos -> R.string.pro_prompt_photos_body
                ProPromptSource.Notes -> R.string.pro_prompt_notes_body
                ProPromptSource.YarnCards -> R.string.pro_prompt_yarn_body
                ProPromptSource.SaveToMyYarn -> R.string.pro_prompt_save_yarn_body
                ProPromptSource.Counters -> R.string.pro_prompt_counters_body
                ProPromptSource.Reminders -> R.string.pro_prompt_reminders_body
                ProPromptSource.PatternCamera -> R.string.pro_prompt_pattern_camera_body
                ProPromptSource.Widget -> R.string.pro_prompt_widget_body
                ProPromptSource.Projects -> error("Project copy uses plurals")
            },
        )
    }
