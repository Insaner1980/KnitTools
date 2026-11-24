package com.finnvek.knittools.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.DEMO_PATTERN_ROWS
import com.finnvek.knittools.domain.model.OnboardingCraft
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingNeed
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.domain.model.PatternDisplayNames
import com.finnvek.knittools.domain.model.demoPatternRow
import com.finnvek.knittools.ui.components.ContinueProjectCard
import com.finnvek.knittools.ui.components.CounterImageButton
import com.finnvek.knittools.ui.components.CraftIllustration
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.CrossStitchIcon
import com.finnvek.knittools.ui.components.IllustrationEntrance
import com.finnvek.knittools.ui.components.cardContainerColor
import com.finnvek.knittools.ui.components.mainCounterLabelText
import com.finnvek.knittools.ui.components.projectNextUpText
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.components.rememberStaggeredEntrance
import com.finnvek.knittools.ui.components.staggeredEntrance
import com.finnvek.knittools.ui.screens.pattern.ReadingLineOverlay
import com.finnvek.knittools.ui.screens.pattern.ReadingLineOverlayActions
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.CrossStitchDimens
import com.finnvek.knittools.ui.theme.IllustrationDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import kotlinx.coroutines.delay

@Composable
internal fun JourneyWelcome(
    usageAnalyticsEnabled: Boolean = false,
    onUsageAnalyticsChanged: (Boolean) -> Unit = {},
) {
    // Kaikki keskitetään: leimalogo on brändin pääkuva, ja kuvitus, otsikko ja hyödyt seuraavat sen alla.
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.LargeContentPadding),
    ) {
        // Leimalogossa lukee KnitTools; sama logo kuin käynnistysruudussa, joten ensivaikutelma jatkuu siitä.
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = null,
            modifier =
                Modifier
                    .size(IllustrationDimens.LogoSize)
                    .staggeredEntrance(rememberStaggeredEntrance(0)),
        )
        journeyIllustrationHeight(IllustrationDimens.WelcomeHeight)?.let { height ->
            CraftIllustration(R.drawable.onboarding_welcome_hero, height = height)
        }
        JourneyTitle(
            stringResource(R.string.journey_welcome_title),
            Modifier.staggeredEntrance(rememberStaggeredEntrance(1)),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.journey_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().staggeredEntrance(rememberStaggeredEntrance(2)),
        )
        // Hyödyt pelkkänä tekstinä: hiusviivoilla erotetut rivit näyttivät napautettavilta listariveiltä.
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
        ) {
            listOf(
                R.string.journey_welcome_point_count,
                R.string.journey_welcome_point_pattern,
                R.string.journey_welcome_point_return,
            ).forEachIndexed { index, point ->
                Text(
                    stringResource(point),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().staggeredEntrance(rememberStaggeredEntrance(index + 3)),
                )
            }
        }
        Text(
            stringResource(R.string.welcome_trust),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().staggeredEntrance(rememberStaggeredEntrance(6)),
        )
        val analyticsLabel = stringResource(R.string.usage_analytics)
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.usage_analytics),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                androidx.compose.material3.Switch(
                    checked = usageAnalyticsEnabled,
                    onCheckedChange = onUsageAnalyticsChanged,
                    modifier = Modifier.semantics { contentDescription = analyticsLabel },
                )
            }
            Text(stringResource(R.string.usage_analytics_description), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun JourneyCraft(
    draft: OnboardingDraft,
    busy: Boolean,
    actions: OnboardingActions,
) {
    var advancing by remember { mutableStateOf(false) }
    LaunchedEffect(advancing, draft.step) {
        if (!advancing) return@LaunchedEffect
        if (draft.step == OnboardingStep.Craft) {
            delay(OnboardingDimens.CRAFT_ADVANCE_DELAY_MILLIS)
            actions.next()
        }
        advancing = false
    }
    JourneyTitle(stringResource(R.string.journey_craft_title))
    Column(
        Modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
    ) {
        OnboardingCraft.entries.forEachIndexed { index, craft ->
            JourneyCraftTile(
                imageRes = craft.imageRes(),
                label = stringResource(craft.label()),
                selected = draft.craftChosen && draft.craft == craft,
                enabled = !busy && !advancing,
                onSelect = {
                    // Valinta näkyy hetken ennen siirtymää; yksi napautus riittää kuten Duolingossa.
                    advancing = true
                    actions.edit { it.copy(craft = craft, craftChosen = true) }
                },
                modifier =
                    Modifier
                        .staggeredEntrance(rememberStaggeredEntrance(index))
                        .testTag("onboarding_craft_${craft.name.lowercase()}"),
            )
        }
    }
    // Rauhoittava apurivi: valinta ei ole lopullinen.
    Text(
        stringResource(R.string.journey_craft_help),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().staggeredEntrance(rememberStaggeredEntrance(OnboardingCraft.entries.size)),
    )
}

@Composable
private fun JourneyCraftTile(
    @DrawableRes imageRes: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(animatedSelectionColor(selected))
            .border(
                OnboardingDimens.SelectionBorderWidth,
                animatedSelectionBorder(selected),
                MaterialTheme.shapes.large,
            ).selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = OnboardingDimens.CraftTileMinHeight)
            .padding(horizontal = ComponentDimens.ContentPadding, vertical = ComponentDimens.StandardSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ContentPadding),
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(OnboardingDimens.CraftTileImageSize),
        )
        Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        JourneySelectionCheck(selected)
    }
}

