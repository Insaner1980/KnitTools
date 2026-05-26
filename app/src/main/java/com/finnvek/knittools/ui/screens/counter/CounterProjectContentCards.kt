package com.finnvek.knittools.ui.screens.counter

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder

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
    val bodyText: String? = null,
    @param:StringRes val bodyRes: Int? = null,
    val photoCount: Int? = null,
    val reminderRow: Int? = null,
    val reminderMessage: String? = null,
)

internal fun projectContentCards(state: CounterUiState): List<ProjectContentCard> =
    buildList {
        add(patternContentCard(state))
        add(yarnContentCard(state))
        add(notesContentCard(state))
        add(photosContentCard(state))
        nearestUpcomingReminder(state.reminders, state.counter.count)?.let { reminder ->
            add(
                ProjectContentCard(
                    kind = ProjectContentCardKind.REMINDER,
                    titleRes = R.string.project_content_next_reminder,
                    reminderRow = reminder.targetRow,
                    reminderMessage = reminder.message,
                ),
            )
        }
    }

internal fun firstProjectNoteLine(notes: String): String? =
    notes
        .lineSequence()
        .map(String::trim)
        .firstOrNull(String::isNotBlank)

internal fun nearestUpcomingReminder(
    reminders: List<RowReminder>,
    currentRow: Int,
): RowReminder? =
    reminders
        .asSequence()
        .filter { !it.isCompleted }
        .filter { it.targetRow >= currentRow }
        .sortedWith(compareBy<RowReminder> { it.targetRow }.thenBy { it.id })
        .firstOrNull()

@Composable
fun ProjectContentCards(
    state: CounterUiState,
    onCardClick: (ProjectContentCardKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.project_content_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        projectContentCards(state).forEach { card ->
            ProjectContentCardView(
                card = card,
                onClick = { onCardClick(card.kind) },
            )
        }
    }
}

private fun patternContentCard(state: CounterUiState): ProjectContentCard {
    val attachedPatternName = state.patternName?.takeIf(String::isNotBlank)
    val linkedPatternName =
        state.linkedPattern
            ?.name
            ?.takeIf(String::isNotBlank)
            ?.let { "$it · Ravelry" }
    val patternName = attachedPatternName ?: linkedPatternName
    return if (patternName != null) {
        ProjectContentCard(
            kind = ProjectContentCardKind.PATTERN,
            titleRes = R.string.project_content_open_pattern,
            bodyText = patternName,
        )
    } else {
        ProjectContentCard(
            kind = ProjectContentCardKind.PATTERN,
            titleRes = R.string.project_content_attach_pattern,
            bodyRes = R.string.project_content_attach_pattern_body,
        )
    }
}

private fun yarnContentCard(state: CounterUiState): ProjectContentCard {
    val linkedYarnNames = state.linkedYarns.mapNotNull { it.second.takeIf(String::isNotBlank) }
    val projectYarnNoteNames = state.projectYarnNotes.toProjectYarnNoteNames()
    val hasYarn = state.linkedYarns.isNotEmpty() || state.projectYarnNotes.isNotEmpty()
    val yarnNames = linkedYarnNames + projectYarnNoteNames
    return if (hasYarn && yarnNames.isNotEmpty()) {
        ProjectContentCard(
            kind = ProjectContentCardKind.YARN,
            titleRes = R.string.project_content_yarn,
            bodyText = yarnNames.joinToString(", "),
        )
    } else {
        ProjectContentCard(
            kind = ProjectContentCardKind.YARN,
            titleRes = R.string.project_content_add_yarn,
            bodyRes = R.string.project_content_add_yarn_body,
        )
    }
}

private fun notesContentCard(state: CounterUiState): ProjectContentCard {
    val notePreview = firstProjectNoteLine(state.notes)
    return if (notePreview != null) {
        ProjectContentCard(
            kind = ProjectContentCardKind.NOTES,
            titleRes = R.string.project_content_notes,
            bodyText = notePreview,
        )
    } else {
        ProjectContentCard(
            kind = ProjectContentCardKind.NOTES,
            titleRes = R.string.project_content_add_note,
            bodyRes = R.string.project_content_add_note_body,
        )
    }
}

private fun photosContentCard(state: CounterUiState): ProjectContentCard =
    if (state.latestPhotos.isNotEmpty()) {
        ProjectContentCard(
            kind = ProjectContentCardKind.PHOTOS,
            titleRes = R.string.project_content_photos,
            photoCount = state.latestPhotos.size,
        )
    } else {
        ProjectContentCard(
            kind = ProjectContentCardKind.PHOTOS,
            titleRes = R.string.project_content_add_photo,
            bodyRes = R.string.project_content_add_photo_body,
        )
    }

private fun List<ProjectYarnNote>.toProjectYarnNoteNames(): List<String> =
    mapNotNull { note -> note.name.takeIf(String::isNotBlank) }

@Composable
private fun ProjectContentCardView(
    card: ProjectContentCard,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = card.kind.icon(),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(card.titleRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.bodyText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProjectContentCard.bodyText(): String =
    when {
        photoCount == 1 -> stringResource(R.string.photo_count_one)
        photoCount != null -> stringResource(R.string.photo_count_many, photoCount)
        reminderRow != null && reminderMessage != null -> {
            stringResource(R.string.project_content_reminder_body, reminderRow, reminderMessage)
        }

        bodyRes != null -> stringResource(bodyRes)
        else -> bodyText.orEmpty()
    }

private fun ProjectContentCardKind.icon(): ImageVector =
    when (this) {
        ProjectContentCardKind.PATTERN -> Icons.Outlined.Description
        ProjectContentCardKind.YARN -> Icons.Outlined.Inventory2
        ProjectContentCardKind.NOTES -> Icons.Outlined.EditNote
        ProjectContentCardKind.PHOTOS -> Icons.Outlined.PhotoLibrary
        ProjectContentCardKind.REMINDER -> Icons.Outlined.Notifications
    }
