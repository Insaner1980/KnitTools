package com.finnvek.knittools.ui.screens.counter

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.CounterDimens

enum class ProjectContentCardKind {
    PATTERN,
    YARN,
    NOTES,
    PHOTOS,
    REMINDER,
}

data class ProjectContentCard(
    val kind: ProjectContentCardKind,
    @param:StringRes val titleRes: Int,
)

internal fun projectContentCards(state: CounterUiState): List<ProjectContentCard> =
    listOf(
        ProjectContentCard(
            kind = ProjectContentCardKind.PATTERN,
            titleRes =
                if (state.patternUri != null || state.linkedPattern != null) {
                    R.string.project_content_open_pattern
                } else {
                    R.string.project_content_attach_pattern
                },
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

@Composable
fun ProjectContentCards(
    state: CounterUiState,
    onCardClick: (ProjectContentCardKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = projectContentCards(state)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CounterDimens.ProjectCardGridSpacing),
    ) {
        Text(
            text = stringResource(R.string.project_content_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        cards.chunked(2).forEach { rowCards ->
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
                if (rowCards.size == 1) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
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
    Surface(
        modifier =
            modifier
                .aspectRatio(1f)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(CounterDimens.ProjectCardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(CounterDimens.ProjectCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = card.kind.icon(),
                contentDescription = null,
                modifier = Modifier.size(CounterDimens.ProjectCardIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(card.titleRes),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun ProjectContentCardKind.icon(): ImageVector =
    when (this) {
        ProjectContentCardKind.PATTERN -> Icons.Outlined.Description
        ProjectContentCardKind.YARN -> Icons.Outlined.Inventory2
        ProjectContentCardKind.NOTES -> Icons.Outlined.EditNote
        ProjectContentCardKind.PHOTOS -> Icons.Outlined.PhotoLibrary
        ProjectContentCardKind.REMINDER -> Icons.Outlined.Notifications
    }
