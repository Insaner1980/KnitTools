package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.OnboardingCraft
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingNeed
import com.finnvek.knittools.ui.components.ContinueProjectCard
import com.finnvek.knittools.ui.components.CounterImageButton
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.StitchProgressRow
import com.finnvek.knittools.ui.components.mainCounterLabelText
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.screens.pattern.ReadingLineOverlay
import com.finnvek.knittools.ui.screens.pattern.ReadingLineOverlayActions
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
internal fun JourneyWelcome(draft: OnboardingDraft) {
    JourneyTitle(stringResource(R.string.journey_welcome_title))
    Text(stringResource(R.string.journey_welcome_body), style = MaterialTheme.typography.bodyLarge)
    JourneyFabric(draft.craftType, rows = 5)
    Text(stringResource(R.string.welcome_trust), style = MaterialTheme.typography.bodyLarge)
}

@Composable
internal fun JourneyNeeds(
    draft: OnboardingDraft,
    busy: Boolean,
    edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    JourneyTitle(stringResource(R.string.journey_needs_title))
    Text(stringResource(R.string.journey_needs_help), style = MaterialTheme.typography.bodyLarge)
    Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
        OnboardingNeed.entries.forEach { need ->
            val selected = need in draft.needs
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (selected) {
                            MaterialTheme.knitToolsColors.actionContainer
                        } else {
                            MaterialTheme.knitToolsColors.cardContainer
                        },
                    ).toggleable(
                        selected,
                        enabled = !busy,
                        role = Role.Checkbox,
                        onValueChange = { edit { it.toggleNeed(need) } },
                    ).heightIn(min = ComponentDimens.SegmentedItemMinHeight)
                    .padding(ComponentDimens.ContentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
            ) {
                Text(
                    stringResource(need.label()),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.knitToolsColors.primaryReadable)
            }
        }
    }
    Text(stringResource(R.string.journey_craft_question), style = MaterialTheme.typography.bodyLarge)
    SegmentedToggle(
        options =
            listOf(
                stringResource(R.string.journey_knit),
                stringResource(R.string.journey_crochet),
                stringResource(R.string.journey_both),
            ),
        selectedIndex = draft.craft.ordinal,
        onSelect = { index -> if (!busy) edit { it.copy(craft = OnboardingCraft.entries[index]) } },
    )
}

@Composable
internal fun JourneyCounter(
    draft: OnboardingDraft,
    busy: Boolean,
    edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    val crochet = draft.craftType == CraftType.CROCHET
    JourneyTitle(
        stringResource(if (crochet) R.string.journey_counter_crochet_title else R.string.journey_counter_title),
    )
    JourneyBenefitCard {
        JourneyFabric(draft.craftType, rows = 4)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                mainCounterLabelText(draft.craftType.defaultMainCounterLabelType(), null),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                formatIntegerForDisplay(draft.demoCount.toLong(), rememberCurrentLocale()),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.testTag("onboarding_demo_count").semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CounterImageButton(
                    imageRes = R.drawable.counter_minus_button,
                    contentDescription = stringResource(R.string.journey_demo_decrease),
                    visualSize = OnboardingDimens.JourneyDemoCounterSize,
                    onClick = { edit { it.changeDemo(-1) } },
                    enabled = !busy && draft.demoCount > 0,
                    modifier = Modifier.size(OnboardingDimens.PlusTouchSize),
                )
                CounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    contentDescription = stringResource(R.string.journey_demo_increase),
                    visualSize = OnboardingDimens.JourneyDemoCounterSize,
                    onClick = { edit { it.changeDemo(1) } },
                    enabled = !busy,
                    modifier = Modifier.size(OnboardingDimens.PlusTouchSize).testTag("onboarding_demo_plus"),
                )
            }
            CounterImageButton(
                imageRes = R.drawable.counter_undo_button,
                contentDescription = stringResource(R.string.welcome_demo_undo),
                visualSize = OnboardingDimens.JourneyDemoUndoSize,
                onClick = { edit { it.undoDemo() } },
                enabled = !busy && draft.demoHistory.isNotEmpty(),
                modifier = Modifier.size(OnboardingDimens.JourneyDemoCounterSize).testTag("onboarding_demo_undo"),
            )
        }
    }
    Text(stringResource(R.string.journey_demo_saved), style = MaterialTheme.typography.bodyLarge)
}

