package com.finnvek.knittools.ui.screens.pro

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.TrialJourneyStep
import com.finnvek.knittools.ui.components.ProPriceDetails
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.StatusMessage
import com.finnvek.knittools.ui.components.StatusMessageType
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.findActivity
import com.finnvek.knittools.ui.theme.ComponentDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeScreen(
    onBack: () -> Unit,
    onPurchase: (Activity) -> Unit,
    viewModelProvider: @Composable () -> ProUpgradeViewModel = { hiltViewModel() },
    onStartupReady: (Boolean) -> Unit = {},
    personalLine: String? = null,
    journeySteps: List<TrialJourneyStep> = emptyList(),
    journeyCraft: CraftType = CraftType.KNITTING,
    onJourneyAction: (TrialJourneyAction) -> Unit = {},
) {
    val viewModel = viewModelProvider()
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    val proStateReady by viewModel.proStateReady.collectAsStateWithLifecycle()
    val purchaseQueryFailed by viewModel.purchaseQueryFailed.collectAsStateWithLifecycle()
    val selectedOffer by viewModel.selectedOffer.collectAsStateWithLifecycle()
    val productStatus by viewModel.productStatus.collectAsStateWithLifecycle()
    val purchaseFlowInFlight by viewModel.purchaseFlowInFlight.collectAsStateWithLifecycle()
    val statusMessageRes by viewModel.statusMessageRes.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val isStartingTrial by viewModel.isStartingTrial.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.retryProductDetails()
        onPauseOrDispose { }
    }
    SideEffect {
        onStartupReady(
            proStateReady || purchaseQueryFailed,
        )
    }

    // Sama läpinäkyvä yläpalkki kuin muissa näytöissä; oma TopAppBar sai vierittäessä sävytetyn pohjan.
    ToolScreenScaffold(
        title = stringResource(R.string.knittools_pro),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProIntroduction(personalLine)
            ProFreeFeatures()
            SectionLabel(stringResource(R.string.onboarding_pro_adds_title))
            ProBenefits()
            TrialJourneyContent(journeySteps, journeyCraft, onJourneyAction)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ProPurchaseSection(
                proState = proState,
                proStateReady = proStateReady,
                purchaseQueryFailed = purchaseQueryFailed,
                offer = selectedOffer,
                productStatusProvider = { productStatus },
                purchaseFlowInFlight = purchaseFlowInFlight,
                isRestoring = isRestoring,
                isStartingTrial = isStartingTrial,
                onPurchase = { context.findActivity()?.let(onPurchase) },
                onRestore = viewModel::restorePurchases,
                onRetry = viewModel::retryProductDetails,
                onStartTrial = viewModel::startTrial,
            )
            statusMessageRes?.let { ProStatusMessage(messageRes = it) }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProIntroduction(personalLine: String?) {
    Text(
        text = stringResource(R.string.onboarding_pro_intro),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
    )
    personalLine?.let {
        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProFreeFeatures() {
    Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
        SectionLabel(stringResource(R.string.onboarding_pro_free_title))
        listOf(
            R.string.onboarding_pro_free_project,
            R.string.onboarding_pro_free_counter,
            R.string.onboarding_pro_free_pdf,
            R.string.onboarding_pro_free_web,
            R.string.onboarding_pro_free_yarn,
            R.string.onboarding_pro_free_time,
            R.string.onboarding_pro_free_tools,
            R.string.onboarding_pro_free_backup,
        ).forEach { resource ->
            Text(stringResource(resource), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProBenefits() {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ProBenefitGroup(
            title = stringResource(R.string.onboarding_pro_group_projects_title),
            body = stringResource(R.string.onboarding_pro_group_projects_body),
        )
        ProBenefitGroup(
            title = stringResource(R.string.onboarding_pro_group_workflow_title),
            body = stringResource(R.string.onboarding_pro_group_workflow_body),
        )
        ProBenefitGroup(
            title = stringResource(R.string.onboarding_pro_group_insights_title),
            body = stringResource(R.string.onboarding_pro_group_insights_body),
        )
    }
}

@Composable
private fun ProBenefitGroup(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Osto-osio näyttää kaikki laskutuksen tilat ja niiden sallitut toiminnot.
private fun ProPurchaseSection(
    proState: ProState,
    proStateReady: Boolean,
    purchaseQueryFailed: Boolean,
    offer: SelectedOneTimeOffer?,
    productStatusProvider: @Composable () -> BillingProductStatus,
    purchaseFlowInFlight: Boolean,
    isRestoring: Boolean,
    isStartingTrial: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onStartTrial: () -> Unit,
) {
    val productStatus = productStatusProvider()
    val price = offer?.formattedPrice
    if (!proStateReady) {
        if (purchaseQueryFailed) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProStatusMessage(messageRes = R.string.generic_error_unknown)
                ProRestoreButton(isRestoring = isRestoring, onRestore = onRestore)
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (proState.status != ProStatus.PRO_PURCHASED) {
            ProPriceDetails(offer, productStatus, proState.status == ProStatus.TRIAL_NOT_STARTED, onRetry)
        }
        when (proState.status) {
            ProStatus.TRIAL_NOT_STARTED -> Text(stringResource(R.string.onboarding_pro_trial_terms))

            ProStatus.TRIAL_ACTIVE -> {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.pro_status_trial_days,
                            proState.trialDaysRemaining,
                            proState.trialDaysRemaining,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            ProStatus.TRIAL_EXPIRED -> {
                Text(
                    text = stringResource(R.string.pro_status_trial_ended),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            ProStatus.PRO_PURCHASED -> {
                Text(
                    text = stringResource(R.string.pro_page_purchased),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (proState.status != ProStatus.PRO_PURCHASED) {
            Text(stringResource(R.string.pro_trial_ended_body), style = MaterialTheme.typography.bodyMedium)
            if (proState.status == ProStatus.TRIAL_NOT_STARTED) {
                Button(onClick = onStartTrial, modifier = Modifier.fillMaxWidth(), enabled = !isStartingTrial) {
                    Text(stringResource(R.string.pro_start_14_day_trial))
                }
            }
            when {
                productStatus == BillingProductStatus.Available && price != null -> {
                    val purchaseButtonModifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    val purchaseButtonContent: @Composable RowScope.() -> Unit = {
                        if (purchaseFlowInFlight) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.pro_buy_for_price, price))
                    }
                    if (proState.status == ProStatus.TRIAL_NOT_STARTED) {
                        OutlinedButton(
                            onClick = onPurchase,
                            modifier = purchaseButtonModifier,
                            enabled = !purchaseFlowInFlight,
                            content = purchaseButtonContent,
                        )
                    } else {
                        Button(
                            onClick = onPurchase,
                            modifier = purchaseButtonModifier,
                            enabled = !purchaseFlowInFlight,
                            content = purchaseButtonContent,
                        )
                    }
                }

                else -> Unit
            }
        }

        ProRestoreButton(isRestoring = isRestoring, onRestore = onRestore)
    }
}

@Composable
private fun ProRestoreButton(
    isRestoring: Boolean,
    onRestore: () -> Unit,
) {
    TextButton(
        onClick = onRestore,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        enabled = !isRestoring,
        contentPadding = ComponentDimens.FlushTextButtonPadding,
    ) {
        if (isRestoring) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            if (isRestoring) {
                stringResource(R.string.restore_purchases_checking)
            } else {
                stringResource(R.string.restore_purchases)
            },
        )
    }
}

@Composable
private fun ProStatusMessage(messageRes: Int) {
    StatusMessage(
        message = stringResource(messageRes),
        type = messageRes.statusMessageType(),
    )
}

private fun Int.statusMessageType(): StatusMessageType =
    when (this) {
        R.string.pro_restored -> StatusMessageType.Success
        R.string.no_purchases_found,
        R.string.billing_price_refreshing,
        R.string.billing_purchase_cancelled,
        R.string.billing_purchase_pending,
        -> StatusMessageType.Info

        else -> StatusMessageType.Error
    }
