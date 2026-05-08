package com.finnvek.knittools.ui.screens.ravelry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.finnvek.knittools.ui.theme.RavelryTeal

@Composable
fun PatternCard(
    name: String,
    designerName: String,
    thumbnailUrl: String?,
    difficulty: Float?,
    isFree: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            PatternThumbnail(thumbnailUrl = thumbnailUrl, contentDescription = name)
            PatternDetails(
                name = name,
                designerName = designerName,
                difficulty = difficulty,
                isFree = isFree,
                modifier = Modifier.weight(1f),
            )
        }
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
    isFree: Boolean,
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
        PatternBadgeRow(difficulty = difficulty, isFree = isFree)
    }
}

@Composable
private fun PatternBadgeRow(
    difficulty: Float?,
    isFree: Boolean,
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
        PriceBadge(isFree = isFree)
    }
}

@Composable
private fun PriceBadge(isFree: Boolean) {
    val badgeColor = if (isFree) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    Box(
        modifier =
            Modifier
                .background(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                ).padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (isFree) stringResource(R.string.free) else stringResource(R.string.paid),
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
        )
    }
}