/** Valinnan taustan pehmeä vaihto; sama kortin väri kuin `cardContainerColor`. */
@Composable
internal fun animatedSelectionColor(selected: Boolean): Color =
    animateColorAsState(
        cardContainerColor(selected),
        animationSpec = tween(IllustrationDimens.SELECTION_MILLIS),
        label = "journeySelection",
    ).value

/** Valitun rivin reunus: pelkkä vaaleampi tausta erottui liian heikosti. */
@Composable
internal fun animatedSelectionBorder(selected: Boolean): Color =
    animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.knitToolsColors.transparentIndicator,
        animationSpec = tween(IllustrationDimens.SELECTION_MILLIS),
        label = "journeySelectionBorder",
    ).value

/** Valintamerkki ponnahtaa näkyviin varatussa tilassa, joten rivin teksti ei hyppää. */
@Composable
private fun JourneySelectionCheck(selected: Boolean) {
    Box(Modifier.size(OnboardingDimens.SelectionCheckSize), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = selected,
            enter =
                scaleIn(spring(dampingRatio = IllustrationDimens.BOUNCE_DAMPING)) +
                    fadeIn(tween(IllustrationDimens.SELECTION_MILLIS)),
            exit = fadeOut(tween(IllustrationDimens.SELECTION_MILLIS)),
        ) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.knitToolsColors.primaryReadable)
        }
    }
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
        OnboardingNeed.entries.forEachIndexed { index, need ->
            val selected = need in draft.needs
            Row(
                Modifier
                    .staggeredEntrance(rememberStaggeredEntrance(index))
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(animatedSelectionColor(selected))
                    .border(
                        OnboardingDimens.SelectionBorderWidth,
                        animatedSelectionBorder(selected),
                        MaterialTheme.shapes.medium,
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
                    stringResource(need.label(draft.craftType)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                JourneySelectionCheck(selected)
            }
        }
    }
    if (draft.needs.isNotEmpty()) {
        // Pelkkä häivytys: tekstin asettelu ei muutu animaation aikana.
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(Unit) { alpha.animateTo(1f, tween(IllustrationDimens.ENTER_MILLIS)) }
        // Kuittaus keskellä valintamerkin kanssa, jotta se luetaan vastauksena valintoihin eikä irrallisena rivinä.
        // Merkki on tekstin yllä: rivissä sisältönsä levyinen teksti raportoi leveyden ylityksen, kuten aiemmin.
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { this.alpha = alpha.value }
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        ) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.knitToolsColors.primaryReadable)
            Text(
                stringResource(R.string.journey_needs_reaction),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
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
        JourneyDemoPattern(draft.craftType, demoPatternRow(draft.demoCount))
        JourneyDemoCounter(draft, busy, edit)
    }
    // Ohje pysyy näkyvissä, kunnes paluukortti nousee: yksi napautus ei vielä näytä lopputulosta.
    // Oranssi on varattu napautettaville kohteille, joten ohje on tavallista tekstiä.
    if (!draft.demoReturnVisible) {
        Text(
            stringResource(R.string.journey_demo_hint),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AnimatedVisibility(
        visible = draft.demoReturnVisible,
        // Paluukortti on demon ahaa-hetki: se nousee näkyviin jousella eikä vain häivyty.
        enter =
            fadeIn(tween(IllustrationDimens.ENTER_MILLIS)) +
                expandVertically() +
                scaleIn(
                    spring(dampingRatio = IllustrationDimens.BOUNCE_DAMPING),
                    initialScale = IllustrationDimens.BOUNCE_SCALE_FROM,
                ),
    ) {
        val requester = remember { BringIntoViewRequester() }
        LaunchedEffect(Unit) {
            delay(IllustrationDimens.ENTER_MILLIS.toLong())
            requester.bringIntoView()
        }
        Column(
            Modifier.bringIntoViewRequester(requester).testTag("onboarding_demo_return"),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
        ) {
            Text(stringResource(R.string.journey_demo_return), style = MaterialTheme.typography.bodyLarge)
            ContinueProjectCard(
                project =
                    CounterProject(
                        name = stringResource(R.string.journey_demo_project_name),
                        count = draft.demoCount,
                        craftType = draft.craftType,
                        mainCounterLabelType = draft.craftType.defaultMainCounterLabelType(),
                    ),
                sessionStatus = null,
                onClick = null,
                onClickLabel = null,
                onOpenCounter = null,
            )
        }
    }
}

/**
 * Ohjepaperi, jonka lukulinja seuraa demon laskuria: linja on sillä rivillä, jota käyttäjä tekee
 * seuraavaksi. Linjaa ei voi vetää, koska sen paikka kertoo laskurin tilan.
 */
@Composable
private fun JourneyDemoPattern(
    craft: CraftType,
    currentRow: Int,
) {
    val rowLines = remember { mutableStateListOf(*Array(DEMO_PATTERN_ROWS) { 0f }) }
    var paperHeight by remember { mutableFloatStateOf(1f) }
    val target = (rowLines[currentRow] / paperHeight).coerceIn(0f, 1f)
    val lineY by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(OnboardingDimens.DEMO_LINE_MILLIS, easing = FastOutSlowInEasing),
        label = "demoReadingLine",
    )
    val rows = craft.demoRows()
    val density = LocalDensity.current
    val paddingPx = with(density) { ComponentDimens.ContentPadding.toPx() }
    val gapPx = with(density) { OnboardingDimens.DemoPatternRowGap.toPx() }
    val repeatRes =
        if (craft == CraftType.CROCHET) R.string.journey_demo_repeat_crochet else R.string.journey_demo_repeat
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.knitToolsColors.paperSurface)
            .onSizeChanged { paperHeight = it.height.toFloat().coerceAtLeast(1f) },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(ComponentDimens.ContentPadding),
            // Väli on lukulinjan kaistan korkuinen, jottei kaista peitä seuraavan rivin yläosaa.
            verticalArrangement = Arrangement.spacedBy(OnboardingDimens.DemoPatternRowGap),
        ) {
            rows.forEachIndexed { index, res ->
                val current = index == currentRow
                Text(
                    stringResource(res),
                    color =
                        if (current) {
                            MaterialTheme.knitToolsColors.paperText
                        } else {
                            MaterialTheme.knitToolsColors.paperText.copy(alpha = UPCOMING_ROW_ALPHA)
                        },
                    style = if (current) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier.onGloballyPositioned {
                            // Lukulinja on rivin alla kuten lukutilassa, ei tekstin päällä.
                            rowLines[index] = paddingPx + it.positionInParent().y + it.size.height + gapPx / 2f
                        },
                )
            }
            // Toistorivi on alaviite: himmein teksti paperilla.
            Text(
                stringResource(repeatRes),
                color = MaterialTheme.knitToolsColors.paperText.copy(alpha = UPCOMING_ROW_ALPHA),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ReadingLineOverlay(
            yFraction = lineY,
            currentRow = null,
            followingCurrentRow = null,
            scale = 1f,
            actions = ReadingLineOverlayActions({}, {}, {}, {}),
            modifier = Modifier.matchParentSize().clearAndSetSemantics { },
        )
    }
}

