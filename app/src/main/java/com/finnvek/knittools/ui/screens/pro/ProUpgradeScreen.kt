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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    ToolScreenScaffold(title = "KnitTools Pro", onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (proState.status == ProStatus.PRO_PURCHASED) {
                Text(
                    text = "You have KnitTools Pro!",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "All features are unlocked.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Unlock all tools",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                val features = listOf(
                    "Unlimited counter projects",
                    "Full counter history",
                    "Notes per project",
                    "Secondary counter",
                    "Yarn label OCR scanning",
                    "Home screen widget",
                )
                features.forEach { feature ->
                    Text(
                        text = "• $feature",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val priceText = productDetails
                    ?.oneTimePurchaseOfferDetails
                    ?.formattedPrice
                    ?: "..."

                Button(
                    onClick = {
                        (context as? Activity)?.let { activity ->
                            viewModel.purchase(activity)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Upgrade for $priceText")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "One-time purchase. No subscription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
