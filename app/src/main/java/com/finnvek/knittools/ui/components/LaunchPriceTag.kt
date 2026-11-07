package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.LaunchPriceTagDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/**
 * Ommeltu hintalappu: hinta, julkaisualennus ja ostopainike yhdessä rajatussa kokonaisuudessa.
 * Värit tulevat kudotusta PRO-merkistä, ja katkoviivareuna muistuttaa neuleen kangasmerkkiä.
 * Ilman voimassa olevaa julkaisutarjousta lappu näyttää vain hinnan ja kertaostotekstin.
 */
@Composable
fun LaunchPriceTag(
    offer: SelectedOneTimeOffer,
    modifier: Modifier = Modifier,
    onBuy: (() -> Unit)? = null,
    buyEnabled: Boolean = true,
    buyInFlight: Boolean = false,
    buyTestTag: String? = null,
) {
    val colors = MaterialTheme.knitToolsColors
    val locale = rememberCurrentLocale()
    val launch = offer.launchDetails
    val fullPrice = formatOfferFullPrice(offer, locale)
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colors.proMarkerContainer)
            .padding(LaunchPriceTagDimens.OuterInset)
            .dashedOutline(colors.proMarkerStitch, LaunchPriceTagDimens.Stitch)
            .padding(LaunchPriceTagDimens.InnerPadding),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (launch != null) R.string.price_tag_launch_label else R.string.price_tag_label),
                style = MaterialTheme.typography.labelLarge,
                color = colors.proMarkerContent,
                modifier = Modifier.weight(1f),
            )
            savingsPercent(offer)?.let { percent ->
                // Prosenttimuoto seuraa käyttöliittymän kieltä, ei laitteen aluetta: englanti suomalaisella
                // alueella antoi muodon "33 %".
                val languageLocale = Locale.forLanguageTag(locale.language)
                val percentText = NumberFormat.getPercentInstance(languageLocale).format(percent)
                BadgePill(
                    text = stringResource(R.string.price_tag_save, percentText),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    uppercase = false,
                )
            }
        }
        FlowRow(
            itemVerticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        ) {
            Text(offer.formattedPrice, style = MaterialTheme.typography.displaySmall, color = colors.proMarkerContent)
            if (launch != null && fullPrice != null) {
                val regularDescription = stringResource(R.string.price_tag_regular_a11y, fullPrice)
                Text(
                    fullPrice,
                    style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.LineThrough),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(bottom = ComponentDimens.CompactSpacing)
                            .clearAndSetSemantics { contentDescription = regularDescription },
                )
            }
        }
        Text(
            if (launch != null) {
                val date = rememberLocaleDateFormat("yMMMdssz", includeTime = true).format(Date(launch.endsAtMillis))
                stringResource(R.string.price_tag_launch_terms, date)
            } else {
                stringResource(R.string.one_time_purchase)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.proMarkerContent,
        )
        onBuy?.let { buy ->
            Button(
                onClick = buy,
                enabled = buyEnabled && !buyInFlight,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = ComponentDimens.FormSheetActionMinHeight)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .then(if (buyTestTag != null) Modifier.testTag(buyTestTag) else Modifier),
            ) {
                if (buyInFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ComponentDimens.StandardIconSize),
                        strokeWidth = LaunchPriceTagDimens.StitchWidth,
                    )
                    Spacer(Modifier.width(ComponentDimens.StandardSpacing))
                }
                Text(
                    stringResource(R.string.pro_buy_for_price, offer.formattedPrice),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Julkaisualennuksen osuus täydestä hinnasta, tai null ilman julkaisutarjousta. */
internal fun savingsPercent(offer: SelectedOneTimeOffer): Double? {
    val full = offer.launchDetails?.fullPriceMicros?.takeIf { it > offer.priceAmountMicros } ?: return null
    val ratio = (full - offer.priceAmountMicros).toDouble() / full
    // Pyöristetään kokonaiseksi prosentiksi, jotta näytetty luku vastaa todellista alennusta.
    return Math.round(ratio * PERCENT) / PERCENT
}

private const val PERCENT = 100.0