@Composable
private fun JourneyDemoCounter(
    draft: OnboardingDraft,
    busy: Boolean,
    edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    val locale = rememberCurrentLocale()
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CounterImageButton(
            imageRes = R.drawable.counter_minus_button,
            contentDescription = stringResource(R.string.journey_demo_decrease),
            visualSize = OnboardingDimens.JourneyDemoButtonSize,
            onClick = { edit { it.changeDemo(-1) } },
            enabled = !busy && draft.demoCount > 0,
            modifier = Modifier.size(OnboardingDimens.JourneyDemoTouchSize),
        )
        Column(
            Modifier.weight(1f).widthIn(min = OnboardingDimens.JourneyDemoNumberMinWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                mainCounterLabelText(draft.craftType.defaultMainCounterLabelType(), null),
                style = MaterialTheme.typography.labelLarge,
            )
            AnimatedContent(
                targetState = draft.demoCount,
                transitionSpec = {
                    val up = targetState > initialState
                    val spec = tween<IntOffset>(OnboardingDimens.DEMO_NUMBER_MILLIS)
                    (
                        slideInVertically(spec) { if (up) it else -it } +
                            fadeIn(tween(OnboardingDimens.DEMO_NUMBER_MILLIS))
                    ) togetherWith
                        (
                            slideOutVertically(spec) { if (up) -it else it } +
                                fadeOut(tween(OnboardingDimens.DEMO_NUMBER_MILLIS))
                        ) using SizeTransform(clip = true)
                },
                label = "demoCount",
            ) { count ->
                Text(
                    formatIntegerForDisplay(count.toLong(), locale),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .testTag("onboarding_demo_count")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        CounterImageButton(
            imageRes = R.drawable.counter_plus_button,
            contentDescription = stringResource(R.string.journey_demo_increase),
            visualSize = OnboardingDimens.JourneyDemoButtonSize,
            onClick = { edit { it.changeDemo(1) } },
            enabled = !busy,
            modifier = Modifier.size(OnboardingDimens.JourneyDemoTouchSize).testTag("onboarding_demo_plus"),
        )
    }
    // Kumoa omalla rivillään kuten oikeassa laskurissa: luvun alla se näytti osalta lukua.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CounterImageButton(
            imageRes = R.drawable.counter_undo_button,
            contentDescription = stringResource(R.string.welcome_demo_undo),
            visualSize = OnboardingDimens.JourneyDemoUndoSize,
            onClick = { edit { it.undoDemo() } },
            enabled = !busy && draft.demoHistory.isNotEmpty(),
            modifier = Modifier.size(OnboardingDimens.JourneyDemoUndoTouchSize).testTag("onboarding_demo_undo"),
        )
    }
}

