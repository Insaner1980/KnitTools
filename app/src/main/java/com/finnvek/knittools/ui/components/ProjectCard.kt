package com.finnvek.knittools.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.YarnColors
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    yarnName: String? = null,
    yarnColorSeed: Long = 0L,
    photoCount: Int = 0,
    patternName: String? = null,
    hasNotes: Boolean = false,
    onNotesClick: (() -> Unit)? = null,
) {
    val trimmedName = name.trim()
    val visiblePatternName =
        patternName
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals(trimmedName, ignoreCase = true) }

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
                if (visiblePatternName != null) {
                    Text(
                        text = visiblePatternName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (sectionName != null) {
                    Text(
                        text = sectionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                ProjectCardStatsRow(
                    rowCount = totalRows ?: rowCount,
                    lastUpdated = lastUpdated,
                    photoCount = photoCount,
                    hasNotes = hasNotes,
                    onNotesClick = onNotesClick,
                )
                ProjectCardYarnLine(
                    yarnName = yarnName,
                    yarnColorSeed = yarnColorSeed,
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

// Erotettu ProjectCard-funktiosta kognitiivisen kompleksisuuden vähentämiseksi (S3776)
@Composable
private fun ProjectCardStatsRow(
    rowCount: Int,
    lastUpdated: Long,
    photoCount: Int,
    hasNotes: Boolean = false,
    onNotesClick: (() -> Unit)? = null,
) {
    val rowCountColor =
        if (rowCount == 0) {
            MaterialTheme.knitToolsColors.onSurfaceMuted
        } else {
            MaterialTheme.colorScheme.primary
        }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.rows_format, rowCount),
            style = MaterialTheme.typography.headlineSmall,
            color = rowCountColor,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = formatDate(lastUpdated),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        )
        if (photoCount > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$photoCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
        if (hasNotes) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                contentDescription = stringResource(R.string.notes),
                modifier =
                    Modifier
                        .size(16.dp)
                        .then(
                            if (onNotesClick != null) {
                                Modifier.clickable(onClick = onNotesClick)
                            } else {
                                Modifier
                            },
                        ),
                tint = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun ProjectCardYarnLine(
    yarnName: String?,
    yarnColorSeed: Long,
) {
    if (yarnName != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun formatDate(timestamp: Long): String {
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
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
