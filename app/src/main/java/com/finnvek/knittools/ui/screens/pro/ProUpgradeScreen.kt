package com.finnvek.knittools.ui.screens.pro

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.StatusMessage
import com.finnvek.knittools.ui.components.StatusMessageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeScreen(
    onBack: () -> Unit,
    onPurchase: (Activity) -> Unit,
    viewModelProvider: @Composable () -> ProUpgradeViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    val selectedOffer by viewModel.selectedOffer.collectAsStateWithLifecycle()
    val productStatus by viewModel.productStatus.collectAsStateWithLifecycle()
    val statusMessageRes by viewModel.statusMessageRes.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.knittools_pro)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
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
            ProIntroduction()
            ProBenefits()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ProPurchaseSection(
                proState = proState,
                price = selectedOffer?.formattedPrice,
                productStatusProvider = { productStatus },
                isRestoring = isRestoring,
                onPurchase = { (context as? Activity)?.let(onPurchase) },
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
private fun ProIntroduction() {
    Text(
        text = stringResource(R.string.pro_page_intro),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stringResource(R.string.pro_page_trust),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProBenefits() {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ProBenefitGroup(
            title = stringResource(R.string.pro_group_projects_title),
            body = stringResource(R.string.pro_group_projects_body),
        )
        ProBenefitGroup(
            title = stringResource(R.string.pro_group_workflow_title),
            body = stringResource(R.string.pro_group_workflow_body),
        )
        ProBenefitGroup(
            title = stringResource(R.string.pro_group_insights_title),
            body = stringResource(R.string.pro_group_insights_body),
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
private fun ProPurchaseSection(
    proState: ProState,
    price: String?,
    productStatusProvider: @Composable () -> BillingProductStatus,
    isRestoring: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onStartTrial: () -> Unit,
) {
    val productStatus = productStatusProvider()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (proState.status) {
            ProStatus.TRIAL_NOT_STARTED -> {
                Button(onClick = onStartTrial, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.pro_start_14_day_trial))
                }
            }

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
            when {
                productStatus == BillingProductStatus.Available && price != null -> {
                    if (proState.status == ProStatus.TRIAL_NOT_STARTED) {
                        OutlinedButton(onClick = onPurchase, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.pro_buy_for_price, price))
                        }
                    } else {
                        Button(onClick = onPurchase, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.pro_buy_for_price, price))
                        }
                    }
                }

                productStatus is BillingProductStatus.Unavailable -> {
                    Text(
                        text = stringResource(R.string.pro_price_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }

                else -> {
                    Row {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.loading_price))
                    }
                }
            }
            Text(
                text = stringResource(R.string.one_time_purchase),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = onRestore, enabled = !isRestoring) {
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
        R.string.billing_purchase_cancelled,
        R.string.billing_purchase_pending,
        -> StatusMessageType.Info

        else -> StatusMessageType.Error
    }
