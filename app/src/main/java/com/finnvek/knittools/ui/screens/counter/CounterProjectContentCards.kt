package com.finnvek.knittools.ui.screens.counter

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.CrossStitchIcon
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

enum class ProjectContentCardKind(
    val glyph: CrossStitchGlyph,
) {
    PATTERN(CrossStitchGlyph.PATTERN),
    YARN(CrossStitchGlyph.YARN),
    NOTES(CrossStitchGlyph.NOTES),
    PHOTOS(CrossStitchGlyph.PHOTOS),
    REMINDER(CrossStitchGlyph.REMINDER),
}

data class ProjectContentCard(
    val kind: ProjectContentCardKind,
    @param:StringRes val titleRes: Int,
)

internal fun projectContentCards(hasPattern: Boolean): List<ProjectContentCard> =
    listOf(
        ProjectContentCard(
            kind = ProjectContentCardKind.PATTERN,
            titleRes = patternContentTitleRes(hasPattern),
        ),
        ProjectContentCard(
            kind = ProjectContentCardKind.YARN,
            titleRes = R.string.project_content_yarn,
        ),
        ProjectContentCard(
            kind = ProjectContentCardKind.NOTES,
            titleRes = R.string.project_content_notes,
        ),
        ProjectContentCard(
            kind = ProjectContentCardKind.PHOTOS,
            titleRes = R.string.project_content_photos,
        ),
        ProjectContentCard(
            kind = ProjectContentCardKind.REMINDER,
            titleRes = R.string.reminders,
        ),
    )

@StringRes
private fun patternContentTitleRes(hasPattern: Boolean): Int =
    if (hasPattern) R.string.saved_pattern_detail_open_pattern else R.string.project_content_add_pattern

@Composable
fun ProjectContentCards(
    onCardClick: (ProjectContentCardKind) -> Unit,
    hasPattern: Boolean,
    modifier: Modifier = Modifier,
) {
    val cards = projectContentCards(hasPattern)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CounterDimens.ProjectCardGridSpacing),
    ) {
        SectionLabel(text = stringResource(R.string.project_content_title))
        cards.take(4).chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CounterDimens.ProjectCardGridSpacing),
            ) {
                rowCards.forEach { card ->
                    ProjectContentCardView(
                        card = card,
                        onClick = { onCardClick(card.kind) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        cards.getOrNull(4)?.let { card ->
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val centeredTileWidth = (maxWidth - CounterDimens.ProjectCardGridSpacing) / 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ProjectContentCardView(
                        card = card,
                        onClick = { onCardClick(card.kind) },
                        modifier = Modifier.width(centeredTileWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectContentCardView(
    card: ProjectContentCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(card.titleRes)
    Surface(
        modifier =
            modifier
                .aspectRatio(1f)
                .clickable(
                    onClickLabel = title,
                    role = Role.Button,
                    onClick = onClick,
                ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.knitToolsColors.cardContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(CounterDimens.ProjectCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    space = CounterDimens.ProjectCardIconTitleSpacing,
                    alignment = Alignment.CenterVertically,
                ),
        ) {
            CrossStitchIcon(
                glyph = card.kind.glyph,
                modifier =
                    Modifier.size(
                        if (LocalDensity.current.fontScale >=
                            1.5f
                        ) {
                            CounterDimens.ProjectCardCompactIconSize
                        } else {
                            CounterDimens.ProjectCardIconSize
                        },
                    ),
            )
            BasicText(
                text = title,
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = TextUnit.Unspecified,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = MaterialTheme.typography.labelSmall.fontSize,
                        maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
                    ),
                maxLines = 3,
            )
        }
    }
}