@Composable
internal fun JourneyReady(project: CounterProject) {
    journeyIllustrationHeight(IllustrationDimens.Height)?.let { height ->
        CraftIllustration(R.drawable.onboarding_project_ready, height = height, entrance = IllustrationEntrance.Bounce)
    }
    JourneyTitle(
        stringResource(R.string.journey_ready_title),
        Modifier.staggeredEntrance(rememberStaggeredEntrance(2)),
    )
    // Ei-interaktiivinen yhteenveto hiusviivojen välissä: jatka-kortti näytti napautettavalta, ja "Row 0"
    // luki lähtöviivana. Seuraava rivi ja liitetty ohje kertovat, että kaikki on valmiina.
    Column(
        Modifier.fillMaxWidth().staggeredEntrance(rememberStaggeredEntrance(3)),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(project.name, style = MaterialTheme.typography.titleLarge)
        JourneyReadyRow(
            CrossStitchGlyph.COUNTER,
            projectNextUpText(project.count, project.mainCounterLabelType),
        )
        project.patternName?.let { pdfName ->
            JourneyReadyRow(
                CrossStitchGlyph.PATTERN,
                stringResource(R.string.journey_ready_pattern, PatternDisplayNames.forDisplay(pdfName)),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    Text(
        stringResource(R.string.journey_ready_body),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().staggeredEntrance(rememberStaggeredEntrance(5)),
    )
}

@Composable
private fun JourneyReadyRow(
    glyph: CrossStitchGlyph,
    text: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrossStitchIcon(glyph, Modifier.size(CrossStitchDimens.RowGlyphSize))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun JourneyBenefitCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.knitToolsColors.cardContainer,
        shape = MaterialTheme.shapes.large,
    ) {
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
        if (craft == CraftType.CROCHET) R.string.project_name_placeholder_crochet else R.string.journey_example_name,
    )

private const val UPCOMING_ROW_ALPHA = 0.62f

private fun CraftType.demoRows(): List<Int> =
    if (this == CraftType.CROCHET) {
        listOf(R.string.journey_crochet_row_one, R.string.journey_crochet_row_two, R.string.journey_crochet_row_three)
    } else {
        listOf(R.string.journey_knit_row_one, R.string.journey_knit_row_two, R.string.journey_knit_row_three)
    }

private fun OnboardingCraft.imageRes(): Int =
    when (this) {
        OnboardingCraft.Knitting -> R.drawable.onboarding_craft_knit
        OnboardingCraft.Crochet -> R.drawable.onboarding_craft_crochet
        OnboardingCraft.Both -> R.drawable.onboarding_craft_both
    }

private fun OnboardingCraft.label(): Int =
    when (this) {
        OnboardingCraft.Knitting -> R.string.journey_knit
        OnboardingCraft.Crochet -> R.string.journey_crochet
        OnboardingCraft.Both -> R.string.journey_both
    }

private fun OnboardingNeed.label(craft: CraftType): Int =
    when (this) {
        OnboardingNeed.Counting -> {
            if (craft == CraftType.CROCHET) R.string.journey_need_counting_crochet else R.string.journey_need_counting
        }

        OnboardingNeed.Pattern -> {
            R.string.journey_need_pattern
        }

        OnboardingNeed.Returning -> {
            R.string.journey_need_returning
        }

        OnboardingNeed.Yarn -> {
            R.string.journey_need_yarn
        }

        OnboardingNeed.Projects -> {
            R.string.journey_need_projects
        }
    }
