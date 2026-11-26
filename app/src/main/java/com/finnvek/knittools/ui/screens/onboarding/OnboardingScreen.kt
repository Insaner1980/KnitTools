package com.finnvek.knittools.ui.screens.onboarding

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.calculator.TargetInput
import com.finnvek.knittools.domain.calculator.parseProjectStartCount
import com.finnvek.knittools.domain.calculator.parseTargetRows
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.domain.model.canGoBack
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.illustrationHeightFor
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
    onPurchase: (Activity, SelectedOneTimeOffer) -> Unit,
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
    val usageAnalyticsEnabled by viewModel.usageAnalyticsEnabled.collectAsStateWithLifecycle()
    val readyProject by viewModel.readyProject.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = rememberCurrentLocale()
    val previewOffer = remember(locale) { OnboardingPreview.offer(locale) }
    var previewPurchaseVisible by rememberSaveable { mutableStateOf(false) }
    var pendingPdfDraftId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPdfUri by rememberSaveable { mutableStateOf<String?>(null) }
    val pdfPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) pendingPdfDraftId = null
            pendingPdfUri = uri?.toString()
        }
    val price =
        OnboardingPriceState(
            ready = ready,
            pro = pro,
            trialUnavailable = state.trialUnavailable || ProPolicyTestControl.currentTrialState(trial).clockTampered,
            offer = previewOffer ?: offer,
            productStatus = if (previewOffer != null) BillingProductStatus.Available else productStatus,
            purchaseBusy = purchaseBusy,
            pending = pending,
        )
    val draft = state.draft
    LaunchedEffect(draft?.id, draft?.step, state.busy, pendingPdfUri) {
        val owner = pendingPdfDraftId ?: return@LaunchedEffect
        if (draft == null || state.busy) return@LaunchedEffect
        if (draft.id != owner || draft.step != OnboardingStep.Project) {
            pendingPdfDraftId = null
            pendingPdfUri = null
        } else {
            pendingPdfUri?.let { uri ->
                pendingPdfDraftId = null
                pendingPdfUri = null
                viewModel.selectPdf(uri, owner)
            }
        }
    }
    SideEffect { onStartupReady(draft != null || state.messageRes != null) }
    if (draft?.step == OnboardingStep.Price) {
        var automaticExitAttempted by remember(draft.id, price.mode, price.pro.status) { mutableStateOf(false) }
        LifecycleResumeEffect(viewModel) {
            viewModel.refreshPrice()
            onPauseOrDispose { }
        }
        LaunchedEffect(draft.id, price.mode, price.pro.status, state.busy) {
            if (price.mode == OnboardingPriceMode.Skip && !state.busy && !automaticExitAttempted) {
                automaticExitAttempted = true
                if (price.pro.status == ProStatus.PRO_PURCHASED) {
                    viewModel.finishWithPro(draft.id)
                } else {
                    viewModel.finish()
                }
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
            usageAnalyticsEnabled = usageAnalyticsEnabled,
            readyProject = readyProject?.takeIf { it.id == draft.projectId },
            onUsageAnalyticsChanged = viewModel::setUsageAnalyticsEnabled,
            actions =
                OnboardingActions(
                    edit = viewModel::edit,
                    next = viewModel::next,
                    previous = viewModel::previous,
                    skip = viewModel::skip,
                    create = viewModel::createProject,
                    selectPdf = {
                        if (pendingPdfDraftId == null && draft.step == OnboardingStep.Project) {
                            pendingPdfDraftId = draft.id
                            pdfPicker.launch(arrayOf("application/pdf"))
                        }
                    },
                    removePdf = viewModel::removePdf,
                    startTrial = viewModel::startTrial,
                    finish = viewModel::finish,
                    purchase = {
                        if (previewOffer != null) {
                            previewPurchaseVisible = true
                        } else {
                            price.confirmedOffer?.let { displayedOffer ->
                                context.findActivity()?.let { onPurchase(it, displayedOffer) }
                            }
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
    usageAnalyticsEnabled: Boolean = false,
    onUsageAnalyticsChanged: (Boolean) -> Unit = {},
    readyProject: CounterProject? = null,
) {
    BackHandler(enabled = busy || draft.step != OnboardingStep.Welcome) {
        if (!busy) {
            when (draft.step) {
                OnboardingStep.Price -> actions.finish()

                // Luotu projekti ei palaa lomakkeeseen: takaisin-ele jatkaa hintavaiheeseen.
                OnboardingStep.Ready -> actions.next()

                else -> actions.previous()
            }
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
            JourneyFooter(draft, price, busy, actions)
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            val viewportHeight = maxHeight
            val shortContent = viewportHeight < OnboardingDimens.ShortContentHeight
            CompositionLocalProvider(LocalJourneyViewportHeight provides viewportHeight) {
                AnimatedContent(
                    targetState = draft.step,
                    transitionSpec = { journeyStepTransition() },
                    label = "onboardingStep",
                ) { step ->
                    // Kuvitetut esittelyvaiheet keskitetään, jottei näkymän alaosaan jää tyhjää aluetta.
                    val centered = step == OnboardingStep.Welcome || step == OnboardingStep.Ready
                    val scroll = rememberScrollState()
                    Box(contentAlignment = Alignment.TopCenter) {
                        Column(
                            Modifier
                                .contentWidth(ContentWidthPolicy.Form)
                                .verticalScroll(scroll)
                                .heightIn(min = viewportHeight)
                                .padding(ComponentDimens.ContentPadding),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    if (shortContent) {
                                        OnboardingDimens.ShortSectionGap
                                    } else {
                                        OnboardingDimens.JourneySectionGap
                                    },
                                    if (centered) Alignment.CenterVertically else Alignment.Top,
                                ),
                        ) {
                            messageRes?.let {
                                Text(
                                    stringResource(it),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                            when (step) {
                                OnboardingStep.Welcome -> JourneyWelcome(usageAnalyticsEnabled, onUsageAnalyticsChanged)
                                OnboardingStep.Craft -> JourneyCraft(draft, busy, actions)
                                OnboardingStep.Needs -> JourneyNeeds(draft, busy, actions.edit)
                                OnboardingStep.Counter -> JourneyCounter(draft, busy, actions.edit)
                                OnboardingStep.Toolbox -> JourneyToolbox(draft.craftType, draft.needs)
                                OnboardingStep.Project -> JourneyProject(draft, busy, actions)
                                OnboardingStep.Ready -> readyProject?.let { JourneyReady(it) }
                                OnboardingStep.Price -> JourneyPriceContent(draft, price, busy, actions.purchase)
                            }
                        }
                        // Vieritetty sisältö katkesi terävästi otsikkoalueen alle; ohut viiva erottaa ne.
                        if (scroll.canScrollBackward) {
                            HorizontalDivider(
                                Modifier.align(Alignment.TopCenter),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        // Sama viiva alareunassa kertoo, että sisältöä on vielä kiinteiden toimintojen alla.
                        if (scroll.canScrollForward) {
                            HorizontalDivider(
                                Modifier.align(Alignment.BottomCenter).testTag("onboarding_more_below"),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyFooter(
    draft: OnboardingDraft,
    price: OnboardingPriceState,
    busy: Boolean,
    actions: OnboardingActions,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        val screenHeight = maxHeight
        Column(
            Modifier
                .contentWidth(ContentWidthPolicy.Form)
                .heightIn(max = maxHeight / 2)
                .verticalScroll(rememberScrollState())
                .padding(ComponentDimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        ) {
            if (draft.step == OnboardingStep.Price) {
                JourneyPriceActions(
                    price = price,
                    busy = busy,
                    actions = actions,
                    sideBySide = screenHeight < OnboardingDimens.SideBySideActionsScreenHeight,
                )
            } else if (draft.step != OnboardingStep.Craft) {
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
}

/** Vaiheen vieritettävän sisällön korkeus; kuvitus ja otsikko mitoittuvat sen mukaan. */
internal val LocalJourneyViewportHeight = staticCompositionLocalOf { Dp.Infinity }

/**
 * Kuvituksen korkeus käytettävissä olevan korkeuden mukaan: matalassa tilassa pienempi ja hyvin
 * matalassa (vaakapuhelin) ei lainkaan, jotta vaiheen varsinainen sisältö näkyy.
 */
@Composable
internal fun journeyIllustrationHeight(regular: Dp): Dp? =
    illustrationHeightFor(LocalJourneyViewportHeight.current, regular, LocalDensity.current.fontScale)

@Composable
private fun JourneyHeader(
    step: OnboardingStep,
    busy: Boolean,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.journey_progress, step.ordinal + 1, OnboardingStep.entries.size)
    // Ready- ja hintavaiheessa ei ole takaisin- eikä ohitustoimintoa, joten tyhjä työkalurivi kutistuu pois.
    val toolbarVisible = step != OnboardingStep.Price && step != OnboardingStep.Ready
    Column(modifier.padding(horizontal = ComponentDimens.ContentPadding)) {
        AnimatedVisibility(visible = toolbarVisible, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = OnboardingDimens.JourneyToolbarHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step.canGoBack) {
                    IconButton(
                        onClick = actions.previous,
                        enabled = !busy,
                        modifier = Modifier.testTag("onboarding_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
                Spacer(Modifier.weight(1f))
                CancelButton(onClick = actions.skip, enabled = !busy, text = stringResource(R.string.journey_skip))
            }
        }
        // Tervetulonäkymä ei ole vielä vaihe: edistymispalkki tulee näkyviin vasta seuraavassa vaiheessa.
        AnimatedVisibility(
            visible = step != OnboardingStep.Welcome,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            JourneyProgress(step, description)
        }
    }
}

@Composable
private fun JourneyProgress(
    step: OnboardingStep,
    description: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = ComponentDimens.StandardSpacing)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(OnboardingDimens.JourneyProgressGap),
    ) {
        OnboardingStep.entries.forEach { item ->
            // Segmentti täyttyy vasemmalta oikealle, kun vaihe valmistuu.
            val fill by animateFloatAsState(
                if (item.ordinal <= step.ordinal) 1f else 0f,
                animationSpec = tween(OnboardingDimens.STEP_TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                label = "journeyProgress",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(OnboardingDimens.JourneyProgressHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Spacer(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
internal fun JourneyTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    // Matalassa tilassa kaksirivinen headlineLarge vei enemmän korkeutta kuin itse kuvitus.
    val short = LocalJourneyViewportHeight.current < OnboardingDimens.ShortContentHeight
    Text(
        text,
        style = if (short) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
        textAlign = textAlign,
        modifier = modifier.fillMaxWidth().semantics { heading() },
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

        // Craft etenee laatan napautuksella, eikä sillä ole alapalkin painiketta.
        OnboardingStep.Craft -> R.string.journey_continue

        OnboardingStep.Needs -> R.string.journey_try_counter

        OnboardingStep.Counter -> R.string.journey_continue

        OnboardingStep.Toolbox -> R.string.journey_start_project

        OnboardingStep.Project -> R.string.journey_create_project

        OnboardingStep.Ready -> R.string.journey_continue

        OnboardingStep.Price -> R.string.journey_start_trial
    }
