package com.finnvek.knittools.ui.screens.counter

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder

data class CounterWorkspaceSummary(
    val patternName: String?,
    val linkedPatternName: String?,
    val linkedYarnNames: List<String>,
    val projectYarnNoteNames: List<String>,
    val notePreview: String?,
    val nearestReminder: RowReminder?,
    val photoCount: Int,
)

enum class ProjectInfoKind {
    PATTERN,
    YARN,
    NOTES,
    REMINDER,
    PHOTOS,
    EMPTY,
}

data class ProjectInfoRow(
    val kind: ProjectInfoKind,
    @param:StringRes val labelRes: Int,
    val value: String? = null,
    val photoCount: Int? = null,
)

internal fun CounterUiState.toCounterWorkspaceSummary(): CounterWorkspaceSummary =
    CounterWorkspaceSummary(
        patternName = patternName?.takeIf { it.isNotBlank() },
        linkedPatternName = linkedPattern?.name?.takeIf { it.isNotBlank() },
        linkedYarnNames = linkedYarns.mapNotNull { it.second.takeIf(String::isNotBlank) },
        projectYarnNoteNames = projectYarnNotes.toProjectYarnNoteNames(),
        notePreview = firstProjectNoteLine(notes),
        nearestReminder = nearestUpcomingReminder(reminders, counter.count),
        photoCount = latestPhotos.size,
    )

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

internal fun projectInfoRows(summary: CounterWorkspaceSummary): List<ProjectInfoRow> {
    val rows = mutableListOf<ProjectInfoRow>()
    val patternValue = summary.patternName ?: summary.linkedPatternName?.let { "$it · Ravelry" }
    if (!patternValue.isNullOrBlank()) {
        rows +=
            ProjectInfoRow(
                kind = ProjectInfoKind.PATTERN,
                labelRes = R.string.project_info_pattern,
                value = patternValue,
            )
    }
    val projectYarnNoteNames = summary.projectYarnNoteNames
    val yarnNames = summary.linkedYarnNames + projectYarnNoteNames
    if (yarnNames.isNotEmpty()) {
        rows +=
            ProjectInfoRow(
                kind = ProjectInfoKind.YARN,
                labelRes = R.string.project_info_yarn,
                value = yarnNames.joinToString(", "),
            )
    }
    if (!summary.notePreview.isNullOrBlank()) {
        rows +=
            ProjectInfoRow(
                kind = ProjectInfoKind.NOTES,
                labelRes = R.string.project_info_notes,
                value = summary.notePreview,
            )
    }
    summary.nearestReminder?.let { reminder ->
        rows +=
            ProjectInfoRow(
                kind = ProjectInfoKind.REMINDER,
                labelRes = R.string.project_info_reminder,
                value = "Row ${reminder.targetRow} · ${reminder.message}",
            )
    }
    if (summary.photoCount > 0) {
        rows +=
            ProjectInfoRow(
                kind = ProjectInfoKind.PHOTOS,
                labelRes = R.string.project_info_photos,
                photoCount = summary.photoCount,
            )
    }
    if (rows.isEmpty()) {
        rows +=
            ProjectInfoRow(
                kind = ProjectInfoKind.EMPTY,
                labelRes = R.string.project_info_empty_label,
            )
    }
    return rows
}

private fun List<ProjectYarnNote>.toProjectYarnNoteNames(): List<String> =
    mapNotNull { note -> note.name.takeIf(String::isNotBlank) }

@Composable
fun ProjectInfoSection(
    rows: List<ProjectInfoRow>,
    onRowClick: (ProjectInfoKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.project_info_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        rows.forEach { row ->
            ProjectInfoRowView(
                row = row,
                onClick = { onRowClick(row.kind) },
            )
        }
    }
}

@Composable
private fun ProjectInfoRowView(
    row: ProjectInfoRow,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(row.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.34f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = projectInfoValue(row),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.66f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun projectInfoValue(row: ProjectInfoRow): String =
    when {
        row.kind == ProjectInfoKind.EMPTY -> stringResource(R.string.project_info_empty_value)
        row.photoCount == 1 -> stringResource(R.string.photo_count_one)
        row.photoCount != null -> stringResource(R.string.photo_count_many, row.photoCount)
        else -> row.value.orEmpty()
    }
