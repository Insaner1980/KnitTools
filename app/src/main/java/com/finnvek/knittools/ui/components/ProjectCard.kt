package com.finnvek.knittools.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.ui.theme.YarnColors
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Suppress("kotlin:S107") // Compose-komponentit käyttävät monta parametria konvention mukaisesti
@Composable
fun ProjectCard(
    name: String,
    rowCount: Int,
    sectionName: String?,
    lastUpdated: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    totalRows: Int? = null,
    metadataLine: String? = null,
    countText: String? = null,
    yarnName: String? = null,
    yarnColorSeed: Long? = null,
    onYarnClick: (() -> Unit)? = null,
    photoCount: Int = 0,
    patternName: String? = null,
    hasPatternAttachment: Boolean = false,
    hasNotes: Boolean = false,
    onPatternClick: (() -> Unit)? = null,
    onNotesClick: (() -> Unit)? = null,
    onPhotosClick: (() -> Unit)? = null,
) {
    val secondaryLine =
        projectCardSecondaryLine(
            sectionName = sectionName,
            patternName = patternName,
            projectName = name,
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (secondaryLine != null) {
                    Text(
                        text = secondaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!metadataLine.isNullOrBlank()) {
                    Text(
                        text = metadataLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                ProjectCardStatsRow(
                    stats =
                        ProjectCardStats(
                            rowCount = totalRows ?: rowCount,
                            countText = countText,
                            lastUpdated = lastUpdated,
                            photoCount = photoCount,
                            hasPatternAttachment = hasPatternAttachment,
                            hasNotes = hasNotes,
                        ),
                    onPatternClick = onPatternClick,
                    onNotesClick = onNotesClick,
                    onPhotosClick = onPhotosClick,
                )
                ProjectCardYarnLine(
                    yarnName = yarnName,
                    yarnColorSeed = yarnColorSeed,
                    onClick = onYarnClick,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    }
}

private fun projectCardSecondaryLine(
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

private data class ProjectCardStats(
    val rowCount: Int,
    val countText: String?,
    val lastUpdated: Long,
    val photoCount: Int,
    val hasPatternAttachment: Boolean,
    val hasNotes: Boolean,
)

// Erotettu ProjectCard-funktiosta kognitiivisen kompleksisuuden vähentämiseksi (S3776)
@Composable
private fun ProjectCardStatsRow(
    stats: ProjectCardStats,
    onPatternClick: (() -> Unit)? = null,
    onNotesClick: (() -> Unit)? = null,
    onPhotosClick: (() -> Unit)? = null,
) {
    val dateFormat = rememberLocaleDateFormat("MMMd")
    val rowCountColor =
        if (stats.rowCount == 0) {
            MaterialTheme.knitToolsColors.onSurfaceMuted
        } else {
            MaterialTheme.colorScheme.primary
        }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stats.countText ?: pluralStringResource(R.plurals.rows_format, stats.rowCount, stats.rowCount),
                style = MaterialTheme.typography.headlineSmall,
                color = rowCountColor,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDate(stats.lastUpdated, dateFormat),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
        ProjectCardAttachmentActionsRow(
            hasPatternAttachment = stats.hasPatternAttachment,
            photoCount = stats.photoCount,
            hasNotes = stats.hasNotes,
            onPatternClick = onPatternClick,
            onPhotosClick = onPhotosClick,
            onNotesClick = onNotesClick,
        )
    }
}

@Composable
private fun ProjectCardAttachmentActionsRow(
    hasPatternAttachment: Boolean,
    photoCount: Int,
    hasNotes: Boolean,
    onPatternClick: (() -> Unit)? = null,
    onPhotosClick: (() -> Unit)? = null,
    onNotesClick: (() -> Unit)? = null,
) {
    if (!hasPatternAttachment && photoCount <= 0 && !hasNotes) return

    Spacer(modifier = Modifier.height(2.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPatternAttachment) {
            ProjectCardAttachmentAction(
                icon = Icons.Filled.AutoStories,
                contentDescription = stringResource(R.string.pattern_viewer_title),
                onClick = onPatternClick,
            )
        }
        if (photoCount > 0) {
            ProjectCardAttachmentAction(
                icon = Icons.Filled.CameraAlt,
                contentDescription = stringResource(R.string.progress_photos),
                label = formatIntegerForDisplay(photoCount.toLong(), rememberCurrentLocale()),
                onClick = onPhotosClick,
            )
        }
        if (hasNotes) {
            ProjectCardAttachmentAction(
                icon = Icons.AutoMirrored.Outlined.StickyNote2,
                contentDescription = stringResource(R.string.notes),
                onClick = onNotesClick,
            )
        }
    }
}

@Composable
private fun ProjectCardAttachmentAction(
    icon: ImageVector,
    contentDescription: String,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
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
private fun ProjectCardYarnLine(
    yarnName: String?,
    yarnColorSeed: Long?,
    onClick: (() -> Unit)? = null,
) {
    if (yarnName != null && yarnColorSeed != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier =
                Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(onClick = onClick)
                        } else {
                            Modifier
                        },
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            YarnColors[(yarnColorSeed % YarnColors.size).toInt()],
                            CircleShape,
                        ),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = yarnName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun formatDate(
    timestamp: Long,
    dateFormat: SimpleDateFormat,
): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val relativeTime =
        DateUtils.getRelativeTimeSpanString(
            timestamp,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        )
    return when {
        diff < 60_000 -> stringResource(R.string.just_now)
        diff < 86_400_000 -> relativeTime.toString()
        else -> dateFormat.format(Date(timestamp))
    }
}
