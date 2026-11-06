package com.finnvek.knittools.ui.screens.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.lerp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.OnboardingNeed
import com.finnvek.knittools.ui.components.CraftIllustration
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.CrossStitchIcon
import com.finnvek.knittools.ui.components.dashedOutline
import com.finnvek.knittools.ui.components.rememberStaggeredEntrance
import com.finnvek.knittools.ui.components.staggeredEntrance
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.IllustrationDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Laatan kertaluontoinen elävä yksityiskohta; useimmat laatat ovat staattisia. */
internal enum class ToolDetail { None, Press, ReadingLine, RisingBars }

internal enum class JourneyTool(
    val glyph: CrossStitchGlyph,
    @param:StringRes val label: Int,
    @param:StringRes val body: Int,
    val detail: ToolDetail = ToolDetail.None,
) {
    Counter(
        CrossStitchGlyph.COUNTER,
        R.string.journey_tool_counter,
        R.string.journey_tool_counter_body,
        ToolDetail.Press,
    ),
    Pattern(
        CrossStitchGlyph.PATTERN,
        R.string.journey_tool_pattern,
        R.string.journey_tool_pattern_body,
        ToolDetail.ReadingLine,
    ),
    Yarn(CrossStitchGlyph.YARN, R.string.my_yarn_title, R.string.journey_tool_yarn_body),
    Calculators(
        CrossStitchGlyph.CALCULATOR,
        R.string.guide_tools_calculators_title,
        R.string.journey_tool_calculators_body,
    ),
    Insights(CrossStitchGlyph.CHART, R.string.tab_insights, R.string.journey_tool_insights_body, ToolDetail.RisingBars),
    WorkTime(CrossStitchGlyph.CLOCK, R.string.onboarding_pro_free_time, R.string.journey_tool_time_body),
    Photos(CrossStitchGlyph.PHOTOS, R.string.project_content_photos, R.string.journey_tool_photos_body),
    Notes(CrossStitchGlyph.NOTES, R.string.project_content_notes, R.string.journey_tool_notes_body),
    Reminders(CrossStitchGlyph.REMINDER, R.string.reminders, R.string.journey_tool_reminders_body),
}

/** Needs-vaiheen valinta vastaa työkalua, joka korostetaan ja nostetaan ruudukon alkuun. */
internal fun OnboardingNeed.tool(): JourneyTool =
    when (this) {
        // Paluun hoitaa laskuri ja sen jatka-kortti, jotka käyttäjä juuri näki demossa.
        OnboardingNeed.Counting, OnboardingNeed.Returning -> JourneyTool.Counter
        OnboardingNeed.Pattern -> JourneyTool.Pattern
        OnboardingNeed.Yarn -> JourneyTool.Yarn
        OnboardingNeed.Projects -> JourneyTool.Insights
    }

/** Valitut työkalut valintajärjestyksessä; jokainen valinta näkyy, rajaa ei ole. */
internal fun pickedJourneyTools(needs: List<OnboardingNeed>): List<JourneyTool> = needs.map { it.tool() }.distinct()

/** Ruudukon järjestys: valitut ensin, sitten muut perusjärjestyksessä. */
internal fun orderedJourneyTools(needs: List<OnboardingNeed>): List<JourneyTool> {
    val picked = pickedJourneyTools(needs)
    return picked + (JourneyTool.entries - picked.toSet())
}

/**
 * Työkalupakki demon jälkeen: kuvitus ja kaikki työkalut samassa kahden sarakkeen ruudukossa.
 * Needs-vaiheessa valitut ovat ensin ja korostettuina oranssilla reunuksella, joten ruudun tiheys ja
 * laajuuden tuntu säilyvät valinnoista riippumatta.
 */
