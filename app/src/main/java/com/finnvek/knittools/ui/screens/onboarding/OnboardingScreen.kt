package com.finnvek.knittools.ui.screens.onboarding

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.domain.calculator.TargetInput
import com.finnvek.knittools.domain.calculator.parseProjectStartCount
import com.finnvek.knittools.domain.calculator.parseTargetRows
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.findActivity
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.contentWidth
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens

data class OnboardingActions(
    val edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit = {},
    val next: () -> Unit = {},
    val previous: () -> Unit = {},
    val skip: () -> Unit = {},
    val create: () -> Unit = {},
    val selectPdf: () -> Unit = {},
    val removePdf: () -> Unit = {},
    val startTrial: () -> Unit = {},
    val finish: () -> Unit = {},
    val purchase: () -> Unit = {},
)

@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel,
    onPurchase: (Activity) -> Unit,
    onStartupReady: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pro by viewModel.proState.collectAsStateWithLifecycle()
    val ready by viewModel.proReady.collectAsStateWithLifecycle()
    val trial by viewModel.trialState.collectAsStateWithLifecycle()
    val offer by viewModel.offer.collectAsStateWithLifecycle()
    val productStatus by viewModel.productStatus.collectAsStateWithLifecycle()
    val purchaseBusy by viewModel.purchaseInFlight.collectAsStateWithLifecycle()
    val pending by viewModel.purchasePending.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = rememberCurrentLocale()
    val previewOffer = remember(locale) { OnboardingPreview.offer(locale) }
    var previewPurchaseVisible by rememberSaveable { mutableStateOf(false) }
    val pdfPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.selectPdf(it.toString()) }
        }
    val price =
        OnboardingPriceState(
            ready = ready,
            pro = pro,
            trialUnavailable = state.trialUnavailable || trial.clockTampered,
            offer = previewOffer ?: offer,
            productStatus = if (previewOffer != null) BillingProductStatus.Available else productStatus,
            purchaseBusy = purchaseBusy,
            pending = pending,
        )
    val draft = state.draft
    SideEffect { onStartupReady(draft != null || state.messageRes != null) }
    if (draft?.step == OnboardingStep.Price) {
        var automaticExitAttempted by remember(draft.id, price.mode) { mutableStateOf(false) }
        LifecycleResumeEffect(viewModel) {
            viewModel.refreshPrice()
            onPauseOrDispose { }
        }
        LaunchedEffect(price.mode, state.busy) {
            if (price.mode == OnboardingPriceMode.Skip && !state.busy && !automaticExitAttempted) {
                automaticExitAttempted = true
                if (price.pro.status == ProStatus.PRO_PURCHASED) viewModel.finishWithPro() else viewModel.finish()
            }
        }
    }
    if (draft == null) {
        Column(
            Modifier.fillMaxSize().padding(ComponentDimens.LargeContentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.messageRes?.let {
                Text(stringResource(it))
                Button(onClick = viewModel::load, enabled = !state.busy) { Text(stringResource(R.string.retry)) }
            } ?: CircularProgressIndicator()
        }
    } else {
        OnboardingScreen(
            draft = draft,
            price = price,
            busy = state.busy,
            messageRes = state.messageRes,
            actions =
                OnboardingActions(
                    edit = viewModel::edit,
                    next = viewModel::next,
                    previous = viewModel::previous,
                    skip = viewModel::skip,
                    create = viewModel::createProject,
                    selectPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
                    removePdf = viewModel::removePdf,
                    startTrial = viewModel::startTrial,
                    finish = viewModel::finish,
                    purchase = {
                        if (previewOffer != null) {
                            previewPurchaseVisible = true
                        } else {
                            context.findActivity()?.let(onPurchase)
                        }
                    },
                ),
        )
        if (previewPurchaseVisible && previewOffer != null) {
            OnboardingPreview.PurchaseDialog(
                price = previewOffer.formattedPrice,
                onDismiss = { previewPurchaseVisible = false },
                onContinue = {
                    previewPurchaseVisible = false
                    viewModel.finish()
                },
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    draft: OnboardingDraft,
    price: OnboardingPriceState,
    busy: Boolean,
    messageRes: Int?,
    actions: OnboardingActions,
) {
    BackHandler(enabled = busy || draft.step != OnboardingStep.Welcome) {
        if (!busy) {
            if (draft.step == OnboardingStep.Price) actions.finish() else actions.previous()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding().testTag("onboarding"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(Modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.Center) {
                JourneyHeader(draft.step, busy, actions, Modifier.contentWidth(ContentWidthPolicy.Form))
            }
        },
        bottomBar = {
            BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .contentWidth(ContentWidthPolicy.Form)
                        .heightIn(max = maxHeight / 2)
                        .verticalScroll(rememberScrollState())
                        .padding(ComponentDimens.ContentPadding),
                    verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
                ) {
                    if (draft.step == OnboardingStep.Price) {
                        JourneyPriceActions(price, busy, actions)
                    } else {
                        val canCreate =
                            draft.name.isNotBlank() &&
                                parseProjectStartCount(draft.initialCount) != null &&
                                parseTargetRows(draft.targetRows) != TargetInput.Invalid
                        JourneyPrimaryButton(
                            label = stringResource(nextLabel(draft.step)),
                            enabled = !busy && (draft.step != OnboardingStep.Project || canCreate),
                            onClick = if (draft.step == OnboardingStep.Project) actions.create else actions.next,
                            modifier = Modifier.testTag("onboarding_next"),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            key(draft.step) {
                Column(
                    Modifier
                        .contentWidth(ContentWidthPolicy.Form)
                        .verticalScroll(rememberScrollState())
                        .padding(ComponentDimens.ContentPadding),
                    verticalArrangement = Arrangement.spacedBy(OnboardingDimens.JourneySectionGap),
                ) {
                    messageRes?.let {
                        Text(
                            stringResource(it),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    when (draft.step) {
                        OnboardingStep.Welcome -> JourneyWelcome(draft)
                        OnboardingStep.Needs -> JourneyNeeds(draft, busy, actions.edit)
                        OnboardingStep.Counter -> JourneyCounter(draft, busy, actions.edit)
                        OnboardingStep.Benefits -> JourneyBenefits(draft)
                        OnboardingStep.Project -> JourneyProject(draft, busy, actions)
                        OnboardingStep.Price -> JourneyPriceContent(draft, price)
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyHeader(
    step: OnboardingStep,
    busy: Boolean,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.journey_progress, step.ordinal + 1, OnboardingStep.entries.size)
    Column(modifier.padding(horizontal = ComponentDimens.ContentPadding)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = OnboardingDimens.JourneyToolbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step.ordinal in 1..4) {
                IconButton(onClick = actions.previous, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            }
            Spacer(Modifier.weight(1f))
            if (step != OnboardingStep.Price) {
                CancelButton(onClick = actions.skip, enabled = !busy, text = stringResource(R.string.journey_skip))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = ComponentDimens.StandardSpacing)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(OnboardingDimens.JourneyProgressGap),
        ) {
            OnboardingStep.entries.forEach { item ->
                Spacer(
                    Modifier
                        .weight(1f)
                        .height(OnboardingDimens.JourneyProgressHeight)
                        .clip(CircleShape)
                        .background(
                            if (item.ordinal <=
                                step.ordinal
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }
    }
}

@Composable
internal fun JourneyTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
}

@Composable
internal fun JourneyPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().heightIn(min = ComponentDimens.FormSheetActionMinHeight),
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

private fun nextLabel(step: OnboardingStep): Int =
    when (step) {
        OnboardingStep.Welcome -> R.string.journey_begin
        OnboardingStep.Needs -> R.string.journey_try_counter
        OnboardingStep.Counter -> R.string.journey_show_more
        OnboardingStep.Benefits -> R.string.journey_start_project
        OnboardingStep.Project -> R.string.journey_create_project
        OnboardingStep.Price -> R.string.journey_start_trial
    }