@Composable
internal fun JourneyBenefits(draft: OnboardingDraft) {
    JourneyTitle(stringResource(R.string.journey_benefits_title))
    val needs =
        draft.needs
            .ifEmpty { listOf(OnboardingNeed.Counting, OnboardingNeed.Pattern, OnboardingNeed.Yarn) }
            .map { if (it == OnboardingNeed.Returning) OnboardingNeed.Counting else it }
            .distinct()
            .take(3)
    needs.forEach { need ->
        when (need) {
            OnboardingNeed.Counting, OnboardingNeed.Returning ->
                JourneyBenefitCard {
                    Text(
                        stringResource(R.string.welcome_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ContinueProjectCard(
                        project =
                            CounterProject(
                                name = journeyExampleName(draft.craftType),
                                count = draft.demoCount,
                                targetRows = 120,
                                craftType = draft.craftType,
                                mainCounterLabelType = draft.craftType.defaultMainCounterLabelType(),
                            ),
                        sessionStatus = null,
                        onClick = null,
                        onClickLabel = null,
                        onOpenCounter = null,
                    )
                }
            OnboardingNeed.Pattern ->
                JourneyBenefitCard {
                    Text(
                        stringResource(R.string.journey_pattern_benefit),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    JourneyPatternExample(draft.craftType)
                }
            OnboardingNeed.Yarn ->
                JourneyBenefitCard {
                    Text(
                        stringResource(R.string.journey_yarn_benefit),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.journey_yarn_example), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.journey_yarn_result), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.journey_yarn_total), style = MaterialTheme.typography.bodyLarge)
                }
            OnboardingNeed.Projects ->
                JourneyBenefitCard {
                    Text(
                        stringResource(R.string.journey_projects_benefit),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.journey_projects_body), style = MaterialTheme.typography.bodyLarge)
                }
        }
    }
}

@Composable
private fun JourneyPatternExample(craft: CraftType) {
    var paperTop by remember { mutableFloatStateOf(0f) }
    var paperHeight by remember { mutableFloatStateOf(1f) }
    var secondBottom by remember { mutableFloatStateOf(0f) }
    var manualY by remember { mutableFloatStateOf(Float.NaN) }
    val automaticY = ((secondBottom - paperTop) / paperHeight).coerceIn(0f, 1f)
    val color = MaterialTheme.knitToolsColors.paperInk
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.knitToolsColors.paperSurface)
            .onGloballyPositioned {
                paperTop = it.positionInRoot().y
                paperHeight =
                    it.size.height
                        .toFloat()
                        .coerceAtLeast(1f)
            },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(ComponentDimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
        ) {
            Text(
                stringResource(
                    if (craft ==
                        CraftType.CROCHET
                    ) {
                        R.string.journey_crochet_row_one
                    } else {
                        R.string.journey_knit_row_one
                    },
                ),
                color = color,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    if (craft ==
                        CraftType.CROCHET
                    ) {
                        R.string.journey_crochet_row_two
                    } else {
                        R.string.journey_knit_row_two
                    },
                ),
                color = color,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.onGloballyPositioned { secondBottom = it.positionInRoot().y + it.size.height },
            )
            Spacer(Modifier.height(ComponentDimens.StandardSpacing))
            Text(
                stringResource(
                    if (craft ==
                        CraftType.CROCHET
                    ) {
                        R.string.journey_crochet_row_three
                    } else {
                        R.string.journey_knit_row_three
                    },
                ),
                color = color,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        ReadingLineOverlay(
            yFraction = if (manualY.isNaN()) automaticY else manualY,
            currentRow = null,
            followingCurrentRow = null,
            scale = 1f,
            actions = ReadingLineOverlayActions({}, { manualY = it }, { manualY = it }, {}),
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun JourneyFabric(
    craft: CraftType,
    rows: Int,
) {
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(rows) { row ->
            StitchProgressRow(
                total = 8,
                done = 8,
                craft = craft,
                color = if (row == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
internal fun JourneyBenefitCard(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.knitToolsColors.cardContainer, shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.fillMaxWidth().padding(ComponentDimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
        ) {
            content()
        }
    }
}

@Composable
internal fun journeyExampleName(craft: CraftType): String =
    stringResource(
        if (craft ==
            CraftType.CROCHET
        ) {
            R.string.project_name_placeholder_crochet
        } else {
            R.string.journey_example_name
        },
    )

private fun OnboardingNeed.label(): Int =
    when (this) {
        OnboardingNeed.Counting -> R.string.journey_need_counting
        OnboardingNeed.Pattern -> R.string.journey_need_pattern
        OnboardingNeed.Returning -> R.string.journey_need_returning
        OnboardingNeed.Yarn -> R.string.journey_need_yarn
        OnboardingNeed.Projects -> R.string.journey_need_projects
    }
