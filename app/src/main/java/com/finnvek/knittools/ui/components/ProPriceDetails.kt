package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.ui.theme.ComponentDimens
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@Composable
internal fun ProPriceDetails(
    offer: SelectedOneTimeOffer?,
    productStatus: BillingProductStatus,
    trialAvailable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    // Pro-sivulla hinta on näkyvä otsikko ja kertaostoteksti sen alla; kehotteissa tiivis rivi.
    prominent: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
        if (!prominent) Text(stringResource(R.string.one_time_purchase), style = MaterialTheme.typography.bodySmall)
        when {
            productStatus == BillingProductStatus.Available && offer != null -> {
                val launch = offer.launchDetails
                Text(
                    stringResource(
                        if (launch == null) R.string.pro_current_price else R.string.pro_launch_price,
                        offer.formattedPrice,
                    ),
                    style =
                        if (prominent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                )
                if (prominent) {
                    Text(stringResource(R.string.one_time_purchase), style = MaterialTheme.typography.bodyMedium)
                }
                if (launch != null) {
                    val dateFormat = rememberLocaleDateFormat("yMMMdssz", includeTime = true)
                    Text(stringResource(R.string.pro_launch_ends_at, dateFormat.format(Date(launch.endsAtMillis))))
                    formatOfferFullPrice(offer, rememberCurrentLocale())?.let {
                        Text(stringResource(R.string.pro_price_after_launch, it))
                    }
                    if (trialAvailable) Text(stringResource(R.string.pro_trial_does_not_reserve_price))
                }
            }
            productStatus == BillingProductStatus.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(ComponentDimens.StandardIconSize))
                Text(stringResource(R.string.loading_price))
            }
            else -> {
                Text(
                    stringResource(
                        if (trialAvailable) R.string.pro_price_unavailable_trial else R.string.pro_price_unavailable,
                    ),
                )
                TextButton(onClick = onRetry, contentPadding = ComponentDimens.FlushTextButtonPadding) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

internal fun formatOfferFullPrice(
    offer: SelectedOneTimeOffer,
    locale: Locale,
): String? {
    val fullPrice = offer.launchDetails?.fullPriceMicros?.takeIf { it > offer.priceAmountMicros } ?: return null
    val currency =
        try {
            Currency.getInstance(offer.priceCurrencyCode)
        } catch (_: IllegalArgumentException) {
            return null
        }
    val digits = currency.defaultFractionDigits.takeIf { it >= 0 } ?: return null
    return NumberFormat
        .getCurrencyInstance(locale)
        .apply {
            this.currency = currency
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(BigDecimal.valueOf(fullPrice, 6))
}
