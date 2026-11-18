package com.finnvek.knittools.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

/** Esimerkki säilyttää vain oman käyttöliittymätilansa eikä muuta projektien laskureita. */
@Composable
fun WelcomeExampleCard(
    onDemoUsed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var count by rememberSaveable { mutableIntStateOf(OnboardingDimens.START_COUNT) }
    var taps by rememberSaveable { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.knitToolsColors.cardContainer),
    ) {
        Column(
            modifier = Modifier.padding(OnboardingDimens.ExampleCardPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
        ) {
            BadgePill(text = stringResource(R.string.welcome_example_label), uppercase = false)
            WelcomeExampleVisuals(count)
            CounterImageButton(
                imageRes = R.drawable.counter_plus_button,
                contentDescription = stringResource(R.string.welcome_plus_a11y),
                visualSize = OnboardingDimens.PlusVisualSize,
                onClick = {
                    if (taps == 0) onDemoUsed()
                    taps++
                    count++
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.size(OnboardingDimens.PlusTouchSize).align(Alignment.CenterHorizontally),
            )
            WelcomeExampleCaption(
                count = count,
                taps = taps,
                onUndo = {
                    if (count > OnboardingDimens.START_COUNT) {
                        count--
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
            )
        }
    }
}

@Composable
private fun WelcomeExampleVisuals(count: Int) {
    val description = stringResource(R.string.welcome_demo_a11y, count)
    val swatchRows =
        (OnboardingDimens.START_ROWS + count - OnboardingDimens.START_COUNT).coerceAtMost(
            OnboardingDimens.MAX_ROWS,
        )
    val swatchHeight by animateDpAsState(
        targetValue = OnboardingDimens.SwatchRowHeight * swatchRows,
        animationSpec = tween(OnboardingDimens.GROWTH_DURATION_MILLIS),
        label = "welcomeFabricGrowth",
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        PatternPageSketch(count)
        Column(
            modifier = Modifier.width(OnboardingDimens.SwatchWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        ) {
            Text(
                text = mainCounterLabelText(MainCounterLabelType.ROWS, null).localizedUppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatIntegerForDisplay(count.toLong(), rememberCurrentLocale()),
                style = MaterialTheme.typography.displayMedium,
            )
            FabricSwatch(
                kind = FabricKind.KNIT,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .width(OnboardingDimens.SwatchWidth)
                        .height(swatchHeight)
                        .clip(MaterialTheme.shapes.small)
                        .testTag("welcomeFabricSwatch"),
                stitchScale = OnboardingDimens.FixedStitchScale,
            )
        }
    }
}

@Composable
private fun WelcomeExampleCaption(
    count: Int,
    taps: Int,
    onUndo: () -> Unit,
) {
    val caption =
        when (taps) {
            0 -> stringResource(R.string.welcome_demo_try)
            1 -> stringResource(R.string.welcome_demo_after_first, count)
            else -> stringResource(R.string.welcome_demo_after_second)
        }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = OnboardingDimens.CaptionMinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (taps > 0) {
            TextButton(onClick = onUndo, enabled = count > OnboardingDimens.START_COUNT) {
                Text(stringResource(R.string.welcome_demo_undo))
            }
        }
    }
}
