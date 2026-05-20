package com.finnvek.knittools.ui.screens.pro

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.BillingUserMessage
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.StatusMessage
import com.finnvek.knittools.ui.components.StatusMessageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeScreen(
    onBack: () -> Unit,
    onPurchase: (Activity) -> Unit,
    viewModel: ProUpgradeViewModel = hiltViewModel(),
) {
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    val productDetails by viewModel.productDetails.collectAsStateWithLifecycle()
    val productStatus by viewModel.productStatus.collectAsStateWithLifecycle()
    val statusMessageRes by viewModel.statusMessageRes.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            ProHeroBanner(backgroundColor = backgroundColor)

            // Sisältö
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (proState.status == ProStatus.PRO_PURCHASED) {
                    ProPurchasedContent()
                } else {
                    ProUpgradeContent(
                        price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice,
                        productStatus = productStatus,
                        isRestoring = isRestoring,
                        onPurchase = {
                            (context as? Activity)?.let(onPurchase)
                        },
                        onRestore = { viewModel.restorePurchases() },
                    )
                }
                statusMessageRes?.let { messageRes ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ProStatusMessage(messageRes = messageRes)
                }
            }
        }
    }
}

@Composable
private fun ProHeroBanner(backgroundColor: Color) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(240.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.pro_upgrade),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        // Gradient-overlay: läpinäkyvä ylhäällä → taustaväri alhaalla
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    backgroundColor.copy(alpha = 0.4f),
                                    backgroundColor,
                                ),
                            startY = 0f,
                        ),
                    ),
        )
    }
}

@Composable
private fun ProPurchasedContent() {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.you_have_pro),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.all_features_unlocked),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ProUpgradeContent(
    price: String?,
    productStatus: BillingProductStatus,
    isRestoring: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    // Otsikko
    Text(
        text = stringResource(R.string.unlock_all_tools),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))

    ProFeatureList()

    Spacer(modifier = Modifier.height(32.dp))

    PurchaseButton(
        price = price,
        enabled = productStatus == BillingProductStatus.Available && price != null,
        unavailable = productStatus is BillingProductStatus.Unavailable,
        onPurchase = onPurchase,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(productStatus.purchaseHelperMessageRes(hasPrice = price != null)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Palauta ostokset
    TextButton(
        onClick = onRestore,
        enabled = !isRestoring,
    ) {
        if (isRestoring) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text =
                if (isRestoring) {
                    stringResource(R.string.restore_purchases_checking)
                } else {
                    stringResource(R.string.restore_purchases)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ProStatusMessage(messageRes: Int) {
    StatusMessage(
        message = stringResource(messageRes),
        type = messageRes.statusMessageType(),
    )
}

@Composable
private fun ProFeatureList() {
    val features =
        listOf(
            R.string.pro_feature_unlimited_projects,
            R.string.pro_feature_full_history,
            R.string.pro_feature_notes,
            R.string.pro_feature_secondary_counter,
            R.string.pro_feature_multiple_counters,
            R.string.pro_feature_row_reminders,
            R.string.pro_feature_progress_photos,
            R.string.pro_feature_unlimited_yarn,
            R.string.pro_feature_ocr,
            R.string.pro_feature_ai_features,
            R.string.pro_feature_voice_commands,
            R.string.pro_feature_voice_live,
            R.string.pro_feature_widget,
            R.string.pro_feature_pattern_camera_scan,
        )
    features.forEach { featureRes ->
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(featureRes),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun PurchaseButton(
    price: String?,
    enabled: Boolean,
    unavailable: Boolean,
    onPurchase: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    brush =
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ),
                    shape = MaterialTheme.shapes.large,
                ).then(
                    if (enabled) Modifier.clickable(onClick = onPurchase) else Modifier,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text =
                if (enabled && price != null) {
                    stringResource(R.string.upgrade_for_price, price)
                } else if (unavailable) {
                    stringResource(R.string.billing_purchase_unavailable_button)
                } else {
                    stringResource(R.string.loading_price)
                },
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun BillingProductStatus.purchaseHelperMessageRes(hasPrice: Boolean): Int =
    when (this) {
        BillingProductStatus.Available -> {
            if (hasPrice) R.string.one_time_purchase else R.string.price_loading_hint
        }

        BillingProductStatus.Loading -> R.string.price_loading_hint
        is BillingProductStatus.Unavailable -> message.toMessageRes()
    }

private fun BillingUserMessage.toMessageRes(): Int =
    when (this) {
        BillingUserMessage.PURCHASE_CANCELLED -> R.string.billing_purchase_cancelled
        BillingUserMessage.PURCHASE_UNAVAILABLE -> R.string.billing_purchase_unavailable
        BillingUserMessage.PURCHASE_NETWORK_ERROR -> R.string.billing_purchase_network_error
        BillingUserMessage.PURCHASE_FAILED -> R.string.billing_purchase_failed
        BillingUserMessage.ALREADY_OWNED_RESTORE_FAILED -> R.string.billing_already_owned_restore_failed
    }

private fun Int.statusMessageType(): StatusMessageType =
    when (this) {
        R.string.pro_restored -> StatusMessageType.Success
        R.string.no_purchases_found,
        R.string.billing_purchase_cancelled,
        -> StatusMessageType.Info

        else -> StatusMessageType.Error
    }