@Composable
internal fun JourneyToolbox(
    craftType: CraftType,
    needs: List<OnboardingNeed>,
) {
    val picked = pickedJourneyTools(needs).toSet()
    CraftIllustration(
        R.drawable.onboarding_toolbox_hero,
        height = IllustrationDimens.WelcomeHeight,
    )
    JourneyTitle(stringResource(R.string.journey_toolbox_title))
    Text(stringResource(R.string.journey_toolbox_body), style = MaterialTheme.typography.bodyLarge)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < OnboardingDimens.JourneyStackedBreakpoint || LocalDensity.current.fontScale > 1.3f
        val columns = if (compact) OnboardingDimens.TOOLBOX_COMPACT_COLUMNS else OnboardingDimens.TOOLBOX_COLUMNS
        // Viimeinen solu (null) on And more -laatta.
        val cells: List<JourneyTool?> = orderedJourneyTools(needs) + null
        Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
            cells.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
                ) {
                    row.forEachIndexed { columnIndex, tool ->
                        val index = rowIndex * columns + columnIndex
                        val cellModifier = Modifier.weight(1f).fillMaxHeight()
                        if (tool == null) {
                            ToolboxMoreTile(index, cellModifier)
                        } else {
                            ToolboxTile(tool, craftType, tool in picked, index, cellModifier)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ToolboxTile(
    tool: JourneyTool,
    craftType: CraftType,
    picked: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier
            .staggeredEntrance(rememberStaggeredEntrance(index))
            .clip(shape)
            .background(animatedSelectionColor(picked))
            .border(OnboardingDimens.SelectionBorderWidth, animatedSelectionBorder(picked), shape)
            .semantics(mergeDescendants = true) { selected = picked }
            .testTag("onboarding_tool_${tool.name}")
            .padding(ComponentDimens.ContentSpacing),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
    ) {
        // Ei valintamerkkiä: laatta ei ole valintaruutu, joten korostus on pelkkä reunus ja pohja.
        ToolboxGlyph(tool, index, Modifier.clearAndSetSemantics {})
        Text(toolLabel(tool, craftType), style = MaterialTheme.typography.titleSmall)
        ToolboxBody(toolBody(tool, craftType))
    }
}

/** Viimeinen laatta luettelee muut toiminnot katkoviivareunuksella: se ei ole oma työkalunsa. */
@Composable
private fun ToolboxMoreTile(
    index: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .staggeredEntrance(rememberStaggeredEntrance(index))
            .dashedOutline(MaterialTheme.colorScheme.outline, OnboardingDimens.ToolboxMoreOutline)
            .semantics(mergeDescendants = true) {}
            .testTag("onboarding_tool_more")
            .padding(ComponentDimens.ContentSpacing),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
    ) {
        Text(stringResource(R.string.journey_toolbox_and_more), style = MaterialTheme.typography.titleSmall)
        ToolboxBody(stringResource(R.string.journey_toolbox_more))
    }
}

@Composable
private fun ToolboxBody(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun toolLabel(
    tool: JourneyTool,
    craftType: CraftType,
): String =
    stringResource(
        if (tool == JourneyTool.Counter && craftType == CraftType.CROCHET) {
            R.string.journey_tool_counter_crochet
        } else {
            tool.label
        },
    )

@Composable
private fun toolBody(
    tool: JourneyTool,
    craftType: CraftType,
): String =
    stringResource(
        if (tool == JourneyTool.Counter && craftType == CraftType.CROCHET) {
            R.string.journey_tool_counter_body_crochet
        } else {
            tool.body
        },
    )

/**
 * Kuvake paperipohjalla, jotta haaleatkin ristipistovärit erottuvat kortista. Yksityiskohta
 * käynnistyy, kun laatta on tullut näkyviin.
 */
@Composable
private fun ToolboxGlyph(
    tool: JourneyTool,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember(tool) { Animatable(0f) }
    var hasBeenVisible by remember(tool) { mutableStateOf(false) }
    LaunchedEffect(tool, hasBeenVisible) {
        if (!hasBeenVisible || tool.detail == ToolDetail.None) return@LaunchedEffect
        delay(
            (
                IllustrationDimens.ENTRANCE_BASE_DELAY_MILLIS +
                    index * IllustrationDimens.ENTRANCE_STAGGER_MILLIS +
                    IllustrationDimens.ENTRANCE_MILLIS
            ).toLong(),
        )
        progress.animateTo(1f, tween(OnboardingDimens.TOOLBOX_DETAIL_MILLIS, easing = LinearEasing))
    }
    Box(
        modifier
            .onGloballyPositioned { coordinates ->
                if (!hasBeenVisible && !coordinates.boundsInWindow().isEmpty) hasBeenVisible = true
            }.size(OnboardingDimens.ToolboxGlyphPadSize)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.knitToolsColors.paperSurface),
        contentAlignment = Alignment.Center,
    ) {
        val glyphModifier = Modifier.size(OnboardingDimens.ToolboxGlyphSize)
        when (tool.detail) {
            ToolDetail.None -> CrossStitchIcon(tool.glyph, glyphModifier)
            ToolDetail.Press ->
                CrossStitchIcon(
                    tool.glyph,
                    glyphModifier.graphicsLayer {
                        // Kaksi painallusta: itseisarvo sinistä antaa kaksi kuoppaa yhden animaation aikana.
                        val press = abs(sin(progress.value * PI.toFloat() * OnboardingDimens.TOOLBOX_PRESS_COUNT))
                        val scale = 1f - OnboardingDimens.TOOLBOX_PRESS_DEPTH * press
                        scaleX = scale
                        scaleY = scale
                    },
                )
            ToolDetail.ReadingLine -> {
                val lineColor = MaterialTheme.colorScheme.primary
                Box(glyphModifier) {
                    // Ohje neutraalina kuten PDF-lukijassa, jotta oranssi lukulinja erottuu sen päällä.
                    CrossStitchIcon(
                        tool.glyph,
                        Modifier.fillMaxSize(),
                        color = MaterialTheme.knitToolsColors.paperText,
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        val y =
                            size.height *
                                lerp(
                                    OnboardingDimens.TOOLBOX_LINE_FROM,
                                    OnboardingDimens.TOOLBOX_LINE_TO,
                                    progress.value,
                                )
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = OnboardingDimens.ToolboxLineStroke.toPx(),
                        )
                    }
                }
            }
            ToolDetail.RisingBars ->
                // Pylväät kasvavat perusviivalta ylös: piirto rajataan alhaalta nousevaan alueeseen.
                CrossStitchIcon(
                    tool.glyph,
                    glyphModifier.drawWithContent {
                        val shown = lerp(OnboardingDimens.TOOLBOX_CHART_BASE, 1f, progress.value)
                        clipRect(top = size.height * (1f - shown)) { this@drawWithContent.drawContent() }
                    },
                )
        }
    }
}
