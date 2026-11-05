package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.proGroupOrder
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.CraftIllustration
import com.finnvek.knittools.ui.components.formatOfferFullPrice
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.components.rememberLocaleDateFormat
import com.finnvek.knittools.ui.screens.pro.ProAddsSection
import com.finnvek.knittools.ui.screens.pro.ProFreeForeverSection
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.IllustrationDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import java.util.Date

data class OnboardingPriceState(
    val ready: Boolean = false,
    val pro: ProState = ProState(),
    val trialUnavailable: Boolean = false,
    val offer: SelectedOneTimeOffer? = null,
    val productStatus: BillingProductStatus = BillingProductStatus.Loading,
    val purchaseBusy: Boolean = false,
    val pending: Boolean = false,
) {
    val mode: OnboardingPriceMode
        get() = onboardingPriceMode(ready, pro.status, trialUnavailable, productStatus, offer)
    val confirmedOffer: SelectedOneTimeOffer?
        get() = offer.takeIf { ready && productStatus == BillingProductStatus.Available }
}

@Composable
internal fun JourneyPriceActions(
    price: OnboardingPriceState,
    busy: Boolean,
    actions: OnboardingActions,
) {
    when (price.mode) {
        OnboardingPriceMode.Trial ->
            JourneyPrimaryButton(
                stringResource(R.string.journey_start_trial),
                !busy,
                actions.startTrial,
                Modifier.testTag("onboarding_start_trial"),
            )
        OnboardingPriceMode.Purchase ->
            JourneyPrimaryButton(
                stringResource(R.string.journey_buy_price, price.confirmedOffer?.formattedPrice.orEmpty()),
                !busy && !price.purchaseBusy && !price.pending,
                actions.purchase,
                Modifier.testTag("onboarding_buy_primary"),
            )
        OnboardingPriceMode.Continue ->
            JourneyPrimaryButton(
                stringResource(R.string.journey_continue_app),
                !busy,
                actions.finish,
            )
        OnboardingPriceMode.Loading ->
            JourneyPrimaryButton(
                stringResource(R.string.journey_loading),
                false,
                {},
                Modifier.testTag("onboarding_loading_action"),
            )
        OnboardingPriceMode.Skip -> Unit
    }
    if (price.mode == OnboardingPriceMode.Trial || price.mode == OnboardingPriceMode.Continue) {
        price.confirmedOffer?.let { offer ->
            OutlinedButton(
                onClick = actions.purchase,
                enabled = !busy && !price.purchaseBusy && !price.pending,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_buy_inline"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Text(
                    stringResource(R.string.journey_buy_price, offer.formattedPrice),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
    if (price.mode != OnboardingPriceMode.Continue) {
        CancelButton(
            onClick = actions.finish,
            text = stringResource(R.string.journey_continue_free),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_continue_free"),
        )
    }
}

@Composable
internal fun JourneyPriceContent(
    draft: OnboardingDraft,
    price: OnboardingPriceState,
) {
    if (!price.ready || price.mode == OnboardingPriceMode.Skip) {
        JourneyTitle(stringResource(R.string.journey_continue_title))
        Text(stringResource(R.string.journey_loading), style = MaterialTheme.typography.bodyLarge)
        return
    }
    CraftIllustration(
        R.drawable.onboarding_pro_hero,
        height = IllustrationDimens.CompactHeight,
    )
    JourneyTitle(
        stringResource(
            if (price.mode ==
                OnboardingPriceMode.Trial
            ) {
                R.string.journey_trial_title
            } else {
                R.string.journey_continue_title
            },
        ),
    )
    if (price.mode == OnboardingPriceMode.Trial) {
        Text(stringResource(R.string.journey_trial_terms), style = MaterialTheme.typography.bodyLarge)
    }
    OnboardingPreview.Notice()
    ProAddsSection(order = proGroupOrder(draft.needs))
    price.confirmedOffer?.let { offer -> JourneyOfferDetails(offer) }
    if (price.mode == OnboardingPriceMode.Trial) {
        Text(stringResource(R.string.journey_timeline_title), style = MaterialTheme.typography.titleLarge)
        Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing)) {
            JourneyTimelineRow(stringResource(R.string.journey_today), stringResource(R.string.journey_today_body))
            JourneyTimelineRow(
                stringResource(R.string.journey_three_days),
                stringResource(R.string.journey_three_days_body),
            )
            JourneyTimelineRow(
                stringResource(R.string.journey_day_fourteen),
                stringResource(R.string.journey_day_fourteen_body),
            )
        }
        Text(stringResource(R.string.journey_keep_content), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.journey_after_trial), style = MaterialTheme.typography.bodyLarge)
    }
    if (price.mode == OnboardingPriceMode.Continue) {
        Text(
            pluralStringResource(
                R.plurals.pro_status_trial_days,
                price.pro.trialDaysRemaining,
                price.pro.trialDaysRemaining,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    if (price.pending) {
        Text(
            stringResource(R.string.billing_purchase_pending),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    ProFreeForeverSection()
}

@Composable
private fun JourneyOfferDetails(offer: SelectedOneTimeOffer) {
    Text(
        stringResource(
            if (offer.launchDetails != null) R.string.journey_buy_launch_title else R.string.journey_buy_title,
        ),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        offer.formattedPrice,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(stringResource(R.string.journey_one_time), style = MaterialTheme.typography.bodyLarge)
    offer.launchDetails?.let { launch ->
        val date = rememberLocaleDateFormat("yMMMd").format(Date(launch.endsAtMillis))
        val fullPrice = formatOfferFullPrice(offer, rememberCurrentLocale())
        Text(
            if (fullPrice != null) {
                stringResource(R.string.journey_launch_terms, date, fullPrice)
            } else {
                stringResource(R.string.pro_launch_ends_at, date)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun JourneyTimelineRow(
    label: String,
    body: String,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < OnboardingDimens.JourneyStackedBreakpoint || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        } else {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(2f))
                }
                HorizontalDivider(
                    Modifier.padding(top = ComponentDimens.ContentSpacing),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
