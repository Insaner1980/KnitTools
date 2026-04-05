package com.finnvek.knittools.ui.screens.pro

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.ToolScreenScaffold

@Composable
fun ProUpgradeScreen(
    onBack: () -> Unit,
    viewModel: ProUpgradeViewModel = hiltViewModel(),
) {
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    val productDetails by viewModel.productDetails.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ToolScreenScaffold(title = stringResource(R.string.knittools_pro), onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (proState.status == ProStatus.PRO_PURCHASED) {
                Text(
                    text = stringResource(R.string.you_have_pro),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.all_features_unlocked),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.unlock_all_tools),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                val features =
                    listOf(
                        R.string.pro_feature_unlimited_projects,
                        R.string.pro_feature_full_history,
                        R.string.pro_feature_notes,
                        R.string.pro_feature_secondary_counter,
                        R.string.pro_feature_ocr,
                        R.string.pro_feature_widget,
                    )
                features.forEach { featureRes ->
                    Text(
                        text = "• ${stringResource(featureRes)}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice

                Button(
                    onClick = {
                        (context as? Activity)?.let { activity ->
                            viewModel.purchase(activity)
                        }
                    },
                    enabled = price != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (price != null) {
                        Text(stringResource(R.string.upgrade_for_price, price))
                    } else {
                        Text(stringResource(R.string.loading_price))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (price != null) {
                    Text(
                        text = stringResource(R.string.one_time_purchase),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.price_loading_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
