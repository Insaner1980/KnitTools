package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class StatusMessageType {
    Info,
    Error,
    Success,
}

@Composable
fun StatusMessage(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    type: StatusMessageType = StatusMessageType.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val accentColor =
        when (type) {
            StatusMessageType.Info -> MaterialTheme.colorScheme.primary
            StatusMessageType.Error -> MaterialTheme.colorScheme.error
            StatusMessageType.Success -> MaterialTheme.colorScheme.primary
        }
    val icon =
        when (type) {
            StatusMessageType.Info -> Icons.Outlined.Info
            StatusMessageType.Error -> Icons.Outlined.ErrorOutline
            StatusMessageType.Success -> Icons.Outlined.CheckCircle
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        StatusIcon(icon = icon, tint = accentColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (title == null) Modifier else Modifier.padding(top = 4.dp),
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(
    icon: ImageVector,
    tint: Color,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = tint,
    )
}
