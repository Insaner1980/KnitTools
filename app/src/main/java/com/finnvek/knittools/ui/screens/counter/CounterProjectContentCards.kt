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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

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

internal fun projectContentCards(): List<ProjectContentCard> =
    listOf(
        ProjectContentCard(
            kind = ProjectContentCardKind.PATTERN,
            titleRes = R.string.project_content_pattern,
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
    onCardClick: (ProjectContentCardKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = projectContentCards()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CounterDimens.ProjectCardGridSpacing),
    ) {
        Text(
            text = stringResource(R.string.project_content_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
        )
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
    val accent = card.kind.accentColor()
    Surface(
        modifier =
            modifier
                .aspectRatio(1f)
                .clickable(
                    onClickLabel = title,
                    role = Role.Button,
                    onClick = onClick,
                ),
        shape = RoundedCornerShape(CounterDimens.ProjectCardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            Icon(
                imageVector = card.kind.icon(),
                contentDescription = null,
                modifier = Modifier.size(CounterDimens.ProjectCardIconSize),
                tint = accent,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProjectContentCardKind.accentColor(): Color =
    when (this) {
        ProjectContentCardKind.PATTERN -> MaterialTheme.colorScheme.primary
        ProjectContentCardKind.YARN -> MaterialTheme.colorScheme.secondary
        ProjectContentCardKind.NOTES -> MaterialTheme.knitToolsColors.brandWine
        ProjectContentCardKind.PHOTOS -> MaterialTheme.colorScheme.tertiary
        ProjectContentCardKind.REMINDER -> MaterialTheme.knitToolsColors.tealAccent
    }

private fun ProjectContentCardKind.icon(): ImageVector =
    when (this) {
        ProjectContentCardKind.PATTERN -> Icons.Outlined.Description
        ProjectContentCardKind.YARN -> Icons.Outlined.Inventory2
        ProjectContentCardKind.NOTES -> Icons.Outlined.EditNote
        ProjectContentCardKind.PHOTOS -> Icons.Outlined.PhotoLibrary
        ProjectContentCardKind.REMINDER -> Icons.Outlined.Notifications
    }
