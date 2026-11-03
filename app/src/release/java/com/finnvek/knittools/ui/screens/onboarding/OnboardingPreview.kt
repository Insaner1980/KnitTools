package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.runtime.Composable
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import java.util.Locale

/** Release builds always use real Play offers and checkout. */
@Suppress("UNUSED_PARAMETER")
object OnboardingPreview {
    fun offer(locale: Locale): SelectedOneTimeOffer? = null

    @Composable
    fun Notice() = Unit

    @Composable
    fun PurchaseDialog(
        price: String,
        onDismiss: () -> Unit,
        onContinue: () -> Unit,
    ) = Unit
}
