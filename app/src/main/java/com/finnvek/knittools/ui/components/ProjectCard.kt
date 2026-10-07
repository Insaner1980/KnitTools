package com.finnvek.knittools.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.projectCardName

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: CounterProject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    photoUri: String? = null,
    patternName: String? = null,
    selected: Boolean? = null,
    onToggleSelection: (() -> Unit)? = null,
    statusText: String? = null,
) {
    val isMultiSelectMode = selected != null
    val openLabel = stringResource(R.string.project_card_open_overview_action)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cardContainerColor(selected = selected == true),
    ) {
        BoxWithConstraints {
            val compact = usesCompactProjectCardLayout(maxWidth.value, LocalDensity.current.fontScale)
            // Kortissa ei ole omaa laskurinappia: napautus avaa projektinäkymän, josta laskuriin
            // pääsee yhdellä napautuksella. Laskuriin vie suoraan vain Continue-hero.
            // Pikkukuva tasataan ylös nimen kanssa, jottei se kellu korkeassa kortissa.
            Row(
                modifier =
                    Modifier
                        .semantics(mergeDescendants = true) {
                            if (selected != null) this.selected = selected
                        }.combinedClickable(
                            role = Role.Button,
                            onClickLabel = if (isMultiSelectMode) null else openLabel,
                            onClick = onClick,
                            onLongClick = onLongClick,
                        ).padding(
                            start = ProjectListDimens.CardPaddingStart,
                            top = ProjectListDimens.CardPaddingVertical,
                            bottom = ProjectListDimens.CardPaddingVertical,
                            end = ProjectListDimens.CardPaddingEnd,
                        ),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(ProjectListDimens.ThumbnailTextGap),
            ) {
                if (selected != null) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelection?.invoke() })
                } else {
                    ProjectThumbnail(
                        projectId = project.id,
                        craftType = project.craftType,
                        photoUri = photoUri,
                        size =
                            if (compact) {
                                ProjectListDimens.ThumbnailSizeCompact
                            } else {
                                ProjectListDimens.ThumbnailSize
                            },
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ProjectListDimens.ItemLineGap),
                ) {
                    Text(
                        project.name,
                        style = MaterialTheme.typography.projectCardName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    projectListItemSecondaryLine(project.sectionName, patternName, project.name)?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (project.isCompleted) {
                        Text(
                            projectTimestampText(project.completedAt ?: project.updatedAt, true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ProjectCardProgress(project, compact)
                    }
                    statusText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCardProgress(
    project: CounterProject,
    compact: Boolean,
) {
    val display = CounterValueFormatter.forMainCounter(project)
    val count = display.targetLine?.let { mainCounterTargetText(it) } ?: mainCounterCountText(display.projectCardCount)
    val status = mainCounterTargetStatus(display.targetLine)?.let { projectListTargetStatusText(it) }
    if (compact) {
        Text(count, style = MaterialTheme.typography.labelLarge)
        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(ProjectListDimens.ItemLineGap)) {
            Text(
                count,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f).alignByBaseline(),
            )
            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
    mainCounterTargetFraction(display.targetLine)?.let { ProjectProgressBar(it) }
}

internal fun usesCompactProjectCardLayout(
    maxWidthDp: Float,
    fontScale: Float,
): Boolean = maxWidthDp < ProjectListDimens.CompactMaxWidthDp || fontScale > ProjectListDimens.CompactFontScaleThreshold
