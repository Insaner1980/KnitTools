package com.finnvek.knittools.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@OptIn(ExperimentalFoundationApi::class)
@Suppress("kotlin:S107") // Compose-komponentit käyttävät monta parametria konvention mukaisesti
@Composable
fun ProjectListItem(
    project: CounterProject,
    lastUpdated: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    yarnName: String? = null,
    onYarnClick: (() -> Unit)? = null,
    photoCount: Int = 0,
    patternName: String? = null,
    hasPatternAttachment: Boolean = false,
    hasNotes: Boolean = false,
    onPatternClick: (() -> Unit)? = null,
    onNotesClick: (() -> Unit)? = null,
    onPhotosClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    onToggleSelection: (() -> Unit)? = null,
    statusText: String? = null,
) {
    val counterDisplay = CounterValueFormatter.forMainCounter(project)
    val targetStatus = mainCounterTargetStatus(counterDisplay.targetLine).takeUnless { project.isCompleted }
    val targetFraction = mainCounterTargetFraction(counterDisplay.targetLine).takeUnless { project.isCompleted }
    val secondaryLine =
        projectListItemSecondaryLine(
            sectionName = project.sectionName,
            patternName = patternName,
            projectName = project.name,
        )
    val updatedText = projectTimestampText(lastUpdated, project.isCompleted)
    val hasFooter = yarnName != null || hasPatternAttachment || photoCount > 0 || hasNotes
    val selectionModifier =
        selected?.let { isSelected ->
            Modifier.semantics { this.selected = isSelected }
        } ?: Modifier

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (selected == true) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                    } else {
                        Modifier
                    },
                ).then(selectionModifier)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(vertical = ProjectListDimens.ProjectItemVerticalPadding),
    ) {
        val compactLayout =
            usesCompactProjectListItemLayout(
                maxWidthDp = maxWidth.value,
                fontScale = LocalDensity.current.fontScale,
            )

        Row(modifier = Modifier.fillMaxWidth()) {
            if (selected != null) {
                Box(
                    modifier =
                        Modifier
                            .width(48.dp)
                            .defaultMinSize(minHeight = 48.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelection?.invoke() },
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                ProjectListItemHeader(
                    name = project.name,
                    updatedText = updatedText,
                    compactLayout = compactLayout,
                )
                statusText?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(ProjectListDimens.ItemLineGap))
                ProjectListItemContext(
                    secondaryLine = secondaryLine,
                    craftType = craftTypeLabel(project.craftType),
                    compactLayout = compactLayout,
                )
                Spacer(modifier = Modifier.height(ProjectListDimens.ProgressGroupTopGap))
                ProjectListItemProgress(
                    countText =
                        counterDisplay.targetLine
                            ?.takeUnless { project.isCompleted }
                            ?.let { mainCounterTargetText(it) }
                            ?: mainCounterCountText(counterDisplay.projectCardCount),
                    statusText = targetStatus?.let { projectListTargetStatusText(it) },
                    progressFraction = targetFraction,
                    compactLayout = compactLayout,
                )
                if (hasFooter) {
                    Spacer(modifier = Modifier.height(ProjectListDimens.FooterTopGap))
                    ProjectListItemFooter(
                        yarnName = yarnName,
                        onYarnClick = onYarnClick.takeIf { selected == null },
                        photoCount = photoCount,
                        hasPatternAttachment = hasPatternAttachment,
                        hasNotes = hasNotes,
                        onPatternClick = onPatternClick.takeIf { selected == null },
                        onNotesClick = onNotesClick.takeIf { selected == null },
                        onPhotosClick = onPhotosClick.takeIf { selected == null },
                        actionsEnabled = selected == null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectListItemHeader(
    name: String,
    updatedText: String,
    compactLayout: Boolean,
) {
    if (compactLayout) {
        Column {
            ProjectListItemName(name)
            Text(
                text = updatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProjectListItemName(
                name = name,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = updatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun ProjectListItemName(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun ProjectListItemContext(
    secondaryLine: String?,
    craftType: String,
    compactLayout: Boolean,
) {
    if (compactLayout) {
        Column {
            secondaryLine?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = craftType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (secondaryLine != null) {
                Text(
                    text = secondaryLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(
                text = craftType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun ProjectListItemProgress(
    countText: String,
    statusText: String?,
    progressFraction: Float?,
    compactLayout: Boolean,
) {
    if (compactLayout) {
        Column {
            ProjectListItemCount(countText)
            statusText?.let { ProjectListItemTargetStatus(it) }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProjectListItemCount(
                text = countText,
                modifier = Modifier.weight(1f),
            )
            statusText?.let { ProjectListItemTargetStatus(it) }
        }
    }

    if (progressFraction != null) {
        Spacer(modifier = Modifier.height(ProjectListDimens.ItemLineGap))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProjectListDimens.ProgressTrackInset)
                    .height(ProjectListDimens.ProgressTrackHeight)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = ProjectListDimens.ProgressTrackAlpha,
                        ),
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progressFraction)
                        .height(ProjectListDimens.ProgressTrackHeight)
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

internal fun usesCompactProjectListItemLayout(
    maxWidthDp: Float,
    fontScale: Float,
): Boolean = maxWidthDp < 320f || fontScale > 1f

@Composable
private fun ProjectListItemCount(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun ProjectListItemTargetStatus(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
@Suppress("kotlin:S107") // Footer välittää toisistaan riippumattomat projektin tilailmaisimet eksplisiittisesti.
private fun ProjectListItemFooter(
    yarnName: String?,
    onYarnClick: (() -> Unit)?,
    photoCount: Int,
    hasPatternAttachment: Boolean,
    hasNotes: Boolean,
    onPatternClick: (() -> Unit)?,
    onNotesClick: (() -> Unit)?,
    onPhotosClick: (() -> Unit)?,
    actionsEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (yarnName != null) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = ProjectListDimens.FooterActionTouchSize)
                        .then(
                            if (actionsEnabled && onYarnClick != null) {
                                Modifier.clickable(onClick = onYarnClick)
                            } else {
                                Modifier
                            },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = yarnName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        ProjectListItemAttachmentActions(
            hasPatternAttachment = hasPatternAttachment,
            photoCount = photoCount,
            hasNotes = hasNotes,
            onPatternClick = onPatternClick,
            onPhotosClick = onPhotosClick,
            onNotesClick = onNotesClick,
        )
    }
}

@Composable
private fun ProjectListItemAttachmentActions(
    hasPatternAttachment: Boolean,
    photoCount: Int,
    hasNotes: Boolean,
    onPatternClick: (() -> Unit)?,
    onPhotosClick: (() -> Unit)?,
    onNotesClick: (() -> Unit)?,
) {
    if (hasPatternAttachment) {
        ProjectListItemAttachmentAction(
            icon = Icons.Filled.AutoStories,
            contentDescription = stringResource(R.string.pattern_viewer_title),
            onClick = onPatternClick,
        )
    }
    if (photoCount > 0) {
        ProjectListItemAttachmentAction(
            icon = Icons.Filled.CameraAlt,
            contentDescription = stringResource(R.string.progress_photos),
            label = formatIntegerForDisplay(photoCount.toLong(), rememberCurrentLocale()),
            onClick = onPhotosClick,
        )
    }
    if (hasNotes) {
        ProjectListItemAttachmentAction(
            icon = Icons.AutoMirrored.Outlined.StickyNote2,
            contentDescription = stringResource(R.string.notes),
            onClick = onNotesClick,
        )
    }
}

@Composable
private fun ProjectListItemAttachmentAction(
    icon: ImageVector,
    contentDescription: String?,
    label: String? = null,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = ProjectListDimens.FooterActionTouchSize,
                    minHeight = ProjectListDimens.FooterActionTouchSize,
                ).then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        horizontalArrangement =
            Arrangement.spacedBy(ProjectListDimens.ItemLineGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(ProjectListDimens.FooterIconSize),
            tint = MaterialTheme.knitToolsColors.onSurfaceMuted,
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun projectListTargetStatusText(status: MainCounterTargetStatus): String =
    when (status) {
        is MainCounterTargetStatus.Remaining ->
            stringResource(
                R.string.counter_target_remaining_format,
                formatIntegerForDisplay(status.countSlot.count.toLong(), rememberCurrentLocale()),
            )
        MainCounterTargetStatus.Reached -> stringResource(R.string.counter_target_reached)
        is MainCounterTargetStatus.Past ->
            stringResource(
                R.string.counter_target_past_format,
                formatIntegerForDisplay(status.countSlot.count.toLong(), rememberCurrentLocale()),
            )
    }

private fun projectListItemSecondaryLine(
    sectionName: String?,
    patternName: String?,
    projectName: String,
): String? {
    sectionName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }

    val trimmedName = projectName.trim()
    return patternName
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.equals(trimmedName, ignoreCase = true) }
        ?.takeUnless(::isRawPdfFileName)
}

private fun isRawPdfFileName(name: String): Boolean = name.endsWith(".pdf", ignoreCase = true)

@Composable
private fun projectTimestampText(
    timestamp: Long,
    isCompleted: Boolean,
): String {
    val now = System.currentTimeMillis()
    val relativeTime =
        if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) {
            stringResource(R.string.just_now)
        } else {
            DateUtils
                .getRelativeTimeSpanString(
                    timestamp,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString()
        }
    return stringResource(
        if (isCompleted) R.string.project_completed_format else R.string.project_updated_format,
        relativeTime,
    )
}
