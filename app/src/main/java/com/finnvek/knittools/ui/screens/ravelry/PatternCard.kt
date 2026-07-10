package com.finnvek.knittools.ui.screens.ravelry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.data.remote.PatternAvailability
import com.finnvek.knittools.ui.theme.RavelryTeal

data class PatternCardState(
    val name: String,
    val designerName: String,
    val thumbnailUrl: String?,
    val difficulty: Float?,
    val availability: PatternAvailability,
)

@Composable
fun PatternCard(
    state: PatternCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PatternThumbnail(thumbnailUrl = state.thumbnailUrl, contentDescription = state.name)
            PatternDetails(
                name = state.name,
                designerName = state.designerName,
                difficulty = state.difficulty,
                availability = state.availability,
                modifier = Modifier.weight(1f),
            )
            PatternCardActionSlot(actionContent = actionContent)
        }
    }
}

@Composable
private fun PatternCardActionSlot(actionContent: (@Composable () -> Unit)?) {
    if (actionContent == null) return

    Spacer(modifier = Modifier.width(8.dp))
    Box(
        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        actionContent()
    }
}

@Composable
private fun PatternThumbnail(
    thumbnailUrl: String?,
    contentDescription: String,
) {
    if (thumbnailUrl != null) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = contentDescription,
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
private fun PatternDetails(
    name: String,
    designerName: String,
    difficulty: Float?,
    availability: PatternAvailability,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = designerName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        PatternBadgeRow(difficulty = difficulty, availability = availability)
    }
}

@Composable
private fun PatternBadgeRow(
    difficulty: Float?,
    availability: PatternAvailability,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (difficulty != null) {
            Text(
                text = stringResource(R.string.difficulty_format, difficulty),
                style = MaterialTheme.typography.labelSmall,
                color = RavelryTeal,
            )
        }
        PriceBadge(availability = availability)
    }
}

@Composable
private fun PriceBadge(availability: PatternAvailability) {
    val badgeColor =
        when (availability) {
            PatternAvailability.Free -> MaterialTheme.colorScheme.secondary
            PatternAvailability.Paid -> MaterialTheme.colorScheme.tertiary
            PatternAvailability.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val text =
        when (availability) {
            PatternAvailability.Free -> stringResource(R.string.free)
            PatternAvailability.Paid -> stringResource(R.string.paid)
            PatternAvailability.Unknown -> stringResource(R.string.availability_unknown)
        }
    Box(
        modifier =
            Modifier
                .background(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                ).padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
        )
    }
}
