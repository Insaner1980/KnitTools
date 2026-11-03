package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.LaunchOfferDetails
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.ui.components.ConfirmationDialog
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale

/** Presentation-only fixture for the separate, offline onboarding preview app. */
object OnboardingPreview {
    fun offer(locale: Locale): SelectedOneTimeOffer? {
        if (!BuildConfig.ONBOARDING_TEST) return null
        val start = ZonedDateTime.of(2026, 11, 3, 0, 0, 0, 0, ZoneId.of("Europe/Helsinki"))
        val formattedPrice =
            NumberFormat
                .getCurrencyInstance(locale)
                .apply {
                    currency = Currency.getInstance("EUR")
                }.format(9.99)
        return SelectedOneTimeOffer(
            formattedPrice = formattedPrice,
            offerToken = "preview-only-no-checkout",
            priceAmountMicros = 9_990_000L,
            priceCurrencyCode = "EUR",
            offerId = "launch",
            launchDetails =
                LaunchOfferDetails(
                    startsAtMillis = start.toInstant().toEpochMilli(),
                    endsAtMillis = start.plusMonths(1).toInstant().toEpochMilli(),
                    fullPriceMicros = 14_990_000L,
                ),
        )
    }

    @Composable
    fun Notice() {
        if (BuildConfig.ONBOARDING_TEST) {
            Text(
                stringResource(R.string.onboarding_preview_notice),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("onboarding_preview_notice"),
            )
        }
    }

    @Composable
    fun PurchaseDialog(
        price: String,
        onDismiss: () -> Unit,
        onContinue: () -> Unit,
    ) {
        if (BuildConfig.ONBOARDING_TEST) {
            ConfirmationDialog(
                title = stringResource(R.string.onboarding_preview_purchase_title),
                message = stringResource(R.string.onboarding_preview_purchase_message, price),
                confirmText = stringResource(R.string.journey_continue_app),
                onConfirm = onContinue,
                onDismiss = onDismiss,
            )
        }
    }
}
