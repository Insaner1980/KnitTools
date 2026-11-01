package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.screens.pro.ProPromptViewModel
import com.finnvek.knittools.ui.theme.ComponentDimens

enum class ProPromptSource {
    Projects,
    ProjectReactivation,
    ProgressPhotos,
    Notes,
    YarnCards,
    SaveToMyYarn,
    Counters,
    Reminders,
    PatternCamera,
    PatternGallery,
    Widget,
    InstructionPaste,
    WidgetPin,
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
    val offer by viewModel.selectedOffer.collectAsStateWithLifecycle()
    val productStatus by viewModel.productStatus.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.retryProductDetails()
        onPauseOrDispose { }
    }
    val isStartingTrial by viewModel.isStartingTrial.collectAsStateWithLifecycle()
    var startFailed by remember(request) { mutableStateOf(false) }
    var pendingActionResumed by rememberSaveable(request) { mutableStateOf(false) }
    val resumePendingAction = {
        if (!pendingActionResumed) {
            pendingActionResumed = true
            onTrialStarted()
        }
    }

    LaunchedEffect(proState.isPro, isStartingTrial) {
        if (proState.isPro && !isStartingTrial) resumePendingAction()
    }

    LaunchedEffect(proState.status) {
        if (proState.status == ProStatus.TRIAL_EXPIRED) {
            viewModel.markContextualPromptShown()
        }
    }

    CollectWithLifecycleEffect({ viewModel.trialStartResults }) { result ->
        when (result) {
            TrialStartResult.Started,
            TrialStartResult.AlreadyActive,
            -> {
                startFailed = false
                resumePendingAction()
            }

            TrialStartResult.AlreadyExpired,
            TrialStartResult.AlreadyTampered,
            -> {
                startFailed = false
            }

            TrialStartResult.Failed -> {
                startFailed = true
            }
        }
    }

    ModalBottomSheet(
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ProPromptContent(
            request,
            proState.status,
            offer,
            productStatus,
            isStartingTrial,
            startFailed,
            onDismiss,
            onSeePro,
            viewModel::startTrial,
            viewModel::retryProductDetails,
        )
    }
}

@Composable
internal fun ProPromptContent(
    request: ProPromptRequest,
    status: ProStatus,
    offer: com.finnvek.knittools.billing.SelectedOneTimeOffer?,
    productStatus: com.finnvek.knittools.billing.BillingProductStatus,
    isStartingTrial: Boolean,
    startFailed: Boolean,
    onDismiss: () -> Unit,
    onSeePro: () -> Unit,
    onStartTrial: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ComponentDimens.FormSheetHorizontalPadding)
                .padding(bottom = ComponentDimens.FormSheetBottomPadding),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.FormSheetItemSpacing),
    ) {
        SheetTitle(text = request.title())
        Text(
            text = request.body(status),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = request.retention(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProPriceDetails(offer, productStatus, status == ProStatus.TRIAL_NOT_STARTED, onRetry)
        Text(
            text =
                stringResource(
                    if (status == ProStatus.TRIAL_NOT_STARTED) {
                        R.string.onboarding_pro_trial_terms
                    } else {
                        R.string.onboarding_pro_purchase_terms
                    },
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (status == ProStatus.TRIAL_NOT_STARTED) {
            TextButton(onClick = onSeePro) { Text(stringResource(R.string.welcome_learn_pro)) }
        }
        if (startFailed) {
            Text(
                text = stringResource(R.string.pro_trial_start_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
        ) {
            // Neutraali peruutus, jotta kokeilun aloitus on ainoa oranssi toiminto.
            CancelButton(onClick = onDismiss, text = stringResource(R.string.pro_prompt_not_now))
            val canStartTrial = status == ProStatus.TRIAL_NOT_STARTED
            Button(onClick = if (canStartTrial) onStartTrial else onSeePro, enabled = !isStartingTrial) {
                Text(
                    stringResource(
                        if (canStartTrial) R.string.pro_start_free_trial else R.string.pro_prompt_see_pro,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProPromptRequest.title(): String =
    stringResource(
        when (source) {
            ProPromptSource.Projects -> R.string.onboarding_pro_projects_title

            ProPromptSource.ProjectReactivation -> R.string.onboarding_pro_reactivation_title

            ProPromptSource.ProgressPhotos -> R.string.onboarding_pro_photos_title

            ProPromptSource.Notes -> R.string.onboarding_pro_notes_title

            ProPromptSource.YarnCards,
            ProPromptSource.SaveToMyYarn,
            -> R.string.onboarding_pro_yarn_title

            ProPromptSource.Counters -> R.string.onboarding_pro_counters_title

            ProPromptSource.Reminders -> R.string.onboarding_pro_reminders_title

            ProPromptSource.PatternCamera,
            ProPromptSource.PatternGallery,
            -> R.string.onboarding_pro_pattern_pdf_title

            ProPromptSource.Widget,
            ProPromptSource.WidgetPin,
            -> R.string.pro_prompt_widget_pin_title

            ProPromptSource.InstructionPaste -> R.string.pro_prompt_instruction_paste_title
        },
    )

@Composable
private fun ProPromptRequest.body(status: ProStatus): String =
    stringResource(
        when (source) {
            ProPromptSource.Projects ->
                if (status == ProStatus.TRIAL_NOT_STARTED) {
                    R.string.onboarding_pro_projects_trial_body
                } else {
                    R.string.onboarding_pro_projects_body
                }
            ProPromptSource.ProjectReactivation -> R.string.onboarding_pro_reactivation_body
            ProPromptSource.ProgressPhotos -> R.string.onboarding_pro_photos_body
            ProPromptSource.Notes -> R.string.onboarding_pro_notes_body
            ProPromptSource.YarnCards,
            ProPromptSource.SaveToMyYarn,
            -> R.string.onboarding_pro_yarn_body
            ProPromptSource.Counters -> R.string.onboarding_pro_counters_body
            ProPromptSource.Reminders -> R.string.onboarding_pro_reminders_body
            ProPromptSource.PatternCamera,
            ProPromptSource.PatternGallery,
            -> R.string.onboarding_pro_pattern_pdf_body
            ProPromptSource.Widget,
            ProPromptSource.WidgetPin,
            -> R.string.onboarding_pro_widget_body
            ProPromptSource.InstructionPaste -> R.string.pro_prompt_instruction_paste_body
        },
    )

@Composable
private fun ProPromptRequest.retention(): String =
    stringResource(
        when (source) {
            ProPromptSource.Projects,
            ProPromptSource.ProjectReactivation,
            -> R.string.onboarding_pro_retention_projects
            ProPromptSource.ProgressPhotos -> R.string.onboarding_pro_retention_photos
            ProPromptSource.Notes -> R.string.onboarding_pro_retention_notes
            ProPromptSource.YarnCards -> R.string.onboarding_pro_retention_yarn
            ProPromptSource.SaveToMyYarn -> R.string.onboarding_pro_retention_project_yarn
            ProPromptSource.Counters -> R.string.onboarding_pro_retention_counters
            ProPromptSource.Reminders -> R.string.onboarding_pro_retention_reminders
            ProPromptSource.PatternCamera,
            ProPromptSource.PatternGallery,
            -> R.string.onboarding_pro_retention_pattern_pdf
            ProPromptSource.Widget,
            ProPromptSource.WidgetPin,
            -> R.string.onboarding_pro_retention_widget
            ProPromptSource.InstructionPaste -> R.string.onboarding_pro_retention_paste
        },
    )
