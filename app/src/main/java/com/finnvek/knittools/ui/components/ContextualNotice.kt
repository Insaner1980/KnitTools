package com.finnvek.knittools.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
internal fun ContextualNotice(
    @StringRes message: Int,
    @StringRes action: Int,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = ProjectOverviewDimens.ContentGap),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NoticeAction(
                label = stringResource(R.string.contextual_notice_skip),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onDismiss,
                modifier = Modifier.weight(1f, fill = false),
            )
            NoticeAction(
                label = stringResource(action),
                color = MaterialTheme.knitToolsColors.primaryReadable,
                onClick = onAction,
                modifier = Modifier.weight(2f, fill = false),
                textAlign = TextAlign.End,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NoticeAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Box(
        modifier =
            modifier
                .clickable(role = Role.Button, onClick = onClick)
                .sizeIn(
                    minWidth = ProjectOverviewDimens.ActionTouchSize,
                    minHeight = ProjectOverviewDimens.ActionTouchSize,
                ),
        contentAlignment = if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelLarge, textAlign = textAlign)
    }
}

@Composable
internal fun ReadingLineNotice(
    hasMainCounter: Boolean,
    following: Boolean,
    dismissed: Boolean,
    blocked: Boolean,
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
) {
    if (!hasMainCounter || following || dismissed || blocked) return
    ContextualNotice(
        message = R.string.reading_line_notice,
        action = R.string.reading_line_notice_enable,
        onDismiss = onDismiss,
        onAction = onEnable,
        modifier = Modifier.padding(horizontal = ProjectOverviewDimens.ScreenHorizontalPadding),
    )
}
